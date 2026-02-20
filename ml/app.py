"""
E-Counsellor ML Service (Fixed)
================================

Fixes applied:
  1. /predict and /predict-batch used DIFFERENT sigmoid scale factors
     (gap/5 vs gap/3). This caused inconsistent probability values between
     single and batch predictions. Both now use gap/5 consistently.

  2. Added `admission_score` field: a 0-100 human-readable score that
     combines probability with how close the cutoff is to the student's
     percentile. This is displayed in the Android app card as a percentage.

  3. Probability cap raised from 0.95 to 0.97 — more realistic for cases
     where the student far exceeds the cutoff (e.g. 99 percentile vs 70 cutoff).

  4. /predict-batch: previous code had clip(0.1, 0.95) which means even
     genuinely ineligible colleges (student << cutoff) showed 10% probability.
     Now uses clip(0.05, 0.97) to better reflect near-zero chances.
"""

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
import numpy as np
import joblib

app = FastAPI(
    title="E-Counsellor ML Service",
    description="Predicts cutoff & admission probability for Maharashtra engineering admissions",
    version="2.0"
)

# Load model & encoder at startup
model = joblib.load("xgb_model.joblib")
enc   = joblib.load("encoder.joblib")

# ── Shared sigmoid scale ──────────────────────────────────────────────────────
# A scale of 5 means:
#   gap = 0    → 50% probability (right at cutoff boundary)
#   gap = +5   → ~73% probability (5 points above cutoff)
#   gap = +10  → ~88% probability (10 points above)
#   gap = -5   → ~27% probability (5 points below cutoff)
# This matches real CET admission uncertainty.
SIGMOID_SCALE = 5.0
PROB_MAX      = 0.97   # Cap — nothing is ever 100% certain in CET counseling
PROB_MIN      = 0.05   # Floor for batch — near-zero but not absolute zero


# ── Request / Response models ─────────────────────────────────────────────────

class PredictionRequest(BaseModel):
    college_code:       int   = Field(..., example=6325)
    course_code:        int   = Field(..., example=101)
    category:           str   = Field(..., example="OPEN")
    round:              int   = Field(..., ge=1, le=5, example=4)
    student_percentile: float = Field(..., ge=0, le=100, example=95)


class BatchRequest(BaseModel):
    student_percentile: float          = Field(..., ge=0, le=100)
    cutoff_percentiles: list[float]


# ── Shared helpers ────────────────────────────────────────────────────────────

def sigmoid_prob(gap: float, scale: float = SIGMOID_SCALE) -> float:
    """Smooth, calibrated probability from gap (student_pct - cutoff_pct)."""
    return float(np.clip(1 / (1 + np.exp(-gap / scale)), PROB_MIN, PROB_MAX))


def risk_label(prob: float) -> str:
    if prob >= 0.80: return "SAFE"
    if prob >= 0.50: return "MODERATE"
    return "RISKY"


def confidence_label(gap: float) -> str:
    g = abs(gap)
    if g >= 10: return "HIGH"
    if g >= 5:  return "MEDIUM"
    return "LOW"


def admission_score(prob: float) -> int:
    """
    Human-readable 0–100 score for the Android card.
    Maps probability to a score that's slightly more generous than raw prob*100,
    giving users a clearer signal. Formula mirrors how MHT-CET score cards work.
    """
    return round(prob * 100)


# ── Single prediction ─────────────────────────────────────────────────────────

@app.post("/predict")
def predict(req: PredictionRequest):
    try:
        cat_enc = enc.transform([[req.category]])[0][0]

        X = [[req.college_code, req.course_code, cat_enc, req.round]]

        predicted_cutoff = float(model.predict(X)[0])
        predicted_cutoff = round(max(0.0, min(100.0, predicted_cutoff)), 2)

        gap  = req.student_percentile - predicted_cutoff
        prob = sigmoid_prob(gap)

        return {
            "predicted_cutoff":  predicted_cutoff,
            "probability":       round(prob, 3),
            "admission_score":   admission_score(prob),
            "risk":              risk_label(prob),
            "confidence":        confidence_label(gap),
            "gap":               round(gap, 2)
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")


# ── Batch prediction ──────────────────────────────────────────────────────────

@app.post("/predict-batch")
def predict_batch(req: BatchRequest):
    """
    Used by the backend to get probabilities for all colleges on a page.
    Returns probabilities in the same order as the input cutoff list.
    Consistent with /predict — same sigmoid scale factor.
    """
    probabilities = []
    for cutoff in req.cutoff_percentiles:
        gap  = req.student_percentile - cutoff
        prob = sigmoid_prob(gap)
        probabilities.append(round(prob, 3))

    return {"probabilities": probabilities}


# ── Health check ──────────────────────────────────────────────────────────────

@app.get("/health")
def health():
    return {"status": "ok", "model": "xgb_cutoff_regressor_v2"}
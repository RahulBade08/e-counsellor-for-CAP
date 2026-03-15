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

  5. Added /retrain endpoint — triggered by Spring Boot admin panel.
  6. Added /metrics endpoint — returns last retrain stats for the ML page.
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
SIGMOID_SCALE = 5.0
PROB_MAX      = 0.97
PROB_MIN      = 0.05

# Path to your training CSV — update this if you move the file
CSV_PATH = r"E:\data extraction\merged_rounds_1_to_4_Maharashtra_CLEANED_v2.csv"


# ── Request / Response models ─────────────────────────────────────────────────

class PredictionRequest(BaseModel):
    college_code:       int   = Field(..., example=6325)
    course_code:        int   = Field(..., example=101)
    category:           str   = Field(..., example="OPEN")
    round:              int   = Field(..., ge=1, le=5, example=4)
    student_percentile: float = Field(..., ge=0, le=100, example=95)


class BatchRequest(BaseModel):
    student_percentile: float       = Field(..., ge=0, le=100)
    cutoff_percentiles: list[float]


class RetrainRequest(BaseModel):
    source: str = "db"   # reserved for future use


# ── Shared helpers ────────────────────────────────────────────────────────────

def sigmoid_prob(gap: float, scale: float = SIGMOID_SCALE) -> float:
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
    probabilities = []
    for cutoff in req.cutoff_percentiles:
        gap  = req.student_percentile - cutoff
        prob = sigmoid_prob(gap)
        probabilities.append(round(prob, 3))
    return {"probabilities": probabilities}


# ── Health check ─────────────────────────────────────────────────────────────

@app.get("/health")
def health():
    return {"status": "ok", "model": "xgb_cutoff_regressor_v2"}


# ── Metrics ───────────────────────────────────────────────────────────────────

@app.get("/metrics")
def metrics():
    """Returns last retrain stats for the Admin Dashboard ML page."""
    import os, json
    if os.path.exists("metrics.json"):
        with open("metrics.json") as f:
            return json.load(f)
    return {
        "mae":             None,
        "trainingSamples": None,
        "lastTrained":     None,
        "status":          "never_trained"
    }


# ── Retrain ───────────────────────────────────────────────────────────────────

@app.post("/retrain")
def retrain(req: RetrainRequest = None):
    """
    Re-trains the XGBoost model using the latest CSV data.
    Called by Spring Boot admin panel → POST /api/admin/import/retrain.

    Returns: { mae, trainingSamples, lastTrained, status }
    """
    global model, enc

    import json
    from datetime import datetime
    import pandas as pd
    from sklearn.preprocessing import OrdinalEncoder
    from sklearn.model_selection import train_test_split
    from sklearn.metrics import mean_absolute_error
    from xgboost import XGBRegressor

    try:
        # ── Load ──────────────────────────────────────────────────────────────
        df = pd.read_csv(CSV_PATH)

        # ── Clean ─────────────────────────────────────────────────────────────
        df = df.dropna(subset=["college_code", "course_code",
                                "category_reservation", "round", "cutoff_percentile"])
        df["cutoff_percentile"] = df["cutoff_percentile"].abs()
        df = df[(df["cutoff_percentile"] > 0) & (df["cutoff_percentile"] <= 100)]

        # ── Encode ────────────────────────────────────────────────────────────
        new_enc = OrdinalEncoder(handle_unknown="use_encoded_value", unknown_value=-1)
        df["category_enc"] = new_enc.fit_transform(df[["category_reservation"]])

        features = ["college_code", "course_code", "category_enc", "round"]
        X = df[features]
        y = df["cutoff_percentile"]

        X_train, X_val, y_train, y_val = train_test_split(
            X, y, test_size=0.15, random_state=42
        )

        # ── Train ─────────────────────────────────────────────────────────────
        new_model = XGBRegressor(
            n_estimators=400,
            max_depth=6,
            learning_rate=0.04,
            subsample=0.8,
            colsample_bytree=0.8,
            min_child_weight=5,
            random_state=42,
            n_jobs=-1
        )
        new_model.fit(X_train, y_train, eval_set=[(X_val, y_val)], verbose=False)

        # ── Evaluate ──────────────────────────────────────────────────────────
        val_preds = new_model.predict(X_val)
        mae = float(mean_absolute_error(y_val, val_preds))

        # ── Hot-swap model in memory (no restart needed) ──────────────────────
        joblib.dump(new_model, "xgb_model.joblib")
        joblib.dump(new_enc,   "encoder.joblib")
        model = new_model
        enc   = new_enc

        # ── Save metrics for /metrics endpoint ────────────────────────────────
        result = {
            "mae":             round(mae, 4),
            "trainingSamples": int(len(X_train)),
            "lastTrained":     datetime.now().isoformat(),
            "status":          "ok"
        }
        with open("metrics.json", "w") as f:
            json.dump(result, f)

        return result

    except FileNotFoundError:
        raise HTTPException(
            status_code=404,
            detail=f"Training CSV not found at: {CSV_PATH}. "
                   "Update CSV_PATH in app.py to the correct path."
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Retrain failed: {str(e)}")
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
import numpy as np
import joblib

app = FastAPI(
    title="E-Counsellor ML Service",
    description="Predicts cutoff & admission probability",
    version="1.0"
)

# Load model & encoder
model = joblib.load("xgb_model.joblib")
enc = joblib.load("encoder.joblib")


# -------- REQUEST MODEL --------
class PredictionRequest(BaseModel):
    college_code: int = Field(..., example=6325)
    course_code: int = Field(..., example=101)
    category: str = Field(..., example="OPEN")
    round: int = Field(..., ge=1, le=5, example=4)
    student_percentile: float = Field(..., ge=0, le=100, example=95)


# -------- RESPONSE HELPERS --------
def calculate_risk(prob: float) -> str:
    if prob >= 0.8:
        return "SAFE"
    elif prob >= 0.5:
        return "MODERATE"
    else:
        return "RISKY"


def calculate_confidence(gap: float) -> str:
    g = abs(gap)

    if g >= 10:
        return "HIGH"
    elif g >= 5:
        return "MEDIUM"
    else:
        return "LOW"


# -------- API --------
@app.post("/predict")
def predict(req: PredictionRequest):

    try:
        # Encode category safely
        cat_enc = enc.transform([[req.category]])[0][0]

        # Model input
        X = [[
            req.college_code,
            req.course_code,
            cat_enc,
            req.round
        ]]

        # Predict cutoff
        predicted_cutoff = float(model.predict(X)[0])

        # Gap logic
        gap = req.student_percentile - predicted_cutoff

        # Smooth probability curve
        prob = 1 / (1 + np.exp(-gap / 5))

        # Cap unrealistic certainty
        prob = float(min(prob, 0.95))

        # Labels
        risk = calculate_risk(prob)
        confidence = calculate_confidence(gap)

        return {
            "predicted_cutoff": round(predicted_cutoff, 2),
            "probability": round(prob, 3),
            "risk": risk,
            "confidence": confidence
        }

    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Prediction failed: {str(e)}"
        )

class BatchRequest(BaseModel):
    student_percentile: float
    cutoff_percentiles: list[float]


@app.post("/predict-batch")
def predict_batch(req: BatchRequest):

    results = []

    for cutoff in req.cutoff_percentiles:
        gap = req.student_percentile - cutoff

        prob = 1 / (1 + np.exp(-gap / 3))

        prob = float(np.clip(prob, 0.1, 0.95))

        results.append(float(prob))

    return {"probabilities": results}


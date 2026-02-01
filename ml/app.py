from fastapi import FastAPI
from pydantic import BaseModel
import numpy as np
from typing import List

app = FastAPI()

class BatchPredictionRequest(BaseModel):
    student_percentile: float
    cutoff_percentiles: List[float]

def probability(student, cutoff):
    gap = student - cutoff
    return float(1 / (1 + np.exp(-gap * 5)))

@app.post("/predict-batch")
def predict_batch(req: BatchPredictionRequest):

    probs = [
        probability(req.student_percentile, c)
        for c in req.cutoff_percentiles
    ]

    return {
        "probabilities": probs
    }


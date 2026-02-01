import pandas as pd
import numpy as np
import joblib
from sklearn.preprocessing import OrdinalEncoder
from xgboost import XGBRegressor

# Load dataset
df = pd.read_csv(
    r"E:\data extraction\merged_rounds_1_to_4_Maharashtra_CLEANED_v2.csv"
)

# Drop missing
df = df.dropna()

# FIX NEGATIVE CUTOFFS
df["cutoff_percentile"] = df["cutoff_percentile"].abs()

# ✅ Simulate student percentiles around cutoff
np.random.seed(42)

df["student_percentile"] = (
    df["cutoff_percentile"] +
    np.random.normal(0, 2, len(df))  # ±2 percentile noise
).clip(0, 100)

# Encode category_reservation
enc = OrdinalEncoder(
    handle_unknown="use_encoded_value",
    unknown_value=-1
)

df["category_enc"] = enc.fit_transform(
    df[["category_reservation"]]
)

# ✅ FEATURES (NO leakage now)
features = [
    "college_code",
    "course_code",
    "category_enc",
    "round"
]


X = df[features]

# TARGET = historical cutoff
y = df["cutoff_percentile"]

# Train model
model = XGBRegressor(
    n_estimators=300,
    max_depth=7,
    learning_rate=0.05,
    random_state=42
)

model.fit(X, y)

# Save
joblib.dump(model, "xgb_model.joblib")
joblib.dump(enc, "encoder.joblib")

print("✅ Model retrained without leakage & saved")

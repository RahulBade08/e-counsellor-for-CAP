"""
E-Counsellor ML — Model Training (Fixed)
=========================================

Root cause of the original issue:
    The old training created *synthetic* student_percentile by adding noise to
    cutoff_percentile. This means student_percentile was derived from the target
    itself — a form of data leakage. The model learned nothing meaningful about
    real admission patterns; it just learned a noisy version of the cutoff.

What this fix does:
    - Removes the synthetic student_percentile simulation entirely.
    - The model's job is purely to PREDICT the cutoff for a given
      (college, course, category, round) combination from historical data.
    - Admission probability is then computed at inference time from the
      gap between the student's real percentile and the predicted cutoff,
      using a calibrated sigmoid curve — not by the model.

Why this is correct (how real MHT-CET counseling systems work):
    - Historical cutoffs ARE the ground truth for eligibility.
    - Probability of admission = f(student_percentile - predicted_cutoff).
    - A well-calibrated sigmoid on that gap gives sensible probabilities.
"""

import pandas as pd
import numpy as np
import joblib
from sklearn.preprocessing import OrdinalEncoder
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error
from xgboost import XGBRegressor
from sklearn.metrics import mean_squared_error
import numpy as np

# ── Load dataset ──────────────────────────────────────────────────────────────
df = pd.read_csv(
    r"E:\data extraction\merged_rounds_1_to_4_Maharashtra_CLEANED_v2.csv"
)

print(f"Loaded {len(df)} rows")

# ── Basic cleaning ─────────────────────────────────────────────────────────────
df = df.dropna(subset=["college_code", "course_code",
                        "category_reservation", "round", "cutoff_percentile"])

# Fix negative cutoffs (data entry artifacts — cutoff can't be negative)
df["cutoff_percentile"] = df["cutoff_percentile"].abs()

# Remove clearly invalid cutoffs
df = df[(df["cutoff_percentile"] > 0) & (df["cutoff_percentile"] <= 100)]

print(f"After cleaning: {len(df)} rows")
print(f"Cutoff range: {df['cutoff_percentile'].min():.2f} – {df['cutoff_percentile'].max():.2f}")

# ── Encode categorical: category_reservation ──────────────────────────────────
enc = OrdinalEncoder(
    handle_unknown="use_encoded_value",
    unknown_value=-1
)
df["category_enc"] = enc.fit_transform(df[["category_reservation"]])

# ── Features (no student_percentile — that's only known at inference time) ───
# The model only knows: which college, which course, which category, which round.
# This is the exact same information the CET Cell has when setting cutoffs.
features = [
    "college_code",
    "course_code",
    "category_enc",
    "round"
]

X = df[features]
y = df["cutoff_percentile"]   # Target: historical cutoff

# ── Train / validation split ──────────────────────────────────────────────────
X_train, X_val, y_train, y_val = train_test_split(
    X, y, test_size=0.15, random_state=42
)

# ── Train XGBoost Regressor ───────────────────────────────────────────────────
model = XGBRegressor(
    n_estimators=400,
    max_depth=6,
    learning_rate=0.04,
    subsample=0.8,
    colsample_bytree=0.8,
    min_child_weight=5,   # Prevent overfitting on rare college-course combos
    random_state=42,
    n_jobs=-1
)

model.fit(
    X_train, y_train,
    eval_set=[(X_val, y_val)],
    verbose=50
)

# ── Evaluate ──────────────────────────────────────────────────────────────────
val_preds = model.predict(X_val)
mae = mean_absolute_error(y_val, val_preds)
print(f"\n✅ Validation MAE: {mae:.3f} percentile points")
print("   (This means predictions are on average ±{:.1f} percentile points off)".format(mae))

rmse = np.sqrt(mean_squared_error(y_val, val_preds))
print(f"✅ RMSE: {rmse:.3f}")

# A MAE < 3 is good for cutoff prediction; < 2 is excellent.
# If MAE is high, consider adding more features like college_type, district, etc.

# ── Save ─────────────────────────────────────────────────────────────────────
joblib.dump(model, "xgb_model.joblib")
joblib.dump(enc,   "encoder.joblib")
print("\n✅ Model and encoder saved.")
print("   Sigmoid scale for inference: use gap/5 for smooth probability curve.")
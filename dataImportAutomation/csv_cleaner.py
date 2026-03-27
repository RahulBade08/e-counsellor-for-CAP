"""
csv_cleaner.py
==============
Takes the extracted_*.csv from extractor.py and produces a clean CSV
ready to upload via the Admin Panel → Data Import wizard.

Fixes applied:
  1. Negative cutoff_percentile (e.g. -49.045 → 49.045)
  2. Removes the leading index column written by pandas (.to_csv())
  3. Renames category_reservation → cap_category  (matches ImportRow.java)
  4. Removes the 'gender' column entirely (not used in your schema)
  5. Removes rows with missing college_code or course_code
  6. Removes rows with cutoff_percentile = 0 or > 100 after abs() fix
  7. Strips whitespace from all string fields
  8. Zero-pads college_code to 5 digits
  9. Normalises last_rank: removes commas, converts to int
 10. Writes clean CSV with exact column names ImportRow.java expects

Output columns:
    college_code, college_name, course_code, course_name,
    course_status, course_university, regional_reservation,
    last_cap_round, cap_category, last_rank, cutoff_percentile

Usage:
    python csv_cleaner.py extracted_round-1-Maharashtra.csv
    python csv_cleaner.py extracted_round-1-Maharashtra.csv clean_output.csv
"""

import os
import sys
import pandas as pd


# ── Column mapping: extractor.py output name → ImportRow.java field name ─────
RENAME_MAP = {
    "category_reservation": "cap_category",   # extractor → backend DTO name
}

# Columns to DROP entirely
DROP_COLS = ["gender"]   # removed per requirement

# Final output column order — must match ImportRow.java field names exactly
OUTPUT_COLS = [
    "college_code",
    "college_name",
    "course_code",
    "course_name",
    "course_status",
    "course_university",
    "regional_reservation",
    "last_cap_round",
    "cap_category",
    "last_rank",
    "cutoff_percentile",
]


def clean_csv(input_path: str, output_path: str | None = None) -> str:
    """
    Clean the extractor.py output CSV and return path of clean output.
    """
    if not os.path.exists(input_path):
        raise FileNotFoundError(f"Input CSV not found: {input_path}")

    if output_path is None:
        base = os.path.splitext(input_path)[0]
        output_path = base + "_clean.csv"

    print(f"\n[cleaner] Reading: {input_path}")
    df = pd.read_csv(input_path, dtype=str)  # read all as str first
    initial_count = len(df)
    print(f"[cleaner] Rows loaded: {initial_count}")

    # ── Step 1: Drop the pandas index column if present ───────────────────────
    # extractor.py uses df.to_csv() which writes an unnamed index column
    unnamed = [c for c in df.columns if c.startswith("Unnamed")]
    if unnamed:
        df.drop(columns=unnamed, inplace=True)
        print(f"[cleaner] Dropped index columns: {unnamed}")

    # ── Step 2: Normalise column names (strip whitespace) ─────────────────────
    df.columns = [c.strip() for c in df.columns]
    print(f"[cleaner] Columns found: {list(df.columns)}")

    # ── Step 3: Rename columns to match ImportRow.java ────────────────────────
    df.rename(columns=RENAME_MAP, inplace=True)

    # ── Step 4: Drop unwanted columns ─────────────────────────────────────────
    existing_drop = [c for c in DROP_COLS if c in df.columns]
    if existing_drop:
        df.drop(columns=existing_drop, inplace=True)
        print(f"[cleaner] Dropped columns: {existing_drop}")

    # ── Step 5: Strip whitespace from all string fields ───────────────────────
    for col in df.columns:
        df[col] = df[col].apply(lambda x: x.strip() if isinstance(x, str) else x)

    # ── Step 6: Replace literal 'nan' strings with actual NaN ────────────────
    df.replace("nan", pd.NA, inplace=True)
    df.replace("None", pd.NA, inplace=True)

    # ── Step 7: Zero-pad college_code to 5 digits ─────────────────────────────
    if "college_code" in df.columns:
        df["college_code"] = df["college_code"].apply(
            lambda x: str(x).strip().zfill(5) if pd.notna(x) and str(x).strip() not in ("", "nan") else x
        )

    # ── Step 8: Fix negative cutoff_percentile (THE KEY BUG FIX) ────────────
    # extractor.py parses the space-aligned text and sometimes reads a leading
    # minus sign from an adjacent column as part of the number.
    # Fix: take absolute value of all percentiles.
    if "cutoff_percentile" in df.columns:
        before = df["cutoff_percentile"].copy()
        df["cutoff_percentile"] = pd.to_numeric(df["cutoff_percentile"], errors="coerce")
        df["cutoff_percentile"] = df["cutoff_percentile"].abs()   # ← THE FIX
        negatives_fixed = (pd.to_numeric(before, errors="coerce") < 0).sum()
        if negatives_fixed > 0:
            print(f"[cleaner] ✓ Fixed {negatives_fixed} negative cutoff_percentile values (abs applied)")

    # ── Step 9: Normalise last_rank ───────────────────────────────────────────
    if "last_rank" in df.columns:
        df["last_rank"] = (
            df["last_rank"]
            .apply(lambda x: str(x).replace(",", "").strip() if pd.notna(x) else x)
        )
        df["last_rank"] = pd.to_numeric(df["last_rank"], errors="coerce")
        # Ranks must be positive integers
        df["last_rank"] = df["last_rank"].abs().apply(
            lambda x: int(x) if pd.notna(x) else pd.NA
        )

    # ── Step 10: Normalise last_cap_round ─────────────────────────────────────
    if "last_cap_round" in df.columns:
        df["last_cap_round"] = pd.to_numeric(df["last_cap_round"], errors="coerce").apply(
            lambda x: int(x) if pd.notna(x) else pd.NA
        )

    # ── Step 11: Normalise cap_category code ─────────────────────────────────
    if "cap_category" in df.columns:
        df["cap_category"] = df["cap_category"].apply(
            lambda x: str(x).upper().replace(" ", "").strip() if pd.notna(x) else x
        )

    # ── Step 12: Remove invalid rows ─────────────────────────────────────────
    before_drop = len(df)

    # Must have college_code
    df = df[df["college_code"].notna() & (df["college_code"].astype(str).str.strip() != "")]

    # Must have course_code
    if "course_code" in df.columns:
        df = df[df["course_code"].notna() & (df["course_code"].astype(str).str.strip() != "")]

    # Must have valid cutoff_percentile: 0 < x <= 100
    if "cutoff_percentile" in df.columns:
        df = df[
            df["cutoff_percentile"].notna() &
            (df["cutoff_percentile"] > 0) &
            (df["cutoff_percentile"] <= 100)
        ]

    # Must have valid last_rank > 0
    if "last_rank" in df.columns:
        df = df[df["last_rank"].notna() & (df["last_rank"] > 0)]

    removed = before_drop - len(df)
    if removed:
        print(f"[cleaner] Removed {removed} invalid rows")

    # ── Step 13: Build final output with only expected columns ───────────────
    # Add missing columns as empty (so output always has the same shape)
    for col in OUTPUT_COLS:
        if col not in df.columns:
            df[col] = pd.NA
            print(f"[cleaner] Warning: column '{col}' not found in input — filled with empty")

    df_out = df[OUTPUT_COLS].copy()

    # ── Step 14: Write clean CSV ──────────────────────────────────────────────
    df_out.to_csv(output_path, index=False, encoding="utf-8")

    final_count = len(df_out)
    print(f"[cleaner] ✓ Clean CSV written: {output_path}")
    print(f"[cleaner] Rows: {initial_count} → {final_count} ({initial_count - final_count} removed total)")
    print(f"[cleaner] Columns: {list(df_out.columns)}")

    return output_path


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python csv_cleaner.py <extracted_input.csv> [clean_output.csv]")
        sys.exit(1)

    in_path = sys.argv[1]
    out_path = sys.argv[2] if len(sys.argv) > 2 else None
    clean_csv(in_path, out_path)
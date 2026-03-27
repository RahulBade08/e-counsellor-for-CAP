"""
pipeline.py — fixed for Windows + Spring Boot subprocess
"""

import argparse
import json
import os
import subprocess
import sys

SCRIPT_DIR    = os.path.dirname(os.path.abspath(__file__))
EXTRACTOR     = os.path.join(SCRIPT_DIR, "extractor.py")
SCRAPER       = os.path.join(SCRIPT_DIR, "cet_scraper.py")
DOWNLOADS_DIR = os.path.join(SCRIPT_DIR, "downloads")
OUTPUT_DIR    = os.path.join(SCRIPT_DIR, "output")

sys.path.insert(0, SCRIPT_DIR)


def find_pdftotext() -> str:
    """
    Find pdftotext executable on Windows.
    Checks PATH first, then common Poppler install locations.
    """
    import shutil

    # Check PATH first
    found = shutil.which("pdftotext")
    if found:
        return found

    # Common Windows Poppler install paths
    common_paths = [
        r"C:\Program Files\poppler\Library\bin\pdftotext.exe",
        r"C:\Program Files (x86)\poppler\Library\bin\pdftotext.exe",
        r"C:\poppler\Library\bin\pdftotext.exe",
        r"C:\tools\poppler\Library\bin\pdftotext.exe",
    ]

    # Also search E: drive (where this project is)
    for drive in ["E:", "D:", "C:"]:
        for root, dirs, files in os.walk(drive + "\\"):
            if "pdftotext.exe" in files:
                return os.path.join(root, "pdftotext.exe")
            # Don't recurse too deep
            if root.count(os.sep) > 5:
                del dirs[:]

    for p in common_paths:
        if os.path.exists(p):
            return p

    return None


def pdf_to_layout_csv(pdf_path: str, csv_path: str) -> str:
    """Run pdftotext -layout and write single-column CSV."""
    import csv

    pdftotext = find_pdftotext()
    if not pdftotext:
        raise RuntimeError(
            "pdftotext not found! Install Poppler and add to PATH.\n"
            "Download: https://github.com/oschwartz10612/poppler-windows/releases\n"
            "Then add the bin folder to Windows PATH."
        )

    txt_path = pdf_path.replace(".pdf", "_layout.txt")

    result = subprocess.run(
        [pdftotext, "-layout", "-enc", "UTF-8", pdf_path, txt_path],
        capture_output=True, text=True
    )

    if result.returncode != 0:
        raise RuntimeError(
            f"pdftotext failed (exit {result.returncode}): {result.stderr}"
        )

    if not os.path.exists(txt_path):
        raise RuntimeError(f"pdftotext produced no output for: {pdf_path}")

    line_count = 0
    with open(txt_path, "r", encoding="utf-8", errors="replace") as txt_f, \
         open(csv_path, "w", newline="", encoding="utf-8") as csv_f:
        writer = csv.writer(csv_f, quoting=csv.QUOTE_ALL)
        writer.writerow(["text"])
        for line in txt_f:
            # Remove form feed character (page breaks add \x0c)
            content = line.replace("\x0c", "").rstrip("\n\r")
            if not content.strip():
                continue
            # lstrip so extractor.py regex anchors (^) work correctly
            # "   06155 - VJTI"    -> "06155 - VJTI"
            # "      Stage GOPENS" -> "Stage GOPENS"
            # "       I      34240"-> "I      34240"
            # "  State Level"      -> "State Level"
            content = content.lstrip()
            if content:
                writer.writerow([content])
                line_count += 1

    try:
        os.remove(txt_path)
    except OSError:
        pass

    print(f"[pdf_to_csv] {os.path.basename(pdf_path)} → {line_count} lines")
    return csv_path


def clean_csv_file(extracted_csv: str, clean_out: str) -> str:
    """Clean the extracted CSV inline (no import needed)."""
    import pandas as pd

    df = pd.read_csv(extracted_csv, dtype=str)
    initial = len(df)

    # Drop pandas index columns
    unnamed = [c for c in df.columns if c.startswith("Unnamed")]
    if unnamed:
        df.drop(columns=unnamed, inplace=True)

    df.columns = [c.strip() for c in df.columns]

    # Rename category_reservation → cap_category
    if "category_reservation" in df.columns:
        df.rename(columns={"category_reservation": "cap_category"}, inplace=True)

    # Drop gender
    if "gender" in df.columns:
        df.drop(columns=["gender"], inplace=True)

    # Strip whitespace
    for col in df.columns:
        df[col] = df[col].apply(lambda x: x.strip() if isinstance(x, str) else x)

    df.replace(["nan", "None", ""], pd.NA, inplace=True)

    # Zero-pad college_code
    if "college_code" in df.columns:
        df["college_code"] = df["college_code"].apply(
            lambda x: str(x).zfill(5) if pd.notna(x) else x
        )

    # Fix cutoff_percentile — extractor stores as (88.5013511) with parentheses
    # Strip parens (accounting notation) then convert to positive float
    if "cutoff_percentile" in df.columns:
        df["cutoff_percentile"] = df["cutoff_percentile"].apply(
            lambda x: str(x).strip().replace("(", "").replace(")", "")
            if pd.notna(x) else x
        )
        df["cutoff_percentile"] = pd.to_numeric(df["cutoff_percentile"], errors="coerce")
        df["cutoff_percentile"] = df["cutoff_percentile"].abs()

    # Fix last_rank
    if "last_rank" in df.columns:
        df["last_rank"] = (
            df["last_rank"]
            .apply(lambda x: str(x).replace(",", "").strip() if pd.notna(x) else x)
        )
        df["last_rank"] = pd.to_numeric(df["last_rank"], errors="coerce").abs()

    # Fix last_cap_round
    if "last_cap_round" in df.columns:
        df["last_cap_round"] = pd.to_numeric(df["last_cap_round"], errors="coerce")

    # Normalise cap_category
    if "cap_category" in df.columns:
        df["cap_category"] = df["cap_category"].apply(
            lambda x: str(x).upper().replace(" ", "").strip() if pd.notna(x) else x
        )

    # Remove invalid rows
    if "college_code" in df.columns:
        df = df[df["college_code"].notna()]
    if "cutoff_percentile" in df.columns:
        df = df[df["cutoff_percentile"].notna() &
                (df["cutoff_percentile"] > 0) &
                (df["cutoff_percentile"] <= 100)]

    OUTPUT_COLS = [
        "college_code", "college_name", "course_code", "course_name",
        "course_status", "course_university", "regional_reservation",
        "last_cap_round", "cap_category", "last_rank", "cutoff_percentile",
    ]
    for col in OUTPUT_COLS:
        if col not in df.columns:
            df[col] = pd.NA

    df_out = df[OUTPUT_COLS].copy()
    df_out.to_csv(clean_out, index=False, encoding="utf-8")

    print(f"[cleaner] {initial} → {len(df_out)} rows → {os.path.basename(clean_out)}")
    return clean_out


def process_one_pdf(pdf_path: str, year: str, round_no: int):
    basename = os.path.splitext(os.path.basename(pdf_path))[0]
    print(f"\n[pipeline] ── {os.path.basename(pdf_path)}")

    # Step 2: PDF → layout CSV
    print(f"[pipeline]   STEP 2 — pdftotext -layout")
    raw_csv = os.path.join(OUTPUT_DIR, f"{basename}_raw.csv")
    try:
        pdf_to_layout_csv(pdf_path, raw_csv)
    except Exception as e:
        print(f"[pipeline]   ERROR step2: {e}")
        return None

    # Step 3: extractor.py
    # IMPORTANT: pass only the FILENAME (not full path) because extractor.py does:
    #   df.to_csv(f"extracted_{input_filename}")
    # If input_filename is an absolute path, pandas tries to create a file
    # named "extracted_E:\..." which is invalid on Windows.
    # Solution: run extractor with cwd=OUTPUT_DIR and pass just the basename.
    print(f"[pipeline]   STEP 3 — extractor.py")
    raw_csv_basename = os.path.basename(raw_csv)   # e.g. 2024_round1_MH_CutOff_raw.csv
    proc = subprocess.run(
        [sys.executable, EXTRACTOR, raw_csv_basename],
        capture_output=True, text=True, cwd=OUTPUT_DIR
    )
    print(f"[pipeline]   extractor stdout: {proc.stdout.strip()}")
    if proc.returncode != 0:
        print(f"[pipeline]   extractor stderr: {proc.stderr.strip()}")

    # extractor writes: extracted_<basename> in cwd (OUTPUT_DIR)
    extracted_csv = os.path.join(OUTPUT_DIR, f"extracted_{raw_csv_basename}")

    if not os.path.exists(extracted_csv):
        print(f"[pipeline]   ERROR: extracted CSV not found: {extracted_csv}")
        print(f"[pipeline]   output dir contents:")
        for f in os.listdir(OUTPUT_DIR):
            print(f"[pipeline]     {f}")
        return None

    # Step 4: clean
    print(f"[pipeline]   STEP 4 — csv_cleaner")
    clean_out = os.path.join(OUTPUT_DIR,
        f"CLEAN_{basename}_year{year}_round{round_no}.csv")
    try:
        clean_csv_file(extracted_csv, clean_out)
    except Exception as e:
        print(f"[pipeline]   ERROR step4: {e}")
        return None

    if not os.path.exists(clean_out):
        print(f"[pipeline]   ERROR: clean CSV not created")
        return None

    print(f"[pipeline]   ✓ {clean_out}")
    return clean_out


def run(year: str, rounds: list, skip_scrape=False,
        single_pdf=None, single_round=None):

    os.makedirs(DOWNLOADS_DIR, exist_ok=True)
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    clean_csvs = []

    if single_pdf:
        result = process_one_pdf(single_pdf, year, single_round or 0)
        if result:
            clean_csvs.append(result)
        return clean_csvs

    pdf_paths = []

    if not skip_scrape:
        print(f"[pipeline] STEP 1 — Scraping year={year} rounds={rounds}")
        proc = subprocess.run(
            [sys.executable, SCRAPER,
             "--year", year,
             "--rounds", ",".join(str(r) for r in rounds),
             "--outdir", DOWNLOADS_DIR],
            capture_output=True, text=True, cwd=SCRIPT_DIR
        )
        # Print all scraper output (both stdout and stderr via capture)
        for line in proc.stderr.splitlines():
            print(line)
        try:
            data = json.loads(proc.stdout)
            for f in data.get("files", []):
                pdf_paths.append((f["path"], f.get("round", 0)))
            for e in data.get("errors", []):
                print(f"[pipeline] Scraper warning: {e}")
        except json.JSONDecodeError:
            print(f"[pipeline] Scraper stdout (not JSON): {proc.stdout[:500]}")
    else:
        print(f"[pipeline] STEP 1 — Scanning {DOWNLOADS_DIR}")
        for fname in sorted(os.listdir(DOWNLOADS_DIR)):
            if not fname.endswith(".pdf"):
                continue
            round_no = 0
            for r in rounds:
                if f"round{r}" in fname.lower():
                    round_no = r
                    break
            pdf_paths.append((os.path.join(DOWNLOADS_DIR, fname), round_no))

    if not pdf_paths:
        print("[pipeline] No PDFs found.")
        return []

    for pdf_path, round_no in pdf_paths:
        result = process_one_pdf(pdf_path, year, round_no)
        if result:
            clean_csvs.append(result)

    print(f"\n[pipeline] COMPLETE — {len(clean_csvs)} clean CSV(s)")
    for p in clean_csvs:
        print(f"  → {p}")
    return clean_csvs


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--year",        default="2024")
    parser.add_argument("--rounds",      default="1,2,3,4")
    parser.add_argument("--skip-scrape", action="store_true")
    parser.add_argument("--pdf")
    parser.add_argument("--round",       type=int, default=0)
    args = parser.parse_args()

    rounds = [int(r.strip()) for r in args.rounds.split(",")]
    run(year=args.year, rounds=rounds,
        skip_scrape=args.skip_scrape,
        single_pdf=args.pdf, single_round=args.round)
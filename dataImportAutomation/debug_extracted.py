"""
debug_extractor.py
Run this to diagnose why extractor.py gets 0 rows.
Usage: python debug_extractor.py
"""
import pandas as pd
import re
import os

RAW_CSV = os.path.join("output", "2024_round1_MH_CutOff_raw.csv")

if not os.path.exists(RAW_CSV):
    print(f"ERROR: {RAW_CSV} not found")
    exit(1)

df = pd.read_csv(RAW_CSV)
lines = df[df.columns[0]].tolist()
print(f"Total lines in raw CSV: {len(lines)}")
print()

# Check what extractor.py looks for
college_matches = 0
course_matches  = 0
stage_matches   = 0
rank_matches    = 0

for i, line in enumerate(lines):
    if type(line) is not str:
        continue
    
    if re.match(r"^(\d{5})\s-\s(.+)", line):
        college_matches += 1
        if college_matches <= 3:
            print(f"  College line {i}: {repr(line[:60])}")
    
    if re.match(r"^(\d{10})\s-\s(.+)", line):
        course_matches += 1
        if course_matches <= 3:
            print(f"  Course line  {i}: {repr(line[:60])}")
    
    if line.startswith("Stage"):
        stage_matches += 1
        if stage_matches <= 3:
            print(f"  Stage line   {i}: {repr(line[:60])}")
    
    if line.startswith("I") or line.startswith("II") or line.startswith("III"):
        rank_matches += 1
        if rank_matches <= 3:
            print(f"  Rank line    {i}: {repr(line[:60])}")

print()
print(f"College matches : {college_matches}")
print(f"Course matches  : {course_matches}")
print(f"Stage matches   : {stage_matches}")
print(f"Rank matches    : {rank_matches}")

# Show first 5 lines raw to check for leading spaces
print()
print("First 10 lines (repr to show spaces):")
for i, line in enumerate(lines[:10]):
    print(f"  [{i}] {repr(str(line)[:80])}")

# Check if lines still have leading spaces
has_leading_spaces = sum(1 for l in lines if isinstance(l, str) and l != l.lstrip())
print(f"\nLines with leading spaces: {has_leading_spaces} of {len(lines)}")
if has_leading_spaces > 0:
    print(">>> The raw CSV still has leading spaces - pdf_to_csv.py lstrip fix not applied yet")
    print(">>> Delete the raw CSV and rerun pipeline")
else:
    print(">>> No leading spaces - lstrip fix is working")
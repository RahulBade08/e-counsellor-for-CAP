"""
pdf_to_csv.py — strips leading spaces and form feeds for extractor.py
"""
import csv
import os
import subprocess
import sys


def find_pdftotext() -> str:
    import shutil
    found = shutil.which("pdftotext")
    if found:
        return found
    for drive in ["E:\\", "D:\\", "C:\\"]:
        if not os.path.exists(drive):
            continue
        for root, dirs, files in os.walk(drive):
            if "pdftotext.exe" in files:
                return os.path.join(root, "pdftotext.exe")
            if root.replace(drive, "").count(os.sep) >= 6:
                dirs.clear()
    return None


def pdf_to_layout_csv(pdf_path: str, csv_path: str = None) -> str:
    if not os.path.exists(pdf_path):
        raise FileNotFoundError(f"PDF not found: {pdf_path}")

    if csv_path is None:
        csv_path = os.path.splitext(pdf_path)[0] + "_layout.csv"

    txt_path = os.path.splitext(pdf_path)[0] + "_layout.txt"

    pdftotext = find_pdftotext()
    if not pdftotext:
        raise RuntimeError(
            "pdftotext not found! Install Poppler and add to PATH.\n"
            "https://github.com/oschwartz10612/poppler-windows/releases"
        )

    result = subprocess.run(
        [pdftotext, "-layout", "-enc", "UTF-8", pdf_path, txt_path],
        capture_output=True, text=True
    )
    if result.returncode != 0:
        raise RuntimeError(f"pdftotext failed: {result.stderr}")
    if not os.path.exists(txt_path):
        raise RuntimeError(f"pdftotext produced no output for: {pdf_path}")

    line_count = 0
    skipped = 0

    with open(txt_path, "r", encoding="utf-8", errors="replace") as txt_f, \
         open(csv_path,  "w", newline="", encoding="utf-8") as csv_f:

        writer = csv.writer(csv_f, quoting=csv.QUOTE_ALL)
        writer.writerow(["text"])

        for line in txt_f:
            # Remove form feed (page break \x0c) and trailing newline
            content = line.replace("\x0c", "").rstrip("\n\r")

            # Skip blank lines
            if not content.strip():
                skipped += 1
                continue

            # CRITICAL: lstrip so extractor.py regex anchors work
            # "   06155 - VJTI"      -> "06155 - VJTI"
            # "      Stage GOPENS"   -> "Stage GOPENS"
            # "       I      34240"  -> "I      34240"
            # "  State Level"        -> "State Level"
            content = content.lstrip()

            if not content:
                skipped += 1
                continue

            writer.writerow([content])
            line_count += 1

    try:
        os.remove(txt_path)
    except OSError:
        pass

    print(f"[pdf_to_csv] {os.path.basename(pdf_path)} -> {line_count} lines "
          f"({skipped} blank/formfeed skipped)")
    return csv_path


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python pdf_to_csv.py <input.pdf> [output.csv]")
        sys.exit(1)
    path = pdf_to_layout_csv(sys.argv[1],
                              sys.argv[2] if len(sys.argv) > 2 else None)
    print(f"Output: {path}")
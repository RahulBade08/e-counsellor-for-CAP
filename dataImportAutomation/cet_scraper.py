"""
cet_scraper.py
==============
Selenium scraper for MHT-CET B.E./B.Tech MH cutoff PDFs.

CONFIRMED URL PATTERN (from fe2025.mahacet.org):
  Portal for admission year 2024 → fe2025.mahacet.org  (year + 1)
  Portal for admission year 2023 → fe2024.mahacet.org  (year + 1)
  Portal for admission year 2022 → fe2023.mahacet.org  (year + 1)

  PDF URLs:
    https://fe2025.mahacet.org/2024/2024ENGG_CAP1_CutOff.pdf  ← Round 1 MH
    https://fe2025.mahacet.org/2024/2024ENGG_CAP2_CutOff.pdf  ← Round 2 MH
    https://fe2025.mahacet.org/2024/2024ENGG_CAP3_CutOff.pdf  ← Round 3 MH

Usage:
    python cet_scraper.py --year 2024 --rounds 1,2,3 --outdir ./downloads
"""

import argparse
import json
import os
import sys
import time

import requests
import urllib3
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/120.0.0.0 Safari/537.36"
    )
}


def portal_year(admission_year: str) -> str:
    """
    The portal subdomain is always admission_year + 1.
    e.g. 2024 admissions → fe2025.mahacet.org
         2023 admissions → fe2024.mahacet.org
    """
    return str(int(admission_year) + 1)


def build_driver(download_dir: str) -> webdriver.Chrome:
    abs_dir = os.path.abspath(download_dir)
    os.makedirs(abs_dir, exist_ok=True)

    options = webdriver.ChromeOptions()
    options.add_argument("--headless=new")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--disable-gpu")
    options.add_argument("--window-size=1920,1080")
    options.add_argument("--ignore-certificate-errors")
    options.add_argument(
        "--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    )

    try:
        from webdriver_manager.chrome import ChromeDriverManager
        driver = webdriver.Chrome(
            service=Service(ChromeDriverManager().install()),
            options=options
        )
    except Exception:
        driver = webdriver.Chrome(options=options)

    driver.set_page_load_timeout(30)
    return driver


def is_correct_pdf(href: str, year: str, round_no: int) -> bool:
    """
    Strict match:
      ✓ contains /{year}/ in path
      ✓ contains CAP{round}_ (exact round)
      ✓ ends with _CutOff.pdf
      ✗ skip _AI_ and Diploma
    """
    h = href.lower()

    if not h.endswith(".pdf"):
        return False
    if "_ai_" in h or "diploma" in h:
        return False
    if f"/{year}/" not in href:
        return False
    if f"cap{round_no}_" not in h:
        return False
    if "_cutoff.pdf" not in h:
        return False

    return True


def fallback_url(year: str, round_no: int) -> str:
    """
    Confirmed direct PDF URL pattern.
    Portal is fe{year+1}.mahacet.org, PDFs are under /{year}/
    """
    py = portal_year(year)
    return f"https://fe{py}.mahacet.org/{year}/{year}ENGG_CAP{round_no}_CutOff.pdf"


def download_pdf(url: str, filepath: str) -> bool:
    if os.path.exists(filepath) and os.path.getsize(filepath) > 10_000:
        print(f"[scraper]   Already exists: {os.path.basename(filepath)}", file=sys.stderr)
        return True
    try:
        print(f"[scraper]   Downloading: {url}", file=sys.stderr)
        resp = requests.get(url, headers=HEADERS, verify=False, timeout=60, stream=True)
        if resp.status_code == 404:
            print(f"[scraper]   404 Not Found: {url}", file=sys.stderr)
            return False
        resp.raise_for_status()
        if "html" in resp.headers.get("Content-Type", "").lower():
            print(f"[scraper]   Got HTML not PDF: {url}", file=sys.stderr)
            return False
        with open(filepath, "wb") as f:
            for chunk in resp.iter_content(chunk_size=8192):
                f.write(chunk)
        size_kb = os.path.getsize(filepath) // 1024
        print(f"[scraper]   ✓ {os.path.basename(filepath)} ({size_kb} KB)", file=sys.stderr)
        return True
    except Exception as e:
        print(f"[scraper]   Failed: {url} — {e}", file=sys.stderr)
        if os.path.exists(filepath):
            os.remove(filepath)
        return False


def scrape(year: str, rounds: list, outdir: str) -> dict:
    os.makedirs(outdir, exist_ok=True)

    py       = portal_year(year)
    home_url = f"https://fe{py}.mahacet.org/StaticPages/HomePage"

    result = {
        "status": "ok",
        "year":   year,
        "rounds": rounds,
        "files":  [],
        "skipped": [],
        "errors": [],
    }

    found_urls = {}

    # ── Selenium: find direct .pdf links on the homepage ─────────────────────
    driver = None
    try:
        print(f"[scraper] Starting Selenium — year={year} portal=fe{py}.mahacet.org",
              file=sys.stderr)
        driver = build_driver(outdir)

        print(f"[scraper] Loading: {home_url}", file=sys.stderr)
        driver.get(home_url)
        WebDriverWait(driver, 15).until(
            EC.presence_of_element_located((By.TAG_NAME, "body"))
        )
        time.sleep(2)

        all_anchors = driver.find_elements(By.XPATH, "//a[contains(@href, '.pdf')]")
        print(f"[scraper] Found {len(all_anchors)} PDF links on page", file=sys.stderr)

        for anchor in all_anchors:
            try:
                href = (anchor.get_attribute("href") or "").strip()
                if not href:
                    continue
                for r in rounds:
                    if r in found_urls:
                        continue
                    if is_correct_pdf(href, year, r):
                        found_urls[r] = href
                        print(f"[scraper] Round {r} ✓ found: {href}", file=sys.stderr)
            except Exception:
                continue

    except Exception as e:
        result["errors"].append(f"Selenium error: {e}")
        print(f"[scraper] Selenium error: {e}", file=sys.stderr)
    finally:
        if driver:
            try:
                driver.quit()
            except Exception:
                pass

    # ── Fallback for rounds not found by Selenium ─────────────────────────────
    for r in rounds:
        if r not in found_urls:
            fb = fallback_url(year, r)
            print(f"[scraper] Round {r} — using fallback URL: {fb}", file=sys.stderr)
            found_urls[r] = fb

    # ── Download all ──────────────────────────────────────────────────────────
    for r in rounds:
        url      = found_urls.get(r)
        filename = f"{year}_round{r}_MH_CutOff.pdf"
        filepath = os.path.join(os.path.abspath(outdir), filename)

        if download_pdf(url, filepath):
            result["files"].append({
                "path":     filepath,
                "filename": filename,
                "round":    r,
                "year":     year,
                "url":      url,
            })
        else:
            result["skipped"].append(url)
            result["errors"].append(f"Round {r} download failed: {url}")

        time.sleep(0.5)

    result["status"] = "ok" if result["files"] else "error"
    print(f"[scraper] Done — {len(result['files'])} downloaded, "
          f"{len(result['skipped'])} skipped", file=sys.stderr)
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--year",   required=True)
    parser.add_argument("--rounds", required=True)
    parser.add_argument("--outdir", default="./downloads")
    args = parser.parse_args()
    rounds = [int(r.strip()) for r in args.rounds.split(",")]
    result = scrape(args.year, rounds, args.outdir)
    print(json.dumps(result, indent=2))
    sys.exit(0 if result["status"] in ("ok", "warn") else 1)
#!/usr/bin/env python3
"""Regenerates the car model suggestions shown when a customer registers a vehicle.

Reads the public FIPE table (parallelum.com.br, no key) once and writes
web/src/presentation/pages/customer-dashboard/vehicle-models.json: a sorted list of
"Marca Modelo" names (versions dropped: "Onix HATCH LT 1.0" -> "Chevrolet Onix").
The app only reads the JSON, so FIPE is never called at runtime.

    python3 -I scripts/fipe-models.py
"""

import json
import pathlib
import re
import sys
import time
import urllib.request
from collections import Counter, defaultdict

API = "https://parallelum.com.br/fipe/api/v1/carros/marcas"
OUT = (
    pathlib.Path(__file__).resolve().parent.parent
    / "web/src/presentation/pages/customer-dashboard/vehicle-models.json"
)
# First words that are not a model on their own ("Grand Siena", "Range Rover").
PREFIXES = {"GRAND", "RANGE", "CLASSE", "NOVA", "NOVO", "NEW", "SPACE", "SANTA", "SERIE"}
BRAND_NAMES = {
    "GM - Chevrolet": "Chevrolet",
    "VW - VolksWagen": "Volkswagen",
    "Mercedes-Benz": "Mercedes-Benz",
    "Citroën": "Citroën",
}


def fetch(url):
    request = urllib.request.Request(url, headers={"User-Agent": "washflow-model-list"})
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def brand_name(raw):
    if raw in BRAND_NAMES:
        return BRAND_NAMES[raw]
    return raw if not raw.isupper() or len(raw) <= 3 else raw.title()


def pretty(token):
    # HB20, C3, 208 and short acronyms (UP!, HRV) stay as they are; LOUD words get title case.
    if any(ch.isdigit() for ch in token) or len(token) <= 3:
        return token.upper() if token.isupper() else token
    return token.title() if token.isupper() else token


def base_model(name):
    tokens = [t for t in re.split(r"\s+", name.strip()) if t]
    if not tokens:
        return None
    first = tokens[0].rstrip(",/")
    if first.upper() in PREFIXES and len(tokens) > 1:
        return f"{pretty(first)} {pretty(tokens[1].rstrip(',/'))}"
    return pretty(first)


def main():
    names = set()
    for brand in fetch(API):
        models = fetch(f"{API}/{brand['codigo']}/modelos")["modelos"]
        spellings = defaultdict(Counter)
        for model in models:
            base = base_model(model["nome"])
            # "Def." and "Range R.Evo" are FIPE abbreviations of a model already listed in full.
            if base and len(base) > 1 and not re.search(r"[./]", base):
                spellings[base.upper()][base] += 1
        for counter in spellings.values():
            # Prefer the spelling with lower case ("Uno" over "UNO"), then the most frequent.
            best = max(counter, key=lambda s: (not s.isupper(), counter[s]))
            names.add(f"{brand_name(brand['nome'])} {best}")
        time.sleep(0.2)
    result = sorted(names, key=str.casefold)
    OUT.write_text(json.dumps(result, ensure_ascii=False, indent=0) + "\n", encoding="utf-8")
    print(f"{len(result)} modelos -> {OUT}", file=sys.stderr)


if __name__ == "__main__":
    main()

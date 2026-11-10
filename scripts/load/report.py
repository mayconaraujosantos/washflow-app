#!/usr/bin/env python3
"""Resume um teste de carga: k6 (--summary-export) + amostras de `podman stats`.

Uso: report.py <perfil> <segundos-para-subir> <summary.json> <stats.jsonl> <descrição dos limites>
"""

import json
import re
import sys

ENDPOINTS = ["queue", "my-orders", "customer-orders", "yard", "report", "ticket",
             "entry", "start", "complete", "finalize"]


def number(text: str) -> float:
    match = re.match(r"([\d.]+)\s*([kMG]?i?B)?", text.strip())
    if not match:
        return 0.0
    value = float(match.group(1))
    scale = {"kB": 1e-3, "KiB": 1 / 1024, "MB": 1, "MiB": 1, "GB": 1e3, "GiB": 1024}
    return value * scale.get(match.group(2) or "MB", 1)


def samples(path: str):
    """`podman stats --format json` writes one pretty-printed array per sample, back to back."""
    text = open(path, encoding="utf-8").read()
    decoder = json.JSONDecoder()
    position = 0
    while True:
        while position < len(text) and text[position].isspace():
            position += 1
        if position >= len(text):
            return
        try:
            value, position = decoder.raw_decode(text, position)
        except json.JSONDecodeError:
            return
        yield from value if isinstance(value, list) else [value]


def stats(path: str):
    usage = {}
    for row in samples(path):
        name = row.get("name") or row.get("Name", "")
        cpu = float(str(row.get("cpu_percent", row.get("CPU", "0"))).rstrip("%") or 0)
        mem = number(str(row.get("mem_usage", row.get("MemUsage", "0"))).split("/")[0])
        usage.setdefault(name, []).append((cpu, mem))
    return usage


def main():
    profile, startup, summary_path, stats_path, limits = sys.argv[1:6]
    metrics = json.load(open(summary_path, encoding="utf-8"))["metrics"]
    duration = metrics["http_req_duration"]
    failed = metrics["http_req_failed"]
    rate = metrics["http_reqs"]["rate"]
    print(f"## {profile}\n")
    print(f"Limites: {limits}  \nAPI no ar em: {startup}s\n")
    print("| Métrica | Valor |\n|---|---|")
    print(f"| Requisições/s | {rate:.0f} |")
    received = metrics["data_received"]["rate"] / 1e6
    print(f"| Tráfego de saída da API | {received:.2f} MB/s (~{received * 36000 / 1e3:.0f} GB em 10 h) |")
    print(f"| Erros | {failed.get('value', failed.get('rate', 0)) * 100:.2f}% |")
    for stat in ["med", "p(95)", "p(99)", "max"]:
        print(f"| Latência {stat} | {duration[stat]:.0f} ms |")
    print("\n| Tela/ação | p95 | p99 |\n|---|---|---|")
    for name in ENDPOINTS:
        sub = metrics.get(f"http_req_duration{{name:{name}}}")
        if sub:
            print(f"| {name} | {sub['p(95)']:.0f} ms | {sub['p(99)']:.0f} ms |")
    print("\n| Contêiner | CPU média | CPU máx. | Memória máx. |\n|---|---|---|---|")
    for name, samples in sorted(stats(stats_path).items()):
        cpus = [cpu for cpu, _ in samples]
        mems = [mem for _, mem in samples]
        print(f"| {name} | {sum(cpus) / len(cpus):.0f}% | {max(cpus):.0f}% | {max(mems):.0f} MB |")
    print("\n(CPU em % de um núcleo.)")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Real-world scenarios against a running server: double clicks, simultaneous requests, retries.

Run on a database prepared by scripts/seed_demo_data.py (it uses the accounts from its CSV):

    python scripts/seed_demo_data.py --base http://localhost:7000 --csv qa-acessos.csv
    python scripts/probe_real_world.py --base http://localhost:7000 --csv qa-acessos.csv

Every scenario prints what was expected and what happened; the exit code is the number of
failures. It writes test data (plates DUP*/CAP*, a customer 92990000999, sales and cash entries)
- use a test database, never production. Standard library only.
"""
import argparse
import csv
import json
import sys
import threading
import urllib.error
import urllib.request
import uuid
from datetime import date, timedelta

sys.stdout.reconfigure(encoding="utf-8")


class Api:
    def __init__(self, base):
        self.base = base.rstrip("/")

    def call(self, method, path, body=None, token=None, shop=None, key=None):
        request = urllib.request.Request(
            self.base + path,
            method=method,
            data=json.dumps(body).encode() if body is not None else None,
        )
        request.add_header("Content-Type", "application/json")
        if token:
            request.add_header("Authorization", "Bearer " + token)
        if shop:
            request.add_header("X-Establishment", shop)
        if key:
            request.add_header("Idempotency-Key", key)
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                raw = response.read()
                return response.status, json.loads(raw) if raw else None, dict(response.headers)
        except urllib.error.HTTPError as error:
            raw = error.read()
            try:
                return error.code, json.loads(raw), dict(error.headers)
            except ValueError:
                return error.code, raw.decode(errors="replace"), dict(error.headers)


def parallel(n, fn):
    """Runs fn(i) for i in 0..n-1 at the same instant, one thread each."""
    out = [None] * n
    barrier = threading.Barrier(n)

    def run(i):
        barrier.wait()
        out[i] = fn(i)

    threads = [threading.Thread(target=run, args=(i,)) for i in range(n)]
    for thread in threads:
        thread.start()
    for thread in threads:
        thread.join()
    return out


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--base", default="http://localhost:7000")
    parser.add_argument("--csv", default="qa-acessos.csv")
    parser.add_argument("--shop", default="lava-rapido-tom")
    args = parser.parse_args()

    api = Api(args.base)
    rows = list(csv.DictReader(open(args.csv, encoding="utf-8")))
    shop_rows = [r for r in rows if r["slug"] == args.shop]

    def row(profile, method):
        return next(r for r in shop_rows if r["perfil"] == profile and r["metodo_login"] == method)

    manager = row("GERENTE", "email_senha")
    mt = api.call(
        "POST", "/api/auth/managers", {"email": manager["email"], "password": manager["senha"]}
    )[1]["token"]
    washer_rows = [r for r in shop_rows if r["perfil"] == "LAVADOR" and r["metodo_login"] == "telefone_senha"]
    washers = [
        api.call("POST", "/api/auth/washers", {"phone": w["telefone"], "password": w["senha"]})[1]
        for w in washer_rows[:2]
    ]
    services = api.call("GET", "/api/services", shop=args.shop)[1]
    svc = services[0]["id"]
    today = date.today().isoformat()
    results = []

    def report(name, expected, got, ok):
        results.append(ok)
        print(f"{'OK   ' if ok else 'FALHA'} {name}\n      esperado: {expected}\n      obtido:   {got}")

    def entry(plate, **extra):
        return api.call(
            "POST",
            "/api/operations/entries",
            {"plate": plate, "servicePriceId": svc, "vehicleModel": "Teste", **extra},
            token=mt,
        )

    w0 = washers[0]

    # 1. Double click on "Receber e finalizar"
    order = entry("DUP1A11", customerName="Teste Duplo")[1]
    api.call("PATCH", f"/api/service-orders/{order['id']}/start", {"washerId": w0["user"]["id"]}, token=w0["token"])
    api.call("PATCH", f"/api/service-orders/{order['id']}/complete", {}, token=w0["token"])
    statuses = parallel(
        2,
        lambda i: api.call(
            "PATCH", f"/api/service-orders/{order['id']}/finalize", {"paymentMethod": "PIX"}, token=mt
        )[0],
    )
    report("Finalizar 2x ao mesmo tempo (clique duplo)", "1 sucesso e 1 recusa (409)",
           f"status {sorted(statuses)}", sorted(statuses) == [200, 409])
    again = api.call("PATCH", f"/api/service-orders/{order['id']}/finalize", {"paymentMethod": "PIX"}, token=mt)[0]
    report("Finalizar de novo depois (reenvio)", "409", again, again == 409)

    # 2. Two washers start the same car at the same time
    if len(washers) > 1:
        order2 = entry("DUP2B22", customerName="Teste Corrida")[1]
        statuses = parallel(
            2,
            lambda i: api.call(
                "PATCH",
                f"/api/service-orders/{order2['id']}/start",
                {"washerId": washers[i]["user"]["id"]},
                token=washers[i]["token"],
            )[0],
        )
        report("Dois lavadores iniciam o mesmo carro ao mesmo tempo", "1 sucesso e 1 recusa (409)",
               f"status {sorted(statuses)}", sorted(statuses) == [200, 409])

    # 3. Same plate entered twice while the first is still in the yard
    first = entry("DUP3C33", customerName="Teste Placa")[0]
    second, body, _ = entry("DUP3C33")
    yard = api.call("GET", "/api/operations/yard", token=mt)[1]
    open_orders = sum(1 for x in yard if x["details"]["vehiclePlate"] == "DUP3C33")
    report("Mesma placa registrada 2x com o carro ainda no pátio", "segunda entrada recusada (409)",
           f"status {first},{second}; {open_orders} atendimento(s) aberto(s); {body.get('error') if isinstance(body, dict) else body}",
           second == 409 and open_orders == 1)

    # 4. Quick sale resent after a timeout, with the same Idempotency-Key (what the app does)
    sale = {"type": "QUICK_SALE", "customerName": "Retry", "items": [{"servicePriceId": svc, "quantity": 1}],
            "discount": 0, "payments": [{"method": "CASH", "amount": 10}]}
    key = f"probe-{uuid.uuid4()}"
    before = len(api.call("GET", f"/api/sales?from={today}&to={today}", token=mt)[1])
    a = api.call("POST", "/api/sales", sale, token=mt, key=key)
    b = api.call("POST", "/api/sales", sale, token=mt, key=key)
    after = len(api.call("GET", f"/api/sales?from={today}&to={today}", token=mt)[1])
    same = isinstance(a[1], dict) and isinstance(b[1], dict) and a[1].get("id") == b[1].get("id")
    report("Venda rápida reenviada com a mesma Idempotency-Key", "1 venda; o reenvio devolve a mesma",
           f"{after - before} venda(s); mesma resposta: {same}; replay: {b[2].get('Idempotent-Replayed')}",
           after - before == 1 and same)

    # 5. Same key sent twice at the same instant (double click that escaped the app)
    key = f"probe-{uuid.uuid4()}"
    before = len(api.call("GET", f"/api/sales?from={today}&to={today}", token=mt)[1])
    statuses = parallel(2, lambda i: api.call("POST", "/api/sales", sale, token=mt, key=key)[0])
    after = len(api.call("GET", f"/api/sales?from={today}&to={today}", token=mt)[1])
    report("Venda com a mesma chave enviada 2x ao mesmo tempo", "1 venda",
           f"{after - before} venda(s); status {sorted(statuses)}", after - before == 1)

    # 6. Cash entry resent with the same key
    categories = api.call("GET", "/api/expense-categories", token=mt)[1] or []
    if categories:
        cash = {"type": "EXPENSE", "description": "Probe retry", "amount": 12.5,
                "categoryId": categories[0]["id"], "referenceDate": today}
        key = f"probe-{uuid.uuid4()}"
        a = api.call("POST", "/api/cash-transactions", cash, token=mt, key=key)
        b = api.call("POST", "/api/cash-transactions", cash, token=mt, key=key)
        ok = a[0] < 300 and b[0] == a[0] and a[1] == b[1]
        report("Lançamento de caixa reenviado com a mesma chave", "1 lançamento; mesma resposta",
               f"status {a[0]},{b[0]}; replay: {b[2].get('Idempotent-Replayed')}", ok)

    # 7. Last slot booked by many people at the same time
    slot = None
    for d in range(1, 8):
        day = (date.today() + timedelta(days=d)).isoformat()
        availability = api.call("GET", f"/api/schedule/availability?date={day}", shop=args.shop)[1]
        free = [s for s in availability["slots"] if s["available"] and s["booked"] == 0]
        if free:
            slot = free[-1]
            break
    capacity = slot["capacity"]
    plates = [f"CAP{i}D{i}{i}" for i in range(capacity + 3)]
    statuses = parallel(
        len(plates),
        lambda i: entry(plates[i], customerName=f"Cap {i}", scheduledAt=slot["startsAt"])[0],
    )
    booked_now = api.call("GET", f"/api/schedule/availability?date={day}", shop=args.shop)[1]
    booked = next(s for s in booked_now["slots"] if s["startsAt"] == slot["startsAt"])["booked"]
    report(f"{len(plates)} agendamentos simultâneos num horário de capacidade {capacity}",
           f"{capacity} agendados, o resto recusado (409)",
           f"{booked} agendados (status {sorted(statuses)})",
           booked == capacity and statuses.count(409) == len(plates) - capacity)

    # 8. Customer sign-up sent twice at the same time
    statuses = parallel(
        2,
        lambda i: api.call("POST", "/api/auth/customers",
                           {"name": "Duplo", "phone": "92990000999", "password": "Cliente@123"},
                           shop=args.shop)[0],
    )
    report("Cadastro de cliente enviado 2x ao mesmo tempo", "as duas entram na mesma conta (sem 500)",
           f"status {sorted(statuses)}", sorted(statuses) == [200, 200])

    # 9. Ticket issued twice -> same link
    t1 = api.call("POST", f"/api/service-orders/{order['id']}/ticket", token=mt)[1]["token"]
    t2 = api.call("POST", f"/api/service-orders/{order['id']}/ticket", token=mt)[1]["token"]
    report("Gerar ticket 2x", "mesmo link", "iguais" if t1 == t2 else "diferentes", t1 == t2)

    failures = results.count(False)
    print(f"\n{len(results) - failures}/{len(results)} cenários com comportamento correto")
    sys.exit(failures)


if __name__ == "__main__":
    main()

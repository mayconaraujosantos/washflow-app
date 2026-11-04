#!/usr/bin/env python3
"""Populates a local Washflow with fake (but realistic) data through the API.

Everything goes through the same endpoints the app uses, so running it is also a
smoke test of the whole flow: platform setup, two lava-jatos with their managers,
washers with badges, customers with cars, service orders in every status, a
scheduled visit, a ticket, a quick sale, a budget and cash expenses.

Usage (server running, e.g. ./gradlew run):

    python scripts/seed_demo_data.py                       # http://localhost:7000
    python scripts/seed_demo_data.py --base http://localhost:7000 \
        --admin-email admin@washflow.dev --admin-password Admin@12345

Re-running is safe: lava-jatos, accounts and cars that already exist are reused;
service orders, sales and cash entries are only created for a lava-jato created
in this run. Badge PINs are re-issued (the old PIN stops working) and printed.

Dev data only - never point this at production.
"""

from __future__ import annotations

import argparse
import csv
import json
import sys
import urllib.error
import urllib.request
from datetime import date, timedelta

ADMIN_NAME = "Admin Washflow"
STAFF_PASSWORD = "Gerente@123"
WASHER_PASSWORD = "Lavador@123"
CUSTOMER_PASSWORD = "Cliente@123"

SHOPS = [
    {
        "name": "Lava Rápido Tom",
        "hours": (7, 19, False, 4),
        "manager": ("Tomás Andrade", "tom@lavarapidotom.com.br", "92991001000"),
        "washers": [("João Pereira", "92991001101"), ("Marcos Silva", "92991001102")],
        "customers": [
            ("Ana Beatriz Souza", "92992002001", [("ABC1D23", "Onix", "Prata")]),
            (
                "Carlos Eduardo Lima",
                "92992002002",
                [("QWE4R56", "HB20", "Branco"), ("RTY2U34", "Corolla", "Cinza")],
            ),
            ("Fernanda Rocha", "92992002003", [("JKL7M89", "Compass", "Preto")]),
        ],
        "walk_ins": [
            ("HGF8J90", "Gol", "Azul", "Roberto Dias", "92995005001"),
            ("ZXC3V45", "Fit", "Prata", "Luciana Prado", "92995005002"),
        ],
    },
    {
        "name": "Brilho Auto Center",
        "hours": (8, 18, True, 3),
        "manager": ("Patrícia Gomes", "patricia@brilhoauto.com.br", "92993003000"),
        "washers": [("Rafael Costa", "92993003101")],
        "customers": [("Juliana Martins", "92994004001", [("MNO5P67", "Kicks", "Vermelho")])],
        "walk_ins": [("BNM6K12", "Strada", "Branca", "Diego Alves", "92995005003")],
    },
]


class Api:
    def __init__(self, base: str) -> None:
        self.base = base.rstrip("/")

    def call(self, method, path, body=None, token=None, shop=None):
        request = urllib.request.Request(self.base + path, method=method)
        request.add_header("Content-Type", "application/json")
        if token:
            request.add_header("Authorization", "Bearer " + token)
        if shop:
            request.add_header("X-Establishment", shop)
        data = json.dumps(body).encode() if body is not None else None
        try:
            with urllib.request.urlopen(request, data, timeout=30) as response:
                raw = response.read()
                return response.status, json.loads(raw) if raw else None
        except urllib.error.HTTPError as error:
            raw = error.read()
            try:
                return error.code, json.loads(raw)
            except ValueError:
                return error.code, {"error": raw.decode(errors="replace")}
        except urllib.error.URLError as error:
            sys.exit(f"Servidor indisponível em {self.base}: {error.reason}")

    def ok(self, label, method, path, body=None, token=None, shop=None):
        status, payload = self.call(method, path, body, token, shop)
        if status >= 300:
            sys.exit(f"Falhou: {label} -> {status} {payload}")
        return payload


def step(text):
    print(f"  - {text}")


def admin_session(api: Api, email: str, password: str, setup_code: str | None) -> dict:
    status = api.ok("status da plataforma", "GET", "/api/setup")
    if not status["configured"]:
        session = api.ok(
            "criar administrador",
            "POST",
            "/api/setup",
            {
                "ownerName": ADMIN_NAME,
                "ownerEmail": email,
                "ownerPassword": password,
                "setupCode": setup_code,
            },
        )
        step(f"plataforma configurada; administrador {email}")
        return session
    code, session = api.call("POST", "/api/auth/admins", {"email": email, "password": password})
    if code != 200:
        sys.exit(
            "A plataforma já tem administrador e o login falhou. Informe o e-mail e a senha dele "
            "com --admin-email/--admin-password."
        )
    step(f"administrador {email} já existia; login feito")
    return session


def ensure_shop(api: Api, admin: str, shop: dict) -> tuple[str, bool]:
    opening, closing, sunday, capacity = shop["hours"]
    manager_name, manager_email, manager_phone = shop["manager"]
    code, created = api.call(
        "POST",
        "/api/admin/establishments",
        {
            "name": shop["name"],
            "openingHour": opening,
            "closingHour": closing,
            "openOnSunday": sunday,
            "maxVehiclesPerHour": capacity,
            "managerName": manager_name,
            "managerEmail": manager_email,
            "managerPhone": manager_phone,
            "managerPassword": STAFF_PASSWORD,
        },
        token=admin,
    )
    if code == 200:
        step(f"lava-jato criado: {created['name']} (/?lavajato={created['slug']})")
        return created["slug"], True
    listed = api.ok("listar lava-jatos", "GET", "/api/admin/establishments", token=admin)
    for item in listed:
        if item["name"] == shop["name"]:
            if not item["active"]:
                api.ok(
                    "reativar lava-jato",
                    "PATCH",
                    f"/api/admin/establishments/{item['id']}/active",
                    {"active": True},
                    token=admin,
                )
            step(f"lava-jato já existia: {item['name']} (/?lavajato={item['slug']})")
            return item["slug"], False
    sys.exit(f"Não foi possível criar {shop['name']}: {created}")


def seed_shop(api: Api, shop: dict, slug: str, fresh: bool) -> dict:
    manager_email = shop["manager"][1]
    manager = api.ok(
        "login do gerente",
        "POST",
        "/api/auth/managers",
        {"email": manager_email, "password": STAFF_PASSWORD},
    )
    mtoken = manager["token"]
    manager_id = manager["user"]["id"]
    report = {"slug": slug, "manager": manager_email, "washers": [], "customers": []}

    # Washers + badges ------------------------------------------------------------
    users = api.ok("listar equipe", "GET", "/api/users", token=mtoken)
    by_phone = {u.get("phone"): u for u in users}
    washers = {}
    for name, phone in shop["washers"]:
        user = by_phone.get(phone)
        if user is None:
            user = api.ok(
                "cadastrar lavador",
                "POST",
                "/api/users",
                {"name": name, "phone": phone, "password": WASHER_PASSWORD},
                token=mtoken,
            )
        badge = api.ok("emitir crachá", "POST", f"/api/users/{user['id']}/badge", token=mtoken)
        session = api.ok(
            "login do lavador por PIN",
            "POST",
            "/api/auth/washers/pin",
            {"phone": phone, "pin": badge["pin"]},
        )
        washers[name] = {"id": user["id"], "token": session["token"]}
        report["washers"].append((name, phone, badge["pin"], badge["badgeToken"]))
    step(f"{len(washers)} lavador(es) com crachá")

    # Customers + cars ------------------------------------------------------------
    customers = {}
    for name, phone, cars in shop["customers"]:
        code, session = api.call(
            "POST",
            "/api/auth/customers",
            {"name": name, "phone": phone, "password": CUSTOMER_PASSWORD},
            shop=slug,
        )
        if code != 200:
            session = api.ok(
                "login do cliente",
                "POST",
                "/api/auth/customers",
                {"phone": phone, "password": CUSTOMER_PASSWORD},
                shop=slug,
            )
        ctoken, cid = session["token"], session["user"]["id"]
        owned = api.ok("veículos do cliente", "GET", f"/api/customers/{cid}/vehicles", token=ctoken)
        plates = {v["plate"]: v for v in owned}
        vehicles = []
        for plate, model, color in cars:
            vehicle = plates.get(plate) or api.ok(
                "cadastrar veículo",
                "POST",
                "/api/vehicles",
                {"customerId": cid, "plate": plate, "model": model, "color": color},
                token=ctoken,
            )
            vehicles.append(vehicle)
        customers[name] = {"id": cid, "token": ctoken, "phone": phone, "vehicles": vehicles}
        report["customers"].append((name, phone, [c[0] for c in cars]))
    step(f"{len(customers)} cliente(s) com veículos")

    if not fresh:
        step("atendimentos, vendas e caixa já foram gerados antes - mantidos")
        walk_in = shop["walk_ins"][0][0]
        yard = api.ok("pátio", "GET", "/api/operations/yard", token=mtoken)
        for order in yard:
            if order["details"]["vehiclePlate"] == walk_in:
                ticket = api.ok(
                    "ticket", "POST", f"/api/service-orders/{order['id']}/ticket", token=mtoken
                )
                report["ticket"] = f"/ticket/{ticket['token']}"
                break
        return report

    services = api.ok("catálogo", "GET", "/api/services", shop=slug)
    service = {s["name"]: s["id"] for s in services}
    simple = service.get("Lavagem Simples", services[0]["id"])
    complete = service.get("Lavagem Completa", services[0]["id"])
    premium = service.get("Lavagem Premium", services[0]["id"])

    def entry(plate, service_id, **extra):
        body = {"plate": plate, "servicePriceId": service_id, **extra}
        return api.ok("registrar entrada", "POST", "/api/operations/entries", body, token=mtoken)

    def wash(order, washer):
        api.ok(
            "iniciar lavagem",
            "PATCH",
            f"/api/service-orders/{order['id']}/start",
            {"washerId": washer["id"]},
            token=washer["token"],
        )

    def finish(order, washer):
        api.ok(
            "concluir lavagem",
            "PATCH",
            f"/api/service-orders/{order['id']}/complete",
            {},
            token=washer["token"],
        )

    first_washer = next(iter(washers.values()))
    second_washer = list(washers.values())[-1]
    cars = [v for c in customers.values() for v in c["vehicles"]]

    # FINALIZADO: washed, ready and paid at the counter.
    paid = entry(cars[-1]["plate"], premium, notes="Cliente pediu atenção às rodas")
    wash(paid, first_washer)
    finish(paid, first_washer)
    api.ok(
        "finalizar com pagamento",
        "PATCH",
        f"/api/service-orders/{paid['id']}/finalize",
        {"paymentMethod": "PIX"},
        token=mtoken,
    )
    counts = {"FINALIZADO": 1}

    # PRONTO: waiting for the customer to pick it up.
    if len(cars) > 1:
        ready = entry(cars[1]["plate"], complete)
        wash(ready, first_washer)
        finish(ready, first_washer)
        counts["PRONTO"] = 1

    # EM_LAVAGEM.
    washing = entry(cars[0]["plate"], simple)
    wash(washing, first_washer)
    counts["EM_LAVAGEM"] = 1

    # PAUSADA (another washer, lunch break).
    if len(cars) > 2 and second_washer is not first_washer:
        paused = entry(cars[2]["plate"], complete)
        wash(paused, second_washer)
        api.ok(
            "pausar lavagem",
            "PATCH",
            f"/api/service-orders/{paused['id']}/pause",
            {"reason": "LUNCH"},
            token=second_washer["token"],
        )
        counts["PAUSADA"] = 1

    # AGUARDANDO_PATIO: walk-in without an account, with a ticket link.
    plate, model, color, name, phone = shop["walk_ins"][0]
    waiting = entry(
        plate,
        simple,
        vehicleModel=model,
        vehicleColor=color,
        customerName=name,
        customerPhone=phone,
    )
    ticket = api.ok("gerar ticket", "POST", f"/api/service-orders/{waiting['id']}/ticket", token=mtoken)
    report["ticket"] = f"/ticket/{ticket['token']}"
    counts["AGUARDANDO_PATIO"] = 1

    # AGENDADO: one booked by a customer in the app, one by the manager.
    slots = free_slots(api, slug)
    if slots:
        customer = next(iter(customers.values()))
        api.ok(
            "cliente agenda",
            "POST",
            "/api/service-orders",
            {
                "vehicleId": customer["vehicles"][0]["id"],
                "servicePriceId": complete,
                "scheduledAt": slots[0],
            },
            token=customer["token"],
        )
        counts["AGENDADO"] = 1
        if len(shop["walk_ins"]) > 1 and len(slots) > 1:
            plate, model, color, name, phone = shop["walk_ins"][1]
            entry(
                plate,
                premium,
                vehicleModel=model,
                vehicleColor=color,
                customerName=name,
                customerPhone=phone,
                scheduledAt=slots[1],
            )
            counts["AGENDADO"] += 1

    # Counter: quick sale (paid) and a budget.
    api.ok(
        "venda rápida",
        "POST",
        "/api/sales",
        {
            "type": "QUICK_SALE",
            "customerName": "Cliente de balcão",
            "items": [
                {"servicePriceId": service.get("Lavagem Externa", simple), "quantity": 1},
                {"servicePriceId": service.get("Lavagem Interna", simple), "quantity": 1},
            ],
            "discount": 5,
            "payments": [{"method": "CASH", "amount": 50}],
            "createdBy": manager_id,
        },
        token=mtoken,
    )
    api.ok(
        "orçamento",
        "POST",
        "/api/sales",
        {
            "type": "BUDGET",
            "customerName": "Mariana Teixeira",
            "customerPhone": "92996006001",
            "items": [{"servicePriceId": service.get("Polimento", premium), "quantity": 1}],
            "discount": 0,
            "payments": [],
            "notes": "Validade de 7 dias",
            "createdBy": manager_id,
        },
        token=mtoken,
    )

    # Cash register: two expenses this month.
    categories = api.ok("categorias de despesa", "GET", "/api/expense-categories", token=mtoken)
    by_name = {c["name"]: c["id"] for c in categories}
    today = date.today().isoformat()
    for category, amount, description in [
        ("Produtos", "150.00", "Shampoo automotivo e cera"),
        ("Energia", "280.00", "Conta de luz"),
    ]:
        if category in by_name:
            api.ok(
                "lançar despesa",
                "POST",
                "/api/cash-transactions",
                {
                    "type": "EXPENSE",
                    "amount": amount,
                    "categoryId": by_name[category],
                    "description": description,
                    "referenceDate": today,
                    "createdBy": manager_id,
                },
                token=mtoken,
            )

    step("atendimentos: " + ", ".join(f"{k} {v}" for k, v in counts.items()))
    step("1 venda rápida, 1 orçamento e 2 despesas no caixa")
    return report


def free_slots(api: Api, slug: str) -> list[str]:
    """Next bookable hours (ISO instants) from tomorrow on."""
    found = []
    for offset in range(1, 8):
        day = (date.today() + timedelta(days=offset)).isoformat()
        payload = api.ok("agenda", "GET", f"/api/schedule/availability?date={day}", shop=slug)
        found += [s["startsAt"] for s in payload.get("slots", []) if s.get("available")]
        if len(found) >= 2:
            break
    return found[:2]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--base", default="http://localhost:7000")
    parser.add_argument("--admin-email", default="admin@washflow.dev")
    parser.add_argument("--admin-password", default="Admin@12345")
    parser.add_argument("--setup-code", default=None, help="SETUP_CODE, if the server requires it")
    parser.add_argument("--csv", default=None, help="also write every login to this CSV file")
    args = parser.parse_args()
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")  # accents on the Windows console

    api = Api(args.base)
    print(f"Populando {api.base}")
    session = admin_session(api, args.admin_email, args.admin_password, args.setup_code)
    admin = session["token"]
    reports = []
    for shop in SHOPS:
        print(f"\n{shop['name']}")
        slug, fresh = ensure_shop(api, admin, shop)
        reports.append((shop, seed_shop(api, shop, slug, fresh)))

    print("\n" + "=" * 72)
    print("ACESSOS DE TESTE")
    print("=" * 72)
    print(f"Administrador  {api.base}/login/admin")
    print(f"  {args.admin_email} / {args.admin_password}")
    for shop, report in reports:
        print(f"\n{shop['name']}  -  clientes: {api.base}/?lavajato={report['slug']}")
        print(f"  Gerente   {api.base}/login/gerente   {report['manager']} / {STAFF_PASSWORD}")
        for name, phone, pin, _badge in report["washers"]:
            print(f"  Lavador   {name:<20} tel {phone}  PIN {pin}  (senha {WASHER_PASSWORD})")
        for name, phone, plates in report["customers"]:
            print(f"  Cliente   {name:<20} tel {phone}  senha {CUSTOMER_PASSWORD}  {', '.join(plates)}")
        if report.get("ticket"):
            print(f"  Ticket    {api.base}{report['ticket']}")
    if args.csv:
        write_csv(
            args.csv,
            api.base,
            (session["user"]["name"], args.admin_email, args.admin_password),
            reports,
        )
        print(f"\nCSV: {args.csv}")


CSV_COLUMNS = [
    "lava_jato",
    "slug",
    "perfil",
    "nome",
    "metodo_login",
    "url_login",
    "api_login",
    "header_x_establishment",
    "email",
    "telefone",
    "senha",
    "pin",
    "veiculos",
    "observacao",
]


def write_csv(path, base, admin, reports):
    """One row per way of signing in, for test automation (UTF-8, comma separated)."""
    admin_name, admin_email, admin_password = admin
    rows = [
        {
            "perfil": "ADMIN",
            "nome": admin_name,
            "metodo_login": "email_senha",
            "url_login": f"{base}/login/admin",
            "api_login": "POST /api/auth/admins {email,password}",
            "email": admin_email,
            "senha": admin_password,
            "observacao": "Administrador da plataforma: cria e suspende lava-jatos em /admin",
        }
    ]
    for shop, report in reports:
        common = {"lava_jato": shop["name"], "slug": report["slug"]}
        manager_name, manager_email, manager_phone = shop["manager"]
        rows.append(
            {
                **common,
                "perfil": "GERENTE",
                "nome": manager_name,
                "metodo_login": "email_senha",
                "url_login": f"{base}/login/gerente",
                "api_login": "POST /api/auth/managers {email,password}",
                "email": manager_email,
                "telefone": manager_phone,
                "senha": STAFF_PASSWORD,
            }
        )
        for name, phone, pin, badge in report["washers"]:
            washer = {**common, "perfil": "LAVADOR", "nome": name, "telefone": phone}
            rows.append(
                {
                    **washer,
                    "metodo_login": "telefone_pin",
                    "url_login": f"{base}/login/lavador",
                    "api_login": "POST /api/auth/washers/pin {phone,pin}",
                    "pin": pin,
                    "observacao": "PIN muda a cada execução do seed; 5 erros bloqueiam por 15 min",
                }
            )
            rows.append(
                {
                    **washer,
                    "metodo_login": "telefone_senha",
                    "url_login": f"{base}/login/lavador",
                    "api_login": "POST /api/auth/washers {phone,password}",
                    "senha": WASHER_PASSWORD,
                }
            )
            rows.append(
                {
                    **washer,
                    "metodo_login": "qr_cracha",
                    "url_login": f"{base}/login/lavador?cracha={badge}",
                    "api_login": "POST /api/auth/washers/badge {badgeToken}",
                    "observacao": "URL do QR do crachá; muda a cada execução do seed",
                }
            )
        plates_by_customer = {name: plates for name, _phone, plates in report["customers"]}
        for name, phone, _cars in shop["customers"]:
            rows.append(
                {
                    **common,
                    "perfil": "CLIENTE",
                    "nome": name,
                    "metodo_login": "telefone_senha",
                    "url_login": f"{base}/?lavajato={report['slug']}",
                    "api_login": "POST /api/auth/customers {phone,password}",
                    "header_x_establishment": report["slug"],
                    "telefone": phone,
                    "senha": CUSTOMER_PASSWORD,
                    "veiculos": " ".join(plates_by_customer.get(name, [])),
                }
            )
        if report.get("ticket"):
            rows.append(
                {
                    **common,
                    "perfil": "PUBLICO",
                    "nome": "Ticket do atendimento (sem login)",
                    "metodo_login": "link",
                    "url_login": f"{base}{report['ticket']}",
                    "api_login": f"GET /api/tickets/{report['ticket'].rsplit('/', 1)[-1]}",
                    "veiculos": shop["walk_ins"][0][0],
                    "observacao": "Mostra veículo, serviço e andamento, sem dados do cliente",
                }
            )
    with open(path, "w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=CSV_COLUMNS, restval="")
        writer.writeheader()
        writer.writerows(rows)


if __name__ == "__main__":
    main()

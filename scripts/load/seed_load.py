#!/usr/bin/env python3
"""Cria a massa do teste de carga e grava as sessões em tokens.json.

Roda contra uma API sem limite de CPU (o BCrypt dos logins é caro) e um banco vazio:
  - N lava-jatos, cada um com gerente, lavadores (login por telefone e senha) e clientes;
  - clientes e veículos inseridos direto no banco (mesmo hash de senha), só os "ativos" entram
    pela API para ter sessão;
  - um carro de cada cliente ativo no pátio (AGUARDANDO_PATIO), com ticket para alguns;
  - histórico de atendimentos finalizados com pagamento e comissão, para as consultas
    trabalharem com volume real.

Uso: python3 -I scripts/load/seed_load.py --api http://localhost:7100 --db-container NOME
"""

import argparse
import json
import subprocess
import sys
import urllib.error
import urllib.request

PASSWORD = "carga-senha-1"


class Api:
    def __init__(self, base: str):
        self.base = base.rstrip("/")

    def call(self, method, path, body=None, token=None, shop=None):
        headers = {"Content-Type": "application/json"}
        if token:
            headers["Authorization"] = f"Bearer {token}"
        if shop:
            headers["X-Establishment"] = shop
        data = json.dumps(body).encode() if body is not None else None
        request = urllib.request.Request(self.base + path, data, headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                return response.status, json.loads(response.read() or b"null")
        except urllib.error.HTTPError as error:
            return error.code, json.loads(error.read() or b"null")

    def ok(self, what, method, path, body=None, token=None, shop=None):
        status, payload = self.call(method, path, body, token, shop)
        if status >= 300:
            sys.exit(f"falhou ao {what}: {status} {payload}")
        return payload


def psql(container: str, sql: str) -> str:
    result = subprocess.run(
        ["podman", "exec", "-i", container, "psql", "-U", "postgres", "-d", "washflow",
         "-v", "ON_ERROR_STOP=1", "-tA"],
        input=sql, capture_output=True, text=True, check=False,
    )
    if result.returncode != 0:
        sys.exit(f"falhou no banco: {result.stderr}")
    return result.stdout.strip()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--api", default="http://localhost:7100")
    parser.add_argument("--db-container", default="washflow-load-db")
    parser.add_argument("--setup-code", default="carga")
    parser.add_argument("--out", default="scripts/load/out/tokens.json")
    parser.add_argument("--shops", type=int, default=20)
    parser.add_argument("--washers", type=int, default=10)
    parser.add_argument("--customers", type=int, default=200)
    parser.add_argument("--active-customers", type=int, default=10)
    parser.add_argument("--tickets", type=int, default=5)
    parser.add_argument("--history-days", type=int, default=180)
    parser.add_argument("--orders-per-day", type=int, default=30)
    args = parser.parse_args()
    api = Api(args.api)

    admin = api.ok("configurar a plataforma", "POST", "/api/setup", {
        "ownerName": "Admin Carga", "ownerEmail": "admin@carga.test",
        "ownerPassword": PASSWORD, "setupCode": args.setup_code,
    })
    shops = []
    for i in range(1, args.shops + 1):
        slug = f"lava-{i:02d}"
        api.ok("criar lava-jato", "POST", "/api/admin/establishments", {
            "name": f"Lava Jato {i:02d}", "slug": slug, "openingHour": 7, "closingHour": 19,
            "maxVehiclesPerHour": 6, "managerName": f"Gerente {i:02d}",
            "managerEmail": f"gerente{i:02d}@carga.test", "managerPhone": f"9299{i:02d}00000",
            "managerPassword": PASSWORD,
        }, admin["token"])
        manager = api.ok("entrar como gerente", "POST", "/api/auth/managers",
                         {"email": f"gerente{i:02d}@carga.test", "password": PASSWORD})
        service_id = api.ok("ler o catálogo", "GET", "/api/services", shop=slug)[0]["id"]
        washers = []
        for w in range(1, args.washers + 1):
            phone = f"9297{i:02d}{w:05d}"
            api.ok("cadastrar lavador", "POST", "/api/users",
                   {"name": f"Lavador {i:02d}-{w:02d}", "phone": phone, "password": PASSWORD},
                   manager["token"])
            session = api.ok("entrar como lavador", "POST", "/api/auth/washers",
                             {"phone": phone, "password": PASSWORD})
            washers.append({"id": session["user"]["id"], "token": session["token"]})
        shops.append({"slug": slug, "serviceId": service_id, "managerToken": manager["token"],
                      "washers": washers, "customers": [], "tickets": []})
        print(f"  {slug}: gerente e {len(washers)} lavadores", flush=True)

    # Everyone shares PASSWORD, so customers reuse a manager's BCrypt hash (no hashing per row).
    psql(args.db_container, f"""
        INSERT INTO usuarios (id, estabelecimento_id, nome, telefone, senha_hash, perfil)
        SELECT gen_random_uuid(), e.id, 'Cliente ' || substr(e.slug, 6) || '-' || lpad(n::text, 5, '0'),
               '9298' || substr(e.slug, 6) || lpad(n::text, 5, '0'),
               (SELECT senha_hash FROM usuarios WHERE perfil = 'GERENTE' LIMIT 1), 'CLIENTE'
        FROM estabelecimentos e, generate_series(1, {args.customers}) n;
        INSERT INTO veiculos (id, estabelecimento_id, cliente_id, placa, modelo, cor)
        SELECT gen_random_uuid(), u.estabelecimento_id, u.id,
               'CAR' || right(u.telefone, 4), 'Onix', 'Prata'
        FROM usuarios u WHERE u.perfil = 'CLIENTE';
    """)
    print(f"  {args.shops * args.customers} clientes e veículos no banco", flush=True)

    for shop in shops:
        index = shop["slug"][5:]
        for n in range(1, args.active_customers + 1):
            phone = f"9298{index}{n:05d}"
            session = api.ok("entrar como cliente", "POST", "/api/auth/customers",
                             {"phone": phone, "password": PASSWORD}, shop=shop["slug"])
            order = api.ok("registrar entrada", "POST", "/api/operations/entries",
                           {"plate": f"CAR{n:04d}", "servicePriceId": shop["serviceId"]},
                           shop["managerToken"])
            shop["customers"].append({"id": session["user"]["id"], "token": session["token"]})
            if n <= args.tickets:
                ticket = api.ok("gerar ticket", "POST",
                                f"/api/service-orders/{order['id']}/ticket",
                                token=shop["managerToken"])
                shop["tickets"].append(ticket["token"])
    print(f"  {args.active_customers} clientes ativos com carro no pátio por lava-jato", flush=True)

    total = args.history_days * args.orders_per_day
    psql(args.db_container, f"""
        CREATE TEMP TABLE historico AS
        SELECT gen_random_uuid() AS id, e.id AS estabelecimento_id,
               (SELECT v.id FROM veiculos v WHERE v.estabelecimento_id = e.id
                ORDER BY v.placa OFFSET (g.n % {args.customers}) LIMIT 1) AS veiculo_id,
               (SELECT s.id FROM servicos_preco s WHERE s.estabelecimento_id = e.id
                ORDER BY s.id OFFSET (g.n % 3) LIMIT 1) AS servico_id,
               (SELECT u.id FROM usuarios u WHERE u.estabelecimento_id = e.id
                  AND u.perfil = 'LAVADOR' ORDER BY u.telefone
                OFFSET (g.n % {args.washers}) LIMIT 1) AS lavador_id,
               now() - interval '1 day' - g.n * interval '1 day' / {args.orders_per_day} AS t
        FROM estabelecimentos e, generate_series(1, {total}) g(n);
        INSERT INTO atendimentos (id, estabelecimento_id, veiculo_id, servico_preco_id, lavador_id,
          status, data_agendamento, criado_em, atualizado_em, lavagem_iniciada_em,
          lavagem_concluida_em)
        SELECT id, estabelecimento_id, veiculo_id, servico_id, lavador_id, 'FINALIZADO', t, t,
               t + interval '1 hour', t + interval '10 minutes', t + interval '50 minutes'
        FROM historico;
        INSERT INTO pagamentos (id, atendimento_id, forma_pagamento, valor, recebido_em)
        SELECT gen_random_uuid(), h.id, 'PIX', s.preco, h.t + interval '1 hour'
        FROM historico h JOIN servicos_preco s ON s.id = h.servico_id;
        INSERT INTO comissoes_lavador (id, atendimento_id, lavador_id, valor, criado_em)
        SELECT gen_random_uuid(), h.id, h.lavador_id, COALESCE(s.comissao_lavador, 0),
               h.t + interval '1 hour'
        FROM historico h JOIN servicos_preco s ON s.id = h.servico_id;
        ANALYZE;
    """)
    print(f"  {args.shops * total} atendimentos de histórico ({args.history_days} dias)", flush=True)

    with open(args.out, "w", encoding="utf-8") as out:
        json.dump({"shops": shops}, out)
    print(f"sessões gravadas em {args.out}")


if __name__ == "__main__":
    main()

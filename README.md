# washflow-app

Fluxo de gerenciamento de lava-jatos: uma instalação atende vários lava-jatos, cada um com equipe (gerente e lavadores), clientes, agenda, pátio, vendas e caixa.

## Documentação

| Onde | O quê |
|---|---|
| [docs/system-design/](docs/system-design/README.md) | Arquitetura, multi-tenant, modelo de dados, máquina de estados, segurança, fluxos, confiabilidade e API |
| [CLAUDE.md](CLAUDE.md) | Dicionário de dados e regras de negócio (referência curta) |
| [infra/README.md](infra/README.md) | Ambiente local, banco, variáveis, observabilidade, CI/CD |
| [docs/testes-mundo-real.md](docs/testes-mundo-real.md) | Cenários de concorrência e reenvio e resultados |
| [diagram-usecase-washflow-system.mermaid](diagram-usecase-washflow-system.mermaid) | Fluxo de uso em uma página |
| [spec.md](spec.md) | Funcionalidades entregues e backlog |
| `/swagger` (com o app rodando) | API navegável |

## Rodando localmente

```
make db-up                       # PostgreSQL (Podman/Docker)
cp .env.example .env             # ajuste DB_HOST se necessário
make run                         # app + API em http://localhost:7000
python scripts/seed_demo_data.py --csv qa-acessos-teste.csv   # dados de demonstração e acessos
```

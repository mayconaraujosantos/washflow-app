# Cenários do mundo real: concorrência, reenvio e idempotência

Registro dos cenários testados contra o servidor rodando e das correções feitas.
Para repetir, use um banco de teste e rode:

```
python scripts/seed_demo_data.py --base http://localhost:7000 --csv qa-acessos.csv
python scripts/probe_real_world.py --base http://localhost:7000 --csv qa-acessos.csv
```

O script cria dados de teste: placas `DUP*`/`CAP*`, o cliente `92990000999`, vendas e lançamentos de caixa. Nunca rode em produção.

## Resultado

| # | Cenário | Antes (04/11/2026) | Depois | Correção |
|---|---|---|---|---|
| 1 | "Receber e finalizar" com clique duplo | ✅ 1 pagamento | ✅ 200 + 409 | A finalização já era condicional. A segunda resposta agora é sempre 409; antes, dependendo do momento, podia ser 500 |
| 2 | Finalizar de novo depois | ✅ 409 | ✅ 409 | — |
| 3 | Dois lavadores iniciam o mesmo carro ao mesmo tempo | ❌ os dois recebiam 200 e o último sobrescrevia o lavador | ✅ 200 + 409 | Transição de status com compare-and-set (`UPDATE ... WHERE status = :expected`) em iniciar, concluir e receber |
| 4 | Mesma placa registrada de novo com o carro no pátio | ❌ dois atendimentos abertos | ✅ 409 "Este veículo já está no pátio" | Índice único `ux_atendimentos_veiculo_no_patio` (migração V3) + `VehicleAlreadyInYardError` |
| 5 | Venda rápida reenviada após timeout | ❌ 2 vendas, caixa em dobro | ✅ 1 venda; o reenvio devolve a mesma | Cabeçalho `Idempotency-Key` (tabela `chaves_idempotencia`), enviado pelo app em todo POST autenticado |
| 6 | Mesma chave enviada 2x ao mesmo tempo | — | ✅ 1 venda (200 + 409 "em processamento") | Idem |
| 7 | Lançamento de caixa reenviado | — | ✅ 1 lançamento | Idem |
| 8 | 7 agendamentos simultâneos num horário de capacidade 4 | ❌ 7 agendados | ✅ 4 agendados, 3 recusados (409) | Contagem e inserção atômicas por lava-jato e hora (`pg_advisory_xact_lock`) |
| 9 | Cadastro de cliente enviado 2x ao mesmo tempo | ❌ 200 + **500** | ✅ 200 + 200 (mesma conta) | Violação de unicidade vira `DuplicateKeyError`; o cadastro perdedor entra na conta recém-criada |
| 10 | Gerar ticket 2x | ✅ mesmo link | ✅ mesmo link | — |

Execução depois das correções: **10/10**, nenhum 5xx no log (109 requisições).

## No app (PWA)

- Um POST idêntico ainda em andamento (clique duplo) não é enviado de novo: a segunda chamada recebe a mesma resposta.
- Se o POST falhar (rede, timeout, erro do servidor), a chave é mantida. Enviar de novo o mesmo conteúdo reaproveita a chave, e o servidor devolve o resultado original caso a primeira tentativa tenha dado certo.
- Depois de um sucesso, a chave é descartada: uma operação idêntica mais tarde é uma operação nova.

## Testes automatizados

- `ConcurrencyIntegrationTest`: requisições liberadas no mesmo instante, cada uma na sua conexão, no PostgreSQL embutido. Cobre lavadores disputando um carro, horário lotado, placa duplicada simultânea e em sequência, e cadastro duplicado.
- `IdempotencyGuardTest` e `JdbiIdempotencyStoreIntegrationTest`: replay, chave reutilizada com outro conteúdo (422), em processamento (409), liberação em falha, tentativa abandonada e expiração em 24 h.
- `DbStartServiceOrderTest`, `DbAuthenticateUserTest`: o lado perdedor de cada corrida.
- `fetch-http-client.test.ts`: chave enviada, clique duplo deduplicado, reenvio com a mesma chave, chave nova após sucesso.

## Ainda sem cobertura

- PWA sem internet ou caindo no meio de uma ação (sem fila offline).
- Sessão expirando durante o uso.
- Testes de navegador ponta a ponta no CI.
- Carga com muitos lava-jatos ao mesmo tempo.

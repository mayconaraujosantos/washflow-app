# Teste de carga

Mede quantos lava-jatos cada plano de hospedagem aguenta, reproduzindo os limites de CPU e memória
de cada plano em contêineres Podman (cgroups v2) e o dia de pico com o k6.

## Como rodar

```bash
make load-seed                     # banco novo + massa (API sem limite; ~2 min)
make load-test PROFILE=render-starter-1g HOLD=5m
make load-test PROFILES="render-free render-starter-1g vps-2cpu-4g"   # vários, em sequência
```

Perfis e limites ficam em `scripts/load/run.sh`; o resultado de cada um vai para
`scripts/load/out/result-<perfil>.md` (CPU e memória amostradas com `podman stats`).

## Cenário (20 lava-jatos)

Massa: 20 lava-jatos, 10 lavadores cada, 4.000 clientes com veículo, 108 mil atendimentos de
histórico (180 dias) com pagamento e comissão. No pico, cada tela repete o que o PWA faz hoje:

| Quem | Telas abertas | Requisições |
|---|---|---|
| Lavadores | 200 | fila + minhas lavagens a cada 3 s |
| Clientes acompanhando o carro | 200 | seus atendimentos a cada 3 s |
| Gerentes | 20 | pátio a cada 3 s, indicadores do dia a cada 30 s |
| Tickets públicos | 100 | ticket a cada 5 s |
| Fluxo de carros | 10 por minuto | entrada, início, conclusão e pagamento |

Isso dá **~210 requisições/s** e **~1,45 MB/s de saída (~52 GB por dia de 10 h)**.

## Resultados (10/11/2026, 5 min por perfil)

| Perfil | API | Postgres | Req/s atendidas | Erros | p95 | p99 | Veredito |
|---|---|---|---|---|---|---|---|
| render-free | 0,1 CPU / 512 MB | 0,1 CPU / 256 MB | 45 | 10% | 21,5 s | 45 s | Não serve |
| render-starter | 0,5 CPU / 512 MB | 0,1 CPU / 256 MB | 68 | 0,03% | 9,2 s | 12,6 s | Não serve (banco no limite) |
| render-starter-1g | 0,5 CPU / 512 MB | 0,5 CPU / 1 GB | 217 | 0% | 104 ms | 1,7 s | Aguenta, no limite |
| render-standard | 1 CPU / 2 GB | 0,5 CPU / 1 GB | 222 | 0% | 6 ms | 694 ms | Aguenta com folga |
| vps-2cpu-4g | 2 CPU / 2 GB | 2 CPU / 2 GB | 224 | 0% | 5 ms | 62 ms | Aguenta com muita folga |

Consumo com a carga toda atendida: API ~0,3 CPU em média (picos de até 1,3), ~320–360 MB;
Postgres ~0,27 CPU, ~130 MB. O relatório do dia é o endpoint mais lento (p95 0,5–2 s).

## Leitura

- O gargalo dos planos pequenos é o **Postgres de 0,1 CPU**: com ele, nem a API de 0,5 CPU passa
  de ~70 req/s.
- Com Postgres de 0,5 CPU, a API de 0,5 CPU atende o pico, mas fica encostada no limite (p99 em
  segundos). A partir de 1 CPU na API sobra folga.
- O custo que mais pesa é o **tráfego**: ~52 GB por dia de expediente, quase todo de telas
  perguntando "mudou algo?" sem mudança. Responder 304 quando nada mudou, pausar com a tela
  apagada e juntar as chamadas do lavador cortam a carga e o tráfego.

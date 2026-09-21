# Especificação Funcional e Técnica: Visão do Cliente (PWA) — WashFlow

## 1. Visão Geral
Esta especificação descreve os requisitos funcionais, regras de negócio, ciclo de vida de status e requisitos técnicos da interface PWA voltada para o **Cliente** no ecossistema **WashFlow**.

---

## 2. Jornada do Usuário & Requisitos Funcionais

### 2.1. Acesso & Onboarding
* **RF-01 (Acesso Simplificado):** O cliente deve conseguir acessar o PWA por meio de URL direta ou leitura de QR Code.
* **RF-02 (Instalação PWA):** A aplicação deve oferecer manifesto PWA para instalação na tela inicial (*Add to Home Screen*) sem necessidade de loja de aplicativos.

### 2.2. Agendamento de Serviço
* **RF-03 (Seleção de Serviços e Horários):** O cliente escolhe o tipo de lavagem/serviço, veículo (se já cadastrado), data e horário desejados.
* **RF-04 (Validação de Capacidade):** O sistema deve validar em tempo real:
  * Horário de funcionamento do estabelecimento.
  * Limite de capacidade (vagas simultâneas / pátio).
  * *Caso inválido:* Exibe mensagem explicativa (ex: "Pátio lotado para este horário") e solicita nova seleção.
  * *Caso válido:* Persiste o agendamento no banco de dados com o status `AGENDADO`.

### 2.3. Notificações e Acompanhamento
* **RF-05 (Notificações em Tempo Real):** O cliente deve receber notificações push ou alertas visuais nas transições de status do seu pedido:
  1. **Confirmação:** Quando o agendamento é realizado (`AGENDADO`).
  2. **Início da Lavagem:** Quando o lavador assume a ordem de serviço (`EM_LAVAGEM`).
  3. **Conclusão:** Quando a lavagem é finalizada (`PRONTO`).
* **RF-06 (Painel de Status / Tracking):** O cliente acompanha na tela do celular a evolução do status do veículo até a liberação final (`FINALIZADO`).

---

## 3. Máquina de Estados da Ordem de Serviço (Perspectiva do Cliente)

```
[Agendamento via PWA]
          │
          ▼
     (AGENDADO)  ──────►  🔔 Notificação 1: Confirmação de Agendamento
          │
 [Recepção no Pátio]
          │
          ▼
 (AGUARDANDO_PATIO)
          │
   [Início pelo Lavador]
          │
          ▼
    (EM_LAVAGEM) ──────►  🔔 Notificação 2: Lavagem Iniciada
          │
  [Conclusão Lavagem]
          │
          ▼
      (PRONTO)   ──────►  🔔 Notificação 3: Carro Pronto para Retirada
          │
 [Pagamento & Entrega]
          │
          ▼
    (FINALIZADO)
```

---

## 4. Endpoints Recomendados (API REST)

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/v1/services` | Lista catálogo de lavagens e preços |
| `GET` | `/api/v1/availability` | Consulta horários/vagas disponíveis |
| `POST` | `/api/v1/service-orders` | Cria novo agendamento de lavagem |
| `GET` | `/api/v1/service-orders/{id}` | Consulta status em tempo real da OS |
| `GET` | `/api/v1/service-orders/{id}/events` | SSE (Server-Sent Events) ou polling para atualizações |

---

## 5. Critérios de Aceite

* **CA-01:** O cliente consegue realizar um agendamento completo em menos de 3 passos.
* **CA-02:** Tentativas de agendamento fora do horário comercial ou acima da capacidade do pátio devem ser rejeitadas instantaneamente com feedback amigável.
* **CA-03:** A alteração do status da OS para `EM_LAVAGEM` pelo lavador deve atualizar a tela do cliente em até 3 segundos.
* **CA-04:** Quando a OS atinge o status `PRONTO`, uma notificação clara deve orientar o cliente a se dirigir ao caixa/balcão para retirada.
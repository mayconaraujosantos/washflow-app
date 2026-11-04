# Especificação Funcional e Técnica: Visão do Cliente (PWA) — WashFlow

## 1. Visão Geral
Esta especificação descreve os requisitos funcionais, regras de negócio, ciclo de vida de status e requisitos técnicos da interface PWA voltada para o **Cliente** no ecossistema **WashFlow**.

---

## 2. Jornada do Usuário & Requisitos Funcionais

### 2.1. Acesso & Onboarding
* **RF-01 (Acesso Simplificado):** O cliente deve conseguir acessar o PWA por meio de URL direta ou leitura de QR Code.
* **RF-01a (Escolha do lava-jato):** Uma instalação atende vários lava-jatos. Antes de entrar, o cliente escolhe o lava-jato, buscando pelo nome (`GET /api/establishments?query=`) ou pelo link/QR do balcão (`/?lavajato=<slug>`). A escolha fica lembrada no aparelho, pode ser trocada a qualquer momento e é enviada à API no cabeçalho `X-Establishment`. A conta do cliente pertence a esse lava-jato: o mesmo telefone em outro lava-jato é outra conta.
* **RF-02 (Instalação PWA):** A aplicação deve oferecer manifesto PWA para instalação na tela inicial (*Add to Home Screen*) sem necessidade de loja de aplicativos.
* **Formas de entrar:**
  * Telefone + senha (mínimo de 8 caracteres, BCrypt). O nome é exigido só para criar a conta. Um telefone sem conta naquele lava-jato recebe a orientação "Toque em Criar conta".
  * Código de 6 dígitos por WhatsApp ou magic link (`/entrar/whatsapp`). Depende de integrar o provedor; hoje o código só aparece no log de desenvolvimento.
  * Google (quando `GOOGLE_CLIENT_ID` está configurado).
  * Sem conta: o ticket entregue no balcão (`/ticket/:token`) mostra o andamento sem login e sem dados pessoais.
* A API emite um token JWT de 8 horas com o perfil `CLIENTE`. O cadastro enviado duas vezes ao mesmo tempo entra na mesma conta, sem erro.

### 2.2. Agendamento de Serviço
* **RF-03 (Seleção de Serviços e Horários):** O cliente escolhe o tipo de lavagem/serviço, veículo (se já cadastrado), data e horário desejados.
  * Os horários exibidos e informados pelo cliente são sempre do estabelecimento em Manaus (`America/Manaus`, UTC-4), independentemente do fuso configurado no dispositivo.
* **RF-04 (Validação de Capacidade):** O sistema deve validar em tempo real:
  * Horário de funcionamento do lava-jato escolhido (dias, horas e domingos configurados pelo gerente).
  * Antecedência mínima de 30 minutos.
  * Limite de capacidade por hora (`max_veiculos_hora`). A tela só oferece horários livres (`GET /api/schedule/availability`), e a reserva é atômica: mesmo com várias pessoas agendando ao mesmo tempo, a hora nunca passa da capacidade.
  * *Caso inválido:* Exibe mensagem explicativa (ex: "Pátio lotado para este horário") e solicita nova seleção.
  * *Caso válido:* Persiste o agendamento no banco de dados com o status `AGENDADO`.

### 2.3. Notificações e Acompanhamento
* **RF-05 (Atualizações visuais):** Com o painel do cliente aberto, o estado do atendimento é consultado automaticamente a cada 3 segundos e mudanças aparecem como aviso visual:
  1. **Confirmação:** Quando o agendamento é realizado (`AGENDADO`).
  2. **Início da Lavagem:** Quando o lavador assume a ordem de serviço (`EM_LAVAGEM`).
  3. **Conclusão:** Quando a lavagem é finalizada (`PRONTO`); a instrução para retirada permanece visível até o atendimento ser finalizado.
  4. **Pausa:** O painel indica que a lavagem está temporariamente pausada.
  * Push com o PWA fechado não está implementado.
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
    (EM_LAVAGEM) ──────►  🔔 Aviso no painel: Lavagem Iniciada
          │
       [Pausa]
          ▼
       (PAUSADA) ──────►  Aviso de pausa temporária
          │
     [Retomada]
          └────────────► (EM_LAVAGEM)
          │
  [Conclusão Lavagem]
          │
          ▼
      (PRONTO)   ──────►  Aviso persistente: Carro Pronto para Retirada
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
| `GET` | `/api/establishments?query=` | Busca o lava-jato pelo nome (público) |
| `GET` | `/api/establishment` | Nome e horário do lava-jato escolhido (`X-Establishment`) |
| `POST` | `/api/auth/customers` | Cria conta ou entra (telefone + senha) |
| `POST` | `/api/auth/customers/otp`, `/otp/verify` | Entra por código de WhatsApp |
| `POST` | `/api/auth/customers/google` | Entra com Google |
| `GET` | `/api/services` | Lista catálogo de lavagens e preços |
| `GET` | `/api/schedule/availability?date=` | Horários do dia com vagas restantes |
| `POST` | `/api/vehicles` | Cadastra veículo próprio |
| `POST` | `/api/service-orders` | Cria novo agendamento de lavagem (aceita `Idempotency-Key`) |
| `GET` | `/api/customers/{id}/service-orders` | Lista os atendimentos do cliente autenticado |
| `GET` | `/api/service-orders/{id}` | Consulta atendimento próprio |
| `GET` | `/api/tickets/{token}` | Ticket público, sem login |

Fluxos detalhados em [system-design/fluxos.md](system-design/fluxos.md).

Não existe endpoint SSE/event stream; o painel usa polling autenticado em `/api/customers/{id}/service-orders`.

---

## 5. Critérios de Aceite

* **CA-01:** O cliente consegue realizar um agendamento completo em menos de 3 passos.
* **CA-02:** Tentativas de agendamento fora do horário comercial ou acima da capacidade do pátio devem ser rejeitadas instantaneamente com feedback amigável.
* **CA-03:** A alteração do status da OS para `EM_LAVAGEM` pelo lavador deve atualizar a tela do cliente em até 3 segundos.
* **CA-04:** Quando a OS atinge o status `PRONTO`, uma notificação clara deve orientar o cliente a se dirigir ao caixa/balcão para retirada.
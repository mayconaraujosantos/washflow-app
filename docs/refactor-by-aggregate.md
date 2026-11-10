# Refatoração: organizar camadas por agregado de domínio

## Contexto

O projeto WashFlow usa Clean Architecture (`domain`, `data`, `presentation`,
`application`, `infra`, `validation`). A separação de camadas está correta, mas
os pacotes de casos de uso, factories, erros e protocolos de banco estão
**achatados por operação**, gerando ~50 pastinhas com 1 arquivo cada. Isso
dificulta a navegação e não reflete os agregados descritos no README
(lava-jato, equipe, clientes, agenda, pátio, vendas, caixa).

Este documento descreve a reorganização **por agregado de domínio**, sem alterar
nomes de classes, assinaturas ou comportamento. Apenas move arquivos e ajusta
imports.

## Princípios desta refatoração

1. **Não renomear classes.** Só mover. Isso preserva `git log --follow`.
2. **Não alterar assinaturas, construtores ou lógica.** Só `package` e imports.
3. **Um PR por camada.** PRs separados para `domain/usecases`, `data/usecases`,
   `data/protocols/db`, `domain/errors`, `application/factories`,
   `presentation/controllers`. Cada PR deve compilar e passar os testes.
4. **Mover pacotes pelo IDE (IntelliJ)**, não por `git mv` manual, para que os
   imports sejam ajustados automaticamente.
5. **Atualizar `package-info.java`** de cada pacote criado/movido.
6. **Rodar `./gradlew test` após cada PR.** A cobertura existente (quase todo
   `Db*` e controller tem teste) é a rede de segurança.

## Agregados de domínio (destino)

| Agregado | Pacote | Responsabilidade |
|---|---|---|
| `serviceorder` | `serviceorder` | Ordem de serviço: ciclo de vida, checklist, fotos, refund |
| `schedule` | `schedule` | Agenda, disponibilidade, agendamento |
| `yard` | `yard` | Pátio, fila de espera, lavagem em andamento |
| `sale` | `sale` | Vendas, itens, pagamentos, refund de venda |
| `cash` | `cash` | Caixa, transações, categorias de despesa, relatório de caixa |
| `vehicle` | `vehicle` | Veículos, busca, vínculo com cliente |
| `user` | `user` | Usuários, equipe, autenticação, perfil, status ativo |
| `establishment` | `establishment` | Lava-jato (tenant), setup, plataforma |
| `customer` | `customer` | Cliente: login passwordless, ticket, histórico |
| `reporting` | `reporting` | Relatórios operacionais e badges |
| `pricing` | `pricing` | Preços de serviço |

---

## PR 1 — `domain/usecases/` por agregado

### Antes
`domain/usecases/` contém ~50 interfaces planas.

### Depois

domain/usecases/
├── serviceorder/
│ ├── CancelServiceOrder.java
│ ├── CheckInServiceOrder.java
│ ├── CompleteServiceOrder.java
│ ├── FinalizeServiceOrder.java
│ ├── GetServiceOrderById.java
│ ├── PauseServiceOrder.java
│ ├── RefundServiceOrder.java
│ ├── ResumeServiceOrder.java
│ ├── SaveServiceOrderChecklist.java
│ ├── SaveServiceOrderPhoto.java
│ ├── DeleteServiceOrderPhoto.java
│ ├── ListServiceOrderChecklists.java
│ ├── ListServiceOrderPhotos.java
│ └── StartServiceOrder.java
├── schedule/
│ ├── ListAgenda.java
│ ├── ScheduleAvailability.java
│ └── ScheduleServiceOrder.java
├── yard/
│ ├── GetWaitingQueue.java
│ ├── GetWashingByWasher.java
│ ├── GetYardOverview.java
│ └── RegisterVehicleEntry.java
├── sale/
│ ├── CreateSale.java
│ ├── ListSales.java
│ └── RefundSale.java
├── cash/
│ ├── CreateCashTransaction.java
│ ├── DeleteCashTransaction.java
│ ├── GetCashTransactionReport.java
│ ├── ListCashTransactions.java
│ ├── ListExpenseCategories.java
│ ├── CreateExpenseCategory.java
│ └── UpdateCashTransaction.java
├── vehicle/
│ ├── CreateVehicle.java
│ ├── ListVehiclesByCustomer.java
│ └── SearchVehicles.java
├── user/
│ ├── AuthenticateUser.java
│ ├── CreateWasher.java
│ ├── ListUsers.java
│ ├── UpdateUserActive.java
│ ├── UpdateUserEmail.java
│ └── UpdateUserProfile.java
├── establishment/
│ ├── ManageEstablishment.java
│ ├── ManagePlatform.java
│ └── SetupEstablishment.java
├── customer/
│ ├── CustomerPasswordlessLogin.java
│ ├── ListServiceOrdersByCustomer.java
│ └── ServiceOrderTicket.java
├── reporting/
│ ├── GetOperationsReport.java
│ └── WasherBadge.java
└── pricing/
├── CreateServicePrice.java
└── ListServicePrices.java


### Mapeamento arquivo-a-arquivo (agregado `serviceorder`)

| Arquivo atual | Novo pacote |
|---|---|
| `domain/usecases/CancelServiceOrder.java` | `domain/usecases/serviceorder/` |
| `domain/usecases/CheckInServiceOrder.java` | `domain/usecases/serviceorder/` |
| `domain/usecases/CompleteServiceOrder.java` | `domain/usecases/serviceorder/` |
| `domain/usecases/FinalizeServiceOrder.java` | `domain/usecases/serviceorder/` |
| `domain/usecases/GetServiceOrderById.java` | `domain/usecases/serviceorder/` |
| `domain/usecases/PauseServiceOrder.java` | `domain/usecases/serviceorder/` |
| `domain/usecases/RefundServiceOrder.java` | `domain/usecases/serviceorder/` |
| `domain/usecases/ResumeServiceOrder.java` | `domain/usecases/serviceorder/` |
| `domain/usecases/SaveServiceOrderChecklist.java` | `domain/usecases/serviceorder/` |
| `domain/usecases/SaveServiceOrderPhoto.java` | `domain/usecases/serviceorder/` |
| `domain/usecases/DeleteServiceOrderPhoto.java` | `domain/usecases/serviceorder/` |
| `domain/usecases/ListServiceOrderChecklists.java` | `domain/usecases/serviceorder/` |
| `domain/usecases/ListServiceOrderPhotos.java` | `domain/usecases/serviceorder/` |
| `domain/usecases/StartServiceOrder.java` | `domain/usecases/serviceorder/` |

### Checklist PR 1
- [ ] Criar pacotes vazios dos 12 agregados em `domain/usecases/`
- [ ] Mover cada interface para o pacote do agregado (use `Refactor > Move` no IntelliJ)
- [ ] Mover `package-info.java` para os novos pacotes onde fizer sentido
- [ ] Ajustar imports em `data/usecases/**` e `application/factories/usecases/**`
- [ ] `./gradlew compileJava` deve passar
- [ ] `./gradlew test` deve passar

---

## PR 2 — `data/usecases/` por agregado

### Antes
`data/usecases/` tem ~50 pastinhas com 1 arquivo cada (`cancelserviceorder/DbCancelServiceOrder.java`, etc.), além de `shared/ContactRules.java`.

### Depois
Espelha exatamente `domain/usecases/`:

data/usecases/
├── serviceorder/
│ ├── DbCancelServiceOrder.java
│ ├── DbCheckInServiceOrder.java
│ ├── DbCompleteServiceOrder.java
│ ├── DbFinalizeServiceOrder.java
│ ├── DbGetServiceOrderById.java
│ ├── DbPauseServiceOrder.java
│ ├── DbRefundServiceOrder.java
│ ├── DbResumeServiceOrder.java
│ ├── DbSaveServiceOrderChecklist.java
│ ├── DbSaveServiceOrderPhoto.java
│ ├── DbDeleteServiceOrderPhoto.java
│ ├── DbListServiceOrderChecklists.java
│ ├── DbListServiceOrderPhotos.java
│ └── DbStartServiceOrder.java
├── schedule/
│ ├── DbListAgenda.java
│ ├── DbScheduleAvailability.java
│ └── DbScheduleServiceOrder.java
├── yard/
│ ├── DbGetWaitingQueue.java
│ ├── DbGetWashingByWasher.java
│ ├── DbGetYardOverview.java
│ └── DbRegisterVehicleEntry.java
├── sale/
│ ├── DbCreateSale.java
│ ├── DbListSales.java
│ └── DbRefundSale.java
├── cash/
│ ├── DbCreateCashTransaction.java
│ ├── DbDeleteCashTransaction.java
│ ├── DbGetCashTransactionReport.java
│ ├── DbListCashTransactions.java
│ ├── DbListExpenseCategories.java
│ ├── DbCreateExpenseCategory.java
│ └── DbUpdateCashTransaction.java
├── vehicle/
│ ├── DbCreateVehicle.java
│ ├── DbListVehiclesByCustomer.java
│ └── DbSearchVehicles.java
├── user/
│ ├── DbAuthenticateUser.java
│ ├── DbCreateWasher.java
│ ├── DbListUsers.java
│ ├── DbUpdateUserActive.java
│ ├── DbUpdateUserEmail.java
│ └── DbUpdateUserProfile.java
├── establishment/
│ ├── DbManageEstablishment.java
│ ├── DbManagePlatform.java
│ ├── DbSetupEstablishment.java
│ └── EstablishmentRules.java
├── customer/
│ ├── DbCustomerPasswordlessLogin.java
│ ├── DbListServiceOrdersByCustomer.java
│ └── DbServiceOrderTicket.java
├── reporting/
│ └── DbGetOperationsReport.java
├── pricing/
│ ├── DbCreateServicePrice.java
│ └── DbListServicePrices.java
├── access/
│ ├── DbWasherBadge.java
│ └── Secrets.java
└── shared/
├── ContactRules.java
└── SchedulingRules.java # mover de scheduleserviceorder/
text


### Mapeamento `serviceorder` (data)

| Arquivo atual | Novo pacote |
|---|---|
| `data/usecases/cancelserviceorder/DbCancelServiceOrder.java` | `data/usecases/serviceorder/` |
| `data/usecases/checkinserviceorder/DbCheckInServiceOrder.java` | `data/usecases/serviceorder/` |
| `data/usecases/completeserviceorder/DbCompleteServiceOrder.java` | `data/usecases/serviceorder/` |
| `data/usecases/finalizeserviceorder/DbFinalizeServiceOrder.java` | `data/usecases/serviceorder/` |
| `data/usecases/getserviceorderbyid/DbGetServiceOrderById.java` | `data/usecases/serviceorder/` |
| `data/usecases/pauseserviceorder/DbPauseServiceOrder.java` | `data/usecases/serviceorder/` |
| `data/usecases/refundserviceorder/DbRefundServiceOrder.java` | `data/usecases/serviceorder/` |
| `data/usecases/resumeserviceorder/DbResumeServiceOrder.java` | `data/usecases/serviceorder/` |
| `data/usecases/savechecklist/DbSaveServiceOrderChecklist.java` | `data/usecases/serviceorder/` |
| `data/usecases/savephoto/DbSaveServiceOrderPhoto.java` | `data/usecases/serviceorder/` |
| `data/usecases/deletephoto/DbDeleteServiceOrderPhoto.java` | `data/usecases/serviceorder/` |
| `data/usecases/listchecklists/DbListServiceOrderChecklists.java` | `data/usecases/serviceorder/` |
| `data/usecases/listphotos/DbListServiceOrderPhotos.java` | `data/usecases/serviceorder/` |
| `data/usecases/startserviceorder/DbStartServiceOrder.java` | `data/usecases/serviceorder/` |

### Checklist PR 2
- [ ] Criar pacotes dos agregados em `data/usecases/`
- [ ] Mover cada `Db*` para o agregado correspondente
- [ ] Mover `SchedulingRules.java` para `data/usecases/shared/`
- [ ] Remover pastinhas vazias (`cancelserviceorder/`, etc.)
- [ ] Ajustar imports em `application/factories/usecases/**` e testes
- [ ] Mover testes `data/usecases/<op>/Db*Test.java` para `data/usecases/<agregado>/`
- [ ] `./gradlew test` deve passar

---

## PR 3 — `data/protocols/db/` por agregado

### Antes
`data/protocols/db/` tem ~60 interfaces planas (`LoadXxxRepository`, `CreateXxxRepository`, `SaveXxxRepository`...).

### Depois

data/protocols/db/
├── serviceorder/
│ ├── CancelServiceOrderRepository.java
│ ├── CreateServiceOrderRepository.java
│ ├── FinalizeServiceOrderRepository.java
│ ├── LoadServiceOrderByIdRepository.java
│ ├── LoadServiceOrdersByCustomerIdRepository.java
│ ├── LoadServiceOrderChecklistRepository.java
│ ├── LoadServiceOrderPhotoRepository.java
│ ├── PauseServiceOrderRepository.java
│ ├── RefundServiceOrderRepository.java
│ ├── ResumeServiceOrderRepository.java
│ ├── SaveServiceOrderChecklistRepository.java
│ ├── SaveServiceOrderPhotoRepository.java
│ ├── DeleteServiceOrderPhotoRepository.java
│ └── UpdateServiceOrderStatusRepository.java
├── schedule/
│ ├── CountServiceOrdersScheduledInRangeRepository.java
│ └── LoadServiceOrdersScheduledBetweenRepository.java
├── yard/
│ ├── LoadWaitingQueueRepository.java
│ ├── LoadWashingByWasherRepository.java
│ └── LoadYardOverviewRepository.java
├── sale/
│ ├── LoadSalesRepository.java
│ ├── RefundSaleRepository.java
│ └── SaveSaleRepository.java
├── cash/
│ ├── CashTransactionReportRepository.java
│ ├── DeleteCashTransactionRepository.java
│ ├── LoadCashTransactionRepository.java
│ ├── LoadExpenseCategoryRepository.java
│ ├── SaveCashTransactionRepository.java
│ ├── SaveExpenseCategoryRepository.java
│ └── UpdateCashTransactionRepository.java
├── vehicle/
│ ├── CreateVehicleRepository.java
│ ├── LoadVehicleByIdRepository.java
│ ├── LoadVehicleByPlateRepository.java
│ ├── LoadVehiclesByCustomerIdRepository.java
│ └── SearchVehiclesRepository.java
├── user/
│ ├── CreateUserRepository.java
│ ├── CreateWasherRepository.java
│ ├── CreateWasherCommissionRepository.java
│ ├── LoadAllUsersRepository.java
│ ├── LoadUserByEmailRepository.java
│ ├── LoadUserByIdRepository.java
│ ├── LoadUserByPhoneRepository.java
│ ├── LoadUserPasswordHashRepository.java
│ ├── SaveUserPasswordHashRepository.java
│ ├── UpdateUserActiveRepository.java
│ ├── UpdateUserEmailRepository.java
│ └── UpdateUserProfileRepository.java
├── establishment/
│ ├── EstablishmentRepository.java
│ └── PlatformRepository.java
├── access/
│ ├── CustomerIdentityRepository.java
│ ├── OtpRepository.java
│ ├── StaffCredentialsRepository.java
│ └── TicketRepository.java
└── shared/
├── CheckDatabaseHealthRepository.java
├── DuplicateKeyError.java
├── LoadOperationsReportRepository.java
├── LoadServicePriceByIdRepository.java
└── LoadAllServicePricesRepository.java
text


> **Nota**: `LoadOperationsReportRepository` pode ficar em `shared/` ou em
> `reporting/` (criar). Prefira `shared/` se for usado por mais de um agregado.

### Checklist PR 3
- [ ] Criar pacotes dos agregados em `data/protocols/db/`
- [ ] Mover cada `*Repository.java` para o agregado
- [ ] Ajustar imports em `data/usecases/**` e `infra/db/jdbi/**`
- [ ] `./gradlew test` deve passar

---

## PR 4 — `domain/errors/` por agregado

### Antes
45 arquivos planos em `domain/errors/`.

### Depois

domain/errors/
├── serviceorder/
│ ├── ServiceOrderNotCancellableError.java
│ ├── ServiceOrderNotFoundError.java
│ ├── ServiceOrderNotPausedError.java
│ ├── ServiceOrderNotRefundableError.java
│ ├── ServiceOrderNotWashingError.java
│ ├── InvalidServiceOrderStatusError.java
│ ├── InvalidChecklistTypeError.java
│ └── InvalidPhotoTypeError.java
├── sale/
│ ├── SaleNotFoundError.java
│ └── SaleRefundNotAllowedError.java
├── user/
│ ├── UserNotFoundError.java
│ ├── UserHasActiveWashesError.java
│ ├── UserProfileMismatchError.java
│ ├── AccountLockedError.java
│ ├── InvalidCredentialsError.java
│ ├── EmailAlreadyRegisteredError.java
│ ├── PhoneAlreadyRegisteredError.java
│ ├── ManagerSelfRegistrationNotAllowedError.java
│ └── WasherSelfRegistrationNotAllowedError.java
├── vehicle/
│ ├── VehicleNotFoundError.java
│ ├── VehicleAlreadyInYardError.java
│ ├── VehiclePlateAlreadyRegisteredError.java
│ └── InvalidPlateError.java
├── establishment/
│ ├── EstablishmentNotFoundError.java
│ ├── EstablishmentAlreadyConfiguredError.java
│ ├── EstablishmentSlugTakenError.java
│ └── InvalidSetupCodeError.java
├── auth/
│ ├── InvalidOtpError.java
│ ├── OtpTooSoonError.java
│ ├── OtpUnavailableError.java
│ └── GoogleLoginUnavailableError.java
├── schedule/
│ ├── SlotFullyBookedError.java
│ ├── InsufficientLeadTimeError.java
│ └── OutsideBusinessHoursError.java
├── cash/
│ ├── InvalidTransactionTypeError.java
│ └── ServicePriceNotFoundError.java
└── common/
├── ForbiddenError.java
├── UnauthorizedError.java
├── TenantRequiredError.java
└── TicketNotFoundError.java
text


### Checklist PR 4
- [ ] Criar pacotes de agregado em `domain/errors/`
- [ ] Mover cada `*Error.java`
- [ ] Ajustar imports em `data/usecases/**`, `presentation/**`, `application/**`
- [ ] `./gradlew test` deve passar

---

## PR 5 — `application/factories/` por agregado

### Antes
`application/factories/controllers/` e `application/factories/usecases/` têm ~50
arquivos planos cada.

### Depois

application/factories/
├── controllers/
│ ├── serviceorder/
│ │ ├── CancelServiceOrderControllerFactory.java
│ │ ├── CheckInServiceOrderControllerFactory.java
│ │ ├── CompleteServiceOrderControllerFactory.java
│ │ ├── FinalizeServiceOrderControllerFactory.java
│ │ ├── GetServiceOrderByIdControllerFactory.java
│ │ ├── PauseServiceOrderControllerFactory.java
│ │ ├── RefundServiceOrderControllerFactory.java
│ │ ├── ResumeServiceOrderControllerFactory.java
│ │ ├── SaveChecklistControllerFactory.java
│ │ ├── SavePhotoControllerFactory.java
│ │ ├── DeletePhotoControllerFactory.java
│ │ ├── GetChecklistsControllerFactory.java
│ │ ├── GetPhotosControllerFactory.java
│ │ └── StartServiceOrderControllerFactory.java
│ ├── schedule/
│ │ ├── ListAgendaControllerFactory.java
│ │ └── ScheduleServiceOrderControllerFactory.java
│ ├── yard/
│ │ ├── GetWaitingQueueControllerFactory.java
│ │ ├── GetWashingByWasherControllerFactory.java
│ │ ├── GetYardOverviewControllerFactory.java
│ │ └── RegisterVehicleEntryControllerFactory.java
│ ├── sale/
│ │ ├── CreateSaleControllerFactory.java
│ │ ├── ListSalesControllerFactory.java
│ │ ├── ListSaleRefundsControllerFactory.java
│ │ └── RefundSaleControllerFactory.java
│ ├── cash/
│ │ ├── CreateCashTransactionControllerFactory.java
│ │ ├── DeleteCashTransactionControllerFactory.java
│ │ ├── GetCashTransactionReportControllerFactory.java
│ │ ├── ListCashTransactionsControllerFactory.java
│ │ ├── ListExpenseCategoriesControllerFactory.java
│ │ ├── CreateExpenseCategoryControllerFactory.java
│ │ └── UpdateCashTransactionControllerFactory.java
│ ├── vehicle/
│ │ ├── CreateVehicleControllerFactory.java
│ │ ├── ListVehiclesByCustomerControllerFactory.java
│ │ └── SearchVehiclesControllerFactory.java
│ ├── user/
│ │ ├── AuthenticateCustomerControllerFactory.java
│ │ ├── AuthenticateManagerControllerFactory.java
│ │ ├── AuthenticateWasherControllerFactory.java
│ │ ├── CreateWasherControllerFactory.java
│ │ ├── ListUsersControllerFactory.java
│ │ ├── UpdateUserActiveControllerFactory.java
│ │ └── UpdateUserProfileControllerFactory.java
│ ├── establishment/
│ │ ├── ListServiceOrdersByCustomerControllerFactory.java # ver nota
│ │ └── ...
│ └── system/
│ ├── HealthControllerFactory.java
│ └── HelloControllerFactory.java
└── usecases/
├── serviceorder/
│ ├── CancelServiceOrderFactory.java
│ ├── CheckInServiceOrderFactory.java
│ ├── CompleteServiceOrderFactory.java
│ ├── FinalizeServiceOrderFactory.java
│ ├── GetServiceOrderByIdFactory.java
│ ├── PauseServiceOrderFactory.java
│ ├── RefundServiceOrderFactory.java
│ ├── ResumeServiceOrderFactory.java
│ ├── SaveServiceOrderChecklistFactory.java
│ ├── SaveServiceOrderPhotoFactory.java
│ ├── DeleteServiceOrderPhotoFactory.java
│ ├── ListServiceOrderChecklistsFactory.java
│ ├── ListServiceOrderPhotosFactory.java
│ └── StartServiceOrderFactory.java
├── schedule/
├── yard/
├── sale/
├── cash/
├── vehicle/
├── user/
├── establishment/
├── customer/
├── reporting/
├── pricing/
└── access/
├── AccessFactory.java
├── AuthenticateUserFactory.java
└── EstablishmentFactory.java
text


> **Nota**: `ListServiceOrdersByCustomerControllerFactory` pertence ao agregado
> `customer` (é uma leitura do ponto de vista do cliente), não a
> `establishment`. Corrija isso no PR.

### Checklist PR 5
- [ ] Criar subpacotes de agregado em `factories/controllers/` e `factories/usecases/`
- [ ] Mover cada factory
- [ ] Ajustar imports em `application/routes/**` e `application/AppConfig.java`
- [ ] `./gradlew test` deve passar

---

## PR 6 — `presentation/controllers/` (correções pontuais)

Aqui a estrutura **já é por caso de uso**, o que é coerente. Só há
inconsistências a corrigir:

| Arquivo atual | Ação |
|---|---|
| `presentation/controllers/operations/PauseServiceOrderRequest.java` | Mover para `presentation/controllers/pauseserviceorder/` |
| `presentation/controllers/establishment/BodyValues.java` | Mover para `presentation/helpers/` |
| `presentation/controllers/access/AccessControllers.java` | Se agrupa >1 controller, dividir em `AuthenticateCustomerController`, `AuthenticateManagerController`, `AuthenticateWasherController` dentro de `access/` |
| `presentation/controllers/establishment/` (4 arquivos) | Renomear pacote para `establishment/` e garantir que `SetupStatusController` fique junto |

### Alinhar testes com main

Renomear pastas em `src/test/java/com/washflow/presentation/controllers/`:

| Teste atual | Renomear para |
|---|---|
| `listordersbycustomer/` | `listserviceordersbycustomer/` |
| `listprices/` | `listserviceprices/` |
| `listvehicles/` | `listvehiclesbycustomer/` |

### Checklist PR 6
- [ ] Mover `PauseServiceOrderRequest.java`
- [ ] Mover `BodyValues.java` para `presentation/helpers/`
- [ ] Avaliar divisão de `AccessControllers.java`
- [ ] Renomear pastas de teste para espelhar main
- [ ] `./gradlew test` deve passar

---

## Ordem de execução recomendada

1. **PR 4** (`domain/errors`) — menos dependências, fácil de validar.
2. **PR 1** (`domain/usecases`) — depende de errors já organizados.
3. **PR 2** (`data/usecases`) — depende de `domain/usecases`.
4. **PR 3** (`data/protocols/db`) — depende de `data/usecases`.
5. **PR 5** (`application/factories`) — depende de `data/usecases`.
6. **PR 6** (`presentation/controllers`) — independente, pode ser o primeiro se preferir.

## Critérios de aceite (todos os PRs)

- [ ] `./gradlew compileJava` sem erros
- [ ] `./gradlew test` verde
- [ ] Nenhuma classe renomeada (só movida)
- [ ] Nenhum `package-info.java` órfão
- [ ] `git diff --stat` mostra apenas renames (Git detecta `R` quando o
      conteúdo é idêntico e só o path muda)
- [ ] Nenhum import quebrado (`grep -r "import com.washflow.domain.usecases.CancelServiceOrder"` deve retornar vazio após PR 1)

## Comandos úteis

```bash
# Verificar se sobrou import do pacote antigo
grep -rn "com\.washflow\.domain\.usecases\.CancelServiceOrder\b" src/

# Verificar pastas vazias
find src -type d -empty

# Confirmar que Git detectou renames
git diff --stat -M

# Rodar só os testes de um agregado
./gradlew test --tests "com.washflow.data.usecases.serviceorder.*"

O que NÃO fazer

    Não alterar domain/entities/ — está bem organizado.

    Não alterar presentation/protocols/ nem validation/ — pequenos e coesos.

    Não renomear classes para "padronizar" — fora do escopo.

    Não mesclar PRs de camadas diferentes — dificulta revisão e rollback.

    Não usar git mv manual em massa — use o IDE para ajustar imports.

text


---

### Como usar com o Claude Code

1. Salve o arquivo em `docs/refactor/domain-by-aggregate.md`.
2. Peça ao Claude Code para executar **um PR por vez**, por exemplo:
   > "Execute o PR 4 do `docs/refactor/domain-by-aggregate.md`. Ao final, rode `./gradlew test` e me mostre o `git diff --stat -M`."
3. Revise o diff antes de commitar. Como o conteúdo das classes não muda, o Git deve mostrar tudo como `R` (rename), o que facilita a revisão.
4. Repita para os PRs 1, 2, 3, 5 e 6 na ordem recomendada.

Se quiser, posso detalhar o mesmo mapeamento arquivo-a-arquivo para outro agregado (ex.: `cash` ou `user`), mas o padrão do `serviceorder` já serve de template para todos os demais.


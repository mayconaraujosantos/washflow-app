# Local CI/CD (kind + Argo)

All CI/CD runs here, on your machine via Podman - there is no GitHub Actions
workflow; GitHub only hosts the repository, which Argo polls and clones.
Production runs on **Render** and is deployed by an Argo Workflow
(`make deploy-prod`).

| Component      | Role                                                           |
| --------------- | --------------------------------------------------------------- |
| kind            | Local Kubernetes cluster, backed by Podman (`infra/kind/`)      |
| Argo Workflows  | CI: backend tests+coverage, formatting, frontend lint, build check, optional SonarCloud |
| Argo CD         | GitOps CD: syncs `infra/gitops` (ingress, LGTM observability, SonarQube, the app) - see [gitops/README.md](gitops/README.md) |
| Render API      | Production deploy, run as an Argo Workflow after the CI checks  |

## First-time setup

```
make cluster-up      # kind cluster + Argo CD + Argo Workflows + GitOps root Application
make image-local      # build the image and push it to the cluster registry (Image Updater deploys it)
make argo-port-forward     # Argo Workflows UI at https://localhost:2746
```

Argo CD, Grafana, SonarQube and the app are served by ingress-nginx on host
port 8880 by name (`http://argocd.local:8880`, `grafana.local`,
`sonarqube.local`, `washflow.local`) once the names are in `/etc/hosts` - see
[gitops/README.md](gitops/README.md) for the hosts entries and prerequisites.

Windows note: `kind`'s Podman provider is a Go binary that aborts its PATH
search when it hits a malformed PATH entry (this machine has one, from an
installer). `scripts/win-clean-path.ps1` sanitizes PATH for just that
subprocess; the Makefile already routes `kind` through it on Windows.

Windows note 2: if `podman ps`/`docker-compose ps` suddenly start failing with
connection-refused or EOF errors, and `podman machine list` still claims it's
"Currently running", the WSL2 utility VM got suspended mid-session (Windows
does this after inactivity) and the SSH/named-pipe tunnel died without the
machine noticing. Fix: `wsl --shutdown` (closes Podman Desktop's tray app
first if open), then `podman machine start`. Anything that was running
inside it (the kind cluster, the postgres container) stops and needs
restarting (`make db-up`; the kind node usually needs a full
`make cluster-down && make cluster-up` since its clock skews across the
suspend and Kubernetes' certs start rejecting requests).

## Database (Postgres)

`docker-compose.yml` has a `postgres` service (talks to Podman through its
Docker-API-compatible pipe - no `podman-compose` needed):

```
make db-up     # Postgres on localhost:5432, db/user/password all "washflow"
make db-down
make db-logs
```

`AppConfig` loads configuration once at startup. Operating-system environment
variables take precedence over a local `.env` file; `.env` values are considered
only outside production. Copy `.env.example` to `.env` for local development.
Never put production secrets in `.env` or commit them.

Generate a local signing secret with `make generate-auth-secret`. It writes a
256-bit secret to `.env` without printing it or replacing an already configured
`AUTH_TOKEN_SECRET`.

To create fictitious local accounts for the customer, washer, and manager
flows, start the development backend with `make dev` and run
`make seed-test-users` in another terminal. The command uses only
`127.0.0.1:7000`, requires the manager password, and refuses `APP_ENV=production`.
It creates the customer and washer through the local API, then promotes the
washer using the seeded manager account. Random test credentials are saved in
the Git-ignored `.local-test-users` file (permissions `0600`) and printed once.
The manager phone is `11999993333`; on a fresh development database, choose a
password of at least eight characters when prompted. If that manager already
has a password, enter its existing password.

`JdbiFactory` (`src/main/java/com/washflow/infra/db/jdbi/JdbiFactory.java`)
builds the app's `Jdbi` + Hikari pool from this configuration:

- Discrete `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD`, all
  defaulting to `docker-compose.yml`'s `postgres` service - so `make dev` /
  `make run` connect with zero configuration once `make db-up` has run.
- Or a single `DATABASE_URL` (`postgres://user:pass@host:port/db`), the form
  Render (and Railway) inject for a managed Postgres - takes priority when set.

The application runs on PostgreSQL only, in every environment: if the
database is unreachable or its migrations fail, startup stops with the error
instead of serving requests without data. Locally, start it with `make db-up`
and put the connection in `.env` (copy `.env.example`; `.env` is git-ignored).
On Windows with Podman, `localhost:5432` may not be forwarded - use the Podman
VM address as `DB_HOST` (`podman machine ssh "ip -4 addr show eth0"`).
In production the configuration comes from environment variables
(`DATABASE_URL` or `DB_*`).

The integration tests start their own PostgreSQL (embedded, via
`io.zonky.test:embedded-postgres`; no Docker needed) and give each test class a
fresh migrated database (`TestDatabase`).
`GET /api/health` reports live database status (`"up"`/`"down"`).

Migrations live in `src/main/resources/db/migration` and run through
`JdbiFactory.migrate` (application and tests alike); the history table is
`schema_history`.
Since 1.5.0 the trail starts from a consolidated baseline: on a database that
still has the pre-1.5.0 schema (history in `flyway_schema_history`), `V1`
removes that schema once and `V2` creates the current one - existing data is
discarded, as decided for the multi-lava-jato launch.

`APP_ENV` selects `development` (default) or `production`. Docker images default
to production; Docker Compose and the local Kubernetes deployment explicitly
select development. `PORT` and `MAX_VEHICLES_PER_HOUR` are also read through
`AppConfig`.

Authentication uses a BCrypt password hash and an HMAC-signed bearer token.
Production must set `AUTH_TOKEN_SECRET` to a secret of at least 32 characters.
One installation serves many lava-jatos. On a fresh install, open the app and
complete the setup wizard (`/configurar`): it creates the **platform
administrator** (e-mail + password). In production the wizard requires
`SETUP_CODE` - a secret you set on the server and type in the wizard - so nobody
else can claim the platform. Then, in `/admin`, create each lava-jato with its
first manager (e-mail + initial password) and hand the manager those
credentials plus the customers link `https://<host>/?lavajato=<address>` (shown
on the admin panel; print it as a QR code for the counter). Suspending a
lava-jato blocks its staff and customers without deleting data.

Other access settings:

| Variable | Default | Purpose |
| --- | --- | --- |
| `SETUP_CODE` | empty | Unlocks the one-time platform setup in production (wizard disabled while empty). |
| `OTP_CHANNEL` | `log` (dev), `none` (prod) | Customer WhatsApp codes: `evolution` sends them through the Evolution API (see "WhatsApp" below), `log` prints the code/magic link to the server log and is refused in production, `none` hides the option. |
| `EVOLUTION_URL` / `EVOLUTION_API_KEY` / `EVOLUTION_INSTANCE` | empty | Required with `OTP_CHANNEL=evolution`: Evolution server base URL, its global `AUTHENTICATION_API_KEY` and the instance (WhatsApp number) that sends the codes. |
| `GOOGLE_CLIENT_ID` | empty | Google Identity Services client ID; empty hides "Entrar com Google". |
| `APP_PUBLIC_URL` | empty | Public origin used in WhatsApp magic links (e.g. `https://app.lavajato.com`). |
| `EMAIL_CHANNEL` | `smtp` when `SMTP_HOST` is set, else `log` (dev) / `none` (prod) | The app's single e-mail channel, used by the customer access code and by "Esqueci a senha" codes (customers by phone → their recovery e-mail; managers by login e-mail). `log` prints the e-mail to the server log and is refused in production; `none` hides both options. |
| `SMTP_HOST` | empty | SMTP relay (Brevo, Resend, Amazon SES, Gmail with an app password; Mailpit locally). |
| `SMTP_PORT` | `587` | Relay port. |
| `SMTP_SECURITY` | `starttls` | `starttls` (587), `ssl` (465, TLS from the start) or `none` (only for a local inbox such as Mailpit). |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | empty | Relay credentials; empty username sends without authentication (e.g. a local test inbox). |
| `SMTP_FROM` | empty | Sender address, required with `EMAIL_CHANNEL=smtp`; use a domain the relay is allowed to send for (SPF/DKIM). |
| `SMTP_FROM_NAME` | `Washflow` | Sender name shown in the inbox. |

## WhatsApp (Evolution API)

The customer's access code goes out through a self-hosted
[Evolution API](https://docs.evolutionfoundation.com.br) (Apache 2.0). On the kind
cluster it runs next to the app (`infra/gitops/apps/washflow/evolution.yaml`),
internal only, with its own `evolution` database on the washflow PostgreSQL; the
`washflow` secret sets `OTP_CHANNEL=evolution` and the `EVOLUTION_*` variables.

Link the sending number once (the session is kept in the database):

1. `make evolution-port-forward` and open http://localhost:8082/manager; log in with
   the `EVOLUTION_API_KEY` of the secret.
2. Create the instance **`washflow`** (same as `EVOLUTION_INSTANCE`), integration
   *Baileys*, and scan the QR code with WhatsApp on the phone (Linked devices).
3. Ask for a code on the customer screen ("Entrar com código por WhatsApp").

Use a dedicated number: the QR (Baileys) link is not an official WhatsApp API and a
number that sends codes to strangers can be banned. With a verified company
(CNPJ), create the instance with the *WhatsApp Business* (Cloud API) integration
instead - the app's call (`EvolutionOtpSender`) doesn't change. A send that fails
answers 503 and the customer can try again right away.

On Render: a paid **Private Service** from the image `evoapicloud/evolution-api`
(same variables as `evolution.yaml`, no public URL), a paid PostgreSQL, and
`EVOLUTION_URL` pointing at the private service address. The free plan sleeps
after 15 minutes and drops the WhatsApp session.

## Backup

The `washflow-db-backup` CronJob (`apps/washflow/backup.yaml`) runs
`pg_dump` every night at 03:00 Manaus time into the `washflow-db-backups`
volume and keeps 14 days. That volume sits on the same node as the database:
it covers a bad migration or a deleted table, not a lost disk. For an off-site
copy add `BACKUP_S3_URI` (plus `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` and,
for R2/MinIO, `AWS_ENDPOINT_URL`) to the `washflow` secret and use an image with
the aws CLI.

`make test-backup` proves the scripts work: it backs up a throwaway Postgres,
drops a table and restores it.

```sh
ns=washflow-dev
# run a backup now
kubectl -n $ns create job --from=cronjob/washflow-db-backup backup-now
# run another command in the backup pod (same volumes and credentials)
backup_job() {
  kubectl -n $ns create job --from=cronjob/washflow-db-backup "$1" --dry-run=client -o json \
    | jq '.spec.template.spec.containers[0].command = $ARGS.positional' --args -- "${@:2}" \
    | kubectl apply -f -
}
backup_job list-backups ls -lh /backups && kubectl -n $ns logs -f job/list-backups
# restore: stop the API, restore, start it again
kubectl -n $ns scale deploy/washflow-api --replicas=0
backup_job restore-now sh /scripts/db-restore.sh --yes /backups/washflow-<stamp>.dump
kubectl -n $ns wait --for=condition=complete job/restore-now --timeout=10m
kubectl -n $ns scale deploy/washflow-api --replicas=1
```

## Logs, tracing and metrics

| Variable | Default | Purpose |
| --- | --- | --- |
| `LOG_LEVEL` | `INFO` | Root log level (`DEBUG` also prints the `/api/health` and `/api/metrics` polls). |
| `METRICS_TOKEN` | empty | Bearer token for `GET /api/metrics` (Prometheus format); empty turns the endpoint off (404). |

* **Logs** go to stdout (the Render "Logs" tab) as one `key=value` line per event, in UTC:
  `2026-11-04T05:20:47.358Z WARN  request_id=suporte-teste-01 logger=http method=POST route=/api/auth/managers status=400 duration_ms=4 user=- ip=... reason="..."`.
  Every API request gets one `logger=http` line: `INFO` for 2xx/3xx, `WARN` for 4xx (with the
  reason), `ERROR` for 5xx (with the stack trace on the line before). Browser crashes reported by
  the PWA appear as `logger=client Browser error ...`.
* **Tracing:** each request has an id, returned in the `X-Request-Id` header and in error bodies
  (`{"error": ..., "requestId": ...}`); a sane incoming `X-Request-Id` is reused. 5xx messages in
  the app show the code to the user. To investigate, search the logs for `request_id=<code>`.
* **Metrics:** `curl -H "Authorization: Bearer $METRICS_TOKEN" https://<host>/api/metrics` -
  `http_server_requests_seconds_*` (by method, route template, status, outcome), `hikaricp_*`
  (pool) and `jvm_*`/`process_*`. Point a Prometheus/Grafana Cloud scrape job at it with the
  bearer token.

Legacy: set `BOOTSTRAP_MANAGER_PASSWORD` (and, if needed, `BOOTSTRAP_MANAGER_PHONE`)
for the initial manager account without a password hash; after its first
successful login the hash is persisted and the bootstrap password can be
removed. Existing production users without a password hash need a secure
password reset before they can log in. Phone ownership is not verified by OTP.
Only customer accounts can self-register; managers must create or promote washer
and manager profiles before those users can sign in.

The local development environment allows a legacy user without a password hash
to choose one at first login. This compatibility behavior is disabled in
production.

## Day to day (git flow)

- **Once per clone:** `make hooks` - the pre-push hook (`.githooks/pre-push`)
  runs Spotless, compilation, lint, Prettier and the TypeScript check (~10 s)
  so a broken build never reaches `develop`/`main`.
- **End-to-end journey:** `make e2e` (visible browser) or `make e2e-ci`
  (headless) runs `web/e2e` - two lava-jatos from a fresh install through
  managers, washers and customers - against a throwaway pod on `:7100`
  (`scripts/e2e-env.sh`), never the dev app. Run it before every `release/*`.
- **feature/\***: `make ci` before opening a PR — runs the same checks Argo
  CD/production will expect (`infra/argo-workflows/ci-workflowtemplate.yaml`).
- **develop**: pushes are what Argo CD's Applications track
  (`infra/gitops/bootstrap/`, `targetRevision: develop`). Within ~2 minutes
  of a push, the `washflow-ci-trigger` CronWorkflow runs the checks, pushes
  `localhost:30500/washflow-api:develop-<SHA>` to the cluster registry and
  Argo CD Image Updater rolls it out to the **dev** environment
  (http://washflow.local:8880). Pushes to `feature/*`, `release/*` and
  `hotfix/*` get the checks only. See
  [gitops/README.md](gitops/README.md#git-flow-and-environments).
- **release/\***: same as develop, run `make ci` against the release branch:
  `argo submit -n argo --from workflowtemplate/washflow-ci -p revision=release/x.y.z --watch`.
- **main**: push `main` + tag, then `make deploy-prod` - an Argo Workflow
  (`infra/argo-workflows/deploy-render-workflowtemplate.yaml`) that:
  1. clones `main` and records the exact commit;
  2. runs the CI checks (backend tests + Spotless, frontend lint + Prettier),
     reusing the `washflow-ci` templates;
  3. asks Render to deploy **that commit** (Render API) and waits until it is
     `live` (fails on `build_failed`/`update_failed`, 20 min timeout);
  4. smoke-tests `GET <RENDER_SERVICE_URL>/api/health` (`database: up`).

  `make deploy-prod REVISION=v1.6.0` deploys a tag or branch;
  `SKIP_CHECKS=true` redeploys an already verified commit without the tests.
  It's a deliberate, manual step: there's no webhook receiver watching this
  repo. In Render, set **Auto-Deploy = Off** for the service, so a push to
  `main` doesn't deploy on its own before the checks.

## Code quality: JaCoCo, Spotless, SonarCloud

- `make test` runs the backend suite and generates a JaCoCo report
  (`build/reports/jacoco/test/html/index.html`); `make coverage` just points
  you at it.
- `make lint` now also runs `spotlessCheck` (google-java-format) alongside the
  frontend lint; `make format` applies it. CI (`make ci`) checks formatting
  the same way - a misformatted file fails the pipeline.
- `bun run test:coverage` (in `web/`) runs the frontend suite and writes
  `web/coverage/lcov.info`; SonarCloud reads it together with the JaCoCo XML.
- SonarCloud runs only through the Argo pipeline (`runSonar=true`, needs the
  `sonar-credentials` Secret below) or `make sonar`; it stays **off by
  default**. There is no GitHub Actions workflow: all CI/CD runs locally in
  Argo, and GitHub only hosts the repository.

### One-time: SonarCloud

1. Go to [sonarcloud.io](https://sonarcloud.io), sign in with GitHub, and
   import `mayconaraujosantos/washflow-app` (it's public, so analysis is free).
2. In the project, go to **Administration → Analysis Method** and turn
   **Automatic Analysis off** - analysis comes from CI (Gradle), and SonarCloud
   rejects CI analyses while automatic analysis is on.
3. If SonarCloud assigns a different project/organization key than the
   defaults in `build.gradle.kts` (`sonar.projectKey` /
   `sonar.organization`), update them there to match.
4. Generate a token (My Account → Security) and store it in the cluster for
   the Argo pipeline:
   ```
   kubectl -n argo create secret generic sonar-credentials \
     --from-literal=SONAR_TOKEN=<token>
   ```
5. Run it: `argo submit -n argo --from workflowtemplate/washflow-ci -p revision=develop -p runSonar=true --watch`,
   or locally without the cluster at all: `SONAR_TOKEN=<token> make sonar`.

## One-time: Render credentials for `make deploy-prod`

Not committed anywhere. In the Render dashboard:

- **API key:** Account Settings → API Keys → Create API Key.
- **Service id:** the `srv-...` part of the service URL
  (`https://dashboard.render.com/web/srv-...`).
- **Public URL:** e.g. `https://washflow-app-edxg.onrender.com`.
- Service → Settings → **Auto-Deploy: Off** (Argo is the only deployer).

```
kubectl create namespace argo --dry-run=client -o yaml | kubectl apply -f -
kubectl -n argo create secret generic render-credentials \
  --from-literal=RENDER_API_KEY=<api-key> \
  --from-literal=RENDER_SERVICE_ID=<srv-...> \
  --from-literal=RENDER_SERVICE_URL=https://<service>.onrender.com
```

Production environment variables (Render → Environment): `APP_ENV=production`,
`DATABASE_URL` (from the Render Postgres), `AUTH_TOKEN_SECRET` (≥ 32 chars),
`SETUP_CODE` (needed once, to create the platform admin in `/configurar`),
`METRICS_TOKEN` (optional, enables `/api/metrics`), `APP_PUBLIC_URL`, and
optionally `GOOGLE_CLIENT_ID` / `OTP_CHANNEL` and `SMTP_HOST` / `SMTP_PORT` /
`SMTP_USERNAME` / `SMTP_PASSWORD` / `SMTP_FROM` ("Esqueci a senha" by e-mail code).

## Tearing down

```
make cluster-down
```

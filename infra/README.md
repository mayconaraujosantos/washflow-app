# Local CI/CD (kind + Argo)

Replaces GitHub Actions, which is blocked by a billing issue on the account.
Runs entirely on your machine via Podman Desktop; production still deploys to
Railway.

| Component      | Role                                                           |
| --------------- | --------------------------------------------------------------- |
| kind            | Local Kubernetes cluster, backed by Podman (`infra/kind/`)      |
| Argo Workflows  | CI: backend tests+coverage, formatting, frontend lint, build check, optional SonarCloud |
| Argo CD         | GitOps CD: syncs `infra/k8s/local` onto the cluster (staging)   |
| Railway CLI     | Production deploy, run as an Argo Workflow                      |

## First-time setup

```
make cluster-up      # kind cluster + Argo CD + Argo Workflows
make image-local      # build washflow-api:local and load it into the cluster
make argocd-port-forward   # UI at https://localhost:8081 (admin / `make argocd-password`)
make argo-port-forward     # UI at https://localhost:2746
```

The app itself is reachable at http://localhost:7000 once Argo CD has synced
`infra/k8s/local` (NodePort 30080, mapped by `infra/kind/kind-config.yaml`) -
same port as `make run`, since 8080 is the default lots of other local
services (Spring Boot, etc.) already grab.

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
  Railway injects for its Postgres plugin - takes priority when set.

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
| `OTP_CHANNEL` | `log` (dev), `none` (prod) | Customer WhatsApp codes. `log` prints the code/magic link to the server log and is refused in production; plug a WhatsApp provider in `AccessFactory.otpSender`. |
| `GOOGLE_CLIENT_ID` | empty | Google Identity Services client ID; empty hides "Entrar com Google". |
| `APP_PUBLIC_URL` | empty | Public origin used in WhatsApp magic links (e.g. `https://app.lavajato.com`). |

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

- **feature/\***: `make ci` before opening a PR — runs the same checks Argo
  CD/production will expect (`infra/argo-workflows/ci-workflowtemplate.yaml`).
- **develop**: pushes are what Argo CD's `washflow-local` Application tracks
  (`infra/argocd/application-local.yaml`, `targetRevision: develop`). After
  pushing, run `make image-local` to rebuild the image the Deployment uses —
  Argo CD manages the manifests, not the image content, since there's no
  registry in this local setup.
- **release/\***: same as develop, run `make ci` against the release branch:
  `argo submit -n argo --from workflowtemplate/washflow-ci -p revision=release/x.y.z --watch`.
- **main**: `make deploy-prod` — runs `railway up` against production inside
  an Argo Workflow (`infra/argo-workflows/deploy-railway-workflowtemplate.yaml`).
  This used to fire automatically on push in GitHub Actions; here it's a
  deliberate, manual step since there's no webhook receiver watching this repo.

## Code quality: JaCoCo, Spotless, SonarCloud

- `make test` runs the backend suite and generates a JaCoCo report
  (`build/reports/jacoco/test/html/index.html`); `make coverage` just points
  you at it.
- `make lint` now also runs `spotlessCheck` (google-java-format) alongside the
  frontend lint; `make format` applies it. CI (`make ci`) checks formatting
  the same way - a misformatted file fails the pipeline.
- `bun run test:coverage` (in `web/`) runs the frontend suite and writes
  `web/coverage/lcov.info`; SonarCloud reads it together with the JaCoCo XML.
- SonarCloud runs in GitHub Actions (`.github/workflows/ci-cd.yml`) on every
  push/PR to `main` and `develop` once the `SONAR_TOKEN` repository secret
  exists; without it the step is skipped. In `make ci` (Argo) it stays
  **off by default** (`runSonar=false`).

### One-time: SonarCloud

1. Go to [sonarcloud.io](https://sonarcloud.io), sign in with GitHub, and
   import `mayconaraujosantos/washflow-app` (it's public, so analysis is free).
2. In the project, go to **Administration → Analysis Method** and turn
   **Automatic Analysis off** - analysis comes from CI (Gradle), and SonarCloud
   rejects CI analyses while automatic analysis is on.
3. If SonarCloud assigns a different project/organization key than the
   defaults in `build.gradle.kts` (`sonar.projectKey` /
   `sonar.organization`), update them there to match.
4. Generate a token (My Account → Security) and add it as a GitHub repository
   secret named `SONAR_TOKEN` (Settings → Secrets and variables → Actions, or
   `gh secret set SONAR_TOKEN`). For the Argo pipeline, also create:
   ```
   kubectl -n argo create secret generic sonar-credentials \
     --from-literal=SONAR_TOKEN=<token>
   ```
5. Run it: `argo submit -n argo --from workflowtemplate/washflow-ci -p revision=develop -p runSonar=true --watch`,
   or locally without the cluster at all: `SONAR_TOKEN=<token> make sonar`.

## One-time: Railway credentials for `make deploy-prod`

Not committed anywhere. Create the secret once, from the same values used in
the old GitHub Actions secrets (`RAILWAY_TOKEN`, `RAILWAY_SERVICE_ID`):

```
kubectl create namespace argo --dry-run=client -o yaml | kubectl apply -f -
kubectl -n argo create secret generic railway-credentials \
  --from-literal=RAILWAY_TOKEN=<token> \
  --from-literal=RAILWAY_SERVICE_ID=<service-id>
```

## Tearing down

```
make cluster-down
```

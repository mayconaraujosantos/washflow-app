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

In development, an unreachable Postgres falls back to in-memory H2, so
`make dev`/`gradle run` still start without `make db-up`. If a database is
reachable but its migrations fail, startup stops with the migration error
instead of serving requests against an incomplete schema. In production, the
database configuration must be supplied through environment variables and the
application fails to start if PostgreSQL or its migrations are unavailable.
`GET /api/health` reports live database status (`"up"`/`"down"`).

`APP_ENV` selects `development` (default) or `production`. Docker images default
to production; Docker Compose and the local Kubernetes deployment explicitly
select development. `PORT` and `MAX_VEHICLES_PER_HOUR` are also read through
`AppConfig`.

Authentication uses a BCrypt password hash and an HMAC-signed bearer token.
Production must set `AUTH_TOKEN_SECRET` to a secret of at least 32 characters.
Set `BOOTSTRAP_MANAGER_PASSWORD` (and, if needed, `BOOTSTRAP_MANAGER_PHONE`)
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
- SonarCloud analysis is wired up but **off by default** in `make ci`
  (`runSonar=false`) until you do the one-time setup below.

### One-time: SonarCloud

1. Go to [sonarcloud.io](https://sonarcloud.io), sign in with GitHub, and
   import `mayconaraujosantos/washflow-app` (it's public, so analysis is free).
2. If SonarCloud assigns a different project/organization key than the
   defaults in `build.gradle.kts` (`sonar.projectKey` /
   `sonar.organization`), update them there to match.
3. Generate a token (My Account → Security) and create the secret:
   ```
   kubectl -n argo create secret generic sonar-credentials \
     --from-literal=SONAR_TOKEN=<token>
   ```
4. Run it: `argo submit -n argo --from workflowtemplate/washflow-ci -p revision=develop -p runSonar=true --watch`,
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

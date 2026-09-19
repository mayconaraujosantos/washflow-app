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

`JdbiFactory` (`src/com/washflow/infra/db/jdbi/JdbiFactory.java`) builds the
app's `Jdbi` + Hikari pool from the environment - the same role
`application.properties` plays for `spring-boot-starter-jdbc`:

- Discrete `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD`, all
  defaulting to `docker-compose.yml`'s `postgres` service - so `make dev` /
  `make run` connect with zero configuration once `make db-up` has run.
- Or a single `DATABASE_URL` (`postgres://user:pass@host:port/db`), the form
  Railway injects for its Postgres plugin - takes priority when set.

Startup never blocks on Postgres being reachable (`initializationFailTimeout
= -1`), so `make dev`/`gradle test` still work without `make db-up`. Live
status shows up in `GET /api/health`'s `database` field (`"up"`/`"down"`).

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

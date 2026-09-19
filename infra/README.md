# Local CI/CD (kind + Argo)

Replaces GitHub Actions, which is blocked by a billing issue on the account.
Runs entirely on your machine via Podman Desktop; production still deploys to
Railway.

| Component      | Role                                                           |
| --------------- | --------------------------------------------------------------- |
| kind            | Local Kubernetes cluster, backed by Podman (`infra/kind/`)      |
| Argo Workflows  | CI: backend tests, frontend lint, Dockerfile build check         |
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

# Local cluster via Argo CD (GitOps)

Everything on the local kind cluster - ingress, observability (LGTM), SonarQube,
the CI pipelines and the washflow **dev** environment - is declared here and
synced by Argo CD (App-of-Apps). Argo Workflows itself (controller, CRDs) is
installed by `make cluster-up`; production runs on Render (see
[../README.md](../README.md)).

## Git flow and environments

| Branch | Pipeline (started by the `washflow-ci-trigger` CronWorkflow) | Environment |
| --- | --- | --- |
| `feature/*`, `bugfix/*` | `washflow-ci`: tests, Spotless, lint, Prettier, build | - |
| `develop` | `washflow-ci` + image `develop-<SHA>` | **dev**: this cluster, http://washflow.local:8880 (Image Updater deploys it) |
| `release/*`, `hotfix/*`, `support/*` | `washflow-ci` | - |
| `main` | `washflow-deploy-render` (checks + Render deploy + smoke test), only if `autoDeployMain=true`; otherwise `make deploy-prod` | **prod**: Render |

- The trigger polls GitHub (`git ls-remote`) every 2 minutes, so nothing on
  this machine has to be reachable from the internet. Its first run only
  records the current branch heads.
- Production stays a deliberate `make deploy-prod` by default. To deploy every
  push to `main`, set `autoDeployMain` to `"true"` in
  `infra/argo-workflows/ci-trigger-cronworkflow.yaml`, and turn Render's own
  Auto-Deploy off.
- Manifests and pipelines (`infra/gitops`, `infra/argo-workflows`) follow
  `develop` too: infrastructure changes go feature -> PR -> develop like code.
- `washflow-ci` runs one at a time (mutex): a burst of pushes queues up
  instead of running several Gradle builds side by side.
- To try a feature on dev before merging: `make deploy-dev` (checks + image
  `local-<SHA>` built in the cluster from the branch as pushed to GitHub) or
  `make image-local` (builds your working tree on this machine, no checks).
  The next `develop` build replaces it.

```
infra/gitops/
├── bootstrap/
│   ├── root.yaml                  # the only manifest applied by hand
│   └── applications/              # child Applications (one per component)
│       ├── argocd.yaml            # wave -2  Argo CD manages itself
│       ├── ingress-nginx.yaml     # wave -1  ingress controller (NodePort 30880/30443)
│       ├── registry.yaml          # wave -1  image registry (NodePort 30500)
│       ├── kube-prometheus-stack.yaml  # 0  Prometheus + Grafana (+ ServiceMonitor CRDs)
│       ├── argocd-image-updater.yaml   # 0  deploys new washflow images
│       ├── ci-pipelines.yaml      # wave 0   infra/argo-workflows (CI, Render deploy, git flow trigger)
│       ├── loki.yaml              # wave 1   logs
│       ├── tempo.yaml             # wave 1   traces
│       ├── sonarqube.yaml         # wave 1   SonarQube + its PostgreSQL
│       ├── alloy.yaml             # wave 2   log shipper DaemonSet -> Loki
│       └── washflow-dev.yaml      # wave 3   the app, dev environment (develop)
├── infrastructure/                # Helm values (and Kustomize where a chart is not enough)
│   ├── argocd/values.yaml
│   ├── ingress-nginx/values.yaml
│   ├── kube-prometheus-stack/values.yaml   # Grafana data sources: Prometheus, Loki, Tempo
│   ├── loki/values.yaml           # single binary, filesystem storage on a PV
│   ├── tempo/values.yaml          # single binary, OTLP on 4317/4318
│   ├── alloy/values.yaml          # Alloy config: pod logs -> Loki, logfmt parsing for washflow
│   ├── registry/                  # Kustomize: registry:3, PVC, NodePort Service
│   ├── argocd-image-updater/      # values (registry) + config/ (ImageUpdater for washflow)
│   └── sonarqube/
│       ├── values.yaml            # Community Build, external DB, ingress
│       └── postgres/              # Kustomize: Secret, Service, StatefulSet
└── apps/
    └── washflow/                  # Kustomize: API (+ PWA), PostgreSQL, Ingress, ServiceMonitor
```

Each child Application takes the chart from its Helm repository and the values
from this repo (multi-source `$values/...`), pinned to a chart version.

| Component | Chart | Namespace | URL |
| --- | --- | --- | --- |
| Argo CD | `argo/argo-cd` 10.11.1 | `argocd` | http://argocd.local:8880 |
| ingress-nginx | `ingress-nginx/ingress-nginx` 4.15.1 | `ingress-nginx` | - |
| Prometheus + Grafana | `prometheus-community/kube-prometheus-stack` 92.13.1 | `observability` | http://grafana.local:8880 |
| Loki | `grafana/loki` 7.5.0 | `observability` | via Grafana |
| Tempo | `grafana/tempo` 1.24.4 | `observability` | via Grafana |
| Alloy | `grafana/alloy` 1.14.3 | `observability` | - |
| SonarQube | `sonarqube/sonarqube` 2026.5.1003 | `sonarqube` | http://sonarqube.local:8880 |
| Registry | Kustomize (`registry:3.0.0`) | `registry` | `localhost:30500` (host only) |
| Argo CD Image Updater | `argo/argocd-image-updater` 1.3.2 | `argocd` | - |
| CI pipelines | Directory (`infra/argo-workflows`) | `argo` | Argo Workflows UI (`make argo-port-forward`) |
| washflow (dev) | Kustomize (`apps/washflow`) | `washflow-dev` | http://washflow.local:8880 |

## How the app is observed

| Signal | Path |
| --- | --- |
| Logs | stdout -> Alloy DaemonSet (reads pod logs through the Kubernetes API) -> Loki. The `key=value` lines get `level`/`logger` labels and `request_id`/`status` as structured metadata. In Grafana, a `request_id` in a line links to every line of that request. |
| Traces | The OpenTelemetry Java agent is downloaded by an init container and loaded with `JAVA_TOOL_OPTIONS` (no image change) -> OTLP/HTTP -> Tempo. Spans link to the service's logs (`service.name` = Loki `app` label). |
| Metrics | `GET /api/metrics` scraped by Prometheus through a ServiceMonitor that sends the `METRICS_TOKEN` bearer token. |

The React PWA is built into the same image and served by Javalin, so there is
one Deployment, not a separate frontend.

## How a commit gets deployed

```
git push develop
  └─▶ washflow-ci-trigger (CronWorkflow, polls GitHub every 2 min)
        └─▶ washflow-ci (pushImage=true)
              checkout ─▶ backend tests + Spotless │ frontend lint + Prettier
                       ─▶ gradle installDist (+ optional Sonar)
                       ─▶ kaniko: runtime image ─▶ registry: washflow-api:develop-<SHA> (+ latest)
                                                          │
        Argo CD Image Updater (polls the registry) ◀──────┘
          └─▶ sets washflow-dev image = localhost:30500/washflow-api:develop-<SHA>
                └─▶ Argo CD syncs the Deployment ─▶ http://washflow.local:8880
```

- The image is named `localhost:30500/washflow-api` everywhere: the registry's
  NodePort is published on the host at the same port, so the kind node, your
  machine (`make image-local`) and the manifests all use that address. CI
  pushes through the in-cluster Service instead, and the Image Updater
  maps one to the other (`argocd-image-updater/values.yaml`).
- Only `develop-<SHA>` and `local-<SHA>` tags are deployed (`newest-build`);
  CI images of other branches (`feature-x-<SHA>`, with `PUSH_IMAGE=true`) stay
  in the registry, and `latest` exists just so the Deployment can start
  before the first SHA image.
- The new tag is written on the Application object (write-back `argocd`), not
  committed to Git; the root Application ignores that field.
- Kaniko only packages the output of `gradle installDist`
  (`infra/argo-workflows/runtime.Dockerfile`), because building the root
  Dockerfile in the cluster hits a kaniko javac bug. Keep the two runtime
  stages in sync.

Manual runs, besides the trigger:

```
make ci                     # checks for the current branch (as pushed to GitHub)
make ci PUSH_IMAGE=true     # + image <branch>-<SHA> (deployed to dev only on develop)
make deploy-dev             # checks + image local-<SHA> for the current branch -> dev
make deploy-dev BRANCH=feature/x   # same, for another pushed branch
make image-local            # build + push local-<SHA> from your working tree, no checks
make deploy-prod            # main -> Render
```

## Working from another machine (e.g. a Mac)

Develop on any machine and push to GitHub; the trigger picks the push up.
To reach the cluster, open an SSH tunnel to the machine that runs it (VS Code
Remote-SSH forwards ports from its "Ports" tab instead):

```
ssh -N -L 8880:localhost:8880 -L 2746:localhost:2746 user@cluster-host
```

Add the `/etc/hosts` entries below on that machine too, then open
`http://washflow.local:8880` (dev) and `https://localhost:2746` (Argo
Workflows). With the `argo` CLI installed there (`brew install argo`), the
Argo Server takes the `make` targets that submit pipelines:

```
export ARGO_SERVER=localhost:2746 ARGO_SECURE=true ARGO_INSECURE_SKIP_VERIFY=true ARGO_NAMESPACE=argo
export ARGO_TOKEN=none        # the CLI requires one; the server runs without auth (see below)
make deploy-dev             # or make ci / make deploy-prod
```

The quick-start Argo Server runs with `--auth-mode server`: anyone who reaches
port 2746 can submit workflows, so keep it on localhost and the SSH tunnel.

## Prerequisites

- Podman, `kind`, `kubectl` and `helm` on the machine that runs the cluster.
- About 8 GB of free RAM (SonarQube alone asks for 2 GB).
- `vm.max_map_count >= 524288` on the host, for SonarQube's Elasticsearch.
  The kind node shares the host kernel, and the chart's privileged init
  container cannot set it from rootless Podman:
  ```
  sysctl vm.max_map_count                       # check
  sudo sysctl -w vm.max_map_count=524288        # if lower
  echo 'vm.max_map_count=524288' | sudo tee /etc/sysctl.d/99-sonarqube.conf
  ```
- **Argo CD reads this directory from GitHub** (`targetRevision: develop`):
  changes under `infra/gitops/` only reach the cluster after they are pushed
  to `develop`. To try a feature branch, change `targetRevision` in
  `bootstrap/root.yaml` and `bootstrap/applications/*.yaml` to that branch.

## Bootstrap

```
make cluster-up       # kind cluster + Argo CD (Helm) + Argo Workflows + root Application
make image-local      # first image: build + push localhost:30500/washflow-api:local-<SHA>
```

`make cluster-up` runs, in order:

```
kind create cluster --name washflow-local --config infra/kind/kind-config.yaml
helm upgrade --install argocd argo-cd --repo https://argoproj.github.io/argo-helm \
  --version 10.11.1 -n argocd --create-namespace \
  -f infra/gitops/infrastructure/argocd/values.yaml --wait
kubectl apply -f infra/gitops/bootstrap/root.yaml
```

From then on Argo CD adopts its own Helm release (`applications/argocd.yaml`)
and syncs the other Applications by wave. The first full sync takes a few
minutes (SonarQube is the slowest). Follow it with:

```
kubectl -n argocd get applications -w
make argocd-password   # admin password for the UI
```

Port mappings in `infra/kind/kind-config.yaml` are fixed when the cluster is
created. On a cluster created before this layout, run
`make cluster-down && make cluster-up`. The old Argo CD install from plain
manifests and the old `washflow-local` Application are replaced by the Helm
release and the root Application (the app now lives in namespace `washflow-dev`).

## Host names (/etc/hosts)

Everything goes through ingress-nginx on host port **8880** (HTTP; 8443 for
HTTPS). Rootless Podman cannot bind ports below 1024. Map the names to the
machine running kind.

**Same machine:**

```
127.0.0.1  argocd.local grafana.local sonarqube.local washflow.local
```

**Another machine on the network** (e.g. a laptop reaching a Linux box over
SSH): use the Linux machine's LAN IP in that laptop's `/etc/hosts`, since kind
publishes 8880 on all interfaces:

```
192.168.x.y  argocd.local grafana.local sonarqube.local washflow.local
```

Or tunnel the port and keep `127.0.0.1` in the laptop's hosts file:

```
ssh -L 8880:localhost:8880 user@linux-box
```

Then open `http://grafana.local:8880` and so on.

On macOS, `.local` names also go through Bonjour/mDNS. If pages take a few
seconds to start loading, add the same names for IPv6 too
(`::1  grafana.local ...`); that skips the mDNS wait.

| UI | Login |
| --- | --- |
| Argo CD | `admin` / `make argocd-password` |
| Grafana | `admin` / `admin` (local only, set in `kube-prometheus-stack/values.yaml`) |
| SonarQube | `admin` / `admin` on first login (it asks for a new password) |

## Analyzing washflow on the local SonarQube

Create a project and a token in http://sonarqube.local:8880, then:

```
./gradlew test sonar -Dsonar.host.url=http://sonarqube.local:8880 \
  -Dsonar.token=<token> -Dsonar.organization=
```

`build.gradle.kts` points at SonarCloud by default; the flags above override
the host and clear the organization (SonarQube Server has none).

## Notes

- **Secrets in Git are local-only placeholders** (`apps/washflow/secret.yaml`,
  `infrastructure/sonarqube/postgres/secret.yaml`, Grafana's `admin/admin`).
  Anything beyond a laptop cluster needs Sealed Secrets / External Secrets.
- **ingress-nginx is retired upstream** (no releases after March 2026; 4.15.1
  is the last chart). It is fine for a local cluster. If you want a maintained
  controller, Traefik or Gateway API (e.g. Envoy Gateway) only needs
  `ingress-nginx.yaml` and the `ingressClassName`s changed.
- **Image pulls from `localhost:30500`** rely on two kind defaults: containerd
  falls back to plain HTTP for `localhost` registries, and the iptables proxy
  mode (set in `kind-config.yaml`) routes NodePorts on 127.0.0.1. If pods sit
  in `ErrImagePull` with an HTTPS error, add a containerd `hosts.toml` for
  `localhost:30500` (kind docs, "Local Registry").
- **Next steps**: point the Sonar step at this SonarQube, report pipeline
  results as GitHub commit statuses, and - if the 2-minute polling delay
  bothers you - swap the trigger for Argo Events with a GitHub webhook.
- On minikube or Docker Desktop instead of kind, drop the kind port mappings and
  reach the NodePorts directly (`minikube ip`:30880, or `localhost:30880` on
  Docker Desktop). Use their default StorageClass (`standard` on minikube,
  `hostpath` on Docker Desktop) in the values files.

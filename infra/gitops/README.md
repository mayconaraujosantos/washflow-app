# Local cluster via Argo CD (GitOps)

Everything on the local kind cluster - ingress, observability (LGTM), SonarQube
and the washflow app - is declared here and synced by Argo CD (App-of-Apps).
Argo Workflows (CI and the Render deploy) is still installed by
`make cluster-up` and is described in [../README.md](../README.md).

```
infra/gitops/
├── bootstrap/
│   ├── root.yaml                  # the only manifest applied by hand
│   └── applications/              # child Applications (one per component)
│       ├── argocd.yaml            # wave -2  Argo CD manages itself
│       ├── ingress-nginx.yaml     # wave -1  ingress controller (NodePort 30880/30443)
│       ├── kube-prometheus-stack.yaml  # 0  Prometheus + Grafana (+ ServiceMonitor CRDs)
│       ├── loki.yaml              # wave 1   logs
│       ├── tempo.yaml             # wave 1   traces
│       ├── sonarqube.yaml         # wave 1   SonarQube + its PostgreSQL
│       ├── alloy.yaml             # wave 2   log shipper DaemonSet -> Loki
│       └── washflow.yaml          # wave 3   the app
├── infrastructure/                # Helm values (and Kustomize where a chart is not enough)
│   ├── argocd/values.yaml
│   ├── ingress-nginx/values.yaml
│   ├── kube-prometheus-stack/values.yaml   # Grafana data sources: Prometheus, Loki, Tempo
│   ├── loki/values.yaml           # single binary, filesystem storage on a PV
│   ├── tempo/values.yaml          # single binary, OTLP on 4317/4318
│   ├── alloy/values.yaml          # Alloy config: pod logs -> Loki, logfmt parsing for washflow
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
| washflow | Kustomize (`apps/washflow`) | `washflow-local` | http://washflow.local:8880 |

## How the app is observed

| Signal | Path |
| --- | --- |
| Logs | stdout -> Alloy DaemonSet (reads pod logs through the Kubernetes API) -> Loki. The `key=value` lines get `level`/`logger` labels and `request_id`/`status` as structured metadata. In Grafana, a `request_id` in a line links to every line of that request. |
| Traces | The OpenTelemetry Java agent is downloaded by an init container and loaded with `JAVA_TOOL_OPTIONS` (no image change) -> OTLP/HTTP -> Tempo. Spans link to the service's logs (`service.name` = Loki `app` label). |
| Metrics | `GET /api/metrics` scraped by Prometheus through a ServiceMonitor that sends the `METRICS_TOKEN` bearer token. |

The React PWA is built into the same image and served by Javalin, so there is
one Deployment, not a separate frontend.

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
make image-local      # build localhost/washflow-api:local and load it into the kind node
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
manifests and the `washflow-local` Application are replaced by the Helm release
and the root Application.

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
- On minikube or Docker Desktop instead of kind, drop the kind port mappings and
  reach the NodePorts directly (`minikube ip`:30880, or `localhost:30880` on
  Docker Desktop). Use their default StorageClass (`standard` on minikube,
  `hostpath` on Docker Desktop) in the values files.

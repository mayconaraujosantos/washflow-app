BACKEND_PORT := 7000
FRONTEND_PORT := 5173

KIND_CLUSTER := washflow-local
KIND_CONFIG := infra/kind/kind-config.yaml
IMAGE := washflow-api:local
GIT_BRANCH := $(shell git rev-parse --abbrev-ref HEAD)

ifeq ($(OS),Windows_NT)
  DETECTED_OS := Windows
else
  DETECTED_OS := $(shell uname -s)
endif

ifeq ($(DETECTED_OS),Windows)
  # kind's podman provider (a Go binary) fails to find podman.exe when the
  # Windows PATH has a malformed entry, which os/exec aborts on outright.
  # This wrapper sanitizes PATH for that one subprocess only. See
  # scripts/win-clean-path.ps1 for details.
  KIND := powershell -NoProfile -ExecutionPolicy Bypass -File scripts/win-clean-path.ps1 kind
else
  KIND := kind
endif

.DEFAULT_GOAL := help
.PHONY: help install dev dev-backend dev-frontend build run start test lint format \
	coverage sonar stop ip clean \
	cluster-up cluster-down cluster-status image-local argocd-install argocd-password \
	argocd-port-forward argo-install argo-port-forward ci deploy-prod

help:
	@echo "Washflow - local commands"
	@echo ""
	@echo "  make dev       Backend (Javalin :$(BACKEND_PORT)) + frontend dev server"
	@echo "                 (Vite :$(FRONTEND_PORT), hot reload) running together."
	@echo "                 Use this while coding. Ctrl+C stops both."
	@echo "  make run       Build the PWA for real and serve it from Javalin on a"
	@echo "                 single origin (:$(BACKEND_PORT)). Use this to test"
	@echo "                 install/offline behavior, including on your phone."
	@echo "  make build     Build the frontend only (web/dist), nothing started."
	@echo "  make test      Run the backend test suite (+ JaCoCo coverage report)."
	@echo "  make lint      Lint + check formatting for both frontend (ESLint/Prettier)"
	@echo "                 and backend (Spotless)."
	@echo "  make format    Auto-format frontend (Prettier) and backend (Spotless)."
	@echo "  make coverage  Print where the JaCoCo HTML coverage report landed."
	@echo "  make sonar     Run a SonarCloud analysis (needs SONAR_TOKEN, see infra/README.md)."
	@echo "  make stop      Best-effort: kill whatever is still holding"
	@echo "                 :$(BACKEND_PORT) / :$(FRONTEND_PORT) (in case Ctrl+C didn't)."
	@echo "  make ip        List LAN IPs to open on your phone (same Wi-Fi)."
	@echo "  make clean     Remove build output (build/, web/dist)."
	@echo ""
	@echo "Local CI/CD (kind + Argo, replaces GitHub Actions):"
	@echo "  make cluster-up          Create the local kind cluster and install Argo CD + Argo Workflows."
	@echo "  make cluster-down        Delete the local kind cluster."
	@echo "  make cluster-status      Show cluster/Argo CD/Argo Workflows health."
	@echo "  make image-local         Build the app image and load it into the kind cluster."
	@echo "  make ci                  Run the CI pipeline (test/lint/build) as an Argo Workflow."
	@echo "  make deploy-prod         Deploy the current 'main' to Railway via an Argo Workflow."
	@echo "  make argocd-password     Print the Argo CD admin password."
	@echo "  make argocd-port-forward Open the Argo CD UI at https://localhost:8081."
	@echo "  make argo-port-forward   Open the Argo Workflows UI at https://localhost:2746."

install:
	cd web && bun install

## Fast iteration: hot reload on both ends. Open http://localhost:$(FRONTEND_PORT)
## in the browser you're coding with. Not representative of the installed PWA -
## use `make run` for that. Vite's own startup log prints a LAN URL too.
dev: install
	@$(MAKE) -j2 dev-backend dev-frontend

dev-backend:
	gradle run --console=plain

dev-frontend:
	cd web && bun run dev

build: install
	cd web && bun run build

## `gradle run` already builds the frontend and serves it (plus /api) from the
## same Jetty instance - that single origin is what makes the manifest/service
## worker installable, so this is the target that matches production.
run:
	@$(MAKE) ip
	@echo "Note: installing the PWA on a phone needs a secure context (HTTPS)."
	@echo "Plain http://<lan-ip> works fine in a phone's browser, but Chrome/Android"
	@echo "will only offer 'Add to Home screen' over HTTPS or on the deployed Railway URL."
	@echo ""
	gradle run --console=plain

start: run

test:
	gradle test --console=plain

lint:
	cd web && bun run lint && bun run format:check
	gradle spotlessCheck --console=plain

format:
	cd web && bun run format
	gradle spotlessApply --console=plain

coverage: test
	@echo "HTML report: build/reports/jacoco/test/html/index.html"

## Needs a SonarCloud token: export SONAR_TOKEN=... first (see infra/README.md).
sonar:
	gradle sonar -Dsonar.token=$(SONAR_TOKEN) --console=plain

stop:
ifeq ($(DETECTED_OS),Windows)
	@powershell -NoProfile -Command 'Get-NetTCPConnection -LocalPort $(BACKEND_PORT),$(FRONTEND_PORT) -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $$_.OwningProcess -Force }; Write-Host "Stopped."'
else
	@lsof -ti :$(BACKEND_PORT) -ti :$(FRONTEND_PORT) 2>/dev/null | xargs kill -9 2>/dev/null; echo "Stopped."
endif

ip:
	@bun scripts/lan-ip.mjs
	@echo "On your phone (same Wi-Fi as this PC): http://<one-of-the-ips-above>:$(BACKEND_PORT)"

clean:
	gradle clean --console=plain
	rm -rf web/dist

## Local CI/CD: kind (via Podman) + Argo CD (GitOps) + Argo Workflows (pipelines).
## Production still deploys to Railway (see `make deploy-prod`); Argo CD only
## manages the local/staging copy of the app in the kind cluster.
cluster-up:
	@echo "==> kind cluster ($(KIND_CLUSTER))"
	@KIND_EXPERIMENTAL_PROVIDER=podman $(KIND) get clusters 2>/dev/null | grep -qx "$(KIND_CLUSTER)" \
		|| KIND_EXPERIMENTAL_PROVIDER=podman $(KIND) create cluster --name $(KIND_CLUSTER) --config $(KIND_CONFIG)
	@echo "==> Argo CD"
	@kubectl get ns argocd >/dev/null 2>&1 || kubectl create namespace argocd
	kubectl apply -n argocd --server-side --force-conflicts \
		-f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
	@echo "==> Argo Workflows"
	@kubectl get ns argo >/dev/null 2>&1 || kubectl create namespace argo
	kubectl apply -n argo --server-side --force-conflicts \
		-f https://github.com/argoproj/argo-workflows/releases/download/v3.7.2/quick-start-minimal.yaml
	kubectl -n argocd rollout status deploy/argocd-server --timeout=180s
	kubectl -n argo rollout status deploy/workflow-controller --timeout=180s
	kubectl -n argo rollout status deploy/argo-server --timeout=180s
	@echo "==> washflow Argo CD Application + Workflow templates"
	kubectl apply -f infra/argocd/application-local.yaml
	kubectl apply -n argo -f infra/argo-workflows/
	@echo ""
	@echo "Cluster ready. Next: 'make image-local' to build+load the app image,"
	@echo "then 'make argocd-port-forward' / 'make argo-port-forward' for the UIs."

cluster-down:
	KIND_EXPERIMENTAL_PROVIDER=podman $(KIND) delete cluster --name $(KIND_CLUSTER)

cluster-status:
	kubectl cluster-info --context kind-$(KIND_CLUSTER)
	@echo ""
	kubectl get pods -n argocd
	@echo ""
	kubectl get pods -n argo
	@echo ""
	kubectl get application -n argocd
	@echo ""
	kubectl get pods -n washflow-local 2>/dev/null || true

## Build the app image locally and load it straight into the kind node
## (no registry involved - Argo CD only tracks the Deployment/Service specs).
image-local:
	podman build -t $(IMAGE) .
	@mkdir -p build
	podman save $(IMAGE) -o build/washflow-api-local.tar
	KIND_EXPERIMENTAL_PROVIDER=podman $(KIND) load image-archive build/washflow-api-local.tar --name $(KIND_CLUSTER)
	@rm -f build/washflow-api-local.tar
	@kubectl -n washflow-local rollout restart deployment/washflow-api 2>/dev/null || true

## Run the CI pipeline (backend tests, frontend lint, Dockerfile build check)
## for the current branch as an Argo Workflow - the local replacement for the
## "quality" + "container" jobs GitHub Actions used to run.
ci:
	argo submit -n argo --from workflowtemplate/washflow-ci -p revision=$(GIT_BRANCH) --watch

## Deploy 'main' to Railway - the local replacement for the GitHub Actions
## "Deploy to Railway" job. Requires the railway-credentials Secret (see
## infra/argo-workflows/README.md).
deploy-prod:
	argo submit -n argo --from workflowtemplate/washflow-deploy-railway -p revision=main --watch

## Uses kubectl's own base64decode template function instead of piping to
## the external `base64` binary, which isn't on PATH in every shell/terminal
## this might run from (e.g. plain cmd.exe).
argocd-password:
	kubectl -n argocd get secret argocd-initial-admin-secret -o go-template='{{.data.password | base64decode}}'
	@echo ""

argocd-port-forward:
	@echo "Argo CD UI: https://localhost:8081  (user: admin, password: make argocd-password)"
	kubectl -n argocd port-forward svc/argocd-server 8081:443

argo-port-forward:
	@echo "Argo Workflows UI: https://localhost:2746"
	kubectl -n argo port-forward svc/argo-server 2746:2746

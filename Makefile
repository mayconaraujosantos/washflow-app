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

# Detect container runtime: Docker first, then Podman as default fallback.
# A `docker` binary can exist with no daemon behind it (e.g. this machine's
# rootless Podman setup), so check that the daemon actually answers rather
# than just that the binary is on PATH.
ifeq ($(OS),Windows_NT)
  HAS_DOCKER := $(shell where docker 2>$null)
  HAS_PODMAN := $(shell where podman 2>$null)
else
  HAS_DOCKER := $(shell docker info >/dev/null 2>&1 && echo yes)
  HAS_PODMAN := $(shell command -v podman 2>/dev/null)
endif

ifeq ($(HAS_DOCKER),)
  ifeq ($(HAS_PODMAN),)
    $(error Nenhum container runtime encontrado. Instale Docker ou Podman.)
  endif
  DOCKER := podman
else
  DOCKER := docker
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

ifeq ($(DETECTED_OS),Windows)
  GRADLE := .\gradlew.bat
else
  GRADLE := ./gradlew
endif

.DEFAULT_GOAL := help
.PHONY: help install dev dev-backend dev-frontend build run start test test-backup lint format generate-auth-secret seed-test-users load-seed load-test \
	coverage sonar stop ip clean db-up db-down db-logs e2e e2e-ci hooks \
	cluster-up cluster-down cluster-status image-local docker-build docker-info \
	argocd-install argocd-password \
	argocd-port-forward evolution-port-forward argo-install argo-port-forward argo-ui-service ci deploy-dev deploy-prod

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
	@echo "  make generate-auth-secret  Generate AUTH_TOKEN_SECRET in the local .env file."
	@echo "  make seed-test-users  Create local-only customer, washer, and manager test accounts."
	@echo "  make load-seed  Create the load-test data (20 lava-jatos, Podman, see docs/load-test.md)."
	@echo "  make load-test  Peak-day load test with a hosting plan's limits"
	@echo "                 (PROFILE=render-starter, HOLD=5m; PROFILES=\"a b\" runs several)."
	@echo "  make test      Run the backend test suite (+ JaCoCo coverage report)."
	@echo "  make test-backup  Back up, drop and restore a throwaway Postgres (db-backup scripts)."
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
	@echo "Docker/Podman (auto-detected, Docker preferred, Podman as fallback):"
	@echo "  make docker-info     Show which container runtime is being used."
	@echo "  make docker-build    Build the Docker image ($(IMAGE)) locally."
	@echo ""
	@echo "Database (Postgres via docker-compose - see infra/README.md):"
	@echo "  make db-up     Start Postgres (:5432) in the background."
	@echo "  make db-down   Stop it (data survives in the postgres-data volume)."
	@echo "  make db-logs   Follow the Postgres container logs."
	@echo ""
	@echo "Local CI/CD (kind + Argo - no GitHub Actions):"
	@echo "  make cluster-up          Create the local kind cluster and install Argo CD + Argo Workflows."
	@echo "  make cluster-down        Delete the local kind cluster."
	@echo "  make cluster-status      Show cluster/Argo CD/Argo Workflows health."
	@echo "  make image-local         Build the app image and push it to the cluster registry (deploys it)."
	@echo "  make ci                  Run the CI pipeline (test/lint/build) as an Argo Workflow (PUSH_IMAGE=true deploys)."
	@echo "  make deploy-prod         Test and deploy 'main' to Render via an Argo Workflow (REVISION=, SKIP_CHECKS=true)."
	@echo "  make argocd-password     Print the Argo CD admin password."
	@echo "  make argocd-port-forward Argo CD UI at http://localhost:8081 (if argocd.local isn't mapped)."
	@echo "  make argo-port-forward   Open the Argo Workflows UI at https://localhost:2746."
	@echo "  make evolution-port-forward Evolution manager (WhatsApp QR) at http://localhost:8082/manager."

install:
	cd web && bun install

generate-auth-secret:
	@bun scripts/generate-auth-secret.mjs

seed-test-users:
	@bun scripts/seed-test-users.mjs

load-seed:
	@scripts/load/run.sh seed

load-test:
	@if [ -n "$(PROFILES)" ]; then scripts/load/matrix.sh $(PROFILES); \
	else scripts/load/run.sh $(or $(PROFILE),render-starter); fi

## Fast iteration: hot reload on both ends. Open http://localhost:$(FRONTEND_PORT)
## in the browser you're coding with. Not representative of the installed PWA -
## use `make run` for that. Vite's own startup log prints a LAN URL too.
dev: install
	@$(MAKE) -j2 dev-backend dev-frontend

dev-backend:
	$(GRADLE) run --console=plain

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
	@echo "will only offer 'Add to Home screen' over HTTPS or on the deployed Render URL."
	@echo ""
	$(GRADLE) run --console=plain

start: run

test:
	$(GRADLE) test --console=plain

test-backup:
	CONTAINER_RUNTIME=$(DOCKER) sh scripts/test-db-backup.sh

lint:
	cd web && bun run lint && bun run format:check
	$(GRADLE) spotlessCheck --console=plain

format:
	cd web && bun run format
	$(GRADLE) spotlessApply --console=plain

coverage: test
	@echo "HTML report: build/reports/jacoco/test/html/index.html"

## Needs a SonarCloud token: export SONAR_TOKEN=... first (see infra/README.md).
## Runs both test suites first so the JaCoCo (backend) and lcov (frontend)
## coverage reports exist when the analysis uploads them.
sonar:
	cd web && bun run test:coverage
	$(GRADLE) test sonar -Dsonar.token=$(SONAR_TOKEN) --console=plain

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
	$(GRADLE) clean --console=plain
	rm -rf web/dist

## Container runtime info: shows which tool (Docker/Podman) is detected.
docker-info:
	@echo "Container runtime: $(DOCKER)"
	@$(DOCKER) --version

## Build the Docker image locally using the auto-detected container runtime.
docker-build:
	@echo "Building image $(IMAGE) with $(DOCKER)..."
	$(DOCKER) build -t $(IMAGE) .
	@echo "Image built successfully: $(IMAGE)"
	@$(DOCKER) images $(IMAGE)

## `docker-compose` here talks to Podman's Docker-API-compatible pipe
## (Podman Desktop's "Docker compatibility" mode) - no podman-compose needed.
db-up:
	docker-compose up -d postgres
	@echo "Postgres on localhost:5432 (db/user/password: washflow) - 'make dev'/'make run' will find it via JdbiFactory's defaults."

db-down:
	docker-compose stop postgres

db-logs:
	docker-compose logs -f postgres

## Versioned git hooks (.githooks) and git-flow (AVH) settings, once per clone:
## pre-commit refuses commits on main/develop (only `git flow ... finish` merges
## land there), pre-push runs the fast checks (formatting, compilation, lint,
## types) so a broken build is caught before it reaches develop/main.
hooks:
	git config core.hooksPath .githooks
	git config gitflow.branch.master main
	git config gitflow.branch.develop develop
	git config gitflow.prefix.feature feature/
	git config gitflow.prefix.bugfix fix/
	git config gitflow.prefix.release release/
	git config gitflow.prefix.hotfix hotfix/
	git config gitflow.prefix.support support/
	git config gitflow.prefix.versiontag v
	git config gitflow.feature.finish.no-ff true
	git config gitflow.bugfix.finish.no-ff true
	git config gitflow.feature.finish.fetch true
	git config gitflow.bugfix.finish.fetch true
	git config gitflow.release.finish.fetch true
	git config gitflow.hotfix.finish.fetch true
	@echo "Git hooks and git flow configured. Check with: git flow config"

## End-to-end journey (web/e2e, Playwright): two lava-jatos set up from
## scratch, managers, washers (PIN and badge QR) and customers (app and
## ticket), each on its own phone. Runs against a throwaway pod on :7100
## (scripts/e2e-env.sh) - never the dev app on :7000 or its database.
## `make e2e` shows the browser, slowed down; videos per person end up in
## web/test-results/people and the report opens with `cd web && bun run e2e:report`.
e2e:
	cd web && bun run e2e

e2e-ci:
	cd web && bun run e2e:ci

## Local CI/CD: kind (via Podman) + Argo CD (GitOps) + Argo Workflows (pipelines).
## Production deploys to Render through Argo Workflows (`make deploy-prod`); Argo
## CD manages the local cluster - ingress, observability (LGTM), SonarQube and
## the app - from infra/gitops (App-of-Apps, see infra/gitops/README.md).
ARGOCD_CHART_VERSION := 10.11.1
cluster-up:
	@echo "==> kind cluster ($(KIND_CLUSTER))"
	@KIND_EXPERIMENTAL_PROVIDER=podman $(KIND) get clusters 2>/dev/null | grep -qx "$(KIND_CLUSTER)" \
		|| KIND_EXPERIMENTAL_PROVIDER=podman $(KIND) create cluster --name $(KIND_CLUSTER) --config $(KIND_CONFIG)
	@echo "==> Argo CD (Helm; afterwards it manages itself from infra/gitops)"
	helm upgrade --install argocd argo-cd --repo https://argoproj.github.io/argo-helm \
		--version $(ARGOCD_CHART_VERSION) --namespace argocd --create-namespace \
		-f infra/gitops/infrastructure/argocd/values.yaml --wait --timeout 5m
	@echo "==> Argo Workflows"
	@kubectl get ns argo >/dev/null 2>&1 || kubectl create namespace argo
	kubectl apply -n argo --server-side --force-conflicts \
		-f https://github.com/argoproj/argo-workflows/releases/download/v3.7.2/quick-start-minimal.yaml
	kubectl -n argo rollout status deploy/workflow-controller --timeout=180s
	kubectl -n argo rollout status deploy/argo-server --timeout=180s
	@# The quick-start's MinIO is unused (see infra/argo-workflows/artifact-repository.yaml)
	@# and its image is no longer pullable: keep it off instead of in ImagePullBackOff.
	kubectl -n argo scale deploy/minio --replicas=0
	@echo "==> GitOps root Application + Workflow templates"
	kubectl apply -f infra/gitops/bootstrap/root.yaml
	kubectl apply -n argo -f infra/argo-workflows/
	@echo ""
	@echo "Cluster ready; Argo CD is now syncing infra/gitops (a few minutes)."
	@echo "Next: 'make image-local' to build+load the app image, then open"
	@echo "http://argocd.local:8880 (admin / 'make argocd-password') - see infra/gitops/README.md."

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
	kubectl get pods -n washflow-dev 2>/dev/null || true

## Build the app image on this machine and push it to the cluster's registry
## (localhost:30500, see infra/gitops/infrastructure/registry) as
## local-<HEAD SHA>; Argo CD Image Updater rolls it out to the dev environment
## (until the next develop build, which is newer). Builds the working tree as
## is, from any branch - handy to try a feature on dev before merging.
## Uses the auto-detected container runtime (Docker or Podman).
REGISTRY := localhost:30500
IMAGE_TAG = local-$(shell git rev-parse HEAD)
image-local:
	$(DOCKER) build -t $(REGISTRY)/washflow-api:$(IMAGE_TAG) -t $(REGISTRY)/washflow-api:latest .
	$(DOCKER) push $(if $(filter podman,$(DOCKER)),--tls-verify=false) $(REGISTRY)/washflow-api:$(IMAGE_TAG)
	$(DOCKER) push $(if $(filter podman,$(DOCKER)),--tls-verify=false) $(REGISTRY)/washflow-api:latest

## Run the CI pipeline (backend tests, frontend lint, build) for the current
## branch as an Argo Workflow - the local replacement for the "quality" +
## "container" jobs GitHub Actions used to run. Pushes are picked up on their
## own by the washflow-ci-trigger CronWorkflow (git flow); this is the manual
## way. PUSH_IMAGE=true also pushes <branch>-<SHA> to the cluster registry
## (deployed to dev only for develop).
PUSH_IMAGE ?= false
ci:
	argo submit -n argo --from workflowtemplate/washflow-ci -p revision=$(GIT_BRANCH) \
		-p pushImage=$(PUSH_IMAGE) --watch

## Deploy a branch to the dev environment (washflow.local) through Argo
## Workflows: runs the CI checks on BRANCH (default: the current one, as pushed
## to GitHub), pushes the image as local-<SHA> and Argo CD Image Updater rolls
## it out. The next develop build replaces it. Works from another machine (e.g.
## the Mac) through the Argo Server - see "Working from another machine" in
## infra/gitops/README.md.
BRANCH ?= $(GIT_BRANCH)
deploy-dev:
	argo submit -n argo --from workflowtemplate/washflow-ci -p revision=$(BRANCH) \
		-p pushImage=true -p tagPrefix=local --watch

## Deploy to production on Render through Argo Workflows: runs the CI checks on
## REVISION (default main), deploys that exact commit through the Render API,
## waits until it is live and checks /api/health. Requires the
## render-credentials Secret (see infra/README.md). SKIP_CHECKS=true redeploys
## without running the tests again.
REVISION ?= main
SKIP_CHECKS ?= false
deploy-prod:
	kubectl apply -n argo -f infra/argo-workflows/
	argo submit -n argo --from workflowtemplate/washflow-deploy-render 		-p revision=$(REVISION) -p skipChecks=$(SKIP_CHECKS) --watch

## Uses kubectl's own base64decode template function instead of piping to
## the external `base64` binary, which isn't on PATH in every shell/terminal
## this might run from (e.g. plain cmd.exe).
argocd-password:
	kubectl -n argocd get secret argocd-initial-admin-secret -o go-template='{{.data.password | base64decode}}'
	@echo ""

## Fallback when argocd.local is not mapped in /etc/hosts.
argocd-port-forward:
	@echo "Argo CD UI: http://localhost:8081  (user: admin, password: make argocd-password)"
	kubectl -n argocd port-forward svc/argocd-server 8081:80

evolution-port-forward:
	@echo "Evolution manager: http://localhost:8082/manager (API key: EVOLUTION_API_KEY in the washflow secret)"
	kubectl -n washflow-dev port-forward svc/washflow-evolution 8082:8080

argo-port-forward:
	@echo "Argo Workflows UI: https://localhost:2746"
	kubectl -n argo port-forward svc/argo-server 2746:2746

## Same as argo-port-forward, but as a systemd user service that starts on
## boot and restarts on its own (infra/systemd/washflow-argo-ui.service) - for
## the machine that acts as the server. Linux only.
argo-ui-service:
	install -D -m 644 infra/systemd/washflow-argo-ui.service $(HOME)/.config/systemd/user/washflow-argo-ui.service
	loginctl enable-linger $(USER)
	systemctl --user daemon-reload
	systemctl --user enable --now washflow-argo-ui.service
	@echo "Argo Workflows UI: https://localhost:2746 (from another machine: ssh -L 2746:localhost:2746 ...)"

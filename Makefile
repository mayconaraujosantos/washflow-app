BACKEND_PORT := 7000
FRONTEND_PORT := 5173

ifeq ($(OS),Windows_NT)
  DETECTED_OS := Windows
else
  DETECTED_OS := $(shell uname -s)
endif

.DEFAULT_GOAL := help
.PHONY: help install dev dev-backend dev-frontend build run start test lint stop ip clean

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
	@echo "  make test      Run the backend test suite."
	@echo "  make lint      Lint the frontend."
	@echo "  make stop      Best-effort: kill whatever is still holding"
	@echo "                 :$(BACKEND_PORT) / :$(FRONTEND_PORT) (in case Ctrl+C didn't)."
	@echo "  make ip        List LAN IPs to open on your phone (same Wi-Fi)."
	@echo "  make clean     Remove build output (build/, web/dist)."

install:
	cd web && bun install

## Fast iteration: hot reload on both ends. Open http://localhost:$(FRONTEND_PORT)
## in the browser you're coding with. Not representative of the installed PWA -
## use `make run` for that. Vite's own startup log prints a LAN URL too.
dev: install
	@$(MAKE) -j2 dev-backend dev-frontend

dev-backend:
	true && gradle run --console=plain

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
	true && gradle run --console=plain

start: run

test:
	true && gradle test --console=plain

lint:
	cd web && bun run lint

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
	true && gradle clean --console=plain
	rm -rf web/dist

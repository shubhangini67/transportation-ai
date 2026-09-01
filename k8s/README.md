# Kubernetes (free, runs on your laptop)

This folder deploys the same TMS stack with Kubernetes:

- PostgreSQL
- Spring Boot API
- ASP.NET Core reports service
- React frontend

Nothing here is paid. Use **Kind** (Kubernetes in Docker) or **Minikube**.

## One-time install (Mac, free)

If these commands are missing, run them in Terminal:

```bash
brew install kubectl kind
```

## Build images, then start the cluster

From the project root:

```bash
# 1. Create a local cluster (once) — maps NodePort 30080 to your Mac
kind create cluster --name tms --config k8s/kind-config.yaml

# 2. Build images
docker build -t tms-backend:local ./tms-backend
docker build -t tms-frontend:local ./tms-frontend
docker build -t tms-dotnet-reports:local ./tms-dotnet-reports

# 3. Load images into Kind
kind load docker-image tms-backend:local --name tms
kind load docker-image tms-frontend:local --name tms
kind load docker-image tms-dotnet-reports:local --name tms

# 4. Apply manifests
kubectl apply -k k8s/

# 5. Wait until pods are Ready
kubectl get pods -n tms -w
```

Open the app: **http://localhost:30080**

Login: `admin` / `admin123`

## Stop

```bash
kind delete cluster --name tms
```

## How the pieces talk

```
Browser → Frontend (Nginx)
              │
              └── /api  → Spring Boot
                            └── insights + dispatch → ASP.NET Core (internal API key)
              PostgreSQL ← Spring Boot only
```

The React app never calls C# directly. That is a normal company pattern (API gateway).

**AI copilot** is a control-room chatbot in Spring Boot (`/api/v1/ai`). Opening the floating bubble or typing `hi` dumps live fleet numbers. No extra pod. Optional: `kubectl -n tms set env deployment/tms-backend GROK_API_KEY=xai-...` (or `GEMINI_API_KEY` / `GROQ_API_KEY`).

Manifests include CPU/memory requests and limits, plus Horizontal Pod Autoscalers (`k8s/hpa.yaml`). On Kind, CPU metrics stay `unknown` until you install metrics-server; the HPA objects are still valid Kubernetes config.

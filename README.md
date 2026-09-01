<p align="center">
  <img src="https://img.shields.io/badge/Transportation_AI-transportation--ai-1F3D32?style=for-the-badge" alt="Transportation AI" />
</p>

<h1 align="center">Transportation AI</h1>

<p align="center">
  by <a href="https://github.com/shubhangini67">Shubhangini</a>
  ·
  <a href="https://github.com/shubhangini67/transportation-ai">github.com/shubhangini67/transportation-ai</a>
</p>

<p align="center">
  <strong>Plan lanes. Assign crew. Track trucks live. Bill the run. Ask Copilot what is delayed.</strong><br />
  <code>transportation-ai</code>
</p>

<p align="center">
  A production-style TMS: <b>Spring Boot</b> is the system of record, <b>React</b> is the control room,
  <b>ASP.NET Core / C#</b> scores dispatch, <b>Kubernetes</b> runs all four services on a laptop.
</p>

<p align="center">
  <a href="https://github.com/shubhangini67/transportation-ai/actions/workflows/ci.yml"><img src="https://github.com/shubhangini67/transportation-ai/actions/workflows/ci.yml/badge.svg" alt="CI" /></a>
  <img src="https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white" alt="Java 17" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/C%23-239120?logo=csharp&logoColor=white" alt="C#" />
  <img src="https://img.shields.io/badge/ASP.NET_Core-8-512BD4?logo=dotnet&logoColor=white" alt="ASP.NET Core 8" />
  <img src="https://img.shields.io/badge/React-18-61DAFB?logo=react&logoColor=black" alt="React 18" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/Kubernetes-Kind-326CE5?logo=kubernetes&logoColor=white" alt="Kubernetes" />
  <img src="https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white" alt="Docker" />
  <img src="https://img.shields.io/badge/license-MIT-informational" alt="MIT" />
</p>

<p align="center">
  <b>Contributor:</b> <a href="https://github.com/shubhangini67">Shubhangini</a>
  &nbsp;·&nbsp;
  Run it on <b>localhost</b> — screenshots below, no hosted deploy
</p>

<p align="center">
  <img src="docs/screenshots/01-login.png" alt="Transportation AI sign-in on localhost" width="920" />
</p>

<p align="center">
  <img src="docs/screenshots/05-copilot-delayed.png" alt="Copilot answering which trips are delayed" width="920" />
</p>

---

## Tech stack — which piece does what

This is the stack map. **Language ≠ framework ≠ job.** Java and C# show on GitHub’s language bar. Spring Boot, ASP.NET Core, and Kubernetes are how those languages are used — they show as **topics** on this repo, not as extra language slices.

<p align="center">
  <img src="docs/diagrams/08-tech-stack.svg" alt="Tech stack grid: Java, Spring Boot, C#, ASP.NET Core, React, PostgreSQL, Kubernetes, Docker" />
</p>

| Tech stack | Kind | Folder / place | What it actually does |
| --- | --- | --- | --- |
| **Java 17** | Language | `tms-backend/` | All product APIs: auth, trips, GPS, invoices, Copilot, geofences |
| **Spring Boot 3.2** | Framework on Java | `tms-backend/` | REST `/api/v1`, Spring Security, JPA, STOMP, OAuth2, Flyway (prod) |
| **C#** | Language | `tms-dotnet-reports/` | Scoring logic only — ranked truck–driver pairs and fleet health |
| **ASP.NET Core 8** | Framework on C# | `tms-dotnet-reports/` | HTTP API Spring calls with `X-Internal-Api-Key` (not a second login) |
| **.NET 8** | Runtime | `tms-dotnet-reports/` | Hosts the ASP.NET Core scoring service |
| **React 18** | UI library | `tms-frontend/src/` | Control room SPA — maps, Copilot widget, RBAC routes |
| **Nginx** | Reverse proxy | frontend Docker image | Serves the SPA; proxies `/api` and `/ws` to Spring Boot |
| **PostgreSQL 16** | Database | Kind / Compose | System of record. Flyway `V1`–`V13` in prod; H2 + seeder in local dev |
| **Kubernetes (Kind)** | Orchestration | `k8s/` | Four pods, NodePort **30080**, ClusterIP for API/C#/Postgres, HPA |
| **Docker** | Packaging | each service `Dockerfile` | One image per process; same images for Compose and Kind |
| **JWT + RBAC** | Auth | Spring Security | Admin / Dispatcher / Driver / Customer; rotating refresh tokens |
| **STOMP / SockJS** | Realtime | Spring + React | Live GPS `/topic/vehicle.*` and notification bell |
| **Leaflet** | Maps | React | India basemap, trip tracking, geofence circles |
| **OpenPDF** | Documents | Spring | Waybill (LR) PDF and invoice PDF |
| **GitHub Actions** | CI | `.github/workflows/` | `mvn test` (Java) + `dotnet build` (C#) |

**Hard rule:** the browser talks only to Nginx and Spring Boot. C# is an internal scoring service. If the .NET pod is down, Assign and Insights still open — Spring has a Java fallback.

---

## Contents

- [Screenshots](#screenshots)
- [Tech stack — which piece does what](#tech-stack--which-piece-does-what)
- [High-level architecture](#high-level-architecture)
- [Low-level architecture](#low-level-architecture)
- [Request flow](#request-flow)
- [Realtime GPS](#realtime-gps)
- [Kubernetes deployment](#kubernetes-deployment)
- [Domain model](#domain-model)
- [Security](#security)
- [Why C# sits next to Spring](#why-c-sits-next-to-spring)
- [What you can demo](#what-you-can-demo)
- [Demo accounts](#demo-accounts)
- [Run it](#run-it)
- [Repository layout](#repository-layout)
- [Contributors](#contributors)
- [License](#license)

---

## Screenshots

Localhost captures of every desk. There is no public deploy — clone the repo and run it.

### Copilot (live)

Green **Live chat**. Copilot answers from real TMS data: delayed runs, shift handover, and who to dispatch.

<table>
  <tr>
    <td width="50%"><img src="docs/screenshots/05-copilot-delayed.png" alt="Copilot delayed trips" /><br /><sub>Ask: “Which trips are delayed?” — overdue Delhi→Jaipur and delayed Kolkata→Bhubaneswar, with Operations / Live tracking links</sub></td>
    <td width="50%"><img src="docs/screenshots/06-copilot-shift-board.png" alt="Copilot shift board" /><br /><sub>Shift board — live trips, SLA exceptions, workshop, on-the-road, best driver</sub></td>
  </tr>
  <tr>
    <td width="50%"><img src="docs/screenshots/07-copilot-assign.png" alt="Copilot dispatch answer" /><br /><sub>Ask: “Who should I dispatch Delhi to Jaipur?” — ranked truck + driver, Smart Dispatch link</sub></td>
    <td width="50%"><img src="docs/screenshots/04-control-room.png" alt="Control room" /><br /><sub>Control room — KPIs, Copilot banner, exception banner, 7-day charts</sub></td>
  </tr>
</table>

### Sign in

<table>
  <tr>
    <td width="50%"><img src="docs/screenshots/01-login.png" alt="Sign in" /><br /><sub>Sign in — Admin, Dispatcher, Driver, Customer one-click desks</sub></td>
    <td width="50%"><img src="docs/screenshots/02-login-mobile.png" alt="Sign in on a phone" /><br /><sub>Same desk on a phone</sub></td>
  </tr>
  <tr>
    <td width="50%"><img src="docs/screenshots/03-register.png" alt="Create an account" /><br /><sub>Create an account</sub></td>
    <td width="50%"><img src="docs/screenshots/31-public-track.png" alt="Public track LANE-DEMO" /><br /><sub>Public track (no login) — token LANE-DEMO</sub></td>
  </tr>
</table>

### Operate

<table>
  <tr>
    <td width="50%"><img src="docs/screenshots/08-exceptions.png" alt="Exceptions" /><br /><sub>Exceptions — overdue start and delayed runs</sub></td>
    <td width="50%"><img src="docs/screenshots/09-live-map.png" alt="Live map" /><br /><sub>Live map — India GPS, status colours</sub></td>
  </tr>
  <tr>
    <td width="50%"><img src="docs/screenshots/10-assign.png" alt="Assign" /><br /><sub>Assign — ASP.NET Core ranked truck–driver pairs</sub></td>
    <td width="50%"><img src="docs/screenshots/15-run-tracking.png" alt="Run tracking" /><br /><sub>Run tracking — live GPS + replay</sub></td>
  </tr>
</table>

### Fleet

<table>
  <tr>
    <td width="50%"><img src="docs/screenshots/11-fleet.png" alt="Fleet" /><br /><sub>Fleet — trucks, status, location</sub></td>
    <td width="50%"><img src="docs/screenshots/12-crew.png" alt="Crew" /><br /><sub>Crew — drivers, license, status</sub></td>
  </tr>
  <tr>
    <td width="50%"><img src="docs/screenshots/12b-crew-add-form.png" alt="Add driver" /><br /><sub>Add driver form</sub></td>
    <td width="50%"><img src="docs/screenshots/13-lanes.png" alt="Lanes" /><br /><sub>Lanes — origin, destination, km, ETA</sub></td>
  </tr>
  <tr>
    <td width="50%"><img src="docs/screenshots/14-runs.png" alt="Runs" /><br /><sub>Runs — planned / in progress / completed</sub></td>
    <td width="50%"><img src="docs/screenshots/16-workshop.png" alt="Workshop" /><br /><sub>Workshop — overdue and due-soon service</sub></td>
  </tr>
  <tr>
    <td width="50%"><img src="docs/screenshots/17-zones.png" alt="Zones" /><br /><sub>Zones — circular geofences on India map</sub></td>
    <td width="50%"><img src="docs/screenshots/29-profile.png" alt="Profile" /><br /><sub>My profile</sub></td>
  </tr>
</table>

### Trade

<table>
  <tr>
    <td width="50%"><img src="docs/screenshots/18-bookings.png" alt="Bookings" /><br /><sub>Bookings</sub></td>
    <td width="50%"><img src="docs/screenshots/19-waybills.png" alt="Waybills" /><br /><sub>Waybills (LR) — PDF download</sub></td>
  </tr>
  <tr>
    <td width="50%"><img src="docs/screenshots/20-costs.png" alt="Costs" /><br /><sub>Costs — fuel, toll, allowance</sub></td>
    <td width="50%"><img src="docs/screenshots/21-billing.png" alt="Billing" /><br /><sub>Billing — invoices generated from trip expenses</sub></td>
  </tr>
  <tr>
    <td width="50%"><img src="docs/screenshots/22-tariffs.png" alt="Tariffs" /><br /><sub>Tariffs — ₹/km rate cards + GST quote</sub></td>
    <td width="50%"></td>
  </tr>
</table>

### Insight

<table>
  <tr>
    <td width="50%"><img src="docs/screenshots/23-insights.png" alt="Insights" /><br /><sub>Insights — trips, C# fleet health, charts</sub></td>
    <td width="50%"><img src="docs/screenshots/24-fuel.png" alt="Fuel" /><br /><sub>Fuel — spend, ₹/km by truck</sub></td>
  </tr>
  <tr>
    <td width="50%"><img src="docs/screenshots/25-scorecards.png" alt="Crew scores" /><br /><sub>Crew scores — on-time band and delay count</sub></td>
    <td width="50%"></td>
  </tr>
</table>

### Access (admin)

<table>
  <tr>
    <td width="50%"><img src="docs/screenshots/26-access.png" alt="Access" /><br /><sub>Access — users and roles</sub></td>
    <td width="50%"><img src="docs/screenshots/27-audit.png" alt="Activity" /><br /><sub>Activity — audit log</sub></td>
  </tr>
  <tr>
    <td width="50%"><img src="docs/screenshots/28-webhooks.png" alt="Hooks" /><br /><sub>Hooks — outbound events</sub></td>
    <td width="50%"></td>
  </tr>
</table>

### Driver desk

<table>
  <tr>
    <td width="50%"><img src="docs/screenshots/30-driver-desk.png" alt="Driver my runs" /><br /><sub>My runs — start / complete only your trips</sub></td>
    <td width="50%"><img src="docs/screenshots/30b-driver-live-map.png" alt="Driver live map" /><br /><sub>Driver live map</sub></td>
  </tr>
</table>

### Customer desk

<table>
  <tr>
    <td width="50%"><img src="docs/screenshots/33-customer-bookings.png" alt="Customer bookings" /><br /><sub>Customer bookings</sub></td>
    <td width="50%"><img src="docs/screenshots/33b-customer-waybills.png" alt="Customer waybills" /><br /><sub>Customer waybills</sub></td>
  </tr>
  <tr>
    <td width="50%"><img src="docs/screenshots/33c-customer-runs.png" alt="Customer runs" /><br /><sub>Customer runs</sub></td>
    <td width="50%"><img src="docs/screenshots/32-public-track-live.png" alt="Public track LIVE-DEMO" /><br /><sub>Public track — token LIVE-DEMO</sub></td>
  </tr>
</table>

---

## High-level architecture

Four processes. React never calls C# directly.

<p align="center">
  <img src="docs/diagrams/01-high-level.svg" alt="High-level architecture: React to Nginx to Spring Boot and PostgreSQL, with ASP.NET Core scoring" />
</p>

| Process | Folder | Stack | Job |
| --- | --- | --- | --- |
| Control room UI | `tms-frontend/` | React 18 + Nginx | Pages, maps, Copilot, RBAC |
| API / system of record | `tms-backend/` | Java 17 + Spring Boot 3.2 | JWT, trips, GPS, invoices, Copilot, WebSocket |
| Dispatch engine | `tms-dotnet-reports/` | C# + ASP.NET Core 8 | Rank assignments; fleet insight scores |
| Data | Postgres (prod) / H2 (dev) | SQL + Flyway | Persistence |
| Cluster | `k8s/` | Kubernetes (Kind) | Same four services on `localhost:30080` |

---

## Low-level architecture

Client → Nginx → security filters → thin controllers → services → JPA, plus side calls for scoring, sockets, geofences, Copilot, and PDFs.

<p align="center">
  <img src="docs/diagrams/02-low-level.svg" alt="Low-level layered architecture of Transportation AI" />
</p>

**Backend contract**

- Controllers stay thin. They return `ApiResponse.ok(data)` / `ApiResponse.created(data)`.
- Services map entities → response DTOs by hand. JPA entities never leave the API.
- Every new table extends `Auditable` except `AuditLog` and `RefreshToken`.
- Errors go through `GlobalExceptionHandler` (`ResourceNotFoundException`, `BadRequestException`, `DuplicateResourceException`).

**Frontend contract**

- One Axios instance attaches JWT + `X-Tenant-ID` and unwraps `ApiResponse`.
- On 401 it queues a refresh against `/auth/refresh`, then retries.
- Pages in `src/pages/` call thin modules in `src/services/`.

---

## Request flow

Assign is the polyglot path: Spring loads the snapshot, **C# / ASP.NET Core** ranks it, Spring writes the trip.

<p align="center">
  <img src="docs/diagrams/03-request-flow.svg" alt="Sequence diagram for assigning a run through Spring Boot and ASP.NET Core" />
</p>

Insights uses the same gateway: Spring → `POST /api/v1/insights/fleet` on ASP.NET Core.

---

## Realtime GPS

Each ping writes history, fans out on STOMP, then checks circular geofences (Haversine).

<p align="center">
  <img src="docs/diagrams/04-realtime.svg" alt="GPS ingest, persist, broadcast, geofence, and alert pipeline" />
</p>

---

## Kubernetes deployment

Kind cluster name: `tms`. Browser hits NodePort **30080**. Postgres keeps a volume — do not bounce that pod on a live demo.

<p align="center">
  <img src="docs/diagrams/05-kubernetes.svg" alt="Kind cluster topology with frontend NodePort, backend, postgres, and ASP.NET Core" />
</p>

Manifests in `k8s/`: namespace, ConfigMap, Secret, postgres, backend, frontend, dotnet-reports, HPA, `kind-config.yaml`.

---

## Domain model

Trip is the hub. Completing a run frees the truck. **C# owns no tables** — it scores a snapshot Spring already loaded.

<p align="center">
  <img src="docs/diagrams/06-domain.svg" alt="Domain model with Trip at the centre of vehicles, bookings, invoices, and GPS" />
</p>

---

## Security

Stateless JWT at Spring, method-level roles, tenant header, cluster secret to C# (not a user JWT).

<p align="center">
  <img src="docs/diagrams/07-security.svg" alt="Security pipeline from login through JWT, tenant filter, RBAC, and internal API key" />
</p>

| Rule | Detail |
| --- | --- |
| Roles | `ADMIN`, `DISPATCHER`, `DRIVER`, `CLIENT` (shown as Admin / Dispatcher / Driver / Customer) |
| Deletes | ADMIN only |
| Public | `/api/v1/auth/**`, `/api/v1/public/**`, Swagger, uploads, WS handshake |
| Tenancy | `TenantFilter` → `TenantContext` → `tenant_id` on rows |
| Refresh | Rotating `RefreshToken`; Axios retries once |
| C# | `X-Internal-Api-Key` from Kubernetes Secret / Compose env |

---

## Why C# sits next to Spring

Spring Boot owns login, persistence, and every screen’s API. **Assign** and **Insights** POST a live fleet snapshot to a small **ASP.NET Core 8** service written in **C#** that scores pairs (capacity, status, origin) and fleet health.

Polyglot microservice: **Java for the product, C# for the scoring engine** (`tms-dotnet-reports/`).

Interview line: *the UI never talks to .NET; Spring is the gateway.*

---

## What you can demo

| Area | Stack involved | What it does |
| --- | --- | --- |
| Control room | React + Spring | KPIs, 7-day charts, exception banner |
| Copilot | Spring (`/api/v1/ai`) | Live ops answers; can fill crew / fleet / lane forms |
| Assign | Spring + **C# / ASP.NET** | Ranked truck–driver plan → create the run |
| Live map | React Leaflet + STOMP | India GPS, status colours |
| Fleet / Crew / Lanes / Runs | Spring JPA | Full CRUD; trip lifecycle locks the truck |
| Waybills & POD | OpenPDF + GPS | LR PDF, GPS-stamped proof of delivery |
| Billing | Spring | Invoice from trip costs, status + PDF |
| Zones | Spring Haversine | Circular geofences, enter/exit alerts |
| Fuel & scores | Spring aggregation | Cost per km, on-time bands |
| Workshop | Spring | Odometer / calendar maintenance alerts |
| Public track | Spring public API | No login — `LANE-DEMO` or `LIVE-DEMO` |

---

## Demo accounts

Click the card on `/login`, or type:

| Role | User | Password | Sees |
| --- | --- | --- | --- |
| Admin | `admin` | `admin123` | Everything, including Access |
| Dispatcher | `dispatcher` | `dispatch123` | Ops, not Access |
| Driver | `driver1` | `driver123` | My runs, map, trucks |
| Customer | `client1` | `client123` | Bookings, waybills, status |

Public tracking tokens (no login): **LANE-DEMO** (Ahmedabad → Surat), **LIVE-DEMO** (Chennai → Madurai).

---

## Run it

### Option A — Kind (full stack, Kubernetes)

```bash
brew install kubectl kind   # once

kind create cluster --name tms --config k8s/kind-config.yaml

docker build -t tms-backend:local ./tms-backend
docker build -t tms-frontend:local ./tms-frontend
docker build -t tms-dotnet-reports:local ./tms-dotnet-reports

kind load docker-image tms-backend:local --name tms
kind load docker-image tms-frontend:local --name tms
kind load docker-image tms-dotnet-reports:local --name tms

kubectl apply -k k8s/
kubectl get pods -n tms -w
```

Open **http://localhost:30080**

### Option B — Docker Compose

```bash
docker compose up --build
```

Frontend **:80**, API **:8080**, C# **:5080**, Postgres **:5432**.

### Option C — local dev (no Docker)

```bash
# API — H2, auto-seed, Flyway off
cd tms-backend && mvn spring-boot:run          # http://localhost:8080

# C# scoring (optional; Spring falls back if this is down)
cd tms-dotnet-reports && dotnet run --urls http://localhost:5080

# UI
cd tms-frontend && npm install && npm start    # http://localhost:3000
```

Swagger: `http://localhost:8080/swagger-ui.html`

---

## Repository layout

```
transportation-ai/
├── tms-backend/              Java 17 + Spring Boot 3.2 API
├── tms-frontend/             React 18 SPA + Nginx image
├── tms-dotnet-reports/       C# / ASP.NET Core 8 dispatch + insights
├── k8s/                      Kubernetes (Kind) manifests
├── docs/diagrams/            Architecture + tech-stack SVGs
├── docs/screenshots/         Localhost UI captures
├── docker-compose.yml
├── CONTRIBUTORS.md
└── .github/                  CODEOWNERS + CI
```

---

## Contributors

Sole contributor: **[Shubhangini](https://github.com/shubhangini67)** (`@shubhangini67`).

See [CONTRIBUTORS.md](CONTRIBUTORS.md).

---

## License

MIT — © 2026 [Shubhangini](https://github.com/shubhangini67). See [LICENSE](LICENSE).

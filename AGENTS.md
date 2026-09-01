# AGENTS.md — Transportation AI (`transportation-ai`)

## Architecture

Monorepo with two independent stacks communicating over REST:

- **`tms-backend/`** — Spring Boot 3.2 (Java 17), Spring Security + JWT, Spring Data JPA, Flyway migrations
- **`tms-frontend/`** — React 18 SPA (Create React App), React Router v6, Context API + useReducer for auth, Axios

All API endpoints live under `/api/v1/`. In Docker, Nginx reverse-proxies `/api/` to the backend (see `tms-frontend/nginx.conf`). In local dev, the frontend hits `http://localhost:8080/api/v1` directly (configured in `src/api/axios.js`).

## Development Setup

```bash
# Backend (H2 in-memory, no Flyway, auto-seeds demo data via DataSeeder)
cd tms-backend && mvn spring-boot:run          # http://localhost:8080

# Frontend
cd tms-frontend && npm install && npm start    # http://localhost:3000

# Full stack with PostgreSQL
docker compose up --build                      # Frontend :80, Backend :8080, Postgres :5432
```

**Profiles:** `dev` (default) uses H2 + `create-drop` DDL + DataSeeder. `prod` uses PostgreSQL + Flyway + `validate` DDL. Profile is set via `SPRING_PROFILES_ACTIVE` (defaults to `dev` in `application.yml`).

**Test users:** `admin/admin123` (ADMIN), `dispatcher/dispatch123` (DISPATCHER), `driver1/driver123` (DRIVER), `client1/client123` (CLIENT).

## Backend Patterns

**Layering:** `Controller → Service → Repository`. Controllers are thin — they delegate to services and wrap results in `ApiResponse.ok(data)` or `ApiResponse.created(data)`. Never return JPA entities directly; always convert to response DTOs.

**Entity → DTO mapping** is done manually in service classes via `toResponse()` methods (no MapStruct). Request DTOs use Jakarta Validation annotations (`@NotNull`, `@Size`, etc.) and are validated with `@Valid` on controller params.

**All entities extend `Auditable`** (`tms-backend/.../entity/Auditable.java`) which provides `createdAt`, `updatedAt`, `createdBy`, `updatedBy` via JPA auditing. New entities must extend this class and use `@SuperBuilder`. Exceptions: `AuditLog` and `RefreshToken` are standalone entities — they use plain `@Builder`, not `@SuperBuilder`, and do not extend `Auditable`. `SpringSecurityAuditorAware` (`config/SpringSecurityAuditorAware.java`) provides the `createdBy`/`updatedBy` auditor from the Security context.

**Primary keys:** Most entities use `UUID` (`@GeneratedValue(strategy = GenerationType.UUID)`). Exception: `Route` and `Booking` use `BIGSERIAL` (Long).

**Enums are stored as strings** (`@Enumerated(EnumType.STRING)`). All status/type enums live in `com.tms.enums/`.

**Global error handling** via `GlobalExceptionHandler` (`@RestControllerAdvice`). Use the project's custom exceptions: `ResourceNotFoundException`, `BadRequestException`, `DuplicateResourceException`. Do not throw raw `RuntimeException`.

**Security:** `SecurityConfig` defines URL-level rules; controllers add `@PreAuthorize("hasAnyRole(...)")` for method-level RBAC. The 4 roles are `ADMIN`, `DISPATCHER`, `DRIVER`, `CLIENT`. Only ADMIN can DELETE. JWT filter chain: `JwtAuthenticationFilter → JwtTokenProvider → CustomUserDetailsService`.

**Flyway migrations** go in `src/main/resources/db/migration/` with naming `V{N}__{description}.sql` (sub-versions like `V3.1__` are allowed). Currently 9 files: V1 through V8 (with V3.1). V5 adds advanced-feature tables (vehicle_location_history, invoices, invoice_items, notifications, file_attachments). V6 fixes schema mismatches and adds missing indexes; V7 backfills route_id on demo trips and seeds data for advanced-feature tables. V8 adds geofences, geofence_events tables and OAuth2 columns to users. Only active when `spring.flyway.enabled=true` (prod). In dev, the `DataSeeder` (`config/DataSeeder.java`) seeds demo data programmatically via `CommandLineRunner`, guarded by `@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "false")`.

**Multi-tenancy:** `TenantFilter` (`config/TenantFilter.java`) extracts the `X-Tenant-ID` header, validates the tenant exists and is active, then sets `TenantContext` (ThreadLocal). Most entities have a `tenantId` column. The frontend Axios instance auto-attaches `X-Tenant-ID` from `localStorage`. Admin-only `TenantController` manages tenants.

**File uploads:** `FileStorageService` handles avatar/file uploads. Uploaded files are served via `/uploads/**` (configured in `WebConfig.java`). `WebConfig` also configures CORS via `app.cors.allowed-origins`.

**File attachments:** `FileAttachmentService` and `FileAttachmentController` handle polymorphic file attachments (entity_type + entity_id). Supports TRIP, VEHICLE, LR entity types. Upload, list, download, and delete endpoints under `/api/v1/attachments`.

**Invoices:** `InvoiceService` + `InvoiceController` provide full CRUD, auto-generation from trip expenses (`POST /invoices/generate/{tripId}`), status lifecycle (DRAFT → SENT → PAID → CANCELLED), and PDF export via `InvoicePdfService`. Admin and Dispatcher roles only.

**Notifications:** `NotificationService` + `NotificationController` provide in-app notifications per user. Endpoints: list (paginated), unread count, mark as read, mark all read. `NotificationBell.jsx` in the Navbar shows real-time unread count. Types: TRIP_UPDATE, BOOKING_UPDATE, INVOICE_UPDATE, SYSTEM, ALERT, GEOFENCE_ALERT.

**WebSocket:** `WebSocketConfig` (`config/WebSocketConfig.java`) enables STOMP over SockJS at `/ws`. JWT authentication is validated on CONNECT via a channel interceptor. Used for real-time GPS location updates (`/app/location.update` → `/topic/vehicle.{vehicleId}`) and notification push (`/user/queue/notifications`). Frontend uses `@stomp/stompjs` + `sockjs-client` (see `websocketService.js`).

**Vehicle GPS tracking:** `VehicleLocationService` + `VehicleLocationController` handle GPS updates via both REST (`POST /vehicles/{vehicleId}/location`) and WebSocket (`@MessageMapping("/location.update")`). `VehicleLocationHistory` entity stores breadcrumb trail. Historical route retrieval: `GET /trips/{tripId}/route-history` and `GET /vehicles/{vehicleId}/location-history`. On each GPS update, `GeofenceService.checkGeofences()` is called to detect boundary crossings.

**Geofencing:** `Geofence` entity (circular zones: lat, lng, radiusMeters) + `GeofenceEvent` entity (enter/exit log). `GeofenceService` provides CRUD for zones and `checkGeofences()` which uses the Haversine formula to compute distance from each GPS point to all active geofence centers. When a vehicle crosses a boundary, a `GeofenceEvent` is created and `NotificationService.createAndPush()` alerts all ADMINs and DISPATCHERs with `GEOFENCE_ALERT` type. Types: `DEPOT`, `RESTRICTED_ZONE`, `DELIVERY_ZONE`, `CUSTOM`. `GeofenceController` under `/api/v1/geofences` — Admin and Dispatcher roles only. Frontend: `GeofenceList.jsx` with Leaflet `Circle` overlays and inline form.

**Fuel Analytics:** `FuelAnalyticsService` + `FuelAnalyticsController` (`/api/v1/analytics/fuel`). Read-only analytics computed from existing `Expense` (category=FUEL) joined with `Trip → Route` (distance). Endpoints: `GET /` (overall fuel analytics with cost-per-km breakdowns, monthly trend, per-vehicle breakdown) and `GET /vehicle/{vehicleId}` (single-vehicle detail). Response DTOs: `FuelAnalyticsResponse`, `VehicleFuelBreakdown`, `FuelTrendPoint`. No new tables — purely aggregation queries. Admin and Dispatcher roles only. Frontend: `FuelAnalytics.jsx` page with stat cards, Recharts BarCharts, and breakdown table.

**SSO / OAuth2:** Spring Security OAuth2 Client (`spring-boot-starter-oauth2-client`) for Google and Microsoft authorization code flow. Config in `application.yml` under `spring.security.oauth2.client.registration.{google,microsoft}`. `SecurityConfig` adds `.oauth2Login()` with custom `OAuth2LoginSuccessHandler`. On success, `OAuth2AuthenticationService.processOAuth2Login()` finds or creates a `User` (auto-links by email), issues JWT + refresh token via existing `JwtTokenProvider` + `RefreshTokenService`, then redirects to `${app.oauth2.frontend-redirect-url}` with tokens as query params. `User` entity has `oauthProvider` and `oauthProviderId` columns. `AuthController` exposes `GET /auth/oauth2/providers` which returns enabled providers (only if client-id ≠ "placeholder"). Frontend: Login page conditionally renders "Sign in with Google/Microsoft" buttons; `OAuthCallback.jsx` page extracts tokens from URL and calls `loginWithOAuth()` in `AuthContext`. `PasswordEncoder` bean is extracted to `PasswordEncoderConfig` to avoid circular dependency with `SecurityConfig`.

**AI copilot (free):** `AiCopilotService` + `AiCopilotController` (`/api/v1/ai`). Typing `hi` returns a live shift handover (trips, SLA exceptions, workshop, GPS) — never small-talk. Optional wording polish via `LlmGateway`: Grok (`GROK_API_KEY` / `XAI_API_KEY`), Gemini, Groq, then Ollama. Demo works with no keys. Frontend: full page `/ai` plus a floating widget on every other page (`FloatingCopilot.jsx`) for ADMIN/DISPATCHER.

**Adding a new resource** requires: Entity (extends Auditable) → Request DTO → Response DTO → Repository → Service (with `toResponse()`) → Controller (with `@PreAuthorize`, `@Tag`, `@Operation`) → Flyway migration (for prod).

## Frontend Patterns

**Service layer:** Each backend resource has a thin service module in `src/services/` (e.g., `tripService.js`) that wraps Axios calls. The shared Axios instance (`src/api/axios.js`) auto-attaches the JWT from `localStorage` and **unwraps the `ApiResponse` wrapper** — so `response.data` is already the inner `data` payload, not `{ success, data }`. The base URL defaults to `http://localhost:8080/api/v1` and is overridable via the `REACT_APP_API_BASE_URL` env var (set to `/api/v1` in Docker via `docker-compose.yml` build arg).

**Silent refresh & tenant header:** The Axios response interceptor catches 401s and attempts token refresh via `/auth/refresh` with a queue for concurrent failures. On refresh failure it clears `localStorage` and redirects to `/login`. The request interceptor also attaches `X-Tenant-ID` from `localStorage` on every request.

**Page structure:** List pages (`*List.jsx`) use the shared `DataTable` component + `Modal` for create/edit forms (`*Form.jsx`) + `ConfirmDialog` for deletes. List pages own all state and pass callbacks down. Server-side pagination params are `page` (0-based) and `size`.

**Auth/routing:** `AuthContext` (`src/context/AuthContext.js`) manages auth state via `useReducer`. Routes are guarded by `ProtectedRoute` which takes an optional `roles` array. Check `src/App.js` for the full route → role mapping.

**Provider nesting order** (see `src/index.js`): `BrowserRouter → I18nProvider → ThemeProvider → AuthProvider → App + ToastContainer`. Maintain this order when adding new providers.

**Dark mode:** `ThemeContext` (`src/context/ThemeContext.js`) toggles `document.documentElement.dataset.theme` between `light`/`dark` and persists to `localStorage`. CSS custom properties in `src/styles/global.css` handle theming.

**i18n:** `I18nProvider` (`src/i18n/I18nProvider.js`) wraps the app with `react-intl`. Locale files live in `src/i18n/messages/{en,hi}.json`. Current locale is persisted to `localStorage`. Use `intl.formatMessage({ id: 'key' })` for translations.

**Styling:** Plain CSS modules in `src/styles/` (global.css, layout.css, forms.css, components.css, dashboard.css). No CSS-in-JS or Tailwind. Use existing CSS classes.

**Common components:** `DataTable` (paginated tables), `Modal` (dialog wrapper), `ConfirmDialog` (delete confirmation), `FormField` (label + input + error wrapper), `StatusBadge` (color-coded status display), `ProtectedRoute` (role guard), `AttachmentSection` (reusable file attachment widget for any entity). All in `src/components/common/`. `NotificationBell` (real-time unread count badge) is in `src/components/layout/`.

**Notifications:** Use `react-toastify` (`toast.success()`, `toast.error()`).

## Key Integration Points

- **Trip lifecycle** (`PLANNED → IN_PROGRESS → COMPLETED`) automatically toggles vehicle status (`AVAILABLE ↔ BUSY`) — see `TripService.updateTripStatus()`. This is the most complex business logic in the app.
- **Booking seat validation** checks remaining vehicle capacity before confirming — see `BookingService`.
- **Refresh token rotation** — `RefreshTokenService` creates, verifies, rotates, and revokes tokens. Frontend Axios interceptor handles silent refresh on 401 with a queue for concurrent requests (see `src/api/axios.js`).
- **Multi-tenant data isolation** — `TenantFilter` validates `X-Tenant-ID` header → sets `TenantContext` (ThreadLocal) → entities store `tenantId` column. Frontend auto-attaches the header from `localStorage`.
- **Invoice auto-generation** — `InvoiceService.generateFromTrip()` aggregates trip expenses into invoice line items with tax calculation. Status lifecycle: DRAFT → SENT → PAID → CANCELLED.
- **Real-time GPS tracking** — `VehicleLocationService` processes location updates from both REST and WebSocket, stores breadcrumb trail in `VehicleLocationHistory`, and broadcasts via STOMP `/topic/vehicle.{vehicleId}`.
- **Geofence detection** — On each GPS update, `GeofenceService.checkGeofences()` computes Haversine distance to all active geofence centers. Boundary crossings create `GeofenceEvent` records and push `GEOFENCE_ALERT` notifications to ADMIN/DISPATCHER users.
- **Fuel Analytics** — `FuelAnalyticsService` aggregates `Expense` (category=FUEL) with `Trip → Route` (distance) to compute cost-per-km, monthly trends, and per-vehicle breakdowns. Read-only — no new tables.
- **SSO / OAuth2** — `OAuth2AuthenticationService` processes Google/Microsoft OAuth2 logins, auto-creates or links `User` accounts by email, and issues JWT + refresh tokens. `OAuth2LoginSuccessHandler` redirects to the frontend with tokens as query params.
- **AI copilot** — `AiCopilotService` classifies the question, retrieves live TMS facts (ops, GPS, maintenance, C# dispatch), and answers with on-device RAG. Optional Ollama rewrite via `OLLAMA_URL`; no paid APIs.
- **Notification push** — `NotificationService` creates in-app notifications and pushes them to connected users via STOMP `/user/queue/notifications`. `NotificationBell.jsx` in the Navbar shows real-time unread count.
- **File attachments** — `FileAttachmentService` supports polymorphic uploads (TRIP, VEHICLE, LR entity types). `AttachmentSection.jsx` provides a reusable drag-and-drop widget.
- **Webhook dispatch** uses Spring Retry (`AsyncRetryConfig`) to push events to registered URLs.
- **LR PDF export** uses OpenPDF (`LrPdfService`). **Invoice PDF export** uses OpenPDF (`InvoicePdfService`).
- **Email** via Spring Boot Mail starter, toggled by `app.email.enabled` property.
- **Audit logging** — `AuditLogService.log()` is `@Async`; serializes old/new values to JSON via `ObjectMapper`.

## Testing

```bash
cd tms-backend && mvn test           # Spring Boot tests with H2
cd tms-frontend && npm test          # React testing (Jest + RTL)
```


# WedFlow — Wedding Planning and Vendor Marketplace

A wedding-planning platform for couples, service vendors and administrators.

## Source modules

| Module | Implementation |
|---|---|
| `backend/` | Java 21, Spring Boot, PostgreSQL, Flyway, bearer-token authentication, Razorpay API/webhooks, notification outbox |
| `web/` | New responsive dependency-free JavaScript web application: customer, vendor and administrator experiences |
| `docs/` | Architecture/deployment/ER diagrams, schema index, guarded migration script, maintenance/KT and UAT matrix |

**Release state:** Development implementation; **not UAT or production certified**. In particular, no live deployed frontend/browser screenshots, successful Maven/PostgreSQL CI report, full payment/refund reconciliation or vendor payouts have been verified. See [full screen-by-screen review](docs/SCREEN-BY-SCREEN-UAT-2026-11-10.md) and [backend deployment blockers](backend/DEPLOYMENT-BLOCKERS.md).

## Local development

Prerequisites: Java 21, Maven 3.9+, PostgreSQL and Python 3 or any static web server.

1. Create a PostgreSQL database `wedflow`. Set `DATABASE_URL` to the JDBC URL and `DATABASE_USERNAME`/`DATABASE_PASSWORD` for an app-specific database user.
2. Set `WEDFLOW_ALLOWED_ORIGINS=http://localhost:4173` on the backend. Supply provider keys only through runtime environment, never in browser code.
3. Run the backend from `backend/`: `mvn spring-boot:run`. Flyway applies versioned migrations on startup after reviewing database migration status.
4. Set `window.WEDFLOW_API_BASE = "http://localhost:8080";` in `web/config.js` for local development.
5. Run `python3 -m http.server 4173 --directory web` from repo root and open http://localhost:4173.
6. Run `cd web && npm run verify` for the dependency-free JS syntax/unit suite, and `cd backend && mvn -B clean verify` for Java tests and JaCoCo.

## Web features

- Login/registration, dashboard and wedding selection.
- Ceremonies, guests and RSVP, tasks, budget allocations, catering menus/items.
- Approved vendor discovery, enquiries, quotation acceptance and booking cancellation.
- Payment intents, Razorpay checkout initiation and verified/pending/failed status (server remains authority); refunds now require administrator approval after cancellation, with a queued provider workflow and reconciliation state.
- Vendor business setup, services, enquiries, quotations and bookings.
- Admin approval/rejection, PII-free operational KPIs, and cancellation refund decisions with deduction and status queues.
- Notification inbox, safe API error messages with correlation ID, and optional short-lived geolocation capture for mutation audit.

## Production configuration and limits

Use HTTPS backend and frontend URLs; restrict CORS; review `web/_headers` CSP against actual Razorpay checkout behavior before go-live. `web/config.js` contains only public API routing configuration, never secrets. Frontend auth token is stored in **sessionStorage**, not persistent storage; backend validates ownership/roles for every operation.

The current product does **not** yet provide complete password recovery/email verification, full planner collaboration, vendor payout settlements, browser/device certification, or automated provider reconciliation. A controlled one-time SUPER_ADMIN bootstrap is available via runtime-only secrets. Consult the [release status](backend/RELEASE-STATUS.md), [operations runbook](docs/architecture-kt/DEPLOYMENT_MAINTENANCE_KT.md), [schema catalogue](docs/architecture-kt/SCHEMA_INDEX.md), and [manual verification workflow](.github/workflows/manual-backend-verify.yml).

The GitHub Actions workflow runs **only when manually dispatched**; it does not consume actions minutes on ordinary pushes.

## Latest 10 November 2026 technical package

**12 Flyway versions**: V11 adds admin-only queued refunds, deduction amounts, idempotency and reconciliation metadata; V12 adds notification worker lease recovery and backoff. The customer page no longer grants self-approved refunds. Refined vendor search and safe machine-readable HTTP status responses. Controlled one-time admin bootstrap uses `WEDFLOW_BOOTSTRAP_ADMIN_EMAIL` and `WEDFLOW_BOOTSTRAP_ADMIN_PASSWORD` supplied securely; remove them after the first approved provisioning.

[Architecture & booking/refund sequence](docs/architecture-kt/SYSTEM_ARCHITECTURE.md) · [Deployment diagrams](docs/architecture-kt/DEPLOYMENT_TOPOLOGIES.md) · [Complete logical ER](docs/architecture-kt/ER_FULL_DOMAIN.md) · [Schema/migrations](docs/architecture-kt/SCHEMA_INDEX.md) · [Safe Flyway script](docs/architecture-kt/migrate-safe.sh) · [Maintenance and KT demo](docs/architecture-kt/DEPLOYMENT_MAINTENANCE_KT.md).

**Current release gate remains BLOCKED:** see [deployment blockers](backend/DEPLOYMENT-BLOCKERS.md). No new database migration, production push or financial provider test was executed as part of these GitHub source commits.

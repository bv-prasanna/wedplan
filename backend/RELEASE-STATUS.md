# WedFlow — release status (10 November 2026)

**Current release decision: NOT UAT-CERTIFIED / NOT PRODUCTION-READY.**

## Source committed

- Static responsive customer, vendor, and administrator web application in `web/`, with core wedding management, vendor search, quotes, bookings, payments and refund decision console.
- Java 21 Spring Boot API with bcrypt, hashed bearer tokens, login throttling, strict owner/role access, protected approvals, safe structured error codes and request telemetry.
- One-time `SUPER_ADMIN` bootstrap via controlled env-only secrets; no public role elevation.
- Razorpay orders and signed webhooks, admin-only queued refund workflow with deduction/reason/request dedupe, a worker that does not auto-retry ambiguous provider outcomes, and reconciliation statuses.
- In-app notifications and a recoverable, bounded email outbox (Brevo); SMS not operational.
- Audit metadata, request ID/status/latency, optional consent-based client-report GPS; immutable/tamper-proof compliance audit still requires further design.
- **Flyway V1–V12** and updated architecture, deployment, ER, schema dictionary, migration script, support/maintenance and customer KT plans.
- Manual-only GitHub Actions workflow for web/Node and Java/JUnit/JaCoCo.

## Source vs evidence distinction

| Gate | Current evidence |
|---|---|
| Code committed to GitHub | Yes; verify actual release SHA in GitHub |
| Web JS source syntax/utility assertions | Source-level checks performed; run full Node suite on release SHA |
| Java 21 Maven build/JUnit/JaCoCo | Not verified |
| PostgreSQL Flyway V1–V12 application | Not verified |
| Razorpay live or sandbox webhook/refund integration | Not verified |
| Frontend browser/device visual accessibility UAT | Not verified |
| Backup restoration / zero-loss reconciliation | Not verified |
| Production deployment / customer acceptance | Not verified |

## Functional blockers

Email/account verification and password reset, staff MFA, robust vendor settlement/payouts, planner collaborators and staff permissions, itemized quote versions, guest seating/dietary planning, advanced analytics/support and full provider/notification reliability. Payment order creation still needs stronger provider reconciliation; refund PROCESSING/REQUIRES_RECONCILIATION requires external verification and staff action. Do not advertise automatic final refunds or payouts.

## Deployment and operations

Run `mvn -B -f backend/pom.xml clean verify`, `cd web && npm run verify` and migration validation on a disposable PostgreSQL instance. Do not apply migrations to production without signed backup/restore evidence. Only UAT against a verified Razorpay test merchant can establish financial state correctness.

[Architecture and flow](../docs/architecture-kt/SYSTEM_ARCHITECTURE.md) · [Deployment](../docs/architecture-kt/DEPLOYMENT_TOPOLOGIES.md) · [ER](../docs/architecture-kt/ER_FULL_DOMAIN.md) · [SQL catalogue](../docs/architecture-kt/SCHEMA_INDEX.md) · [Migration script](../docs/architecture-kt/migrate-safe.sh) · [Maintenance and KT](../docs/architecture-kt/DEPLOYMENT_MAINTENANCE_KT.md) · [UAT](../docs/SCREEN-BY-SCREEN-UAT-2026-11-10.md).

# WedFlow backend — Java 21 / Spring Boot / PostgreSQL

**Development source; not production-certified.** The backend contains customer auth, wedding planning, vendors, quotations, booking/calendar, catering, in-app notifications, Razorpay REST/webhooks, refund queues, audit and email-outbox processing.

## Setup / validation

```bash
# Java 21, Maven 3.9+, PostgreSQL
export DATABASE_URL='jdbc:postgresql://localhost:5432/wedflow'
export DATABASE_USERNAME='wedflow'
# Set DATABASE_PASSWORD in a secure environment.
export WEDFLOW_ALLOWED_ORIGINS='http://localhost:4173'
mvn -B clean verify
mvn spring-boot:run
```

Migrations are Flyway `src/main/resources/db/migration/V1__initial.sql` through `V12__notification_delivery_recovery.sql`; they apply at startup if the database is available. On controlled release use [guarded Flyway operator script](../docs/architecture-kt/migrate-safe.sh) and [migration runbook](../docs/architecture-kt/DEPLOYMENT_MAINTENANCE_KT.md).

## Security/authentication

- Customers register with bcrypt-hashed passwords (minimum 12 characters) and receive opaque access tokens hashed in the database.
- Login attempts are throttled per hashed email. Token expiry and explicit logout are supported.
- Role and resource-owner checks are server-side for weddings, vendors, approvals and finance.
- Optional one-time `SUPER_ADMIN` creation: configure **both** `WEDFLOW_BOOTSTRAP_ADMIN_EMAIL` and `WEDFLOW_BOOTSTRAP_ADMIN_PASSWORD` (16+ characters) through deployment secrets. The bootstrap will not update an existing admin. Remove both values immediately after provisioning. Never publish or commit passwords.
- MFA, verified-email onboarding, password recovery, advanced planner/role policies and full security certification remain pending.

## Financial and notification workflows

Razorpay order checkout and signed captured/failed webhooks update local payment records. After cancellation, only an administrator can queue a partial/full refund with amount, deduction and documented reason. Worker takes the queued request after DB commit and attempts a single provider call; uncertain provider state requires **manual reconciliation** rather than duplicate refunds. `QUEUED`, `PROCESSING`, `PENDING` and `REQUIRES_RECONCILIATION` are not successful refunds.

Brevo handles outbound email. A durable worker retries stale mail deliveries with increasing backoff but at-least-once delivery may duplicate an email after a crash; SMS remains unimplemented.

**Never approve production based only on the existence of these source modules.** Actual Maven/JUnit/PostgreSQL/JaCoCo, payment sandbox and negative UAT evidence remain required. See [release blockers](DEPLOYMENT-BLOCKERS.md), [architecture](../docs/architecture-kt/SYSTEM_ARCHITECTURE.md) and [database ER](../docs/architecture-kt/ER_FULL_DOMAIN.md).

## CI

`.github/workflows/manual-backend-verify.yml` is `workflow_dispatch` only to avoid CI minutes on every push. No successful run has been verified for this repository at this revision.

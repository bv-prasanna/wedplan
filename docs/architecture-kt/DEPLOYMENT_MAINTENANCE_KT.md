# WedFlow — deployment, maintenance, operations and customer KT playbook

**Handover pack updated:** 10 November 2026; source SHA must be recorded on the actual release ticket. **Not an attestation of a deployed service, executed UAT, working Razorpay merchant or restored backup.**

## 1. Technical ownership / RACI

| Activity | Product engineer | Operations / DBA | Finance / customer admin | UAT lead |
|---|---|---|---|---|
| Source release and migrations | Build/change review | Apply approved release, backup/restore | Approve downtime | Verify regression |
| Customer/vendor authorization | Implement backend checks | Monitor access logs | Approve staff accounts | Check wrong-role cases |
| Payments and refunds | Provider signature/idempotency implementation | Monitor callbacks, worker queues | Approve refund amounts/deductions, reconcile gateway | Check test-mode provider results |
| Domain/email configuration | Document endpoint contract | DNS, TLS, CORS, secrets, sender | Approve branded identity | Browser/device smoke |
| Privacy/geo audit | Consent and access controls | Retention and encryption | Confirm lawful use/retention | Verify opt-out/missing GPS |
| Incident handling | Diagnose/fix | Coordinate containment/recovery | Customer communications | Re-test and sign off |

Keep production secret values only in the approved secret store; record *references*, never secret contents or customer PAN/guest PII in GitHub issues and KT.

## 2. Clone, build and local demo

```bash
git clone https://github.com/bv-prasanna/wedplan.git
cd wedplan

# Node 20+, Java 21 + Maven 3.9+, PostgreSQL
cd web && npm run verify && cd ..
mvn -B -f backend/pom.xml clean verify

# Review backend/src/main/resources/application.properties
# Set disposable PostgreSQL JDBC and local CORS env.
export DATABASE_URL='jdbc:postgresql://localhost:5432/wedflow'
export DATABASE_USERNAME='wedflow'
export WEDFLOW_ALLOWED_ORIGINS='http://localhost:4173'
# Supply DATABASE_PASSWORD securely.
mvn -f backend/pom.xml spring-boot:run

# Separate terminal: use an isolated backend origin in web/config.js
python3 -m http.server 4173 --directory web
```

The browser UI is a static single-page app under `web/`; no React build is required. Backend Flyway is enabled at startup. Never point the development frontend or UAT gateway keys to live customer records.

## 3. Controlled DB migration and physical schema

All committed migration files are in `backend/src/main/resources/db/migration/V*__*.sql`. At this revision, expected versions are **V1–V12**. This is a *source* count; the real DB must be inspected.

```bash
# Safe/read-only, no DB credentials needed
bash docs/architecture-kt/migrate-safe.sh plan

# For actual DB inspection, supply FLYWAY_URL/FLYWAY_USER/FLYWAY_PASSWORD
# through approved environment/secret manager, never in docs:
bash docs/architecture-kt/migrate-safe.sh info
bash docs/architecture-kt/migrate-safe.sh validate

# Read-only PostgreSQL physical schema and Flyway history:
# supply PGHOST, PGDATABASE, PGUSER and secure authentication
bash docs/architecture-kt/migrate-safe.sh export-schema

# Only after tested restore, change approval, source SHA and a maintenance lock:
export MIGRATION_APPROVAL=APPROVED
export RESTORE_TESTED=true
export BACKUP_REFERENCE='<approved-snapshot-reference>'
export RELEASE_SHA="$(git rev-parse HEAD)"
bash docs/architecture-kt/migrate-safe.sh migrate
```

The operator wrapper refuses arbitrary shell migration commands, unknown engines, duplicate versions, git SHA mismatch, dirty migration SQL and migration without backup/restore acknowledgement. **It does not prove** the snapshot exists or a restoration actually worked: independent approval and evidence remain required. Do not run the CLI migrator concurrently with a newly starting Spring instance that has automatic Flyway enabled. Do not erase applied checksums with `repair` or `clean`.

## 4. Production configuration checklist

- [ ] HTTPS web/API with verified DNS and certificate; CORS `WEDFLOW_ALLOWED_ORIGINS` matches exact client origins
- [ ] Distinct PostgreSQL prod and UAT DB, tested backup/restore, application user has least needed rights
- [ ] Java 21 compatible version, health endpoint, resource limits and Hikari connection pool sized to DB
- [ ] `web/config.js` points to the correct API; no private key/secret in `web/`
- [ ] Razorpay key ID, secret and webhook key in secret store; signed webhook endpoint reachable
- [ ] Brevo verified sender, queue retries and failure alerts; SMS not advertised as working
- [ ] Admin access provisioned through approved procedures; strong password, future MFA/recovery program
- [ ] Payment refund reasons, fee deductions and `REQUIRES_RECONCILIATION` runbook owned by finance
- [ ] Audit privacy, geolocation consent and retention policy reviewed
- [ ] Manual CI and disposable PostgreSQL migration/integration test evidence retained
- [ ] Page-by-page UX, accessibility and negative UAT checklists signed

**No live go-live recommendation without the unchecked items and documented limitations in the release-status file.**

## 5. Operational daily/weekly/monthly checks

**Daily:** HTTP 5xx, abnormal 401/403/429, overdue payment intent, signed webhook failures, refund state `PROCESSING` or `REQUIRES_RECONCILIATION`, email `FAILED`, stale outbox leases, DB availability, customer cases.

**Weekly:** confirm backup success, run disposable restoration test, inspect Flyway checksum drift, active auth tokens/lockouts, refund provider IDs and amount reconciliation, merchant settlement mismatches and cost/quota thresholds.

**Monthly:** dependency/security updates, role access review, data privacy/retention requests, certificate expiry, backup RTO/RPO review, load/booking concurrency test and breach response drill.

### Incident response

| Severity | Trigger | Immediate action |
|---|---|---|
| P0 | Unauthorized data exposure, duplicate refund/transfer, leaked gateway credentials | Disable affected payment/refund operations, isolate secrets, preserve request IDs and provider receipts, notify incident owner |
| P1 | Widespread login, booking or payment outage; webhook processing stuck | Stop new checkout if consistency uncertain; maintain DB backup; reconcile gateway before replaying |
| P2 | Isolated page or email failure | Capture request ID, reproduce on UAT, create regression case and deploy approved fix |

**Financial retry protocol:** if a refund is `REQUIRES_RECONCILIATION`, verify the corresponding provider payment, notes `wedflow_refund_id`, amount and external refund status in the actual Razorpay account. Do **not** resubmit blindly. Preserve the original request key and provider evidence. Do not set `PROCESSED` without signed callback/provider reconciliation backed by an approved audit procedure.

**Outbox:** stale `SENDING` emails can retry after five minutes. A provider may have accepted a message before the worker crashed, so an email can be delivered twice. Never use email delivery as proof of payment/refund success.

## 6. KT demonstration script — 60–90 minutes

| Step | Demonstration | Required evidence |
|---|---|---|
| 1. Architecture | Walk through frontend, APIs, PostgreSQL, Razorpay, Brevo and audit boundaries | Mermaid diagrams and release SHA |
| 2. Customer | Register/sign in, create wedding, ceremonies, tasks, guests and budget | Screenshots, 201/200 and 400/409 negative |
| 3. Vendor | Register business, admin approval, service listing, enquiries and quotations | Role-based 403; unapproved vendor 409 |
| 4. Booking | Select date, accept quote, prevent vendor/day conflict, cancel booking | Booking record, unique-index evidence, 409 duplicate |
| 5. Finance | Test-mode order, captured/failed callback, cancel booking, administrator-approved partial refund with deduction | HMAC validation, PENDING/VERIFIED, QUEUED/PROCESSED distinctions |
| 6. Reliability | Simulate provider timeout, refund reconciliation queue and stale mail worker | State transitions and idempotency key evidence |
| 7. Errors | Invalid request, expired auth, wrong owner, 404/405/413/415/429/500 | Safe problem payload, code and X-Request-ID |
| 8. Geo and audit | Consent-enabled client request vs denied/missing coordinates | Audit event and honest source `CLIENT_REPORTED` / unavailable |
| 9. Operations | Schema-only export, Flyway validate, backup restoration and on-call checklist | Operator run log, no disclosed secrets |
| 10. Handover | Open defects, release gating and support responsibilities | Signed UAT matrix, owner and planned corrective actions |

Demonstrate only against isolated **synthetic UAT data** and Razorpay sandbox; do not perform real payments, customer refunds or migration merely for a sales/KT demonstration.

## 7. Expected handover artifacts

Release manifest (commit SHA, build/test reports), deployment/env inventory, architecture/ER diagrams, PostgreSQL schema-only export, Flyway history, backup and restore evidence, approved API inventory, business/role matrix, Razorpay sandbox receipts, audit retention rules, privacy policy, negative UAT screenshots, open-defect list, on-call escalation, and KT attendee signoff.

[Source review](SYSTEM_ARCHITECTURE.md) · [Deployment diagrams](DEPLOYMENT_TOPOLOGIES.md) · [Logical ER](ER_FULL_DOMAIN.md) · [Versioned schema catalogue](SCHEMA_INDEX.md) · [Page UAT](../SCREEN-BY-SCREEN-UAT-2026-11-10.md).

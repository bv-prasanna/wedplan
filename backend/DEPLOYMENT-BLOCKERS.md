# WedFlow — deployment blockers and release gates

**Status: BLOCKED (source updated 10 November 2026).** A committed feature or test is not a successful build, deployed migration or proven security control.

## Mandatory P0 gates

- [ ] Java 21 `mvn -B -f backend/pom.xml clean verify` passes at the **release SHA**; inspect Surefire, JaCoCo and regression test results.
- [ ] Node `cd web && npm run verify` passes and web files are served in a real browser across desktop/mobile viewports.
- [ ] Flyway **V1–V12** validates and migrates on fresh PostgreSQL UAT; schema/constraints and migrations verified against a tested restore.
- [ ] RBAC and ownership negative tests: customer cannot approve vendors or request a refund, vendor cannot read other vendor's enquiries, admin-only queue protected.
- [ ] Razorpay test-mode checkout, signed captured/failed callbacks, duplicate/replayed webhook, order creation failure and reconciliation scenarios pass.
- [ ] Admin cancellation refund is queued once; concurrent same-request-key submissions never double-refund; provider timeout/crash/early callback is reconciled before any retry.
- [ ] No provider status is misrepresented: QUEUED, PROCESSING and PENDING are not PROCESSED; no email delivery used to prove captured money.
- [ ] Stale notification leases and bounded retries work; email provider configured; SMS features not advertised without a real adapter.
- [ ] Admin bootstrap credentials used only once, secured and removed; customer identity recovery/verification and privileged MFA roadmap approved.
- [ ] Guest/PII and geolocation consent, retention, TLS, CSP, CORS, rate limiting, request IDs, auth error statuses and secrets handling reviewed.
- [ ] Full page-by-page UAT, keyboard/screen-reader/mobile visuals and payment error/success messages signed.
- [ ] Production environment, database backup/restore, error alerts, webhook monitoring, incident/on-call owner and rollback procedure verified.

## Known functional limitations

- Account registration is not verified by email; password recovery and staff MFA are not complete.
- Vendor settlements are bookkeeping placeholders and do not implement vendor payouts/reconciliation.
- Quote line items/versioning, planners/collaboration, guest seating/dietary mapping, full support workflow and advanced reports remain incomplete.
- Razorpay order creation still performs a provider API call during a database transaction, which must be hardened for uncertain provider outcomes before high-volume production.
- Refund PROCESSING and REQUIRES_RECONCILIATION statuses require gateway reconciliation runbook; no automatic retry is allowed without proof.
- Email delivery is at-least-once and can duplicate a message after a crash; SMS adapter is not implemented.
- Audit recording after request completion is best-effort and not a strong transactional compliance record.

## Runbooks and source of truth

[Architecture](../docs/architecture-kt/SYSTEM_ARCHITECTURE.md) · [Deployment](../docs/architecture-kt/DEPLOYMENT_TOPOLOGIES.md) · [Full logical ER](../docs/architecture-kt/ER_FULL_DOMAIN.md) · [Schema index](../docs/architecture-kt/SCHEMA_INDEX.md) · [Safe migration tool](../docs/architecture-kt/migrate-safe.sh) · [Maintenance and KT](../docs/architecture-kt/DEPLOYMENT_MAINTENANCE_KT.md) · [UAT test matrix](../docs/SCREEN-BY-SCREEN-UAT-2026-11-10.md).

**No live production deployment or UAT signoff should be inferred from GitHub commits.**

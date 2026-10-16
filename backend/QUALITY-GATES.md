# Backend quality gates

## Commands (local, zero CI minutes)
```bash
cd backend
mvn -B clean verify
```

The Maven build, PostgreSQL migrations, integration tests, and coverage **must be executed and reviewed** before any production deployment. The code is not certified production-ready.

## Release blockers
- Configure real payment provider integration with verified, authenticated callbacks, idempotency, refunds, and settlement ledger. The payment API currently records **PENDING intents only**; it does not charge money.
- Add availability locking and collision handling at quote acceptance; the current unique index only protects non-null service_date, which is not populated during acceptance.
- Add password reset, email verification, account lockout/rate limiting, token expiry cleanup, secure CORS policy, and audit coverage.
- Build integration tests against PostgreSQL (Flyway migrations and transaction isolation), API authorization and negative scenarios.
- Add vendor staff/planner role permissions, messaging delivery, guest invitations, seating, refunds, cancellation policies, and payouts.
- Resolve budget spend reconciliation and prevent duplicate/overlapping payments.
- Review privacy, retention, data export/deletion, observability, backups, and deployment secrets.
- Validate migration roll-forward/rollback and inspect OWASP dependencies.
- Do not enable automatic CI on each commit. Use manual local checks until a manual-only workflow is agreed.

## Existing tests
JUnit tests cover budget math, SHA-256 utility and request validation. This is not full API or integration coverage.

# WedFlow — engineering quality gates (10 November 2026)

**No production/UAT certification yet.** This is a list of required checks, not a report of executed passing tests.

## Mandatory commands

```bash
cd web && npm run verify && cd ..
mvn -B -f backend/pom.xml clean verify
bash docs/architecture-kt/migrate-safe.sh plan
```

For integration run, use dedicated disposable PostgreSQL, run approved Flyway V1–V12, exercise the API with customer, vendor, admin and wrong-role identities. Preserve Surefire reports, JaCoCo XML/HTML, SQL migration history and the immutable Git SHA.

## High-risk negative test gates

- **Account:** wrong password 401, rate-limited 429, expired token 401, wrong-role 403, non-owned wedding/vendor 404; no raw passwords/tokens in client error/log.
- **Super-admin:** bootstrap works once with strong controlled secrets; never overwrites an existing account; zero public admin self-registration.
- **Vendor:** pending profile is hidden from public search and cannot quote; approved provider can quote; quote accept must reject a conflicting vendor/date (409).
- **Money:** requested amount cannot exceed unpaid quote; signed Razorpay webhook replay is idempotent; wrong amount/currency or invalid signature rejected.
- **Refund:** customer 403, active booking 409, negative/oversized refund rejected, duplicated key returns the same request, different payload with same key rejected; provider timeout transitions to reconciliation and never silently resubmits.
- **Messaging:** stale SENDING recovered; max attempts respected; provider unavailable cannot fabricate SENT; email at-least-once caveat documented.
- **Errors:** safe 400,401,403,404,405,409,413,415,429,500 with request reference and Cache-Control no-store.
- **Location:** paired lat/lon and accuracy validated; user denial/missing coordinates explicitly unavailable; not treated as trusted presence.
- **Privacy:** guest phone, credentials, Razorpay secrets and precise coordinates not present in raw logs, URLs, or unauthorized accounts.
- **Web:** role-appropriate navigation, all routes, empty/loading/error/success, responsive and keyboard/screen-reader accessible, checkout return not counted as verified payment.

## Remaining release blockers

Full customer identity recovery/verification, advanced planner features, vendor settlement and reconciliation, reliable SMS, real provider incident drills, browser accessibility review, backups/restore, tests with PostgreSQL and actual JaCoCo reports. Run [manual workflow](../.github/workflows/manual-backend-verify.yml) if permitted. See [deployment checklist](DEPLOYMENT-BLOCKERS.md), [page UAT](../docs/SCREEN-BY-SCREEN-UAT-2026-11-10.md), [KT runbook](../docs/architecture-kt/DEPLOYMENT_MAINTENANCE_KT.md).

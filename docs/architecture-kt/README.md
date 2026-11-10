# WedFlow — architecture, database, deployment, maintenance and KT package

**Repository:** [bv-prasanna/wedplan](https://github.com/bv-prasanna/wedplan) · **Updated:** 10 November 2026. Documents are derived from committed application and migration sources. They **do not confirm** a live website, successful build, PostgreSQL migration, payment sandbox result or signed UAT.

## Main technical deliverables

| Deliverable | File | What it covers |
|---|---|---|
| System architecture | [SYSTEM_ARCHITECTURE.md](SYSTEM_ARCHITECTURE.md) | Components, roles, trust boundaries and booking/payment/refund sequence |
| Deployment diagrams | [DEPLOYMENT_TOPOLOGIES.md](DEPLOYMENT_TOPOLOGIES.md) | Proposed static web + Java backend + PostgreSQL, DEV/UAT/PROD and recovery |
| Complete logical ER | [ER_FULL_DOMAIN.md](ER_FULL_DOMAIN.md) | Identity, wedding plans, vendor marketplace, catering, finance, outbox, audit and geolocation |
| Full migration catalogue | [SCHEMA_INDEX.md](SCHEMA_INDEX.md) | Versioned SQL **V1–V12**, table dictionary and sources |
| Operator Flyway script | [migrate-safe.sh](migrate-safe.sh) | Read-only plan, info, validate, schema export, approved migrate; never arbitrary shell commands |
| Deployment/maintenance/KT | [DEPLOYMENT_MAINTENANCE_KT.md](DEPLOYMENT_MAINTENANCE_KT.md) | Configuration, backups, errors, reconciliation, on-call and demo scenarios |
| Page-by-page UAT | [../SCREEN-BY-SCREEN-UAT-2026-11-10.md](../SCREEN-BY-SCREEN-UAT-2026-11-10.md) | Customer/vendor/admin routes, quality and negative tests |
| Security test matrix | [SECURITY-TEST-MATRIX.md](SECURITY-TEST-MATRIX.md) | Authentication, data isolation, financial and audit checks |
| Source reverse engineering | [REVERSE_ENGINEERING.md](REVERSE_ENGINEERING.md) | Code inventory, modules, known gaps and evidence status |
| Release readiness | [../../backend/DEPLOYMENT-BLOCKERS.md](../../backend/DEPLOYMENT-BLOCKERS.md) | Mandatory not-yet-verified release gates |

## Actual application

`backend/`: Java 21, Spring Boot, Spring Security bearer-token authentication, JDBC, PostgreSQL/Flyway, Razorpay REST + signed webhooks, Brevo mail, scheduling workers and API audit.

`web/`: responsive static HTML/CSS/JavaScript customer, vendor and administrator application, **not a compiled React project**. No native Android/iOS app is present in this repository.

**V11** adds admin-only queued partial refunds and idempotency metadata; **V12** adds notification retry lease/recovery columns. Neither is confirmed installed on a production DB.

## Safe commands

```bash
cd web && npm run verify && cd ..
mvn -B -f backend/pom.xml clean verify
bash docs/architecture-kt/migrate-safe.sh plan
# The next commands require separately approved Flyway/database credentials:
bash docs/architecture-kt/migrate-safe.sh info
bash docs/architecture-kt/migrate-safe.sh validate
bash docs/architecture-kt/migrate-safe.sh export-schema
# migrate requires approval, tested backup, RELEASE_SHA and clean versioned SQL
```

CI is [manual GitHub Actions](../../.github/workflows/manual-backend-verify.yml). **No workflow run has yet been confirmed** for this repository. Static source and committed tests are not equivalent to measured build/coverage evidence.

## Release decision

**UAT/production: BLOCKED.** In particular, production payment/refund reconciliation, real PostgreSQL integration tests, security/role testing, complete planner workflows, account recovery/email verification, SMS integration, browser accessibility/visual UAT, backup restoration and actual hosting settings remain unverified or incomplete.

The administrator account may be provisioned **once** with `WEDFLOW_BOOTSTRAP_ADMIN_EMAIL` and `WEDFLOW_BOOTSTRAP_ADMIN_PASSWORD` (strong, 16–128 characters) as controlled runtime secrets. Remove them after successful bootstrapping. No public admin sign-up or auto-password reset is provided.

# WedFlow backend release status (2026-10-19)

**Not production ready.** The GitHub repository contains backend source, migrations and some unit tests. Successful pushes do not prove compilation, test execution, coverage or runtime correctness.

## Implemented source modules
- Customer registration/login/logout and bearer token authentication, account profile and logout-all
- Weddings, ceremonies, guests, RSVP, tasks and budget allocations
- Vendor registration and admin approval/rejection
- Vendor service listings, enquiries, quotations and quote acceptance
- Vendor service-date availability, unique active vendor/day constraint, cancellation and reviews
- Catering menus and dietary items
- Payment intent records (PENDING only; no payment provider processing)
- In-app notification inbox records and audit events
- PostgreSQL Flyway V1–V6 migrations, JUnit tests and JaCoCo plugin

## Release blockers — mandatory
1. Execute `mvn clean verify` using Java 21 and Maven; repair compilation and tests.
2. Run Flyway V1–V6 against disposable PostgreSQL and run full integration tests including concurrent booking/payment requests.
3. Add payment gateway integration and **server-verified** signed callbacks, idempotency, reconciliation, refunds, taxes and settlements; never mark payments VERIFIED based solely on client requests.
4. Add rate limiting, verified email, password reset, secure admin bootstrap, fine-grained role authorization, token lifecycle and key management.
5. Add production-ready notification delivery adapters, retries, outbox and monitoring.
6. Add full booking cancellation policy, vendor calendars/time slots and payout workflow.
7. Add negative-path API tests, tenant isolation, authorization tests, concurrency tests, migration tests and measured coverage.
8. Add operational readiness: backups/restore drill, health checks, logs, observability, privacy/retention/deletion and deployment review.

## Cost controls
No automatic GitHub Actions workflow was introduced. Execute tests locally. Open-source Java/PostgreSQL software has no license cost; hosting, SMS, email and payment processing may incur charges.

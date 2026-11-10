# WedFlow — code-derived reverse-engineering inventory

**Snapshot:** 10 November 2026 · [bv-prasanna/wedplan main](https://github.com/bv-prasanna/wedplan). File presence is not test evidence.

## Actual architecture

- `web/`: dependency-free responsive single-page frontend with customer, vendor and administrator routes; `index.html`, `styles.css`, `src/app.mjs` and `src/core.mjs`.
- `backend/`: Java 21 Spring Boot REST application. Authentication uses opaque hashed bearer tokens and bcrypt passwords. Core controllers implement weddings, guests, RSVP, budgets, tasks, vendors, enquiries, quotes, bookings, catering, payments, notifications and admin approvals.
- `backend/src/main/java/com/wedflow/payment`: Razorpay order checkout, signed webhook, admin-only queued refund requests and refund dispatch worker. Provider reconciliation remains a release gate.
- `backend/src/main/java/com/wedflow/notification`: Brevo email sender and durable retry outbox worker; SMS not operational.
- `backend/src/main/java/com/wedflow/audit`: best-effort mutation audit, request ID, HTTP status and optional self-reported device coordinates.
- `backend/src/main/resources/db/migration/`: 12 versioned SQL files, **V1 through V12**, distinct from live schema state.
- `.github/workflows/manual-backend-verify.yml`: workflow_dispatch, frontend Node assertions and backend Maven/JaCoCo; no successful run is yet verified.

## Key table groups

Identity: `app_users`, `auth_tokens`, `auth_login_attempts`. Wedding planning: `weddings`, `ceremonies`, `budget_categories`, `wedding_tasks`, `wedding_guests`. Vendors: `vendors`, `vendor_profiles`, `vendor_services`, `enquiries`, `vendor_quotes`, `vendor_bookings`, `vendor_unavailability`, `vendor_reviews`. Payments: `payment_records`, `payment_events`, `payment_refunds`, `settlement_entries`. Notifications/audits: `user_notifications`, `notification_outbox`, `audit_events`. Catering: `catering_menus`, `catering_items`.

## Boundaries and release caveats

- Finance-only refunds require an administrator, a cancelled booking, reason, amount, fee deduction and request key. Refunds remain pending until verified with Razorpay; unknown provider outcomes are never blindly retried.
- Wedding/guest/payment/vendor ownership is primarily enforced in SQL. No planner collaborator or fine-grained multi-role policy model exists yet.
- Server errors have sanitized details, machine-readable codes and request IDs, but a penetration test and real provider outage exercise are still needed.
- No native mobile source is present. The static web UI has not been browser/device certified.
- No production DB metadata/export, signed deployment run, test coverage or observed live environment is in the current source audit.

Refer to [architecture](SYSTEM_ARCHITECTURE.md), [ER model](ER_FULL_DOMAIN.md), [deployment](DEPLOYMENT_TOPOLOGIES.md), [migration](migrate-safe.sh), [ops/KT](DEPLOYMENT_MAINTENANCE_KT.md) and [UAT](../SCREEN-BY-SCREEN-UAT-2026-11-10.md).

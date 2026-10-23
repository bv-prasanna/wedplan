# Deployment gates — WedFlow

Do not deploy live payments until all gates are green.

## Current source
Razorpay order creation, captured/failed webhooks, refund requests and processed/failed refund webhooks; email outbox delivery using Brevo; migration V7; signature tests.

## Required work
- Maven clean verify and JaCoCo measured coverage (not run in this environment; Maven unavailable).
- Disposable PostgreSQL migration test V1–V7, real DB API tests, concurrent payment/booking and refund tests.
- Razorpay test-mode checkout, webhook replay, failed payment, partial/full refund and network outage tests.
- Financial reconciliation and accounting, Razorpay Route linked accounts/transfers if vendor payouts are required. Settlement entries alone do not pay vendors.
- SMS provider integration, notification template restrictions, worker crash recovery and reliable backoff. Existing outbox worker can leave SENDING messages stranded after a process crash.
- Fix retry/idempotency for refund requests and external order creation: DB rollback after a successful provider API call may lead to duplicate provider operations.
- Security audit: arbitrary self-addressed email content, email verification, login rate limiting, password reset, role-based permissions, privacy compliance.
- Webhook response policy for unknown orders/refunds, replay tests and dead-letter review.
- Secrets configured only in deployment platform. Configure BREVO_API_KEY, NOTIFY_FROM_EMAIL, RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET, RAZORPAY_WEBHOOK_SECRET, WEDFLOW_ALLOWED_ORIGINS.
- Do not create automatic GitHub Actions runs. Execute Maven locally or on a manual runner.

## Local command
```bash
cd backend
mvn -B clean verify
```

The app is **not production ready** until these gates are independently verified.

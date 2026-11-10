# WedFlow Web
A responsive, dependency-free browser frontend for the Spring Boot REST API. The app covers sign-in/register, wedding planning, vendor discovery, enquiries, quotes, bookings, payments, catering, notifications, vendor workspace and admin approval.

## Local
1. Start the backend with PostgreSQL and Flyway migrations applied.
2. Run `python3 -m http.server 4173 --directory web` from the repo root.
3. Set `window.WEDFLOW_API_BASE = "http://localhost:8080";` in `web/config.js` for local development, then open http://localhost:4173.
4. Include `http://localhost:4173` in the backend `WEDFLOW_ALLOWED_ORIGINS` list.
5. Run `node --test web/tests/*.test.mjs` and `node --check web/src/app.mjs`.

## Deployment
Cloudflare Pages: connect this repository, **root directory `web`**, no build command, output directory `.`. Set the public HTTPS API URL in `config.js` before release; backend CORS must whitelist the exact HTTPS frontend origin. Do not place secrets in static files.

This is a functional initial web implementation, **not a certified production application**. Confirm payment provider/notification readiness, permissions, signed webhooks, server-side audit and device accessibility. A hosted checkout requires loading Razorpay's SDK at user initiation, and only a server-verified payment record is authoritative. The client never represents checkout callback as proof of capture.

## Security and privacy
Access tokens are kept in per-tab sessionStorage (not persistent localStorage). Do not introduce unsafe inline event handlers or render API content as unescaped HTML. All write operations require the backend to enforce authorization; hiding navigation is not an authorization boundary. The frontend cannot securely determine GPS provenance from client headers.

## Administrator finance and truthful statuses

The customer payments tab no longer exposes an unrestricted refund submit form. The administrator page loads `/api/v1/admin/refundable-payments` and `/api/v1/admin/refund-requests`, requires a cancelled booking and recorded refund/deduction/reason, and submits a stable request key for retries. The API replies `202 QUEUED`; only a signed provider callback can complete a refund. A `REQUIRES_RECONCILIATION` record needs provider-side financial investigation, not another click to resubmit.

A one-time SUPER_ADMIN account must be provisioned via backend controlled bootstrap before admin screens can be used. Production email verification, recovery and MFA remain backlog items. See [operators runbook](../docs/architecture-kt/DEPLOYMENT_MAINTENANCE_KT.md).

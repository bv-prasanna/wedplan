# Razorpay integration: test-mode setup

Required backend environment variables:
- RAZORPAY_KEY_ID: Razorpay test-mode key ID
- RAZORPAY_KEY_SECRET: Razorpay test-mode secret, backend only
- RAZORPAY_WEBHOOK_SECRET: independently configured webhook secret, backend only
- WEDFLOW_ALLOWED_ORIGINS: comma-separated exact frontend origins

Workflow:
1. Customer creates a pending payment intent through existing wedding payment API.
2. Customer calls POST /api/v1/razorpay/orders with paymentIntentId. Backend creates a Razorpay order.
3. Frontend uses returned orderId, keyId and amount with Razorpay Checkout.
4. Subscribe to payment.captured and payment.failed webhooks at POST /api/v1/razorpay/webhook.
5. The webhook verifies HMAC-SHA256 against the raw body, checks order/amount/currency and deduplicates event IDs.

Important limitations:
- Checkout callback signature utility is available, but webhook is the authoritative capture signal.
- Refund initiation endpoint exists, but refund completion webhook and reconciliation are not yet implemented.
- Settlement entries are schema-only; vendor payouts are not implemented. Do not promise or disburse vendor funds.
- Notification outbox is schema/API only; no real email/SMS sender is active.
- Never commit secrets. Never enable live keys before payment/refund/settlement integration testing and operational approval.
- Test locally with Java 21, Maven and PostgreSQL; verify Flyway migrations, coverage, provider test-mode payments and signed webhook delivery.
- No automatic CI is configured.

# WedFlow — full logical ER diagrams, schema and data ownership

**Verified against repository SQL V1–V12, not against a deployed physical database.** The diagrams represent declared references and important columns, not every implicit business invariant. All financial amounts are integer paise. Review `flyway_schema_history` and `pg_dump --schema-only` to establish production physical schema.

## Identity and wedding planning

```mermaid
erDiagram
  APP_USERS {
    UUID id PK
    VARCHAR email UK
    VARCHAR password_hash
    VARCHAR role
    TIMESTAMPTZ created_at
  }
  AUTH_TOKENS {
    UUID id PK
    UUID user_id FK
    VARCHAR token_hash UK
    TIMESTAMPTZ expires_at
  }
  AUTH_LOGIN_ATTEMPTS {
    CHAR email_hash PK
    INTEGER failed_attempts
    TIMESTAMPTZ blocked_until
  }
  WEDDINGS {
    UUID id PK
    UUID owner_id FK
    VARCHAR title
    VARCHAR city
    DATE event_date
    BIGINT total_budget_paise
  }
  CEREMONIES {
    UUID id PK
    UUID wedding_id FK
    VARCHAR name
    DATE event_date
    VARCHAR venue
  }
  BUDGET_CATEGORIES {
    UUID id PK
    UUID wedding_id FK
    VARCHAR name
    BIGINT allocated_paise
    BIGINT spent_paise
  }
  WEDDING_TASKS {
    UUID id PK
    UUID wedding_id FK
    VARCHAR title
    DATE due_date
    BOOLEAN completed
  }
  WEDDING_GUESTS {
    UUID id PK
    UUID wedding_id FK
    VARCHAR full_name
    VARCHAR phone
    VARCHAR rsvp
  }
  APP_USERS ||--o{ AUTH_TOKENS : sessions
  APP_USERS ||--o{ WEDDINGS : owns
  WEDDINGS ||--o{ CEREMONIES : hosts
  WEDDINGS ||--o{ BUDGET_CATEGORIES : allocates
  WEDDINGS ||--o{ WEDDING_TASKS : plans
  WEDDINGS ||--o{ WEDDING_GUESTS : invites
```

**Important:** `AUTH_LOGIN_ATTEMPTS.email_hash` is an independently hashed identifier and **not a declared FK** to `APP_USERS`. Wedding guest phone numbers are personally identifiable and must remain owner-scoped. Wedding deletion must reject vendor history; the original cascade on planning tables must never be used to erase financial records.

## Vendor, service, catering and booking domain

```mermaid
erDiagram
  APP_USERS {
    UUID id PK
  }
  WEDDINGS {
    UUID id PK
  }
  CEREMONIES {
    UUID id PK
  }
  VENDORS {
    UUID id PK
    VARCHAR display_name
    VARCHAR category
    VARCHAR city
    BOOLEAN approved
  }
  VENDOR_PROFILES {
    UUID id PK,FK
    UUID owner_id FK
    VARCHAR description
    VARCHAR verification_status
  }
  VENDOR_SERVICES {
    UUID id PK
    UUID vendor_id FK
    VARCHAR title
    BIGINT base_price_paise
    BOOLEAN active
  }
  ENQUIRIES {
    UUID id PK
    UUID wedding_id FK
    UUID vendor_id FK
    VARCHAR status
  }
  VENDOR_QUOTES {
    UUID id PK
    UUID enquiry_id FK
    UUID vendor_id FK
    BIGINT amount_paise
    VARCHAR status
  }
  VENDOR_BOOKINGS {
    UUID id PK
    UUID quote_id UK,FK
    UUID wedding_id FK
    UUID vendor_id FK
    DATE service_date
    VARCHAR status
  }
  VENDOR_UNAVAILABILITY {
    UUID id PK
    UUID vendor_id FK
    DATE unavailable_date
  }
  VENDOR_REVIEWS {
    UUID id PK
    UUID booking_id UK,FK
    INT rating
  }
  CATERING_MENUS {
    UUID id PK
    UUID wedding_id FK
    UUID ceremony_id FK
    VARCHAR meal
    INT guest_count
  }
  CATERING_ITEMS {
    UUID id PK
    UUID menu_id FK
    VARCHAR name
    VARCHAR diet
    INT quantity
  }
  APP_USERS ||--o{ VENDOR_PROFILES : operates
  VENDORS ||--o| VENDOR_PROFILES : approved_as
  VENDORS ||--o{ VENDOR_SERVICES : offers
  WEDDINGS ||--o{ ENQUIRIES : creates
  VENDORS ||--o{ ENQUIRIES : receives
  ENQUIRIES ||--o{ VENDOR_QUOTES : quotes
  VENDORS ||--o{ VENDOR_QUOTES : prepares
  VENDOR_QUOTES ||--o| VENDOR_BOOKINGS : accepted_as
  WEDDINGS ||--o{ VENDOR_BOOKINGS : books
  VENDORS ||--o{ VENDOR_BOOKINGS : fulfils
  VENDORS ||--o{ VENDOR_UNAVAILABILITY : blocks
  VENDOR_BOOKINGS ||--o| VENDOR_REVIEWS : reviewed
  WEDDINGS ||--o{ CATERING_MENUS : plans
  CEREMONIES |o--o{ CATERING_MENUS : linked_to
  CATERING_MENUS ||--o{ CATERING_ITEMS : contains
```

**Booking invariant:** PostgreSQL partial unique index `ux_active_vendor_day` covers one `CONFIRMED` booking for a vendor/service date (not all statuses). Accepting a quote requires source-level locks and vendor availability checks. `catering_menus` has a `UNIQUE(wedding_id,ceremony_id,meal)` constraint, but PostgreSQL uniqueness of nullable ceremony_id needs separate handling; the controller also serializes and rejects duplicate null-ceremony menus.

## Finance, notifications and auditing

```mermaid
erDiagram
  APP_USERS {
    UUID id PK
  }
  WEDDINGS {
    UUID id PK
  }
  VENDORS {
    UUID id PK
  }
  VENDOR_BOOKINGS {
    UUID id PK
    UUID wedding_id FK
    UUID vendor_id FK
  }
  PAYMENT_RECORDS {
    UUID id PK
    UUID booking_id FK
    BIGINT amount_paise
    VARCHAR status
    VARCHAR razorpay_order_id UK
    VARCHAR razorpay_payment_id UK
  }
  PAYMENT_REFUNDS {
    UUID id PK
    UUID payment_id FK
    UUID requested_by FK
    VARCHAR request_key
    BIGINT amount_paise
    BIGINT deduction_paise
    VARCHAR reason
    VARCHAR status
    VARCHAR razorpay_refund_id UK
  }
  PAYMENT_EVENTS {
    UUID id PK
    UUID payment_id FK
    VARCHAR provider_event_id UK
    VARCHAR event_type
  }
  SETTLEMENT_ENTRIES {
    UUID id PK
    UUID payment_id FK
    UUID vendor_id FK
    BIGINT gross_paise
    BIGINT fee_paise
    BIGINT net_paise
    VARCHAR status
  }
  USER_NOTIFICATIONS {
    UUID id PK
    UUID user_id FK
    VARCHAR kind
    TIMESTAMPTZ read_at
  }
  NOTIFICATION_OUTBOX {
    UUID id PK
    VARCHAR recipient
    VARCHAR channel
    VARCHAR status
    INT attempts
    TIMESTAMPTZ claimed_at
    TIMESTAMPTZ next_attempt_at
  }
  AUDIT_EVENTS {
    UUID id PK
    UUID actor_id FK
    VARCHAR action
    UUID subject_id
    INT http_status
    VARCHAR request_id
    NUMERIC latitude
    NUMERIC longitude
    VARCHAR location_source
  }
  VENDOR_BOOKINGS ||--o{ PAYMENT_RECORDS : charged
  PAYMENT_RECORDS ||--o{ PAYMENT_REFUNDS : refunded
  APP_USERS |o--o{ PAYMENT_REFUNDS : authorizes
  PAYMENT_RECORDS ||--o{ PAYMENT_EVENTS : verified_by
  PAYMENT_RECORDS ||--o{ SETTLEMENT_ENTRIES : recorded_in
  VENDORS ||--o{ SETTLEMENT_ENTRIES : beneficiary
  APP_USERS ||--o{ USER_NOTIFICATIONS : receives
  APP_USERS |o--o{ AUDIT_EVENTS : performs
```

**Declared versus implied relationships:** `notification_outbox.recipient` is a string, not an FK to `app_users`. `audit_events.subject_id` is polymorphic, not an FK. `payment_refunds.request_key` has a partial **composite unique index on (payment_id,request_key)**. `settlement_entries` does **not** make live transfers to vendors; it is not a certified payout ledger.

## Schema integrity and security rules

- Keep bearer tokens as SHA-256 hashes in `auth_tokens`; only the browser receives a raw bearer token. Enforce 401 for missing/expired tokens and 403 for wrong-role operations.
- Keep money in integer paise; enforce overpayment, over-refund and concurrent booking controls at the database transaction layer.
- Use **admin-only** refund creation, a cancelled booking, a mandatory reason and an idempotency key; `QUEUED` and `PROCESSING` are not financially completed states.
- Provider-signed refund callbacks validate provider payment ID, amount and INR currency. Unknown provider outcomes need explicit reconciliation without blind replay.
- Keep email delivery status independent of payment confirmation. V12 enables at-least-once delivery but cannot guarantee exactly once.
- Store optional event location only with user consent and a declared unavailable reason; never treat client GPS as authoritative identity or attendance proof.
- Restrict DB exports, user guest data and audit geo records; document retention, encryption, backups and account deletion policy.

## Source of truth

`backend/src/main/resources/db/migration/V1__initial.sql` through `V12__notification_delivery_recovery.sql`, supplemented by actual target Flyway checksums and read-only `pg_dump --schema-only`. For an operator-checked schema export use [guarded migration tool](migrate-safe.sh) in `export-schema` mode. No migration has been applied by this documentation update.

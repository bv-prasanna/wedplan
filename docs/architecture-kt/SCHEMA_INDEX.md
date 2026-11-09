# WedPlan/WedFlow - SQL and ER Catalog

> All **8** Flyway SQL migration files on current `main` were inspected on 2026-11-09. **Source-derived**, not verified live PostgreSQL physical schema.

## Migration source links
- [`V1__initial.sql`](../../backend/src/main/resources/db/migration/V1__initial.sql)
- [`V2__auth_tokens.sql`](../../backend/src/main/resources/db/migration/V2__auth_tokens.sql)
- [`V3__planning.sql`](../../backend/src/main/resources/db/migration/V3__planning.sql)
- [`V4__commerce.sql`](../../backend/src/main/resources/db/migration/V4__commerce.sql)
- [`V5__operations.sql`](../../backend/src/main/resources/db/migration/V5__operations.sql)
- [`V6__constraints.sql`](../../backend/src/main/resources/db/migration/V6__constraints.sql)
- [`V7__razorpay.sql`](../../backend/src/main/resources/db/migration/V7__razorpay.sql)
- [`V8__event_geolocation.sql`](../../backend/src/main/resources/db/migration/V8__event_geolocation.sql)

## ER model — newly created tables and explicit references
```mermaid
erDiagram
    APP_USERS {
        UUID id
        VARCHAR254 email
        VARCHAR255 password_hash
        VARCHAR32 role
        TIMESTAMPTZ created_at
    }
    WEDDINGS {
        UUID id
        UUID owner_id
        VARCHAR200 title
        VARCHAR120 city
        DATE event_date
        BIGINT total_budget_paise
    }
    CEREMONIES {
        UUID id
        UUID wedding_id
        VARCHAR120 name
        DATE event_date
        VARCHAR250 venue
    }
    VENDORS {
        UUID id
        VARCHAR200 display_name
        VARCHAR80 category
        VARCHAR120 city
        BOOLEAN approved
    }
    ENQUIRIES {
        UUID id
        UUID wedding_id
        UUID vendor_id
        VARCHAR4000 message
        VARCHAR32 status
        TIMESTAMPTZ created_at
    }
    AUTH_TOKENS {
        UUID id
        UUID user_id
        VARCHAR64 token_hash
        TIMESTAMPTZ expires_at
    }
    BUDGET_CATEGORIES {
        UUID id
        UUID wedding_id
        VARCHAR100 name
        BIGINT allocated_paise
        BIGINT spent_paise
    }
    WEDDING_TASKS {
        UUID id
        UUID wedding_id
        VARCHAR200 title
        DATE due_date
        BOOLEAN completed
    }
    WEDDING_GUESTS {
        UUID id
        UUID wedding_id
        VARCHAR160 full_name
        VARCHAR30 phone
        VARCHAR20 rsvp
    }
    VENDOR_PROFILES {
        UUID id
        UUID owner_id
        VARCHAR2000 description
        VARCHAR20 verification_status
    }
    VENDOR_SERVICES {
        UUID id
        UUID vendor_id
        VARCHAR200 title
        BIGINT base_price_paise
        BOOLEAN active
    }
    VENDOR_QUOTES {
        UUID id
        UUID enquiry_id
        UUID vendor_id
        BIGINT amount_paise
        VARCHAR2000 description
        VARCHAR20 status
    }
    VENDOR_BOOKINGS {
        UUID id
        UUID quote_id
        UUID wedding_id
        UUID vendor_id
        VARCHAR20 status
        TIMESTAMPTZ created_at
    }
    CATERING_MENUS {
        UUID id
        UUID wedding_id
        UUID ceremony_id
        VARCHAR30 meal
        INT guest_count
    }
    CATERING_ITEMS {
        UUID id
        UUID menu_id
        VARCHAR150 name
        VARCHAR20 diet
        INT quantity
    }
    PAYMENT_RECORDS {
        UUID id
        UUID booking_id
        BIGINT amount_paise
        VARCHAR200 external_reference
        VARCHAR20 status
        TIMESTAMPTZ created_at
    }
    VENDOR_UNAVAILABILITY {
        UUID id
        UUID vendor_id
        DATE unavailable_date
        VARCHAR200 reason
    }
    VENDOR_REVIEWS {
        UUID id
        UUID booking_id
        INT rating
        VARCHAR1000 comment
        TIMESTAMPTZ created_at
    }
    USER_NOTIFICATIONS {
        UUID id
        UUID user_id
        VARCHAR40 kind
        VARCHAR200 title
        VARCHAR1000 body
        TIMESTAMPTZ read_at
    }
    PAYMENT_EVENTS {
        UUID id
        UUID payment_id
        VARCHAR200 provider_event_id
        VARCHAR40 event_type
        TIMESTAMPTZ received_at
    }
    AUDIT_EVENTS {
        UUID id
        UUID actor_id
        VARCHAR100 action
        UUID subject_id
        TIMESTAMPTZ created_at
    }
    PAYMENT_REFUNDS {
        UUID id
        UUID payment_id
        VARCHAR100 razorpay_refund_id
        BIGINT amount_paise
        VARCHAR30 status
        TIMESTAMPTZ created_at
    }
    SETTLEMENT_ENTRIES {
        UUID id
        UUID payment_id
        UUID vendor_id
        BIGINT gross_paise
        BIGINT fee_paise
        BIGINT net_paise
    }
    NOTIFICATION_OUTBOX {
        UUID id
        VARCHAR254 recipient
        VARCHAR20 channel
        VARCHAR200 subject
        VARCHAR4000 body
        VARCHAR20 status
    }
    APP_USERS ||--o{ WEDDINGS : references
    WEDDINGS ||--o{ CEREMONIES : references
    WEDDINGS ||--o{ ENQUIRIES : references
    VENDORS ||--o{ ENQUIRIES : references
    APP_USERS ||--o{ AUTH_TOKENS : references
    WEDDINGS ||--o{ BUDGET_CATEGORIES : references
    WEDDINGS ||--o{ WEDDING_TASKS : references
    WEDDINGS ||--o{ WEDDING_GUESTS : references
    VENDORS ||--o{ VENDOR_PROFILES : references
    APP_USERS ||--o{ VENDOR_PROFILES : references
    VENDORS ||--o{ VENDOR_SERVICES : references
    ENQUIRIES ||--o{ VENDOR_QUOTES : references
    VENDORS ||--o{ VENDOR_QUOTES : references
    VENDOR_QUOTES ||--o{ VENDOR_BOOKINGS : references
    WEDDINGS ||--o{ VENDOR_BOOKINGS : references
    VENDORS ||--o{ VENDOR_BOOKINGS : references
    WEDDINGS ||--o{ CATERING_MENUS : references
    CEREMONIES ||--o{ CATERING_MENUS : references
    CATERING_MENUS ||--o{ CATERING_ITEMS : references
    VENDOR_BOOKINGS ||--o{ PAYMENT_RECORDS : references
    VENDORS ||--o{ VENDOR_UNAVAILABILITY : references
    VENDOR_BOOKINGS ||--o{ VENDOR_REVIEWS : references
    APP_USERS ||--o{ USER_NOTIFICATIONS : references
    PAYMENT_RECORDS ||--o{ PAYMENT_EVENTS : references
    APP_USERS ||--o{ AUDIT_EVENTS : references
    PAYMENT_RECORDS ||--o{ PAYMENT_REFUNDS : references
    PAYMENT_RECORDS ||--o{ SETTLEMENT_ENTRIES : references
    VENDORS ||--o{ SETTLEMENT_ENTRIES : references
```

## Table and column dictionary
### `app_users`
Introduced by [`V1__initial.sql`](../../backend/src/main/resources/db/migration/V1__initial.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `email` | `VARCHAR(254)` |
| `password_hash` | `VARCHAR(255)` |
| `role` | `VARCHAR(32)` |
| `created_at` | `TIMESTAMPTZ` |

### `weddings`
Introduced by [`V1__initial.sql`](../../backend/src/main/resources/db/migration/V1__initial.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `owner_id` | `UUID` |
| `title` | `VARCHAR(200)` |
| `city` | `VARCHAR(120)` |
| `event_date` | `DATE` |
| `total_budget_paise` | `BIGINT` |
| `created_at` | `TIMESTAMPTZ` |

### `ceremonies`
Introduced by [`V1__initial.sql`](../../backend/src/main/resources/db/migration/V1__initial.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `wedding_id` | `UUID` |
| `name` | `VARCHAR(120)` |
| `event_date` | `DATE` |
| `venue` | `VARCHAR(250)` |

### `vendors`
Introduced by [`V1__initial.sql`](../../backend/src/main/resources/db/migration/V1__initial.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `display_name` | `VARCHAR(200)` |
| `category` | `VARCHAR(80)` |
| `city` | `VARCHAR(120)` |
| `approved` | `BOOLEAN` |

### `enquiries`
Introduced by [`V1__initial.sql`](../../backend/src/main/resources/db/migration/V1__initial.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `wedding_id` | `UUID` |
| `vendor_id` | `UUID` |
| `message` | `VARCHAR(4000)` |
| `status` | `VARCHAR(32)` |
| `created_at` | `TIMESTAMPTZ` |

### `auth_tokens`
Introduced by [`V2__auth_tokens.sql`](../../backend/src/main/resources/db/migration/V2__auth_tokens.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `user_id` | `UUID` |
| `token_hash` | `VARCHAR(64)` |
| `expires_at` | `TIMESTAMPTZ` |

### `budget_categories`
Introduced by [`V3__planning.sql`](../../backend/src/main/resources/db/migration/V3__planning.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `wedding_id` | `UUID` |
| `name` | `VARCHAR(100)` |
| `allocated_paise` | `BIGINT` |
| `spent_paise` | `BIGINT` |

### `wedding_tasks`
Introduced by [`V3__planning.sql`](../../backend/src/main/resources/db/migration/V3__planning.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `wedding_id` | `UUID` |
| `title` | `VARCHAR(200)` |
| `due_date` | `DATE` |
| `completed` | `BOOLEAN` |

### `wedding_guests`
Introduced by [`V3__planning.sql`](../../backend/src/main/resources/db/migration/V3__planning.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `wedding_id` | `UUID` |
| `full_name` | `VARCHAR(160)` |
| `phone` | `VARCHAR(30)` |
| `rsvp` | `VARCHAR(20)` |

### `vendor_profiles`
Introduced by [`V4__commerce.sql`](../../backend/src/main/resources/db/migration/V4__commerce.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `owner_id` | `UUID` |
| `description` | `VARCHAR(2000)` |
| `verification_status` | `VARCHAR(20)` |

### `vendor_services`
Introduced by [`V4__commerce.sql`](../../backend/src/main/resources/db/migration/V4__commerce.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `vendor_id` | `UUID` |
| `title` | `VARCHAR(200)` |
| `base_price_paise` | `BIGINT` |
| `active` | `BOOLEAN` |

### `vendor_quotes`
Introduced by [`V4__commerce.sql`](../../backend/src/main/resources/db/migration/V4__commerce.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `enquiry_id` | `UUID` |
| `vendor_id` | `UUID` |
| `amount_paise` | `BIGINT` |
| `description` | `VARCHAR(2000)` |
| `status` | `VARCHAR(20)` |
| `created_at` | `TIMESTAMPTZ` |

### `vendor_bookings`
Introduced by [`V4__commerce.sql`](../../backend/src/main/resources/db/migration/V4__commerce.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `quote_id` | `UUID` |
| `wedding_id` | `UUID` |
| `vendor_id` | `UUID` |
| `status` | `VARCHAR(20)` |
| `created_at` | `TIMESTAMPTZ` |

### `catering_menus`
Introduced by [`V4__commerce.sql`](../../backend/src/main/resources/db/migration/V4__commerce.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `wedding_id` | `UUID` |
| `ceremony_id` | `UUID` |
| `meal` | `VARCHAR(30)` |
| `guest_count` | `INT` |

### `catering_items`
Introduced by [`V4__commerce.sql`](../../backend/src/main/resources/db/migration/V4__commerce.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `menu_id` | `UUID` |
| `name` | `VARCHAR(150)` |
| `diet` | `VARCHAR(20)` |
| `quantity` | `INT` |

### `payment_records`
Introduced by [`V4__commerce.sql`](../../backend/src/main/resources/db/migration/V4__commerce.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `booking_id` | `UUID` |
| `amount_paise` | `BIGINT` |
| `external_reference` | `VARCHAR(200)` |
| `status` | `VARCHAR(20)` |
| `created_at` | `TIMESTAMPTZ` |

### `vendor_unavailability`
Introduced by [`V5__operations.sql`](../../backend/src/main/resources/db/migration/V5__operations.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `vendor_id` | `UUID` |
| `unavailable_date` | `DATE` |
| `reason` | `VARCHAR(200)` |

### `vendor_reviews`
Introduced by [`V5__operations.sql`](../../backend/src/main/resources/db/migration/V5__operations.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `booking_id` | `UUID` |
| `rating` | `INT` |
| `comment` | `VARCHAR(1000)` |
| `created_at` | `TIMESTAMPTZ` |

### `user_notifications`
Introduced by [`V5__operations.sql`](../../backend/src/main/resources/db/migration/V5__operations.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `user_id` | `UUID` |
| `kind` | `VARCHAR(40)` |
| `title` | `VARCHAR(200)` |
| `body` | `VARCHAR(1000)` |
| `read_at` | `TIMESTAMPTZ` |
| `created_at` | `TIMESTAMPTZ` |

### `payment_events`
Introduced by [`V5__operations.sql`](../../backend/src/main/resources/db/migration/V5__operations.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `payment_id` | `UUID` |
| `provider_event_id` | `VARCHAR(200)` |
| `event_type` | `VARCHAR(40)` |
| `received_at` | `TIMESTAMPTZ` |

### `audit_events`
Introduced by [`V5__operations.sql`](../../backend/src/main/resources/db/migration/V5__operations.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `actor_id` | `UUID` |
| `action` | `VARCHAR(100)` |
| `subject_id` | `UUID` |
| `created_at` | `TIMESTAMPTZ` |

### `payment_refunds`
Introduced by [`V7__razorpay.sql`](../../backend/src/main/resources/db/migration/V7__razorpay.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `payment_id` | `UUID` |
| `razorpay_refund_id` | `VARCHAR(100)` |
| `amount_paise` | `BIGINT` |
| `status` | `VARCHAR(30)` |
| `created_at` | `TIMESTAMPTZ` |

### `settlement_entries`
Introduced by [`V7__razorpay.sql`](../../backend/src/main/resources/db/migration/V7__razorpay.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `payment_id` | `UUID` |
| `vendor_id` | `UUID` |
| `gross_paise` | `BIGINT` |
| `fee_paise` | `BIGINT` |
| `net_paise` | `BIGINT` |
| `status` | `VARCHAR(30)` |
| `created_at` | `TIMESTAMPTZ` |

### `notification_outbox`
Introduced by [`V7__razorpay.sql`](../../backend/src/main/resources/db/migration/V7__razorpay.sql)

| Column | Declared type |
|---|---|
| `id` | `UUID` |
| `recipient` | `VARCHAR(254)` |
| `channel` | `VARCHAR(20)` |
| `subject` | `VARCHAR(200)` |
| `body` | `VARCHAR(4000)` |
| `status` | `VARCHAR(20)` |
| `attempts` | `INT` |
| `created_at` | `TIMESTAMPTZ` |
| `sent_at` | `TIMESTAMPTZ` |

## Added columns in later migrations
| Table | Column | Type | Migration |
|---|---|---|---|
| `vendor_bookings` | `service_date` | `DATE` | [SQL](../../backend/src/main/resources/db/migration/V5__operations.sql) |
| `vendor_bookings` | `cancelled_at` | `TIMESTAMPTZ` | [SQL](../../backend/src/main/resources/db/migration/V5__operations.sql) |
| `payment_records` | `razorpay_order_id` | `VARCHAR` | [SQL](../../backend/src/main/resources/db/migration/V7__razorpay.sql) |
| `payment_records` | `razorpay_payment_id` | `VARCHAR` | [SQL](../../backend/src/main/resources/db/migration/V7__razorpay.sql) |
| `audit_events` | `latitude` | `NUMERIC` | [SQL](../../backend/src/main/resources/db/migration/V8__event_geolocation.sql) |
| `audit_events` | `longitude` | `NUMERIC` | [SQL](../../backend/src/main/resources/db/migration/V8__event_geolocation.sql) |
| `audit_events` | `location_source` | `VARCHAR` | [SQL](../../backend/src/main/resources/db/migration/V8__event_geolocation.sql) |
| `audit_events` | `location_accuracy_m` | `NUMERIC` | [SQL](../../backend/src/main/resources/db/migration/V8__event_geolocation.sql) |
| `audit_events` | `location_recorded_at` | `TIMESTAMPTZ` | [SQL](../../backend/src/main/resources/db/migration/V8__event_geolocation.sql) |
| `audit_events` | `location_unavailable_reason` | `VARCHAR` | [SQL](../../backend/src/main/resources/db/migration/V8__event_geolocation.sql) |

## Deployment verification and KT
- Test wedding planning, vendor quotes, order and cancellation flows, Razorpay **sandbox** callbacks, user permissions and event geolocation retention/consent.
- Before applying migrations: exact DB instance, Flyway history, checksum, backup/restore, staging tests, approved version/release and post-migrate smoke.
- Reconcile with `pg_dump --schema-only` for final FK, indexes, views, triggers and all ALTER/DROP changes. This parser is conservative and not equivalent to live introspection.
- Handover: [architecture/deployment/UAT and maintenance](README.md) · [guarded migration operator script](migrate-safe.sh).

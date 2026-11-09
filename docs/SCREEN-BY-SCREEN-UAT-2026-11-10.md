# WedFlow — page-by-page UX, API status, security and release test matrix

**As of 2026-11-10:** Source presence checked on GitHub. **No deployed visual review, cross-browser tests, PostgreSQL build, or live payment verification has been completed.** “Implemented” means endpoint and/or UI source is present, not that UAT passed. Reference [architecture and KT](architecture-kt/README.md) and [security matrix](architecture-kt/SECURITY-TEST-MATRIX.md).

## Pages, flows and expected behavior

| Screen / route | Intended behavior | Negative / UI and accessibility review | Source status |
|---|---|---|---|
| Sign in `#/login` | Login with email/password, session-scoped access token | Wrong password 401, throttled 429, password masked, no technical leaks, field labels | Implemented; unverified |
| Registration `#/register` | Register customer and return to login | Duplicate email 409, bad email/short password 400, no account enumeration mitigation | Partial; verification/recovery pending |
| Dashboard `#/overview` | Wedding cards, total budget, upcoming dates, create workspace | No weddings, invalid amount/date, offline, duplicate click, mobile card stacking | Implemented; unverified |
| Ceremonies `#/planning/ceremonies` | List/add ceremony name/date/venue | Empty state, optional venue, backdated event policy, cross-owner 404 | Partial |
| Guests `#/planning/guests` | List/add guests and change RSVP | Missing guest 404, invalid RSVP 400, privacy of phone numbers, keyboard select | Partial; seating/dietary pending |
| Tasks `#/planning/tasks` | List/add and complete tasks | Idempotent completion, invalid task, optional date, focus order | Partial |
| Budget `#/planning/budget` | Allocate categories and show unallocated budget | Exceed total 409, currency validation and rounding, concurrent updates | Partial; actual spend sync pending |
| Catering `#/planning/catering` | Create menus and assign dietary items | Duplicate menu 409, invalid ceremony, required portions, accessible cards | Partial; pricing quote pending |
| Vendor discovery `#/vendors` | Approved vendor search and send enquiries | No results, unapproved vendor must be hidden, invalid/no wedding | Partial; portfolios/advanced search pending |
| Enquiries `#/finance/enquiries` | Review sent requests | Cross-owner isolation, safe long text, status clarity | Implemented; unverified |
| Quotes `#/finance/quotes` | Review offered quotes and accept with service date | Past date 400; concurrent vendor booking 409; quote no longer offered | Partial |
| Bookings `#/finance/bookings` | Show confirmed/cancelled bookings and create payment intent | Repeat cancel 409, overpayment 409, show refund state separately | Partial; amendments pending |
| Payment records `#/finance/payments` | Show PENDING/VERIFIED/FAILED; invoke Razorpay checkout | Checkout close not success; delayed/replayed webhook, mismatched payment, offline | Partial; provider UAT needed |
| Refund request `#/finance/payments` | Request amount on verified payment; show PENDING | Over-refund 409; no instant “processed”; provider reconciliation pending | Partial |
| Notifications `#/notifications` | Display inbox and mark read | Empty inbox, 404 for foreign notification, unread state, mail failure | Partial |
| Vendor registration `#/vendor` | Create profile awaiting admin approval | Invalid details 400, no public listing while pending | Implemented; unverified |
| Vendor services `#/vendor/services` | Add and see owned services | Other vendor 404, bad price 400, inactive handling | Partial |
| Vendor enquiries `#/vendor/enquiries` | Read enquiries and send quotation | Unauthorized vendor 404, unapproved vendor 409, amount validation | Partial |
| Vendor quotes/bookings `#/vendor/quotes` and `#/vendor/bookings` | Read quotations and confirmed work | Cross-tenant isolation, status labels, empty state | Partial |
| Admin approvals `#/admin` | List pending vendors, approve/reject | Customer role 403, nonpending 409, decision logged | Implemented; unverified |

## UI quality acceptance (apply to each page)

- Test responsive viewport widths **360, 390, 768, 1024, 1440 px**. No horizontal overflow, clipped content, inaccessible buttons or mobile OS navigation overlap.
- Check keyboard-only tab order, visible focus state, semantic labels, text contrast, minimum hit area, error recovery, loading indicator and screen-reader announcements.
- Check long user-entered text, empty lists, slow/offline network, expired auth, duplicate taps, refresh and browser back/forward.
- Confirm user messages are honest about **requested / pending / verified / completed** states, especially payment, refund and notification delivery.
- Confirm email, phone, guest information and geo coordinates cannot appear in URL query strings, logs, public responses or another user's account.
- Use authenticated API traffic capture to confirm 400, 401, 403, 404, 405, 409, 415, 429 and 500 are correctly represented, with request ID and without stack traces or credentials.

## Server security scenarios

1. Unauthenticated requests to `/api/v1/weddings` return JSON 401 with request ID.
2. Customer accessing `/api/v1/admin/vendors/pending` returns 403.
3. User A accessing User B's wedding, guest, payment, quote or vendor resource returns a safe 404.
4. Wrong password triggers increasing failure counter, with 429 after threshold; successful login clears count.
5. Vendor pending approval cannot send a quote; approval audit is recorded.
6. Vendor accepted quote cannot be booked for the same service date twice (concurrency test).
7. Auth tokens, OTPs, raw email credentials, Razorpay secrets, webhook signatures, card details and PII are not logged.
8. Razorpay webhook signature and event ID are verified; amount/currency mismatches rejected; replay idempotent.
9. Audit logs include mutation method/path, HTTP status, request ID, actor when known, and client-reported GPS only when actively shared.
10. Geo permission denial or missing coordinates does not block the operation and records an explicit unavailable reason.
11. Provider outage or unknown webhook must not falsely mark a payment or refund successful.
12. Schema migrations are applied only on approved staging/prod with verified backup and restore rehearsal.

## Test evidence required before UAT sign-off

| Artifact | Required evidence |
|---|---|
| Source/build | Git SHA; Java 21 Maven `clean verify`; Node `npm run verify` |
| Coverage | JaCoCo XML/HTML, measured percentage and security critical-path analysis |
| Database | Flyway V1–V10 status and constraints; PostgreSQL `pg_dump --schema-only` |
| Browser/device | Screenshot and test outcome for every route/viewport; accessibility report |
| Integration | Real PostgreSQL test run; Razorpay test-mode payment/refund/webhook receipts |
| Operations | Backup restore, logs, alerts, incident steps, release and rollback owner |
| UAT sign-off | Defect severity list, retest outcome, approver and date |

**Release certification: BLOCKED** until this evidence exists and P0 gaps are resolved. The static web source is newly implemented, not a proven production UI.

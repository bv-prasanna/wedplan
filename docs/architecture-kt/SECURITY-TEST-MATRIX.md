# WedFlow security, status and regression matrix
Status: planned checks. Source review does not prove execution.

| ID | API / workflow | Positive assertion | Negative assertion | Priority |
|---|---|---|---|---|
| AUTH-01 | register/login | own account token issued | wrong password denied, no token leak | P0 |
| AUTH-02 | logout | revoked token rejected | replay after logout denied | P0 |
| AUTH-03 | roles | permitted actor succeeds | customer cannot admin approve | P0 |
| DATA-01 | weddings | owner lists own records | cross-owner get/delete returns 404 | P0 |
| DATA-02 | wedding delete | no-history wedding removable | enquiries/bookings yield 409 | P0 |
| DATA-03 | guests/RSVP | owner mutation persists | foreign wedding/guest denied | P0 |
| VEND-01 | vendor approval | admin approval persists | unapproved vendor cannot transact | P0 |
| BOOK-01 | booking | accepted quote reserves date | duplicate and concurrent booking denied | P0 |
| PAY-01 | checkout | order amount/currency match | cross-owner order creation denied | P0 |
| PAY-02 | webhook | valid HMAC captured once | bad signature/replay/amount mismatch denied | P0 |
| PAY-03 | refund | eligible refund tracked | duplicate/over-refund/cancel conflict denied | P0 |
| PAY-04 | settlement | reconciled vendor ledger | double payout and negative net denied | P0 |
| NOTIF-01 | outbox | delivered message recorded | provider outage retries without false success | P0 |
| AUDIT-01 | geolocation | valid paired coordinates | invalid range/unpaired coordinates rejected | P0 |
| SEC-01 | errors | safe 4xx/5xx + request ID | no stack trace, PII or secrets | P0 |
| SEC-02 | CORS | approved origin accepted | unapproved origin rejected | P0 |
| SEC-03 | abuse | rate limiting enforced | repeated OTP/login/refund abuse blocked | P0 |
| UI-01 | customer screens | responsive and accessible | empty/error/offline/retry handled | P1 |
| UI-02 | vendor/admin screens | roles and clear actions | no unauthorized UI/data exposure | P1 |

For each row attach test name, command, commit SHA, result, timestamp and coverage. Do not substitute code presence for an executed test.

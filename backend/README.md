# WedFlow backend (foundation)

Java 21, Spring Boot 3.5, PostgreSQL, Flyway, JUnit 5 and JaCoCo. This is an **incomplete backend foundation**, not production-ready.

## Run tests

```bash
mvn clean verify
```

## Configure database
Set DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD as environment variables. Never commit passwords.

## Status
Domain budget allocation and initial schema are implemented. Authentication, REST controllers, bookings, payments, security policies and full integration tests are not yet present in this committed slice. Maven verification has not been run in this environment.

## CI policy
No automatic GitHub Actions workflows are included. Run tests locally; introduce manual workflow_dispatch only after agreeing on CI-minute budget.

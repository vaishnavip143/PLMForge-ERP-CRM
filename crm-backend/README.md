# PLMForge ERP – CRM Module Backend

Full-stack intern assessment backend (assignment **IN-LVL-01**): a Spring Boot REST API for
customer queries, leads, tickets, role-based access control, notifications and SLA monitoring.

The backend is intentionally simple and follows the standard
**Controller → Service → Repository** architecture.

---

## 1. Project overview

The CRM module supports three business roles:

| Role | Business name | Main capabilities |
|---|---|---|
| `ROLE_CUSTOMER` | Customer | Register/login, submit queries, view own tickets, TAT and resolution |
| `ROLE_SALES_REPRESENTATIVE` | Sales Representative | View and work on assigned tickets, update status/resolution |
| `ROLE_SALES_MANAGER` | Sales Manager | View all tickets/customers/leads, assign tickets, view notifications and SLA alerts |

Core business flow – **dynamic query ingestion** (`POST /api/tickets/ingest`):

1. The incoming email (or optional `customerId`) is matched against `customers`.
2. **Existing customer** → a new ticket is linked to that customer, status `OPEN`, 24-hour TAT,
   `dueAt = createdAt + 24h`.
3. **Unknown customer** → a `PotentialCustomer` is created **only if the email is new**, a ticket is
   linked to it, and an immediate `NEW_LEAD` notification is stored for every Sales Manager.
4. Repeating the same email creates another ticket but **never a duplicate lead**.
5. If the email belongs to a potential customer who later opens an account, their lead tickets remain
   visible to them and to the Sales Manager.

SLA rule: an active ticket (`OPEN` / `IN_PROGRESS`) with **more than 0 and less than 2 hours**
remaining before `dueAt` is an SLA warning; a past-due active ticket is **overdue**. Resolved and
closed tickets are never flagged. A scheduled task scans every 60 seconds (configurable) and stores
one notification per ticket/type/recipient (duplicate-safe).

---

## 2. Technology stack

- Java 17
- Spring Boot 3.5
- Spring Web (REST)
- Spring Security + JWT (jjwt 0.12)
- Spring Data JPA / Hibernate
- PostgreSQL
- Bean Validation (Jakarta Validation)
- Lombok
- SLF4J / Logback
- Swagger / OpenAPI (springdoc 2.8)

---

## 3. Architecture

```
com.plmforge.crm
├── config
│   ├── SecurityConfig          JWT filter chain + RBAC route rules + CORS
│   ├── OpenApiConfig           Swagger definition + Bearer auth
│   ├── SchedulingConfig        enables @Scheduled SLA scanning
│   └── DataInitializer         development seed data (runs only on an empty DB)
├── controller
│   ├── AuthController          /api/auth/**
│   ├── CustomerController      /api/customers/**
│   ├── TicketController        /api/tickets/**
│   ├── SalesController         /api/sales/**
│   ├── ManagerController       /api/manager/**
│   └── NotificationController  /api/notifications/**
├── service
│   ├── AuthService             register, login, current user
│   ├── CustomerService         profile and customer listing
│   ├── TicketService           ingestion, TAT, ownership, assignment, status
│   ├── LeadService             potential customer listing
│   ├── NotificationService     notification creation and read APIs
│   ├── SlaService              scheduled SLA scan and SLA alert view
│   └── AuditService            simple audit trail
├── repository                  Spring Data JPA repositories
├── entity                      User, Customer, PotentialCustomer, Ticket, Notification, AuditLog
├── dto
│   ├── request                 validated request DTOs
│   └── response                response DTOs and the ApiResponse envelope
├── enums                       Role, TicketStatus, TicketPriority, CustomerType, LeadStatus, NotificationType
├── security                    JwtService, JwtAuthenticationFilter, CustomUserDetailsService,
│                               UserPrincipal, JSON 401/403 handlers
├── exception                   GlobalExceptionHandler + domain exceptions
└── CrmApplication
```

All API responses use one envelope:

```json
{
  "success": true,
  "message": "Login successful",
  "data": { },
  "timestamp": "2026-11-12T23:30:00"
}
```

Error responses contain `success`, `status`, `error`, `message`, `path`, `timestamp` and, for
validation failures, a `validationErrors` map.

---

## 4. PostgreSQL configuration

`src/main/resources/application.properties` (existing local database):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/crm_db
spring.datasource.username=postgres
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:your-password}
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

Change only the username/password if your PostgreSQL installation differs. The committed
template is `application.properties.example`; copy it to `application.properties` and replace the
placeholders. The real local file is git-ignored.

### Database setup

1. Make sure PostgreSQL is running on port `5432`.
2. Create the database (once):

```sql
CREATE DATABASE crm_db;
```

Optionally create the tables manually with `database/schema.sql`:

```bash
psql -U postgres -d crm_db -f database/schema.sql
```

With `ddl-auto=update`, Hibernate creates/updates all tables on startup, so manual creation is not
required.

### Tables

| Table | Purpose |
|---|---|
| `users` | login accounts and roles (BCrypt passwords) |
| `customers` | verified/existing customers (unique email) |
| `potential_customers` | leads captured from unknown senders (one row per email) |
| `query_tickets` | tickets; `sla_deadline` column stores `dueAt`, `assigned_to_user_id` stores the representative |
| `notifications` | per-recipient notifications for managers and representatives |
| `audit_logs` | simple audit trail of business operations |

> The existing table `alert_notifications` from the earlier prototype is no longer used.
> It can be dropped manually once you no longer need its data
> (`DROP TABLE alert_notifications;`). No application data lives there.

---

## 5. Running the application

Prerequisites: Java 17+, Maven (wrapper included), PostgreSQL running.

```bash
# Windows
mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

or build and run the jar:

```bash
./mvnw clean package
java -jar target/crm-backend-0.0.1-SNAPSHOT.jar
```

- API: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

On an **empty** database the `DataInitializer` creates the development accounts and sample records
below. On an existing database it does nothing, so your data is preserved.

### Development test accounts

All seed users share a development password defined in `DataInitializer` (stored as a BCrypt
hash, never plain text). It is intentionally not reproduced in this document.

| Role | Email |
|---|---|
| Customer | `customer@erp.com` |
| Sales Representative | `salesrep@erp.com` |
| Sales Manager | `manager@erp.com` |

Sample existing customers: `acme@acmecorp.com`, `techcorp@techcorp.io`, `customer@erp.com`.

> These accounts are for local development and Postman testing only. Sales representatives and
> managers are provisioned through seed data; public registration always creates a `CUSTOMER`.

---

## 6. Authentication and JWT

### Register (public)

```http
POST /api/auth/register
Content-Type: application/json

{
  "name": "Jane Customer",
  "email": "jane@example.com",
  "password": "secretPass123"
}
```

```json
{
  "success": true,
  "message": "Customer registered successfully",
  "data": {
    "userId": 4,
    "name": "Jane Customer",
    "email": "jane@example.com",
    "role": "CUSTOMER",
    "createdAt": "2026-11-12T23:30:00"
  },
  "timestamp": "2026-11-12T23:30:00"
}
```

- A `CUSTOMER` role is always assigned; the request has no role field at all.
- Passwords are BCrypt-hashed.
- Duplicate email returns **409 Conflict**.
- A matching `customers` profile is created automatically so self-service APIs work immediately.

### Login

```http
POST /api/auth/login
Content-Type: application/json

{ "email": "manager@erp.com", "password": "<seed-password>" }
```

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "userId": 2,
    "name": "Sarah Connor",
    "email": "manager@erp.com",
    "role": "SALES_MANAGER"
  },
  "timestamp": "2026-11-12T23:30:00"
}
```

### Using the token

Send it on every protected request:

```http
Authorization: Bearer <token>
```

In Swagger UI click **Authorize** and paste the token (without `Bearer`).

`GET /api/auth/me` returns the authenticated user id, name, email, role and `authenticated: true`.
The identity always comes from the validated JWT, never from a request body.

Invalid/expired/missing token → **401**, valid token with the wrong role → **403**.

---

## 7. API endpoints

| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register a customer |
| POST | `/api/auth/login` | Public | Login and receive JWT |
| GET | `/api/auth/me` | Any authenticated | Current user |
| POST | `/api/tickets/ingest` | Customer, Rep, Manager | Ingest a query (existing customer or lead) |
| GET | `/api/tickets/my` | Customer | Own tickets |
| GET | `/api/tickets/{id}` | All roles (ownership enforced) | One ticket |
| GET | `/api/sales/tickets` | Representative, Manager | Assigned tickets (manager: all) |
| PUT | `/api/sales/tickets/{id}/status` | Representative (own), Manager | Update status/resolution |
| GET | `/api/manager/tickets` | Manager | All tickets |
| GET | `/api/manager/leads` | Manager | All potential customers |
| GET | `/api/manager/customers` | Manager | All existing customers |
| GET | `/api/manager/representatives` | Manager | Representatives available for assignment |
| POST | `/api/manager/tickets/{id}/assign` | Manager | Assign ticket to a representative |
| GET | `/api/manager/sla-alerts` | Manager | SLA warning + overdue tickets |
| GET | `/api/manager/notifications` | Manager | Manager notifications |
| GET | `/api/manager/tickets/{id}/audit` | Manager | Ticket audit trail |
| GET | `/api/notifications` | Any authenticated | Own notifications |
| GET | `/api/notifications/unread` | Any authenticated | Own unread notifications |
| PATCH | `/api/notifications/{id}/read` | Owner | Mark notification read |
| GET | `/api/customers/my-profile` | Customer | Own customer profile |
| GET | `/api/customers/my-tickets` | Customer | Own tickets with TAT |

### Query ingestion examples

**Existing customer** (email or `customerId` matches):

```http
POST /api/tickets/ingest
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Alice Smith",
  "email": "acme@acmecorp.com",
  "subject": "Invoice discrepancy",
  "description": "Invoice 8819 has the wrong discount.",
  "phone": "+1-555-0102"
}
```

```json
{
  "success": true,
  "message": "Query registered for existing customer. 24-hour TAT applied.",
  "data": {
    "ticket": {
      "id": 12,
      "ticketNumber": "TICK-20261112-483920",
      "subject": "Invoice discrepancy",
      "description": "Invoice 8819 has the wrong discount.",
      "customerType": "EXISTING_CUSTOMER",
      "status": "OPEN",
      "priority": "MEDIUM",
      "customerId": 2,
      "customerName": "Alice Smith",
      "customerEmail": "acme@acmecorp.com",
      "assignedToId": null,
      "tatHours": 24,
      "createdAt": "2026-11-12T23:30:00",
      "dueAt": "2026-11-13T23:30:00",
      "remainingMinutes": 1439,
      "remainingTime": "23h 59m remaining",
      "slaAlert": false,
      "overdue": false
    },
    "potentialCustomer": null,
    "notificationCreated": false,
    "message": "Query registered for existing customer. 24-hour TAT applied."
  },
  "timestamp": "2026-11-12T23:30:00"
}
```

**New / potential customer**:

```json
{
  "name": "Michael Green",
  "email": "newprospect@example.com",
  "subject": "ERP demo request",
  "description": "We need a demo for 120 employees.",
  "phone": "+1-555-0199"
}
```

The response contains the ticket, the `potentialCustomer` record, `notificationCreated: true` and the
`notificationMessage`. Sending the same email again creates another ticket, reuses the lead and
notifies the manager again (no duplicate lead row).

### Assignment

```http
POST /api/manager/tickets/12/assign
Authorization: Bearer <manager-token>
Content-Type: application/json

{ "representativeId": 1 }
```

```json
{
  "success": true,
  "message": "Ticket assigned successfully",
  "data": {
    "id": 12,
    "status": "IN_PROGRESS",
    "assignedToId": 1,
    "assignedToName": "Alex Turner",
    "assignedToEmail": "salesrep@erp.com",
    "remainingTime": "23h 20m remaining",
    "slaAlert": false,
    "overdue": false
  },
  "timestamp": "2026-11-12T23:40:00"
}
```

`GET /api/manager/representatives` returns the valid ids. The assigned representative receives a
`TICKET_ASSIGNED` notification.

### Status update

```http
PUT /api/sales/tickets/12/status
Authorization: Bearer <rep-token>
Content-Type: application/json

{
  "status": "RESOLVED",
  "resolutionNotes": "Invoice corrected and resent to the customer."
}
```

Allowed statuses: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`. Resolving/closing sets `resolvedAt`;
reopening clears it. A representative can only update tickets assigned to them.

### Notifications and SLA

```http
GET /api/notifications
GET /api/notifications/unread
PATCH /api/notifications/5/read
GET /api/manager/notifications
GET /api/manager/sla-alerts
```

`GET /api/manager/sla-alerts`:

```json
{
  "success": true,
  "message": "SLA alerts retrieved",
  "data": {
    "warningTickets": [ { "ticketNumber": "TICK-20261112-100002", "remainingTime": "1h 15m remaining", "slaAlert": true } ],
    "overdueTickets": [ { "ticketNumber": "TICK-20261112-100005", "remainingTime": "Overdue by 2h 10m", "overdue": true } ],
    "warningCount": 1,
    "overdueCount": 1,
    "generatedAt": "2026-11-12T23:45:00"
  },
  "timestamp": "2026-11-12T23:45:00"
}
```

---

## 8. Role permissions

| Capability | Customer | Representative | Manager |
|---|---|---|---|
| Register (public) | ✅ | – | – |
| Login / `/auth/me` | ✅ | ✅ | ✅ |
| Submit query | ✅ | ✅ | ✅ |
| View own tickets | ✅ | – | – |
| View/update own assigned tickets | – | ✅ | ✅ |
| View another customer's ticket | ❌ 403 | ❌ 403 | ✅ |
| View all tickets | ❌ | ❌ | ✅ |
| Assign tickets | ❌ | ❌ | ✅ |
| View leads / customers | ❌ | ❌ | ✅ |
| SLA alerts | ❌ | ❌ | ✅ |
| Own notifications | ✅ | ✅ | ✅ |

Authorization is enforced twice: route rules in `SecurityConfig` and ownership checks inside
`TicketService` / `NotificationService`.

---

## 9. Postman testing guide

1. Start PostgreSQL and the application (`mvnw.cmd spring-boot:run`).
2. Import `http://localhost:8080/v3/api-docs` as an OpenAPI collection in Postman, or create the
   requests manually.
3. **Login**: `POST /api/auth/login` with `manager@erp.com` / `<seed-password>`.
   Copy `data.token` into a collection variable, e.g. `token`.
4. For protected requests set header `Authorization: Bearer {{token}}`.
5. Suggested flow:
   1. `POST /api/auth/register` with a new email → 201, role `ROLE_CUSTOMER`.
   2. Repeat the same registration → 409.
   3. Login as the manager and the representative.
   4. `POST /api/tickets/ingest` with `acme@acmecorp.com` (existing) → ticket with 24h TAT.
   5. `POST /api/tickets/ingest` with a new email (lead) → potential customer + notification.
   6. Repeat step 5 with the same email → new ticket, no duplicate lead, new notification.
   7. `GET /api/manager/leads`, `GET /api/manager/notifications`.
   8. `GET /api/manager/representatives`, then assign a ticket.
   9. Login as the representative, `GET /api/sales/tickets`, update the status to `RESOLVED`.
   10. Login as the customer, `GET /api/tickets/my` and `GET /api/tickets/{ownId}`.
   11. Try reading another customer's ticket → 403.
   12. Call a manager API with the representative/customer token → 403.
   13. Call any private API without a token → 401.
   14. `GET /api/manager/sla-alerts` (the seeded ticket `TICK-…-100002` is within 2 hours).

### Curl example

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"manager@erp.com","password":"<seed-password>"}' | jq -r .data.token)

curl -s http://localhost:8080/api/manager/tickets -H "Authorization: Bearer $TOKEN"
```

---

## 10. Validation, errors, logging and audit

- DTO validation: `@NotBlank`, `@NotNull`, `@Email`, `@Size`, `@Pattern`, `@Positive`.
- `GlobalExceptionHandler` returns:
  | Situation | HTTP status |
  |---|---|
  | Validation failure | 400 |
  | Malformed JSON / invalid enum | 400 |
  | Missing/invalid JWT | 401 |
  | Invalid credentials | 401 |
  | Wrong role / ownership violation | 403 |
  | Not found | 404 |
  | Duplicate email / constraint conflict | 409 |
  | Unexpected error | 500 (generic message, stack trace only in server logs) |
- SLF4J logs: registration, login success/failure, ingestion branch, new lead, manager
  notification, assignment, status changes, SLA warnings/breaches, with ids. Passwords, hashes and
  JWT tokens are never logged.
- `audit_logs` stores a simple trace of ingestion, assignment, status changes and SLA flags.

---

## 11. Configuration reference

| Property | Default | Meaning |
|---|---|---|
| `server.port` | `8080` | HTTP port |
| `app.jwt.secret` | local secret | HMAC signing key (not committed; configure locally) |
| `app.jwt.expiration-ms` | `86400000` | token lifetime (24h) |
| `app.sla.scan-rate-ms` | `60000` | SLA scan interval |
| `app.cors.allowed-origins` | `localhost:3000,localhost:5173` | allowed frontend origins |

---

## 12. Known limitations

- Schema is managed by Hibernate `ddl-auto=update`; `database/schema.sql` is a reference script, not a
  Flyway migration. For production, add Flyway or Liquibase and switch to `ddl-auto=validate`.
- The SLA warning threshold (2 hours) and the 24-hour TAT are constants in `TicketService`/`SlaService`
  by assignment design; the scan interval is configurable.
- Notifications are in-app records only (no email/SMS integration, as required).
- No refresh tokens or token revocation; logout is client-side by discarding the JWT.
- The first SLA scan runs one interval (default 60s) after startup; `GET /api/manager/sla-alerts`
  always computes live results immediately.

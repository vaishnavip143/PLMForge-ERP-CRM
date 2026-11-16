# PLMForge ERP — CRM Module

A full-stack Customer Relationship Management module for the PLMForge ERP assignment
(**IN-LVL-01**). The project combines a Spring Boot REST API secured with JWT and role-based
access control with a React single-page dashboard for three business roles.

The application lets customers submit ERP queries, automatically classifies each query as an
existing customer or a potential customer (lead), assigns a 24-hour turnaround time (TAT), and
gives Sales Representatives and Sales Managers the tools to resolve tickets and monitor SLA risk.

> This README documents the implementation that exists in this repository. It does not describe
> features that are not present in the source code.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Key Features](#2-key-features)
3. [Technology Stack](#3-technology-stack)
4. [Project Folder Structure](#4-project-folder-structure)
5. [System Architecture](#5-system-architecture)
6. [Database Design](#6-database-design)
7. [Authentication and Authorization](#7-authentication-and-authorization)
8. [Roles and Their Responsibilities](#8-roles-and-their-responsibilities)
9. [Backend Setup Instructions](#9-backend-setup-instructions)
10. [PostgreSQL Database Setup](#10-postgresql-database-setup)
11. [Frontend Setup Instructions](#11-frontend-setup-instructions)
12. [API Documentation](#12-api-documentation)
13. [Application Screens and Dashboard Workflows](#13-application-screens-and-dashboard-workflows)
14. [Ticket Creation and Dynamic Ingestion Logic](#14-ticket-creation-and-dynamic-ingestion-logic)
15. [Potential Customer and Lead Management](#15-potential-customer-and-lead-management)
16. [SLA Monitoring and Notifications](#16-sla-monitoring-and-notifications)
17. [Testing Instructions](#17-testing-instructions)
18. [Troubleshooting](#18-troubleshooting)
19. [Future Improvements](#19-future-improvements)
20. [Assignment Acceptance Criteria](#20-assignment-acceptance-criteria)
21. [Repository Hygiene and Security Notes](#21-repository-hygiene-and-security-notes)

---

## 1. Project Overview

The CRM module covers the complete customer-query lifecycle:

```
Customer submits query
        │
        ▼
Backend identifies the sender
        │
        ├── Existing customer  → ticket linked to customer profile
        │
        └── Unknown email      → potential customer (lead) created/reused
                                 + ticket linked to the lead
                                 + Sales Manager notified
        │
        ▼
Ticket created with a 24-hour TAT and due date
        │
        ▼
Sales Representative updates status / resolution notes
        │
        ▼
Sales Manager monitors all tickets, leads,
notifications and SLA warnings/breaches
```

The repository contains two applications:

| Application | Folder | Purpose |
|---|---|---|
| Spring Boot backend | `crm-backend/` | REST API, security, persistence, SLA scheduler, seed data |
| React frontend | `crm-frontend/` | Role-aware dashboards and query submission UI |

---

## 2. Key Features

### Authentication and access control

- Public customer registration; the backend always assigns the `CUSTOMER` role.
- Login with email and password, returning a JWT access token.
- BCrypt password hashing.
- Stateless Spring Security filter chain with a JWT authentication filter.
- Route-level RBAC in `SecurityConfig` and ownership checks in the service layer.
- Consistent JSON `401` and `403` responses.
- **Portal-specific frontend login:** the selected portal (Customer, Sales Representative,
  Sales Manager) is compared with the role returned by the backend. A mismatch discards the new
  JWT, creates no session and shows
  `Role mismatch! Please select the correct login portal.`
- Saved frontend sessions are re-validated through `GET /api/auth/me` on page load and are bound
  to the portal role; invalid/tampered tokens log the user out.

### Customer features

- Submit a query with name, email, optional phone, subject and message.
- Automatic existing-customer detection by email or optional `customerId`.
- Automatic 24-hour TAT and due date.
- Immediate success/error feedback with ticket number, status, remaining TAT and due date.

### Sales Representative features

- View only the tickets assigned to the authenticated representative.
- See customer/lead details, subject, priority, status and remaining TAT.
- Update ticket status (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`) and resolution notes.
- Representatives cannot view or update another representative's tickets.

### Sales Manager features

- **Standard Tickets:** all tickets with customer, priority, status, remaining TAT and assignee.
- **Potential Customers:** every captured lead with contact details, source and status.
- **Alerts Panel:** manager notifications (new leads, SLA warnings/breaches, assignments) with
  mark-as-read, plus live SLA warning and overdue ticket tables.
- Manager APIs also expose customers, representatives, ticket assignment and ticket audit trails.

### Supporting features

- Automatic lead de-duplication by email.
- Duplicate-safe manager notifications per ticket/type/recipient.
- Scheduled SLA scan every 60 seconds (configurable) plus a live SLA alert endpoint.
- Audit trail for ingestion, assignment, status changes and SLA flags.
- Swagger / OpenAPI documentation with Bearer authentication.
- Development seed data created only when the database has no users.
- Responsive React UI built with plain CSS.

---

## 3. Technology Stack

### Backend

| Technology | Version / detail |
|---|---|
| Java | 17 |
| Spring Boot | 3.5.16 |
| Spring Web | REST controllers |
| Spring Security | Stateless JWT security, method security enabled |
| Spring Data JPA / Hibernate | PostgreSQL persistence, `ddl-auto=update` |
| PostgreSQL | Database `crm_db` on port 5432 |
| JJWT | 0.12.6 (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) |
| Bean Validation | Jakarta Validation annotations |
| Lombok | Boilerplate reduction |
| springdoc-openapi | 2.8.5 (Swagger UI) |
| Maven | Wrapper included (`mvnw`, `mvnw.cmd`) |

### Frontend

| Technology | Version / detail |
|---|---|
| React | 18.3.1 |
| Vite | 6.x |
| JavaScript | JSX, ES modules |
| Axios | 1.7.x |
| CSS | Single handwritten stylesheet |
| Node.js | 18+ required by Vite 6 |

No React Router, state-management library or UI framework is used. The UI is a single page whose
content is selected from the authenticated role.

---

## 4. Project Folder Structure

```
crm-backend/                         # repository root
├── README.md                        # this document
├── .gitignore                       # repository-wide ignores
├── crm-backend/                     # Spring Boot application
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── database/
│   │   └── schema.sql               # reference PostgreSQL schema
│   └── src/main/
│       ├── java/com/plmforge/crm/
│       │   ├── CrmApplication.java
│       │   ├── config/              # Security, OpenAPI, scheduling, seed data
│       │   ├── controller/          # Auth, Customer, Ticket, Sales, Manager, Notification
│       │   ├── dto/request/         # Validated request DTOs
│       │   ├── dto/response/        # Response DTOs and ApiResponse envelope
│       │   ├── entity/              # User, Customer, PotentialCustomer, Ticket, Notification, AuditLog
│       │   ├── enums/               # Role, status, priority, customer type, lead status, notification type
│       │   ├── exception/           # Global exception handling and domain exceptions
│       │   ├── repository/          # Spring Data JPA repositories
│       │   ├── security/            # JWT service/filter, UserPrincipal, 401/403 handlers
│       │   └── service/             # Business logic
│       └── resources/
│           ├── application.properties            # local config (git-ignored)
│           
└── crm-frontend/                    # React application
    ├── package.json
    ├── package-lock.json
    ├── vite.config.js               # dev server on port 5173
    ├── index.html
    ├── .env.example                 # optional VITE_API_URL
    └── src/
        ├── main.jsx
        ├── App.jsx                  # session, portal-role check, dashboard selection
        ├── api.js                   # Axios instance, JWT header, error messages
        ├── LoginScreen.jsx          # role portal selection, login, registration
        ├── CustomerDashboard.jsx
        ├── SalesDashboard.jsx
        ├── ManagerDashboard.jsx
        ├── components.jsx           # Layout, Message, TicketTable
        ├── helpers.js               # role/date/label helpers
        └── index.css
```

`node_modules/`, `dist/`, `target/`, IDE folders, local configuration and logs are excluded from
version control.

---

## 5. System Architecture

### Request flow

```
React (Vite dev server, port 5173)
        │  Axios + JSON
        │  Authorization: Bearer <JWT>
        ▼
Spring Boot REST API (port 8080)
        │
        ▼
Spring Security filter chain
  ├── JwtAuthenticationFilter
  │     validates signature/expiry, loads UserDetails, sets SecurityContext
  ├── Route authorization (SecurityConfig)
  │     /api/manager/**  → SALES_MANAGER
  │     /api/sales/**    → SALES_REPRESENTATIVE or SALES_MANAGER
  │     /api/customers/**→ CUSTOMER
  │     other requests   → authenticated
  └── JSON 401/403 handlers
        │
        ▼
Controller layer
        │  request DTO validation
        ▼
Service layer
        │  business rules, ownership checks, TAT/SLA, notifications, audit
        ▼
Spring Data JPA repositories
        │
        ▼
Hibernate
        │
        ▼
PostgreSQL (crm_db)
```

### Login and portal-role verification

1. The user selects a portal on the React login screen.
2. Axios sends `POST /api/auth/login` with the typed email and password.
3. Spring Security authenticates the pair with `DaoAuthenticationProvider` + BCrypt.
4. The backend returns a JWT whose role claim belongs to that account.
5. The React layer compares the response role with the selected portal:
   - match → store the session and render that role's dashboard;
   - mismatch → discard the JWT, clear session state and show the role-mismatch error.
6. Every later API call carries the JWT. Backend route rules and ownership checks remain the
   final authority; the frontend cannot grant a role.

### Why the dashboards differ

The React app uses the authenticated role from the backend response to choose exactly one
dashboard component. There is no router and no URL-based role switch. A Customer session can only
render the customer query form; a representative session can only render assigned tickets; a
manager session renders the three-section manager dashboard.

---

## 6. Database Design

Hibernate creates/updates the schema (`spring.jpa.hibernate.ddl-auto=update`). The
`crm-backend/database/schema.sql` file is a reference script; it is not executed automatically and
there is no migration tool.

### Tables

#### `users`

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGSERIAL` | Primary key |
| `full_name` | `VARCHAR(150)` | Mapped from `User.name` |
| `email` | `VARCHAR(150)` | Unique, used as login username |
| `password` | `VARCHAR(255)` | BCrypt hash, never returned by the API |
| `role` | `VARCHAR(50)` | `ROLE_CUSTOMER`, `ROLE_SALES_REPRESENTATIVE`, `ROLE_SALES_MANAGER` |
| `created_at` | `TIMESTAMP` | Set on insert |
| `updated_at` | `TIMESTAMP` | Set on insert/update |

#### `customers`

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGSERIAL` | Primary key |
| `name` | `VARCHAR(150)` | Customer name |
| `email` | `VARCHAR(150)` | Unique |
| `phone_number` | `VARCHAR(50)` | Optional |
| `company_name` | `VARCHAR(150)` | Optional |
| `created_at`, `updated_at` | `TIMESTAMP` | Audit timestamps |

#### `potential_customers`

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGSERIAL` | Primary key |
| `name` | `VARCHAR(150)` | Lead name |
| `email` | `VARCHAR(150)` | Not unique at DB level; application reuses by email |
| `phone_number` | `VARCHAR(50)` | Optional, filled from a later query if previously empty |
| `source` | `VARCHAR(100)` | Defaults to `INCOMING_QUERY` |
| `status` | `VARCHAR(50)` | Defaults to `NEW_LEAD` |
| `created_at`, `updated_at` | `TIMESTAMP` | Audit timestamps |

Lead status values: `NEW_LEAD`, `CONTACTED`, `QUALIFIED`, `CONVERTED`, `DISQUALIFIED`. The current
UI displays the status; there is no lead-status update API.

#### `query_tickets`

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGSERIAL` | Primary key |
| `ticket_number` | `VARCHAR(50)` | Unique; format `TICK-yyyyMMdd-NNNNNN` |
| `sender_email` | `VARCHAR(150)` | Normalised lower-case email |
| `sender_name` | `VARCHAR(150)` | Name supplied with the query |
| `subject` | `VARCHAR(255)` | Query subject |
| `description` | `TEXT` | Query message |
| `customer_type` | `VARCHAR(50)` | `EXISTING_CUSTOMER` or `POTENTIAL_CUSTOMER` |
| `customer_id` | `BIGINT` | FK → `customers.id`, nullable |
| `potential_customer_id` | `BIGINT` | FK → `potential_customers.id`, nullable |
| `status` | `VARCHAR(50)` | `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED` |
| `priority` | `VARCHAR(50)` | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` (new tickets default `MEDIUM`) |
| `tat_hours` | `INT` | 24 for new tickets |
| `sla_deadline` | `TIMESTAMP` | Exposed as `dueAt` in the API |
| `is_near_sla_breach` | `BOOLEAN` | SLA warning flag |
| `is_sla_breached` | `BOOLEAN` | Overdue flag |
| `assigned_to_user_id` | `BIGINT` | FK → `users.id`, nullable |
| `resolution_notes` | `TEXT` | Optional |
| `resolved_at` | `TIMESTAMP` | Set for resolved/closed tickets |
| `created_at`, `updated_at` | `TIMESTAMP` | Audit timestamps |

#### `notifications`

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGSERIAL` | Primary key |
| `recipient_id` | `BIGINT` | FK → `users.id`, required |
| `type` | `VARCHAR(50)` | `NEW_LEAD`, `SLA_WARNING`, `SLA_BREACH`, `TICKET_ASSIGNED` |
| `message` | `TEXT` | Human-readable message |
| `related_ticket_id` | `BIGINT` | FK → `query_tickets.id`, nullable |
| `is_read` | `BOOLEAN` | Default `false` |
| `created_at` | `TIMESTAMP` | Creation time |

#### `audit_logs`

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGSERIAL` | Primary key |
| `ticket_id` | `BIGINT` | Ticket reference (no JPA relation) |
| `ticket_number` | `VARCHAR(50)` | Denormalised for readability |
| `action` | `VARCHAR(100)` | e.g. `TICKET_STATUS_CHANGED` |
| `performed_by` | `VARCHAR(100)` | Email or `SYSTEM_SLA_MONITOR` |
| `details` | `TEXT` | Description |
| `timestamp` | `TIMESTAMP` | Creation time |

### Relationships

```
User (1) ──── (N) Ticket        assigned_to_user_id
User (1) ──── (N) Notification  recipient_id
Customer (1) ──── (N) Ticket    customer_id
PotentialCustomer (1) ──── (N) Ticket  potential_customer_id
Ticket (1) ──── (N) Notification related_ticket_id
Ticket (1) ──── (N) AuditLog    ticket_id (logical reference)
```

### Indexes

`database/schema.sql` defines indexes on customer/lead email, ticket sender email, ticket status,
SLA deadline, assignee, notification recipient/read state and audit ticket id.

### Seed data

There is **no `data.sql` and no migration script**. Seed data is created by
`config/DataInitializer.java` as a `CommandLineRunner`, and only when `users` is empty. Existing
data is never modified or duplicated.

On a fresh database it creates:

- 3 users: `customer@erp.com` (John Doe), `salesrep@erp.com` (Alex Turner),
  `manager@erp.com` (Sarah Connor).
- 3 existing customers: John Doe (`customer@erp.com`, Global Logistics Inc.),
  Alice Smith (`acme@acmecorp.com`, Acme Corporation), David Miller
  (`techcorp@techcorp.io`, TechCorp Solutions).
- 1 potential customer: Michael Green (`prospect@startup.co`, source `INCOMING_QUERY`,
  status `NEW_LEAD`).
- 4 tickets: three assigned to the representative (one `OPEN`, one `IN_PROGRESS`, one
  `RESOLVED`) and one `OPEN` ticket linked to the lead and left unassigned.
- 2 manager notifications: one `NEW_LEAD` and one `SLA_WARNING`.

All seeded users share the development password **`password123`** (the `DEV_PASSWORD` constant in
`DataInitializer`; it is stored as a BCrypt hash). That is a local development credential only -
change it before any shared or production deployment.

> Because the initializer checks `users`, a database that already contains users will not receive
> seed data again. See [Troubleshooting](#18-troubleshooting) if the demo accounts are missing.

---

## 7. Authentication and Authorization

### Registration

`POST /api/auth/register` is public.

```json
{
  "name": "Jane Customer",
  "email": "jane@example.com",
  "password": "your-password"
}
```

- Validation: name 2–100 characters, valid email, password 8–72 characters.
- The backend always creates `ROLE_CUSTOMER`; the request DTO has no role field.
- A matching `customers` profile is created so customer self-service works immediately.
- Duplicate email → `409 Conflict`.

### Login

`POST /api/auth/login` is public and accepts:

```json
{ "email": "user@example.com", "password": "your-password" }
```

A successful response contains:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "<jwt>",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "userId": 1,
    "name": "User Name",
    "email": "user@example.com",
    "role": "CUSTOMER"
  },
  "timestamp": "2026-01-01T12:00:00"
}
```

Invalid credentials return `401` with `Invalid email or password`.

### JWT

- Signed with HMAC using `app.jwt.secret`.
- Claims: `sub` (email), `userId`, `name`, `role` (display name without the `ROLE_` prefix).
- Lifetime: `app.jwt.expiration-ms`, default 24 hours (`86400000`).
- Sent as `Authorization: Bearer <token>`.
- No refresh token and no server-side revocation; logout is client-side.

### Spring Security rules

From `SecurityConfig`:

| Request | Required authority |
|---|---|
| `POST /api/auth/register`, `POST /api/auth/login` | Public |
| `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/error` | Public |
| `/api/manager/**` | `ROLE_SALES_MANAGER` |
| `/api/sales/**` | `ROLE_SALES_REPRESENTATIVE` or `ROLE_SALES_MANAGER` |
| `/api/customers/**` | `ROLE_CUSTOMER` |
| `GET /api/tickets/my` | `ROLE_CUSTOMER` |
| `POST /api/tickets/ingest` | Any of the three roles |
| Everything else (ticket details, notifications, `/auth/me`) | Authenticated |

Additional ownership rules are enforced in services:

- A customer can read only tickets linked to their own customer/lead email.
- A representative can read/update only tickets assigned to them.
- A manager can read every ticket and update any ticket status.
- A notification can be marked read only by its recipient.

Unauthenticated requests receive a JSON `401`; authenticated requests with the wrong role receive
a JSON `403` reading
`Access denied: your role is not allowed to perform this action.`

### Frontend session protection

- The JWT and user identity are kept in `localStorage` under `plmforge-crm-session`, together with
  the selected `portalRole`.
- On load, `GET /api/auth/me` validates the token and confirms the account role still matches the
  portal role. Failure logs the user out.
- A login whose backend role differs from the portal role is rejected before any session is
  stored, and the newly received token is discarded.
- Logout and role switching clear the session.
- Client-side checks are a usability guard only; backend authorization remains authoritative.

---

## 8. Roles and Their Responsibilities

| Role | Backend authority | Responsibilities | Frontend screen |
|---|---|---|---|
| Customer | `ROLE_CUSTOMER` | Register, sign in, submit queries, view success/TAT receipt | Customer Dashboard |
| Sales Representative | `ROLE_SALES_REPRESENTATIVE` | Work on assigned tickets, update status/resolution | Sales Representative Dashboard |
| Sales Manager | `ROLE_SALES_MANAGER` | Oversee all tickets, leads, notifications and SLA alerts; assign tickets through the API | Sales Manager Dashboard |

Public registration creates only Customers. Representative and manager accounts come from seed
data (or direct database provisioning); there is no public endpoint to create them.

### Portal-specific login

The login UI has three portals. A portal only succeeds for an account of the matching role:

| Portal | Accepted backend role |
|---|---|
| Customer | `CUSTOMER` |
| Sales Representative | `SALES_REPRESENTATIVE` |
| Sales Manager | `SALES_MANAGER` |

All other combinations are rejected on the client after the backend authenticates the credentials,
with no dashboard and no retained session.

---

## 9. Backend Setup Instructions

### Prerequisites

- Java 17 (`java -version`)
- PostgreSQL running locally
- No global Maven installation required; the Maven wrapper is included
- Windows: `mvnw.cmd`; macOS/Linux: `./mvnw`

### Configure

The committed repository contains a safe template, not the real local configuration:

```bash
cd crm-backend
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Windows Command Prompt:

```bat
copy src\main\resources\application.properties.example src\main\resources\application.properties
```

Edit the new local file and set:

- PostgreSQL URL, username and password
- a long random JWT secret (at least 32 bytes; use 64+ random characters)
- CORS origins if the frontend runs elsewhere

The local file is git-ignored.

### Run

```bash
# macOS / Linux
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Or package and run the jar:

```bash
./mvnw clean package
java -jar target/crm-backend-0.0.1-SNAPSHOT.jar
```

- API base URL: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

Swagger UI is explicitly public in `SecurityConfig`. Use the **Authorize** button and paste the JWT (without the `Bearer ` prefix) to
call protected endpoints.

### Configuration reference

| Property | Environment override | Default / meaning |
|---|---|---|
| `server.port` | `SERVER_PORT` | `8080` |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/crm_db` |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | `postgres` in the example |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | set locally, never committed |
| `app.jwt.secret` | `APP_JWT_SECRET` | HMAC signing key; must be long and random |
| `app.jwt.expiration-ms` | `APP_JWT_EXPIRATION_MS` | `86400000` (24 hours) |
| `app.sla.scan-rate-ms` | `APP_SLA_SCAN_RATE_MS` | `60000` (SLA scan interval) |
| `app.cors.allowed-origins` | `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` |

Environment variables use Spring Boot relaxed binding and override the local properties file.

---

## 10. PostgreSQL Database Setup

1. Start PostgreSQL and confirm it listens on port `5432`.
2. Create the database:

```bash
psql -U postgres -c "CREATE DATABASE crm_db;"
```

3. Configure the local `application.properties` (or environment variables) with the database URL,
   username and password.

4. Hibernate creates/updates tables on startup because `spring.jpa.hibernate.ddl-auto=update`.

Optionally create the schema explicitly with the reference script:

```bash
psql -U postgres -d crm_db -f crm-backend/database/schema.sql
```

The script uses `CREATE TABLE IF NOT EXISTS`, so it is safe to apply to an empty database and will
not delete data.

No database password or JWT secret is stored in this repository. Never commit the real
`application.properties` or `.env` files.

---

## 11. Frontend Setup Instructions

### Prerequisites

- Node.js 18 or newer
- npm

### Install and run

```bash
cd crm-frontend
npm install
npm run dev
```

- Frontend URL: <http://localhost:5173>
- Production build: `npm run build` (output in `dist/`)
- Local production preview: `npm run preview`

The dev server is fixed to port `5173` with `strictPort: true` in `vite.config.js`.

### Backend URL

`src/api.js` uses:

```js
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'
```

To point the UI at a different backend, create `crm-frontend/.env`:

```env
VITE_API_URL=http://localhost:8080
```

The default already works for local development. The backend must allow the frontend origin in
`app.cors.allowed-origins` (default includes `http://localhost:5173`).

### Build check

```bash
npm run build
```

---

## 12. API Documentation

All responses use the envelope:

```json
{
  "success": true,
  "message": "Human-readable message",
  "data": {},
  "timestamp": "2026-01-01T12:00:00"
}
```

Error responses use:

```json
{
  "success": false,
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/api/...",
  "timestamp": "2026-01-01T12:00:00",
  "validationErrors": { "field": "message" }
}
```

`validationErrors` is present only for bean-validation failures.

### Endpoint summary

| # | Method | Endpoint | Required role | Purpose |
|---|---|---|---|---|
| 1 | POST | `/api/auth/register` | Public | Register a customer account |
| 2 | POST | `/api/auth/login` | Public | Authenticate and receive a JWT |
| 3 | GET | `/api/auth/me` | Authenticated | Current user details |
| 4 | POST | `/api/tickets/ingest` | Customer / Rep / Manager | Submit a query and create a ticket |
| 5 | GET | `/api/tickets/my` | Customer | Own tickets |
| 6 | GET | `/api/tickets/{id}` | Authenticated, ownership rules | One ticket |
| 7 | GET | `/api/customers/my-profile` | Customer | Own customer profile |
| 8 | GET | `/api/customers/my-tickets` | Customer | Own tickets with TAT |
| 9 | GET | `/api/sales/tickets` | Rep / Manager | Assigned tickets (manager: all) |
| 10 | PUT | `/api/sales/tickets/{id}/status` | Rep (own) / Manager | Update status and notes |
| 11 | GET | `/api/manager/tickets` | Manager | All tickets |
| 12 | GET | `/api/manager/leads` | Manager | All potential customers |
| 13 | GET | `/api/manager/customers` | Manager | All existing customers |
| 14 | GET | `/api/manager/representatives` | Manager | Representatives available for assignment |
| 15 | POST | `/api/manager/tickets/{id}/assign` | Manager | Assign a ticket |
| 16 | GET | `/api/manager/sla-alerts` | Manager | Live SLA warnings and overdue tickets |
| 17 | GET | `/api/manager/notifications` | Manager | Manager notifications |
| 18 | GET | `/api/manager/tickets/{id}/audit` | Manager | Ticket audit trail |
| 19 | GET | `/api/notifications` | Authenticated | Own notifications |
| 20 | GET | `/api/notifications/unread` | Authenticated | Own unread notifications |
| 21 | PATCH | `/api/notifications/{id}/read` | Owner | Mark a notification as read |

### 12.1 Authentication

#### POST `/api/auth/register` — public

Request:

```json
{
  "name": "Jane Customer",
  "email": "jane@example.com",
  "password": "your-password"
}
```

Response `201 Created`:

```json
{
  "success": true,
  "message": "Customer registered successfully",
  "data": {
    "userId": 4,
    "name": "Jane Customer",
    "email": "jane@example.com",
    "role": "CUSTOMER",
    "createdAt": "2026-01-01T12:00:00"
  },
  "timestamp": "2026-01-01T12:00:00"
}
```

Errors: `400` validation, `409` duplicate email.

#### POST `/api/auth/login` — public

Request:

```json
{ "email": "user@example.com", "password": "your-password" }
```

Response `200 OK`: `AuthResponse` with `token`, `tokenType`, `expiresIn`, `userId`, `name`,
`email`, `role`. Invalid credentials: `401`.

#### GET `/api/auth/me` — authenticated

Response `200 OK`: `CurrentUserResponse` with `userId`, `name`, `email`, `role`,
`authenticated: true`. Invalid/expired token: `401`.

### 12.2 Tickets

#### POST `/api/tickets/ingest` — Customer, Representative or Manager

Request:

```json
{
  "name": "Alice Smith",
  "email": "acme@acmecorp.com",
  "subject": "Invoice discrepancy",
  "description": "Invoice 8819 has the wrong discount.",
  "phone": "+1-555-0102",
  "customerId": null
}
```

Validation: name 2–150, valid email, subject ≤ 255, description ≤ 5000, optional phone matching
7–20 phone characters, optional positive `customerId`.

Response `201 Created`:

```json
{
  "success": true,
  "message": "Query registered for existing customer. 24-hour TAT applied.",
  "data": {
    "ticket": {
      "id": 12,
      "ticketNumber": "TICK-20260101-123456",
      "subject": "Invoice discrepancy",
      "description": "Invoice 8819 has the wrong discount.",
      "customerType": "EXISTING_CUSTOMER",
      "status": "OPEN",
      "priority": "MEDIUM",
      "customerId": 2,
      "customerName": "Alice Smith",
      "customerEmail": "acme@acmecorp.com",
      "potentialCustomerId": null,
      "assignedToId": null,
      "tatHours": 24,
      "createdAt": "2026-01-01T12:00:00",
      "dueAt": "2026-01-02T12:00:00",
      "remainingMinutes": 1440,
      "remainingTime": "24h 0m remaining",
      "slaAlert": false,
      "overdue": false
    },
    "potentialCustomer": null,
    "notificationCreated": false,
    "notificationMessage": null,
    "message": "Query registered for existing customer. 24-hour TAT applied."
  },
  "timestamp": "2026-01-01T12:00:00"
}
```

For an unknown email the response includes the created/reused `potentialCustomer`,
`notificationCreated: true`, a `notificationMessage`, and an appropriate message.

#### GET `/api/tickets/my` — Customer

Returns `TicketResponse[]` for tickets linked to the customer's email and to any lead tickets with
the same email.

#### GET `/api/tickets/{id}` — authenticated

- Customer: only own ticket (customer or lead email match).
- Representative: only a ticket assigned to them.
- Manager: any ticket.

`200` with `TicketResponse`; `403` for ownership violations; `404` if missing.

#### GET `/api/customers/my-profile` — Customer

Returns `CustomerResponse` (`id`, `name`, `email`, `phone`, `companyName`, `createdAt`); `404` if
no profile exists for the account email.

#### GET `/api/customers/my-tickets` — Customer

Same result as `/api/tickets/my`, including TAT fields.

### 12.3 Sales

#### GET `/api/sales/tickets` — Representative or Manager

Returns `TicketResponse[]` ordered by creation date. A representative sees only tickets assigned
to their user id; a manager sees all tickets.

#### PUT `/api/sales/tickets/{id}/status` — Representative (own) or Manager

Request:

```json
{
  "status": "RESOLVED",
  "resolutionNotes": "Invoice corrected and resent."
}
```

- `status` is required and must be one of `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`.
- `resolutionNotes` is optional, maximum 2000 characters.
- Resolving/closing sets `resolvedAt`; reopening clears it.
- A representative may update only their assigned tickets.
- Response: `200` with the updated `TicketResponse`.

### 12.4 Manager

#### GET `/api/manager/tickets` — Manager

Returns all tickets, newest first.

#### GET `/api/manager/leads` — Manager

Returns `PotentialCustomerResponse[]` with `id`, `name`, `email`, `phone`, `source`, `status`,
`createdAt`.

#### GET `/api/manager/customers` — Manager

Returns `CustomerResponse[]` for all existing customers.

#### GET `/api/manager/representatives` — Manager

Returns `RepresentativeResponse[]` (`id`, `name`, `email`) used for assignment.

#### POST `/api/manager/tickets/{id}/assign` — Manager

Request:

```json
{ "representativeId": 1 }
```

- The user must exist and have `ROLE_SALES_REPRESENTATIVE`.
- An `OPEN` ticket becomes `IN_PROGRESS`.
- The representative receives a `TICKET_ASSIGNED` notification.
- Response: `200` with the updated `TicketResponse`.

#### GET `/api/manager/sla-alerts` — Manager

Response `200`:

```json
{
  "success": true,
  "message": "SLA alerts retrieved",
  "data": {
    "warningTickets": [],
    "overdueTickets": [],
    "warningCount": 0,
    "overdueCount": 0,
    "generatedAt": "2026-01-01T12:00:00"
  },
  "timestamp": "2026-01-01T12:00:00"
}
```

`warningTickets` contains active tickets with more than 0 and fewer than 120 minutes remaining.
`overdueTickets` contains active tickets whose due date has passed.

#### GET `/api/manager/notifications` — Manager

Returns the authenticated manager's `NotificationResponse[]`. Equivalent to
`GET /api/notifications` for a manager.

#### GET `/api/manager/tickets/{id}/audit` — Manager

Returns `AuditLogResponse[]` ordered newest first.

### 12.5 Notifications

#### GET `/api/notifications` — authenticated

Returns the authenticated user's notifications (`id`, `type`, `message`, `ticketId`,
`ticketNumber`, `read`, `createdAt`).

#### GET `/api/notifications/unread` — authenticated

Returns only unread notifications for the authenticated user.

#### PATCH `/api/notifications/{id}/read` — owner

Marks one notification read. A notification belonging to another user returns `404`.

### HTTP status summary

| Status | Meaning in this project |
|---|---|
| 200 | Successful read/update |
| 201 | Registration or query ingestion created a resource |
| 400 | Validation failure, malformed JSON, invalid enum or parameter |
| 401 | Missing/invalid JWT or invalid credentials |
| 403 | Authenticated but wrong role or ownership violation |
| 404 | Resource not found (including another user's notification) |
| 409 | Duplicate email or data-integrity conflict |
| 500 | Unexpected server error (generic message) |

---

## 13. Application Screens and Dashboard Workflows

The frontend is a single page with conditional rendering; there is no URL router. Vite's SPA
fallback returns `index.html` for any path, and the rendered dashboard depends only on the
authenticated role.

### Login screen

- Three role portals: Customer, Sales Representative, Sales Manager.
- Selecting a portal only highlights it and shows guidance; it never signs the user in and does
  not pre-fill credentials.
- Two tabs: **Sign in** and **Register as customer**.
- Customer guidance explains that customers must register first.
- Representative/manager guidance names the seeded account email, but never displays a password.
- On successful login the backend role is compared with the selected portal. A mismatch shows
  `Role mismatch! Please select the correct login portal.` and no dashboard opens.
- Registration is separate from login: after success the screen switches to Sign in with the new
  email pre-filled and the password empty.

### Customer Dashboard

- Header: authenticated customer's name, email and role.
- **Submit a query** form: full name, email, optional phone, subject, query message.
- On success: backend message plus a receipt showing ticket number, status, remaining TAT and due
  date; the subject/description/phone fields are cleared.
- On failure: the backend validation/auth error is displayed.
- No sales or manager data is rendered.

### Sales Representative Dashboard

- **Assigned tickets** table: ticket number, customer/lead, subject, priority, status, remaining
  TAT, actions.
- TAT highlighting for near-SLA and overdue tickets.
- **Update status** panel: status select (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`) and
  optional resolution notes; saves via `PUT /api/sales/tickets/{id}/status`.
- Only tickets returned by the representative endpoint are shown.

### Sales Manager Dashboard

Exactly three sections:

1. **Standard Tickets** — all tickets with assignee details.
2. **Potential Customers** — all leads with contact, source, status and capture time.
3. **Alerts Panel** — unread/read manager notifications with a mark-as-read action, plus SLA
   summaries and two tables: tickets with fewer than 2 hours remaining and overdue tickets.

### Sidebar and navigation

- Shows the signed-in user, email and role.
- Changing role signs the user out and reopens the login screen with that portal selected; it does
  not switch the active session.
- **Sign out** clears the stored session and token.

### Session behavior

- Session key: `plmforge-crm-session`.
- Stored fields: JWT, `portalRole`, and user `{ name, email, role }`.
- On reload, the token is validated through `/api/auth/me`; the account role must still match the
  portal role.
- Invalid/expired/tampered tokens or role-edited storage result in logout.

---

## 14. Ticket Creation and Dynamic Ingestion Logic

`POST /api/tickets/ingest` is the single ingestion entry point. The backend normalises the email
to lower case and trims the name/phone before classification.

### Existing customer

1. If `customerId` is supplied and exists, that customer is used.
2. Otherwise the email is looked up in `customers` (case-insensitive).
3. A ticket is created with:
   - `customerType = EXISTING_CUSTOMER`
   - the customer relationship
   - `status = OPEN`
   - `priority = MEDIUM`
   - `tatHours = 24`
   - `dueAt = now + 24 hours`
4. No lead and no manager lead notification are created.
5. An audit entry `QUERY_INGESTED_EXISTING_CUSTOMER` is written.
6. Response message: `Query registered for existing customer. 24-hour TAT applied.`

### Unknown email / potential customer

1. The email is not in `customers`.
2. The backend looks for an existing `potential_customers` row by email.
3. If none exists, a lead is created with `source = INCOMING_QUERY` and `status = NEW_LEAD`.
4. If a lead exists, it is reused; a missing phone number is filled from the new query.
5. A ticket is created with `customerType = POTENTIAL_CUSTOMER`, status `OPEN`, priority `MEDIUM`,
   TAT 24 hours and `dueAt = now + 24h`.
6. Every Sales Manager receives a `NEW_LEAD` notification (duplicate-safe per manager/ticket/type).
7. Audit entry `QUERY_INGESTED_POTENTIAL_CUSTOMER` is written.
8. Response includes the lead, `notificationCreated`, and the notification message.

### Ticket numbering

Tickets use `TICK-yyyyMMdd-NNNNNN`, where the numeric part is a random six-digit value checked for
uniqueness, with a timestamp-based fallback after repeated collisions.

### Ownership and visibility after lead capture

`GET /api/tickets/my` combines tickets linked to the customer profile **and** tickets linked to a
lead with the same email. This means a lead's earlier tickets become visible if the same email
later registers a customer account.

### Status lifecycle

`OPEN → IN_PROGRESS → RESOLVED / CLOSED`

These transitions are not strictly enforced by a state machine; the API accepts any of the four
statuses, and updates are restricted by role/ownership. Resolving or closing sets `resolvedAt`;
reopening clears it.

---

## 15. Potential Customer and Lead Management

- Leads are created only by query ingestion; there is no standalone lead-creation endpoint.
- Lead de-duplication is by case-insensitive email.
- A repeat query from an unknown email creates another ticket but reuses the lead and sends a new
  manager notification.
- The lead status starts at `NEW_LEAD`. The enum also supports `CONTACTED`, `QUALIFIED`,
  `CONVERTED`, `DISQUALIFIED`, but the current API/UI only display the value; there is no lead
  update endpoint.
- Managers can list all leads via `GET /api/manager/leads`.
- Customers/representatives cannot access the lead list; it is manager-only.
- Leads are not automatically converted to `customers` when the email registers; registration
  creates a separate customer profile, while lead tickets remain visible through the email match
  described above.

---

## 16. SLA Monitoring and Notifications

### TAT and SLA rules

- Every newly ingested ticket gets a 24-hour TAT (`TicketService.DEFAULT_TAT_HOURS`).
- SLA warning threshold: **120 minutes** (`TicketService.SLA_WARNING_THRESHOLD_MINUTES`).
- Active statuses: `OPEN` and `IN_PROGRESS`.
- A ticket with `0 < remainingMinutes < 120` is a warning.
- A ticket with `remainingMinutes <= 0` is overdue.
- `RESOLVED` and `CLOSED` tickets are never SLA-flagged and display `Closed` in the
  remaining-TAT column.

### Scheduled scan

`SlaService.scheduledSlaScan()` runs with:

```properties
app.sla.scan-rate-ms=60000
```

The first run is delayed by the same interval. For each active ticket it:

- sets `is_sla_breached` and `is_near_sla_breach` and creates one `SLA_BREACH` notification per
  manager when overdue;
- sets `is_near_sla_breach` and creates one `SLA_WARNING` notification per manager when under two
  hours remain;
- does not repeat a notification for the same ticket/type/recipient;
- writes audit entries `SLA_WARNING_FLAGGED` / `SLA_BREACH_FLAGGED`.

### Live manager view

`GET /api/manager/sla-alerts` computes warning and overdue lists immediately, independent of the
scheduler, and returns counts plus `generatedAt`. The manager UI shows:

- summary counts (`near breach`, `overdue`);
- a table of tickets with less than two hours remaining;
- a table of overdue tickets.

### Notifications

Types: `NEW_LEAD`, `SLA_WARNING`, `SLA_BREACH`, `TICKET_ASSIGNED`.

- Notifications are stored in `notifications` for a specific recipient.
- Managers receive lead and SLA notifications; a representative receives assignment notifications.
- `GET /api/notifications` and `/unread` are scoped to the authenticated user.
- `PATCH /api/notifications/{id}/read` marks one owned notification read.
- Notifications are in-app only; there is no email/SMS delivery.

---

## 17. Testing Instructions

### Automated tests

There are currently **no automated backend tests** (`src/test` is absent) and **no frontend test
suite**. The checks below are manual/compile/build validations.

### Backend compilation

```bash
cd crm-backend
./mvnw -q -DskipTests compile
```

Windows:

```bat
mvnw.cmd -q -DskipTests compile
```

### Frontend build

```bash
cd crm-frontend
npm install
npm run build
```

### Swagger smoke check

With the backend running:

- <http://localhost:8080/swagger-ui.html> (redirects to the Swagger UI page)
- <http://localhost:8080/v3/api-docs> (OpenAPI JSON)

### Manual authentication and RBAC checks

Use Swagger or curl:

1. `POST /api/auth/register` with a new email → `201`, role `CUSTOMER`.
2. Repeat the same registration → `409`.
3. Login as customer, representative and manager → each returns its own role.
4. Call `GET /api/manager/tickets` with a customer token → `403`.
5. Call any protected endpoint without a token → `401`.
6. Customer `GET /api/tickets/{id}` for another customer's ticket → `403`.
7. Representative updates a ticket not assigned to them → `403`.
8. Manager assigns an unassigned ticket to the representative → `200`, status `IN_PROGRESS`,
   assignment notification created.
9. Representative updates the assigned ticket → `200`, status changes and audit entry appears.
10. Manager `GET /api/manager/sla-alerts` → warning/overdue lists and counts.
11. `PATCH /api/notifications/{id}/read` for an owned notification → `200`; another user's
    notification → `404`.

### Frontend login-role matrix

The frontend rejects a portal/account mismatch. Test all nine combinations:

| Portal | Account role | Expected |
|---|---|---|
| Customer | Customer | Customer Dashboard |
| Customer | Representative | Role mismatch error, no session |
| Customer | Manager | Role mismatch error, no session |
| Sales Representative | Customer | Role mismatch error, no session |
| Sales Representative | Representative | Sales Representative Dashboard |
| Sales Representative | Manager | Role mismatch error, no session |
| Sales Manager | Customer | Role mismatch error, no session |
| Sales Manager | Representative | Role mismatch error, no session |
| Sales Manager | Manager | Sales Manager Dashboard |

After each mismatch, verify:

- the message `Role mismatch! Please select the correct login portal.` is visible;
- no dashboard renders;
- `localStorage.getItem('plmforge-crm-session')` is `null`.

### Session, URL and storage checks

- Sign in as a customer, refresh → Customer Dashboard remains.
- Sign in as a manager, refresh → Manager Dashboard remains.
- Open `/manager` without a session → login screen, no manager content.
- Open `/manager` as a customer → Customer Dashboard (path does not grant a role).
- Sign out → session storage removed.
- Edit the stored `portalRole`/`user.role` and reload → session rejected and cleared.
- Corrupt the stored token and reload → `/api/auth/me` returns `401` and the app logs out.

### Test data caution

The seed initializer runs only when the database has no users. Existing data, including records
created during manual testing, is preserved and not reset. Do not run destructive SQL against a
database you need to keep.

---

## 18. Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| Frontend page does not load / connection refused | Vite dev server stopped | Run `npm run dev` in `crm-frontend`; keep the terminal open |
| `Cannot reach the backend at http://localhost:8080` | Spring Boot not running or wrong port | Start the backend; confirm port 8080 is free |
| Login returns 401 | Wrong credentials or the account does not exist | Register a customer first; use a seeded account for sales roles |
| Login succeeds but no dashboard / role mismatch | Portal and account role differ | Select the portal matching the account role |
| Immediate manager dashboard on reload | A saved session is being restored | Click **Sign out**, then sign in through the intended portal |
| `403 Access denied` from the API | Authenticated with the wrong role or another user's resource | Use the correct role/account; check ownership rules |
| `401` after changing storage or restarting | Invalid/expired token | Sign out and log in again |
| CORS error in the browser | Frontend origin not allowed | Add it to `app.cors.allowed-origins` and restart the backend |
| Backend fails on startup with a JWT key error | Missing/short `app.jwt.secret` | Use a long random secret (32+ bytes) |
| Seed accounts missing | `users` table was not empty at startup | Use existing accounts or seed a fresh database |
| PostgreSQL connection refused | Database not running or wrong URL/credentials | Start PostgreSQL and verify the datasource properties |
| `mvnw` permission denied (Linux/macOS) | Wrapper not executable | `chmod +x mvnw` |
| Vite port already in use | Another dev server is running | Stop it; `strictPort` prevents automatic port changes |
| `npm install` fails behind a proxy | Registry/network restrictions | Configure the npm registry/proxy and retry |

---

## 19. Future Improvements

These are not implemented and would be reasonable next steps:

- Automated unit/integration tests for services and controllers, plus React component tests.
- Flyway or Liquibase migrations and `ddl-auto=validate` for reproducible schemas.
- Refresh tokens, token revocation and server-side logout.
- Environment-specific Spring profiles and stronger secret management.
- Pagination/filtering for large ticket and notification lists.
- Lead status update and lead-to-customer conversion workflows.
- Email/SMS or WebSocket delivery for notifications.
- Docker Compose for PostgreSQL + backend + frontend.
- CI pipeline running backend tests, frontend build and linting.
- Production deployment hardening (HTTPS, rate limiting, audit retention).

---

## 20. Assignment Acceptance Criteria

| Requirement | Implementation | Status |
|---|---|---|
| RBAC with three roles | `Role` enum, Spring Security route rules, `@EnableMethodSecurity`, frontend portal-role check | Implemented |
| Existing customer identification | Email or optional `customerId` lookup in `TicketService.ingest` | Implemented |
| Potential customer creation | New unknown email creates one `potential_customers` row; repeats reuse it | Implemented |
| Automatic 24-hour TAT | `DEFAULT_TAT_HOURS = 24`, `dueAt = createdAt + 24h` | Implemented |
| Sales Manager notifications | `NEW_LEAD` notification per manager on lead ingestion | Implemented |
| SLA warning under 2 hours | 120-minute threshold in `TicketService`/`SlaService`; scheduled scan + live endpoint | Implemented |
| Customer query submission | `POST /api/tickets/ingest` and Customer Dashboard form | Implemented |
| Sales Representative ticket management | Assigned-ticket list and status/resolution update endpoint and UI | Implemented |
| Sales Manager dashboard | Standard Tickets, Potential Customers, Alerts Panel | Implemented |
| Pre-populated users and mock customers | `DataInitializer` on an empty database | Implemented |
| Strict portal-specific login | Frontend compares selected portal with backend role, discards mismatches | Implemented (additional safeguard) |

### Implemented extras

- Ticket audit trail and audit endpoint.
- Ticket assignment endpoint with representative notification.
- Duplicate-safe manager notifications.
- Swagger/OpenAPI documentation with Bearer auth.
- Live SLA alert endpoint and `SLA_BREACH` notifications.
- Session re-validation through `/api/auth/me` and tamper detection.

### Not implemented / future scope

- Automated test suites.
- Database migrations.
- Email/SMS notifications.
- Token refresh/revocation.
- Lead status update or conversion APIs.
- Docker/CI configuration.

---

## 21. Repository Hygiene and Security Notes

- `crm-backend/src/main/resources/application.properties` is **not committed**; use
  `application.properties.example` and provide your own database credentials and JWT secret.
- `node_modules/`, `dist/`, `target/`, `.idea/`, `.vscode/`, `.freebuff/`, logs, `.env` files and
  local backend configuration are ignored by `.gitignore`.
- `crm-frontend/.env.example` documents the optional `VITE_API_URL` setting.
- No production password, JWT secret or database credential appears in this README. The only
  credential written down is the shared local seed password above.
- Development seed credentials are defined in `DataInitializer` for local use only. Change them
  before any shared or production deployment.
- The JWT secret in the local properties file must be a long random value; rotate it if it has
  ever been exposed.
- Swagger and the API are intended for local development; review CORS and HTTPS settings before
  deploying publicly.

### Files that must stay private

- `crm-backend/src/main/resources/application.properties`
- `crm-frontend/.env`
- Any PostgreSQL dumps or log files containing user data

### Repository status

The project is prepared for a fresh Git repository at the `crm-backend/` root (the folder that
contains both the backend and frontend). Initialize and commit with:

```bash
git init
git add .
git status        # confirm ignored files are absent
git commit -m "PLMForge ERP CRM module"
```

Do not commit `target/`, `node_modules/`, `dist/`, IDE metadata, local configuration or logs.

---

## Documentation Verification

The endpoint list, roles, request fields, response fields, table names, seed records and setup
commands in this README were checked against:

- `crm-backend/src/main/java/com/plmforge/crm/controller/*.java`
- `crm-backend/src/main/java/com/plmforge/crm/config/SecurityConfig.java`
- `crm-backend/src/main/java/com/plmforge/crm/service/*.java`
- `crm-backend/src/main/java/com/plmforge/crm/entity/*.java`
- `crm-backend/src/main/java/com/plmforge/crm/config/DataInitializer.java`
- `crm-backend/database/schema.sql`
- `crm-frontend/src/*`
- `pom.xml`, `package.json`, `vite.config.js` and the local application configuration

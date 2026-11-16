PLMForge CRM – Simple Project README
Overview
PLMForge CRM is a full-stack Customer Relationship Management module. It lets customers submit queries, classifies them as existing customers or new leads, assigns a 24-hour response time (TAT), and lets Sales Representatives and Sales Managers track and resolve tickets.

Backend: Spring Boot 3.x, Java 17, Spring Security + JWT, Spring Data JPA, PostgreSQL
Frontend: React 18, Vite, Axios, plain CSS
Roles: Customer, Sales Representative, Sales Manager

Project Architecture (Simple)
The app works in three layers:

Browser (React, port 5173)
        │  Axios calls + JSON
        ▼
Spring Boot Backend (port 8091)
        │  Spring Security + JWT
        ▼
PostgreSQL Database (crm_db)
How the pieces connect
Frontend (React + Vite)

Single-page app with no router.
Shows public landing page, login/register, and three role dashboards based on the user's JWT role.
Sends API calls with Axios; attaches the JWT token only for protected endpoints.
Backend (Spring Boot)

REST controllers receive requests.
Spring Security checks the JWT and role for every protected request.
Service layer contains business rules: ticket creation, customer vs lead classification, SLA checks, ticket assignment, status updates.
Spring Data JPA saves/reads data from PostgreSQL.
Database (PostgreSQL)

Stores users, customers, potential customers (leads), tickets, notifications, and audit logs.
Tables are created/updated automatically on startup (ddl-auto=update).
Security flow
Public pages (register, login, submit query) need no token.
Protected pages require a valid JWT.
Backend checks roles: Customer, Sales Representative, or Sales Manager.
A customer can only see their own tickets; a sales rep only their assigned tickets; a manager sees everything.
Seeded Login Credentials
Created automatically on a fresh database (change before production):

Role	Email	Password
Customer	customer@erp.com	password123
Sales Representative	salesrep@erp.com	password123
Sales Manager	manager@erp.com	password123
Customers can also register their own account at POST /api/auth/register.

How It Works (Simple Flow)
Public visitor opens the website → sees Submit Your Query form (no login needed).
Visitor enters Name, Email, Phone, Subject, Description and submits.
Backend checks the email:
If it matches a registered customer → ticket links to that customer.
If new → creates/reuses a potential customer (lead) and notifies the Sales Manager.
Ticket is created with 24-hour TAT.
Visitor sees a professional success message with the ticket number and status.
Customer logs in → submits queries and views their own tickets/status/resolution messages.
Sales Representative logs in → views assigned tickets, updates status, adds a resolution message (required when resolving).
Sales Manager logs in → views all tickets, leads, notifications, SLA alerts, and assigns tickets to representatives.
Setup & Run
1. Database
Install PostgreSQL and create database crm_db.
Update crm-backend/src/main/resources/application.properties with your PostgreSQL username/password.
Backend runs on port 8091.
2. Backend
cd crm-backend\crm-backend
.\mvnw.cmd spring-boot:run
Swagger UI: http://localhost:8091/swagger-ui.html

3. Frontend
cd crm-frontend
npm install
npm run dev
Frontend: http://localhost:5173

Create crm-frontend\.env:

VITE_API_URL=http://localhost:8091
Important Notes
Backend port is 8091 (not 8080). Make sure frontend API URL points to http://localhost:8091.
No email is sent automatically when a ticket is resolved; the resolution message is shown in the Customer Dashboard only.
All three roles are preserved. Customers cannot see other customers' tickets.

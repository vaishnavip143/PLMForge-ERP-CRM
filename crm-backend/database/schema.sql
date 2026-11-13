-- ============================================================
-- PLMForge ERP CRM module - PostgreSQL reference schema
-- ============================================================
-- The application creates/updates these tables automatically through
-- Hibernate (spring.jpa.hibernate.ddl-auto=update). This file documents
-- the resulting schema and can be used to create the database manually.
--
--   createdb -U postgres crm_db
--   psql -U postgres -d crm_db -f database/schema.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    full_name   VARCHAR(150) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS customers (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    phone_number  VARCHAR(50),
    company_name  VARCHAR(150),
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_customers_email ON customers(email);

CREATE TABLE IF NOT EXISTS potential_customers (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    phone_number VARCHAR(50),
    source      VARCHAR(100),
    status      VARCHAR(50) NOT NULL DEFAULT 'NEW_LEAD',
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_potential_customers_email ON potential_customers(email);

CREATE TABLE IF NOT EXISTS query_tickets (
    id                     BIGSERIAL PRIMARY KEY,
    ticket_number          VARCHAR(50) NOT NULL UNIQUE,
    sender_email           VARCHAR(150) NOT NULL,
    sender_name            VARCHAR(150),
    subject                VARCHAR(255) NOT NULL,
    description            TEXT NOT NULL,
    customer_type          VARCHAR(50) NOT NULL,
    customer_id            BIGINT REFERENCES customers(id),
    potential_customer_id  BIGINT REFERENCES potential_customers(id),
    status                 VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    priority               VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    tat_hours              INT NOT NULL DEFAULT 24,
    sla_deadline           TIMESTAMP NOT NULL,
    is_near_sla_breach     BOOLEAN NOT NULL DEFAULT FALSE,
    is_sla_breached        BOOLEAN NOT NULL DEFAULT FALSE,
    assigned_to_user_id    BIGINT REFERENCES users(id),
    resolution_notes       TEXT,
    resolved_at            TIMESTAMP,
    created_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_tickets_sender_email ON query_tickets(sender_email);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON query_tickets(status);
CREATE INDEX IF NOT EXISTS idx_tickets_deadline ON query_tickets(sla_deadline);
CREATE INDEX IF NOT EXISTS idx_tickets_assigned ON query_tickets(assigned_to_user_id);

CREATE TABLE IF NOT EXISTS notifications (
    id                 BIGSERIAL PRIMARY KEY,
    recipient_id       BIGINT NOT NULL REFERENCES users(id),
    type               VARCHAR(50) NOT NULL,
    message            TEXT NOT NULL,
    related_ticket_id  BIGINT REFERENCES query_tickets(id),
    is_read            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications(recipient_id);
CREATE INDEX IF NOT EXISTS idx_notifications_read ON notifications(is_read);

CREATE TABLE IF NOT EXISTS audit_logs (
    id             BIGSERIAL PRIMARY KEY,
    ticket_id      BIGINT,
    ticket_number  VARCHAR(50),
    action         VARCHAR(100) NOT NULL,
    performed_by   VARCHAR(100) NOT NULL,
    details        TEXT NOT NULL,
    timestamp      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_audit_ticket_id ON audit_logs(ticket_id);

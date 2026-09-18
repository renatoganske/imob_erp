CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    plan VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    clerk_user_id VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    commission_rate NUMERIC(5, 2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_users_tenant_id ON users (tenant_id);

CREATE TABLE properties (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    type VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    address VARCHAR(255) NOT NULL,
    neighborhood VARCHAR(255),
    city VARCHAR(255) NOT NULL,
    price NUMERIC(14, 2) NOT NULL,
    area NUMERIC(10, 2),
    bedrooms INTEGER,
    bathrooms INTEGER,
    parking_spots INTEGER,
    status VARCHAR(20) NOT NULL,
    purpose VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_properties_tenant_id ON properties (tenant_id);
CREATE INDEX idx_properties_tenant_status ON properties (tenant_id, status);

CREATE TABLE property_photos (
    property_id UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    photos VARCHAR(1024)
);
CREATE INDEX idx_property_photos_property_id ON property_photos (property_id);

CREATE TABLE leads (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    assigned_to UUID NOT NULL REFERENCES users (id),
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(255),
    source VARCHAR(30) NOT NULL,
    stage VARCHAR(30) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_leads_tenant_id ON leads (tenant_id);
CREATE INDEX idx_leads_tenant_stage ON leads (tenant_id, stage);
CREATE INDEX idx_leads_assigned_to ON leads (assigned_to);

CREATE TABLE lead_property (
    lead_id UUID NOT NULL REFERENCES leads (id) ON DELETE CASCADE,
    property_id UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    PRIMARY KEY (lead_id, property_id)
);

CREATE TABLE visits (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    lead_id UUID NOT NULL REFERENCES leads (id),
    property_id UUID NOT NULL REFERENCES properties (id),
    agent_id UUID NOT NULL REFERENCES users (id),
    scheduled_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    result TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_visits_tenant_id ON visits (tenant_id);

CREATE TABLE contracts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    lead_id UUID REFERENCES leads (id),
    property_id UUID NOT NULL REFERENCES properties (id),
    agent_id UUID NOT NULL REFERENCES users (id),
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    value NUMERIC(14, 2) NOT NULL,
    signed_at DATE,
    start_date DATE NOT NULL,
    end_date DATE,
    adjustment_index VARCHAR(10),
    buyer_name VARCHAR(255) NOT NULL,
    buyer_document VARCHAR(30) NOT NULL,
    owner_name VARCHAR(255) NOT NULL,
    owner_document VARCHAR(30) NOT NULL,
    document_url VARCHAR(1024),
    commission_rate_override NUMERIC(5, 2),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_contracts_tenant_id ON contracts (tenant_id);
CREATE INDEX idx_contracts_tenant_status ON contracts (tenant_id, status);

CREATE TABLE financial_entries (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    contract_id UUID REFERENCES contracts (id),
    type VARCHAR(10) NOT NULL,
    category VARCHAR(30) NOT NULL,
    description VARCHAR(255) NOT NULL,
    value NUMERIC(14, 2) NOT NULL,
    due_date DATE NOT NULL,
    paid_at DATE,
    status VARCHAR(20) NOT NULL,
    recurrent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_financial_entries_tenant_id ON financial_entries (tenant_id);
CREATE INDEX idx_financial_entries_tenant_status ON financial_entries (tenant_id, status);
CREATE INDEX idx_financial_entries_due_date ON financial_entries (due_date);

CREATE TABLE commissions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    contract_id UUID NOT NULL REFERENCES contracts (id),
    agent_id UUID NOT NULL REFERENCES users (id),
    rate NUMERIC(5, 2) NOT NULL,
    base_value NUMERIC(14, 2) NOT NULL,
    value NUMERIC(14, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    paid_at DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_commissions_tenant_id ON commissions (tenant_id);
CREATE INDEX idx_commissions_agent_id ON commissions (agent_id);

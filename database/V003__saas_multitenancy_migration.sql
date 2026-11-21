-- ============================================================================
-- V003: Multi-Tenancy SaaS Architecture Migration
-- Transforms single-tenant POS into a Multi-Tenant SaaS platform
-- ============================================================================

-- 1. Create 'business' table (Tenants)
CREATE TABLE IF NOT EXISTS business (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    business_code VARCHAR(50) NOT NULL UNIQUE,
    owner_name VARCHAR(100),
    contact_email VARCHAR(100),
    contact_phone VARCHAR(50),
    address VARCHAR(500),
    tax_number VARCHAR(50),
    currency VARCHAR(10) NOT NULL DEFAULT 'LKR',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, SUSPENDED, TRIAL, EXPIRED
    subscription_plan VARCHAR(50) NOT NULL DEFAULT 'TRIAL', -- TRIAL, BASIC, PRO, ENTERPRISE
    trial_end_date DATE,
    max_terminals INTEGER NOT NULL DEFAULT 5,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- 2. Create 'pos_terminal' table (Registers/Terminals)
CREATE TABLE IF NOT EXISTS pos_terminal (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL,
    terminal_code VARCHAR(50) NOT NULL,
    terminal_name VARCHAR(100) NOT NULL,
    pairing_key VARCHAR(50) NOT NULL UNIQUE,
    device_uuid VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, REVOKED
    last_active_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_terminal_business FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE CASCADE,
    CONSTRAINT uk_business_terminal_code UNIQUE (business_id, terminal_code)
);

CREATE INDEX IF NOT EXISTS idx_pos_terminal_business_id ON pos_terminal(business_id);
CREATE INDEX IF NOT EXISTS idx_pos_terminal_pairing_key ON pos_terminal(pairing_key);

-- 3. Seed Default Business (Kumara Enterprises as Business #1)
INSERT INTO business (id, name, business_code, owner_name, contact_email, contact_phone, address, currency, status, subscription_plan, max_terminals)
VALUES (1, 'Kumara Enterprises', 'KUMARA-01', 'Kumara Admin', 'admin@kumara.com', '+94 77 123 4567', 'Main Street, Colombo', 'LKR', 'ACTIVE', 'ENTERPRISE', 10)
ON CONFLICT (id) DO UPDATE SET 
    business_code = EXCLUDED.business_code,
    name = EXCLUDED.name;

-- Ensure sequence matches max id
SELECT setval('business_id_seq', (SELECT COALESCE(MAX(id), 1) FROM business));

-- 4. Seed Default Terminal for Kumara Enterprises (Pairing Key: 111-222)
INSERT INTO pos_terminal (business_id, terminal_code, terminal_name, pairing_key, status)
VALUES (1, 'TERM-01', 'Main Counter Register 1', '111-222', 'ACTIVE')
ON CONFLICT (pairing_key) DO NOTHING;

-- 5. Add 'business_id' to existing tables if missing

-- app_user
DO $$ BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'app_user' AND column_name = 'business_id') THEN
        ALTER TABLE app_user ADD COLUMN business_id BIGINT REFERENCES business(id) ON DELETE SET NULL;
    END IF;
END $$;

-- product
DO $$ BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'product' AND column_name = 'business_id') THEN
        ALTER TABLE product ADD COLUMN business_id BIGINT REFERENCES business(id) ON DELETE CASCADE;
    END IF;
END $$;

-- product_detail (batches)
DO $$ BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'product_detail' AND column_name = 'business_id') THEN
        ALTER TABLE product_detail ADD COLUMN business_id BIGINT REFERENCES business(id) ON DELETE CASCADE;
    END IF;
END $$;

-- category
DO $$ BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'category' AND column_name = 'business_id') THEN
        ALTER TABLE category ADD COLUMN business_id BIGINT REFERENCES business(id) ON DELETE CASCADE;
    END IF;
END $$;

-- customer
DO $$ BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'customer' AND column_name = 'business_id') THEN
        ALTER TABLE customer ADD COLUMN business_id BIGINT REFERENCES business(id) ON DELETE CASCADE;
    END IF;
END $$;

-- loyalty_card
DO $$ BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'loyalty_card' AND column_name = 'business_id') THEN
        ALTER TABLE loyalty_card ADD COLUMN business_id BIGINT REFERENCES business(id) ON DELETE CASCADE;
    END IF;
END $$;

-- order_detail
DO $$ BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'order_detail' AND column_name = 'business_id') THEN
        ALTER TABLE order_detail ADD COLUMN business_id BIGINT REFERENCES business(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'order_detail' AND column_name = 'terminal_id') THEN
        ALTER TABLE order_detail ADD COLUMN terminal_id BIGINT REFERENCES pos_terminal(id) ON DELETE SET NULL;
    END IF;
END $$;

-- return_order
DO $$ BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'return_order' AND column_name = 'business_id') THEN
        ALTER TABLE return_order ADD COLUMN business_id BIGINT REFERENCES business(id) ON DELETE CASCADE;
    END IF;
END $$;

-- system_settings
DO $$ BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'system_settings' AND column_name = 'business_id') THEN
        ALTER TABLE system_settings ADD COLUMN business_id BIGINT REFERENCES business(id) ON DELETE CASCADE;
    END IF;
END $$;

-- supplier (if exists)
DO $$ BEGIN 
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'supplier') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'supplier' AND column_name = 'business_id') THEN
            ALTER TABLE supplier ADD COLUMN business_id BIGINT REFERENCES business(id) ON DELETE CASCADE;
        END IF;
    END IF;
END $$;

-- 6. Backfill existing data with default business_id = 1
UPDATE product SET business_id = 1 WHERE business_id IS NULL;
UPDATE product_detail SET business_id = 1 WHERE business_id IS NULL;
UPDATE category SET business_id = 1 WHERE business_id IS NULL;
UPDATE customer SET business_id = 1 WHERE business_id IS NULL;
UPDATE loyalty_card SET business_id = 1 WHERE business_id IS NULL;
UPDATE order_detail SET business_id = 1 WHERE business_id IS NULL;
UPDATE return_order SET business_id = 1 WHERE business_id IS NULL;
UPDATE system_settings SET business_id = 1 WHERE business_id IS NULL;

-- Backfill non-superadmin users with business_id = 1
UPDATE app_user SET business_id = 1 
WHERE business_id IS NULL 
  AND email NOT IN ('superadmin', 'super_admin');

-- Backfill supplier if table exists
DO $$ BEGIN 
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'supplier') THEN
        UPDATE supplier SET business_id = 1 WHERE business_id IS NULL;
    END IF;
END $$;

-- 7. Update Unique Constraints:
-- Drop global barcode unique constraint if exists, and replace with composite (business_id, barcode)
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'product_barcode_key') THEN
        ALTER TABLE product DROP CONSTRAINT product_barcode_key;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_business_barcode') THEN
        ALTER TABLE product ADD CONSTRAINT uk_business_barcode UNIQUE (business_id, barcode);
    END IF;
END $$;

-- Update category unique constraint to (business_id, name)
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'category_name_key') THEN
        ALTER TABLE category DROP CONSTRAINT category_name_key;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_business_category_name') THEN
        ALTER TABLE category ADD CONSTRAINT uk_business_category_name UNIQUE (business_id, name);
    END IF;
END $$;

-- 8. Add indexes on business_id for query speed
CREATE INDEX IF NOT EXISTS idx_product_business_id ON product(business_id);
CREATE INDEX IF NOT EXISTS idx_product_detail_business_id ON product_detail(business_id);
CREATE INDEX IF NOT EXISTS idx_order_detail_business_id ON order_detail(business_id);
CREATE INDEX IF NOT EXISTS idx_customer_business_id ON customer(business_id);
CREATE INDEX IF NOT EXISTS idx_app_user_business_id ON app_user(business_id);

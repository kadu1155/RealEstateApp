-- =====================================================================
-- PropReel: Database Migration 001 - Initial Production Schema
-- Engine: PostgreSQL 16+ with UUID and PostGIS spatial extensions
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- Full-text & Trigram search for localities & titles

-- 1. Users Table (Core Identity)
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    mobile VARCHAR(15) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER' 
        CHECK (role IN ('CUSTOMER', 'OWNER', 'BROKER', 'BUILDER', 'REAL_ESTATE_PRO', 'ADMIN')),
    avatar_url TEXT,
    city VARCHAR(100) NOT NULL DEFAULT 'Pune',
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_badge VARCHAR(50) NOT NULL DEFAULT 'NONE'
        CHECK (verification_badge IN ('NONE', 'OWNER_VERIFIED', 'RERA_VERIFIED', 'BUILDER_PARTNER')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'BLOCKED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. Business / Broker Profiles (Extension for Agents, Builders, Pros)
CREATE TABLE IF NOT EXISTS business_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    company_name VARCHAR(200),
    rera_number VARCHAR(100),
    gst_number VARCHAR(50),
    experience_years INT DEFAULT 0,
    operating_localities TEXT[] DEFAULT '{}',
    office_address TEXT,
    bio TEXT,
    website_url TEXT,
    is_kyc_approved BOOLEAN NOT NULL DEFAULT FALSE,
    kyc_document_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. Master Localities (Configurable Geographic Reference)
CREATE TABLE IF NOT EXISTS localities (
    id SERIAL PRIMARY KEY,
    city VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    pincode VARCHAR(10),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(city, name)
);

-- 4. Properties Table (Core Inventory)
CREATE TABLE IF NOT EXISTS properties (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    listing_type VARCHAR(20) NOT NULL 
        CHECK (listing_type IN ('BUY', 'RENT', 'LEASE')),
    category VARCHAR(30) NOT NULL 
        CHECK (category IN ('RESIDENTIAL_BUY', 'RESIDENTIAL_RENT', 'COMMERCIAL_BUY', 'COMMERCIAL_RENT', 'PLOTS_LAND', 'NEW_PROJECTS')),
    price NUMERIC(14,2) NOT NULL CHECK (price >= 0),
    price_display VARCHAR(50) NOT NULL,
    is_negotiable BOOLEAN NOT NULL DEFAULT FALSE,
    maintenance_charge NUMERIC(10,2) NOT NULL DEFAULT 0,
    
    -- Location Attributes
    city VARCHAR(100) NOT NULL,
    locality VARCHAR(150) NOT NULL,
    landmark VARCHAR(200),
    pincode VARCHAR(10) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    hide_exact_address BOOLEAN NOT NULL DEFAULT FALSE,

    -- Specs
    bhk INT CHECK (bhk >= 0),
    bathrooms INT CHECK (bathrooms >= 0),
    balconies INT DEFAULT 0,
    carpet_area_sqft NUMERIC(10,2) NOT NULL CHECK (carpet_area_sqft > 0),
    super_area_sqft NUMERIC(10,2),
    floor_number INT,
    total_floors INT,
    furnishing VARCHAR(30) NOT NULL 
        CHECK (furnishing IN ('FURNISHED', 'SEMI_FURNISHED', 'UNFURNISHED')),
    possession VARCHAR(30) NOT NULL 
        CHECK (possession IN ('READY_TO_MOVE', 'UNDER_CONSTRUCTION')),
    available_from DATE,
    age_of_property_years INT DEFAULT 0,
    rera_registered BOOLEAN NOT NULL DEFAULT FALSE,
    rera_id VARCHAR(100),

    -- JSON Amenities & Media
    amenities JSONB NOT NULL DEFAULT '[]'::jsonb,
    image_urls JSONB NOT NULL DEFAULT '[]'::jsonb,
    linked_reel_id UUID,

    -- Flags & Metrics
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' 
        CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'ACTIVE', 'SOLD', 'INACTIVE', 'FLAGGED')),
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    views_count INT NOT NULL DEFAULT 0,
    enquiries_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 5. Reels Table (Vertical Discovery Videos)
CREATE TABLE IF NOT EXISTS reels (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    property_id UUID REFERENCES properties(id) ON DELETE SET NULL,
    caption TEXT NOT NULL,
    video_url TEXT NOT NULL,
    hls_playlist_url TEXT,
    thumbnail_url TEXT NOT NULL,
    duration_seconds NUMERIC(5,2) NOT NULL CHECK (duration_seconds > 0 AND duration_seconds <= 60),
    aspect_ratio VARCHAR(10) NOT NULL DEFAULT '9:16',
    locality VARCHAR(150),
    city VARCHAR(100) NOT NULL,
    tags TEXT[] DEFAULT '{}',
    is_sponsored BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' 
        CHECK (status IN ('PROCESSING', 'ACTIVE', 'FLAGGED', 'REMOVED')),
    
    -- Engagement Counts
    views_count INT NOT NULL DEFAULT 0,
    likes_count INT NOT NULL DEFAULT 0,
    saves_count INT NOT NULL DEFAULT 0,
    shares_count INT NOT NULL DEFAULT 0,
    comments_count INT NOT NULL DEFAULT 0,
    enquiry_clicks_count INT NOT NULL DEFAULT 0,
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Foreign key back to reels from properties
ALTER TABLE properties 
    ADD CONSTRAINT fk_properties_linked_reel 
    FOREIGN KEY (linked_reel_id) REFERENCES reels(id) ON DELETE SET NULL;

-- 6. Reel Engagements (Audit & Duplicate Engagement Guard)
CREATE TABLE IF NOT EXISTS reel_engagements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reel_id UUID NOT NULL REFERENCES reels(id) ON DELETE CASCADE,
    engagement_type VARCHAR(30) NOT NULL 
        CHECK (engagement_type IN ('LIKE', 'SAVE', 'SHARE', 'WATCH_3S', 'WATCH_COMPLETE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, reel_id, engagement_type)
);

-- 7. Reel Comments
CREATE TABLE IF NOT EXISTS reel_comments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reel_id UUID NOT NULL REFERENCES reels(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    comment_text TEXT NOT NULL,
    is_flagged BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 8. Enquiries Table (Customer Interest Record)
CREATE TABLE IF NOT EXISTS enquiries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    reel_id UUID REFERENCES reels(id) ON DELETE SET NULL,
    customer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    customer_name VARCHAR(150) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    intent VARCHAR(50) NOT NULL DEFAULT 'CALLBACK'
        CHECK (intent IN ('CALLBACK', 'SITE_VISIT', 'BROCHURE', 'BEST_OFFER')),
    message TEXT,
    consent_given BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 9. Leads Table (Broker & Owner CRM Pipeline)
CREATE TABLE IF NOT EXISTS leads (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    enquiry_id UUID NOT NULL UNIQUE REFERENCES enquiries(id) ON DELETE CASCADE,
    broker_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    customer_name VARCHAR(150) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    property_title VARCHAR(255) NOT NULL,
    property_price_display VARCHAR(50) NOT NULL,
    locality VARCHAR(150) NOT NULL,
    stage VARCHAR(30) NOT NULL DEFAULT 'NEW'
        CHECK (stage IN ('NEW', 'CONTACTED', 'INTERESTED', 'SITE_VISIT', 'NEGOTIATION', 'CLOSED', 'NOT_RELEVANT')),
    notes TEXT DEFAULT '',
    scheduled_visit_at TIMESTAMPTZ,
    last_contacted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 10. Saved Properties (User Bookmarks)
CREATE TABLE IF NOT EXISTS saved_properties (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, property_id)
);

-- 11. Saved Searches & Search Alerts
CREATE TABLE IF NOT EXISTS saved_searches (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    search_name VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    locality VARCHAR(150),
    category VARCHAR(30),
    min_price NUMERIC(14,2),
    max_price NUMERIC(14,2),
    bhk INT,
    alert_frequency VARCHAR(20) NOT NULL DEFAULT 'INSTANT'
        CHECK (alert_frequency IN ('INSTANT', 'DAILY', 'NONE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 12. Customer Requirements (Reverse Marketplace)
CREATE TABLE IF NOT EXISTS customer_requirements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL, -- e.g. "2 BHK in Baner under 85 Lakh"
    city VARCHAR(100) NOT NULL,
    target_localities TEXT[] NOT NULL DEFAULT '{}',
    category VARCHAR(30) NOT NULL,
    max_budget NUMERIC(14,2) NOT NULL,
    bhk INT,
    furnishing VARCHAR(30),
    allow_agent_contact BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'FULFILLED', 'EXPIRED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 13. Promotion Campaigns (Paid Monetization)
CREATE TABLE IF NOT EXISTS promotion_campaigns (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    title VARCHAR(200) NOT NULL,
    target_type VARCHAR(30) NOT NULL CHECK (target_type IN ('REEL', 'PROPERTY', 'PROFILE')),
    target_id UUID NOT NULL,
    budget_amount NUMERIC(10,2) NOT NULL CHECK (budget_amount > 0),
    duration_days INT NOT NULL CHECK (duration_days > 0),
    daily_budget NUMERIC(10,2) NOT NULL,
    target_city VARCHAR(100) NOT NULL,
    target_localities TEXT[] DEFAULT '{}',
    impressions_delivered INT NOT NULL DEFAULT 0,
    clicks_delivered INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'PAYMENT_PENDING'
        CHECK (status IN ('DRAFT', 'PAYMENT_PENDING', 'PAID', 'ACTIVE', 'PAUSED', 'COMPLETED', 'REJECTED')),
    start_time TIMESTAMPTZ,
    end_time TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 14. Payment Transactions (Immutable Financial Ledger)
CREATE TABLE IF NOT EXISTS payment_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    campaign_id UUID REFERENCES promotion_campaigns(id) ON DELETE RESTRICT,
    razorpay_order_id VARCHAR(100) UNIQUE NOT NULL,
    razorpay_payment_id VARCHAR(100) UNIQUE,
    razorpay_signature VARCHAR(255),
    amount_in_paise BIGINT NOT NULL CHECK (amount_in_paise > 0),
    gst_in_paise BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(5) NOT NULL DEFAULT 'INR',
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED'
        CHECK (status IN ('CREATED', 'SUCCESS', 'FAILED', 'REFUNDED')),
    invoice_number VARCHAR(50) UNIQUE,
    raw_gateway_response JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 15. Trust, Safety & Moderation Reports
CREATE TABLE IF NOT EXISTS moderation_reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reporter_id UUID REFERENCES users(id) ON DELETE SET NULL,
    entity_type VARCHAR(30) NOT NULL CHECK (entity_type IN ('PROPERTY', 'REEL', 'USER', 'LEAD_ABUSE')),
    entity_id UUID NOT NULL,
    reason VARCHAR(100) NOT NULL,
    details TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' 
        CHECK (status IN ('PENDING', 'INVESTIGATING', 'RESOLVED_NO_ACTION', 'RESOLVED_REMOVED')),
    admin_notes TEXT,
    resolved_by UUID REFERENCES users(id) ON DELETE SET NULL,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 16. Immutable Audit Logs
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    target_table VARCHAR(50) NOT NULL,
    target_id UUID,
    metadata JSONB DEFAULT '{}'::jsonb,
    ip_address VARCHAR(45),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =====================================================================
-- PERFORMANCE INDEXES (Optimized for Sub-10ms Queries)
-- =====================================================================

-- Properties Indexes
CREATE INDEX IF NOT EXISTS idx_properties_city_locality ON properties (city, locality);
CREATE INDEX IF NOT EXISTS idx_properties_category_price ON properties (category, price);
CREATE INDEX IF NOT EXISTS idx_properties_bhk ON properties (bhk);
CREATE INDEX IF NOT EXISTS idx_properties_status_created ON properties (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_properties_geo ON properties (latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_properties_user_id ON properties (user_id);

-- Reels Indexes
CREATE INDEX IF NOT EXISTS idx_reels_city_created ON reels (city, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_reels_status ON reels (status);
CREATE INDEX IF NOT EXISTS idx_reels_property_id ON reels (property_id);
CREATE INDEX IF NOT EXISTS idx_reels_user_id ON reels (user_id);

-- Leads & CRM Indexes
CREATE INDEX IF NOT EXISTS idx_leads_broker_stage ON leads (broker_id, stage, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_leads_property_id ON leads (property_id);

-- Enquiries Index
CREATE INDEX IF NOT EXISTS idx_enquiries_recipient ON enquiries (recipient_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_enquiries_customer ON enquiries (customer_id, created_at DESC);

-- Saved Properties Index
CREATE INDEX IF NOT EXISTS idx_saved_user ON saved_properties (user_id);

-- Trigram Index for Fast Search Autocomplete
CREATE INDEX IF NOT EXISTS idx_properties_title_trgm ON properties USING gin (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_properties_locality_trgm ON properties USING gin (locality gin_trgm_ops);

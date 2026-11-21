-- 004_monetization_verification_schema.sql: PropReel Monetization, Promotions & Verification System

CREATE TABLE IF NOT EXISTS promotions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    property_id UUID REFERENCES properties(id) ON DELETE CASCADE,
    reel_id UUID REFERENCES reels(id) ON DELETE CASCADE,
    package_type VARCHAR(50) NOT NULL CHECK (package_type IN ('FEATURED_REEL', 'LOCALITY_BOOST', 'PREMIUM_SPOTLIGHT')),
    amount_inr NUMERIC(10, 2) NOT NULL,
    duration_days INT NOT NULL DEFAULT 7,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'EXPIRED', 'PENDING_PAYMENT')),
    payment_order_id VARCHAR(100),
    starts_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '7 days'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_promotions_active ON promotions (status, expires_at);
CREATE INDEX IF NOT EXISTS idx_promotions_property ON promotions (property_id);

CREATE TABLE IF NOT EXISTS verifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rera_number VARCHAR(100),
    physical_inspection_status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (physical_inspection_status IN ('PENDING', 'SCHEDULED', 'COMPLETED', 'EXEMPT')),
    documents_verified BOOLEAN NOT NULL DEFAULT FALSE,
    badge_level VARCHAR(50) NOT NULL DEFAULT 'RERA_VERIFIED' CHECK (badge_level IN ('RERA_VERIFIED', 'PROPREEL_INSPECTED', 'TOP_SELLER_CERTIFIED')),
    reviewer_notes TEXT,
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_verifications_prop ON verifications (property_id);

CREATE TABLE IF NOT EXISTS subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan_tier VARCHAR(50) NOT NULL CHECK (plan_tier IN ('FREE', 'STARTER_BROKER', 'PRO_BROKER', 'ENTERPRISE_DEVELOPER')),
    price_inr NUMERIC(10, 2) NOT NULL DEFAULT 0,
    monthly_leads_limit INT NOT NULL DEFAULT 10,
    features JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED')),
    renews_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '30 days'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user ON subscriptions (user_id);

-- =====================================================================
-- PropReel: Database Migration 002 - Comprehensive Pune / PCMC Seed Data
-- =====================================================================

-- 1. Insert Master Localities for Pune
INSERT INTO localities (city, name, pincode, latitude, longitude) VALUES
('Pune', 'Baner', '411045', 18.5590, 73.7868),
('Pune', 'Wakad', '411057', 18.5987, 73.7688),
('Pune', 'Kharadi', '411014', 18.5514, 73.9348),
('Pune', 'Hinjawadi', '411057', 18.5913, 73.7389),
('Pune', 'Kothrud', '411038', 18.5074, 73.8077),
('Pune', 'Viman Nagar', '411014', 18.5679, 73.9143),
('Pune', 'Bavdhan', '411021', 18.5134, 73.7699),
('Pune', 'Aundh', '411007', 18.5602, 73.8031)
ON CONFLICT (city, name) DO NOTHING;

-- 2. Insert Core Users with distinct roles
INSERT INTO users (id, mobile, email, full_name, role, avatar_url, city, is_verified, verification_badge, status) VALUES
('b1111111-1111-1111-1111-111111111111', '+919823045678', 'vikram.deshmukh@propreel.in', 'Vikram Deshmukh', 'BROKER', 'https://images.unsplash.com/photo-1560250097-0b93528c311a?w=400&q=80', 'Pune', TRUE, 'RERA_VERIFIED', 'ACTIVE'),
('b2222222-2222-2222-2222-222222222222', '+919890123456', 'priya.kulkarni@gmail.com', 'Priya Kulkarni', 'OWNER', 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=400&q=80', 'Pune', TRUE, 'OWNER_VERIFIED', 'ACTIVE'),
('b3333333-3333-3333-3333-333333333333', '+918000011223', 'sales.pune@godrejproperties.com', 'Godrej Properties Pune', 'BUILDER', 'https://images.unsplash.com/photo-1541888946425-d0fbb18086f6?w=400&q=80', 'Pune', TRUE, 'BUILDER_PARTNER', 'ACTIVE'),
('c4444444-4444-4444-4444-444444444444', '+919822111222', 'rohan.sharma@gmail.com', 'Rohan Sharma', 'CUSTOMER', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&q=80', 'Pune', TRUE, 'NONE', 'ACTIVE'),
('c5555555-5555-5555-5555-555555555555', '+919765433211', 'ananya.p@infosys.com', 'Ananya Patwardhan', 'CUSTOMER', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400&q=80', 'Pune', TRUE, 'NONE', 'ACTIVE'),
('a6666666-6666-6666-6666-666666666666', '+919000000001', 'admin@propreel.in', 'PropReel Admin Desk', 'ADMIN', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&q=80', 'Pune', TRUE, 'RERA_VERIFIED', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- 3. Insert Business Profile for Broker Vikram Deshmukh
INSERT INTO business_profiles (user_id, company_name, rera_number, gst_number, experience_years, operating_localities, office_address, bio, is_kyc_approved) VALUES
('b1111111-1111-1111-1111-111111111111', 'Apex Prime Realty Pune', 'A52100034190', '27AABCU9603R1ZM', 12, '{"Baner", "Wakad", "Hinjawadi", "Bavdhan"}', 'Level 4, Amar Paradigm, Baner Road, Pune 411045', 'Specialized in premium residential high-rises and IT rentals across West Pune with 100% legal RERA title check.', TRUE)
ON CONFLICT (user_id) DO NOTHING;

-- 4. Insert Properties
INSERT INTO properties (
    id, user_id, title, description, listing_type, category, price, price_display, is_negotiable, maintenance_charge,
    city, locality, landmark, pincode, latitude, longitude, bhk, bathrooms, carpet_area_sqft, floor_number, total_floors,
    furnishing, possession, rera_registered, rera_id, amenities, image_urls, status, is_featured, views_count, enquiries_count
) VALUES
(
    'p1111111-1111-1111-1111-111111111111',
    'b1111111-1111-1111-1111-111111111111',
    'VTP Earth One — Luxury 3 BHK with Panoramic Valley View',
    'Magnificent 3 BHK in prime Mahalunge-Baner link road. Features imported marble flooring, acoustic sliding glass windows, modular kitchen with chimney, home automation, and 2 designated covered parking slots. Resort-style clubhouse, infinity swimming pool, squash court, and reflexology garden.',
    'BUY', 'RESIDENTIAL_BUY', 14500000.00, '₹1.45 Cr', TRUE, 4500.00,
    'Pune', 'Baner', 'Near High Street & Expressway', '411045', 18.5590, 73.7868,
    3, 3, 1150.00, 16, 32, 'SEMI_FURNISHED', 'READY_TO_MOVE', TRUE, 'P52100029892',
    '["Lift", "Security", "Power Backup", "Gym", "Swimming Pool", "Covered Parking", "Clubhouse", "Jogging Track"]'::jsonb,
    '["https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=1000&q=80", "https://images.unsplash.com/photo-1600566753376-12c8ab7fb75b?w=1000&q=80", "https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?w=1000&q=80"]'::jsonb,
    'ACTIVE', TRUE, 1840, 28
),
(
    'p2222222-2222-2222-2222-222222222222',
    'b2222222-2222-2222-2222-222222222222',
    'Direct Owner: Elegant 2 BHK in Pristine Grandeur Wakad',
    'Spacious East-facing 2 BHK flat available for sale directly from owner. No brokerage. Well-ventilated living room, dry balcony, piped gas, and RO water system. Located 5 minutes away from Phoenix Marketcity Wakad and Hinjawadi IT Park Phase 1.',
    'BUY', 'RESIDENTIAL_BUY', 8200000.00, '₹82 Lakh', FALSE, 2800.00,
    'Pune', 'Wakad', 'Opposite Indira College', '411057', 18.5987, 73.7688,
    2, 2, 780.00, 7, 14, 'FURNISHED', 'READY_TO_MOVE', FALSE, NULL,
    '["Lift", "Security", "Piped Gas", "Children Play Area", "Covered Parking"]'::jsonb,
    '["https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?w=1000&q=80", "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=1000&q=80"]'::jsonb,
    'ACTIVE', FALSE, 920, 15
),
(
    'p3333333-3333-3333-3333-333333333333',
    'b3333333-3333-3333-3333-333333333333',
    'Godrej Rivergreens — Nature-Centric 2 & 3 BHK Waterfront Homes',
    'Master-planned 100-acre township along the Mula-Mutha river. Features over 5 acres of open greens, 1 km riverfront boulevard, retail plaza, and wellness sports arena. Easy connectivity to EON IT Park and World Trade Centre Kharadi.',
    'BUY', 'NEW_PROJECTS', 6900000.00, '₹69 Lakh Onwards', FALSE, 3200.00,
    'Pune', 'Kharadi', 'Near EON IT Park & Riverside Road', '411014', 18.5514, 73.9348,
    2, 2, 720.00, 4, 22, 'UNFURNISHED', 'UNDER_CONSTRUCTION', TRUE, 'P52100022411',
    '["Lift", "24x7 Security", "Clubhouse", "Cricket Pitch", "Swimming Pool", "Tennis Court", "EV Charging"]'::jsonb,
    '["https://images.unsplash.com/photo-1613490493576-7fde63acd811?w=1000&q=80", "https://images.unsplash.com/photo-1580587771525-78b9dba3b914?w=1000&q=80"]'::jsonb,
    'ACTIVE', TRUE, 3450, 62
),
(
    'p4444444-4444-4444-4444-444444444444',
    'b1111111-1111-1111-1111-111111111111',
    'Fully Furnished 2 BHK Rental Near Megapolis Hinjawadi',
    'Prime ready-to-move rental home for IT professionals. Includes double door refrigerator, smart TV, 3-seater sofa, queen beds with orthopaedic mattresses, modular kitchen with piped gas, washing machine, and high-speed fiber setup.',
    'RENT', 'RESIDENTIAL_RENT', 28000.00, '₹28,000 / mo', TRUE, 2000.00,
    'Pune', 'Hinjawadi', 'Phase 3, Megapolis Circle', '411057', 18.5913, 73.7389,
    2, 2, 680.00, 9, 18, 'FURNISHED', 'READY_TO_MOVE', FALSE, NULL,
    '["Lift", "Power Backup", "Security", "High-Speed Wi-Fi Ready", "Gym", "Reserved Car Parking"]'::jsonb,
    '["https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=1000&q=80", "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=1000&q=80"]'::jsonb,
    'ACTIVE', FALSE, 760, 19
)
ON CONFLICT (id) DO NOTHING;

-- 5. Insert Reels (Vertical Real Estate Videos Linked to Properties)
INSERT INTO reels (
    id, user_id, property_id, caption, video_url, thumbnail_url, duration_seconds,
    city, locality, tags, is_sponsored, views_count, likes_count, saves_count, shares_count, comments_count, enquiry_clicks_count
) VALUES
(
    'r1111111-1111-1111-1111-111111111111',
    'b1111111-1111-1111-1111-111111111111',
    'p1111111-1111-1111-1111-111111111111',
    'Check out this breathtaking 3 BHK with 180° unobstructed hills view in Baner! 3 bathrooms, Italian marble, and luxury amenities. Schedule your site visit today!',
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
    'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=1000&q=80',
    28.0, 'Pune', 'Baner', '{"LuxuryHomes", "Baner", "3BHK", "PuneRealEstate"}', TRUE, 4820, 428, 112, 64, 37, 85
),
(
    'r2222222-2222-2222-2222-222222222222',
    'b2222222-2222-2222-2222-222222222222',
    'p2222222-2222-2222-2222-222222222222',
    'Zero brokerage! Selling my East-facing 2 BHK flat in Pristine Grandeur, Wakad. Complete interior woodwork done, ready to move in immediately. DM or tap Enquire.',
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',
    'https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?w=1000&q=80',
    24.0, 'Pune', 'Wakad', '{"ZeroBrokerage", "OwnerListing", "WakadHomes", "2BHK"}', FALSE, 2150, 215, 89, 31, 18, 42
),
(
    'r3333333-3333-3333-3333-333333333333',
    'b3333333-3333-3333-3333-333333333333',
    'p3333333-3333-3333-3333-333333333333',
    'Live alongside nature! Launching Tower 4 at Godrej Rivergreens, Kharadi. Smart 2 & 3 BHK homes with 5 acres of green township. Special festive pricing offer!',
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4',
    'https://images.unsplash.com/photo-1613490493576-7fde63acd811?w=1000&q=80',
    30.0, 'Pune', 'Kharadi', '{"GodrejProperties", "NewLaunch", "Kharadi", "RiversideHomes"}', TRUE, 8920, 890, 340, 180, 54, 210
),
(
    'r4444444-4444-4444-4444-444444444444',
    'b1111111-1111-1111-1111-111111111111',
    'p4444444-4444-4444-4444-444444444444',
    'Working in Hinjawadi IT Park? Walk to your office! Fully furnished 2 BHK with all appliances & high-speed Wi-Fi setup ready. Available immediately for rent.',
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4',
    'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=1000&q=80',
    22.0, 'Pune', 'Hinjawadi', '{"HinjawadiITPark", "FurnishedRentals", "PuneRent"}', FALSE, 1640, 164, 55, 19, 12, 33
)
ON CONFLICT (id) DO NOTHING;

-- Update linked reel pointers on properties
UPDATE properties SET linked_reel_id = 'r1111111-1111-1111-1111-111111111111' WHERE id = 'p1111111-1111-1111-1111-111111111111';
UPDATE properties SET linked_reel_id = 'r2222222-2222-2222-2222-222222222222' WHERE id = 'p2222222-2222-2222-2222-222222222222';
UPDATE properties SET linked_reel_id = 'r3333333-3333-3333-3333-333333333333' WHERE id = 'p3333333-3333-3333-3333-333333333333';
UPDATE properties SET linked_reel_id = 'r4444444-4444-4444-4444-444444444444' WHERE id = 'p4444444-4444-4444-4444-444444444444';

-- 6. Insert Enquiries & Leads
INSERT INTO enquiries (
    id, property_id, reel_id, customer_id, recipient_id, customer_name, customer_phone, intent, message, consent_given
) VALUES
(
    'e1111111-1111-1111-1111-111111111111',
    'p1111111-1111-1111-1111-111111111111',
    'r1111111-1111-1111-1111-111111111111',
    'c4444444-4444-4444-4444-444444444444',
    'b1111111-1111-1111-1111-111111111111',
    'Rohan Sharma', '+919822111222', 'SITE_VISIT',
    'Interested in 3 BHK Baner for family. Please schedule a site visit this Saturday.', TRUE
),
(
    'e2222222-2222-2222-2222-222222222222',
    'p4444444-4444-4444-4444-444444444444',
    'r4444444-4444-4444-4444-444444444444',
    'c5555555-5555-5555-5555-555555555555',
    'b1111111-1111-1111-1111-111111111111',
    'Ananya Patwardhan', '+919765433211', 'CALLBACK',
    'Moving to Infosys Phase 2 next month. Need full video tour.', TRUE
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO leads (
    id, enquiry_id, broker_id, customer_name, customer_phone, property_id, property_title, property_price_display, locality, stage, notes, scheduled_visit_at
) VALUES
(
    'l1111111-1111-1111-1111-111111111111',
    'e1111111-1111-1111-1111-111111111111',
    'b1111111-1111-1111-1111-111111111111',
    'Rohan Sharma', '+919822111222',
    'p1111111-1111-1111-1111-111111111111',
    'VTP Earth One — Luxury 3 BHK with Panoramic Valley View', '₹1.45 Cr', 'Baner',
    'SITE_VISIT', 'Customer pre-approved loan of ₹1.5 Cr from HDFC Bank. Looking for possession within 3 months.',
    NOW() + INTERVAL '2 days'
),
(
    'l2222222-2222-2222-2222-222222222222',
    'e2222222-2222-2222-2222-222222222222',
    'b1111111-1111-1111-1111-111111111111',
    'Ananya Patwardhan', '+919765433211',
    'p4444444-4444-4444-4444-444444444444',
    'Fully Furnished 2 BHK Rental Near Megapolis Hinjawadi', '₹28,000 / mo', 'Hinjawadi',
    'CONTACTED', 'Spoke on call; shared full video walkthrough on WhatsApp. Will finalize rent agreement draft.',
    NULL
)
ON CONFLICT (id) DO NOTHING;

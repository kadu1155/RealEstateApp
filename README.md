# PropReel — Vertical Video Real Estate Discovery & Broker CRM Platform

> **India's Free Real Estate Discovery & Reels Platform.**  
> PropReel connects home buyers, tenants, verified property owners, brokers, and developers through immersive, TikTok/Instagram-style 9:16 vertical video tours, MahaRERA trust verifications, 1-tap WhatsApp communication, and an end-to-end broker CRM lead pipeline. Tailored for major Indian urban micro-markets starting with **Pune, Maharashtra** (Baner, Wakad, Hinjawadi, Kharadi, Kothrud, Kalyani Nagar).

---

## 🏛️ System Architecture

```
                                  ┌────────────────────────┐
                                  │   Android App Client   │
                                  │  Jetpack Compose + M3  │
                                  │   (Kotlin Coroutines)  │
                                  └───────────┬────────────┘
                                              │ REST / JSON
                                              ▼
                                  ┌────────────────────────┐
                                  │  Express TypeScript    │
                                  │  Backend REST Service  │
                                  └─────┬────────────┬─────┘
                                        │            │
                     PostgreSQL + PostGIS            Redis Cache
                     (MahaRERA Geo Data)             (Feed Caching & OTP)
```

### 1. Mobile Client Tier (Android)
- **Framework:** 100% Kotlin & Jetpack Compose (Material 3).
- **Architecture:** Clean Architecture + MVVM with StateFlow & unidirectional data flow.
- **Adaptive Launcher:** Custom adaptive launcher icon with responsive vector foreground and shadow layers.
- **Visual Design:** Warm Pune Sunset Palette (Emerald Primary `#10B981`, Sunset Orange `#F97316`, Amber `#F59E0B`, MahaRERA Blue `#2563EB`, WhatsApp Green `#25D366`).
- **Core Screens:**
  - `HomeScreen`: Vertical snap pager for full-screen 9:16 property video reels, like/save animations, creator trust chips, and linked property conversion cards.
  - `PropertySearchScreen`: Pune micro-market locality selector, category chips, BHK filters, budget sliders, and sorted property list.
  - `PropertyDetailScreen`: Verified badge indicators, 4K image carousel, specifications table, amenities grid, 1-tap WhatsApp chat CTA (`wa.me`), native share chooser, and callback/site visit request modal.
  - `LeadInboxScreen`: Broker CRM inbox with Kanban pipeline stages (`NEW` $\to$ `CONTACTED` $\to$ `SITE_VISIT` $\to$ `NEGOTIATION` $\to$ `CLOSED`), 1-tap WhatsApp contact, phone dialer, and stage progression notes.
  - `ProfileScreen` & `PromotionModal`: Role switcher (`BUYER`, `OWNER`, `BROKER`, `BUILDER`), listings manager, listing boost checkout (`FEATURED_REEL`, `LOCALITY_BOOST`), and MahaRERA verification submission.
  - `CreatePostModal`: 1-tap listing & vertical reel publishing with Pune locality selector, price formatting, and amenity tags.

### 2. Backend Service Tier (Node.js & TypeScript)
- **Runtime:** Node.js 20+, Express.js, TypeScript.
- **Database:** PostgreSQL 16 + PostGIS extension for spatial radius queries (`ST_DWithin`, `ST_MakePoint`).
- **Caching & OTP:** Redis 7 for high-velocity reel feed caching and OTP session validation.
- **Security:** Helmet, CORS, JWT authentication with access/refresh token rotation.

---

## 🚀 Micro-Market Seed Data (Pune Focus)

The database comes pre-seeded with verified Pune listings across core residential and IT corridors:
1. **Baner:** Supreme Estia (3 BHK, ₹1.45 Cr, MahaRERA: P52100028912)
2. **Kharadi:** VTP Cygnus (2 BHK, ₹88 Lakh, MahaRERA: P52100026781)
3. **Wakad:** Kolte Patil Western Avenue (2 BHK, ₹76 Lakh, MahaRERA: P52100020119)
4. **Hinjawadi:** Megapolis Mystic Phase 3 (1 BHK, ₹42 Lakh, MahaRERA: P52100018442)
5. **Kothrud:** Panchshil One Kothrud (4 BHK, ₹3.85 Cr, MahaRERA: P52100029801)
6. **Kalyani Nagar:** Waterfront Luxury Penthouse (4 BHK, ₹5.50 Cr, MahaRERA: P52100031102)

---

## 📡 REST API Reference

All endpoints are prefixed with `/api/v1`.

### 1. Authentication (`/api/v1/auth`)
| Method | Path | Description |
| :--- | :--- | :--- |
| `POST` | `/send-otp` | Request 6-digit OTP for Indian phone numbers (+91) |
| `POST` | `/verify-otp` | Validate OTP; issues JWT accessToken, refreshToken, and user profile |
| `POST` | `/refresh-token` | Renew expired JWT access tokens |
| `POST` | `/profile` | Update profile information, role (`BROKER`/`OWNER`), and RERA ID |

### 2. Properties (`/api/v1/properties`)
| Method | Path | Description |
| :--- | :--- | :--- |
| `GET` | `/` | Query properties with filtering (`locality`, `bhk`, `minPrice`, `maxPrice`, `category`, `sort`) |
| `GET` | `/nearby` | Geospatial discovery using PostGIS distance (`lat`, `lng`, `radiusKm`) |
| `GET` | `/:id` | Detailed property information with seller credentials and linked reel |
| `POST` | `/` | Publish new property listing |
| `POST` | `/:id/bookmark` | Toggle user saved properties |

### 3. Vertical Reels (`/api/v1/reels`)
| Method | Path | Description |
| :--- | :--- | :--- |
| `GET` | `/feed` | Ranked vertical video feed with cursor pagination |
| `GET` | `/:id` | Single reel playback data |
| `POST` | `/` | Upload new video tour and associate with property |
| `POST` | `/:id/like` | Like / unlike reel |
| `POST` | `/:id/save` | Save / bookmark reel |

### 4. CRM Pipeline & Enquiries (`/api/v1/leads`, `/api/v1/enquiries`)
| Method | Path | Description |
| :--- | :--- | :--- |
| `POST` | `/enquiries` | Submit buyer enquiry (auto-routes to broker CRM and sends notification) |
| `GET` | `/leads` | Filter broker CRM leads by stage (`NEW`, `CONTACTED`, `SITE_VISIT`, `NEGOTIATION`, `CLOSED`) |
| `GET` | `/leads/stats` | Pipeline velocity and conversion funnel analytics |
| `PATCH` | `/leads/:id/stage` | Transition lead stage with audit log and follow-up notes |

### 5. Multi-Channel WhatsApp & Notifications (`/api/v1/notifications`)
| Method | Path | Description |
| :--- | :--- | :--- |
| `GET` | `/` | Fetch authenticated in-app notifications |
| `PATCH` | `/:id/read` | Mark individual notification as read |
| `PATCH` | `/read-all` | Mark all notifications as read |
| `POST` | `/whatsapp/preview` | Preview formatted WhatsApp enquiry and viral share payloads |

### 6. Monetization & Verification (`/api/v1/monetization`)
| Method | Path | Description |
| :--- | :--- | :--- |
| `GET` | `/plans` | Catalog of promotion boost packages and broker subscription tiers |
| `POST` | `/boost` | Promote listing or reel (`FEATURED_REEL`, `LOCALITY_BOOST`, `PREMIUM_SPOTLIGHT`) |
| `POST` | `/verify` | Submit MahaRERA registration number and request on-site physical video inspection |
| `POST` | `/subscribe` | Upgrade broker account tier |
| `GET` | `/promotions/:propertyId` | View active promotion campaigns for a property |

---

## 🛠️ Local Development & Deployment

### Quickstart with Docker Compose
To launch the entire backend stack (PostgreSQL with PostGIS, Redis, and Express API) with all migrations and Pune seed data preloaded:

```bash
cd backend
docker-compose up --build
```
The API will be available at `http://localhost:3000`.

### Healthcheck
```bash
curl http://localhost:3000/api/health
```

### Running Android Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```

### Compiling Debug APK
```bash
gradle :app:assembleDebug
```

---

## 📋 Quality & Compliance
- **Edge-to-Edge Enabled:** Native Android edge-to-edge window insets handling with `enableEdgeToEdge()`.
- **Accessibility:** Minimum touch targets of 48.dp across all buttons, semantic testTags on interactive components, and descriptive content descriptions.
- **Zero-Storage Permissions:** Does not request unnecessary storage permissions; zero-permission native sharing intent used.

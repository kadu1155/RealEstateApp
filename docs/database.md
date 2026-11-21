# PropReel Database & Backend Architecture Documentation

## 1. Overview
The **PropReel** data tier is architected on **PostgreSQL 16** with **PostGIS** spatial capabilities and **pg_trgm** full-text indexing, paired with **Redis 7** for caching and queues.

The database enforces ACID compliance, referential integrity with cascading constraints, and audit trails across all monetized actions.

---

## 2. Core Entities & Relationships

```
                     ┌──────────────────┐
                     │      users       │
                     └────────┬─────────┘
                              │ 1:1
                              ▼
                     ┌──────────────────┐
                     │business_profiles │
                     └──────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          │ 1:N               │ 1:N               │ 1:N
          ▼                   ▼                   ▼
   ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
   │  properties  │◄───┤    reels     │    │   campaigns  │
   └──────┬───────┘    └──────┬───────┘    └──────┬───────┘
          │                   │                   │
          │                   ├─────────────┐     │ 1:N
          │                   ▼             ▼     ▼
          │            ┌─────────────┐┌──────────────┐
          │            │ engagements ││ transactions │
          │            └─────────────┘└──────────────┘
          │
          │ 1:N
          ▼
   ┌──────────────┐
   │  enquiries   │
   └──────┬───────┘
          │ 1:1
          ▼
   ┌──────────────┐
   │    leads     │ (CRM Pipeline)
   └──────────────┘
```

---

## 3. Data Tables Reference

### `users`
- Primary identity table.
- Mobile number is unique and validated (+91 format).
- Roles: `CUSTOMER`, `OWNER`, `BROKER`, `BUILDER`, `REAL_ESTATE_PRO`, `ADMIN`.
- Verification Badges: `NONE`, `OWNER_VERIFIED`, `RERA_VERIFIED`, `BUILDER_PARTNER`.

### `business_profiles`
- Extended profile for brokers and builders.
- Contains MahaRERA registration number, operating localities array, and GSTIN.

### `properties`
- Core real estate listing inventory.
- Pricing in INR (`price` NUMERIC(14,2)), with formatted string `price_display`.
- Exact address privacy flag (`hide_exact_address` boolean).
- JSONB columns for dynamic amenities (`["Lift", "Gym", "Power Backup"]`) and media URLs.

### `reels`
- Vertical short-form videos (maximum 60s, optimized for 9:16 aspect ratio).
- Directly references `property_id` with `ON DELETE SET NULL` so reels can stand alone or link to inventory.
- Stores denormalized metrics (`likes_count`, `views_count`, `saves_count`, `enquiry_clicks_count`).

### `enquiries` & `leads`
- Customer initiates `enquiry` specifying intent (`CALLBACK`, `SITE_VISIT`, `BROCHURE`, `BEST_OFFER`) with explicit data consent.
- System automatically injects a corresponding `lead` into the seller's CRM inbox with stage `NEW`.
- Stages: `NEW` $\to$ `CONTACTED` $\to$ `INTERESTED` $\to$ `SITE_VISIT` $\to$ `NEGOTIATION` $\to$ `CLOSED`.

### `promotion_campaigns` & `payment_transactions`
- Paid ad campaigns for boosting reels, properties, or profiles.
- Strict Razorpay webhook signature verification before marking campaigns as `ACTIVE`.
- Immutable transaction ledger storing GST breakdowns and invoice numbers.

---

## 4. Query Performance & Indexing Strategy
- **Composite Index:** `(city, locality)` on `properties` allows instant sub-10ms filtering.
- **Trigram Index (`gin_trgm_ops`):** Accelerates partial-match search queries like `"Ban"` $\to$ `"Baner"`.
- **Feed Ordering Index:** `(city, created_at DESC)` on `reels` eliminates expensive in-memory sorts for high-throughput mobile scrolling.
- **Lead Pipeline Index:** `(broker_id, stage, updated_at DESC)` powers rapid CRM Kanban/list rendering.

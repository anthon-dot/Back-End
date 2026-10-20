# Public Market of Manticao - Rental Management System

An automated, role-governed rental lifecycle management platform for the **Public Market of Manticao (Misamis Oriental)**.

Migrated from legacy Spring Boot monolithic architecture to **Vue.js 3 + Node.js Express + Supabase PostgreSQL**.

---

## Architecture Overview

- **Frontend:** Vue.js 3, Vite, PrimeVue / Sakai design system, Leaflet interactive visual stall mapping.
- **Backend API:** Node.js, Express.js REST API (`server/src/`), modular architecture.
- **Database:** Supabase PostgreSQL with normalized schema, constraints, and Row Level Security (RLS).
- **Authentication & Authorization:** Supabase Auth + Server-side JWT RBAC for **6 Principal Actors**:
  1. `APPLICANT`
  2. `VENDOR` (Active Tenant)
  3. `MARKET_SUPERVISOR`
  4. `MAYOR` (Executive Decisions)
  5. `TREASURER` (Revenue & Collections)
  6. `COLLECTOR_INSPECTOR` (Field Collections & Safety Inspections)
  7. `ADMIN` (System Configuration)
- **Document Generation:** 3-Year Contract of Lease PDF, Municipal Occupancy Permit with QR verification and SHA-256 integrity hash, Official Payment Receipts.
- **Background Jobs:** `node-cron` workers handling monthly recurring billing on the 1st of each month and daily delinquency scans (30-, 60-, and 90-day demand notices).

---

## Getting Started

### 1. Prerequisites
- Node.js 18+ or 20+
- npm 9+
- Supabase account & project

### 2. Environment Setup
Copy the example environment file:
```bash
cp .env.example .env
```
Configure your `.env` variables:
```env
PORT=5000
VITE_BACKEND_URL=http://localhost:5000/api
VITE_SUPABASE_URL=https://your-project.supabase.co
VITE_SUPABASE_ANON_KEY=your-anon-key
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key
JWT_SECRET=your-32-char-jwt-secret
```

### 3. Database Migration & Seeding
Apply the SQL migration located in `supabase/migrations/`:
```bash
# Via Supabase CLI or Dashboard SQL Editor:
supabase/migrations/20261020_complete_manticao_migration.sql
supabase/seed.sql
```
Data dictionary reference: `docs/DATA_DICTIONARY.md`.

### 4. Running the Application

#### Start the Node.js Express Backend API
```bash
npm run server
# Server listening on http://localhost:5000
# Health check: http://localhost:5000/api/health
```

#### Start the Vue.js Frontend (in parallel)
```bash
npm run dev
# Vite dev server running at http://localhost:5173
```

---

## Testing & Automation Verification

Run the automated backend test suite:
```bash
npm run test:server
```
Runs 11/11 automated tests covering:
- Role-based authorization across all principal actors.
- Dynamic initial fee calculation: stall rights fee + 2 months advance rental baseline.
- Cryptographic SHA-256 integrity hash and QR code generation for digital permits.
- Multi-office application workflow state machine.
- 3-Year Contract of Lease duration calculation.
- Monthly recurring billing idempotency on `(contract_id, billing_period)`.
- Advance credit amortization without duplicate cash payments.
- Delinquency notice triggers (30, 60, and 90 days) and Mayoral revocation authorization.
- Field collection remittance reconciliation without double revenue counting.
- Inspector physical stall verification checklist and active vendor promotion.

Run the client-side lifecycle and status scenario tests:
```bash
node src/test-rental-lifecycle.js
node src/test-status-scenarios.js
```

---

## Legacy Data Migration Utility
To safely normalize and map legacy Spring Boot database records into the new Supabase schema:
```bash
npm run migrate:legacy
```

---

## Documentation Links
- [Data Dictionary](docs/DATA_DICTIONARY.md)
- [Migration Report](docs/MIGRATION_REPORT.md)

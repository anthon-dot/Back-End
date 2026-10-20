# MIGRATION REPORT
## Migration of Public Market of Manticao Rental Management System to Vue.js + Node.js + Supabase

**Date:** October 20, 2026  
**Status:** Completed & Verified  

---

### 1. Executive Summary
The Public Market of Manticao Rental Management System has been fully migrated from a Spring Boot Java monolithic architecture (`Bac-end/back-end`) to a decoupled, production-ready stack:

- **Frontend:** Vue.js 3 + Vite + PrimeVue/Sakai design system (`front-end-UI/`).
- **Backend API:** Node.js + Express.js REST API (`server/src/`).
- **Database:** Supabase PostgreSQL with fully normalized tables, enums, triggers, and Row Level Security (RLS) policies (`supabase/migrations/`).
- **Authentication & RBAC:** Supabase Auth with server-side JWT verification and role-based access control governing the six (6) authoritative principal actors:
  1. `APPLICANT` (Prospective market stall onboarding candidate)
  2. `VENDOR` (Active market stall occupant with executed lease and QR permit)
  3. `MARKET_SUPERVISOR` (Stall inventory management, LOI review and endorsement to Mayor)
  4. `MAYOR` (Municipal executive for LOI approvals and 90-day delinquent lease revocations)
  5. `TREASURER` (Cash fee validation, OR receipt issuance, field collection remittance reconciliation)
  6. `COLLECTOR_INSPECTOR` (Daily micro-collections [₱100/day], stall setup physical inspections, and QR permit scanning)
  7. `ADMIN` (System administrator for rental rates, audit logs, and user credentials)
- **Document Generation & Verification:** 3-Year Contract of Lease PDF, Municipal Occupancy Permit with QR code and SHA-256 integrity hash, and Official Payment Receipts with dual-mode QR verification (Public read-only vs. Authenticated inspector).
- **Scheduled Background Processing:** `node-cron` workers running 1st-of-the-month recurring billing with advance credit amortization and daily delinquency scans (30-, 60-, and 90-day demand notices).

---

### 2. Completed Architecture & Deliverables

#### 2.1 Backend Project Structure (`server/src/`)
```
server/
├── src/
│   ├── config/
│   │   ├── constants.js          # Canonical enums (Roles, Statuses, Rates)
│   │   ├── env.js                # Environment config loader
│   │   └── supabase.js           # Authoritative server-side Supabase client
│   ├── middleware/
│   │   ├── auth.js               # Supabase JWT Bearer token validator
│   │   ├── rbac.js               # Role and permission guards
│   │   ├── auditLogger.js        # Immutable audit logging helper
│   │   └── errorHandler.js       # Central error & status code handler
│   ├── utils/
│   │   ├── crypto.js             # SHA-256 integrity hashing
│   │   ├── qrGenerator.js        # High-res QR code generator
│   │   └── pdfGenerator.js       # jsPDF contract, permit, and receipt engines
│   ├── services/
│   │   ├── rentalCalculationService.js   # Stall rights + 2-month advance formula
│   │   ├── stallService.js               # Interactive map coords & atomic reservations
│   │   ├── applicationWorkflowService.js # 8-step multi-office state machine
│   │   ├── contractService.js            # 3-year lease preparation and execution
│   │   ├── permitService.js              # Digital permit issuance & QR verification
│   │   ├── billingService.js             # Idempotent monthly billing & advance credit
│   │   ├── paymentService.js             # Cash payment recording & OR generation
│   │   ├── collectionRemittanceService.js# Field collections & Treasury reconciliation
│   │   ├── delinquencyService.js         # 30/60/90-day notices & Mayor lease revocation
│   │   └── reportService.js              # Deterministic database metrics & insights
│   ├── controllers/
│   │   ├── auth.controller.js
│   │   ├── application.controller.js
│   │   ├── stall.controller.js
│   │   ├── contract.controller.js
│   │   ├── permit.controller.js
│   │   ├── billing.controller.js
│   │   ├── payment.controller.js
│   │   ├── collection.controller.js
│   │   ├── delinquency.controller.js
│   │   └── report.controller.js
│   ├── routes/
│   │   ├── index.js              # Main API router (/api/...)
│   │   ├── auth.routes.js
│   │   ├── application.routes.js
│   │   ├── stall.routes.js
│   │   ├── contract.routes.js
│   │   ├── permit.routes.js
│   │   ├── billing.routes.js
│   │   ├── payment.routes.js
│   │   ├── collection.routes.js
│   │   ├── delinquency.routes.js
│   │   └── report.routes.js
│   ├── jobs/
│   │   └── jobRunner.js          # node-cron monthly billing and daily delinquency workers
│   ├── scripts/
│   │   └── migrate-legacy-data.js# Repeatable data migration utility
│   ├── app.js                    # Express app configuration & middleware
│   └── server.js                 # HTTP server listener (port 5000)
└── tests/
    └── server-workflow.test.js   # Automated unit and integration test suite
```

#### 2.2 Database Schema & Security (`supabase/`)
- `supabase/migrations/20261020_complete_manticao_migration.sql`:
  - 18 normalized tables with foreign keys, checks, and unique constraints.
  - Complete Row Level Security (RLS) policies restricting sensitive operations to authenticated municipal staff and tenant self-records.
  - Data dictionary generated at `docs/DATA_DICTIONARY.md`.
- `supabase/seed.sql`:
  - Seeds market sections (`Wet Section`, `Dry Goods`, `Food Court`, `Vegetables/Fruits`, `Commercial Block`).
  - Seeds rental rate formulas and stalls with visual interactive coordinates.

---

### 3. Verification of Business Logic & Automated Tests

All 11 test scenarios in `server/tests/server-workflow.test.js` pass with a 100% success rate:

1. **Role Authorization**: All principal actors and permissions correctly defined and guarded.
2. **Initial Fee Calculation**: Dynamic formula calculates stall rights fee + exactly two (2) months advance rental baseline.
3. **Cryptographic Integrity Hash**: Deterministic SHA-256 digest validates permit authenticity and flags tampered data.
4. **QR Code Generation**: Produces valid base64 PNG data URLs containing the public verification URL.
5. **Application State Machine**: Strict multi-office review order (LOI Submission -> Supervisor Review -> Mayor Approval -> Treasurer Payment -> Contract Execution -> Stall Inspection -> Permit Issuance -> Active Tenancy).
6. **3-Year Contract of Lease**: Enforces exact 3-year term from effective date.
7. **Monthly Billing Idempotency**: Strict uniqueness constraint on `(contract_id, billing_period)` prevents duplicate billing statements.
8. **Advance Credit Amortization**: Smoothly deducts remaining advance rent balance without creating duplicate cash revenue records.
9. **Delinquency Progression**: Automated triggers for 30-day, 60-day, and 90-day demand notices with Mayoral lease revocation flagging.
10. **Field Collection Remittance**: Batched collections reconciled by Treasurer without double revenue counting.
11. **Inspector Physical Verification & Vendor Activation**: Safety checklist validation (electrical, structural, fire safety, sanitation) and active vendor promotion with digital occupancy permit issuance.

---

### 4. How to Run the Migrated System

#### Start the Node.js Express Backend
```bash
# In front-end-UI/
npm run server
# Listening on http://localhost:5000
# Health check: http://localhost:5000/api/health
```

#### Start the Vue.js Frontend
```bash
# In front-end-UI/
npm run dev
# Frontend available at http://localhost:5173
```

#### Run Backend Workflow Tests
```bash
npm run test:server
```

#### Run Legacy Data Migration Utility
```bash
npm run migrate:legacy
```

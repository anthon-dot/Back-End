# Public Market Rental Management System — Supabase Backend

A high-performance, serverless backend architecture built on **Supabase** for the Public Market Stall & Rental Management System (Municipality of Manticao).

## Architecture Overview

This project has migrated from a hybrid Render + Supabase setup to a **100% Supabase Serverless Architecture**:

```
[ Frontend (React / Vue / Next.js) ]
       │                                     │
       ▼ (Direct JS Queries with RLS)        ▼ (Privileged Multi-Stage Workflow)
 [ Supabase Database + Auth + Storage ]   [ Supabase Edge Functions (Hono + TS) ]
```

- **Authentication & RBAC**: Supabase Auth (`auth.users`) synchronized with `public.profiles` via database triggers. Roles supported: `ADMIN`, `TREASURER`, `MARKET_SUPERVISOR`, `BPLO`, `ENDORSING_OFFICE`, `APPLICANT`, `TENANT`.
- **Database & Security**: PostgreSQL with granular **Row Level Security (RLS)** on all tables.
- **File Storage**: Supabase Storage bucket (`uploads`) for applicant IDs, clearance permits, and business letters.
- **Serverless Edge Functions**: TypeScript + [Hono](https://hono.dev/) serverless functions handling multi-stage municipal approvals and Google Gemini AI insights.
- **Automated Cron**: PostgreSQL `pg_cron` jobs automatically mark overdue invoices and alert administrators of expiring contracts.
- **CI/CD**: GitHub Actions workflow automatically deploys migrations and edge functions on push to `main`.

---

## Directory Structure

```
├── .github/workflows/
│   └── deploy-supabase.yml              # CI/CD: Automated Supabase CLI deployment
├── supabase/
│   ├── config.toml                      # Supabase CLI project configuration
│   ├── migrations/                      # Version-controlled database migrations
│   │   ├── 20260920000001_initial_schema.sql
│   │   ├── 20260920000002_storage_setup.sql
│   │   └── 20260920000003_cron_jobs.sql
│   ├── functions/                       # Deno TypeScript Edge Functions (Hono)
│   │   ├── approval-workflow/index.ts   # Multi-stage municipal approval workflow
│   │   ├── ai-insights/index.ts         # Google Gemini AI market intelligence briefings
│   │   └── deno.json                    # Deno dependencies and import maps
│   ├── schema.sql                       # Complete monolithic SQL schema
│   ├── seed.sql                         # Initial market stall types and stalls
│   ├── storage.sql                      # Bucket and storage policies
│   └── cron.sql                         # Scheduled cron jobs
├── SUPABASE_INTEGRATION_GUIDE.md        # Comprehensive frontend query guide
├── package.json                         # Supabase deployment commands
└── back-end/                            # Legacy Java Spring Boot application (archived)
```

---

## Deployment Guide

### Option A: Automatic Deployment (CI/CD via GitHub Actions)

Add the following repository secrets in **GitHub -> Settings -> Secrets and variables -> Actions**:

| Secret Name | Description | Where to find |
| :--- | :--- | :--- |
| `SUPABASE_ACCESS_TOKEN` | Supabase Personal Access Token | Supabase Dashboard -> Account -> Access Tokens |
| `SUPABASE_PROJECT_ID` | Your Project Reference (`uzykxxphunglcwojoqtf`) | Project Settings -> General |
| `SUPABASE_DB_PASSWORD` | PostgreSQL Database Password | Provided when creating project |

Whenever code is pushed to `main`, GitHub Actions will automatically apply migrations and deploy the Edge Functions.

---

### Option B: Manual CLI Deployment

1. **Login to Supabase CLI**:
   ```bash
   npm run supabase:login
   ```

2. **Link to your cloud project**:
   ```bash
   npm run supabase:link
   ```

3. **Deploy database migrations**:
   ```bash
   npm run supabase:db:push
   ```

4. **Deploy Edge Functions**:
   ```bash
   npm run supabase:functions:deploy
   ```

5. **Deploy everything at once**:
   ```bash
   npm run supabase:deploy
   ```

---

## Edge Function Endpoints

### 1. `approval-workflow`

| Method | Path | Required Role | Purpose |
| :--- | :--- | :--- | :--- |
| `POST` | `/approval-workflow/treasurer-approve` | `TREASURER`, `ADMIN` | Records advance deposit and issues receipt |
| `POST` | `/approval-workflow/assign-stall` | `MARKET_SUPERVISOR`, `ADMIN` | Assigns stall and prepares lease contract |
| `POST` | `/approval-workflow/bplo-approve` | `BPLO`, `ADMIN` | Validates business permit requirements |
| `POST` | `/approval-workflow/final-endorse` | `ENDORSING_OFFICE`, `ADMIN` | Final municipal endorsement |
| `POST` | `/approval-workflow/permit-payment` | `TREASURER`, `ADMIN` | Final fee payment, activates contract and first invoice |
| `POST` | `/approval-workflow/reject` | Reviewers | Disapproves application and releases stall |

### 2. `ai-insights`

| Method | Path | Purpose |
| :--- | :--- | :--- |
| `GET` | `/ai-insights/summary` | Executive briefing on occupancy and overdue accounts |
| `POST` | `/ai-insights/generate` | On-demand customized report generation with Gemini AI |

---

## Frontend Integration

See [`SUPABASE_INTEGRATION_GUIDE.md`](./SUPABASE_INTEGRATION_GUIDE.md) for full `@supabase/supabase-js` query hooks, authentication handling, and Realtime event subscriptions.

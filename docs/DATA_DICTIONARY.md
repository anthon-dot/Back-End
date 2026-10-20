# Data Dictionary: Public Market of Manticao Rental Management System

This document outlines the normalized database schema in Supabase PostgreSQL, detailing the entity definitions, column descriptions, relationships, constraints, and status lifecycle matrices.

---

## 1. Identity & Access

### `user_profiles`
- **Purpose**: Stores extended user profiles synchronized 1-to-1 with `auth.users`.
- **Columns**:
  - `id` (UUID, PK): References `auth.users(id)` ON DELETE CASCADE.
  - `username` (VARCHAR(100), UNIQUE): Unique user login handle.
  - `full_name` (VARCHAR(255), NOT NULL): Official full name of the user.
  - `email` (VARCHAR(255)): User email address.
  - `phone_number` (VARCHAR(50)): Contact mobile number.
  - `role` (`user_role_enum`, NOT NULL): Strictly one of the 6 Principal Actors:
    1. `APPLICANT`: Prospective vendor undergoing onboarding
    2. `VENDOR`: Active market stall occupant
    3. `MARKET_SUPERVISOR`: Market inventory and application endorser
    4. `MAYOR`: Municipal executive for LOI approval and lease revocation
    5. `TREASURER`: Municipal treasury revenue and payment validator
    6. `COLLECTOR_INSPECTOR`: Field market collector and stall setup inspector
    7. `ADMIN`: System administrator
  - `is_active` (BOOLEAN, NOT NULL DEFAULT TRUE): Active account status.
  - `created_at` / `updated_at` (TIMESTAMPTZ).

### `profiles` (Compatibility View)
- **Purpose**: Compatibility view for frontend queries targeting `public.profiles`. Aliases `full_name` as `name` and casts `role` to text.

### `temporary_accounts`
- **Purpose**: Tracks transient applicant registrations during LOI submission before full authentication verification.
- **Columns**: `id` (UUID), `auth_user_id` (UUID), `email`, `phone_number`, `first_name`, `last_name`, `access_token_hash`, `expires_at`, `is_claimed`.

### `login_history`
- **Purpose**: Security auditing of authentication attempts.
- **Columns**: `id` (BIGSERIAL), `user_id` (UUID), `ip_address`, `user_agent`, `login_time`, `success`.

### `audit_logs`
- **Purpose**: Immutable ledger recording all administrative, financial, and approval operations.
- **Columns**: `id` (BIGSERIAL), `user_id` (UUID), `role`, `action`, `entity_name`, `entity_id`, `details` (JSONB), `ip_address`, `created_at`.

---

## 2. Market & Stall Inventory

### `markets`
- **Purpose**: Represents municipal market facilities.
- **Columns**: `id` (BIGSERIAL), `code`, `name`, `municipality`, `description`, `is_active`.

### `sections`
- **Purpose**: Market zoning sections (e.g. Wet Section, Dry Goods, Food Court, Commercial Block).
- **Columns**: `id` (BIGSERIAL), `market_id` (FK), `code`, `name`, `rate_category` (`OLD` / `NEW`).

### `rental_rates`
- **Purpose**: Configurable formula rules for monthly rent and initial fees.
- **Columns**: `id` (BIGSERIAL), `section_id` (FK), `stall_type`, `rate_category` (`OLD` / `NEW`), `base_monthly_rate` (NUMERIC), `rate_per_sqm`, `stall_rights_fee`, `advance_months` (INT DEFAULT 2).

### `stalls`
- **Purpose**: Physical market stall spaces with visual map coordinates.
- **Columns**:
  - `id` (BIGSERIAL, PK)
  - `stall_no` (VARCHAR(100), UNIQUE): e.g. `WS-01`, `DG-02`.
  - `stall_type` (VARCHAR(100))
  - `rate_category` (`OLD` / `NEW`)
  - `dimensions` (VARCHAR(50)): e.g. `3m x 3m`.
  - `area_sqm` (NUMERIC(8,2))
  - `monthly_rent` (NUMERIC(12,2))
  - `stall_rights_fee` (NUMERIC(12,2))
  - `status` (`stall_status_enum`): `AVAILABLE`, `PENDING`, `RESERVED`, `OCCUPIED`, `MAINTENANCE`, `ARCHIVED`.
  - `map_coordinates` (JSONB): Visual interactive placement `{ x, y, lat, lng }`.
  - `occupant_id` (BIGINT, FK to `occupants(id)`).

---

## 3. Stakeholder & Application Management

### `stakeholders`
- **Purpose**: Master vendor profiles linking user credentials, business details, advance balances, and verification states.
- **Columns**: `id` (BIGSERIAL), `user_id` (UUID), `first_name`, `middle_name`, `last_name`, `business_name`, `business_type`, `contact`, `email`, `address`, `selected_stall_id`, `total_advance_amount`, `advance_balance`, `verified_tenant`, `is_archived`.

### `stakeholder_applications`
- **Purpose**: Persisted workflow instance tracking an application from submission to completion.
- **Status Lifecycle (17-Step Municipal State Machine)**:
  - `DRAFT`: Initial draft
  - `PENDING_SUPERVISOR_REVIEW`: LOI submitted, stall reserved as PENDING
  - `SUPERVISOR_REJECTED`: Rejected by Market Supervisor
  - `PENDING_MAYOR_APPROVAL`: Endorsed by Supervisor to Mayor
  - `MAYOR_REJECTED`: Disapproved by Municipal Mayor
  - `PENDING_TREASURER_PAYMENT`: Mayor approved LOI, initial fees calculated
  - `PAID_READY_FOR_CONTRACT`: Initial fees (Stall Rights + 2 Mos Advance) recorded by Treasurer
  - `PENDING_INSPECTION`: 3-Year Contract of Lease executed, awaiting physical stall setup verification
  - `INSPECTION_PASSED`: Field Inspector verified clearances (electrical, structural, fire safety, sanitation)
  - `BUSINESS_PERMIT_ELIGIBLE`: Commercial compliance validated
  - `COMPLETED`: Promoted to ACTIVE_VENDOR, digital occupancy permit issued with QR code
  - `REVOKED`: Mayor-authorized lease cancellation due to 90+ days arrears
  - `CANCELLED`: Voluntary withdrawal or administrative cancellation
- **Key Tracking Columns**: `market_supervisor_approved`, `market_supervisor_approved_at`, `mayor_decision`, `mayor_approved_at`, `mayor_remarks`, `contract_executed_at`, `hazard_free_confirmed`, `hazard_free_document_url`.

### `stakeholder_documents`
- **Purpose**: Secure document file storage references for LOI, Valid IDs, and inspection proofs.
- **Columns**: `id` (BIGSERIAL), `stakeholder_id` (FK), `document_type`, `file_name`, `file_path`.

### `approval_history`
- **Purpose**: Stage-by-stage audit trail of who approved or rejected each application.
- **Columns**: `id` (BIGSERIAL), `stakeholder_id`, `stage`, `status`, `action_by` (UUID), `actor_name`, `actor_role`, `remarks`.

---

## 4. Legal & Occupancy

### `occupants`
- **Purpose**: Active physical stall tenancies.
- **Columns**: `id` (BIGSERIAL), `stakeholder_id` (FK UNIQUE), `stall_id` (FK), `advance_balance` (NUMERIC), `status` (`PENDING`, `ACTIVE`, `REVOKED`), `occupancy_date`.

### `contracts`
- **Purpose**: 3-Year Contract of Lease between Municipality of Manticao and the Vendor.
- **Columns**: `id` (BIGSERIAL), `contract_no` (UNIQUE), `occupant_id`, `stall_id`, `start_date`, `end_date` (3 years), `monthly_rent`, `billing_frequency` (`MONTHLY`), `status` (`DRAFT`, `PENDING_APPROVAL`, `ACTIVE`, `EXPIRED`, `REVOKED`).

### `occupancy_permits`
- **Purpose**: Official municipal digital occupancy permit featuring QR verification and cryptographic integrity hash.
- **Columns**: `id` (UUID), `permit_no` (UNIQUE), `vendor_id`, `stall_id`, `contract_id`, `issue_date`, `expiration_date`, `status` (`ACTIVE`, `EXPIRED`, `REVOKED`), `qr_code_data`, `integrity_hash` (SHA-256), `pdf_storage_path`.

### `permit_scan_logs`
- **Purpose**: Scan verification logs recorded upon public inspection or field officer scanning.
- **Columns**: `id` (BIGSERIAL), `permit_id`, `scanned_by` (UUID), `scanner_role`, `scanner_ip`, `scan_type` (`PUBLIC` / `INSPECTOR`), `is_valid`.

### `stall_inspections`
- **Purpose**: Pre-occupancy and recurring safety inspections.
- **Columns**: `id` (BIGSERIAL), `stall_id`, `inspector_id`, `electrical_clearance`, `structural_clearance`, `fire_safety_clearance`, `sanitation_clearance`, `status` (`PASSED`, `FAILED`, `CONDITIONAL`).

---

## 5. Financial Management

### `billings`
- **Purpose**: Monthly recurring rent invoices generated on the 1st of each month.
- **Columns**: `id` (BIGSERIAL), `billing_no` (UNIQUE), `occupant_id`, `contract_id`, `billing_period` (`MONTHLY-MM-YYYY`), `total_amount`, `paid_amount`, `balance`, `due_date`, `status` (`UNPAID`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`).
- **Uniqueness Constraint**: Unique on `(contract_id, billing_period)` preventing duplicate billing.

### `payments`
- **Purpose**: Recorded cash transactions, walk-in payments, and field collections.
- **Columns**: `id` (BIGSERIAL), `receipt_no` (UNIQUE), `reference_no`, `billing_id`, `stakeholder_id`, `amount`, `payment_type` (`ADVANCE_PAYMENT`, `STALL_RIGHTS_FEE`, `APPLICATION_FEE`, `RENT_PAYMENT`, `FIELD_COLLECTION`), `payment_channel` (`CASH`, `FIELD_COLLECTOR`, `ONLINE`), `cashier_or_collector_id`.

### `payment_receipts`
- **Purpose**: Official Receipt records for audit and PDF printing.
- **Columns**: `id` (BIGSERIAL), `receipt_no` (UNIQUE), `payment_id`, `payer_name`, `stall_no`, `amount_paid`, `issued_by`, `issued_at`.

### `collection_remittances` & `collection_remittance_items`
- **Purpose**: Batch reconciliation of daily field collections (e.g. ₱100/day collections) submitted by Field Market Collectors to the Municipal Treasury without double revenue counting.
- **Columns**: `id` (BIGSERIAL), `remittance_no`, `collector_id`, `total_amount`, `collection_count`, `status` (`SUBMITTED`, `RECONCILED`, `REJECTED`), `treasurer_id`, `reconciled_at`.

---

## 6. Communications & Background Jobs

### `notifications`
- **Purpose**: Multi-channel in-app notifications.
- **Columns**: `id` (BIGSERIAL), `stakeholder_id`, `user_id`, `target_role`, `title`, `message`, `notification_type`, `is_read`.

### `notification_delivery_logs`
- **Purpose**: Email & SMS delivery audit trail.
- **Columns**: `id` (BIGSERIAL), `notification_id`, `channel` (`IN_APP`, `EMAIL`, `SMS`), `recipient`, `delivery_status`, `error_message`, `attempts`.

### `background_jobs`
- **Purpose**: Durable, retryable background job queue for automated monthly billing, delinquency scans, and notification dispatches.
- **Columns**: `id` (BIGSERIAL), `job_type`, `payload` (JSONB), `status` (`PENDING`, `RUNNING`, `COMPLETED`, `FAILED`), `attempts`, `run_at`, `completed_at`.

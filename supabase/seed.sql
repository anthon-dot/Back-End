-- ==============================================================================
-- INITIAL SEED DATA FOR RENTAL MANAGEMENT SYSTEM (MANTICAO PUBLIC MARKET)
-- Run this in Supabase SQL Editor after schema.sql
-- ==============================================================================

-- 1. Stall Types
INSERT INTO public.stall_types (name, description, status)
VALUES 
    ('Wet Section', 'Fish, meat, poultry, and related fresh produce', 'ACTIVE'),
    ('Dry Goods', 'Clothing, footwear, kitchenware, and general merchandise', 'ACTIVE'),
    ('Vegetable & Fruit Section', 'Fresh local fruits, vegetables, and tubers', 'ACTIVE'),
    ('Food Court / Eatery', 'Cooked meals, snacks, and beverage stalls', 'ACTIVE'),
    ('Grocery & Commercial', 'Packaged grocery goods, spices, and grains', 'ACTIVE')
ON CONFLICT (name) DO NOTHING;

-- 2. Rental Rates
INSERT INTO public.rental_rates (stall_type, monthly_rate, description, status)
VALUES
    ('Wet Section', 3500.00, 'Standard rate for wet market stalls with drainage access', 'ACTIVE'),
    ('Dry Goods', 2800.00, 'Standard rate for dry goods stalls', 'ACTIVE'),
    ('Vegetable & Fruit Section', 2200.00, 'Standard rate for open vegetable display tables', 'ACTIVE'),
    ('Food Court / Eatery', 4500.00, 'Standard rate for eatery booths with utility provisions', 'ACTIVE'),
    ('Grocery & Commercial', 3800.00, 'Standard rate for commercial grocery stalls', 'ACTIVE');

-- 3. Initial Stalls
INSERT INTO public.stalls (stall_no, stall_type, monthly_rent, status, info)
VALUES
    ('WS-01', 'Wet Section', 3500.00, 'AVAILABLE', 'Corner stall near water supply'),
    ('WS-02', 'Wet Section', 3500.00, 'AVAILABLE', 'Standard wet section fish stall'),
    ('WS-03', 'Wet Section', 3500.00, 'AVAILABLE', 'Meat section counter stall'),
    ('DG-01', 'Dry Goods', 2800.00, 'AVAILABLE', 'Front entrance dry goods stall'),
    ('DG-02', 'Dry Goods', 2800.00, 'AVAILABLE', 'Interior aisle dry goods stall'),
    ('VF-01', 'Vegetable & Fruit Section', 2200.00, 'AVAILABLE', 'Central display booth for fruits & veggies'),
    ('VF-02', 'Vegetable & Fruit Section', 2200.00, 'AVAILABLE', 'Side aisle vegetable counter'),
    ('FC-01', 'Food Court / Eatery', 4500.00, 'AVAILABLE', 'Food stall with dedicated sink and grease trap'),
    ('GC-01', 'Grocery & Commercial', 3800.00, 'AVAILABLE', 'Spacious grocery booth near unloading dock')
ON CONFLICT (stall_no) DO NOTHING;

-- ==============================================================================
-- 4. INSTRUCTIONS TO PROMOTE A USER TO ADMIN:
-- Once you register your first user account through your frontend or Supabase Auth UI,
-- execute this SQL statement in the Supabase SQL Editor replacing 'your-email@example.com':
--
-- UPDATE public.profiles
-- SET role = 'ADMIN'
-- WHERE id = (SELECT id FROM auth.users WHERE email = 'your-email@example.com');
-- ==============================================================================

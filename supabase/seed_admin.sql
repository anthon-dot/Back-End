-- ==============================================================================
-- MUNICIPAL STAFF AND ACTORS SEED SCRIPT: PUBLIC MARKET OF MANTICAO
-- Seeds the 6 principal actors into auth.users and public.user_profiles
-- ==============================================================================

DO $$
DECLARE
  v_admin_id UUID := '8387ef47-c07f-4c2e-a952-51c3dd2789b4';
  v_supervisor_id UUID := 'a1111111-c07f-4c2e-a952-51c3dd2789b1';
  v_mayor_id UUID := 'c3333333-c07f-4c2e-a952-51c3dd2789b3';
  v_treasurer_id UUID := 'd4444444-c07f-4c2e-a952-51c3dd2789b4';
  v_collector_id UUID := 'e5555555-c07f-4c2e-a952-51c3dd2789b5';
  v_vendor_id UUID := 'f6666666-c07f-4c2e-a952-51c3dd2789b6';
  v_applicant_id UUID := 'b7777777-c07f-4c2e-a952-51c3dd2789b7';
  v_default_pw TEXT := extensions.crypt('AdminPassword123!', extensions.gen_salt('bf'));
BEGIN

  -- 1. System Administrator
  IF NOT EXISTS (SELECT 1 FROM auth.users WHERE email = 'admin@manticao.market') THEN
    INSERT INTO auth.users (
      instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
      raw_app_meta_data, raw_user_meta_data, created_at, updated_at
    ) VALUES (
      '00000000-0000-0000-0000-000000000000', v_admin_id, 'authenticated', 'authenticated',
      'admin@manticao.market', v_default_pw, NOW(),
      '{"provider":"email","providers":["email"]}',
      '{"username":"admin","name":"System Administrator","role":"ADMIN"}',
      NOW(), NOW()
    );
  END IF;

  INSERT INTO public.user_profiles (id, username, full_name, email, role, is_active)
  VALUES (
    (SELECT id FROM auth.users WHERE email = 'admin@manticao.market'),
    'admin', 'System Administrator', 'admin@manticao.market', 'ADMIN', TRUE
  ) ON CONFLICT (id) DO UPDATE SET role = 'ADMIN', username = 'admin', is_active = TRUE;

  -- 2. Market Supervisor
  IF NOT EXISTS (SELECT 1 FROM auth.users WHERE email = 'supervisor@manticao.gov.ph') THEN
    INSERT INTO auth.users (
      instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
      raw_app_meta_data, raw_user_meta_data, created_at, updated_at
    ) VALUES (
      '00000000-0000-0000-0000-000000000000', v_supervisor_id, 'authenticated', 'authenticated',
      'supervisor@manticao.gov.ph', v_default_pw, NOW(),
      '{"provider":"email","providers":["email"]}',
      '{"username":"supervisor","name":"Office of the Market Supervisor","role":"MARKET_SUPERVISOR"}',
      NOW(), NOW()
    );
  END IF;

  INSERT INTO public.user_profiles (id, username, full_name, email, role, is_active)
  VALUES (
    (SELECT id FROM auth.users WHERE email = 'supervisor@manticao.gov.ph'),
    'supervisor', 'Office of the Market Supervisor', 'supervisor@manticao.gov.ph', 'MARKET_SUPERVISOR', TRUE
  ) ON CONFLICT (id) DO UPDATE SET role = 'MARKET_SUPERVISOR', username = 'supervisor', is_active = TRUE;

  -- 3. Municipal Mayor
  IF NOT EXISTS (SELECT 1 FROM auth.users WHERE email = 'mayor@manticao.gov.ph') THEN
    INSERT INTO auth.users (
      instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
      raw_app_meta_data, raw_user_meta_data, created_at, updated_at
    ) VALUES (
      '00000000-0000-0000-0000-000000000000', v_mayor_id, 'authenticated', 'authenticated',
      'mayor@manticao.gov.ph', v_default_pw, NOW(),
      '{"provider":"email","providers":["email"]}',
      '{"username":"mayor","name":"Office of the Municipal Mayor","role":"MAYOR"}',
      NOW(), NOW()
    );
  END IF;

  INSERT INTO public.user_profiles (id, username, full_name, email, role, is_active)
  VALUES (
    (SELECT id FROM auth.users WHERE email = 'mayor@manticao.gov.ph'),
    'mayor', 'Office of the Municipal Mayor', 'mayor@manticao.gov.ph', 'MAYOR', TRUE
  ) ON CONFLICT (id) DO UPDATE SET role = 'MAYOR', username = 'mayor', is_active = TRUE;

  -- 4. Municipal Treasurer
  IF NOT EXISTS (SELECT 1 FROM auth.users WHERE email = 'treasurer@manticao.gov.ph') THEN
    INSERT INTO auth.users (
      instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
      raw_app_meta_data, raw_user_meta_data, created_at, updated_at
    ) VALUES (
      '00000000-0000-0000-0000-000000000000', v_treasurer_id, 'authenticated', 'authenticated',
      'treasurer@manticao.gov.ph', v_default_pw, NOW(),
      '{"provider":"email","providers":["email"]}',
      '{"username":"treasurer","name":"Municipal Treasury Office","role":"TREASURER"}',
      NOW(), NOW()
    );
  END IF;

  INSERT INTO public.user_profiles (id, username, full_name, email, role, is_active)
  VALUES (
    (SELECT id FROM auth.users WHERE email = 'treasurer@manticao.gov.ph'),
    'treasurer', 'Municipal Treasury Office', 'treasurer@manticao.gov.ph', 'TREASURER', TRUE
  ) ON CONFLICT (id) DO UPDATE SET role = 'TREASURER', username = 'treasurer', is_active = TRUE;

  -- 5. Field Market Collector / Inspector
  IF NOT EXISTS (SELECT 1 FROM auth.users WHERE email = 'collector@manticao.gov.ph') THEN
    INSERT INTO auth.users (
      instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
      raw_app_meta_data, raw_user_meta_data, created_at, updated_at
    ) VALUES (
      '00000000-0000-0000-0000-000000000000', v_collector_id, 'authenticated', 'authenticated',
      'collector@manticao.gov.ph', v_default_pw, NOW(),
      '{"provider":"email","providers":["email"]}',
      '{"username":"collector","name":"Field Market Collector & Inspector","role":"COLLECTOR_INSPECTOR"}',
      NOW(), NOW()
    );
  END IF;

  INSERT INTO public.user_profiles (id, username, full_name, email, role, is_active)
  VALUES (
    (SELECT id FROM auth.users WHERE email = 'collector@manticao.gov.ph'),
    'collector', 'Field Market Collector & Inspector', 'collector@manticao.gov.ph', 'COLLECTOR_INSPECTOR', TRUE
  ) ON CONFLICT (id) DO UPDATE SET role = 'COLLECTOR_INSPECTOR', username = 'collector', is_active = TRUE;

  -- 6. Active Vendor (Stakeholder)
  IF NOT EXISTS (SELECT 1 FROM auth.users WHERE email = 'vendor@manticao.market') THEN
    INSERT INTO auth.users (
      instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
      raw_app_meta_data, raw_user_meta_data, created_at, updated_at
    ) VALUES (
      '00000000-0000-0000-0000-000000000000', v_vendor_id, 'authenticated', 'authenticated',
      'vendor@manticao.market', v_default_pw, NOW(),
      '{"provider":"email","providers":["email"]}',
      '{"username":"vendor","name":"Maria Santos (Active Vendor)","role":"VENDOR"}',
      NOW(), NOW()
    );
  END IF;

  INSERT INTO public.user_profiles (id, username, full_name, email, role, is_active)
  VALUES (
    (SELECT id FROM auth.users WHERE email = 'vendor@manticao.market'),
    'vendor', 'Maria Santos (Active Vendor)', 'vendor@manticao.market', 'VENDOR', TRUE
  ) ON CONFLICT (id) DO UPDATE SET role = 'VENDOR', username = 'vendor', is_active = TRUE;

  -- 7. Prospective Applicant (Stakeholder)
  IF NOT EXISTS (SELECT 1 FROM auth.users WHERE email = 'applicant@manticao.market') THEN
    INSERT INTO auth.users (
      instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
      raw_app_meta_data, raw_user_meta_data, created_at, updated_at
    ) VALUES (
      '00000000-0000-0000-0000-000000000000', v_applicant_id, 'authenticated', 'authenticated',
      'applicant@manticao.market', v_default_pw, NOW(),
      '{"provider":"email","providers":["email"]}',
      '{"username":"applicant","name":"Juan Dela Cruz (Applicant)","role":"APPLICANT"}',
      NOW(), NOW()
    );
  END IF;

  INSERT INTO public.user_profiles (id, username, full_name, email, role, is_active)
  VALUES (
    (SELECT id FROM auth.users WHERE email = 'applicant@manticao.market'),
    'applicant', 'Juan Dela Cruz (Applicant)', 'applicant@manticao.market', 'APPLICANT', TRUE
  ) ON CONFLICT (id) DO UPDATE SET role = 'APPLICANT', username = 'applicant', is_active = TRUE;

END $$;

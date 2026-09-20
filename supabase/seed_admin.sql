-- ==============================================================================
-- Default Admin Account Seeding Script
-- Creates or updates the default system administrator account
-- ==============================================================================

DO $$
DECLARE
  admin_user_id uuid := '8387ef47-c07f-4c2e-a952-51c3dd2789b4';
BEGIN
  -- Check if admin user exists in auth.users
  IF NOT EXISTS (SELECT 1 FROM auth.users WHERE email = 'admin@manticao.market') THEN
    INSERT INTO auth.users (
      instance_id,
      id,
      aud,
      role,
      email,
      encrypted_password,
      email_confirmed_at,
      raw_app_meta_data,
      raw_user_meta_data,
      created_at,
      updated_at,
      confirmation_token,
      recovery_token,
      email_change_token_new,
      email_change_token_current,
      email_change,
      phone_change_token,
      phone_change,
      reauthentication_token,
      is_super_admin
    ) VALUES (
      '00000000-0000-0000-0000-000000000000',
      admin_user_id,
      'authenticated',
      'authenticated',
      'admin@manticao.market',
      extensions.crypt('AdminPassword123!', extensions.gen_salt('bf')),
      NOW(),
      '{"provider":"email","providers":["email"]}',
      '{"username":"admin","name":"System Administrator","role":"ADMIN"}',
      NOW(),
      NOW(),
      '',
      '',
      '',
      '',
      '',
      '',
      '',
      '',
      false
    );

    INSERT INTO auth.identities (
      id,
      user_id,
      identity_data,
      provider,
      provider_id,
      last_sign_in_at,
      created_at,
      updated_at
    ) VALUES (
      admin_user_id,
      admin_user_id,
      format('{"sub":"%s","email":"admin@manticao.market"}', admin_user_id)::jsonb,
      'email',
      admin_user_id::text,
      NOW(),
      NOW(),
      NOW()
    );
  END IF;

  -- Ensure profile exists and has role ADMIN
  INSERT INTO public.profiles (id, username, name, role, status)
  VALUES (
    (SELECT id FROM auth.users WHERE email = 'admin@manticao.market'),
    'admin',
    'System Administrator',
    'ADMIN',
    'ACTIVE'
  )
  ON CONFLICT (id) DO UPDATE SET
    role = 'ADMIN',
    username = 'admin',
    name = 'System Administrator',
    status = 'ACTIVE';
    
END $$;

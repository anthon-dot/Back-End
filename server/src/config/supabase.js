import { createClient } from '@supabase/supabase-js';
import { config } from './env.js';

// Authoritative server-side client (used by Express services to enforce business rules)
export const supabase = createClient(config.supabaseUrl, config.supabaseServiceRoleKey || config.supabaseAnonKey, {
  auth: {
    autoRefreshToken: false,
    persistSession: false
  }
});

// User-scoped client factory for operations constrained by caller's JWT
export function getSupabaseForUser(accessToken) {
  if (!accessToken) return supabase;
  return createClient(config.supabaseUrl, config.supabaseAnonKey, {
    global: {
      headers: {
        Authorization: `Bearer ${accessToken}`
      }
    },
    auth: {
      autoRefreshToken: false,
      persistSession: false
    }
  });
}

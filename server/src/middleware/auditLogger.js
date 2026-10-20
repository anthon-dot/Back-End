import { supabase } from '../config/supabase.js';

/**
 * Audit Logger Helper: Persists security, financial, and lifecycle state changes to audit_logs.
 */
export async function logAudit({ userId, role, action, entityName, entityId, details, ipAddress }) {
  try {
    await supabase.from('audit_logs').insert({
      user_id: userId || null,
      role: role || 'SYSTEM',
      action,
      entity_name: entityName,
      entity_id: String(entityId || ''),
      details: typeof details === 'object' ? details : { info: details },
      ip_address: ipAddress || null,
      created_at: new Date().toISOString()
    });
  } catch (err) {
    console.warn('[AuditLogger] Failed to write audit log entry:', err.message);
  }
}

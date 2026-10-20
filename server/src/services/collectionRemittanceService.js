import { supabase } from '../config/supabase.js';
import { PAYMENT_TYPES, ROLES } from '../config/constants.js';
import { recordPayment } from './paymentService.js';
import { logAudit } from '../middleware/auditLogger.js';

/**
 * Records a field micro-payment (e.g. daily ₱100 field collection) by an authorized Field Collector
 */
export async function recordFieldCollection({
  collectorId,
  collectorName,
  stakeholderId,
  billingId,
  amount = 100.00,
  stallNo,
  notes
}) {
  const paymentResult = await recordPayment({
    stakeholderId,
    billingId,
    amount,
    paymentType: PAYMENT_TYPES.FIELD_COLLECTION,
    paymentChannel: 'FIELD_COLLECTOR',
    cashierId: collectorId,
    cashierName: collectorName,
    notes: notes || `Field daily collection collected by ${collectorName}`
  });

  return paymentResult;
}

/**
 * Creates a collection remittance batch submitted by the Field Collector to Treasury
 */
export async function submitRemittanceBatch({ collectorId, collectorName, paymentIds }) {
  if (!Array.isArray(paymentIds) || paymentIds.length === 0) {
    throw new Error('At least one payment must be selected for remittance.');
  }

  // Fetch payments to aggregate
  const { data: payments, error } = await supabase
    .from('payments')
    .select('*, stakeholder:stakeholders(business_name, selected_stall_id, stall:stalls(stall_no))')
    .in('id', paymentIds)
    .eq('cashier_or_collector_id', collectorId)
    .is('remittance_id', null);

  if (error || !payments || payments.length === 0) {
    throw new Error('No unremitted payments found matching the selected IDs.');
  }

  const totalAmount = payments.reduce((sum, p) => sum + Number(p.amount), 0);
  const remittanceNo = `REMIT-${new Date().getFullYear()}-${Date.now().toString().slice(-6)}`;

  // Create remittance record
  const { data: remittance, error: remErr } = await supabase
    .from('collection_remittances')
    .insert({
      remittance_no: remittanceNo,
      collector_id: collectorId,
      collector_name: collectorName,
      total_amount: totalAmount,
      collection_count: payments.length,
      status: 'SUBMITTED'
    })
    .select()
    .single();

  if (remErr) throw remErr;

  // Insert remittance items and link payments
  const items = payments.map(p => ({
    remittance_id: remittance.id,
    payment_id: p.id,
    stall_no: p.stakeholder?.stall?.stall_no || 'N/A',
    vendor_name: p.stakeholder?.business_name || 'Vendor',
    amount: p.amount,
    collected_at: p.payment_date
  }));

  await supabase.from('collection_remittance_items').insert(items);
  await supabase.from('payments').update({ remittance_id: remittance.id }).in('id', paymentIds);

  await logAudit({
    userId: collectorId,
    role: ROLES.COLLECTOR_INSPECTOR,
    action: 'SUBMIT_REMITTANCE',
    entityName: 'collection_remittances',
    entityId: remittance.id,
    details: { remittanceNo, totalAmount, count: payments.length }
  });

  return remittance;
}

/**
 * Treasurer reconciles remittance batch.
 * CRITICAL RULE: Reconciling remittance does NOT create new payments (avoids double revenue counting).
 */
export async function reconcileRemittance(remittanceId, { treasurerId, approved, remarks }) {
  const newStatus = approved ? 'RECONCILED' : 'REJECTED';

  const { data: remittance, error } = await supabase
    .from('collection_remittances')
    .update({
      status: newStatus,
      treasurer_id: treasurerId,
      reconciled_at: new Date().toISOString(),
      remarks
    })
    .eq('id', Number(remittanceId))
    .select()
    .single();

  if (error) throw error;

  await logAudit({
    userId: treasurerId,
    role: ROLES.TREASURER,
    action: approved ? 'RECONCILE_REMITTANCE' : 'REJECT_REMITTANCE',
    entityName: 'collection_remittances',
    entityId: remittanceId,
    details: { remittanceNo: remittance.remittance_no, status: newStatus }
  });

  return remittance;
}

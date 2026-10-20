import { supabase } from '../config/supabase.js';
import { BILLING_STATUS, CONTRACT_STATUS, ROLES } from '../config/constants.js';
import { logAudit } from '../middleware/auditLogger.js';

/**
 * Runs monthly billing generation for all active, billable lease contracts.
 * Idempotent: Can be run multiple times safely without creating duplicate bills.
 */
export async function runMonthlyBillingGeneration({ targetYear, targetMonth } = {}) {
  const now = new Date();
  const year = targetYear || now.getFullYear();
  const month = targetMonth || (now.getMonth() + 1);

  const billingPeriod = `MONTHLY-${month}-${year}`;
  const dueDateStr = `${year}-${String(month).padStart(2, '0')}-05`; // Due on the 5th
  const todayStr = now.toISOString().split('T')[0];

  console.log(`[billingService] Starting recurring billing generation for period ${billingPeriod}...`);

  // 1. Fetch only ACTIVE contracts
  const { data: activeContracts, error: conErr } = await supabase
    .from('contracts')
    .select('*, occupant:occupants(*, stakeholder:stakeholders(*)), stall:stalls(*)')
    .eq('status', CONTRACT_STATUS.ACTIVE);

  if (conErr) throw conErr;
  if (!activeContracts || activeContracts.length === 0) {
    console.log('[billingService] No active contracts eligible for billing.');
    return { generated: 0, skipped: 0, period: billingPeriod };
  }

  let generatedCount = 0;
  let skippedCount = 0;

  for (const contract of activeContracts) {
    try {
      const occupant = contract.occupant;
      if (!occupant?.id) continue;

      const stakeholder = occupant.stakeholder;
      const rentAmount = Number(contract.monthly_rent || contract.stall?.monthly_rent || 1500.00);

      // Check for existing bill for this contract and billing period (Idempotency check)
      const { data: existingBill } = await supabase
        .from('billings')
        .select('id, billing_no, status, balance')
        .eq('contract_id', contract.id)
        .eq('billing_period', billingPeriod)
        .maybeSingle();

      if (existingBill) {
        skippedCount++;
        continue;
      }

      // Check available advance balance on occupant / stakeholder
      let curAdvance = Number(occupant.advance_balance ?? stakeholder?.advance_balance ?? 0);
      let paidAmount = 0;
      let balance = rentAmount;
      let status = BILLING_STATUS.UNPAID;

      const isPastDue = todayStr > dueDateStr;

      if (curAdvance <= 0) {
        paidAmount = 0;
        balance = rentAmount;
        status = isPastDue ? BILLING_STATUS.OVERDUE : BILLING_STATUS.UNPAID;
      } else if (curAdvance >= rentAmount) {
        // Full advance credit allocated
        paidAmount = rentAmount;
        balance = 0;
        status = BILLING_STATUS.PAID;
        curAdvance -= rentAmount;
      } else {
        // Partial advance credit allocated
        paidAmount = curAdvance;
        balance = rentAmount - curAdvance;
        status = isPastDue ? BILLING_STATUS.OVERDUE : BILLING_STATUS.PARTIALLY_PAID;
        curAdvance = 0;
      }

      const billingNo = `BILL-${Date.now().toString().slice(-6)}${Math.floor(Math.random() * 1000)}`;

      // Insert billing statement
      const { data: newBill, error: billInsertErr } = await supabase
        .from('billings')
        .insert({
          occupant_id: occupant.id,
          contract_id: contract.id,
          billing_no: billingNo,
          billing_period: billingPeriod,
          total_amount: rentAmount,
          paid_amount: paidAmount,
          balance: balance,
          due_date: dueDateStr,
          status: status,
          created_at: new Date().toISOString()
        })
        .select()
        .single();

      if (billInsertErr) {
        console.warn(`[billingService] Error inserting bill for contract ${contract.id}:`, billInsertErr.message);
        continue;
      }

      // Update remaining advance balance if credit was applied
      if (Number(occupant.advance_balance || 0) !== curAdvance) {
        await supabase
          .from('occupants')
          .update({ advance_balance: curAdvance })
          .eq('id', occupant.id);

        if (stakeholder?.id) {
          await supabase
            .from('stakeholders')
            .update({ advance_balance: curAdvance })
            .eq('id', stakeholder.id);
        }
      }

      generatedCount++;
    } catch (innerErr) {
      console.warn(`[billingService] Error processing contract #${contract.id}:`, innerErr);
    }
  }

  await logAudit({
    role: 'SYSTEM',
    action: 'GENERATE_MONTHLY_BILLING',
    entityName: 'billings',
    entityId: billingPeriod,
    details: { period: billingPeriod, generated: generatedCount, skipped: skippedCount }
  });

  return { generated: generatedCount, skipped: skippedCount, period: billingPeriod };
}

/**
 * Fetches billings with dynamic overdue calculation and relation enrichment
 */
export async function getBillings({ occupantId, status, search } = {}) {
  let query = supabase
    .from('billings')
    .select('*, occupant:occupants(*, stakeholder:stakeholders(*)), contract:contracts(*, stall:stalls(*))')
    .order('due_date', { ascending: false });

  if (occupantId) {
    query = query.eq('occupant_id', Number(occupantId));
  }
  if (status) {
    query = query.eq('status', status);
  }

  const { data: bills, error } = await query;
  if (error) throw error;

  const todayStr = new Date().toISOString().split('T')[0];

  // Dynamically evaluate overdue status based on current date
  return (bills || []).map(b => {
    let currentStatus = b.status;
    const balance = Number(b.balance || 0);

    if (balance <= 0) {
      currentStatus = BILLING_STATUS.PAID;
    } else if (b.due_date && todayStr > b.due_date) {
      currentStatus = BILLING_STATUS.OVERDUE;
    }

    return {
      ...b,
      status: currentStatus
    };
  });
}

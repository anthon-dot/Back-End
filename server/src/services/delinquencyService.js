import { supabase } from '../config/supabase.js';
import { BILLING_STATUS, STALL_STATUS, CONTRACT_STATUS, ROLES } from '../config/constants.js';
import { logAudit } from '../middleware/auditLogger.js';

/**
 * Scans all overdue accounts and generates 30-, 60-, and 90-day demand notices.
 * Prevents duplicate notices for the same billing statement and stage.
 */
export async function scanDelinquentAccounts() {
  const today = new Date();
  const todayStr = today.toISOString().split('T')[0];

  // Fetch all bills with positive balance that are past due
  const { data: overdueBills, error } = await supabase
    .from('billings')
    .select('*, occupant:occupants(*, stakeholder:stakeholders(*)), contract:contracts(*, stall:stalls(*))')
    .gt('balance', 0)
    .lt('due_date', todayStr);

  if (error) throw error;
  if (!overdueBills || overdueBills.length === 0) {
    return { scanned: 0, noticesGenerated: 0 };
  }

  let noticesCount = 0;

  for (const bill of overdueBills) {
    const dueDate = new Date(bill.due_date);
    const diffTime = Math.abs(today - dueDate);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    const stakeholder = bill.occupant?.stakeholder;
    if (!stakeholder) continue;

    // 1. At 30 days overdue: 1st Demand Notice
    if (diffDays >= 30 && diffDays < 60 && !bill.first_notice_sent_at) {
      await supabase.from('notifications').insert({
        stakeholder_id: stakeholder.id,
        user_id: stakeholder.user_id,
        target_role: ROLES.VENDOR,
        title: 'First Notice of Rental Overdue (30 Days)',
        message: `Your billing statement ${bill.billing_no} (${bill.billing_period}) is 30 days past due with an outstanding balance of ₱${Number(bill.balance).toLocaleString()}. Please settle your account with the Municipal Treasurer.`,
        notification_type: 'DELINQUENCY_NOTICE_1',
        related_entity_type: 'billings',
        related_entity_id: String(bill.id)
      });

      await supabase.from('billings').update({ first_notice_sent_at: today.toISOString() }).eq('id', bill.id);
      noticesCount++;
    }

    // 2. At 60 days overdue: 2nd Demand Notice
    else if (diffDays >= 60 && diffDays < 90 && !bill.second_notice_sent_at) {
      await supabase.from('notifications').insert({
        stakeholder_id: stakeholder.id,
        user_id: stakeholder.user_id,
        target_role: ROLES.VENDOR,
        title: 'Second Notice of Rental Arrears (60 Days)',
        message: `URGENT: Billing statement ${bill.billing_no} is now 60 days overdue. Outstanding balance: ₱${Number(bill.balance).toLocaleString()}. Account may be subject to operational suspension.`,
        notification_type: 'DELINQUENCY_NOTICE_2',
        related_entity_type: 'billings',
        related_entity_id: String(bill.id)
      });

      await supabase.from('billings').update({ second_notice_sent_at: today.toISOString() }).eq('id', bill.id);
      noticesCount++;
    }

    // 3. At 90 days overdue: Final Demand Notice & Flag for Mayor Review
    else if (diffDays >= 90 && !bill.final_notice_sent_at) {
      await supabase.from('notifications').insert({
        stakeholder_id: stakeholder.id,
        user_id: stakeholder.user_id,
        target_role: ROLES.VENDOR,
        title: 'FINAL DEMAND NOTICE: Lease Revocation Pending (90 Days)',
        message: `FINAL NOTICE: Billing statement ${bill.billing_no} is 90 days past due (Balance: ₱${Number(bill.balance).toLocaleString()}). Your case has been flagged for Mayoral lease revocation review.`,
        notification_type: 'DELINQUENCY_NOTICE_FINAL',
        related_entity_type: 'billings',
        related_entity_id: String(bill.id)
      });

      // Notify Mayor & Market Supervisor
      await supabase.from('notifications').insert({
        target_role: ROLES.MAYOR,
        title: 'Delinquency Revocation Review Required',
        message: `Vendor ${stakeholder.business_name} (Stall #${bill.contract?.stall?.stall_no || 'N/A'}) has reached 90 days overdue with arrears of ₱${Number(bill.balance).toLocaleString()}. Mayoral review required.`,
        notification_type: 'MAYOR_REVOCATION_FLAG',
        related_entity_type: 'contracts',
        related_entity_id: String(bill.contract_id)
      });

      await supabase.from('billings').update({ final_notice_sent_at: today.toISOString() }).eq('id', bill.id);
      noticesCount++;
    }
  }

  return {
    scanned: overdueBills.length,
    noticesGenerated: noticesCount
  };
}

/**
 * Mayor authorizes or rejects proposed lease revocation.
 * Preserves contract, payments, bills, and audit history; archives operational vendor record and releases stall.
 */
export async function mayorAuthorizeRevocation({ contractId, mayorId, mayorName, approved, reason }) {
  const { data: contract, error: conErr } = await supabase
    .from('contracts')
    .select('*, occupant:occupants(*, stakeholder:stakeholders(*)), stall:stalls(*)')
    .eq('id', Number(contractId))
    .single();

  if (conErr || !contract) throw new Error('Contract not found.');

  if (!approved) {
    // Mayor gave extension / rejected revocation
    await logAudit({
      userId: mayorId,
      role: ROLES.MAYOR,
      action: 'REJECT_LEASE_REVOCATION',
      entityName: 'contracts',
      entityId: contractId,
      details: { contractNo: contract.contract_no, reason: reason || 'Grace period extension granted by Mayor.' }
    });
    return { success: true, revoked: false, message: 'Lease revocation rejected by Mayor.' };
  }

  // Approved Revocation:
  // 1. Update contract status to REVOKED
  await supabase
    .from('contracts')
    .update({ status: CONTRACT_STATUS.REVOKED, updated_at: new Date().toISOString() })
    .eq('id', Number(contractId));

  // 2. Revoke digital occupancy permit
  await supabase
    .from('occupancy_permits')
    .update({
      status: 'REVOKED',
      revocation_reason: reason || 'Revoked due to 90+ days rental delinquency.',
      revoked_at: new Date().toISOString(),
      revoked_by: mayorId
    })
    .eq('contract_id', Number(contractId));

  // 3. Archive vendor operational status (preserving complete financial & legal history)
  await supabase
    .from('stakeholders')
    .update({
      is_archived: true,
      archived_on: new Date().toISOString().split('T')[0],
      archived_reason: reason || 'Mayoral lease revocation for non-payment of rent.',
      application_status: 'REVOKED'
    })
    .eq('id', contract.occupant.stakeholder_id);

  await supabase
    .from('occupants')
    .update({ is_archived: true, status: 'REVOKED' })
    .eq('id', contract.occupant_id);

  // 4. Release stall to AVAILABLE
  if (contract.stall_id) {
    await supabase
      .from('stalls')
      .update({
        status: STALL_STATUS.AVAILABLE,
        occupant_id: null,
        updated_at: new Date().toISOString()
      })
      .eq('id', contract.stall_id);
  }

  await logAudit({
    userId: mayorId,
    role: ROLES.MAYOR,
    action: 'AUTHORIZE_LEASE_REVOCATION',
    entityName: 'contracts',
    entityId: contractId,
    details: {
      contractNo: contract.contract_no,
      vendorName: contract.occupant?.stakeholder?.business_name,
      stallNo: contract.stall?.stall_no,
      reason
    }
  });

  return {
    success: true,
    revoked: true,
    message: `Contract ${contract.contract_no} officially revoked by Municipal Mayor. Stall #${contract.stall?.stall_no} released.`
  };
}

import {
  scanDelinquentAccounts,
  mayorAuthorizeRevocation
} from '../services/delinquencyService.js';
import { supabase } from '../config/supabase.js';

export async function runDelinquencyScan(req, res, next) {
  try {
    const result = await scanDelinquentAccounts();
    return res.json({
      success: true,
      message: 'Delinquency scan executed.',
      ...result
    });
  } catch (err) {
    next(err);
  }
}

export async function authorizeRevocation(req, res, next) {
  try {
    const { contractId, approved, reason } = req.body;
    const result = await mayorAuthorizeRevocation({
      contractId,
      mayorId: req.user.id,
      mayorName: req.user.profile?.full_name || 'Hon. Municipal Mayor',
      approved: Boolean(approved),
      reason
    });

    return res.json({ success: true, ...result });
  } catch (err) {
    next(err);
  }
}

export async function listFlaggedRevocations(req, res, next) {
  try {
    // Find active contracts with overdue bills that received final demand notices
    const { data: bills, error: billErr } = await supabase
      .from('billings')
      .select('*, contract:contracts(*, occupant:occupants(*, stakeholder:stakeholders(*)), stall:stalls(*))')
      .not('final_notice_sent_at', 'is', null)
      .gt('balance', 0)
      .order('due_date', { ascending: true });

    if (billErr) throw billErr;

    // Filter to active contracts only
    const flaggedList = (bills || [])
      .filter((b) => b.contract && b.contract.status === 'ACTIVE')
      .map((b) => ({
        billingId: b.id,
        billingNo: b.billing_no,
        contractId: b.contract.id,
        contractNo: b.contract.contract_no,
        vendorName: b.contract.occupant?.stakeholder?.business_name || `${b.contract.occupant?.stakeholder?.first_name} ${b.contract.occupant?.stakeholder?.last_name}`,
        stallNo: b.contract.stall?.stall_no,
        overdueBalance: b.balance,
        dueDate: b.due_date,
        finalNoticeSentAt: b.final_notice_sent_at
      }));

    return res.json({ success: true, flaggedContracts: flaggedList });
  } catch (err) {
    next(err);
  }
}

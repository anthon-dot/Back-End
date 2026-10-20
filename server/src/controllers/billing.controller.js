import { getBillings, runMonthlyBillingGeneration } from '../services/billingService.js';
import { ROLES } from '../config/constants.js';

export async function listBillings(req, res, next) {
  try {
    const { status, occupantId } = req.query;
    const bills = await getBillings({ status, occupantId });

    // Filter if vendor/applicant
    let filteredBills = bills;
    if (req.user.role === ROLES.VENDOR || req.user.role === ROLES.APPLICANT) {
      filteredBills = bills.filter(b => b.occupant?.stakeholder?.user_id === req.user.id);
    }

    return res.json({ success: true, billings: filteredBills });
  } catch (err) {
    next(err);
  }
}

export async function triggerMonthlyBilling(req, res, next) {
  try {
    const { targetYear, targetMonth } = req.body;
    const result = await runMonthlyBillingGeneration({ targetYear, targetMonth });
    return res.json({
      success: true,
      message: 'Monthly recurring billing run completed.',
      ...result
    });
  } catch (err) {
    next(err);
  }
}

import { recordPayment } from '../services/paymentService.js';
import { supabase } from '../config/supabase.js';
import { ROLES } from '../config/constants.js';

export async function createPayment(req, res, next) {
  try {
    const { stakeholderId, billingId, amount, paymentType, paymentChannel, referenceNo, notes } = req.body;
    const result = await recordPayment({
      stakeholderId,
      billingId,
      amount,
      paymentType,
      paymentChannel: paymentChannel || 'CASH',
      referenceNo,
      cashierId: req.user.id,
      cashierName: req.user.profile?.full_name || 'Municipal Treasurer',
      notes
    });

    return res.status(201).json({
      success: true,
      message: 'Payment recorded successfully and Official Receipt generated.',
      ...result
    });
  } catch (err) {
    next(err);
  }
}

export async function listPayments(req, res, next) {
  try {
    let query = supabase
      .from('payments')
      .select('*, stakeholder:stakeholders(*), billing:billings(*)')
      .order('payment_date', { ascending: false });

    // Vendors can only see their own payments
    if (req.user.role === ROLES.VENDOR || req.user.role === ROLES.APPLICANT) {
      query = query.eq('stakeholder.user_id', req.user.id);
    }

    const { data, error } = await query;
    if (error) throw error;
    return res.json({ success: true, payments: data || [] });
  } catch (err) {
    next(err);
  }
}

export async function getReceipt(req, res, next) {
  try {
    const { receiptNo } = req.params;
    const { data: receipt, error } = await supabase
      .from('payment_receipts')
      .select('*')
      .eq('receipt_no', receiptNo)
      .maybeSingle();

    if (error || !receipt) {
      return res.status(404).json({ success: false, error: 'Receipt not found.' });
    }

    return res.json({ success: true, receipt });
  } catch (err) {
    next(err);
  }
}

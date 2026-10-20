import {
  recordFieldCollection,
  submitRemittanceBatch,
  reconcileRemittance
} from '../services/collectionRemittanceService.js';
import { supabase } from '../config/supabase.js';

export async function createFieldCollection(req, res, next) {
  try {
    const { stakeholderId, billingId, amount, stallNo, notes } = req.body;
    const result = await recordFieldCollection({
      collectorId: req.user.id,
      collectorName: req.user.profile?.full_name || 'Field Collector',
      stakeholderId,
      billingId,
      amount: Number(amount || 100.00),
      stallNo,
      notes
    });

    return res.status(201).json({
      success: true,
      message: 'Field collection recorded successfully.',
      ...result
    });
  } catch (err) {
    next(err);
  }
}

export async function submitRemittance(req, res, next) {
  try {
    const { paymentIds } = req.body;
    const remittance = await submitRemittanceBatch({
      collectorId: req.user.id,
      collectorName: req.user.profile?.full_name || 'Field Collector',
      paymentIds
    });

    return res.status(201).json({
      success: true,
      message: 'Remittance batch submitted to Treasury for reconciliation.',
      remittance
    });
  } catch (err) {
    next(err);
  }
}

export async function reconcile(req, res, next) {
  try {
    const { id } = req.params;
    const { approved, remarks } = req.body;
    const remittance = await reconcileRemittance(id, {
      treasurerId: req.user.id,
      approved: Boolean(approved),
      remarks
    });

    return res.json({
      success: true,
      message: approved ? 'Remittance batch reconciled.' : 'Remittance batch rejected.',
      remittance
    });
  } catch (err) {
    next(err);
  }
}

export async function listRemittances(req, res, next) {
  try {
    const { data, error } = await supabase
      .from('collection_remittances')
      .select('*, items:collection_remittance_items(*)')
      .order('created_at', { ascending: false });

    if (error) throw error;
    return res.json({ success: true, remittances: data || [] });
  } catch (err) {
    next(err);
  }
}

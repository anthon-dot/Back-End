import { supabase } from '../config/supabase.js';
import { PAYMENT_TYPES, BILLING_STATUS, APPLICATION_STATUS, ROLES } from '../config/constants.js';
import { generateReceiptPDF } from '../utils/pdfGenerator.js';
import { logAudit } from '../middleware/auditLogger.js';

/**
 * Records an official payment (walk-in cash payment recorded by Treasurer or Field Collector)
 */
export async function recordPayment({
  stakeholderId,
  billingId,
  amount,
  paymentType,
  paymentChannel = 'CASH',
  referenceNo,
  cashierId,
  cashierName = 'Municipal Treasurer',
  notes
}) {
  const paymentAmount = Number(amount);
  if (isNaN(paymentAmount) || paymentAmount <= 0) {
    throw new Error('Valid positive payment amount is required.');
  }

  // 1. Fetch stakeholder
  const { data: stakeholder, error: shErr } = await supabase
    .from('stakeholders')
    .select('*, stall:stalls(*)')
    .eq('id', Number(stakeholderId))
    .single();

  if (shErr || !stakeholder) throw new Error('Stakeholder record not found.');

  const receiptNo = `OR-${new Date().getFullYear()}-${Date.now().toString().slice(-6)}${Math.floor(Math.random() * 100)}`;
  const paymentDate = new Date().toISOString();

  // 2. Insert Payment Record
  const { data: payment, error: pErr } = await supabase
    .from('payments')
    .insert({
      receipt_no: receiptNo,
      reference_no: referenceNo || null,
      billing_id: billingId ? Number(billingId) : null,
      stakeholder_id: Number(stakeholderId),
      amount: paymentAmount,
      payment_type: paymentType,
      payment_channel: paymentChannel,
      cashier_or_collector_id: cashierId || null,
      cashier_name: cashierName,
      notes,
      payment_date: paymentDate
    })
    .select()
    .single();

  if (pErr) throw pErr;

  // 3. Handle Payment Allocation based on Payment Type
  if (paymentType === PAYMENT_TYPES.ADVANCE_PAYMENT) {
    // Credit advance balance
    const updatedAdvance = Number(stakeholder.advance_balance || 0) + paymentAmount;
    await supabase
      .from('stakeholders')
      .update({
        advance_balance: updatedAdvance,
        advance_payment_paid: true,
        advance_payment_completed: true,
        advance_payment_amount: paymentAmount,
        advance_payment_date: paymentDate.split('T')[0],
        application_status: APPLICATION_STATUS.PAID_READY_FOR_CONTRACT
      })
      .eq('id', stakeholder.id);

    // Update occupant advance balance if exists
    await supabase
      .from('occupants')
      .update({ advance_balance: updatedAdvance })
      .eq('stakeholder_id', stakeholder.id);

    // Update stakeholder_applications
    await supabase
      .from('stakeholder_applications')
      .update({
        status: APPLICATION_STATUS.PAID_READY_FOR_CONTRACT,
        treasurer_receipt_no: receiptNo,
        treasurer_recorded_at: paymentDate,
        amount_paid: paymentAmount,
        current_holding_office: 'Market Supervisor'
      })
      .eq('stakeholder_id', stakeholder.id);
  } else if (billingId) {
    // Allocate to specific billing record
    const { data: bill } = await supabase
      .from('billings')
      .select('*')
      .eq('id', Number(billingId))
      .single();

    if (bill) {
      const newPaid = Number(bill.paid_amount || 0) + paymentAmount;
      const newBalance = Math.max(0, Number(bill.total_amount) - newPaid);
      const newStatus = newBalance <= 0 ? BILLING_STATUS.PAID : BILLING_STATUS.PARTIALLY_PAID;

      await supabase
        .from('billings')
        .update({
          paid_amount: newPaid,
          balance: newBalance,
          status: newStatus,
          updated_at: new Date().toISOString()
        })
        .eq('id', Number(billingId));
    }
  }

  // 4. Generate Official Receipt Record & PDF
  let pdfBuffer = null;
  try {
    pdfBuffer = await generateReceiptPDF({
      receiptNo,
      payerName: `${stakeholder.first_name} ${stakeholder.last_name}`,
      stallNo: stakeholder.stall?.stall_no || 'Assigned Stall',
      amount: paymentAmount,
      paymentType,
      paymentChannel,
      cashierName,
      paymentDate: new Date(paymentDate).toLocaleDateString()
    });

    const receiptPath = `receipts/${receiptNo}.pdf`;
    await supabase.storage.from('uploads').upload(receiptPath, pdfBuffer, {
      contentType: 'application/pdf',
      upsert: true
    });

    await supabase.from('payment_receipts').insert({
      receipt_no: receiptNo,
      payment_id: payment.id,
      payer_name: `${stakeholder.first_name} ${stakeholder.last_name}`,
      stall_no: stakeholder.stall?.stall_no || 'N/A',
      amount_paid: paymentAmount,
      payment_type: paymentType,
      issued_by: cashierId || null,
      pdf_url: receiptPath
    });
  } catch (receiptErr) {
    console.warn('[paymentService] Receipt PDF generation note:', receiptErr.message);
  }

  await logAudit({
    userId: cashierId,
    role: ROLES.TREASURER,
    action: 'RECORD_PAYMENT',
    entityName: 'payments',
    entityId: payment.id,
    details: { receiptNo, amount: paymentAmount, paymentType }
  });

  return {
    payment,
    receiptNo
  };
}

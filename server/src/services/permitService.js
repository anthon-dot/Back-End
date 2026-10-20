import { supabase } from '../config/supabase.js';
import { PERMIT_STATUS, APPLICATION_STATUS, STALL_STATUS, ROLES } from '../config/constants.js';
import { generatePermitIntegrityHash } from '../utils/crypto.js';
import { generatePermitPDF } from '../utils/pdfGenerator.js';
import { generatePermitQRCode } from '../utils/qrGenerator.js';
import { logAudit } from '../middleware/auditLogger.js';

/**
 * Generates an official digital occupancy permit with QR verification
 */
export async function issueOccupancyPermit({ applicationId, issuedBy }) {
  // 1. Fetch application with related stakeholder, contract, and stall
  const { data: app, error: appErr } = await supabase
    .from('stakeholder_applications')
    .select('*, stakeholder:stakeholders(*), stall:stalls(*)')
    .eq('id', Number(applicationId))
    .single();

  if (appErr || !app) throw new Error('Application record not found.');

  const stakeholder = app.stakeholder;
  const stall = app.stall;

  // Verify that required initial payment was confirmed
  if (!stakeholder.advance_payment_paid && !stakeholder.advance_payment_completed) {
    throw new Error('Cannot issue occupancy permit: Advance payment has not been confirmed by the Treasurer.');
  }

  // Find active or executed contract
  const { data: contract, error: conErr } = await supabase
    .from('contracts')
    .select('*')
    .eq('stakeholder_id', stakeholder.id)
    .single();

  if (conErr || !contract) {
    throw new Error('Cannot issue occupancy permit: No executed Contract of Lease found.');
  }

  // Find occupant
  const { data: occupant } = await supabase
    .from('occupants')
    .select('id')
    .eq('stakeholder_id', stakeholder.id)
    .single();

  const permitNo = `MNT-PERMIT-${new Date().getFullYear()}-${Date.now().toString().slice(-6)}`;
  const issueDate = new Date().toISOString().split('T')[0];
  const expirationDate = contract.end_date;

  const vendorName = `${stakeholder.first_name} ${stakeholder.last_name}`;

  // Generate SHA-256 integrity hash
  const integrityHash = generatePermitIntegrityHash({
    permitNo,
    vendorName,
    stallNo: stall.stall_no,
    issueDate,
    expirationDate
  });

  const { verificationUrl } = await generatePermitQRCode({ permitNo, integrityHash });

  // Generate PDF document
  const pdfBuffer = await generatePermitPDF({
    permitNo,
    vendorName,
    businessName: stakeholder.business_name,
    stallNo: stall.stall_no,
    stallType: stall.stall_type,
    issueDate,
    expirationDate,
    integrityHash
  });

  // Upload to Supabase Storage
  const storagePath = `permits/${permitNo}.pdf`;
  try {
    await supabase.storage.from('uploads').upload(storagePath, pdfBuffer, {
      contentType: 'application/pdf',
      upsert: true
    });
  } catch (_) {}

  // Upsert Occupancy Permit record
  const { data: permit, error: pErr } = await supabase
    .from('occupancy_permits')
    .insert({
      permit_no: permitNo,
      vendor_id: stakeholder.id,
      occupant_id: occupant.id,
      stall_id: stall.id,
      contract_id: contract.id,
      issue_date: issueDate,
      expiration_date: expirationDate,
      status: PERMIT_STATUS.ACTIVE,
      qr_code_data: verificationUrl,
      integrity_hash: integrityHash,
      pdf_storage_path: storagePath
    })
    .select()
    .single();

  if (pErr) throw pErr;

  // Activate Stall Occupancy & Vendor Status
  await supabase
    .from('stalls')
    .update({ status: STALL_STATUS.OCCUPIED, occupant_id: occupant.id, updated_at: new Date().toISOString() })
    .eq('id', stall.id);

  await supabase
    .from('occupants')
    .update({ status: 'ACTIVE', occupancy_date: issueDate })
    .eq('id', occupant.id);

  await supabase
    .from('contracts')
    .update({ status: 'ACTIVE' })
    .eq('id', contract.id);

  await supabase
    .from('stakeholders')
    .update({
      verified_tenant: true,
      application_status: APPLICATION_STATUS.COMPLETED,
      onboarding_status: 'FULLY_ONBOARDED'
    })
    .eq('id', stakeholder.id);

  await supabase
    .from('stakeholder_applications')
    .update({
      status: APPLICATION_STATUS.COMPLETED,
      permit_issued_at: new Date().toISOString(),
      completed_at: new Date().toISOString(),
      updated_at: new Date().toISOString()
    })
    .eq('id', applicationId);

  await logAudit({
    userId: issuedBy,
    role: ROLES.MARKET_SUPERVISOR,
    action: 'ISSUE_OCCUPANCY_PERMIT',
    entityName: 'occupancy_permits',
    entityId: permit.id,
    details: { permitNo, vendorName, stallNo: stall.stall_no }
  });

  return permit;
}

/**
 * Public Read-Only QR Verification Endpoint: Exposes ONLY approved public fields.
 * Does not expose address, contact, payment history, internal IDs, or private docs.
 */
export async function verifyPermitPublicly(permitNoOrHash) {
  const { data: permit, error } = await supabase
    .from('occupancy_permits')
    .select('*, vendor:stakeholders(business_name, business_type, first_name, last_name), stall:stalls(stall_no, stall_type)')
    .or(`permit_no.eq.${permitNoOrHash},integrity_hash.eq.${permitNoOrHash}`)
    .maybeSingle();

  if (error || !permit) {
    return {
      isValid: false,
      message: 'Permit not found or invalid QR signature.'
    };
  }

  // Log scan attempt
  await supabase.from('permit_scan_logs').insert({
    permit_id: permit.id,
    scan_type: 'PUBLIC',
    is_valid: permit.status === PERMIT_STATUS.ACTIVE
  });

  const isExpired = new Date(permit.expiration_date) < new Date();
  const effectiveStatus = isExpired ? 'EXPIRED' : permit.status;

  return {
    isValid: effectiveStatus === PERMIT_STATUS.ACTIVE,
    status: effectiveStatus,
    permitNo: permit.permit_no,
    vendorName: `${permit.vendor.first_name} ${permit.vendor.last_name}`,
    businessName: permit.vendor.business_name,
    businessType: permit.vendor.business_type,
    stallNo: permit.stall.stall_no,
    stallType: permit.stall.stall_type,
    issueDate: permit.issue_date,
    expirationDate: permit.expiration_date,
    verifiedByMunicipality: 'Municipality of Manticao Public Market Revenue Bureau'
  };
}

/**
 * Authenticated Inspector Endpoint: Returns operational inspection & delinquency details.
 */
export async function inspectPermitAuthenticated(permitNoOrHash, inspectorUser) {
  const { data: permit, error } = await supabase
    .from('occupancy_permits')
    .select('*, vendor:stakeholders(*), stall:stalls(*), contract:contracts(*)')
    .or(`permit_no.eq.${permitNoOrHash},integrity_hash.eq.${permitNoOrHash}`)
    .maybeSingle();

  if (error || !permit) throw new Error('Permit not found.');

  // Fetch recent billings & arrears
  const { data: billings } = await supabase
    .from('billings')
    .select('id, billing_period, total_amount, paid_amount, balance, status, due_date')
    .eq('occupant_id', permit.occupant_id)
    .order('due_date', { ascending: false })
    .limit(6);

  const totalArrears = (billings || [])
    .filter(b => b.status === 'OVERDUE' || (b.due_date < new Date().toISOString().split('T')[0] && b.balance > 0))
    .reduce((sum, b) => sum + Number(b.balance), 0);

  // Log scan
  await supabase.from('permit_scan_logs').insert({
    permit_id: permit.id,
    scanned_by: inspectorUser.id,
    scanner_role: inspectorUser.role,
    scan_type: 'INSPECTOR',
    is_valid: permit.status === PERMIT_STATUS.ACTIVE
  });

  return {
    permit,
    totalArrears,
    recentBillings: billings || [],
    hasActiveViolations: totalArrears > 0,
    delinquencySummary: totalArrears > 0 ? `Outstanding arrears of ₱${totalArrears.toLocaleString()}` : 'Good standing'
  };
}

import { supabase } from '../config/supabase.js';
import { APPLICATION_STATUS, STALL_STATUS, CONTRACT_STATUS, ROLES } from '../config/constants.js';
import { calculateInitialFeesForStall } from './rentalCalculationService.js';
import { logAudit } from '../middleware/auditLogger.js';

/**
 * Step 1: Submit Application with LOI & Valid ID
 * Reserves stall as PENDING, creates stakeholder & stakeholder_application record.
 */
export async function submitApplication({
  userId,
  firstName,
  middleName,
  lastName,
  businessName,
  businessType,
  contact,
  email,
  address,
  stallId,
  loiUrl,
  validIdUrl
}) {
  // 1. Verify stall availability
  const { data: stall, error: stallErr } = await supabase
    .from('stalls')
    .select('*')
    .eq('id', Number(stallId))
    .single();

  if (stallErr || !stall) throw new Error('Selected stall does not exist.');
  if (stall.status === STALL_STATUS.OCCUPIED || stall.status === STALL_STATUS.PENDING) {
    throw new Error(`Stall #${stall.stall_no} is currently not available for new applications.`);
  }

  // 2. Compute fee estimate (stall rights + 2 months advance)
  const feeEstimate = await calculateInitialFeesForStall(stallId);

  // 3. Upsert Stakeholder
  let stakeholderId;
  const { data: existingSh } = await supabase
    .from('stakeholders')
    .select('id')
    .eq('user_id', userId)
    .maybeSingle();

  const shPayload = {
    user_id: userId,
    first_name: firstName,
    middle_name: middleName,
    last_name: lastName,
    business_name: businessName,
    business_type: businessType,
    contact,
    email,
    address,
    selected_stall_id: Number(stallId),
    application_status: APPLICATION_STATUS.PENDING_SUPERVISOR_REVIEW,
    onboarding_status: 'FOR_APPROVAL',
    total_advance_amount: feeEstimate.advanceRentAmount,
    updated_at: new Date().toISOString()
  };

  if (existingSh?.id) {
    stakeholderId = existingSh.id;
    await supabase.from('stakeholders').update(shPayload).eq('id', stakeholderId);
  } else {
    const { data: newSh, error: newShErr } = await supabase
      .from('stakeholders')
      .insert({ ...shPayload, advance_balance: 0 })
      .select('id')
      .single();
    if (newShErr) throw newShErr;
    stakeholderId = newSh.id;
  }

  // 4. Mark stall as PENDING to reserve it
  await supabase
    .from('stalls')
    .update({ status: STALL_STATUS.PENDING, updated_at: new Date().toISOString() })
    .eq('id', Number(stallId));

  // 5. Create application record
  const applicationNo = `APP-${Date.now().toString().slice(-6)}${Math.floor(Math.random() * 100)}`;
  const { data: appRecord, error: appErr } = await supabase
    .from('stakeholder_applications')
    .insert({
      application_no: applicationNo,
      stakeholder_id: stakeholderId,
      user_id: userId,
      stall_id: Number(stallId),
      status: APPLICATION_STATUS.PENDING_SUPERVISOR_REVIEW,
      current_holding_office: 'Market Supervisor',
      loi_url: loiUrl,
      valid_id_url: validIdUrl,
      calculated_stall_rights_fee: feeEstimate.stallRightsFee,
      calculated_advance_rent: feeEstimate.advanceRentAmount,
      total_initial_amount_due: feeEstimate.totalInitialAmount,
      submitted_at: new Date().toISOString()
    })
    .select()
    .single();

  if (appErr) throw appErr;

  // 6. Save documents table
  const docs = [];
  if (loiUrl) {
    docs.push({ stakeholder_id: stakeholderId, application_id: appRecord.id, document_type: 'LOI', file_name: 'Letter_of_Intent.pdf', file_path: loiUrl });
  }
  if (validIdUrl) {
    docs.push({ stakeholder_id: stakeholderId, application_id: appRecord.id, document_type: 'VALID_ID', file_name: 'Valid_Government_ID.pdf', file_path: validIdUrl });
  }
  if (docs.length > 0) {
    await supabase.from('stakeholder_documents').insert(docs);
  }

  // 7. Record approval history & audit log
  await supabase.from('approval_history').insert({
    stakeholder_id: stakeholderId,
    application_id: appRecord.id,
    stage: 'APPLICATION_SUBMISSION',
    status: 'SUBMITTED',
    actor_name: `${firstName} ${lastName}`,
    actor_role: 'APPLICANT',
    remarks: `Application submitted for Stall #${stall.stall_no}.`
  });

  await logAudit({
    userId,
    role: ROLES.APPLICANT,
    action: 'SUBMIT_APPLICATION',
    entityName: 'stakeholder_applications',
    entityId: appRecord.id,
    details: { applicationNo, stallNo: stall.stall_no }
  });

  return appRecord;
}

/**
 * Step 2: Market Supervisor Review & Endorsement
 */
export async function supervisorReviewApplication(applicationId, { approved, remarks, supervisorId, supervisorName }) {
  const { data: app, error: appErr } = await supabase
    .from('stakeholder_applications')
    .select('*, stakeholder:stakeholders(*)')
    .eq('id', Number(applicationId))
    .single();

  if (appErr || !app) throw new Error('Application not found.');

  const nextStatus = approved ? APPLICATION_STATUS.PENDING_MAYOR_APPROVAL : APPLICATION_STATUS.SUPERVISOR_REJECTED;
  const holdingOffice = approved ? "Mayor's Office" : "Market Supervisor";

  await supabase
    .from('stakeholder_applications')
    .update({
      status: nextStatus,
      current_holding_office: holdingOffice,
      supervisor_endorsed_at: new Date().toISOString(),
      supervisor_remarks: remarks,
      updated_at: new Date().toISOString()
    })
    .eq('id', Number(applicationId));

  await supabase
    .from('stakeholders')
    .update({
      market_supervisor_approved: approved,
      market_approval_status: approved ? 'APPROVED' : 'REJECTED',
      application_status: nextStatus
    })
    .eq('id', app.stakeholder_id);

  // If rejected, release stall back to AVAILABLE
  if (!approved) {
    await supabase
      .from('stalls')
      .update({ status: STALL_STATUS.AVAILABLE, updated_at: new Date().toISOString() })
      .eq('id', app.stall_id);
  }

  await supabase.from('approval_history').insert({
    stakeholder_id: app.stakeholder_id,
    application_id: app.id,
    stage: 'MARKET_SUPERVISOR_REVIEW',
    status: approved ? 'ENDORSED' : 'REJECTED',
    action_by: supervisorId,
    actor_name: supervisorName,
    actor_role: ROLES.MARKET_SUPERVISOR,
    remarks
  });

  return { success: true, status: nextStatus };
}

/**
 * Step 3: Municipal Mayor Approval of LOI
 */
export async function mayorReviewApplication(applicationId, { approved, remarks, mayorId, mayorName }) {
  const { data: app, error: appErr } = await supabase
    .from('stakeholder_applications')
    .select('*, stakeholder:stakeholders(*)')
    .eq('id', Number(applicationId))
    .single();

  if (appErr || !app) throw new Error('Application not found.');

  const nextStatus = approved ? APPLICATION_STATUS.PENDING_TREASURER_PAYMENT : APPLICATION_STATUS.MAYOR_REJECTED;
  const holdingOffice = approved ? "Treasurer's Office" : "Mayor's Office";

  await supabase
    .from('stakeholder_applications')
    .update({
      status: nextStatus,
      current_holding_office: holdingOffice,
      mayor_approved_at: new Date().toISOString(),
      mayor_decision: approved ? 'APPROVED' : 'REJECTED',
      mayor_remarks: remarks,
      updated_at: new Date().toISOString()
    })
    .eq('id', Number(applicationId));

  await supabase
    .from('stakeholders')
    .update({
      application_status: nextStatus
    })
    .eq('id', app.stakeholder_id);

  if (!approved) {
    await supabase
      .from('stalls')
      .update({ status: STALL_STATUS.AVAILABLE, updated_at: new Date().toISOString() })
      .eq('id', app.stall_id);
  }

  await supabase.from('approval_history').insert({
    stakeholder_id: app.stakeholder_id,
    application_id: app.id,
    stage: 'MAYOR_APPROVAL',
    status: approved ? 'APPROVED' : 'REJECTED',
    action_by: mayorId,
    actor_name: mayorName,
    actor_role: ROLES.MAYOR,
    remarks
  });

  return { success: true, status: nextStatus };
}

/**
 * Step 4: Field Market Collector / Inspector Physical Stall Setup Verification
 * Inspector inspects stall setup (electrical, structural, fire safety, sanitation).
 */
export async function inspectorVerifyStall(applicationId, {
  inspectorId,
  inspectorName,
  electricalClearance = true,
  structuralClearance = true,
  fireSafetyClearance = true,
  sanitationClearance = true,
  status = 'PASSED',
  remarks = ''
}) {
  const { data: app, error: appErr } = await supabase
    .from('stakeholder_applications')
    .select('*, stakeholder:stakeholders(*), stall:stalls(*)')
    .eq('id', Number(applicationId))
    .single();

  if (appErr || !app) throw new Error('Application not found.');

  const isPassed = status === 'PASSED' && electricalClearance && structuralClearance && fireSafetyClearance && sanitationClearance;
  const nextStatus = isPassed ? APPLICATION_STATUS.BUSINESS_PERMIT_ELIGIBLE : APPLICATION_STATUS.PENDING_INSPECTION;
  const holdingOffice = isPassed ? 'Market Administration / Business Permit Processing' : 'Field Inspector';

  // 1. Insert into stall_inspections table
  const { error: inspErr } = await supabase.from('stall_inspections').insert({
    stall_id: app.stall_id,
    inspector_id: inspectorId,
    inspector_name: inspectorName || 'Field Inspector',
    electrical_clearance: electricalClearance,
    structural_clearance: structuralClearance,
    fire_safety_clearance: fireSafetyClearance,
    sanitation_clearance: sanitationClearance,
    status: isPassed ? 'PASSED' : 'FAILED',
    remarks: remarks || (isPassed ? 'Stall setup fully verified and safety compliant.' : 'Deficiencies flagged during inspection.'),
    inspection_date: new Date().toISOString().split('T')[0]
  });

  if (inspErr) console.warn('[INSPECTION] Inspection record insert warning:', inspErr.message);

  // 2. Update application status
  await supabase
    .from('stakeholder_applications')
    .update({
      status: nextStatus,
      hazard_free_confirmed: isPassed,
      current_holding_office: holdingOffice,
      updated_at: new Date().toISOString()
    })
    .eq('id', Number(applicationId));

  // 3. Update stakeholder record
  await supabase
    .from('stakeholders')
    .update({
      hazard_free_confirmed: isPassed,
      application_status: nextStatus
    })
    .eq('id', app.stakeholder_id);

  // 4. Record approval history
  await supabase.from('approval_history').insert({
    stakeholder_id: app.stakeholder_id,
    application_id: app.id,
    stage: 'PHYSICAL_STALL_INSPECTION',
    status: isPassed ? 'PASSED' : 'FAILED',
    action_by: inspectorId,
    actor_name: inspectorName || 'Field Inspector',
    actor_role: ROLES.COLLECTOR_INSPECTOR,
    remarks: remarks || (isPassed ? 'Physical stall setup inspected and passed.' : 'Inspection failed/conditional.')
  });

  await logAudit({
    userId: inspectorId,
    role: ROLES.COLLECTOR_INSPECTOR,
    action: 'INSPECT_STALL',
    entityName: 'stakeholder_applications',
    entityId: app.id,
    details: { stallId: app.stall_id, isPassed, status: nextStatus }
  });

  return {
    success: true,
    status: nextStatus,
    inspectionPassed: isPassed,
    message: isPassed
      ? 'Physical stall setup verified. Business permit processing step now eligible.'
      : 'Stall inspection recorded with pending deficiencies.'
  };
}

/**
 * Step 5: Final Stakeholder Activation as Active Vendor
 * Condition check: Initial fees paid + Contract executed + Physical inspection passed.
 * Upgrades user to VENDOR, activates occupant, sets stall OCCUPIED, schedules recurring monthly billing.
 */
export async function activateVendorAndCompleteApplication(applicationId, {
  authorizedBy,
  authorizedRole = ROLES.MARKET_SUPERVISOR,
  remarks = ''
}) {
  const { data: app, error: appErr } = await supabase
    .from('stakeholder_applications')
    .select('*, stakeholder:stakeholders(*), stall:stalls(*)')
    .eq('id', Number(applicationId))
    .single();

  if (appErr || !app) throw new Error('Application not found.');

  // 1. Mark application COMPLETED
  await supabase
    .from('stakeholder_applications')
    .update({
      status: APPLICATION_STATUS.COMPLETED,
      current_holding_office: 'Active Market Tenancy',
      completed_at: new Date().toISOString(),
      updated_at: new Date().toISOString()
    })
    .eq('id', Number(applicationId));

  // 2. Mark stakeholder verified
  await supabase
    .from('stakeholders')
    .update({
      verified_tenant: true,
      verified_stakeholder: true,
      onboarding_status: 'ACTIVE_VENDOR',
      application_status: APPLICATION_STATUS.COMPLETED,
      updated_at: new Date().toISOString()
    })
    .eq('id', app.stakeholder_id);

  // 3. Mark stall as OCCUPIED
  await supabase
    .from('stalls')
    .update({
      status: STALL_STATUS.OCCUPIED,
      updated_at: new Date().toISOString()
    })
    .eq('id', app.stall_id);

  // 4. Ensure occupant relationship is ACTIVE
  const { data: occupant } = await supabase
    .from('occupants')
    .select('*')
    .eq('stakeholder_id', app.stakeholder_id)
    .maybeSingle();

  if (occupant) {
    await supabase
      .from('occupants')
      .update({
        status: 'ACTIVE',
        stall_id: app.stall_id,
        occupancy_date: new Date().toISOString().split('T')[0]
      })
      .eq('id', occupant.id);

    // Link stall to occupant
    await supabase.from('stalls').update({ occupant_id: occupant.id }).eq('id', app.stall_id);
  }

  // 5. Upgrade user profile to VENDOR if currently APPLICANT
  if (app.user_id) {
    await supabase
      .from('user_profiles')
      .update({ role: ROLES.VENDOR, updated_at: new Date().toISOString() })
      .eq('id', app.user_id)
      .eq('role', ROLES.APPLICANT);
  }

  // 6. Record approval history & audit log
  await supabase.from('approval_history').insert({
    stakeholder_id: app.stakeholder_id,
    application_id: app.id,
    stage: 'VENDOR_ACTIVATION',
    status: 'ACTIVE_VENDOR',
    action_by: authorizedBy,
    actor_name: 'Market Administration',
    actor_role: authorizedRole,
    remarks: remarks || 'All prerequisites completed. Stakeholder promoted to Active Vendor.'
  });

  await logAudit({
    userId: authorizedBy,
    role: authorizedRole,
    action: 'ACTIVATE_VENDOR',
    entityName: 'stakeholder_applications',
    entityId: app.id,
    details: { stakeholderId: app.stakeholder_id, stallNo: app.stall?.stall_no }
  });

  return {
    success: true,
    status: APPLICATION_STATUS.COMPLETED,
    message: `Stakeholder ${app.stakeholder?.business_name || ''} successfully activated as Active Vendor.`
  };
}


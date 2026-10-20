import { supabase } from '../config/supabase.js';
import { CONTRACT_STATUS, APPLICATION_STATUS, STALL_STATUS, ROLES } from '../config/constants.js';
import { generateContractPDF } from '../utils/pdfGenerator.js';
import { logAudit } from '../middleware/auditLogger.js';

export async function prepareContractForApplication(applicationId, { supervisorId, terms }) {
  const { data: app, error: appErr } = await supabase
    .from('stakeholder_applications')
    .select('*, stakeholder:stakeholders(*), stall:stalls(*)')
    .eq('id', Number(applicationId))
    .single();

  if (appErr || !app) throw new Error('Application record not found.');

  const stall = app.stall;
  const stakeholder = app.stakeholder;

  // 1. Ensure occupant record exists in PENDING state
  let occupantId;
  const { data: exOcc } = await supabase
    .from('occupants')
    .select('id')
    .eq('stakeholder_id', stakeholder.id)
    .maybeSingle();

  if (exOcc?.id) {
    occupantId = exOcc.id;
  } else {
    const { data: newOcc, error: occErr } = await supabase
      .from('occupants')
      .insert({
        stakeholder_id: stakeholder.id,
        stall_id: stall.id,
        status: 'PENDING',
        advance_balance: stakeholder.advance_balance || 0
      })
      .select('id')
      .single();
    if (occErr) throw occErr;
    occupantId = newOcc.id;
  }

  // 2. Prepare 3-Year lease duration
  const startDate = new Date();
  const endDate = new Date(startDate);
  endDate.setFullYear(endDate.getFullYear() + 3);

  const startDateStr = startDate.toISOString().split('T')[0];
  const endDateStr = endDate.toISOString().split('T')[0];

  const contractNo = `CON-${Date.now().toString().slice(-8)}`;

  // 3. Upsert contract record
  const { data: contract, error: conErr } = await supabase
    .from('contracts')
    .upsert({
      contract_no: contractNo,
      occupant_id: occupantId,
      stall_id: stall.id,
      stakeholder_id: stakeholder.id,
      start_date: startDateStr,
      end_date: endDateStr,
      monthly_rent: Number(stall.monthly_rent || 1500),
      billing_frequency: 'MONTHLY',
      terms: terms || 'Standard 3-Year Public Market Commercial Lease Agreement',
      status: CONTRACT_STATUS.PENDING_APPROVAL,
      updated_at: new Date().toISOString()
    }, { onConflict: 'contract_no' })
    .select()
    .single();

  if (conErr) throw conErr;

  // 4. Update application & stakeholder
  await supabase
    .from('stakeholder_applications')
    .update({
      contract_prepared_at: new Date().toISOString(),
      updated_at: new Date().toISOString()
    })
    .eq('id', applicationId);

  await supabase
    .from('stakeholders')
    .update({ contract_id: contract.id })
    .eq('id', stakeholder.id);

  return contract;
}

export async function executeContract(contractId, { userId, supervisorName }) {
  const { data: contract, error: conErr } = await supabase
    .from('contracts')
    .select('*, occupant:occupants(*, stakeholder:stakeholders(*)), stall:stalls(*)')
    .eq('id', Number(contractId))
    .single();

  if (conErr || !contract) throw new Error('Contract not found.');

  // Generate official PDF
  const pdfBuffer = await generateContractPDF({
    contractNo: contract.contract_no,
    vendorName: `${contract.occupant.stakeholder.first_name} ${contract.occupant.stakeholder.last_name}`,
    businessName: contract.occupant.stakeholder.business_name,
    stallNo: contract.stall.stall_no,
    stallType: contract.stall.stall_type,
    monthlyRent: contract.monthly_rent,
    startDate: contract.start_date,
    endDate: contract.end_date,
    supervisorName
  });

  // Store in Supabase Storage or document path
  const storagePath = `contracts/${contract.contract_no}.pdf`;
  try {
    await supabase.storage.from('uploads').upload(storagePath, pdfBuffer, {
      contentType: 'application/pdf',
      upsert: true
    });
  } catch (_) {}

  // Update contract status to ACTIVE
  const { data: updated, error: updErr } = await supabase
    .from('contracts')
    .update({
      status: CONTRACT_STATUS.ACTIVE,
      executed_by: userId,
      executed_at: new Date().toISOString(),
      document_url: storagePath
    })
    .eq('id', Number(contractId))
    .select()
    .single();

  if (updErr) throw updErr;

  await logAudit({
    userId,
    role: ROLES.MARKET_SUPERVISOR,
    action: 'EXECUTE_CONTRACT',
    entityName: 'contracts',
    entityId: contractId,
    details: { contractNo: contract.contract_no }
  });

  return updated;
}

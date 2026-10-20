import {
  submitApplication,
  supervisorReviewApplication,
  mayorReviewApplication,
  inspectorVerifyStall,
  activateVendorAndCompleteApplication
} from '../services/applicationWorkflowService.js';
import { supabase } from '../config/supabase.js';
import { ROLES } from '../config/constants.js';

export async function createApplication(req, res, next) {
  try {
    const {
      firstName,
      middleName,
      lastName,
      businessName,
      businessType,
      contact,
      email,
      address,
      stallId,
      selectedStallId,
      loiUrl,
      validIdUrl
    } = req.body;

    let finalIdUrl = validIdUrl;
    let finalLoiUrl = loiUrl;

    if (req.files?.idFile?.[0]) {
      const file = req.files.idFile[0];
      const fileName = `id_${Date.now()}_${file.originalname.replace(/[^a-zA-Z0-9_.-]/g, '_')}`;
      const filePath = `documents/${fileName}`;
      try {
        await supabase.storage.from('uploads').upload(filePath, file.buffer, {
          contentType: file.mimetype,
          upsert: true
        });
        const { data: pubData } = supabase.storage.from('uploads').getPublicUrl(filePath);
        finalIdUrl = pubData?.publicUrl || filePath;
      } catch (upErr) {
        console.warn('[STORAGE] ID upload notice:', upErr.message);
      }
    }

    if (req.files?.letterFile?.[0]) {
      const file = req.files.letterFile[0];
      const fileName = `loi_${Date.now()}_${file.originalname.replace(/[^a-zA-Z0-9_.-]/g, '_')}`;
      const filePath = `documents/${fileName}`;
      try {
        await supabase.storage.from('uploads').upload(filePath, file.buffer, {
          contentType: file.mimetype,
          upsert: true
        });
        const { data: pubData } = supabase.storage.from('uploads').getPublicUrl(filePath);
        finalLoiUrl = pubData?.publicUrl || filePath;
      } catch (upErr) {
        console.warn('[STORAGE] LOI upload notice:', upErr.message);
      }
    }

    const targetStallId = stallId || selectedStallId;

    const result = await submitApplication({
      userId: req.user.id,
      firstName,
      middleName,
      lastName,
      businessName,
      businessType,
      contact,
      email: email || req.user.email,
      address,
      stallId: targetStallId,
      loiUrl: finalLoiUrl,
      validIdUrl: finalIdUrl
    });

    return res.status(201).json({
      success: true,
      id: result.id,
      user_id: result.user_id,
      stakeholderId: result.stakeholder_id,
      message: 'Application submitted successfully. Stall reserved as PENDING.',
      application: result,
      ...result
    });
  } catch (err) {
    next(err);
  }
}

export async function listApplications(req, res, next) {
  try {
    let query = supabase
      .from('stakeholder_applications')
      .select('*, stakeholder:stakeholders(*), stall:stalls(*)')
      .order('submitted_at', { ascending: false });

    // Applicants can only see their own applications
    if (req.user.role === ROLES.APPLICANT || req.user.role === ROLES.VENDOR) {
      query = query.eq('user_id', req.user.id);
    }

    const { data, error } = await query;
    if (error) throw error;

    return res.json({ success: true, applications: data || [] });
  } catch (err) {
    next(err);
  }
}

export async function getApplicationById(req, res, next) {
  try {
    const { id } = req.params;
    const { data, error } = await supabase
      .from('stakeholder_applications')
      .select('*, stakeholder:stakeholders(*), stall:stalls(*)')
      .eq('id', Number(id))
      .single();

    if (error || !data) {
      return res.status(404).json({ success: false, error: 'Application not found.' });
    }

    // Authorization check
    if (
      (req.user.role === ROLES.APPLICANT || req.user.role === ROLES.VENDOR) &&
      data.user_id !== req.user.id
    ) {
      return res.status(403).json({ success: false, error: 'Access denied.' });
    }

    return res.json({ success: true, application: data });
  } catch (err) {
    next(err);
  }
}

export async function supervisorReview(req, res, next) {
  try {
    const { id } = req.params;
    const { approved, remarks } = req.body;

    const result = await supervisorReviewApplication(id, {
      approved: Boolean(approved),
      remarks: remarks || '',
      supervisorId: req.user.id,
      supervisorName: req.user.profile?.full_name || 'Market Supervisor'
    });

    return res.json({
      success: true,
      message: approved ? 'Application endorsed to Mayor.' : 'Application rejected.',
      ...result
    });
  } catch (err) {
    next(err);
  }
}

export async function mayorReview(req, res, next) {
  try {
    const { id } = req.params;
    const { approved, remarks } = req.body;

    const result = await mayorReviewApplication(id, {
      approved: Boolean(approved),
      remarks: remarks || '',
      mayorId: req.user.id,
      mayorName: req.user.profile?.full_name || 'Hon. Municipal Mayor'
    });

    return res.json({
      success: true,
      message: approved ? 'LOI officially approved by Mayor. Ready for Treasurer fee payment.' : 'LOI rejected by Mayor.',
      ...result
    });
  } catch (err) {
    next(err);
  }
}

export async function inspectorVerify(req, res, next) {
  try {
    const { id } = req.params;
    const {
      electricalClearance,
      structuralClearance,
      fireSafetyClearance,
      sanitationClearance,
      status,
      remarks
    } = req.body;

    const result = await inspectorVerifyStall(id, {
      inspectorId: req.user.id,
      inspectorName: req.user.profile?.full_name || 'Field Inspector',
      electricalClearance: electricalClearance !== undefined ? Boolean(electricalClearance) : true,
      structuralClearance: structuralClearance !== undefined ? Boolean(structuralClearance) : true,
      fireSafetyClearance: fireSafetyClearance !== undefined ? Boolean(fireSafetyClearance) : true,
      sanitationClearance: sanitationClearance !== undefined ? Boolean(sanitationClearance) : true,
      status: status || 'PASSED',
      remarks: remarks || ''
    });

    return res.json({
      success: true,
      message: result.message,
      ...result
    });
  } catch (err) {
    next(err);
  }
}

export async function activateVendor(req, res, next) {
  try {
    const { id } = req.params;
    const { remarks } = req.body;

    const result = await activateVendorAndCompleteApplication(id, {
      authorizedBy: req.user.id,
      authorizedRole: req.user.role,
      remarks: remarks || ''
    });

    return res.json({
      success: true,
      message: result.message,
      ...result
    });
  } catch (err) {
    next(err);
  }
}


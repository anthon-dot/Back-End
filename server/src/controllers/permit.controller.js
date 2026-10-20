import {
  issueOccupancyPermit,
  verifyPermitPublicly,
  inspectPermitAuthenticated
} from '../services/permitService.js';
import { supabase } from '../config/supabase.js';
import { ROLES } from '../config/constants.js';

export async function issuePermit(req, res, next) {
  try {
    const { applicationId } = req.body;
    const permit = await issueOccupancyPermit({
      applicationId,
      issuedBy: req.user.id
    });

    return res.status(201).json({
      success: true,
      message: 'Occupancy permit officially issued with QR verification. Vendor is now an ACTIVE_VENDOR.',
      permit
    });
  } catch (err) {
    next(err);
  }
}

/**
 * Public Verification Endpoint for QR Code scans
 * Exposes ONLY non-sensitive verified fields
 */
export async function publicVerify(req, res, next) {
  try {
    const { identifier } = req.params;
    const verification = await verifyPermitPublicly(identifier);
    return res.json({ success: true, verification });
  } catch (err) {
    next(err);
  }
}

/**
 * Authenticated Inspector Scan Endpoint
 */
export async function inspectorScan(req, res, next) {
  try {
    const { identifier } = req.params;
    const inspectionData = await inspectPermitAuthenticated(identifier, req.user);
    return res.json({ success: true, inspection: inspectionData });
  } catch (err) {
    next(err);
  }
}

export async function listPermits(req, res, next) {
  try {
    let query = supabase
      .from('occupancy_permits')
      .select('*, vendor:stakeholders(*), stall:stalls(*), contract:contracts(*)')
      .order('issue_date', { ascending: false });

    // Vendors can only see their own permit
    if (req.user.role === ROLES.VENDOR) {
      query = query.eq('vendor.user_id', req.user.id);
    }

    const { data, error } = await query;
    if (error) throw error;
    return res.json({ success: true, permits: data || [] });
  } catch (err) {
    next(err);
  }
}

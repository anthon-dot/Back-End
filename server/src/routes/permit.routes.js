import { Router } from 'express';
import { authenticateToken } from '../middleware/auth.js';
import { requireRole } from '../middleware/rbac.js';
import { ROLES } from '../config/constants.js';
import {
  issuePermit,
  publicVerify,
  inspectorScan,
  listPermits
} from '../controllers/permit.controller.js';

const router = Router();

// PUBLIC QR Code verification endpoint (Unauthenticated)
router.get('/verify/:identifier', publicVerify);

// Authenticated inspector scan endpoint
router.get('/inspect/:identifier', authenticateToken, requireRole(ROLES.COLLECTOR_INSPECTOR, ROLES.MARKET_SUPERVISOR, ROLES.ADMIN), inspectorScan);

// Issue permit (Market Supervisor / Admin)
router.post('/issue', authenticateToken, requireRole(ROLES.MARKET_SUPERVISOR, ROLES.ADMIN), issuePermit);

// List permits
router.get('/', authenticateToken, listPermits);

export default router;

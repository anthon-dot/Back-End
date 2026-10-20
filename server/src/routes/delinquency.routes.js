import { Router } from 'express';
import { authenticateToken } from '../middleware/auth.js';
import { requireRole } from '../middleware/rbac.js';
import { ROLES } from '../config/constants.js';
import {
  runDelinquencyScan,
  authorizeRevocation,
  listFlaggedRevocations
} from '../controllers/delinquency.controller.js';

const router = Router();

router.get('/flagged', authenticateToken, requireRole(ROLES.MAYOR, ROLES.MARKET_SUPERVISOR, ROLES.ADMIN), listFlaggedRevocations);
router.post('/scan', authenticateToken, requireRole(ROLES.MARKET_SUPERVISOR, ROLES.TREASURER), runDelinquencyScan);
router.post('/mayor-revocation', authenticateToken, requireRole(ROLES.MAYOR), authorizeRevocation);

export default router;

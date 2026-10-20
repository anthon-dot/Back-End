import { Router } from 'express';
import { authenticateToken } from '../middleware/auth.js';
import { requireRole } from '../middleware/rbac.js';
import { ROLES } from '../config/constants.js';
import { listBillings, triggerMonthlyBilling } from '../controllers/billing.controller.js';

const router = Router();

router.get('/', authenticateToken, listBillings);
router.post('/generate', authenticateToken, requireRole(ROLES.MARKET_SUPERVISOR, ROLES.TREASURER), triggerMonthlyBilling);

export default router;

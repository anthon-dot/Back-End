import { Router } from 'express';
import { authenticateToken } from '../middleware/auth.js';
import { requireRole } from '../middleware/rbac.js';
import { ROLES } from '../config/constants.js';
import {
  listStalls,
  getStall,
  updateMapPosition,
  listSections,
  listRates
} from '../controllers/stall.controller.js';

const router = Router();

// Publicly readable endpoints (stalls, sections, rates)
router.get('/', listStalls);
router.get('/sections', listSections);
router.get('/rates', listRates);
router.get('/:id', getStall);

// Visual Map position updates (Market Supervisor & Admin only)
router.put('/:id/map-position', authenticateToken, requireRole(ROLES.MARKET_SUPERVISOR), updateMapPosition);

export default router;

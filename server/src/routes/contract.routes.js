import { Router } from 'express';
import { authenticateToken } from '../middleware/auth.js';
import { requireRole } from '../middleware/rbac.js';
import { ROLES } from '../config/constants.js';
import {
  prepareContract,
  execute,
  listContracts,
  getContractById
} from '../controllers/contract.controller.js';

const router = Router();

router.get('/', authenticateToken, listContracts);
router.get('/:id', authenticateToken, getContractById);

// Contract preparation & execution restricted to Market Supervisor
router.post('/prepare', authenticateToken, requireRole(ROLES.MARKET_SUPERVISOR), prepareContract);
router.post('/:id/execute', authenticateToken, requireRole(ROLES.MARKET_SUPERVISOR), execute);

export default router;

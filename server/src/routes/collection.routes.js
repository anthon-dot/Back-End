import { Router } from 'express';
import { authenticateToken } from '../middleware/auth.js';
import { requireRole } from '../middleware/rbac.js';
import { ROLES } from '../config/constants.js';
import {
  createFieldCollection,
  submitRemittance,
  reconcile,
  listRemittances
} from '../controllers/collection.controller.js';

const router = Router();

router.get('/remittances', authenticateToken, requireRole(ROLES.TREASURER, ROLES.COLLECTOR_INSPECTOR), listRemittances);

// Field Collector records daily collections
router.post('/field', authenticateToken, requireRole(ROLES.COLLECTOR_INSPECTOR), createFieldCollection);

// Field Collector submits remittance batch
router.post('/remittances', authenticateToken, requireRole(ROLES.COLLECTOR_INSPECTOR), submitRemittance);

// Treasurer reconciles remittance batch
router.put('/remittances/:id/reconcile', authenticateToken, requireRole(ROLES.TREASURER), reconcile);

export default router;

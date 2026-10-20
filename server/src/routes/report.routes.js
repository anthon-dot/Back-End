import { Router } from 'express';
import { authenticateToken } from '../middleware/auth.js';
import { requireStaff } from '../middleware/rbac.js';
import { getDashboardReports } from '../controllers/report.controller.js';

const router = Router();

router.get('/dashboard', authenticateToken, requireStaff, getDashboardReports);

export default router;

import { Router } from 'express';
import multer from 'multer';
import { authenticateToken } from '../middleware/auth.js';
import { requireRole } from '../middleware/rbac.js';
import { ROLES } from '../config/constants.js';
import {
  createApplication,
  listApplications,
  getApplicationById,
  supervisorReview,
  mayorReview,
  inspectorVerify,
  activateVendor
} from '../controllers/application.controller.js';

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 15 * 1024 * 1024 }
});

const router = Router();

// Submission endpoint (supports both multipart/form-data and JSON payloads)
router.post(
  '/',
  authenticateToken,
  upload.fields([
    { name: 'idFile', maxCount: 1 },
    { name: 'letterFile', maxCount: 1 }
  ]),
  createApplication
);

router.get('/', authenticateToken, listApplications);
router.get('/:id', authenticateToken, getApplicationById);

// Workflow review & validation endpoints
router.put('/:id/supervisor-review', authenticateToken, requireRole(ROLES.MARKET_SUPERVISOR, ROLES.ADMIN), supervisorReview);
router.put('/:id/mayor-review', authenticateToken, requireRole(ROLES.MAYOR, ROLES.ADMIN), mayorReview);
router.put('/:id/inspector-verify', authenticateToken, requireRole(ROLES.COLLECTOR_INSPECTOR, ROLES.MARKET_SUPERVISOR, ROLES.ADMIN), inspectorVerify);
router.put('/:id/activate-vendor', authenticateToken, requireRole(ROLES.MARKET_SUPERVISOR, ROLES.ADMIN), activateVendor);

export default router;

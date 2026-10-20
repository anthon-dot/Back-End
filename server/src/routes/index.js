import { Router } from 'express';
import authRoutes from './auth.routes.js';
import applicationRoutes from './application.routes.js';
import stallRoutes from './stall.routes.js';
import contractRoutes from './contract.routes.js';
import permitRoutes from './permit.routes.js';
import billingRoutes from './billing.routes.js';
import paymentRoutes from './payment.routes.js';
import collectionRoutes from './collection.routes.js';
import delinquencyRoutes from './delinquency.routes.js';
import reportRoutes from './report.routes.js';

const router = Router();

router.use('/auth', authRoutes);
router.use('/applications', applicationRoutes);
router.use('/stalls', stallRoutes);
router.use('/contracts', contractRoutes);
router.use('/permits', permitRoutes);
router.use('/billings', billingRoutes);
router.use('/payments', paymentRoutes);
router.use('/collections', collectionRoutes);
router.use('/delinquency', delinquencyRoutes);
router.use('/reports', reportRoutes);

// Health check endpoint
router.get('/health', (req, res) => {
  res.json({
    status: 'online',
    service: 'Manticao Public Market Rental Management API (Node.js/Express)',
    version: '2.0.0',
    timestamp: new Date().toISOString()
  });
});

export default router;

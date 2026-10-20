import { Router } from 'express';
import { authenticateToken } from '../middleware/auth.js';
import { getMe, updateProfile, register, login } from '../controllers/auth.controller.js';

const router = Router();

// Public auth endpoints
router.post('/register', register);
router.post('/login', login);

// Authenticated session & profile endpoints
router.get('/me', authenticateToken, getMe);
router.put('/profile', authenticateToken, updateProfile);

export default router;

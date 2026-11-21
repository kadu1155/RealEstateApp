import { Router } from 'express';
import { authService } from './auth.service';
import { requireAuth, AuthenticatedRequest } from './auth.middleware';
import { sendSuccess, sendError } from '../../common/response';
import { query } from '../../database/db';

export const authRouter = Router();

// 1. POST /api/v1/auth/send-otp
authRouter.post('/send-otp', async (req, res) => {
  try {
    const { mobile } = req.body;
    if (!mobile) {
      return sendError(res, 'Mobile number is required', 400);
    }
    const result = await authService.sendOtp(mobile);
    return sendSuccess(res, result, result.message);
  } catch (err: any) {
    return sendError(res, err.message, 400);
  }
});

// 2. POST /api/v1/auth/verify-otp
authRouter.post('/verify-otp', async (req, res) => {
  try {
    const { mobile, otp, role, fullName } = req.body;
    if (!mobile || !otp) {
      return sendError(res, 'Mobile and OTP are required', 400);
    }
    const result = await authService.verifyOtp(mobile, otp, role, fullName);
    return sendSuccess(res, result, 'Authentication successful');
  } catch (err: any) {
    return sendError(res, err.message, 401);
  }
});

// 3. POST /api/v1/auth/refresh-token
authRouter.post('/refresh-token', async (req, res) => {
  try {
    const { refreshToken } = req.body;
    if (!refreshToken) {
      return sendError(res, 'Refresh token is required', 400);
    }
    const result = await authService.refreshAccessToken(refreshToken);
    return sendSuccess(res, result, 'Token refreshed');
  } catch (err: any) {
    return sendError(res, err.message, 401);
  }
});

// 4. GET /api/v1/auth/me (Protected)
authRouter.get('/me', requireAuth, async (req: AuthenticatedRequest, res) => {
  try {
    const userRes = await query('SELECT * FROM users WHERE id = $1', [req.user!.userId]);
    if (userRes.rows.length === 0) {
      return sendError(res, 'User not found', 404);
    }
    const user = userRes.rows[0];

    let businessProfile = null;
    if (user.role === 'BROKER' || user.role === 'BUILDER') {
      const bpRes = await query('SELECT * FROM business_profiles WHERE user_id = $1', [user.id]);
      businessProfile = bpRes.rows[0] || null;
    }

    return sendSuccess(res, { user, businessProfile }, 'Current profile retrieved');
  } catch (err: any) {
    return sendError(res, err.message, 500);
  }
});

// 5. POST /api/v1/auth/update-role (Protected)
authRouter.post('/update-role', requireAuth, async (req: AuthenticatedRequest, res) => {
  try {
    const { role, businessDetails } = req.body;
    if (!role) {
      return sendError(res, 'Role is required', 400);
    }
    const updatedUser = await authService.updateUserRole(req.user!.userId, role, businessDetails);
    return sendSuccess(res, updatedUser, `Role successfully updated to ${role}`);
  } catch (err: any) {
    return sendError(res, err.message, 400);
  }
});

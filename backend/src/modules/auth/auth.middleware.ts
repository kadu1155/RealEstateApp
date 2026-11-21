import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { config } from '../../common/config';
import { TokenPayload } from './auth.service';
import { sendError } from '../../common/response';

export interface AuthenticatedRequest extends Request {
  user?: TokenPayload;
}

// Validates Bearer Access JWT
export function requireAuth(req: AuthenticatedRequest, res: Response, next: NextFunction) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return sendError(res, 'Authentication required. Missing Bearer token.', 401);
  }

  const token = authHeader.split(' ')[1];
  try {
    const decoded = jwt.verify(token, config.jwtAccessSecret) as TokenPayload;
    req.user = decoded;
    next();
  } catch (err: any) {
    return sendError(res, 'Invalid or expired access token. Please refresh token.', 401);
  }
}

// Enforces Role-Based Access Control (RBAC)
export function requireRole(...allowedRoles: string[]) {
  return (req: AuthenticatedRequest, res: Response, next: NextFunction) => {
    if (!req.user) {
      return sendError(res, 'Authentication required.', 401);
    }
    if (!allowedRoles.includes(req.user.role) && req.user.role !== 'ADMIN') {
      return sendError(
        res,
        `Forbidden: Role '${req.user.role}' does not have permission to access this resource. Required: ${allowedRoles.join(', ')}`,
        403
      );
    }
    next();
  };
}

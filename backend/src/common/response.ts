import { Response } from 'express';

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  meta?: {
    page?: number;
    limit?: number;
    total?: number;
  };
}

export function sendSuccess<T>(res: Response, data: T, message?: string, meta?: any, statusCode = 200) {
  return res.status(statusCode).json({
    success: true,
    data,
    message,
    meta
  });
}

export function sendError(res: Response, message: string, statusCode = 400, errors?: any) {
  return res.status(statusCode).json({
    success: false,
    message,
    errors
  });
}

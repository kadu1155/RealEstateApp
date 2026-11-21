import { Router } from 'express';
import { reelService } from './reel.service';
import { requireAuth, AuthenticatedRequest } from '../auth/auth.middleware';
import { sendSuccess, sendError } from '../../common/response';

export const reelRouter = Router();

// 1. GET /api/v1/reels/feed
reelRouter.get('/feed', async (req, res) => {
  try {
    const city = (req.query.city as string) || 'Pune';
    const page = parseInt(req.query.page as string || '1', 10);
    const limit = parseInt(req.query.limit as string || '20', 10);

    const reels = await reelService.getFeed({ city, page, limit });
    return sendSuccess(res, reels, 'Reels feed fetched successfully', {
      page,
      limit,
      count: reels.length
    });
  } catch (err: any) {
    return sendError(res, err.message, 500);
  }
});

// 2. GET /api/v1/reels/:id
reelRouter.get('/:id', async (req, res) => {
  try {
    const reel = await reelService.getReelById(req.params.id);
    return sendSuccess(res, reel, 'Reel details retrieved');
  } catch (err: any) {
    return sendError(res, err.message, 404);
  }
});

// 3. POST /api/v1/reels (Protected)
reelRouter.post('/', requireAuth, async (req: AuthenticatedRequest, res) => {
  try {
    const reel = await reelService.createReel({
      ...req.body,
      userId: req.user!.userId
    });
    return sendSuccess(res, reel, 'Real estate reel uploaded successfully', null, 201);
  } catch (err: any) {
    return sendError(res, err.message, 400);
  }
});

// 4. POST /api/v1/reels/:id/like (Protected)
reelRouter.post('/:id/like', requireAuth, async (req: AuthenticatedRequest, res) => {
  try {
    const result = await reelService.toggleLike(req.user!.userId, req.params.id);
    return sendSuccess(res, result, result.liked ? 'Reel liked' : 'Reel unliked');
  } catch (err: any) {
    return sendError(res, err.message, 500);
  }
});

// 5. POST /api/v1/reels/:id/view
reelRouter.post('/:id/view', async (req, res) => {
  try {
    const { userId, watchTimeSeconds } = req.body;
    await reelService.recordView(req.params.id, userId, watchTimeSeconds);
    return sendSuccess(res, { recorded: true }, 'View recorded');
  } catch (err: any) {
    return sendError(res, err.message, 500);
  }
});

// 6. POST /api/v1/reels/:id/enquiry-click
reelRouter.post('/:id/enquiry-click', async (req, res) => {
  try {
    await reelService.recordEnquiryClick(req.params.id);
    return sendSuccess(res, { recorded: true }, 'Enquiry click recorded');
  } catch (err: any) {
    return sendError(res, err.message, 500);
  }
});

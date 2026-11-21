import { Router } from 'express';
import { propertyService, PropertyFilterParams } from './property.service';
import { requireAuth, AuthenticatedRequest } from '../auth/auth.middleware';
import { sendSuccess, sendError } from '../../common/response';

export const propertyRouter = Router();

// 1. GET /api/v1/properties
propertyRouter.get('/', async (req, res) => {
  try {
    const filters: PropertyFilterParams = {
      city: req.query.city as string,
      locality: req.query.locality as string,
      category: req.query.category as string,
      listingType: req.query.listingType as string,
      minPrice: req.query.minPrice ? parseFloat(req.query.minPrice as string) : undefined,
      maxPrice: req.query.maxPrice ? parseFloat(req.query.maxPrice as string) : undefined,
      bhk: req.query.bhk ? parseInt(req.query.bhk as string, 10) : undefined,
      furnishing: req.query.furnishing as string,
      possession: req.query.possession as string,
      verifiedOnly: req.query.verifiedOnly === 'true',
      sortBy: req.query.sortBy as any,
      limit: req.query.limit ? parseInt(req.query.limit as string, 10) : 30,
      offset: req.query.offset ? parseInt(req.query.offset as string, 10) : 0
    };

    const properties = await propertyService.listProperties(filters);
    return sendSuccess(res, properties, 'Properties retrieved successfully');
  } catch (err: any) {
    return sendError(res, err.message, 500);
  }
});

// 2. GET /api/v1/properties/localities/popular
propertyRouter.get('/localities/popular', async (req, res) => {
  try {
    const city = (req.query.city as string) || 'Pune';
    const localities = await propertyService.getPopularLocalities(city);
    return sendSuccess(res, localities, 'Popular localities retrieved');
  } catch (err: any) {
    return sendError(res, err.message, 500);
  }
});

// 3. GET /api/v1/properties/:id
propertyRouter.get('/:id', async (req, res) => {
  try {
    const property = await propertyService.getPropertyById(req.params.id);
    return sendSuccess(res, property, 'Property details retrieved');
  } catch (err: any) {
    return sendError(res, err.message, 404);
  }
});

// 4. POST /api/v1/properties (Protected)
propertyRouter.post('/', requireAuth, async (req: AuthenticatedRequest, res) => {
  try {
    const property = await propertyService.createProperty({
      ...req.body,
      userId: req.user!.userId
    });
    return sendSuccess(res, property, 'Property listing published successfully', null, 201);
  } catch (err: any) {
    return sendError(res, err.message, 400);
  }
});

// 5. POST /api/v1/properties/:id/save (Protected Bookmark Toggle)
propertyRouter.post('/:id/save', requireAuth, async (req: AuthenticatedRequest, res) => {
  try {
    const isSaved = await propertyService.toggleSaveProperty(req.user!.userId, req.params.id);
    return sendSuccess(res, { isSaved }, isSaved ? 'Property bookmarked' : 'Property removed from saved');
  } catch (err: any) {
    return sendError(res, err.message, 500);
  }
});

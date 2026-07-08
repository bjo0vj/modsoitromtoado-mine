import 'dotenv/config.js';
import { sendError } from '../utils/response.js';

const authMiddleware = (req, res, next) => {
    const apiKey = req.header('X-API-Key');
    
    if (!apiKey) {
        return sendError(res, 401, 'API Key is missing');
    }

    if (apiKey !== process.env.API_KEY) {
        return sendError(res, 403, 'Invalid API Key');
    }

    next();
};

export default authMiddleware;

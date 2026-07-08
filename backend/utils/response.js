export const sendSuccess = (res, data = {}, message = 'Success') => {
    return res.status(200).json({
        success: true,
        message,
        data
    });
};

export const sendError = (res, statusCode = 500, message = 'Internal Server Error', error = null) => {
    console.error(`[ERROR] ${message}`, error || '');
    return res.status(statusCode).json({
        success: false,
        message,
        error: error ? error.toString() : null
    });
};


/**
 * Central Error Handler Middleware
 */
export function errorHandler(err, req, res, next) {
  const statusCode = err.statusCode || err.status || 500;
  const message = err.message || 'Internal Server Error';

  console.error(`[API Error] [${req.method}] ${req.originalUrl} - Status: ${statusCode}`, err);

  res.status(statusCode).json({
    success: false,
    error: message,
    code: err.code || 'SERVER_ERROR',
    timestamp: new Date().toISOString()
  });
}

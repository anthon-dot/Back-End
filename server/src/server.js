import app from './app.js';
import { config } from './config/env.js';
import { startBackgroundJobs } from './jobs/jobRunner.js';

const PORT = config.port || 5000;

const server = app.listen(PORT, () => {
  console.log(`================================================================`);
  console.log(` Manticao Rental Management System - Node.js Express API Server `);
  console.log(` Environment : ${config.nodeEnv}`);
  console.log(` Listening on: http://localhost:${PORT}`);
  console.log(` Health check: http://localhost:${PORT}/api/health`);
  console.log(`================================================================`);

  // Start background schedulers
  startBackgroundJobs();
});

// Graceful shutdown handling
process.on('SIGTERM', () => {
  console.log('[Server] SIGTERM received. Closing HTTP server gracefully...');
  server.close(() => {
    console.log('[Server] HTTP server closed.');
    process.exit(0);
  });
});

process.on('SIGINT', () => {
  console.log('[Server] SIGINT received. Shutting down...');
  server.close(() => {
    console.log('[Server] HTTP server closed.');
    process.exit(0);
  });
});

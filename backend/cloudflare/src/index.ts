// Main Cloudflare Worker Entry Point (Hono + Queues)

import { Hono } from 'hono';
import { cors } from 'hono/cors';
import { logger } from 'hono/logger';
import { Env } from './types/index.js';
import { attributionRouter } from './routes/attribution.js';
import { webhooksRouter } from './routes/webhooks.js';
import { handleSubscriptionQueue } from './queue/consumer.js';

const app = new Hono<{ Bindings: Env }>();

// Global Middlewares
app.use('*', logger());
app.use('*', cors({
  origin: '*',
  allowMethods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowHeaders: ['Content-Type', 'Authorization', 'x-revenuecat-secret', 'x-adapty-secret', 'adapty-auth-token'],
}));

// Root endpoint
app.get('/', (c) => {
  return c.json({
    name: 'koko-backend',
    description: 'Cloudflare Edge Backend for KMP Starter Kit',
    version: '1.0.0',
    endpoints: {
      health: '/health',
      attributionSync: 'POST /api/v1/attribution/sync',
      revenuecatWebhook: 'POST /api/v1/webhooks/revenuecat',
      adaptyWebhook: 'POST /api/v1/webhooks/adapty',
    },
  });
});

// Health check endpoint
app.get('/health', (c) => {
  return c.json({
    status: 'ok',
    service: 'koko-backend',
    timestamp: Date.now(),
    environment: c.env.ENVIRONMENT || 'production',
  });
});

// Mount Routes
app.route('/api/v1/attribution', attributionRouter);
app.route('/api/v1/webhooks', webhooksRouter);

// 404 Handler
app.notFound((c) => {
  return c.json({ error: 'Route not found' }, 404);
});

// Global Error Handler
app.onError((err, c) => {
  console.error('[Global Error]', err);
  return c.json(
    {
      error: 'Internal Server Error',
      message: err.message,
    },
    500
  );
});

// Cloudflare Worker export with Fetch and Queue handlers
export default {
  fetch: app.fetch,
  queue: handleSubscriptionQueue,
};

// Webhook routes for RevenueCat and Adapty

import { Hono } from 'hono';
import { Env, SubscriptionQueueMessage, WebhookLogRecord } from '../types/index.js';
import { processSingleSubscriptionMessage } from '../queue/consumer.js';

export const webhooksRouter = new Hono<{ Bindings: Env }>();

/**
 * Validates bearer or plain token from Authorization or custom header
 */
function isAuthorized(expectedSecret: string | undefined, authHeader: string | undefined): boolean {
  if (!expectedSecret || expectedSecret.trim() === '') {
    // If no secret configured in environment, allow or warn in non-prod
    return true;
  }
  if (!authHeader) {
    return false;
  }

  const cleanHeader = authHeader.replace(/^Bearer\s+/i, '').trim();
  return cleanHeader === expectedSecret.trim();
}

/**
 * POST /api/v1/webhooks/revenuecat
 */
webhooksRouter.post('/revenuecat', async (c) => {
  const env = c.env;
  const authHeader = c.req.header('authorization') || c.req.header('x-revenuecat-secret');

  // Verify authentication
  if (!isAuthorized(env.WEBHOOK_SECRET_REVENUECAT, authHeader)) {
    return c.json({ error: 'Unauthorized: Invalid RevenueCat webhook secret' }, 401);
  }

  let body: Record<string, unknown>;
  try {
    body = await c.req.json();
  } catch (e) {
    return c.json({ error: 'Invalid JSON payload' }, 400);
  }

  const event = (body?.event || {}) as Record<string, unknown>;
  const eventType = (event.type as string) || 'UNKNOWN';
  const appUserId = (event.app_user_id as string) || (event.original_app_user_id as string) || '';
  const transactionId = (event.transaction_id as string) || (event.id as string) || crypto.randomUUID();
  const originalTransactionId = (event.original_transaction_id as string) || undefined;
  const productId = (event.product_id as string) || '';
  const price = typeof event.price_in_purchased_currency === 'number'
    ? event.price_in_purchased_currency
    : typeof event.price === 'number'
      ? event.price
      : undefined;
  const currency = (event.currency as string) || 'USD';
  const eventTimestamp = typeof event.event_timestamp_ms === 'number'
    ? event.event_timestamp_ms
    : typeof event.purchased_at_ms === 'number'
      ? event.purchased_at_ms
      : Date.now();
  const isTrial = event.period_type === 'TRIAL';

  const webhookLogId = crypto.randomUUID();
  const now = Date.now();

  try {
    // Audit log: insert initial RECEIVED record into D1
    await env.DB.prepare(
      `INSERT INTO webhooks_log (id, provider, event_type, payload, status, error, created_at)
       VALUES (?, 'revenuecat', ?, ?, 'RECEIVED', NULL, ?)`
    ).bind(webhookLogId, eventType, JSON.stringify(body), now).run();

    // Enqueue message into Cloudflare Queue
    const queueMessage: SubscriptionQueueMessage = {
      id: webhookLogId,
      provider: 'revenuecat',
      eventType,
      appUserId,
      transactionId,
      originalTransactionId,
      productId,
      price,
      currency,
      eventTimestamp,
      isTrial,
      rawPayload: body,
    };

    if (env.SUBS_QUEUE) {
      await env.SUBS_QUEUE.send(queueMessage);
      await env.DB.prepare(
        `UPDATE webhooks_log SET status = 'QUEUED' WHERE id = ?`
      ).bind(webhookLogId).run();
      return c.json({ ok: true, logId: webhookLogId, status: 'queued' });
    } else {
      c.executionCtx.waitUntil(processSingleSubscriptionMessage(queueMessage, env));
      await env.DB.prepare(
        `UPDATE webhooks_log SET status = 'PROCESSING_DIRECT' WHERE id = ?`
      ).bind(webhookLogId).run();
      return c.json({ ok: true, logId: webhookLogId, status: 'dispatched_direct' });
    }
  } catch (err) {
    const errorMsg = err instanceof Error ? err.message : String(err);
    console.error('[RevenueCat Webhook Error]', errorMsg);

    // Attempt to update status to FAILED
    try {
      await env.DB.prepare(
        `UPDATE webhooks_log SET status = 'FAILED', error = ? WHERE id = ?`
      ).bind(errorMsg, webhookLogId).run();
    } catch {
      // Ignored if DB is down
    }

    return c.json({ error: 'Internal server error processing webhook', details: errorMsg }, 500);
  }
});

/**
 * POST /api/v1/webhooks/adapty
 */
webhooksRouter.post('/adapty', async (c) => {
  const env = c.env;
  const authHeader = c.req.header('authorization') || c.req.header('adapty-auth-token') || c.req.header('x-adapty-secret');

  // Verify authentication
  if (!isAuthorized(env.WEBHOOK_SECRET_ADAPTY, authHeader)) {
    return c.json({ error: 'Unauthorized: Invalid Adapty webhook secret' }, 401);
  }

  let body: Record<string, unknown>;
  try {
    body = await c.req.json();
  } catch (e) {
    return c.json({ error: 'Invalid JSON payload' }, 400);
  }

  // Handle Adapty test/ping event
  if (body.adapty_check_event === true) {
    return c.json({ ok: true, message: 'Adapty webhook verification success' });
  }

  const eventType = (body.event_type as string) || 'UNKNOWN';
  const appUserId = (body.customer_user_id as string) || (body.profile_id as string) || '';
  const transactionId = (body.transaction_id as string) || (body.id as string) || crypto.randomUUID();
  const originalTransactionId = (body.original_transaction_id as string) || undefined;
  const productId = (body.vendor_product_id as string) || '';
  const price = typeof body.price_usd === 'number'
    ? body.price_usd
    : typeof body.price === 'number'
      ? body.price
      : undefined;
  const currency = (body.currency as string) || 'USD';
  const eventTimestamp = body.event_datetime
    ? new Date(body.event_datetime as string).getTime()
    : Date.now();
  const isTrial = body.is_in_trial === true;

  const webhookLogId = crypto.randomUUID();
  const now = Date.now();

  try {
    // Audit log: insert initial RECEIVED record into D1
    await env.DB.prepare(
      `INSERT INTO webhooks_log (id, provider, event_type, payload, status, error, created_at)
       VALUES (?, 'adapty', ?, ?, 'RECEIVED', NULL, ?)`
    ).bind(webhookLogId, eventType, JSON.stringify(body), now).run();

    // Enqueue message into Cloudflare Queue
    const queueMessage: SubscriptionQueueMessage = {
      id: webhookLogId,
      provider: 'adapty',
      eventType,
      appUserId,
      transactionId,
      originalTransactionId,
      productId,
      price,
      currency,
      eventTimestamp,
      isTrial,
      rawPayload: body,
    };

    if (env.SUBS_QUEUE) {
      await env.SUBS_QUEUE.send(queueMessage);
      await env.DB.prepare(
        `UPDATE webhooks_log SET status = 'QUEUED' WHERE id = ?`
      ).bind(webhookLogId).run();
      return c.json({ ok: true, logId: webhookLogId, status: 'queued' });
    } else {
      c.executionCtx.waitUntil(processSingleSubscriptionMessage(queueMessage, env));
      await env.DB.prepare(
        `UPDATE webhooks_log SET status = 'PROCESSING_DIRECT' WHERE id = ?`
      ).bind(webhookLogId).run();
      return c.json({ ok: true, logId: webhookLogId, status: 'dispatched_direct' });
    }
  } catch (err) {
    const errorMsg = err instanceof Error ? err.message : String(err);
    console.error('[Adapty Webhook Error]', errorMsg);

    try {
      await env.DB.prepare(
        `UPDATE webhooks_log SET status = 'FAILED', error = ? WHERE id = ?`
      ).bind(errorMsg, webhookLogId).run();
    } catch {
      // Ignored if DB is down
    }

    return c.json({ error: 'Internal server error processing webhook', details: errorMsg }, 500);
  }
});

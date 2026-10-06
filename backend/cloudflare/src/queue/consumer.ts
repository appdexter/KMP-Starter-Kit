// Cloudflare Queue Consumer for Subscription and Conversion Events

import { ConversionStatus, Env, SubscriptionQueueMessage, UserRecord } from '../types/index.js';
import { sendMetaConversionEvent } from '../services/metaCapi.js';
import { sendGoogleConversionEvent } from '../services/googleAds.js';

/**
 * Maps incoming provider webhook event types to standard marketing conversion actions.
 */
function classifyEvent(
  provider: 'revenuecat' | 'adapty',
  eventType: string
): {
  action: 'SEND_PURCHASE' | 'SEND_REFUND' | 'SKIP_CLIENT_HANDLED' | 'IGNORE';
  metaEventName: string;
  googleEventName: string;
} {
  const normalized = eventType.toUpperCase();

  // RevenueCat classification
  if (provider === 'revenuecat') {
    switch (normalized) {
      case 'RENEWAL':
      case 'NON_RENEWING_PURCHASE':
      case 'PRODUCT_CHANGE':
        return { action: 'SEND_PURCHASE', metaEventName: 'Purchase', googleEventName: 'purchase' };
      case 'REFUND':
      case 'REVOCATION':
        return { action: 'SEND_REFUND', metaEventName: 'Refund', googleEventName: 'refund' };
      case 'INITIAL_PURCHASE':
        // Client app typically handles initial purchase event directly via SDK
        return { action: 'SKIP_CLIENT_HANDLED', metaEventName: 'Purchase', googleEventName: 'purchase' };
      default:
        return { action: 'IGNORE', metaEventName: '', googleEventName: '' };
    }
  }

  // Adapty classification
  if (provider === 'adapty') {
    switch (normalized) {
      case 'SUBSCRIPTION_RENEWED':
      case 'RENEWAL':
        return { action: 'SEND_PURCHASE', metaEventName: 'Purchase', googleEventName: 'purchase' };
      case 'SUBSCRIPTION_REFUNDED':
      case 'REFUND':
        return { action: 'SEND_REFUND', metaEventName: 'Refund', googleEventName: 'refund' };
      case 'INITIAL_PURCHASE':
      case 'SUBSCRIPTION_STARTED':
        return { action: 'SKIP_CLIENT_HANDLED', metaEventName: 'Purchase', googleEventName: 'purchase' };
      default:
        return { action: 'IGNORE', metaEventName: '', googleEventName: '' };
    }
  }

  return { action: 'IGNORE', metaEventName: '', googleEventName: '' };
}

/**
 * Processes a single subscription message (works for both Cloudflare Queue and Direct invocation)
 */
export async function processSingleSubscriptionMessage(
  payload: SubscriptionQueueMessage,
  env: Env
): Promise<void>;
export async function processSingleSubscriptionMessage(
  env: Env,
  payload: SubscriptionQueueMessage
): Promise<void>;
export async function processSingleSubscriptionMessage(
  arg1: SubscriptionQueueMessage | Env,
  arg2: Env | SubscriptionQueueMessage
): Promise<void> {
  let payload: SubscriptionQueueMessage;
  let env: Env;

  if ('DB' in arg1) {
    env = arg1 as Env;
    payload = arg2 as SubscriptionQueueMessage;
  } else {
    payload = arg1 as SubscriptionQueueMessage;
    env = arg2 as Env;
  }

  const { action, metaEventName, googleEventName } = classifyEvent(
    payload.provider,
    payload.eventType
  );

  // Check for ignored events (e.g. CANCELLATION, TEST, EXPIRATION without financial change)
  if (action === 'IGNORE') {
    try {
      await env.DB.prepare(
        `UPDATE webhooks_log SET status = 'IGNORED' WHERE id = ?`
      ).bind(payload.id).run();
    } catch {
      // Ignored if DB is unavailable
    }
    return;
  }

  // Fetch user attribution parameters from D1
  const userStmt = env.DB.prepare('SELECT * FROM users WHERE app_user_id = ? LIMIT 1');
  const user = (await userStmt.bind(payload.appUserId).first()) as UserRecord | null;

  const dedupeKey = `${payload.provider}_${payload.transactionId}_${payload.eventType.toLowerCase()}`;

  // Handle client-handled initial purchase: record for audit trail without sending duplicate CAPI / Google Ads
  if (action === 'SKIP_CLIENT_HANDLED') {
    const checkExisting = await env.DB.prepare(
      'SELECT id FROM conversions WHERE event_id = ? LIMIT 1'
    ).bind(dedupeKey).first();

    if (!checkExisting) {
      const conversionId = crypto.randomUUID();
      await env.DB.prepare(
        `INSERT INTO conversions (id, user_id, event_name, value, currency, event_id, status, error, created_at)
         VALUES (?, ?, ?, ?, ?, ?, 'SKIPPED_CLIENT_HANDLED', 'Client-side handled initial purchase (audit only)', ?)`
      ).bind(
        conversionId,
        payload.appUserId,
        metaEventName,
        payload.price ?? null,
        payload.currency ?? 'USD',
        dedupeKey,
        Date.now()
      ).run();
    }

    try {
      await env.DB.prepare(
        `UPDATE webhooks_log SET status = 'PROCESSED' WHERE id = ?`
      ).bind(payload.id).run();
    } catch {
      // Ignored
    }

    return;
  }

  // Check deduplication in D1 conversions table: if already SENT, skip
  const existingConversion = await env.DB.prepare(
    'SELECT id, status FROM conversions WHERE event_id = ? LIMIT 1'
  ).bind(dedupeKey).first<{ id: string; status: string }>();

  if (existingConversion && existingConversion.status === 'SENT') {
    // Event already processed and sent previously
    try {
      await env.DB.prepare(
        `UPDATE webhooks_log SET status = 'PROCESSED' WHERE id = ?`
      ).bind(payload.id).run();
    } catch {
      // Ignored
    }
    return;
  }

  // Dispatch to Meta Conversions API (CAPI) and Google Ads / GA4 MP in parallel
  const [metaSettled, googleSettled] = await Promise.allSettled([
    sendMetaConversionEvent(env, {
      eventName: metaEventName,
      appUserId: payload.appUserId,
      userRecord: user,
      transactionId: payload.transactionId,
      productId: payload.productId,
      value: payload.price,
      currency: payload.currency,
      eventTimestamp: payload.eventTimestamp,
      actionSource: 'app',
    }),
    sendGoogleConversionEvent(env, {
      eventName: googleEventName,
      appUserId: payload.appUserId,
      userRecord: user,
      transactionId: payload.transactionId,
      productId: payload.productId,
      value: payload.price,
      currency: payload.currency,
      eventTimestamp: payload.eventTimestamp,
    }),
  ]);

  const metaResult = metaSettled.status === 'fulfilled'
    ? metaSettled.value
    : { success: false, error: metaSettled.reason instanceof Error ? metaSettled.reason.message : String(metaSettled.reason) };

  const googleResult = googleSettled.status === 'fulfilled'
    ? googleSettled.value
    : { success: false, error: googleSettled.reason instanceof Error ? googleSettled.reason.message : String(googleSettled.reason) };

  // Determine overall status
  const hasErrors = !metaResult.success && !googleResult.success;
  const combinedError = [
    !metaResult.success ? `Meta: ${metaResult.error}` : null,
    !googleResult.success ? `Google: ${googleResult.error}` : null,
  ].filter(Boolean).join('; ');

  const conversionId = existingConversion?.id || crypto.randomUUID();
  const status: ConversionStatus = hasErrors ? 'FAILED' : 'SENT';

  if (existingConversion) {
    await env.DB.prepare(
      `UPDATE conversions SET status = ?, error = ?, created_at = ? WHERE id = ?`
    ).bind(status, combinedError || null, Date.now(), existingConversion.id).run();
  } else {
    await env.DB.prepare(
      `INSERT INTO conversions (id, user_id, event_name, value, currency, event_id, status, error, created_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).bind(
      conversionId,
      payload.appUserId,
      metaEventName,
      payload.price ?? null,
      payload.currency ?? 'USD',
      dedupeKey,
      status,
      combinedError || null,
      Date.now()
    ).run();
  }

  // Update webhook log audit status
  try {
    await env.DB.prepare(
      `UPDATE webhooks_log SET status = ?, error = ? WHERE id = ?`
    ).bind(hasErrors ? 'FAILED' : 'PROCESSED', combinedError || null, payload.id).run();
  } catch {
    // Ignored if DB is down
  }
}

/**
 * Cloudflare Queue Consumer Handler
 */
export async function handleSubscriptionQueue(
  batch: MessageBatch<SubscriptionQueueMessage>,
  env: Env,
  ctx?: ExecutionContext
): Promise<void> {
  for (const message of batch.messages) {
    const payload = message.body;

    try {
      await processSingleSubscriptionMessage(payload, env);
      message.ack();
    } catch (error) {
      const errorMsg = error instanceof Error ? error.message : String(error);
      console.error(`[Queue Consumer] Error processing message ${message.id}:`, errorMsg);
    }
  }
}

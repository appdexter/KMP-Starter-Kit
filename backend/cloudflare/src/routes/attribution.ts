// Attribution route for KMP Mobile Client

import { Hono } from 'hono';
import { Env, UserRecord } from '../types/index.js';

export const attributionRouter = new Hono<{ Bindings: Env }>();

interface AttributionSyncBody {
  app_user_id?: string;
  user_id?: string; // alias for app_user_id
  fbclid?: string | null;
  gclid?: string | null;
  fbp?: string | null;
  fbc?: string | null;
  ip_address?: string | null;
  user_agent?: string | null;
}

/**
 * POST /api/v1/attribution/sync
 * Receives advertising and attribution identifiers from KMP mobile/web client.
 */
attributionRouter.post('/sync', async (c) => {
  let body: AttributionSyncBody;
  try {
    body = await c.req.json();
  } catch {
    return c.json({ error: 'Invalid JSON request body' }, 400);
  }

  const appUserId = (body.app_user_id || body.user_id)?.trim();
  if (!appUserId) {
    return c.json({ error: 'Missing required field: app_user_id' }, 400);
  }

  // Extract IP and User Agent fallbacks from Cloudflare request headers
  const cfIp = c.req.header('cf-connecting-ip') || c.req.header('x-forwarded-for') || null;
  const cfUserAgent = c.req.header('user-agent') || null;

  const fbclid = body.fbclid?.trim() || null;
  const gclid = body.gclid?.trim() || null;
  const fbp = body.fbp?.trim() || null;
  const fbc = body.fbc?.trim() || null;
  const ipAddress = body.ip_address?.trim() || cfIp;
  const userAgent = body.user_agent?.trim() || cfUserAgent;

  const now = Date.now();
  const env = c.env;

  try {
    const existingUser = (await env.DB.prepare(
      'SELECT id FROM users WHERE app_user_id = ? LIMIT 1'
    ).bind(appUserId).first()) as { id: string } | null;

    if (existingUser) {
      // Upsert: keep previous values if new values are null/undefined
      await env.DB.prepare(
        `UPDATE users
         SET fbclid = COALESCE(?, fbclid),
             gclid = COALESCE(?, gclid),
             fbp = COALESCE(?, fbp),
             fbc = COALESCE(?, fbc),
             ip_address = COALESCE(?, ip_address),
             user_agent = COALESCE(?, user_agent),
             updated_at = ?
         WHERE app_user_id = ?`
      ).bind(
        fbclid,
        gclid,
        fbp,
        fbc,
        ipAddress,
        userAgent,
        now,
        appUserId
      ).run();
    } else {
      const newId = crypto.randomUUID();
      await env.DB.prepare(
        `INSERT INTO users (id, app_user_id, fbclid, gclid, fbp, fbc, ip_address, user_agent, created_at, updated_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
      ).bind(
        newId,
        appUserId,
        fbclid,
        gclid,
        fbp,
        fbc,
        ipAddress,
        userAgent,
        now,
        now
      ).run();
    }

    return c.json({
      ok: true,
      message: 'Attribution identifiers synced successfully',
      app_user_id: appUserId,
    });
  } catch (err) {
    const errorMsg = err instanceof Error ? err.message : String(err);
    console.error(`[Attribution Sync Error] app_user_id: ${appUserId}`, errorMsg);
    return c.json({ error: 'Failed to sync attribution', details: errorMsg }, 500);
  }
});

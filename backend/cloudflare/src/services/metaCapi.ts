// Meta Conversions API (Graph API v20.0) Client

import { Env, MetaCapiEvent, MetaCapiPayload, MetaCapiResponse, UserRecord } from '../types/index.js';
import { formatFbc, sha256 } from './crypto.js';

export interface SendMetaEventParams {
  eventName: 'Purchase' | 'Subscribe' | 'StartTrial' | 'Refund' | string;
  appUserId: string;
  userRecord?: UserRecord | null;
  transactionId: string;
  productId: string;
  value?: number;
  currency?: string;
  eventTimestamp: number; // in milliseconds
  actionSource?: 'app' | 'website' | 'system_generated';
}

export interface MetaCapiResult {
  success: boolean;
  eventsReceived?: number;
  fbtraceId?: string;
  error?: string;
}

/**
 * Sends conversion event to Meta Conversions API (v20.0)
 */
export async function sendMetaConversionEvent(
  env: Env,
  params: SendMetaEventParams
): Promise<MetaCapiResult> {
  const pixelId = env.META_PIXEL_ID;
  const accessToken = env.META_CAPI_ACCESS_TOKEN;

  if (!pixelId || !accessToken) {
    return {
      success: false,
      error: 'Missing META_PIXEL_ID or META_CAPI_ACCESS_TOKEN in environment bindings',
    };
  }

  try {
    // Hash identifiers according to Meta standards
    const hashedUserId = await sha256(params.appUserId);
    const fbc = formatFbc(params.userRecord?.fbc || params.userRecord?.fbclid);
    const fbp = params.userRecord?.fbp || undefined;
    const clientIp = params.userRecord?.ip_address || undefined;
    const clientUserAgent = params.userRecord?.user_agent || undefined;

    const eventTime = Math.floor(params.eventTimestamp / 1000);
    // Deduplication event_id matching app event if fired from SDK
    const eventId = `meta_${params.transactionId}_${params.eventName.toLowerCase()}`;

    const event: MetaCapiEvent = {
      event_name: params.eventName,
      event_time: eventTime,
      event_id: eventId,
      action_source: params.actionSource || 'app',
      user_data: {
        external_id: hashedUserId ? [hashedUserId] : undefined,
        fbc: fbc || undefined,
        fbp: fbp || undefined,
        client_ip_address: clientIp,
        client_user_agent: clientUserAgent,
      },
      custom_data: {
        value: params.value,
        currency: params.currency ? params.currency.toUpperCase() : 'USD',
        content_name: params.productId,
        content_type: 'product',
        contents: [
          {
            id: params.productId,
            quantity: 1,
            item_price: params.value,
          },
        ],
      },
    };

    const payload: MetaCapiPayload = {
      data: [event],
      ...(env.META_TEST_EVENT_CODE ? { test_event_code: env.META_TEST_EVENT_CODE } : {}),
    };

    const url = `https://graph.facebook.com/v20.0/${encodeURIComponent(pixelId)}/events?access_token=${encodeURIComponent(accessToken)}`;

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      body: JSON.stringify(payload),
    });

    const responseBody = (await response.json()) as MetaCapiResponse & { error?: { message: string; fbtrace_id?: string } };

    if (!response.ok) {
      const errorMsg = responseBody?.error?.message || `HTTP ${response.status}: ${response.statusText}`;
      return {
        success: false,
        error: `Meta CAPI error: ${errorMsg}`,
        fbtraceId: responseBody?.error?.fbtrace_id,
      };
    }

    return {
      success: true,
      eventsReceived: responseBody.events_received ?? 1,
      fbtraceId: responseBody.fbtrace_id,
    };
  } catch (err) {
    const errorMsg = err instanceof Error ? err.message : String(err);
    return {
      success: false,
      error: `Meta CAPI invocation failed: ${errorMsg}`,
    };
  }
}

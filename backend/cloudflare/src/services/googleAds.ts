// Google Ads Offline Conversion & GA4 Measurement Protocol Client

import { Env, Ga4MpPayload, UserRecord } from '../types/index.js';

export interface SendGoogleConversionParams {
  eventName: 'purchase' | 'refund' | 'in_app_purchase' | string;
  appUserId: string;
  userRecord?: UserRecord | null;
  transactionId: string;
  productId: string;
  value?: number;
  currency?: string;
  eventTimestamp: number; // in milliseconds
}

export interface GoogleConversionResult {
  success: boolean;
  channel: 'google_ads_api' | 'ga4_measurement_protocol' | 'none';
  error?: string;
  details?: Record<string, unknown>;
}

/**
 * Exchanges Google OAuth Refresh Token for a short-lived Access Token.
 */
async function getGoogleAdsAccessToken(env: Env): Promise<string> {
  const clientId = env.GOOGLE_ADS_CLIENT_ID;
  const clientSecret = env.GOOGLE_ADS_CLIENT_SECRET;
  const refreshToken = env.GOOGLE_ADS_REFRESH_TOKEN;

  if (!clientId || !clientSecret || !refreshToken) {
    throw new Error('Missing Google Ads OAuth credentials (client_id, client_secret, refresh_token)');
  }

  const response = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams({
      client_id: clientId,
      client_secret: clientSecret,
      refresh_token: refreshToken,
      grant_type: 'refresh_token',
    }),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Failed to refresh Google Ads access token: ${response.status} - ${errorText}`);
  }

  const data = (await response.json()) as { access_token: string };
  return data.access_token;
}

/**
 * Formats a Date to Google Ads Conversion DateTime format:
 * "yyyy-mm-dd hh:mm:ss+|-hh:mm"
 */
function formatGoogleAdsDateTime(timestampMillis: number): string {
  const date = new Date(timestampMillis);
  const pad = (n: number) => n.toString().padStart(2, '0');

  const year = date.getUTCFullYear();
  const month = pad(date.getUTCMonth() + 1);
  const day = pad(date.getUTCDate());
  const hours = pad(date.getUTCHours());
  const minutes = pad(date.getUTCMinutes());
  const seconds = pad(date.getUTCSeconds());

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}+00:00`;
}

/**
 * Uploads conversion directly to Google Ads API v18 via uploadClickConversions
 */
async function uploadToGoogleAdsApi(
  env: Env,
  params: SendGoogleConversionParams,
  gclid: string
): Promise<GoogleConversionResult> {
  const customerId = env.GOOGLE_ADS_CUSTOMER_ID?.replace(/-/g, '');
  const developerToken = env.GOOGLE_ADS_DEVELOPER_TOKEN;
  const conversionAction = env.GOOGLE_ADS_CONVERSION_ACTION_ID;

  if (!customerId || !developerToken || !conversionAction) {
    return {
      success: false,
      channel: 'google_ads_api',
      error: 'Incomplete Google Ads API configuration (CUSTOMER_ID, DEVELOPER_TOKEN, CONVERSION_ACTION_ID)',
    };
  }

  const accessToken = await getGoogleAdsAccessToken(env);
  const conversionDateTime = formatGoogleAdsDateTime(params.eventTimestamp);

  // Conversion action resource name format: customers/{customer_id}/conversionActions/{conversion_action_id}
  const conversionActionResource = conversionAction.startsWith('customers/')
    ? conversionAction
    : `customers/${customerId}/conversionActions/${conversionAction}`;

  const payload = {
    conversions: [
      {
        conversionAction: conversionActionResource,
        gclid: gclid,
        conversionDateTime: conversionDateTime,
        conversionValue: params.value ?? 0,
        currencyCode: params.currency ? params.currency.toUpperCase() : 'USD',
        orderId: params.transactionId,
      },
    ],
    partialFailure: true,
  };

  const url = `https://googleads.googleapis.com/v18/customers/${customerId}:uploadClickConversions`;

  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'developer-token': developerToken,
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(payload),
  });

  const responseJson = (await response.json()) as {
    partialFailureError?: { message: string };
    results?: Array<{ conversionAction: string; gclid: string }>;
  };

  if (!response.ok || responseJson.partialFailureError) {
    const errorMsg = responseJson.partialFailureError?.message || `HTTP ${response.status}: ${response.statusText}`;
    return {
      success: false,
      channel: 'google_ads_api',
      error: `Google Ads API error: ${errorMsg}`,
      details: responseJson,
    };
  }

  return {
    success: true,
    channel: 'google_ads_api',
    details: responseJson,
  };
}

/**
 * Sends event via Google Analytics 4 Measurement Protocol
 * (Automatically routes to linked Google Ads campaigns)
 */
async function sendGa4MeasurementProtocol(
  env: Env,
  params: SendGoogleConversionParams
): Promise<GoogleConversionResult> {
  const measurementId = env.GOOGLE_ANALYTICS_MEASUREMENT_ID;
  const apiSecret = env.GOOGLE_ANALYTICS_API_SECRET;

  if (!measurementId || !apiSecret) {
    return {
      success: false,
      channel: 'ga4_measurement_protocol',
      error: 'Missing GOOGLE_ANALYTICS_MEASUREMENT_ID or GOOGLE_ANALYTICS_API_SECRET',
    };
  }

  // Client ID fallback: uses hashed or sanitized app_user_id
  const clientId = params.userRecord?.gclid || params.appUserId || 'unknown_client';

  const ga4Payload: Ga4MpPayload = {
    client_id: clientId,
    user_id: params.appUserId,
    events: [
      {
        name: params.eventName === 'refund' ? 'refund' : 'purchase',
        params: {
          currency: params.currency ? params.currency.toUpperCase() : 'USD',
          value: params.value ?? 0,
          transaction_id: params.transactionId,
          items: [
            {
              item_id: params.productId,
              item_name: params.productId,
              price: params.value,
              quantity: 1,
            },
          ],
          gclid: params.userRecord?.gclid || undefined,
        },
      },
    ],
  };

  const url = `https://www.google-analytics.com/mp/collect?measurement_id=${encodeURIComponent(
    measurementId
  )}&api_secret=${encodeURIComponent(apiSecret)}`;

  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(ga4Payload),
  });

  if (!response.ok) {
    return {
      success: false,
      channel: 'ga4_measurement_protocol',
      error: `GA4 Measurement Protocol error HTTP ${response.status}`,
    };
  }

  return {
    success: true,
    channel: 'ga4_measurement_protocol',
  };
}

/**
 * Main dispatcher for Google Conversions:
 * Tries Google Ads API if gclid and OAuth configs are available;
 * Falls back to or complements with GA4 Measurement Protocol.
 */
export async function sendGoogleConversionEvent(
  env: Env,
  params: SendGoogleConversionParams
): Promise<GoogleConversionResult> {
  const gclid = params.userRecord?.gclid;

  // Try direct Google Ads API upload first if gclid and Google Ads API credentials exist
  if (gclid && env.GOOGLE_ADS_CUSTOMER_ID && env.GOOGLE_ADS_DEVELOPER_TOKEN && env.GOOGLE_ADS_REFRESH_TOKEN) {
    try {
      const gAdsResult = await uploadToGoogleAdsApi(env, params, gclid);
      if (gAdsResult.success) {
        return gAdsResult;
      }
      // If direct Google Ads upload failed, fall through to GA4 Measurement Protocol
    } catch (err) {
      // Continue to GA4 MP fallback
    }
  }

  // Fallback / standard route: GA4 Measurement Protocol
  if (env.GOOGLE_ANALYTICS_MEASUREMENT_ID && env.GOOGLE_ANALYTICS_API_SECRET) {
    return sendGa4MeasurementProtocol(env, params);
  }

  return {
    success: false,
    channel: 'none',
    error: 'No Google conversion channels configured (neither Google Ads API nor GA4 MP)',
  };
}

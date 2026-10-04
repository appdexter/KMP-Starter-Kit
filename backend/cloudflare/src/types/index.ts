// Type definitions for Cloudflare Edge Backend (Koko)

export interface Env {
  // Bindings
  DB: D1Database;
  SUBS_QUEUE?: Queue<SubscriptionQueueMessage>;

  // Meta CAPI Config
  META_PIXEL_ID?: string;
  META_CAPI_ACCESS_TOKEN?: string;
  META_TEST_EVENT_CODE?: string;

  // Google Ads / GA4 Measurement Protocol Config
  GOOGLE_ADS_CUSTOMER_ID?: string;
  GOOGLE_ADS_CONVERSION_ACTION_ID?: string;
  GOOGLE_ADS_DEVELOPER_TOKEN?: string;
  GOOGLE_ADS_CLIENT_ID?: string;
  GOOGLE_ADS_CLIENT_SECRET?: string;
  GOOGLE_ADS_REFRESH_TOKEN?: string;
  GOOGLE_ANALYTICS_MEASUREMENT_ID?: string;
  GOOGLE_ANALYTICS_API_SECRET?: string;

  // Webhook Authentication Secrets
  WEBHOOK_SECRET_REVENUECAT?: string;
  WEBHOOK_SECRET_ADAPTY?: string;

  // App Environment
  ENVIRONMENT?: string;
}

// Database Entity Types
export interface UserRecord {
  id: string;
  app_user_id: string;
  fbclid: string | null;
  gclid: string | null;
  fbp: string | null;
  fbc: string | null;
  ip_address: string | null;
  user_agent: string | null;
  created_at: number;
  updated_at: number;
}

export type WebhookStatus = 'RECEIVED' | 'QUEUED' | 'PROCESSED' | 'FAILED' | 'IGNORED';

export interface WebhookLogRecord {
  id: string;
  provider: 'revenuecat' | 'adapty';
  event_type: string;
  payload: string;
  status: WebhookStatus;
  error: string | null;
  created_at: number;
}

export type ConversionStatus = 'SENT' | 'FAILED' | 'SKIPPED_DEDUPE';

export interface ConversionRecord {
  id: string;
  user_id: string;
  event_name: string;
  value: number | null;
  currency: string | null;
  event_id: string;
  status: ConversionStatus;
  error: string | null;
  created_at: number;
}

// Queue Message Payload
export interface SubscriptionQueueMessage {
  id: string;
  provider: 'revenuecat' | 'adapty';
  eventType: string;
  appUserId: string;
  transactionId: string;
  originalTransactionId?: string;
  productId: string;
  price?: number;
  currency?: string;
  eventTimestamp: number;
  isTrial?: boolean;
  rawPayload: Record<string, unknown>;
}

// Meta Conversions API (CAPI) Interfaces
export interface MetaUserData {
  em?: string[]; // SHA256 hashed emails
  ph?: string[]; // SHA256 hashed phones
  external_id?: string[]; // SHA256 hashed external IDs or user IDs
  client_ip_address?: string;
  client_user_agent?: string;
  fbc?: string;
  fbp?: string;
}

export interface MetaCustomData {
  value?: number;
  currency?: string;
  content_name?: string;
  content_type?: string;
  contents?: Array<{
    id: string;
    quantity: number;
    item_price?: number;
  }>;
}

export interface MetaCapiEvent {
  event_name: 'Purchase' | 'Subscribe' | 'StartTrial' | 'Refund' | string;
  event_time: number; // Unix timestamp in seconds
  event_id: string; // Deduplication ID
  event_source_url?: string;
  action_source: 'app' | 'website' | 'system_generated';
  user_data: MetaUserData;
  custom_data?: MetaCustomData;
}

export interface MetaCapiPayload {
  data: MetaCapiEvent[];
  test_event_code?: string;
}

export interface MetaCapiResponse {
  events_received: number;
  fbtrace_id: string;
  messages?: string[];
}

// Google Analytics 4 Measurement Protocol Interfaces
export interface Ga4MpItem {
  item_id: string;
  item_name?: string;
  price?: number;
  quantity?: number;
}

export interface Ga4MpEvent {
  name: string;
  params: {
    currency?: string;
    value?: number;
    transaction_id?: string;
    items?: Ga4MpItem[];
    [key: string]: unknown;
  };
}

export interface Ga4MpPayload {
  client_id: string;
  user_id?: string;
  events: Ga4MpEvent[];
}

// Google Ads Click Conversion Upload Interfaces
export interface GoogleAdsClickConversion {
  conversionAction: string;
  gclid?: string;
  conversionDateTime: string; // "yyyy-mm-dd hh:mm:ss+|-hh:mm"
  conversionValue?: number;
  currencyCode?: string;
  orderId?: string;
}

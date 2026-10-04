// Cryptographic helper for Cloudflare Workers (using Web Crypto API)

/**
 * Normalizes and hashes input string using SHA-256.
 * Meta CAPI requirement: lowercase, trim whitespace, then SHA-256 hex string.
 */
export async function sha256(value: string | null | undefined): Promise<string | null> {
  if (!value || typeof value !== 'string') {
    return null;
  }

  const normalized = value.trim().toLowerCase();
  if (normalized.length === 0) {
    return null;
  }

  const msgUint8 = new TextEncoder().encode(normalized);
  const hashBuffer = await crypto.subtle.digest('SHA-256', msgUint8);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  const hashHex = hashArray.map((b) => b.toString(16).padStart(2, '0')).join('');
  return hashHex;
}

/**
 * Normalizes phone numbers according to E.164 without '+' or leading zeros, then hashes.
 */
export async function sha256Phone(phone: string | null | undefined): Promise<string | null> {
  if (!phone || typeof phone !== 'string') {
    return null;
  }
  // Strip non-digits
  const digitsOnly = phone.replace(/\D/g, '');
  if (digitsOnly.length === 0) {
    return null;
  }
  return sha256(digitsOnly);
}

/**
 * Formats Facebook Click ID (`fbc`) if only raw fbclid is provided.
 * Standard format: `fb.1.{creationTimeInMillis}.{fbclid}`
 */
export function formatFbc(fbclid: string | null | undefined, timestampMillis?: number): string | null {
  if (!fbclid) return null;
  if (fbclid.startsWith('fb.')) return fbclid;
  const time = timestampMillis ?? Date.now();
  return `fb.1.${time}.${fbclid.trim()}`;
}

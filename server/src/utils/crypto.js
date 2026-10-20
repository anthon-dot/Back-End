import crypto from 'crypto';

/**
 * Generates an immutable SHA-256 integrity hash for occupancy permits.
 * Combines permit number, vendor name, stall number, issue date, and expiration date.
 */
export function generatePermitIntegrityHash({ permitNo, vendorName, stallNo, issueDate, expirationDate }) {
  const payload = [
    permitNo,
    (vendorName || '').trim().toUpperCase(),
    (stallNo || '').trim().toUpperCase(),
    issueDate,
    expirationDate,
    'MANTICAO_MUNICIPAL_SEAL'
  ].join('|');

  return crypto.createHash('sha256').update(payload).digest('hex');
}

/**
 * Generates an immutable hash for payment receipts.
 */
export function generateReceiptIntegrityHash({ receiptNo, amount, payerName, stallNo, paymentDate }) {
  const payload = [
    receiptNo,
    Number(amount).toFixed(2),
    (payerName || '').trim().toUpperCase(),
    (stallNo || '').trim().toUpperCase(),
    paymentDate,
    'MANTICAO_TREASURY_SEAL'
  ].join('|');

  return crypto.createHash('sha256').update(payload).digest('hex');
}

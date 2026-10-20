import QRCode from 'qrcode';

/**
 * Generates QR Code Data URL for digital occupancy permits.
 * Contains the public verification endpoint URL and integrity hash.
 */
export async function generatePermitQRCode({ permitNo, integrityHash, baseUrl = 'http://localhost:5173' }) {
  const verificationUrl = `${baseUrl}/verify-permit?permit=${encodeURIComponent(permitNo)}&hash=${encodeURIComponent(integrityHash)}`;

  const qrDataUrl = await QRCode.toDataURL(verificationUrl, {
    errorCorrectionLevel: 'H',
    margin: 2,
    width: 256,
    color: {
      dark: '#0f172a',
      light: '#ffffff'
    }
  });

  return {
    verificationUrl,
    qrDataUrl
  };
}

import { jsPDF } from 'jspdf';
import { generatePermitQRCode } from './qrGenerator.js';

/**
 * Generates an official 3-Year Contract of Lease PDF
 */
export async function generateContractPDF({
  contractNo,
  vendorName,
  businessName,
  stallNo,
  stallType,
  monthlyRent,
  startDate,
  endDate,
  municipality = 'Municipality of Manticao',
  supervisorName = 'Market Supervisor'
}) {
  const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });

  // Header
  doc.setFontSize(10);
  doc.text('REPUBLIC OF THE PHILIPPINES', 105, 20, { align: 'center' });
  doc.text('PROVINCE OF MISAMIS ORIENTAL', 105, 25, { align: 'center' });
  doc.setFont('helvetica', 'bold');
  doc.text(municipality.toUpperCase(), 105, 30, { align: 'center' });
  doc.text('OFFICE OF THE MARKET SUPERVISOR', 105, 35, { align: 'center' });

  doc.setLineWidth(0.5);
  doc.line(20, 40, 190, 40);

  // Title
  doc.setFontSize(16);
  doc.text('CONTRACT OF LEASE OF MARKET STALL', 105, 50, { align: 'center' });
  doc.setFontSize(10);
  doc.setFont('helvetica', 'normal');
  doc.text(`Contract No: ${contractNo}`, 105, 56, { align: 'center' });

  // Body text
  const bodyText = `KNOW ALL MEN BY THESE PRESENTS:

This CONTRACT OF LEASE is entered into by and between:

The MUNICIPAL GOVERNMENT OF MANTICAO, represented herein by the Municipal Mayor / Market Supervisor, hereinafter referred to as the "LESSOR";

- and -

${vendorName.toUpperCase()}, of legal age, Filipino, doing business under the name and style of "${businessName.toUpperCase()}", hereinafter referred to as the "LESSEE".

WITNESSETH:
1. SUBJECT OF LEASE: The LESSOR hereby leases unto the LESSEE Stall No. ${stallNo} (${stallType}) situated at the Public Market of Manticao.
2. TERM: This lease shall be for a duration of THREE (3) YEARS, commencing on ${startDate} and expiring on ${endDate}, renewable upon mutual agreement and compliance with municipal regulations.
3. RENTAL: The monthly rental shall be PHILIPPINE PESOS: ₱${Number(monthlyRent).toLocaleString('en-PH', { minimumFractionDigits: 2 })} (PHP), payable on or before the fifth (5th) day of each calendar month.
4. ADVANCE RENT & STALL RIGHTS: The LESSEE has settled the required two (2) months advance rental and applicable stall rights fees prior to execution.
5. RESTRICTIONS: The LESSEE shall not sublease, assign, or transfer stall rights without prior written authority from the Municipal Mayor and Market Supervisor.`;

  const lines = doc.splitTextToSize(bodyText, 170);
  doc.text(lines, 20, 68);

  // Signatures
  const sigY = 220;
  doc.setFont('helvetica', 'bold');
  doc.text('LESSOR:', 30, sigY);
  doc.text('LESSEE:', 130, sigY);

  doc.line(30, sigY + 20, 85, sigY + 20);
  doc.line(130, sigY + 20, 185, sigY + 20);

  doc.setFontSize(9);
  doc.setFont('helvetica', 'normal');
  doc.text(supervisorName, 30, sigY + 25);
  doc.text('Market Supervisor / Mayor', 30, sigY + 29);

  doc.text(vendorName, 130, sigY + 25);
  doc.text('Vendor / Tenant', 130, sigY + 29);

  return Buffer.from(doc.output('arraybuffer'));
}

/**
 * Generates an Official Occupancy Permit PDF with embedded QR Code & Integrity Hash
 */
export async function generatePermitPDF({
  permitNo,
  vendorName,
  businessName,
  stallNo,
  stallType,
  issueDate,
  expirationDate,
  integrityHash
}) {
  const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });

  // Border frame
  doc.setDrawColor(24, 76, 120);
  doc.setLineWidth(1.5);
  doc.rect(15, 15, 180, 267);
  doc.setLineWidth(0.5);
  doc.rect(17, 17, 176, 263);

  // Header
  doc.setFontSize(10);
  doc.text('REPUBLIC OF THE PHILIPPINES', 105, 30, { align: 'center' });
  doc.text('MUNICIPALITY OF MANTICAO, MISAMIS ORIENTAL', 105, 35, { align: 'center' });
  doc.text('PUBLIC MARKET & REVENUE OPERATIONS', 105, 40, { align: 'center' });

  doc.setFontSize(18);
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(24, 76, 120);
  doc.text('CERTIFICATE OF OCCUPANCY PERMIT', 105, 52, { align: 'center' });

  doc.setFontSize(11);
  doc.setTextColor(220, 38, 38);
  doc.text(`PERMIT NO: ${permitNo}`, 105, 60, { align: 'center' });

  doc.setTextColor(15, 23, 42);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(10);
  doc.text('THIS IS TO CERTIFY THAT THE REGISTERED VENDOR NAMED BELOW HAS SATISFIED ALL', 105, 75, { align: 'center' });
  doc.text('MUNICIPAL ORDINANCE REQUIREMENTS, PHYSICAL STALL INSPECTIONS, AND ADVANCE FEES:', 105, 80, { align: 'center' });

  // Vendor Details Box
  doc.setFillColor(248, 250, 252);
  doc.rect(30, 90, 150, 55, 'F');
  doc.setDrawColor(203, 213, 225);
  doc.rect(30, 90, 150, 55, 'D');

  doc.setFontSize(10);
  doc.setFont('helvetica', 'bold');
  doc.text('Registered Lessee:', 35, 100);
  doc.setFont('helvetica', 'normal');
  doc.text(vendorName, 80, 100);

  doc.setFont('helvetica', 'bold');
  doc.text('Business Trade Name:', 35, 108);
  doc.setFont('helvetica', 'normal');
  doc.text(businessName, 80, 108);

  doc.setFont('helvetica', 'bold');
  doc.text('Assigned Stall Number:', 35, 116);
  doc.setFont('helvetica', 'normal');
  doc.text(`Stall No. ${stallNo} (${stallType})`, 80, 116);

  doc.setFont('helvetica', 'bold');
  doc.text('Date of Issuance:', 35, 124);
  doc.setFont('helvetica', 'normal');
  doc.text(issueDate, 80, 124);

  doc.setFont('helvetica', 'bold');
  doc.text('Valid Until:', 35, 132);
  doc.setFont('helvetica', 'normal');
  doc.text(expirationDate, 80, 132);

  // Generate QR Code Image
  const { qrDataUrl } = await generatePermitQRCode({ permitNo, integrityHash });
  doc.addImage(qrDataUrl, 'PNG', 80, 155, 50, 50);

  doc.setFontSize(8);
  doc.setTextColor(100, 116, 139);
  doc.text('Scan QR Code with mobile camera to verify permit validity and authenticity.', 105, 210, { align: 'center' });
  doc.text(`Digital Integrity Hash: ${integrityHash.slice(0, 32)}...`, 105, 215, { align: 'center' });

  // Signatures
  doc.setFontSize(9);
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(15, 23, 42);
  doc.text('HON. MUNICIPAL MAYOR', 50, 250, { align: 'center' });
  doc.text('MARKET SUPERVISOR', 150, 250, { align: 'center' });

  return Buffer.from(doc.output('arraybuffer'));
}

/**
 * Generates an Official Payment Receipt PDF
 */
export async function generateReceiptPDF({
  receiptNo,
  payerName,
  stallNo,
  amount,
  paymentType,
  paymentChannel = 'CASH',
  cashierName = 'Municipal Treasurer',
  paymentDate
}) {
  const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a5' });

  doc.setFontSize(10);
  doc.text('MUNICIPALITY OF MANTICAO', 74, 15, { align: 'center' });
  doc.text('OFFICE OF THE MUNICIPAL TREASURER', 74, 20, { align: 'center' });
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(12);
  doc.text('OFFICIAL RECEIPT', 74, 28, { align: 'center' });

  doc.setFontSize(9);
  doc.setTextColor(220, 38, 38);
  doc.text(`OR No: ${receiptNo}`, 74, 34, { align: 'center' });

  doc.setTextColor(15, 23, 42);
  doc.setFont('helvetica', 'normal');
  doc.text(`Date: ${paymentDate || new Date().toLocaleDateString()}`, 15, 45);
  doc.text(`Received From: ${payerName}`, 15, 52);
  doc.text(`Stall No: ${stallNo}`, 15, 59);
  doc.text(`Payment Nature: ${paymentType.replace(/_/g, ' ')}`, 15, 66);
  doc.text(`Payment Channel: ${paymentChannel}`, 15, 73);

  doc.line(15, 80, 133, 80);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(12);
  doc.text(`AMOUNT PAID: PHP ₱${Number(amount).toLocaleString('en-PH', { minimumFractionDigits: 2 })}`, 15, 90);
  doc.line(15, 96, 133, 96);

  doc.setFontSize(8);
  doc.setFont('helvetica', 'normal');
  doc.text(`Collected by: ${cashierName}`, 15, 115);
  doc.text('Official System-Generated Receipt. Valid for municipal accounting audit.', 74, 130, { align: 'center' });

  return Buffer.from(doc.output('arraybuffer'));
}

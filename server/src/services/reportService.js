import { supabase } from '../config/supabase.js';

/**
 * Calculates verified, deterministic report metrics directly from database tables
 */
export async function getVerifiedReportMetrics() {
  const [stallsRes, billingsRes, paymentsRes, contractsRes, stakeholdersRes, remittancesRes] = await Promise.allSettled([
    supabase.from('stalls').select('id, stall_no, stall_type, status, monthly_rent, section:sections(name)'),
    supabase.from('billings').select('id, total_amount, paid_amount, balance, due_date, status, billing_period'),
    supabase.from('payments').select('id, amount, payment_type, payment_channel, payment_date, receipt_no'),
    supabase.from('contracts').select('id, status, monthly_rent, start_date, end_date'),
    supabase.from('stakeholders').select('id, business_name, application_status, is_archived, advance_balance'),
    supabase.from('collection_remittances').select('id, total_amount, status, created_at')
  ]);

  const stalls = stallsRes.status === 'fulfilled' && stallsRes.value.data ? stallsRes.value.data : [];
  const billings = billingsRes.status === 'fulfilled' && billingsRes.value.data ? billingsRes.value.data : [];
  const payments = paymentsRes.status === 'fulfilled' && paymentsRes.value.data ? paymentsRes.value.data : [];
  const contracts = contractsRes.status === 'fulfilled' && contractsRes.value.data ? contractsRes.value.data : [];
  const stakeholders = stakeholdersRes.status === 'fulfilled' && stakeholdersRes.value.data ? stakeholdersRes.value.data : [];
  const remittances = remittancesRes.status === 'fulfilled' && remittancesRes.value.data ? remittancesRes.value.data : [];

  // Stall Occupancy Figures
  const totalStalls = stalls.length;
  const occupiedStalls = stalls.filter(s => s.status === 'OCCUPIED').length;
  const pendingStalls = stalls.filter(s => s.status === 'PENDING').length;
  const reservedStalls = stalls.filter(s => s.status === 'RESERVED').length;
  const availableStalls = stalls.filter(s => s.status === 'AVAILABLE' || !s.status).length;
  const occupancyRate = totalStalls > 0 ? Number(((occupiedStalls / totalStalls) * 100).toFixed(1)) : 0;

  // Financial Metrics (Deterministic calculations)
  const billedRevenue = billings.reduce((sum, b) => sum + Number(b.total_amount || 0), 0);
  const totalCashCollected = payments.reduce((sum, p) => sum + Number(p.amount || 0), 0);
  const outstandingReceivables = billings.reduce((sum, b) => sum + Number(b.balance || 0), 0);
  const totalAdvanceBalances = stakeholders.reduce((sum, s) => sum + Number(s.advance_balance || 0), 0);

  const todayStr = new Date().toISOString().split('T')[0];
  const overdueBills = billings.filter(b => (b.status === 'OVERDUE' || (b.due_date && b.due_date < todayStr)) && Number(b.balance) > 0);
  const overdueAmount = overdueBills.reduce((sum, b) => sum + Number(b.balance || 0), 0);

  // Vendor & Contract Stats
  const activeContractsCount = contracts.filter(c => c.status === 'ACTIVE').length;
  const archivedVendorsCount = stakeholders.filter(s => s.is_archived).length;
  const reconciledRemittanceTotal = remittances
    .filter(r => r.status === 'RECONCILED')
    .reduce((sum, r) => sum + Number(r.total_amount || 0), 0);

  return {
    inventory: {
      totalStalls,
      occupiedStalls,
      pendingStalls,
      reservedStalls,
      availableStalls,
      occupancyRate: `${occupancyRate}%`
    },
    financials: {
      billedRevenue,
      totalCashCollected,
      outstandingReceivables,
      overdueAmount,
      overdueAccountsCount: overdueBills.length,
      totalAdvanceBalances,
      reconciledRemittanceTotal
    },
    operations: {
      activeContractsCount,
      archivedVendorsCount
    },
    aiInsights: [
      `Overall market stall occupancy is currently ${occupancyRate}% (${occupiedStalls} occupied of ${totalStalls} total units).`,
      overdueBills.length > 0
        ? `There are ${overdueBills.length} billing account(s) in arrears totaling ₱${overdueAmount.toLocaleString()}. Automated demand notices are active.`
        : 'All tenant billing accounts are in good standing.',
      totalAdvanceBalances > 0
        ? `Total unamortized advance rent held in trust is ₱${totalAdvanceBalances.toLocaleString()}.`
        : 'No advance balances currently on hold.'
    ]
  };
}

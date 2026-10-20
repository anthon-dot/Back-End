import { supabase } from '../config/supabase.js';

/**
 * Service for calculating dynamic stall rates and required initial fees
 * Blueprint formula: Stall Rights Fee + Two (2) Months Advance Rent
 */
export async function calculateInitialFeesForStall(stallId) {
  // 1. Fetch stall details
  const { data: stall, error: stallErr } = await supabase
    .from('stalls')
    .select('*, section:sections(*)')
    .eq('id', Number(stallId))
    .single();

  if (stallErr || !stall) {
    throw new Error(`Stall #${stallId} not found for rental rate calculation.`);
  }

  // 2. Fetch system settings or default rate formula
  const { data: settings } = await supabase
    .from('system_settings')
    .select('*')
    .eq('id', 1)
    .maybeSingle();

  const isOldCategory = stall.rate_category === 'OLD' || stall.section?.rate_category === 'OLD';
  const defaultStallRights = isOldCategory
    ? Number(settings?.stall_rights_fee_old || 3000.00)
    : Number(settings?.stall_rights_fee_new || 5000.00);

  const stallRightsFee = Number(stall.stall_rights_fee || defaultStallRights);
  const monthlyRent = Number(stall.monthly_rent || 1500.00);
  const advanceMonths = Number(settings?.advance_rent_months || 2); // 2 months baseline

  const advanceRentAmount = monthlyRent * advanceMonths;
  const totalInitialAmount = stallRightsFee + advanceRentAmount;

  return {
    stallId: stall.id,
    stallNo: stall.stall_no,
    stallType: stall.stall_type,
    rateCategory: isOldCategory ? 'OLD' : 'NEW',
    areaSqm: Number(stall.area_sqm || 9.00),
    monthlyRent,
    advanceMonths,
    stallRightsFee,
    advanceRentAmount,
    totalInitialAmount
  };
}

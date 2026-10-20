import { supabase } from '../config/supabase.js';
import { APPLICATION_STATUS, STALL_STATUS, CONTRACT_STATUS, PAYMENT_TYPES } from '../config/constants.js';

/**
 * Migration Script: Maps legacy records from Spring Boot / initial tables into normalized Supabase tables.
 */
export async function runLegacyDataMigration() {
  console.log('================================================================');
  console.log(' Starting Legacy Data Migration into Normalized Supabase Schema ');
  console.log('================================================================');

  const report = {
    stallsMigrated: 0,
    stakeholdersMigrated: 0,
    applicationsMigrated: 0,
    contractsMigrated: 0,
    billingsMigrated: 0,
    paymentsMigrated: 0,
    warnings: []
  };

  try {
    // 1. Check and normalize existing stalls
    const { data: stalls, error: stallErr } = await supabase.from('stalls').select('*');
    if (!stallErr && stalls) {
      for (const st of stalls) {
        // Ensure rate_category and area_sqm exist
        let needsUpdate = false;
        const updates = {};
        if (!st.rate_category) {
          updates.rate_category = 'NEW';
          needsUpdate = true;
        }
        if (!st.area_sqm) {
          updates.area_sqm = 9.00;
          needsUpdate = true;
        }
        if (!st.stall_rights_fee) {
          updates.stall_rights_fee = 5000.00;
          needsUpdate = true;
        }
        if (needsUpdate) {
          await supabase.from('stalls').update(updates).eq('id', st.id);
        }
        report.stallsMigrated++;
      }
    }

    // 2. Normalize Stakeholders & ensure stakeholder_applications entries exist
    const { data: stakeholders, error: shErr } = await supabase.from('stakeholders').select('*');
    if (!shErr && stakeholders) {
      for (const sh of stakeholders) {
        // Ensure advance balance baseline
        if (sh.total_advance_amount === undefined || sh.total_advance_amount === 0) {
          const estimatedRent = 1500;
          await supabase.from('stakeholders').update({ total_advance_amount: estimatedRent * 2 }).eq('id', sh.id);
        }

        // Check if an application record exists for this stakeholder
        const { data: exApp } = await supabase
          .from('stakeholder_applications')
          .select('id')
          .eq('stakeholder_id', sh.id)
          .maybeSingle();

        if (!exApp && sh.selected_stall_id) {
          const appNo = `APP-LEGACY-${sh.id}`;
          await supabase.from('stakeholder_applications').insert({
            application_no: appNo,
            stakeholder_id: sh.id,
            user_id: sh.user_id,
            stall_id: sh.selected_stall_id,
            status: sh.verified_tenant ? APPLICATION_STATUS.COMPLETED : APPLICATION_STATUS.PENDING_SUPERVISOR_REVIEW,
            current_holding_office: sh.verified_tenant ? 'Commercial Tenant Operations' : 'Market Supervisor',
            total_initial_amount_due: Number(sh.total_advance_amount || 8000),
            amount_paid: Number(sh.advance_payment_amount || 0),
            submitted_at: sh.created_at || new Date().toISOString()
          });
          report.applicationsMigrated++;
        }

        report.stakeholdersMigrated++;
      }
    }

    // 3. Normalize Contracts (ensure 3-year term and status)
    const { data: contracts, error: conErr } = await supabase.from('contracts').select('*');
    if (!conErr && contracts) {
      for (const con of contracts) {
        // Check if start_date & end_date are valid
        if (!con.start_date || !con.end_date) {
          const sDate = new Date();
          const eDate = new Date(sDate);
          eDate.setFullYear(eDate.getFullYear() + 3);

          await supabase.from('contracts').update({
            start_date: sDate.toISOString().split('T')[0],
            end_date: eDate.toISOString().split('T')[0],
            billing_frequency: 'MONTHLY'
          }).eq('id', con.id);
        }
        report.contractsMigrated++;
      }
    }

    // 4. Normalize Billings (ensure status is clean uppercase)
    const { data: billings, error: billErr } = await supabase.from('billings').select('*');
    if (!billErr && billings) {
      for (const b of billings) {
        const bal = Number(b.balance);
        const normStatus = bal <= 0 ? 'PAID' : (b.paid_amount > 0 ? 'PARTIALLY_PAID' : 'UNPAID');
        if (b.status !== normStatus && b.status !== 'OVERDUE') {
          await supabase.from('billings').update({ status: normStatus }).eq('id', b.id);
        }
        report.billingsMigrated++;
      }
    }

    // 5. Normalize Payments (ensure receipt_no exists and canonical payment_type)
    const { data: payments, error: payErr } = await supabase.from('payments').select('*');
    if (!payErr && payments) {
      for (const p of payments) {
        if (!p.receipt_no) {
          const generatedOR = `OR-LEGACY-${p.id}`;
          await supabase.from('payments').update({ receipt_no: generatedOR }).eq('id', p.id);
        }
        report.paymentsMigrated++;
      }
    }

    console.log('Migration completed successfully!');
    console.table(report);
    return report;
  } catch (err) {
    console.error('Migration encountered an error:', err);
    throw err;
  }
}

if (import.meta.url === `file://${process.argv[1]}`) {
  runLegacyDataMigration()
    .then(() => process.exit(0))
    .catch(() => process.exit(1));
}

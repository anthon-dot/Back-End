import cron from 'node-cron';
import { runMonthlyBillingGeneration } from '../services/billingService.js';
import { scanDelinquentAccounts } from '../services/delinquencyService.js';

/**
 * Initializes durable background jobs using node-cron.
 */
export function startBackgroundJobs() {
  console.log('[JobRunner] Initializing background workers...');

  // 1. Monthly Recurring Billing: Runs on the 1st day of every month at 00:01 AM
  cron.schedule('1 0 1 * *', async () => {
    console.log('[JobRunner:Billing] Triggering scheduled monthly billing generation...');
    try {
      const result = await runMonthlyBillingGeneration();
      console.log('[JobRunner:Billing] Completed:', result);
    } catch (err) {
      console.error('[JobRunner:Billing] Error during monthly billing job:', err);
    }
  });

  // 2. Daily Delinquency Scan: Runs daily at 06:00 AM
  cron.schedule('0 6 * * *', async () => {
    console.log('[JobRunner:Delinquency] Running daily delinquency arrears scan...');
    try {
      const result = await scanDelinquentAccounts();
      console.log('[JobRunner:Delinquency] Completed:', result);
    } catch (err) {
      console.error('[JobRunner:Delinquency] Error scanning delinquency:', err);
    }
  });

  console.log('[JobRunner] Monthly billing and daily delinquency workers scheduled.');
}

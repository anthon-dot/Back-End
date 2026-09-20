-- ==============================================================================
-- AUTOMATED SCHEDULED JOBS (CRON) FOR SUPABASE
-- Requires pg_cron extension enabled in Supabase Project Settings -> Database -> Extensions
-- ==============================================================================

-- 1. Enable pg_cron and pg_net
CREATE EXTENSION IF NOT EXISTS pg_cron;
CREATE EXTENSION IF NOT EXISTS pg_net;

-- 2. Daily check: Flag overdue billings
-- Runs every night at midnight (00:00 UTC)
SELECT cron.schedule(
    'mark-overdue-billings',
    '0 0 * * *',
    $$
        UPDATE public.billings
        SET status = 'OVERDUE'
        WHERE due_date < CURRENT_DATE 
          AND status IN ('UNPAID', 'PARTIAL')
          AND balance > 0;
    $$
);

-- 3. Daily check: Expiring contracts alert
-- Runs daily at 01:00 UTC to notify administrators of contracts expiring within 30 days
SELECT cron.schedule(
    'alert-expiring-contracts',
    '0 1 * * *',
    $$
        INSERT INTO public.notifications (title, message, priority, notification_type, related_record_type, related_record_id)
        SELECT 
            'Contract Expiring Soon' AS title,
            'Contract ' || contract_no || ' is scheduled to expire on ' || end_date || '.' AS message,
            'HIGH' AS priority,
            'CONTRACT_EXPIRATION' AS notification_type,
            'CONTRACT' AS related_record_type,
            id AS related_record_id
        FROM public.contracts
        WHERE status = 'ACTIVE'
          AND end_date BETWEEN CURRENT_DATE AND (CURRENT_DATE + INTERVAL '30 days')
          AND NOT EXISTS (
              SELECT 1 FROM public.notifications n
              WHERE n.related_record_type = 'CONTRACT'
                AND n.related_record_id = contracts.id
                AND n.created_at > (CURRENT_DATE - INTERVAL '7 days')
          );
    $$
);

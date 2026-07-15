ALTER TABLE business_applications
    ADD COLUMN IF NOT EXISTS selected_stall_id BIGINT REFERENCES stalls(id);

ALTER TABLE stakeholders
    ADD COLUMN IF NOT EXISTS selected_stall_id BIGINT REFERENCES stalls(id);

CREATE INDEX IF NOT EXISTS idx_business_application_selected_stall
    ON business_applications(selected_stall_id);

CREATE INDEX IF NOT EXISTS idx_stakeholder_selected_stall
    ON stakeholders(selected_stall_id);

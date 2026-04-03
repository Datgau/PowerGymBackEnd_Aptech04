-- Add registration_type column to service_registrations table
-- This migration adds support for tracking whether a registration was created online or at the counter

ALTER TABLE service_registrations 
ADD COLUMN IF NOT EXISTS registration_type VARCHAR(20) DEFAULT 'ONLINE';

-- Update existing records to have ONLINE as default
UPDATE service_registrations 
SET registration_type = 'ONLINE' 
WHERE registration_type IS NULL;

-- Add comment for documentation
COMMENT ON COLUMN service_registrations.registration_type IS 'Type of registration: ONLINE (created through online payment) or COUNTER (created by admin at counter)';

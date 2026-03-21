-- Add expirationDate column to service_registrations table
ALTER TABLE service_registrations 
ADD COLUMN expiration_date DATETIME;

-- Update existing records to calculate expiration_date based on service duration
UPDATE service_registrations sr
JOIN gym_services gs ON sr.service_id = gs.id
SET sr.expiration_date = DATE_ADD(sr.registration_date, INTERVAL gs.duration DAY)
WHERE sr.expiration_date IS NULL;

-- Add EXPIRED status to enum (MySQL will handle this automatically when the enum is used)
-- The enum update will be handled by JPA when the application starts
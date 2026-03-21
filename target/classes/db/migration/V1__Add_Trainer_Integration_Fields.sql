-- Migration script for adding trainer integration fields to existing tables
-- Version: V1
-- Description: Add trainer integration fields to ServiceRegistration and TrainerBooking tables

-- ServiceRegistration table modifications
ALTER TABLE service_registrations 
ADD COLUMN trainer_id BIGINT,
ADD COLUMN trainer_selected_at TIMESTAMP,
ADD COLUMN trainer_selection_notes TEXT;

-- Add foreign key constraint for trainer
ALTER TABLE service_registrations
ADD CONSTRAINT fk_service_registration_trainer 
    FOREIGN KEY (trainer_id) REFERENCES users(id);

-- TrainerBooking table modifications  
ALTER TABLE trainer_bookings
ADD COLUMN service_registration_id BIGINT,
ADD COLUMN session_objective TEXT,
ADD COLUMN session_number INTEGER,
ADD COLUMN trainer_notes TEXT,
ADD COLUMN client_feedback TEXT,
ADD COLUMN rating INTEGER CHECK (rating >= 1 AND rating <= 5);

-- Add foreign key constraint for service registration
ALTER TABLE trainer_bookings
ADD CONSTRAINT fk_trainer_booking_service_registration 
    FOREIGN KEY (service_registration_id) REFERENCES service_registrations(id);

-- Update BookingStatus enum to include new statuses
-- Note: This depends on your database. For PostgreSQL:
ALTER TABLE trainer_bookings 
ALTER COLUMN status TYPE VARCHAR(20);

-- Update existing CONFIRMED bookings to maintain compatibility
-- New bookings will default to PENDING status

-- Performance indexes
CREATE INDEX idx_service_registrations_trainer_id ON service_registrations(trainer_id);
CREATE INDEX idx_service_registrations_status_trainer ON service_registrations(status, trainer_id);
CREATE INDEX idx_service_registrations_trainer_selected_at ON service_registrations(trainer_selected_at);

CREATE INDEX idx_trainer_bookings_service_registration ON trainer_bookings(service_registration_id);
CREATE INDEX idx_trainer_bookings_trainer_date_status ON trainer_bookings(trainer_id, booking_date, status);
CREATE INDEX idx_trainer_bookings_status_date ON trainer_bookings(status, booking_date);
CREATE INDEX idx_trainer_bookings_rating ON trainer_bookings(rating) WHERE rating IS NOT NULL;
CREATE INDEX idx_trainer_bookings_session_number ON trainer_bookings(session_number) WHERE session_number IS NOT NULL;

-- Add comments for documentation
COMMENT ON COLUMN service_registrations.trainer_id IS 'Selected trainer for this service registration (nullable for backward compatibility)';
COMMENT ON COLUMN service_registrations.trainer_selected_at IS 'Timestamp when trainer was selected';
COMMENT ON COLUMN service_registrations.trainer_selection_notes IS 'Notes about trainer selection process';

COMMENT ON COLUMN trainer_bookings.service_registration_id IS 'Link to the service registration that created this booking';
COMMENT ON COLUMN trainer_bookings.session_objective IS 'Specific objective for this training session';
COMMENT ON COLUMN trainer_bookings.session_number IS 'Sequential session number within the service program';
COMMENT ON COLUMN trainer_bookings.trainer_notes IS 'Trainer notes about the session';
COMMENT ON COLUMN trainer_bookings.client_feedback IS 'Client feedback after session completion';
COMMENT ON COLUMN trainer_bookings.rating IS 'Session rating from 1-5 stars';
-- Creates a dedicated application user and grants minimal privileges on the Hospital schema
-- Run with a privileged account (e.g., root):
--   mysql -u root -p < create_app_user.sql

-- Adjust password as desired
CREATE USER IF NOT EXISTS 'hospital_app'@'localhost' IDENTIFIED BY 'hospital_pass123!';

-- Grant essential DML permissions on the Hospital database
GRANT SELECT, INSERT, UPDATE, DELETE ON Hospital.* TO 'hospital_app'@'localhost';

FLUSH PRIVILEGES;


-- Test Database Setup
-- Run this to verify your Hospital database is properly configured

-- 1. Check database exists
SHOW DATABASES LIKE 'Hospital';

-- 2. Use the database
USE Hospital;

-- 3. Check all tables exist
SHOW TABLES;

-- 4. Verify Person table
SELECT 'Checking Person table...' AS Status;
SELECT COUNT(*) AS person_count FROM Person;
SELECT * FROM Person;

-- 5. Verify Patient table
SELECT 'Checking Patient table...' AS Status;
SELECT COUNT(*) AS patient_count FROM Patient;
SELECT p.person_id, p.first_name, p.last_name, pt.insurance_id, pt.notes
FROM Person p
JOIN Patient pt ON pt.patient_id = p.person_id;

-- 6. Verify Staff and Doctor
SELECT 'Checking Doctor table...' AS Status;
SELECT COUNT(*) AS doctor_count FROM Doctor;
SELECT p.person_id, p.first_name, p.last_name, d.license_no, s.name AS specialty
FROM Person p
JOIN Staff st ON st.staff_id = p.person_id
JOIN Doctor d ON d.staff_id = st.staff_id
JOIN Specialty s ON s.specialty_id = d.specialty_id;

-- 7. Verify AppointmentStatus
SELECT 'Checking AppointmentStatus...' AS Status;
SELECT * FROM AppointmentStatus;

-- 8. Verify Appointments
SELECT 'Checking Appointment table...' AS Status;
SELECT COUNT(*) AS appointment_count FROM Appointment;
SELECT a.appointment_id, a.patient_id, a.doctor_id, a.scheduled_at,
       a.reason, ast.status
FROM Appointment a
JOIN AppointmentStatus ast ON a.status_id = ast.status_id;

-- 9. Check constraints
SELECT 'Checking foreign key constraints...' AS Status;
SELECT
    TABLE_NAME,
    COLUMN_NAME,
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM
    INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE
    TABLE_SCHEMA = 'Hospital'
    AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME, COLUMN_NAME;

SELECT 'Database verification complete!' AS Status;


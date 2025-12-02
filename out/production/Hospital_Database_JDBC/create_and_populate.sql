-- create_and_populate.sql
-- ========================
-- HOSPITAL DATABASE SCHEMA
-- ========================

CREATE DATABASE IF NOT EXISTS Hospital;
USE Hospital;

SET FOREIGN_KEY_CHECKS = 0;
DROP TRIGGER IF EXISTS trg_set_bill_timestamp;
DROP VIEW IF EXISTS BillSummary;
DROP FUNCTION IF EXISTS GetPatientBalance;
DROP TABLE IF EXISTS BillItem;
DROP TABLE IF EXISTS Bill;
DROP TABLE IF EXISTS Service;
DROP TABLE IF EXISTS PrescriptionItem;
DROP TABLE IF EXISTS Medication;
DROP TABLE IF EXISTS Prescription;
DROP TABLE IF EXISTS Appointment;
DROP TABLE IF EXISTS AppointmentStatus;
DROP TABLE IF EXISTS Doctor;
DROP TABLE IF EXISTS Staff;
DROP TABLE IF EXISTS Patient;
DROP TABLE IF EXISTS Contact;
DROP TABLE IF EXISTS StaffRole;
DROP TABLE IF EXISTS Specialty;
DROP TABLE IF EXISTS Department;
DROP TABLE IF EXISTS Person;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. PERSON
CREATE TABLE Person (
    person_id INT PRIMARY KEY AUTO_INCREMENT,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    date_of_birth DATETIME NOT NULL
);

-- 2. CONTACT
CREATE TABLE Contact (
    contact_id INT PRIMARY KEY AUTO_INCREMENT,
    person_id INT NOT NULL,
    contact_type ENUM('email', 'phone') NOT NULL,
    contact_info VARCHAR(100) NOT NULL,
    FOREIGN KEY (person_id) REFERENCES Person(person_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_contact_person ON Contact(person_id);

-- 3. STAFFROLE
CREATE TABLE StaffRole (
    staff_role_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- 4. SPECIALTY
CREATE TABLE Specialty (
    specialty_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    base_visit_fee DECIMAL(10,2) NOT NULL
);

-- 5. DEPARTMENT
CREATE TABLE Department (
    department_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    building_number VARCHAR(10),
    floor INT,
    capacity INT
);

-- 6. PATIENT (subtype of Person)
CREATE TABLE Patient (
    patient_id INT PRIMARY KEY,
    insurance_id VARCHAR(50),
    notes TEXT,
    FOREIGN KEY (patient_id) REFERENCES Person(person_id)
        ON DELETE CASCADE
);

-- 7. STAFF (subtype of Person)
CREATE TABLE Staff (
    staff_id INT PRIMARY KEY,
    staff_role_id INT NOT NULL,
    department_id INT NOT NULL,
    hire_date DATE NOT NULL,
    FOREIGN KEY (staff_id) REFERENCES Person(person_id)
        ON DELETE CASCADE,
    FOREIGN KEY (staff_role_id) REFERENCES StaffRole(staff_role_id),
    FOREIGN KEY (department_id) REFERENCES Department(department_id)
);

CREATE INDEX idx_staff_dept ON Staff(department_id);

-- 8. DOCTOR (subtype of Staff)
CREATE TABLE Doctor (
    staff_id INT PRIMARY KEY,
    specialty_id INT NOT NULL,
    license_no VARCHAR(50) UNIQUE,
    FOREIGN KEY (staff_id) REFERENCES Staff(staff_id)
        ON DELETE CASCADE,
    FOREIGN KEY (specialty_id) REFERENCES Specialty(specialty_id)
);

-- 9. APPOINTMENTSTATUS
CREATE TABLE AppointmentStatus (
    status_id INT PRIMARY KEY AUTO_INCREMENT,
    status VARCHAR(30) NOT NULL UNIQUE
);

-- 10. APPOINTMENT
CREATE TABLE Appointment (
    appointment_id INT PRIMARY KEY AUTO_INCREMENT,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    scheduled_at DATETIME NOT NULL,
    reason VARCHAR(255),
    status_id INT NOT NULL,
    FOREIGN KEY (patient_id) REFERENCES Patient(patient_id),
    FOREIGN KEY (doctor_id) REFERENCES Doctor(staff_id),
    FOREIGN KEY (status_id) REFERENCES AppointmentStatus(status_id)
);

CREATE INDEX idx_appt_patient ON Appointment(patient_id);
CREATE INDEX idx_appt_doctor ON Appointment(doctor_id);

-- 11. PRESCRIPTION
CREATE TABLE Prescription (
    prescription_id INT PRIMARY KEY AUTO_INCREMENT,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    appointment_id INT,
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    notes TEXT,
    FOREIGN KEY (patient_id) REFERENCES Patient(patient_id),
    FOREIGN KEY (doctor_id) REFERENCES Doctor(staff_id),
    FOREIGN KEY (appointment_id) REFERENCES Appointment(appointment_id)
);

CREATE INDEX idx_prescription_patient ON Prescription(patient_id);

-- 12. MEDICATION
CREATE TABLE Medication (
    medication_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    unit VARCHAR(20),
    unit_price DECIMAL(10,2)
);

-- 13. PRESCRIPTIONITEM
CREATE TABLE PrescriptionItem (
    prescription_id INT NOT NULL,
    order_no INT NOT NULL,
    medication_id INT NOT NULL,
    dosage VARCHAR(50),
    quantity INT,
    PRIMARY KEY (prescription_id, order_no),
    FOREIGN KEY (prescription_id) REFERENCES Prescription(prescription_id)
        ON DELETE CASCADE,
    FOREIGN KEY (medication_id) REFERENCES Medication(medication_id)
);

-- 14. SERVICE
CREATE TABLE Service (
    service_id INT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    unit_price DECIMAL(10,2) NOT NULL
);

-- 15. BILL
CREATE TABLE Bill (
    bill_no INT PRIMARY KEY AUTO_INCREMENT,
    patient_id INT NOT NULL,
    appointment_id INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES Patient(patient_id),
    FOREIGN KEY (appointment_id) REFERENCES Appointment(appointment_id)
);

CREATE INDEX idx_bill_patient ON Bill(patient_id);

-- 16. BILLITEM
CREATE TABLE BillItem (
    bill_no INT NOT NULL,
    order_no INT NOT NULL,
    charge_type ENUM('Doctor','Room','Prescription','Medication','Service','Other') NOT NULL,
    service_id INT,
    description VARCHAR(255),
    amount DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (bill_no, order_no),
    FOREIGN KEY (bill_no) REFERENCES Bill(bill_no)
        ON DELETE CASCADE,
    FOREIGN KEY (service_id) REFERENCES Service(service_id)
);

-- ======================
-- TRIGGER EXAMPLE
-- ======================

-- Automatically set created_at if not provided on insert
CREATE TRIGGER trg_set_bill_timestamp
BEFORE INSERT ON Bill
FOR EACH ROW
SET NEW.created_at = COALESCE(NEW.created_at, NOW());

-- ======================
-- INDEX EXAMPLES
-- ======================

CREATE INDEX idx_appointment_status ON Appointment(status_id);
CREATE INDEX idx_prescription_doctor ON Prescription(doctor_id);
CREATE INDEX idx_billitem_service ON BillItem(service_id);

-- ======================
-- VIEW FOR REPORTING
-- ======================
CREATE VIEW BillSummary AS
SELECT b.bill_no,
       b.patient_id,
       p.first_name,
       p.last_name,
       b.appointment_id,
       b.created_at,
       COALESCE(SUM(bi.amount), 0) AS total_amount
FROM Bill b
JOIN Patient pt ON pt.patient_id = b.patient_id
JOIN Person p ON p.person_id = pt.patient_id
LEFT JOIN BillItem bi ON bi.bill_no = b.bill_no
GROUP BY b.bill_no, b.patient_id, p.first_name, p.last_name, b.appointment_id, b.created_at;

-- ======================
-- FUNCTION FOR BILL TOTALS
-- ======================
DELIMITER //
CREATE FUNCTION GetPatientBalance(p_patient_id INT)
RETURNS DECIMAL(10,2)
DETERMINISTIC
BEGIN
    DECLARE total DECIMAL(10,2);
    SELECT COALESCE(SUM(bi.amount), 0)
      INTO total
      FROM Bill b
      LEFT JOIN BillItem bi ON bi.bill_no = b.bill_no
     WHERE b.patient_id = p_patient_id;
    RETURN total;
END;//
DELIMITER ;

-- ======================
-- SAMPLE DATA
-- ======================

INSERT INTO StaffRole(name) VALUES ('Doctor'), ('Nurse'), ('Admin');

INSERT INTO Specialty(name, base_visit_fee) VALUES
('Cardiology', 200.00),
('Pediatrics', 150.00);

INSERT INTO Department(name, building_number, floor, capacity) VALUES
('Cardiology Dept', 'A', 2, 30),
('Pediatrics Dept', 'B', 3, 40);

INSERT INTO AppointmentStatus(status) VALUES ('Scheduled'), ('Completed'), ('Cancelled');

-- Persons
INSERT INTO Person(first_name, last_name, date_of_birth) VALUES
('Alice', 'Smith', '1990-05-10 00:00:00'),  -- person_id 1 (patient)
('Bob', 'Johnson', '1985-03-22 00:00:00'),  -- person_id 2 (doctor)
('Carol', 'Davis', '2000-07-15 00:00:00');  -- person_id 3 (another patient)

-- Patients
INSERT INTO Patient(patient_id, insurance_id, notes) VALUES
(1, 'INS-123', 'Allergic to penicillin'),
(3, 'INS-456', NULL);

-- Staff (Bob as doctor)
INSERT INTO Staff(staff_id, staff_role_id, department_id, hire_date) VALUES
(2, 1, 1, '2015-01-01');

-- Doctor
INSERT INTO Doctor(staff_id, specialty_id, license_no) VALUES
(2, 1, 'LIC-ABC-123');

-- Appointments
INSERT INTO Appointment(patient_id, doctor_id, scheduled_at, reason, status_id) VALUES
(1, 2, DATE_ADD(NOW(), INTERVAL 1 DAY), 'Regular checkup', 1),
(3, 2, DATE_ADD(NOW(), INTERVAL 2 DAY), 'Chest pain', 1);

-- Service and Bill examples
INSERT INTO Service(code, description, unit_price) VALUES
('CONSULT', 'Doctor consultation', 100.00),
('XRAY', 'X-Ray imaging', 250.00);

-- Create one example bill manually
INSERT INTO Bill(patient_id, appointment_id) VALUES (1, 1); -- bill_no 1

INSERT INTO BillItem(bill_no, order_no, charge_type, service_id, description, amount) VALUES
(1, 1, 'Doctor', NULL, 'Doctor consultation fee', 100.00),
(1, 2, 'Service', 2, 'X-Ray imaging', 250.00);

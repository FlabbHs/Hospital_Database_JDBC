import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class Main {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("MySQL driver not found. Ensure mysql-connector-j is installed.", ex);
        }
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in);
             Connection connection = DB.getConnection()) {

            PatientTable patientTable = new PatientTable(connection);
            AppointmentTable appointmentTable = new AppointmentTable(connection);

            boolean running = true;
            while (running) {
                printMenu();
                String choice = scanner.nextLine().trim();
                switch (choice) {
                    case "1":
                        try { patientTable.listAllPatients(); } catch (SQLException ex) { logError("view all patients", ex); }
                        break;
                    case "2":
                        try { viewPatientByID(patientTable, scanner); } catch (SQLException ex) { logError("view patient by ID", ex); }
                        break;
                    case "3":
                        try { appointmentTable.listAllAppointments(); } catch (SQLException ex) { logError("view all appointments", ex); }
                        break;
                    case "4":
                        try { viewAppointmentByID(appointmentTable, scanner); } catch (SQLException ex) { logError("view appointment by ID", ex); }
                        break;
                    case "5":
                        try { insertPatient(patientTable, scanner); } catch (SQLException ex) { logError("insert patient", ex); }
                        break;
                    case "6":
                        try { updatePatientNotes(patientTable, scanner); } catch (SQLException ex) { logError("update patient notes", ex); }
                        break;
                    case "7":
                        try { deletePatient(patientTable, scanner); } catch (SQLException ex) { logError("delete patient", ex); }
                        break;
                    case "8":
                        try { insertAppointment(appointmentTable, scanner); } catch (SQLException ex) { logError("insert appointment", ex); }
                        break;
                    case "9":
                        try { updateAppointmentStatus(appointmentTable, scanner); } catch (SQLException ex) { logError("update appointment status", ex); }
                        break;
                    case "10":
                        try { deleteAppointment(appointmentTable, scanner); } catch (SQLException ex) { logError("delete appointment", ex); }
                        break;
                    case "11":
                        runAppointmentTransaction(connection, scanner);
                        break;
                    case "12":
                        try { viewAllPersons(connection); } catch (SQLException ex) { logError("view all persons", ex); }
                        break;
                    case "13":
                        try { viewPatientDetailsWithJoin(connection, scanner); } catch (SQLException ex) { logError("view patient details", ex); }
                        break;
                    case "14":
                        try { viewAppointmentsByStatus(connection, scanner); } catch (SQLException ex) { logError("view appointments by status", ex); }
                        break;
                    case "0":
                        running = false;
                        System.out.println("Goodbye!");
                        break;
                    default:
                        System.out.println("Unknown option. Try again.");
                }
            }
        } catch (SQLException ex) {
            System.out.println("Fatal DB error: " + ex.getMessage());
        } catch (Exception ex) {
            System.out.println("Fatal error: " + ex.getMessage());
        }
    }

    private static void printMenu() {
        System.out.println("\n========== Hospital Management System ==========");
        System.out.println("PATIENT OPERATIONS:");
        System.out.println("  1) View All Patients (via PatientTable class)");
        System.out.println("  2) View Patient by ID (via PatientTable class)");
        System.out.println("  5) Insert New Patient");
        System.out.println("  6) Update Patient Notes");
        System.out.println("  7) Delete Patient");
        System.out.println("\nAPPOINTMENT OPERATIONS:");
        System.out.println("  3) View All Appointments (via AppointmentTable class)");
        System.out.println("  4) View Appointment by ID (via AppointmentTable class)");
        System.out.println("  8) Insert New Appointment");
        System.out.println("  9) Update Appointment Status");
        System.out.println("  10) Delete Appointment");
        System.out.println("\nADDITIONAL SELECT QUERIES (Direct in Main.java):");
        System.out.println("  12) View All Persons");
        System.out.println("  13) View Patient Details with JOIN");
        System.out.println("  14) View Appointments by Status");
        System.out.println("\nTRANSACTION:");
        System.out.println("  11) Run Appointment Transaction (COMMIT/ROLLBACK Demo)");
        System.out.println("\n  0) Exit");
        System.out.println("================================================");
        System.out.print("Select option: ");
    }

    private static void viewPatientByID(PatientTable patientTable, Scanner scanner) throws SQLException {
        System.out.print("Enter Patient ID: ");
        int id = Integer.parseInt(scanner.nextLine().trim());
        patientTable.selectPatientByID(id);
    }

    private static void viewAppointmentByID(AppointmentTable appointmentTable, Scanner scanner) throws SQLException {
        System.out.print("Enter Appointment ID: ");
        int id = Integer.parseInt(scanner.nextLine().trim());
        appointmentTable.selectAppointmentByID(id);
    }

    private static void insertPatient(PatientTable patientTable, Scanner scanner) throws SQLException {
        System.out.println("\n-- Insert Patient --");
        int patientId = promptInt(scanner, "Existing Person ID");
        String insurance = promptString(scanner, "Insurance ID");
        String notes = promptString(scanner, "Notes (optional)");
        patientTable.insertPatient(patientId, insurance, notes);
    }

    private static void updatePatientNotes(PatientTable patientTable, Scanner scanner) throws SQLException {
        System.out.println("\n-- Update Patient Notes --");
        int patientId = promptInt(scanner, "Patient ID");
        String notes = promptString(scanner, "New notes");
        patientTable.updatePatientNotes(patientId, notes);
    }

    private static void deletePatient(PatientTable patientTable, Scanner scanner) throws SQLException {
        System.out.println("\n-- Delete Patient --");
        int patientId = promptInt(scanner, "Patient ID");
        patientTable.deletePatient(patientId);
    }

    private static void insertAppointment(AppointmentTable appointmentTable, Scanner scanner) throws SQLException {
        System.out.println("\n-- Insert Appointment --");
        int patientId = promptInt(scanner, "Patient ID");
        int doctorId = promptInt(scanner, "Doctor ID (staff_id from Doctor table)");
        Timestamp scheduledAt = promptTimestamp(scanner, "Scheduled time (yyyy-MM-dd HH:mm)");
        String reason = promptString(scanner, "Reason (optional)");
        int statusId = promptInt(scanner, "Status ID (1=Scheduled, 2=Completed, 3=Cancelled)");
        appointmentTable.insertAppointment(patientId, doctorId, scheduledAt, reason, statusId);
    }

    private static void updateAppointmentStatus(AppointmentTable appointmentTable, Scanner scanner) throws SQLException {
        System.out.println("\n-- Update Appointment Status --");
        int appointmentId = promptInt(scanner, "Appointment ID");
        int statusId = promptInt(scanner, "New Status ID (1=Scheduled, 2=Completed, 3=Cancelled)");
        appointmentTable.updateAppointmentStatus(appointmentId, statusId);
    }

    private static void deleteAppointment(AppointmentTable appointmentTable, Scanner scanner) throws SQLException {
        System.out.println("\n-- Delete Appointment --");
        int appointmentId = promptInt(scanner, "Appointment ID");
        appointmentTable.deleteAppointment(appointmentId);
    }

    private static void runAppointmentTransaction(Connection connection, Scanner scanner) {
        System.out.println("\n========== TRANSACTION DEMO: Schedule Appointment + Update Patient Notes ==========");
        System.out.println("This transaction will:");
        System.out.println("  1. Insert a new appointment");
        System.out.println("  2. Update patient notes to reflect the appointment");
        System.out.println("  3. Ask you to COMMIT or ROLLBACK");
        System.out.println();

        try {
            connection.setAutoCommit(false);

            int patientId = promptInt(scanner, "Patient ID");
            int doctorId = promptInt(scanner, "Doctor ID (staff_id)");
            Timestamp appointmentTime = promptTimestamp(scanner, "Appointment time (yyyy-MM-dd HH:mm)");
            String reason = promptString(scanner, "Reason for visit");
            int statusId = promptInt(scanner, "Status ID (1=Scheduled, 2=Completed, 3=Cancelled)");
            String noteFragment = promptString(scanner, "Note text to append to patient record");

            // Step 1: Insert appointment
            String insertAppointment = "INSERT INTO Appointment (patient_id, doctor_id, scheduled_at, reason, status_id) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(insertAppointment)) {
                ps.setInt(1, patientId);
                ps.setInt(2, doctorId);
                ps.setTimestamp(3, appointmentTime);
                ps.setString(4, reason);
                ps.setInt(5, statusId);
                int rows = ps.executeUpdate();
                System.out.println("✓ Step 1: Inserted " + rows + " appointment(s)");
            }

            // Step 2: Update patient notes
            String updateNotes = "UPDATE Patient SET notes = CONCAT(COALESCE(notes, ''), ?) WHERE patient_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(updateNotes)) {
                ps.setString(1, "\n[Appointment Scheduled] " + noteFragment);
                ps.setInt(2, patientId);
                int rows = ps.executeUpdate();
                System.out.println("✓ Step 2: Updated " + rows + " patient record(s)");
            }

            System.out.println("\n>>> Transaction ready. Commit or rollback? (y=COMMIT / n=ROLLBACK): ");
            System.out.print("Your choice: ");
            String choice = scanner.nextLine().trim();

            if (choice.equalsIgnoreCase("y")) {
                connection.commit();
                System.out.println("✓✓✓ Transaction COMMITTED successfully! ✓✓✓");
            } else {
                connection.rollback();
                System.out.println("✗✗✗ Transaction ROLLED BACK - no changes made. ✗✗✗");
            }
        } catch (SQLException ex) {
            try {
                connection.rollback();
                System.out.println("✗✗✗ Transaction ROLLED BACK due to error. ✗✗✗");
            } catch (SQLException rollbackEx) {
                System.out.println("CRITICAL: Rollback failed: " + rollbackEx.getMessage());
            }
            System.out.println("Transaction failed: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                System.out.println("Could not reset auto-commit: " + ex.getMessage());
            }
        }
    }

    private static int promptInt(Scanner scanner, String label) {
        while (true) {
            System.out.print(label + ": ");
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException ex) {
                System.out.println("Enter a valid integer.");
            }
        }
    }

    private static String promptString(Scanner scanner, String label) {
        System.out.print(label + ": ");
        return scanner.nextLine().trim();
    }

    private static Timestamp promptTimestamp(Scanner scanner, String label) {
        while (true) {
            System.out.print(label + ": ");
            String input = scanner.nextLine().trim();
            try {
                LocalDateTime parsed = LocalDateTime.parse(input, DATE_TIME_FORMATTER);
                return Timestamp.valueOf(parsed);
            } catch (DateTimeParseException ex) {
                System.out.println("Use format yyyy-MM-dd HH:mm (e.g., 2024-05-31 09:30).");
            }
        }
    }

    private static void logError(String action, SQLException ex) {
        System.out.println("Failed to " + action + ": " + ex.getMessage());
    }

    // ========== ADDITIONAL SELECT QUERIES IN MAIN.JAVA ==========

    /**
     * SELECT query example 1: View all persons from Person table
     * Demonstrates simple SELECT with PreparedStatement
     */
    private static void viewAllPersons(Connection connection) throws SQLException {
        String sql = "SELECT person_id, first_name, last_name, date_of_birth FROM Person ORDER BY person_id";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            System.out.println("\n-- All Persons (SELECT in Main.java) --");
            System.out.println("ID | Name | Date of Birth");
            System.out.println("------------------------------------");
            while (rs.next()) {
                System.out.printf("%d | %s %s | %s%n",
                        rs.getInt("person_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getTimestamp("date_of_birth"));
            }
        }
    }

    /**
     * SELECT query example 2: View patient details with complex JOIN
     * Demonstrates PreparedStatement with WHERE clause and parameter binding
     */
    private static void viewPatientDetailsWithJoin(Connection connection, Scanner scanner) throws SQLException {
        System.out.print("Enter Patient ID: ");
        int patientId = Integer.parseInt(scanner.nextLine().trim());

        String sql = "SELECT p.person_id, p.first_name, p.last_name, p.date_of_birth, " +
                     "pt.insurance_id, pt.notes, " +
                     "COUNT(a.appointment_id) AS appointment_count " +
                     "FROM Person p " +
                     "JOIN Patient pt ON pt.patient_id = p.person_id " +
                     "LEFT JOIN Appointment a ON a.patient_id = pt.patient_id " +
                     "WHERE p.person_id = ? " +
                     "GROUP BY p.person_id, p.first_name, p.last_name, p.date_of_birth, pt.insurance_id, pt.notes";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\n-- Patient Details with JOIN (SELECT in Main.java) --");
                if (rs.next()) {
                    System.out.printf("ID: %d%n", rs.getInt("person_id"));
                    System.out.printf("Name: %s %s%n", rs.getString("first_name"), rs.getString("last_name"));
                    System.out.printf("DOB: %s%n", rs.getTimestamp("date_of_birth"));
                    System.out.printf("Insurance: %s%n", rs.getString("insurance_id"));
                    System.out.printf("Notes: %s%n", rs.getString("notes"));
                    System.out.printf("Total Appointments: %d%n", rs.getInt("appointment_count"));
                } else {
                    System.out.println("No patient found with ID " + patientId);
                }
            }
        }
    }

    /**
     * SELECT query example 3: View appointments filtered by status
     * Demonstrates PreparedStatement with JOIN and WHERE clause
     */
    private static void viewAppointmentsByStatus(Connection connection, Scanner scanner) throws SQLException {
        System.out.println("Status options: 1=Scheduled, 2=Completed, 3=Cancelled");
        System.out.print("Enter Status ID: ");
        int statusId = Integer.parseInt(scanner.nextLine().trim());

        String sql = "SELECT a.appointment_id, a.patient_id, a.doctor_id, a.scheduled_at, a.reason, " +
                     "ast.status, p.first_name AS patient_name, p.last_name AS patient_lastname " +
                     "FROM Appointment a " +
                     "JOIN AppointmentStatus ast ON a.status_id = ast.status_id " +
                     "JOIN Patient pt ON a.patient_id = pt.patient_id " +
                     "JOIN Person p ON pt.patient_id = p.person_id " +
                     "WHERE a.status_id = ? " +
                     "ORDER BY a.scheduled_at";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, statusId);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\n-- Appointments by Status (SELECT in Main.java) --");
                System.out.println("ID | Patient | Doctor | Scheduled At | Status | Reason");
                System.out.println("----------------------------------------------------------------");
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("%d | %s %s (ID:%d) | Doctor %d | %s | %s | %s%n",
                            rs.getInt("appointment_id"),
                            rs.getString("patient_name"),
                            rs.getString("patient_lastname"),
                            rs.getInt("patient_id"),
                            rs.getInt("doctor_id"),
                            rs.getTimestamp("scheduled_at"),
                            rs.getString("status"),
                            rs.getString("reason"));
                }
                if (!found) {
                    System.out.println("No appointments found with the specified status.");
                }
            }
        }
    }
}

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
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

            ensureCoreTables(connection);
            ensureSeedData(connection);
            createBillSummaryArtifacts(connection);
            ensurePatientAppointmentsView(connection);

            PatientTable patientTable = new PatientTable(connection);
            AppointmentTable appointmentTable = new AppointmentTable(connection);
            BillTable billTable = new BillTable(connection);

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
                    case "15":
                        try { listBillsForPatient(billTable, scanner); } catch (SQLException ex) { logError("list bills", ex); }
                        break;
                    case "16":
                        try { viewBillByID(billTable, scanner); } catch (SQLException ex) { logError("view bill", ex); }
                        break;
                    case "17":
                        try { insertBill(billTable, scanner); } catch (SQLException ex) { logError("insert bill", ex); }
                        break;
                    case "18":
                        try { deleteBill(billTable, scanner); } catch (SQLException ex) { logError("delete bill", ex); }
                        break;
                    case "19":
                        runBillingTransaction(connection, scanner);
                        break;
                    case "20":
                        try { viewBillSummary(connection); } catch (SQLException ex) { logError("view bill summary", ex); }
                        break;
                    case "21":
                        try { viewPatientBalance(connection, scanner); } catch (SQLException ex) { logError("view patient balance", ex); }
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
        System.out.println("\nBILL OPERATIONS:");
        System.out.println("  15) List Bills for Patient");
        System.out.println("  16) View Bill by ID");
        System.out.println("  17) Insert New Bill");
        System.out.println("  18) Delete Bill");
        System.out.println("\nTRANSACTIONS:");
        System.out.println("  11) Run Appointment Transaction (COMMIT/ROLLBACK Demo)");
        System.out.println("  19) Run Billing Transaction (Appointment + Bill + Notes)");
        System.out.println("\nREPORTING / VIEWS / FUNCTIONS:");
        System.out.println("  20) View BillSummary View");
        System.out.println("  21) View Patient Balance via Stored Function");
        System.out.println("\n  0) Exit");
        System.out.println("================================================");
        System.out.print("Select option: ");
    }

    // Create or replace a VIEW combining Patient, Person, Appointment, and AppointmentStatus
    private static void ensurePatientAppointmentsView(Connection connection) throws SQLException {
        String sql = ""
            + "CREATE OR REPLACE VIEW v_patient_appointments AS "
            + "SELECT "
            + "  pt.patient_id, "
            + "  p.first_name, "
            + "  p.last_name, "
            + "  a.appointment_id, "
            + "  a.doctor_id, "
            + "  a.scheduled_at, "
            + "  a.reason, "
            + "  ast.status "
            + "FROM Patient pt "
            + "JOIN Person p ON p.person_id = pt.patient_id "
            + "LEFT JOIN Appointment a ON a.patient_id = pt.patient_id "
            + "LEFT JOIN AppointmentStatus ast ON ast.status_id = a.status_id";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    // Query the VIEW using PreparedStatement and optional filters
    private static void viewPatientAppointmentsFromView(Connection connection, Scanner scanner) throws SQLException {
        System.out.println("\n-- View Patient Appointments (from SQL VIEW) --");
        Integer patientId = promptOptionalInt(scanner, "Filter by Patient ID (blank for all)");
        Integer statusId = promptOptionalInt(scanner, "Filter by Status ID (1=Scheduled, 2=Completed, 3=Cancelled; blank for all)");

        String base = "SELECT patient_id, first_name, last_name, appointment_id, doctor_id, scheduled_at, reason, status "
                    + "FROM v_patient_appointments ";
        String where = "";
        if (patientId != null && statusId != null) {
            where = "WHERE patient_id = ? AND status = (SELECT status FROM AppointmentStatus WHERE status_id = ?) ";
        } else if (patientId != null) {
            where = "WHERE patient_id = ? ";
        } else if (statusId != null) {
            where = "WHERE status = (SELECT status FROM AppointmentStatus WHERE status_id = ?) ";
        }
        String order = "ORDER BY scheduled_at IS NULL, scheduled_at";

        try (PreparedStatement ps = connection.prepareStatement(base + where + " " + order)) {
            int idx = 1;
            if (patientId != null) {
                ps.setInt(idx++, patientId);
            }
            if (statusId != null) {
                ps.setInt(idx++, statusId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("PID | Name | ApptID | DoctorID | Scheduled At | Status | Reason");
                System.out.println("-----------------------------------------------------------------");
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf("%d | %s %s | %s | %s | %s | %s | %s%n",
                            rs.getInt("patient_id"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getObject("appointment_id"),
                            rs.getObject("doctor_id"),
                            rs.getObject("scheduled_at"),
                            rs.getString("status"),
                            rs.getString("reason"));
                }
                if (!any) {
                    System.out.println("No rows found for given filters.");
                }
            }
        }
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

    private static void listBillsForPatient(BillTable billTable, Scanner scanner) throws SQLException {
        System.out.println("\n-- List Bills for Patient --");
        int patientId = promptInt(scanner, "Patient ID");
        billTable.listBillsForPatient(patientId);
    }

    private static void viewBillByID(BillTable billTable, Scanner scanner) throws SQLException {
        System.out.println("\n-- View Bill --");
        int billNo = promptInt(scanner, "Bill Number");
        billTable.selectBillByID(billNo);
    }

    private static void insertBill(BillTable billTable, Scanner scanner) throws SQLException {
        System.out.println("\n-- Insert Bill --");
        int patientId = promptInt(scanner, "Patient ID");
        Integer appointmentId = promptOptionalInt(scanner, "Appointment ID (blank if none)");
        billTable.insertBill(patientId, appointmentId);
    }

    private static void deleteBill(BillTable billTable, Scanner scanner) throws SQLException {
        System.out.println("\n-- Delete Bill --");
        int billNo = promptInt(scanner, "Bill Number");
        billTable.deleteBill(billNo);
    }

    private static void runAppointmentTransaction(Connection connection, Scanner scanner) {
        System.out.println("\n========== TRANSACTION DEMO: Schedule Appointment + Update Patient Notes ==========");
        System.out.println("This transaction will:");
        System.out.println("  1. Insert a new appointment");
        System.out.println("  2. Update patient notes to reflect the appointment");
        System.out.println("  3. Ask you to COMMIT or ROLLBACK");
        System.out.println();

        int patientId = promptInt(scanner, "Patient ID");
        int doctorId = promptInt(scanner, "Doctor ID (staff_id)");
        Timestamp appointmentTime = promptTimestamp(scanner, "Appointment time (yyyy-MM-dd HH:mm)");
        String reason = promptString(scanner, "Reason for visit");
        int statusId = promptInt(scanner, "Status ID (1=Scheduled, 2=Completed, 3=Cancelled)");
        String noteFragment = promptString(scanner, "Note text to append to patient record");

        try {
            if (!assertEntityExists(connection, "SELECT 1 FROM Patient WHERE patient_id = ?", patientId, "Patient")) {
                return;
            }
            if (!assertEntityExists(connection, "SELECT 1 FROM Doctor WHERE staff_id = ?", doctorId, "Doctor")) {
                return;
            }
            if (!assertEntityExists(connection, "SELECT 1 FROM AppointmentStatus WHERE status_id = ?", statusId, "Status")) {
                return;
            }

            connection.setAutoCommit(false);

            String insertAppointment = "INSERT INTO Appointment (patient_id, doctor_id, scheduled_at, reason, status_id) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(insertAppointment)) {
                ps.setInt(1, patientId);
                ps.setInt(2, doctorId);
                ps.setTimestamp(3, appointmentTime);
                ps.setString(4, reason);
                ps.setInt(5, statusId);
                int rows = ps.executeUpdate();
                System.out.println("\u2713 Step 1: Inserted " + rows + " appointment(s)");
            }

            String updateNotes = "UPDATE Patient SET notes = CONCAT(COALESCE(notes, ''), ?) WHERE patient_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(updateNotes)) {
                ps.setString(1, "\n[Appointment Scheduled] " + noteFragment);
                ps.setInt(2, patientId);
                int rows = ps.executeUpdate();
                System.out.println("\u2713 Step 2: Updated " + rows + " patient record(s)");
            }

            System.out.println("\n>>> Transaction ready. Commit or rollback? (y=COMMIT / n=ROLLBACK): ");
            System.out.print("Your choice: ");
            String choice = scanner.nextLine().trim();

            if (choice.equalsIgnoreCase("y")) {
                connection.commit();
                System.out.println("\u2713\u2713\u2713 Transaction COMMITTED successfully! \u2713\u2713\u2713");
            } else {
                connection.rollback();
                System.out.println("\u2717\u2717\u2717 Transaction ROLLED BACK - no changes made. \u2717\u2717\u2717");
            }
        } catch (SQLException ex) {
            try {
                connection.rollback();
                System.out.println("\u2717\u2717\u2717 Transaction ROLLED BACK due to error. \u2717\u2717\u2717");
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

    private static void runBillingTransaction(Connection connection, Scanner scanner) {
        System.out.println("\n========== TRANSACTION DEMO: Appointment + Bill + Patient Note ==========");
        System.out.println("This transaction will:");
        System.out.println("  1. Insert a new appointment");
        System.out.println("  2. Immediately create a bill linked to that appointment");
        System.out.println("  3. Append a billing note to the patient record");
        System.out.println("  4. Let you COMMIT or ROLLBACK the whole workflow");
        System.out.println();

        int patientId = promptInt(scanner, "Patient ID");
        int doctorId = promptInt(scanner, "Doctor ID (staff_id)");
        Timestamp appointmentTime = promptTimestamp(scanner, "Appointment time (yyyy-MM-dd HH:mm)");
        String reason = promptString(scanner, "Reason (optional, blank allowed)");
        if (reason.isEmpty()) {
            reason = null;
        }
        int statusId = promptInt(scanner, "Status ID (1=Scheduled, 2=Completed, 3=Cancelled)");
        String noteFragment = promptString(scanner, "Note text to append to patient record");

        try {
            if (!assertEntityExists(connection, "SELECT 1 FROM Patient WHERE patient_id = ?", patientId, "Patient")) {
                return;
            }
            if (!assertEntityExists(connection, "SELECT 1 FROM Doctor WHERE staff_id = ?", doctorId, "Doctor")) {
                return;
            }
            if (!assertEntityExists(connection, "SELECT 1 FROM AppointmentStatus WHERE status_id = ?", statusId, "Status")) {
                return;
            }

            connection.setAutoCommit(false);

            int newAppointmentId;
            String insertAppointment = "INSERT INTO Appointment (patient_id, doctor_id, scheduled_at, reason, status_id) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(insertAppointment, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, patientId);
                ps.setInt(2, doctorId);
                ps.setTimestamp(3, appointmentTime);
                if (reason == null) {
                    ps.setNull(4, Types.VARCHAR);
                } else {
                    ps.setString(4, reason);
                }
                ps.setInt(5, statusId);
                int rows = ps.executeUpdate();
                System.out.println("\u2713 Step 1: Inserted " + rows + " appointment(s)");

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        newAppointmentId = keys.getInt(1);
                    } else {
                        throw new SQLException("Appointment insert succeeded but no ID was returned.");
                    }
                }
            }

            String insertBill = "INSERT INTO Bill (patient_id, appointment_id) VALUES (?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(insertBill)) {
                ps.setInt(1, patientId);
                ps.setInt(2, newAppointmentId);
                int rows = ps.executeUpdate();
                System.out.println("\u2713 Step 2: Inserted " + rows + " bill(s)");
            }

            String updateNotes = "UPDATE Patient SET notes = CONCAT(COALESCE(notes, ''), ?) WHERE patient_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(updateNotes)) {
                ps.setString(1, "\n[Billing Transaction] " + noteFragment);
                ps.setInt(2, patientId);
                int rows = ps.executeUpdate();
                System.out.println("\u2713 Step 3: Updated " + rows + " patient record(s)");
            }

            System.out.println("\n>>> Transaction ready. Commit or rollback? (y=COMMIT / n=ROLLBACK): ");
            System.out.print("Your choice: ");
            String choice = scanner.nextLine().trim();

            if (choice.equalsIgnoreCase("y")) {
                connection.commit();
                System.out.println("\u2713\u2713\u2713 Billing transaction COMMITTED successfully! \u2713\u2713\u2713");
            } else {
                connection.rollback();
                System.out.println("\u2717\u2717\u2717 Billing transaction ROLLED BACK - no changes made. \u2717\u2717\u2717");
            }
        } catch (SQLException ex) {
            try {
                connection.rollback();
                System.out.println("\u2717\u2717\u2717 Billing transaction ROLLED BACK due to error. \u2717\u2717\u2717");
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

    private static void viewBillSummary(Connection connection) throws SQLException {
        String sql = "SELECT bill_no, patient_id, first_name, last_name, appointment_id, created_at, total_amount FROM BillSummary ORDER BY bill_no";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            System.out.println("\n-- Bill Summary View --");
            System.out.println("Bill | Patient | Name | Appt | Created | Total");
            while (rs.next()) {
                System.out.printf("%d | %d | %s %s | %s | %s | %.2f%n",
                        rs.getInt("bill_no"),
                        rs.getInt("patient_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getObject("appointment_id") == null ? "NULL" : rs.getInt("appointment_id"),
                        rs.getTimestamp("created_at"),
                        rs.getDouble("total_amount"));
            }
        }
    }

    private static void viewPatientBalance(Connection connection, Scanner scanner) throws SQLException {
        System.out.println("\n-- Patient Balance (Stored Function) --");
        int patientId = promptInt(scanner, "Patient ID");
        String sql = "SELECT GetPatientBalance(?) AS balance";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.printf("Patient %d outstanding balance: %.2f%n", patientId, rs.getDouble("balance"));
                } else {
                    System.out.println("Function did not return a result.");
                }
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

    private static Integer promptOptionalInt(Scanner scanner, String label) {
        while (true) {
            System.out.print(label + ": ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException ex) {
                System.out.println("Enter a valid integer or leave blank.");
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

    private static void ensureCoreTables(Connection connection) throws SQLException {
        String[][] required = {
                {"person", "CREATE TABLE Person (" +
                        "person_id INT PRIMARY KEY AUTO_INCREMENT," +
                        "first_name VARCHAR(50) NOT NULL," +
                        "last_name VARCHAR(50) NOT NULL," +
                        "date_of_birth DATETIME NOT NULL)"},
                {"staffrole", "CREATE TABLE StaffRole (" +
                        "staff_role_id INT PRIMARY KEY AUTO_INCREMENT," +
                        "name VARCHAR(50) NOT NULL UNIQUE)"},
                {"specialty", "CREATE TABLE Specialty (" +
                        "specialty_id INT PRIMARY KEY AUTO_INCREMENT," +
                        "name VARCHAR(50) NOT NULL UNIQUE," +
                        "base_visit_fee DECIMAL(10,2) NOT NULL)"},
                {"department", "CREATE TABLE Department (" +
                        "department_id INT PRIMARY KEY AUTO_INCREMENT," +
                        "name VARCHAR(100) NOT NULL," +
                        "building_number VARCHAR(10)," +
                        "floor INT," +
                        "capacity INT)"},
                {"patient", "CREATE TABLE Patient (" +
                        "patient_id INT PRIMARY KEY," +
                        "insurance_id VARCHAR(50)," +
                        "notes TEXT," +
                        "FOREIGN KEY (patient_id) REFERENCES Person(person_id) ON DELETE CASCADE)"},
                {"staff", "CREATE TABLE Staff (" +
                        "staff_id INT PRIMARY KEY," +
                        "staff_role_id INT NOT NULL," +
                        "department_id INT NOT NULL," +
                        "hire_date DATE NOT NULL," +
                        "FOREIGN KEY (staff_id) REFERENCES Person(person_id) ON DELETE CASCADE," +
                        "FOREIGN KEY (staff_role_id) REFERENCES StaffRole(staff_role_id)," +
                        "FOREIGN KEY (department_id) REFERENCES Department(department_id))"},
                {"doctor", "CREATE TABLE Doctor (" +
                        "staff_id INT PRIMARY KEY," +
                        "specialty_id INT NOT NULL," +
                        "license_no VARCHAR(50) UNIQUE," +
                        "FOREIGN KEY (staff_id) REFERENCES Staff(staff_id) ON DELETE CASCADE," +
                        "FOREIGN KEY (specialty_id) REFERENCES Specialty(specialty_id))"},
                {"appointmentstatus", "CREATE TABLE AppointmentStatus (" +
                        "status_id INT PRIMARY KEY AUTO_INCREMENT," +
                        "status VARCHAR(30) NOT NULL UNIQUE)"},
                {"appointment", "CREATE TABLE Appointment (" +
                        "appointment_id INT PRIMARY KEY AUTO_INCREMENT," +
                        "patient_id INT NOT NULL," +
                        "doctor_id INT NOT NULL," +
                        "scheduled_at DATETIME NOT NULL," +
                        "reason VARCHAR(255)," +
                        "status_id INT NOT NULL," +
                        "FOREIGN KEY (patient_id) REFERENCES Patient(patient_id)," +
                        "FOREIGN KEY (doctor_id) REFERENCES Doctor(staff_id)," +
                        "FOREIGN KEY (status_id) REFERENCES AppointmentStatus(status_id))"},
                {"bill", "CREATE TABLE Bill (" +
                        "bill_no INT PRIMARY KEY AUTO_INCREMENT," +
                        "patient_id INT NOT NULL," +
                        "appointment_id INT," +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "FOREIGN KEY (patient_id) REFERENCES Patient(patient_id)," +
                        "FOREIGN KEY (appointment_id) REFERENCES Appointment(appointment_id))"}
        };

        DatabaseMetaData meta = connection.getMetaData();
        for (String[] entry : required) {
            String table = entry[0];
            try (ResultSet rs = meta.getTables(null, null, table, null)) {
                if (!rs.next()) {
                    try (Statement stmt = connection.createStatement()) {
                        stmt.executeUpdate(entry[1]);
                    }
                }
            }
        }
    }

    private static void createBillSummaryArtifacts(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("CREATE VIEW IF NOT EXISTS BillSummary AS " +
                    "SELECT bill_no, patient_id, '' AS first_name, '' AS last_name, " +
                    "appointment_id, created_at, 0 AS total_amount FROM Bill");
        } catch (SQLException ignored) { }

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("CREATE FUNCTION IF NOT EXISTS GetPatientBalance(p_patient_id INT) RETURNS DECIMAL(10,2) DETERMINISTIC RETURN 0");
        } catch (SQLException ignored) { }
    }

    private static void ensureSeedData(Connection connection) throws SQLException {
        ensureAppointmentStatusesSeed(connection);
        ensureDemoPatient(connection);
        ensureDemoDoctor(connection);
    }

    private static void ensureAppointmentStatusesSeed(Connection connection) throws SQLException {
        String[] statuses = {"Scheduled", "Completed", "Cancelled"};
        for (String status : statuses) {
            if (!valueExists(connection, "SELECT 1 FROM AppointmentStatus WHERE status = ?", status)) {
                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO AppointmentStatus(status) VALUES (?)")) {
                    ps.setString(1, status);
                    ps.executeUpdate();
                }
            }
        }
    }

    private static void ensureDemoPatient(Connection connection) throws SQLException {
        final String insurance = "DEMO-INS-001";
        if (valueExists(connection, "SELECT 1 FROM Patient WHERE insurance_id = ?", insurance)) {
            return;
        }
        int personId = insertPerson(connection, "Demo", "Patient", LocalDate.of(1995, 1, 1));
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO Patient (patient_id, insurance_id, notes) VALUES (?, ?, ?)")) {
            ps.setInt(1, personId);
            ps.setString(2, insurance);
            ps.setString(3, "Auto-generated demo patient");
            ps.executeUpdate();
        }
    }

    private static void ensureDemoDoctor(Connection connection) throws SQLException {
        final String license = "DEMO-LIC-001";
        if (valueExists(connection, "SELECT 1 FROM Doctor WHERE license_no = ?", license)) {
            return;
        }
        int staffRoleId = ensureStaffRole(connection, "Doctor");
        int departmentId = ensureDepartment(connection, "General Medicine", "A", 1, 20);
        int specialtyId = ensureSpecialty(connection, "General Medicine", new BigDecimal("150.00"));
        int personId = insertPerson(connection, "Demo", "Doctor", LocalDate.of(1980, 6, 15));

        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO Staff (staff_id, staff_role_id, department_id, hire_date) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, personId);
            ps.setInt(2, staffRoleId);
            ps.setInt(3, departmentId);
            ps.setDate(4, Date.valueOf(LocalDate.now().minusYears(5)));
            ps.executeUpdate();
        }

        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO Doctor (staff_id, specialty_id, license_no) VALUES (?, ?, ?)")) {
            ps.setInt(1, personId);
            ps.setInt(2, specialtyId);
            ps.setString(3, license);
            ps.executeUpdate();
        }
    }

    private static int ensureStaffRole(Connection connection, String name) throws SQLException {
        String select = "SELECT staff_role_id FROM StaffRole WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(select)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO StaffRole(name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Could not ensure staff role " + name);
    }

    private static int ensureDepartment(Connection connection, String name, String building, int floor, int capacity) throws SQLException {
        String select = "SELECT department_id FROM Department WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(select)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO Department(name, building_number, floor, capacity) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, building);
            ps.setInt(3, floor);
            ps.setInt(4, capacity);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Could not ensure department " + name);
    }

    private static int ensureSpecialty(Connection connection, String name, BigDecimal baseVisitFee) throws SQLException {
        String select = "SELECT specialty_id FROM Specialty WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(select)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO Specialty(name, base_visit_fee) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setBigDecimal(2, baseVisitFee);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Could not ensure specialty " + name);
    }

    private static int insertPerson(Connection connection, String firstName, String lastName, LocalDate dob) throws SQLException {
        String sql = "INSERT INTO Person (first_name, last_name, date_of_birth) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setTimestamp(3, Timestamp.valueOf(dob.atStartOfDay()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Could not insert person record");
    }

    private static boolean valueExists(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean assertEntityExists(Connection connection, String sql, int id, String label) throws SQLException {
        if (entityExists(connection, sql, id)) {
            return true;
        }
        System.out.println(label + " ID " + id + " does not exist. Operation cancelled.");
        return false;
    }

    private static boolean entityExists(Connection connection, String sql, int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}

import java.sql.*;

// HANDLES SELECT, UPDATE, INSERT, DELETE FOR PATIENT TABLE
public class PatientTable {
    private Connection connection;

    public PatientTable(Connection connection) {
        this.connection = connection;
    }

    // SELECT ALL PATIENTS
    public void listAllPatients() throws SQLException {
        String sql = "SELECT Person.first_name, Person.last_name, Person.date_of_birth, Person.person_id, "
                + "Patient.insurance_id, Patient.notes FROM Person JOIN Patient ON Patient.patient_id = Person.person_id";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            System.out.println("\n-- All Patients --");
            System.out.println("ID | Name | DOB | Insurance | Notes");
            while (rs.next()) {
                System.out.printf("%d | %s %s | %s | %s | %s%n",
                        rs.getInt("person_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getTimestamp("date_of_birth"),
                        rs.getString("insurance_id"),
                        rs.getString("notes"));
            }
        }
    }

    // SELECT INDIVIDUAL PATIENT BY ID
    public void selectPatientByID(int id) throws SQLException {
        String sql = "SELECT Person.first_name, Person.last_name, Person.date_of_birth, Person.person_id, "
                + "Patient.insurance_id, Patient.notes FROM Person JOIN Patient ON Patient.patient_id = Person.person_id "
                + "WHERE Person.person_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\n-- Patient Details --");
                if (rs.next()) {
                    System.out.printf("ID: %d%n", rs.getInt("person_id"));
                    System.out.printf("Name: %s %s%n", rs.getString("first_name"), rs.getString("last_name"));
                    System.out.printf("DOB: %s%n", rs.getTimestamp("date_of_birth"));
                    System.out.printf("Insurance: %s%n", rs.getString("insurance_id"));
                    System.out.printf("Notes: %s%n", rs.getString("notes"));
                } else {
                    System.out.println("No patient found with ID " + id);
                }
            }
        }
    }

    // UPDATE PATIENT NOTES
    public void updatePatientNotes(int id, String notes) throws SQLException {
        String sql = "UPDATE Patient SET notes = ? WHERE patient_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, notes);
            ps.setInt(2, id);
            int rows = ps.executeUpdate();
            System.out.println(rows + " patient record(s) updated.");
        }
    }

    // INSERT NEW PATIENT TO TABLE
    public void insertPatient(int p_id, String insurance_id, String notes) throws SQLException {
        String sql = "INSERT INTO Patient (patient_id, insurance_id, notes) VALUES(?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, p_id);
            ps.setString(2, insurance_id);
            if (notes == null || notes.trim().isEmpty()) {
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(3, notes);
            }
            int rows = ps.executeUpdate();
            System.out.println(rows + " patient record(s) inserted.");
        }
    }

    // DELETE PATIENT FROM TABLE
    public void deletePatient(int id) throws SQLException {
        String sql = "DELETE FROM Patient WHERE patient_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            System.out.println(rows + " patient record(s) deleted.");
        }
    }
}

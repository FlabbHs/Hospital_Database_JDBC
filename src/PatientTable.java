import java.sql.*;

// HANDLES SELECT, UPDATE, INSERT, DELETE FOR PATIENT TABLE
public class PatientTable {
    // SELECT ALL PATIENTS
    public void listAllPatients() throws SQLException {
        String sql = "SELECT Person.first_name, Person.last_name, Person.date_of_birth, Person.person_id,"
                + "Patient.insurance_id, Patient.notes FROM PERSON JOIN PATIENT ON Patient.patient_id = Person.person_id";
        PreparedStatement ps = myCon.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
    }

    // SELECT INDIVIDUAL PATIENT BY ID
    public void selectPatientByID(int id) throws SQLException {
        String sql = "SELECT Person.first_name, Person.last_name, Person.date_of_birth, Person.person_id,"
                + "Patient.insurance_id, Patient.notes FROM PERSON JOIN PATIENT ON Patient.patient_id = Person.person_id"
                + "WHERE Person.person_id = ?";
        PreparedStatement ps = myCon.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
    }

    // UPDATE PATIENT NOTES
    public void updatePatientNotes(int id, String notes) throws SQLException {
        String sql = "UPDATE Patient SET notes = ? WHERE patient_id = ?";
        PreparedStatement ps = myCon.prepareStatement(sql);
        ps.setString(1, notes);
        ps.setInt(2, id);
        int rs = ps.executeUpdate();
    }

    // INSERT NEW PATIENT TO TABLE
    public void insertPatient(int p_id, int i_id, String notes) throws SQLException {
        String sql = "INSERT INTO Patient VALUES(?, ?, ?)";
        PreparedStatement ps = myCon.prepareStatement(sql);
        ps.setInt(1, p_id);
        ps.setInt(2, i_id);
        ps.setString(3, notes);
        int rs = ps.executeUpdate();
    }

    // DELETE PATIENT FROM TABLE
    public void deletePatient(int id) throws SQLException {
        String sql = "DELETE FROM Patient WHERE patient_id = ?";
        PreparedStatement ps = myCon.prepareStatement(sql);
        ps.setInt(1, id);
        int rs = ps.executeUpdate();
    }
}

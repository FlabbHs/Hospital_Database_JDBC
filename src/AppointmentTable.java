import java.sql.*;

// HANDLES SELECT, UPDATE, INSERT, DELETE FOR APPOINTMENT TABLE
public class AppointmentTable {
    // SELECT ALL APPOINTMENTS
    public void listAllAppointments() throws SQLException {
        String sql = "SELECT Appointment.appointment_id, Appointment.patient_id, Appointment.doctor_id,"
                    + "Appointment.scheduled_at, Appointment.reason, AppointmentStatus.status"
                    + "FROM Appointment JOIN AppointmentStatus ON Appointment.status_id = AppointmentStatus.status_id";
        PreparedStatement ps = myCon.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        System.out.println("ID | Patient | Doctor | Date | Status | Reason");
        while (rs.next()) {
            System.out.printf("%d | %d | %d | %s | %s | %s%n",
                    rs.getInt("appointment_id"),
                    rs.getInt("patient_id"),
                    rs.getInt("doctor_id"),
                    rs.getTimestamp("scheduled_at").toString(),
                    rs.getString("status"),
                    rs.getString("reason")
            );
        }
    }

    // SELECT INDIVIDUAL APPOINTMENT BY ID
    public void selectPatientByID(int apptId) throws SQLException {
        String sql = "SELECT Appointment.appointment_id, Appointment.patient_id, Appointment.doctor_id,"
                + "Appointment.scheduled_at, Appointment.reason, AppointmentStatus.status"
                + "FROM Appointment JOIN AppointmentStatus ON Appointment.status_id = AppointmentStatus.status_id"
                + "WHERE Appointment.appointment_id = ?";
        PreparedStatement ps = myCon.prepareStatement(sql);
        ps.setInt(1, apptId);
        ResultSet rs = ps.executeQuery();
        System.out.println("ID | Patient | Doctor | Date | Status | Reason");
        if (rs.next()) {
            System.out.printf(
                    "Appointment %d: patient=%d doctor=%d at=%s status=%s reason=%s%n",
                    rs.getInt("appointment_id"),
                    rs.getInt("patient_id"),
                    rs.getInt("doctor_id"),
                    rs.getTimestamp("scheduled_at").toString(),
                    rs.getString("status"),
                    rs.getString("reason")
            );
        } else {
            System.out.println("No appointment found with ID " + apptId);
        }
    }

    // UPDATE APPOINTMENT STATUS
    public void updateAppointmentStatus(int apptId, int statusID, String status) throws SQLException {
        String sql = "UPDATE Patient SET notes = ? WHERE patient_id = ?";
        PreparedStatement ps = myCon.prepareStatement(sql);
        ps.setString(1, notes);
        ps.setInt(2, id);
        int rs = ps.executeUpdate();
    }

    // INSERT NEW APPOINTMENT TO TABLE
    public void insertPatient(int p_id, int i_id, String notes) throws SQLException {
        String sql = "INSERT INTO Patient VALUES(?, ?, ?)";
        PreparedStatement ps = myCon.prepareStatement(sql);
        ps.setInt(1, p_id);
        ps.setInt(2, i_id);
        ps.setString(3, notes);
        int rs = ps.executeUpdate();
    }

    // DELETE APPOINTMENT FROM TABLE
    public void deletePatient(int id) throws SQLException {
        String sql = "DELETE FROM Patient WHERE patient_id = ?";
        PreparedStatement ps = myCon.prepareStatement(sql);
        ps.setInt(1, id);
        int rs = ps.executeUpdate();
    }
}

import java.sql.*;

// HANDLES SELECT, UPDATE, INSERT, DELETE FOR APPOINTMENT TABLE
public class AppointmentTable {
    private Connection connection;

    public AppointmentTable(Connection connection) {
        this.connection = connection;
    }

    // SELECT ALL APPOINTMENTS
    public void listAllAppointments() throws SQLException {
        String sql = "SELECT Appointment.appointment_id, Appointment.patient_id, Appointment.doctor_id, "
                    + "Appointment.scheduled_at, Appointment.reason, AppointmentStatus.status "
                    + "FROM Appointment JOIN AppointmentStatus ON Appointment.status_id = AppointmentStatus.status_id";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            System.out.println("\n-- All Appointments --");
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
    }

    // SELECT INDIVIDUAL APPOINTMENT BY ID
    public void selectAppointmentByID(int apptId) throws SQLException {
        String sql = "SELECT Appointment.appointment_id, Appointment.patient_id, Appointment.doctor_id, "
                + "Appointment.scheduled_at, Appointment.reason, AppointmentStatus.status "
                + "FROM Appointment JOIN AppointmentStatus ON Appointment.status_id = AppointmentStatus.status_id "
                + "WHERE Appointment.appointment_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, apptId);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\n-- Appointment Details --");
                if (rs.next()) {
                    System.out.printf("ID: %d%n", rs.getInt("appointment_id"));
                    System.out.printf("Patient ID: %d%n", rs.getInt("patient_id"));
                    System.out.printf("Doctor ID: %d%n", rs.getInt("doctor_id"));
                    System.out.printf("Scheduled At: %s%n", rs.getTimestamp("scheduled_at").toString());
                    System.out.printf("Status: %s%n", rs.getString("status"));
                    System.out.printf("Reason: %s%n", rs.getString("reason"));
                } else {
                    System.out.println("No appointment found with ID " + apptId);
                }
            }
        }
    }

    // UPDATE APPOINTMENT STATUS
    public void updateAppointmentStatus(int apptId, int statusID) throws SQLException {
        String sql = "UPDATE Appointment SET status_id = ? WHERE appointment_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, statusID);
            ps.setInt(2, apptId);
            int rows = ps.executeUpdate();
            System.out.println(rows + " appointment record(s) updated.");
        }
    }

    // INSERT NEW APPOINTMENT TO TABLE
    public void insertAppointment(int patientId, int doctorId, Timestamp scheduledAt, String reason, int statusId) throws SQLException {
        String sql = "INSERT INTO Appointment (patient_id, doctor_id, scheduled_at, reason, status_id) VALUES(?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ps.setInt(2, doctorId);
            ps.setTimestamp(3, scheduledAt);
            if (reason == null || reason.trim().isEmpty()) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, reason);
            }
            ps.setInt(5, statusId);
            int rows = ps.executeUpdate();
            System.out.println(rows + " appointment record(s) inserted.");
        }
    }

    // DELETE APPOINTMENT FROM TABLE
    public void deleteAppointment(int id) throws SQLException {
        String sql = "DELETE FROM Appointment WHERE appointment_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            System.out.println(rows + " appointment record(s) deleted.");
        }
    }
}

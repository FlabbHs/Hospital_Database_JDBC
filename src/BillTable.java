import java.sql.*;

// HANDLES SELECT, INSERT, DELETE FOR BILL TABLE
public class BillTable {

    private Connection connection;

    public BillTable(Connection connection) {
        this.connection = connection;
    }

    // 1. LIST ALL BILLS FOR A PATIENT
    public void listBillsForPatient(int patientID) throws SQLException {
        String sql =
                "SELECT bill_no, patient_id, appointment_id, created_at " + "FROM Bill WHERE patient_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, patientID);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("BillNo | Patient | Appointment | CreatedAt");

                while (rs.next()) {
                    System.out.printf("%d | %d | %s | %s%n",
                            rs.getInt("bill_no"),
                            rs.getInt("patient_id"),
                            rs.getInt("appointment_id") == 0 ? "NULL" : rs.getInt("appointment_id"),
                            rs.getTimestamp("created_at").toString()
                    );
                }
            }
        }
    }

    // 2. SELECT A SPECIFIC BILL BY ID
    public void selectBillByID(int billNo) throws SQLException {
        String sql =
                "SELECT bill_no, patient_id, appointment_id, created_at " +
                        "FROM Bill WHERE bill_no = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, billNo);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    System.out.println("Bill Details:");
                    System.out.printf("Bill %d: patient=%d appointment=%s created_at=%s%n",
                            rs.getInt("bill_no"),
                            rs.getInt("patient_id"),
                            rs.getInt("appointment_id") == 0 ? "NULL" : rs.getInt("appointment_id"),
                            rs.getTimestamp("created_at").toString()
                    );
                } else {
                    System.out.println("No bill found with ID " + billNo);
                }
            }
        }
    }

    // 3. INSERT NEW BILL
    public void insertBill(int patientID, Integer appointmentID) throws SQLException {

        String sql = "INSERT INTO Bill (patient_id, appointment_id) VALUES (?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, patientID);

            // Allow NULL appointment
            if (appointmentID == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, appointmentID);
            }

            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new SQLException("Bill insert failed, no rows affected.");
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int newID = keys.getInt(1);
                    System.out.println("Bill inserted with ID: " + newID);
                }
            }
        }
    }

    // 4. DELETE BILL
    public void deleteBill(int billNo) throws SQLException {
        String sql = "DELETE FROM Bill WHERE bill_no = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, billNo);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Bill " + billNo + " has been deleted.");
            } else {
                System.out.println("No bill found with ID: " + billNo);
            }
        }
    }
}

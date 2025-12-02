import java.sql.*;
public class Main {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/Hospital";
        String username = "root";
        String password = "1q2w3e4r";
        String sql = "SELECT Person.first_name, Person.last_name, Person.date_of_birth, Person.person_id,"
                + "Patient.insurance_id, Patient.notes FROM PERSON JOIN PATIENT ON Patient.patient_id = Person.person_id";

        Class.forName("com.mysql.cj.jdbc.Driver");

        Connection myCon = DriverManager.getConnection(url, username, password);
        Statement mySt = myCon.createStatement();
        ResultSet rs = mySt.executeQuery(sql);

        while (rs.next()) {
            System.out.printf("%d | %s %s | %s | %s | %s%n",
                    rs.getInt("person_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getTimestamp("date_of_birth"),
                    rs.getString("insurance_id"),
                    rs.getString("notes"));
        }

        // HANDLES SELECT, UPDATE, INSERT, DELETE FOR PATIENT TABLE
        String selectPatient = "SELECT Person.first_name, Person.last_name, Person.date_of_birth, Person.person_id,"
                            + "Patient.insurance_id, Patient.notes FROM PERSON JOIN PATIENT ON Patient.patient_id = Person.person_id";


        // You MUST close manually
        rs.close();
        mySt.close();
        myCon.close();
    }
}


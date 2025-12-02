// filepath: c:\Users\auson\Desktop\Hospital\src\DB.java
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.io.InputStream;

public class DB {
    private static final String PROPS_PATH = "/db.properties"; // must be on classpath

    public static Connection getConnection() throws Exception {
        // Ensure driver present (older JVMs sometimes need this)
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignore) {
            // DriverManager can still load via service loader in newer JDKs
        }

        Properties props = new Properties();
        try (InputStream in = DB.class.getResourceAsStream(PROPS_PATH)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + PROPS_PATH + " on classpath. Create src/db.properties and ensure it's copied alongside classes.");
            }
            props.load(in);
        }

        String url = props.getProperty("url");
        String user = props.getProperty("user");
        String pass = props.getProperty("password");
        if (url == null || user == null || pass == null) {
            throw new IllegalStateException("db.properties must define url, user, and password");
        }
        return DriverManager.getConnection(url, user, pass);
    }
}


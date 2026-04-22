package com.radiantskin.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utility class for Oracle DB connection.
 * JDBC URL: jdbc:oracle:thin:@localhost:1521:XE
 * User: system | Password: 552005
 */
public class DBConnection {

    private static final String DRIVER = "oracle.jdbc.driver.OracleDriver";
    private static final String URL    = "jdbc:oracle:thin:@localhost:1521:XE";
    private static final String USER   = "system";
    private static final String PASS   = "552005";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Oracle JDBC Driver not found. Add ojdbc8.jar to WEB-INF/lib.", e);
        }
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
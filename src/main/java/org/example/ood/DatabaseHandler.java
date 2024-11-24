package org.example.ood;

import com.mysql.cj.jdbc.exceptions.CommunicationsException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseHandler {
    private static volatile DatabaseHandler instance;
    private static final String URL = Config.getProperties().getProperty("DB_URL");
    private static final String USER = Config.getProperties().getProperty("DB_USER");
    private static final String PASSWORD = Config.getProperties().getProperty("DB_PASSWORD");
    private static final int MAX_RETRIES = 3; // max retry attempts
    private static Connection connection;

    private DatabaseHandler() throws SQLException {
        connectWithRetry();
    }

    private static void connectWithRetry() throws SQLException {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                attempt++;
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Database connected successfully.");
                break;
            } catch (CommunicationsException e) {
                System.err.println("Attempt " + attempt + ": Communications link failure. Retrying...");
                if (attempt == MAX_RETRIES) {
                    System.err.println("Max retries reached. Could not establish a database connection.");
                    throw new SQLException("Unable to connect to database after " + MAX_RETRIES + " attempts.", e);
                }
                try {
                    Thread.sleep(2000); // wait for 2 seconds before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Thread interrupted during retry wait", ie);
                }
            } catch (ClassNotFoundException | SQLException ex) {
                System.err.println("Database connection creation failed: " + ex.getMessage());
                throw new SQLException("Error connecting to the database", ex);
            }
        }
    }

    public static DatabaseHandler getInstance() throws SQLException {
        if (instance == null) {
            synchronized (DatabaseHandler.class) {
                if (instance == null) {
                    instance = new DatabaseHandler();
                }
            }
        } else if (connection == null || connection.isClosed()) {
            synchronized (DatabaseHandler.class) {
                if (connection == null || connection.isClosed()) {
                    instance = new DatabaseHandler();
                }
            }
        }
        return instance;
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connectWithRetry();
        }
        return connection;
    }


//    public static void testConnection() {
//        try (Connection connection = getConnection()) {
//            if (connection != null && !connection.isClosed()) {
//                System.out.println("Connection successful!");
//            } else {
//                System.out.println("Failed to connect.");
//            }
//        } catch (SQLException e) {
//            System.err.println("Error connecting to the database: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
}



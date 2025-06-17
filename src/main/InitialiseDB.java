package src.main;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * InitialiseDB class creates and initializes the movie database schema.
 * It deletes any existing database file, creates a new one, and executes
 * the DDL statements to create all required tables.
 */
public class InitialiseDB {
    /**
     * Main method that initializes the database.
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        // Load JDBC driver
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found.");
            System.err.println("Please add sqlite-jdbc.jar to your classpath.");
            return;
        }
        
        String dbFile = "database.db";
        String ddlFile = "schema.ddl";
        
        // Delete existing database file if it exists
        File file = new File(dbFile);
        if (file.exists()) {
            if (!file.delete()) {
                System.err.println("Failed to delete the existing database file.");
                return;
            }
            System.out.println("Existing database file deleted.");
        }
        
        // Create a connection to an SQLite database
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile)) {
            if (connection == null) {
                System.err.println("Failed to create database connection.");
                return;
            }
            System.out.println("New database file created: " + dbFile);
            
            // Execute DDL statements
            executeDDL(connection, ddlFile);
            
            // Verify tables were created
            if (verifyTables(connection)) {
                System.out.println("OK - Database initialized successfully");
            } else {
                System.err.println("ERROR - Database initialization failed");
            }
            
        } catch (SQLException e) {
            System.err.println("SQL error while creating database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Executes the DDL statements from the specified file.
     * @param connection The database connection
     * @param ddlFile The path to the DDL file
     */
    private static void executeDDL(Connection connection, String ddlFile) {
        try (BufferedReader br = new BufferedReader(new FileReader(ddlFile));
            Statement statement = connection.createStatement()) {
            String line;
            StringBuilder sql = new StringBuilder();
            
            while ((line = br.readLine()) != null) { //Read DDL
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("--")) { // Ignore empty lines and comments
                    sql.append(line).append(" ");
                    if (line.endsWith(";")) { // Execute complete SQL statements
                        statement.executeUpdate(sql.toString());
                        sql.setLength(0); // Reset the StringBuilder
                    }
                }
            }
            System.out.println("DDL statements executed successfully.");
        } catch (IOException e) {
            System.err.println("Error reading DDL file: " + e.getMessage());
            throw new RuntimeException("DDL file reading failed", e);
        } catch (SQLException e) {
            System.err.println("SQL error executing DDL file: " + e.getMessage());
            throw new RuntimeException("DDL execution failed", e);
        }
    }
    
    /**
     * Verifies that all required tables were created by checking against all tables defined in the DDL.
     * @param connection The database connection
     * @return true if all tables exist, false otherwise
     */
    private static boolean verifyTables(Connection connection) {
        // List of all tables defined in the DDL file
        String[] expectedTables = {
            "Actors", 
            "Movies", 
            "Directors", 
            "Awards",
            "Movie_Actors",
            "Movie_Director",
            "Movie_Awards",
            "Actor_Awards",
            "Director_Awards"
        };
        
        try {
            DatabaseMetaData meta = connection.getMetaData();
            boolean allTablesExist = true;
            
            for (String table : expectedTables) {
                try (ResultSet rs = meta.getTables(null, null, table, null)) {
                    if (!rs.next()) {
                        System.err.println("Table not found: " + table);
                        allTablesExist = false;
                    } else {
                        System.out.println("Verified table exists: " + table);
                    }
                }
            }
            
            return allTablesExist;
        } catch (SQLException e) {
            System.err.println("Error verifying tables: " + e.getMessage());
            return false;
        }
    }
}
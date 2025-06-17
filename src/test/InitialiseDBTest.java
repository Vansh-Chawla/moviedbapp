package src.test;

import org.junit.*;

import src.main.InitialiseDB;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import static org.junit.Assert.*;

public class InitialiseDBTest {
    private static final String DB_FILE = "database.db";

    /**
     * Deletes any existing database file and initializes a fresh one before tests run.
     */
    @BeforeClass
    public static void setUp() {
        // Ensure the database file is deleted before the test starts
        File file = new File(DB_FILE);
        if (file.exists()) {
            assertTrue("Failed to delete existing database before test.", file.delete());
        }
        // Run the main method to initialize the database
        InitialiseDB.main(new String[]{});
    }

    /**
     * Verifies that the database file exists after initialization.
     */
    @Test
    public void testDatabaseFileExists() {
        File file = new File(DB_FILE);
        assertTrue("Database file should exist after initialization.", file.exists());
    }

    /**
     * Tests if the 'Actors' table was successfully created in the database.
     */
    @Test
    public void testTableCreation() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
            Statement statement = connection.createStatement()) {
    
            // Check if any expected table exists (e.g., 'Actors')
            ResultSet rs = statement.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='Actors';");
            
            assertTrue("Table 'Actors' should be created.", rs.next());
    
        } catch (Exception e) {
            fail("Database connection or query failed: " + e.getMessage());
        }
    }

    /**
     * Prints all tables in the database for debugging purposes.
     */
    @Test
    public void debugTables() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
            Statement statement = connection.createStatement()) {

            ResultSet rs = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table';");
            System.out.println("Tables found in database:");
            while (rs.next()) {
                System.out.println(rs.getString("name"));
            }

        } catch (Exception e) {
            fail("Failed to fetch tables: " + e.getMessage());
        }
    }

    /**
     * Tests database deletion and re-creation functionality.
     */
    @Test
    public void testDatabaseDeletion() {
        File file = new File(DB_FILE);
        if (file.exists()) {
            assertTrue("Database file should be deleted before re-creation", file.delete());
        }
        InitialiseDB.main(new String[]{});
        assertTrue("Database file should be created again", file.exists());
    }
    
    /**
     * Deletes the test database file after all tests complete.
     */
    @AfterClass
    public static void tearDown() {
        // Clean up database file after tests
        File file = new File(DB_FILE);
        if (file.exists()) {
            assertTrue("Failed to delete database after tests.", file.delete());
        }
    }
}

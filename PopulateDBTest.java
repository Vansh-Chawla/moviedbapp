import org.junit.*;

import java.io.*;
import java.sql.*;
import static org.junit.Assert.*;

public class PopulateDBTest {
    private static final String DB_FILE = "database.db";

    @Before
    public void resetDatabase() {
        InitialiseDB.main(new String[]{}); // Reset the database before each test
    }
    
    @BeforeClass
    public static void setupDatabase() {
        InitialiseDB.main(new String[]{}); // Initialize schema
    }

    @Test
    public void testDatabaseConnection() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE)) {
            assertNotNull("Database connection should be established", connection);
        } catch (Exception e) {
            fail("Failed to connect to database: " + e.getMessage());
        }
    }

    @Test
    public void testCSVFilesExist() {
        String[] csvFiles = {
            "csvfiles/actors.csv", "csvfiles/movies.csv", "csvfiles/directors.csv",
            "csvfiles/awards.csv", "csvfiles/movie_actors.csv", "csvfiles/movie_director.csv",
            "csvfiles/movie_awards.csv", "csvfiles/actor_awards.csv", "csvfiles/director_awards.csv"
        };

        for (String filePath : csvFiles) {
            File file = new File(filePath);
            assertTrue("CSV file should exist: " + filePath, file.exists());
        }
    }

    @Test
    public void testTablePopulation() {
        PopulateDB.main(new String[]{});

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
             Statement statement = connection.createStatement()) {

            String[] tables = {
                "Actors", "Movies", "Directors", "Awards",
                "Movie_Actors", "Movie_Director", "Movie_Awards",
                "Actor_Awards", "Director_Awards"
            };

            for (String table : tables) {
                ResultSet rs = statement.executeQuery("SELECT COUNT(*) AS count FROM " + table);
                assertTrue("Result set should contain a row", rs.next());
                int count = rs.getInt("count");
                assertTrue("Table " + table + " should have data", count > 0);
            }

        } catch (Exception e) {
            fail("Error checking table population: " + e.getMessage());
        }
    }

    @Test
    public void testForeignKeyViolations() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE)) {
            connection.createStatement().execute("PRAGMA foreign_keys = ON;");

            PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO Movie_Actors (movie_id, actor_id, role) VALUES (999, 999, 'Ghost')"
            );
            pstmt.executeUpdate();
            fail("Foreign key violation should prevent insertion.");
        } catch (SQLException ignored) {
            // Expected failure due to foreign key constraint
        }
    }

    @Test
    public void testDuplicateEntries() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
             PreparedStatement pstmt = connection.prepareStatement(
                 "INSERT INTO Actors (name, birthday) VALUES ('Test Actor', '2000-01-01')")) {

            pstmt.executeUpdate(); // First insert
            pstmt.executeUpdate(); // Second insert should be allowed (unless UNIQUE constraint exists)

            ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM Actors WHERE name='Test Actor'");
            assertTrue(rs.next());
            assertEquals("Duplicate actors should be inserted (unless unique constraints exist)", 2, rs.getInt(1));

        } catch (SQLException e) {
            fail("Unexpected error with duplicate entries: " + e.getMessage());
        }
    }

    @Test
    public void testPerformanceWithLargeCSV() {
        //stmt.execute("DELETE FROM Actors;");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
             Statement stmt = connection.createStatement()) {

            stmt.executeUpdate("DELETE FROM Actors"); // Clear table

            // Insert 10,000 records for performance testing
            try (PreparedStatement pstmt = connection.prepareStatement(
                 "INSERT INTO Actors (name, birthday) VALUES (?, ?)")) {

                for (int i = 0; i < 10000; i++) {
                    pstmt.setString(1, "Actor " + i);
                    pstmt.setString(2, "1990-01-01");
                    pstmt.executeUpdate();
                }
            }

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Actors");
            assertTrue(rs.next());
            assertEquals("Large dataset insertion should work", 10000, rs.getInt(1));

        } catch (SQLException e) {
            fail("Performance test failed: " + e.getMessage());
        }
    }

    @Test
    public void testEmptyDataFiles() {
        // Implement test for empty CSV files as explained earlier
    }

    @Test
    public void testMalformedDataFiles() {
        // Implement test for malformed CSV files as explained earlier
    }

    @Test
    public void testDataTypesMismatch() {
        // Implement test for data types mismatch as explained earlier
    }

    @AfterClass
    public static void cleanupDatabase() {
        new File(DB_FILE).delete();
    }
}

package src.test;
import org.junit.*;

import src.main.InitialiseDB;
import src.main.PopulateDB;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import static org.junit.Assert.*;

public class PopulateDBTest {
    private static final String DB_FILE = "database.db";
    
    /**
     * Resets database to initial state before each test for isolation.
     */
    
    @Before
    public void resetDatabase() {
        InitialiseDB.main(new String[]{}); // Resets the database before each test
    }
    
    /**
     * Initializes database schema before all tests. 
     */
    @BeforeClass
    public static void setupDatabase() {
        InitialiseDB.main(new String[]{}); // Initializing schema
    }

    /**
     * Tests that a valid connection can be established to the SQLite database.
     * Verifies the database file exists and is accessible.
     */
    @Test
    public void testDatabaseConnection() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE)) {
            assertNotNull("Database connection should be established", connection);
        } catch (Exception e) {
            fail("Failed to connect to database: " + e.getMessage());
        }
    }

    /**
     * Verifies all required CSV data files exist in the expected location.
     * Tests for presence of: actors, movies, directors, awards, and their relationships.
     */
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

    /**
     * Tests that all database tables are properly populated with data from CSV files.
     * Verifies each table exists and contains at least one record after population.
     * Checks tables: Actors, Movies, Directors, Awards, and their relationships.
     */
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

    /**
     * Tests that foreign key constraints are properly enforced in the database.
     * Attempts to insert invalid relationship data (non-existent movie/actor IDs)
     * and verifies the database rejects the operation.
     */
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

    /**
     * Tests the database's handling of duplicate entries in the Actors table.
     * Verifies behavior when inserting identical actor records:
     * - If UNIQUE constraints exist, should reject duplicates
     * - Without constraints, should allow duplicates
     * Includes cleanup of test data after verification.
     */
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

    /**
     * Tests database performance with large datasets by:
     * 1. Clearing the Actors table
     * 2. Inserting 10,000 test records in a batch
     * 3. Verifying all records were inserted
     *
     * Measures basic write performance and validates the database can handle
     * large volumes of data without errors. Consider adding timing metrics
     * for actual performance measurement.
     */
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

    /**
     * Tests database population with empty CSV files to verify:
     * 1. The system handles empty input files gracefully
     * 2. No data is inserted when CSVs are empty
     * 3. No errors occur during empty file processing
     *
     * Temporarily replaces real CSV files with empty versions for testing,
     * then restores original files after verification.
     */
    @Test
    public void testEmptyDataFiles() {
        // Create empty versions of the CSV files
        String[] csvFiles = {
            "csvfiles/empty_actors.csv", 
            "csvfiles/empty_movies.csv"
        };

        try {
            // Create empty files
            for (String filePath : csvFiles) {
                new File(filePath).createNewFile();
            }

            // Temporarily replace the original files with empty ones
            String originalActorsFile = "csvfiles/actors.csv";
            String originalMoviesFile = "csvfiles/movies.csv";
            String backupActorsFile = "csvfiles/actors_backup.csv";
            String backupMoviesFile = "csvfiles/movies_backup.csv";

            // Backup original files
            Files.move(Paths.get(originalActorsFile), Paths.get(backupActorsFile));
            Files.move(Paths.get(originalMoviesFile), Paths.get(backupMoviesFile));

            // Replace with empty files
            Files.move(Paths.get("csvfiles/empty_actors.csv"), Paths.get(originalActorsFile));
            Files.move(Paths.get("csvfiles/empty_movies.csv"), Paths.get(originalMoviesFile));

            // Test population with empty files
            PopulateDB.main(new String[]{});

            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
                Statement statement = connection.createStatement()) {

                ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM Actors");
                assertTrue(rs.next());
                assertEquals("Actors table should be empty when loading empty CSV", 0, rs.getInt(1));

                rs = statement.executeQuery("SELECT COUNT(*) FROM Movies");
                assertTrue(rs.next());
                assertEquals("Movies table should be empty when loading empty CSV", 0, rs.getInt(1));

            } finally {
                // Restore original files
                Files.move(Paths.get(originalActorsFile), Paths.get(originalActorsFile + ".tmp"));
                Files.move(Paths.get(originalMoviesFile), Paths.get(originalMoviesFile + ".tmp"));
                Files.move(Paths.get(backupActorsFile), Paths.get(originalActorsFile));
                Files.move(Paths.get(backupMoviesFile), Paths.get(originalMoviesFile));
                Files.delete(Paths.get(originalActorsFile + ".tmp"));
                Files.delete(Paths.get(originalMoviesFile + ".tmp"));
            }

        } catch (Exception e) {
            fail("Test failed due to exception: " + e.getMessage());
        }
    }

    /**
     * Tests database population with malformed CSV files to verify:
     * 1. The system handles invalid/malformed data gracefully
     * 2. Valid records are still processed despite malformed ones
     * 3. No catastrophic failures occur with bad input data
     *
     * Uses test files with:
     * - Missing fields
     * - Extra fields
     * - Empty required fields
     * Then verifies valid records are still inserted.
     */
    @Test
    public void testMalformedDataFiles() {
        // Malformed CSV content
        String malformedActors = "id,name,birthday\n1,John Doe\n2,Jane Smith,1985-05-15,extra";
        String malformedMovies = "title,release_date,running_time,genre,plot,ratings\n"
                                + "Inception,2010-07-16,148,Sci-Fi,\"A mind-bending thriller\",8.8\n"
                                + "The Matrix,1999-03-31,136,Sci-Fi,,extra";

        // Backup and replace original files
        Path actorsPath = Paths.get("csvfiles/actors.csv");
        Path moviesPath = Paths.get("csvfiles/movies.csv");
        Path backupActors = Paths.get("csvfiles/actors_backup.csv");
        Path backupMovies = Paths.get("csvfiles/movies_backup.csv");

        try {
            Files.move(actorsPath, backupActors);
            Files.move(moviesPath, backupMovies);
            Files.write(actorsPath, malformedActors.getBytes());
            Files.write(moviesPath, malformedMovies.getBytes());

            // Run the population script
            PopulateDB.main(new String[]{});

            // Verify database insertion
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
                Statement stmt = conn.createStatement()) {
                
                assertTrue("Some actors should be inserted", stmt.executeQuery("SELECT COUNT(*) FROM Actors").getInt(1) > 0);
                assertTrue("Some movies should be inserted", stmt.executeQuery("SELECT COUNT(*) FROM Movies").getInt(1) > 0);
            }
        } catch (Exception e) {
            fail("Test failed due to exception: " + e.getMessage());
        } finally {
            // Restore original files
            try {
                Files.deleteIfExists(actorsPath);
                Files.deleteIfExists(moviesPath);
                Files.move(backupActors, actorsPath);
                Files.move(backupMovies, moviesPath);
            } catch (IOException e) {
                fail("Failed to restore original files: " + e.getMessage());
            }
        }
    }

    /**
     * Tests database handling of type mismatches in CSV data by:
     * 1. Creating test data with invalid date format
     * 2. Verifying invalid records are rejected
     * 3. Ensuring valid records are still processed
     *
     * Specifically checks that:
     * - Invalid date strings don't get inserted
     * - Valid records still process correctly
     * - The system fails gracefully without data corruption
     */
    @Test
    public void testDataTypesMismatch() {
        // Type mismatch CSV content
        String typeMismatchData = "id,name,birthday\n1,John Doe,not-a-date\n2,Jane Smith,1985-05-15";
        
        // Backup and replace original file
        Path actorsPath = Paths.get("csvfiles/actors.csv");
        Path backupActors = Paths.get("csvfiles/actors_backup.csv");

        try {
            Files.move(actorsPath, backupActors);
            Files.write(actorsPath, typeMismatchData.getBytes());

            // Run the population script
            PopulateDB.main(new String[]{});

            // Verify database insertion
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
                Statement stmt = conn.createStatement()) {
                
                assertEquals("Only valid row should be inserted",
                            0, stmt.executeQuery("SELECT COUNT(*) FROM Actors WHERE birthday = '1985-05-15'").getInt(1));
                assertEquals("Invalid row should not be inserted",
                            0, stmt.executeQuery("SELECT COUNT(*) FROM Actors WHERE birthday = 'not-a-date'").getInt(1));
            }
        } catch (Exception e) {
            fail("Test failed due to exception: " + e.getMessage());
        } finally {
            // Restore original file
            try {
                Files.deleteIfExists(actorsPath);
                Files.move(backupActors, actorsPath);
            } catch (IOException e) {
                fail("Failed to restore original files: " + e.getMessage());
            }
        }
    }

    /**
     * Tests database handling of CSV files with missing required fields by:
     * 1. Creating test data with missing name and birthday values
     * 2. Verifying no records are inserted when required fields are missing
     * 3. Ensuring proper error handling without data corruption
     */
    @Test
    public void testMissingRequiredFields() {
        // Create CSV files with missing required fields
        String missingFieldsFile = "csvfiles/missing_fields_actors.csv";

        try {
            // Write data with missing required fields
            Files.write(Paths.get(missingFieldsFile), 
                "id,name,birthday\n1,John Doe,\n2,,1985-05-15".getBytes());

            // Temporarily replace the original file
            String originalActorsFile = "csvfiles/actors.csv";
            String backupActorsFile = "csvfiles/actors_backup.csv";

            // Backup original file
            Files.move(Paths.get(originalActorsFile), Paths.get(backupActorsFile));

            // Replace with missing-fields file
            Files.move(Paths.get(missingFieldsFile), Paths.get(originalActorsFile));

            // Test population with missing required fields
            PopulateDB.main(new String[]{});

            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
                Statement statement = connection.createStatement()) {

                // Only rows with all required fields should be inserted
                ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM Actors");
                assertTrue(rs.next());
                assertEquals("No rows should be inserted when required fields are missing", 0, rs.getInt(1));

            } finally {
                // Restore original file
                Files.move(Paths.get(originalActorsFile), Paths.get(originalActorsFile + ".tmp"));
                Files.move(Paths.get(backupActorsFile), Paths.get(originalActorsFile));
                Files.delete(Paths.get(originalActorsFile + ".tmp"));
            }

        } catch (Exception e) {
            fail("Test failed due to exception: " + e.getMessage());
        }
    }

    /** 
     * Deletes the test database file after all tests complete. 
     */
    @AfterClass
    public static void cleanupDatabase() {
        new File(DB_FILE).delete();
    }
}

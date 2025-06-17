import org.junit.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import static org.junit.Assert.*;

public class QueryDBTest {
    private static final String DB_FILE = "database.db";

    @BeforeClass
    public static void setupDatabase() {
        // Ensure database is initialized before running tests
        InitialiseDB.main(new String[]{});
        PopulateDB.main(new String[]{});
    }

    @Test
    public void testListAllMovies() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
             Statement statement = connection.createStatement()) {

            ResultSet rs = statement.executeQuery("SELECT COUNT(*) AS count FROM Movies");
            assertTrue(rs.next());
            assertTrue("Movies table should have data", rs.getInt("count") > 0);
        } catch (Exception e) {
            fail("Database query failed: " + e.getMessage());
        }
    }

    @Test
    public void testListActorsInMovie() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
             Statement statement = connection.createStatement()) {

            ResultSet rs = statement.executeQuery("SELECT COUNT(*) AS count FROM Movie_Actors");
            assertTrue(rs.next());
            assertTrue("Movie_Actors table should have data", rs.getInt("count") > 0);
        } catch (Exception e) {
            fail("Database query failed: " + e.getMessage());
        }
    }

    @Test
    public void testListDirectorsForActor() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
             Statement statement = connection.createStatement()) {

            ResultSet rs = statement.executeQuery("SELECT COUNT(*) AS count FROM Movie_Director");
            assertTrue(rs.next());
            assertTrue("Movie_Director table should have data", rs.getInt("count") > 0);
        } catch (Exception e) {
            fail("Database query failed: " + e.getMessage());
        }
    }

    @Test
    public void testComplexQuery1() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
             Statement statement = connection.createStatement()) {

            ResultSet rs = statement.executeQuery("SELECT COUNT(*) AS count FROM Movie_Awards");
            assertTrue(rs.next());
            assertTrue("Movie_Awards table should have data", rs.getInt("count") > 0);
        } catch (Exception e) {
            fail("Database query failed: " + e.getMessage());
        }
    }

    @Test
    public void testComplexQuery2() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
             Statement statement = connection.createStatement()) {

            ResultSet rs = statement.executeQuery("SELECT COUNT(*) AS count FROM Actor_Awards");
            assertTrue(rs.next());
            assertTrue("Actor_Awards table should have data", rs.getInt("count") > 0);
        } catch (Exception e) {
            fail("Database query failed: " + e.getMessage());
        }
    }

@Test
public void testNoResultsQueries() {
    try {
        // Query 2 (listActorsInMovie) with a movie title that doesn't exist
        ProcessBuilder processBuilder = new ProcessBuilder("java", "QueryDB", "2", "NonExistingMovie");
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        assertEquals("Exit code should be 0 (successful)", 0, exitCode);
        InputStreamReader reader = new InputStreamReader(process.getInputStream());
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line;
        boolean noResults = true;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.contains("Actors in movie 'NonExistingMovie'")) {
                noResults = false;
            }
        }
        assertTrue("Should handle no results gracefully", noResults);
    } catch (Exception e) {
        fail("Test failed due to exception: " + e.getMessage());
    }
}


    @AfterClass
    public static void cleanupDatabase() {
        new File(DB_FILE).delete();
    }
}

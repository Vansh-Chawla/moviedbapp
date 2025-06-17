import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class QueryDB {
    /**
     * Queries the database based on the provided query number and parameters.
     * @param args The command line arguments.
     */
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: ./queryDB.sh <query_number> [additional_parameters]");
            return;
        }

        int queryNumber = Integer.parseInt(args[0]);
        String dbFile = "database.db"; // Name of the SQLite database file

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile)) {
            switch (queryNumber) {
                case 1:
                    listAllMovies(connection);
                    break;
                case 2:
                    if (args.length < 2) {
                        System.out.println("Usage: ./queryDB.sh 2 <movie_title>");
                        return;
                    }
                    listActorsInMovie(connection, args[1]);
                    break;
                case 3:
                    if (args.length < 3) {
                        System.out.println("Usage: ./queryDB.sh 3 <actor_name> <director_name>");
                        return;
                    }
                    listPlotsForActorAndDirector(connection, args[1], args[2]);
                    break;
                case 4:
                    if (args.length < 2) {
                        System.out.println("Usage: ./queryDB.sh 4 <actor_name>");
                        return;
                    }
                    listDirectorsForActor(connection, args[1]);
                    break;
                case 5:
                    complexQuery1(connection);
                    break;
                case 6:
                    complexQuery2(connection);
                    break;
                default:
                    System.out.println("Invalid query number. Please choose a number between 1 and 6.");
            }
        } catch (SQLException e) {
            System.err.println("Error querying database: " + e.getMessage());
        }
    }

/**
 * Queries the database to list all movies with numbered output.
 * @param connection The database connection.
 * @throws SQLException If an SQL error occurs.
 */
private static void listAllMovies(Connection connection) throws SQLException {
    String sql = "SELECT title FROM Movies ORDER BY title";  // Added ORDER BY for consistent ordering
    try (PreparedStatement pstmt = connection.prepareStatement(sql);
         ResultSet rs = pstmt.executeQuery()) {
        System.out.println("List of all movies:");
        int counter = 1;
        while (rs.next()) {
            System.out.println(counter + ". " + rs.getString("title"));
            counter++;
        }
        // Print message if no movies found
        if (counter == 1) {
            System.out.println("No movies found in the database.");
        }
    }
}

    /**
     * Queries the database to list actors in a specific movie.
     * @param connection The database connection.
     * @param movieTitle The title of the movie.
     * @throws SQLException If an SQL error occurs.
     */
    private static void listActorsInMovie(Connection connection, String movieTitle) throws SQLException {
        String sql = "SELECT a.name FROM Actors a " +
                     "JOIN Movie_Actors ma ON a.actor_id = ma.actor_id " +
                     "JOIN Movies m ON ma.movie_id = m.movie_id " +
                     "WHERE m.title = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, movieTitle);
            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.println("Actors in movie '" + movieTitle + "':");
                while (rs.next()) {
                    System.out.println(rs.getString("name"));
                }
            }
        }
    }

    /**
     * Queries the database to list plots for a specific actor and director.
     * @param connection The database connection.
     * @param actorName The name of the actor.
     * @param directorName The name of the director.
     * @throws SQLException If an SQL error occurs.
     */
    private static void listPlotsForActorAndDirector(Connection connection, String actorName, String directorName) throws SQLException {
        String sql = "SELECT m.plot FROM Movies m " +
                     "JOIN Movie_Actors ma ON m.movie_id = ma.movie_id " +
                     "JOIN Actors a ON ma.actor_id = a.actor_id " +
                     "JOIN Movie_Director md ON m.movie_id = md.movie_id " +
                     "JOIN Directors d ON md.director_id = d.director_id " +
                     "WHERE a.name = ? AND d.name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, actorName);
            pstmt.setString(2, directorName);
            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.println("Plots of movies with actor '" + actorName + "' and director '" + directorName + "':");
                while (rs.next()) {
                    System.out.println(rs.getString("plot"));
                }
            }
        }
    }

    /**
     * Queries the database to list directors for a specific actor.
     * @param connection The database connection.
     * @param actorName The name of the actor.
     * @throws SQLException If an SQL error occurs.
     */
    private static void listDirectorsForActor(Connection connection, String actorName) throws SQLException {
        String sql = "SELECT d.name FROM Directors d " +
                     "JOIN Movie_Director md ON d.director_id = md.director_id " +
                     "JOIN Movies m ON md.movie_id = m.movie_id " +
                     "JOIN Movie_Actors ma ON m.movie_id = ma.movie_id " +
                     "JOIN Actors a ON ma.actor_id = a.actor_id " +
                     "WHERE a.name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, actorName);
            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.println("Directors of movies with actor '" + actorName + "':");
                while (rs.next()) {
                    System.out.println(rs.getString("name"));
                }
            }
        }
    }

    /**
     * Complex query 1: List movies that have won an Oscar and have a rating between 7.0-9.0 and lists the number of Oscars won.
     * @param connection The database connection.
     * @throws SQLException If an SQL error occurs.
     */
    private static void complexQuery1(Connection connection) throws SQLException {
        String sql = "SELECT m.title, m.ratings, COUNT(a.award_id) AS oscar_count " +
                     "FROM Movies m " +
                     "JOIN Movie_Awards ma ON m.movie_id = ma.movie_id " +
                     "JOIN Awards a ON ma.award_id = a.award_id " +
                     "WHERE a.name = 'Oscar' " +
                     "AND m.ratings BETWEEN 7.0 AND 9.0 " +
                     "GROUP BY m.movie_id, m.title, m.ratings " +
                     "ORDER BY m.ratings DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            System.out.println("Movies with ratings between 7 and 9 that have won Oscars:");
            int counter = 1;
            while (rs.next()) {
                String title = rs.getString("title");
                double rating = rs.getDouble("ratings");
                int oscarCount = rs.getInt("oscar_count");
                System.out.printf("%d. %s (Rating: %.1f) - %d Oscar%s%n", 
                                counter++, title, rating, oscarCount,
                                oscarCount > 1 ? "s" : ""); // Pluralize "Oscar" if needed
            }
            
            if (counter == 1) {
                System.out.println("No movies found matching the criteria.");
            }
        }
    }

    /**
     * Complex query 2: Listing all actors who have 2 or more awards and have starred movies with ratings of 8 and above.
     * @param connection The database connection.
     * @throws SQLException If an SQL error occurs.
     */
    private static void complexQuery2(Connection connection) throws SQLException {
        String sql = "SELECT DISTINCT a.name AS actor_name " +
                     "FROM Actors a " +
                     "JOIN Movie_Actors ma ON a.actor_id = ma.actor_id " +
                     "JOIN Movies m ON ma.movie_id = m.movie_id " +
                     "JOIN Actor_Awards aa ON a.actor_id = aa.actor_id " +
                     "WHERE m.ratings > 8.0 " +
                     "GROUP BY a.actor_id, a.name " +
                     "HAVING COUNT(DISTINCT aa.award_id) >= 2 " +
                     "ORDER BY COUNT(DISTINCT ma.movie_id) DESC, " +
                     "COUNT(DISTINCT aa.award_id) DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            System.out.println("Actors who won 2 or more awards and starred in >8.0 rated movies:");
            int counter = 1;
            while (rs.next()) {
                System.out.println(counter + ". " + rs.getString("actor_name"));
                counter++;
            }
            
            if (counter == 1) {
                System.out.println("No actors found matching the criteria.");
            }
        }
    }
}
import java.io.*;
import java.sql.*;
import java.util.Scanner;
import java.util.regex.Pattern;

public class PopulateDB {
    /**
     * Populates the database with data from CSV files.
     * The CSV files should be in the format:
     * 1. actors.csv: name, birthday
     * 2. movies.csv: title, release_date, running_time, genre, plot, ratings
     * 3. directors.csv: name, birthday
     * 4. awards.csv: name, category
     * 5. movie_actors.csv: movie_id, actor_id
     * 6. movie_director.csv: movie_id, director_id
     * 7. movie_awards.csv: movie_id, award_id
     * 8. actor_awards.csv: actor_id, award_id
     * 9. director_awards.csv: director_id, award_id
     * @param args
     */
    public static void main(String[] args) {
        String dbFile = "database.db";
        
        // First check if database exists
        File db = new File(dbFile);
        if (!db.exists()) {
            System.err.println("Database file does not exist. Please run InitialiseDB.");
            System.out.println("Enter 0 if you want to initialise the database and retry or any other key to exit.");
            
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine().trim();
            
            if (input.equals("0")) {
                try {
                    // Run the initialiseDB.sh script
                    ProcessBuilder pb = new ProcessBuilder("./initialiseDB.sh");
                    pb.inheritIO(); // This makes the script's output visible in the console
                    Process process = pb.start();
                    int exitCode = process.waitFor();
                    
                    if (exitCode == 0) {
                        System.out.println("Database initialized successfully. Retrying PopulateDB...");
                        main(args); // Recursively call main to retry
                        return;
                    } else {
                        System.err.println("Failed to initialize database. Exiting.");
                        System.exit(1);
                    }
                } catch (IOException | InterruptedException e) {
                    System.err.println("Error running initialiseDB.sh: " + e.getMessage());
                    System.exit(1);
                }
            } else {
                System.out.println("exited");
                System.exit(0);
            }
        }

        String[] csvFiles = {
            "csvfiles/actors.csv", "csvfiles/movies.csv", "csvfiles/directors.csv",
            "csvfiles/awards.csv", "csvfiles/movie_actors.csv", "csvfiles/movie_director.csv",
            "csvfiles/movie_awards.csv", "csvfiles/actor_awards.csv", "csvfiles/director_awards.csv"
        };
        String[] insertSQLs = {
            "INSERT INTO Actors (name, birthday) VALUES (?, ?)",
            "INSERT INTO Movies (title, release_date, running_time, genre, plot, ratings) VALUES (?, ?, ?, ?, ?, ?)",
            "INSERT INTO Directors (name, birthday) VALUES (?, ?)",
            "INSERT INTO Awards (name, category) VALUES (?, ?)",
            "INSERT INTO Movie_Actors (movie_id, actor_id) VALUES (?, ?)",
            "INSERT INTO Movie_Director (movie_id, director_id) VALUES (?, ?)",
            "INSERT INTO Movie_Awards (movie_id, award_id) VALUES (?, ?)",
            "INSERT INTO Actor_Awards (actor_id, award_id) VALUES (?, ?)",
            "INSERT INTO Director_Awards (director_id, award_id) VALUES (?, ?)"
        };
        
        // Tables to clear in order that respects foreign key constraints
        String[] tablesToClear = {
            "Actor_Awards", "Director_Awards", "Movie_Awards",
            "Movie_Actors", "Movie_Director",
            "Actors", "Directors", "Movies", "Awards"
        };
        
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile)) {
            Statement stmt = connection.createStatement();
            stmt.execute("PRAGMA foreign_keys = ON;");
            
            // Clear all existing data
            System.out.println("Clearing existing data...");
            for (String table : tablesToClear) {
                try {
                    stmt.execute("DELETE FROM " + table + ";");
                    System.out.println("Cleared table: " + table);
                } catch (SQLException e) {
                    System.err.println("Error clearing table " + table + ": " + e.getMessage());
                }
            }
            
            // Reset auto-increment counters (SQLite specific)
            try {
                stmt.execute("DELETE FROM sqlite_sequence;");
            } catch (SQLException e) {
                System.out.println("Note: sqlite_sequence table doesn't exist or couldn't be cleared");
            }
            
            // Populate with new data
            for (int i = 0; i < csvFiles.length; i++) {
                boolean isUploaded = populateTable(connection, csvFiles[i], insertSQLs[i]);
                if (isUploaded) {
                    System.out.println(csvFiles[i] + " uploaded successfully.");
                }
            }
            System.out.println("Database repopulated successfully.");
        } catch (Exception e) {
            System.err.println("Error populating database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Populates a table in the database with data from a CSV file.
     * @param connection The database connection.
     * @param csvFile The path to the CSV file.
     * @param insertSQL The SQL insert statement.
     * @return true if data was uploaded, false otherwise.
     */
    private static boolean populateTable(Connection connection, String csvFile, String insertSQL) {
        boolean isUploaded = false;
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile));
             PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
    
            String line;
            boolean firstLine = true;
            boolean hasData = false;
            int expectedColumns = insertSQL.split("\\?").length - 1;
            Pattern pattern = Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                if (line.trim().isEmpty()) continue;
                
                // Using regex to split only on commas not inside quotes
                String[] data = pattern.split(line, -1);
                
                // Clean up quoted fields
                for (int i = 0; i < data.length; i++) {
                    data[i] = data[i].trim();
                    if (data[i].startsWith("\"") && data[i].endsWith("\"")) {
                        data[i] = data[i].substring(1, data[i].length() - 1);
                    }
                }

                // Verify column count
                if (data.length != expectedColumns) {
                    System.err.println("Column mismatch in " + csvFile + ": expected " + 
                                     expectedColumns + " but got " + data.length);
                    System.err.println("Problem line: " + line);
                    continue;
                }

                // Set parameters
                for (int i = 0; i < data.length; i++) {
                    String value = data[i].trim().isEmpty() ? null : data[i].trim();
                    pstmt.setString(i + 1, value);
                }
                
                try {
                    pstmt.executeUpdate();
                    hasData = true;
                } catch (SQLException e) {
                    System.err.println("Error inserting row: " + line);
                    System.err.println(e.getMessage());
                }
            }
    
            isUploaded = hasData;
        } catch (Exception e) {
            System.err.println("Error populating table from " + csvFile + ": " + e.getMessage());
            e.printStackTrace();
        }
        return isUploaded;
    }
}
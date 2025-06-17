package src.main;
import java.io.*;
import java.sql.*;
import java.util.Scanner;
import java.util.regex.Pattern;

public class PopulateDB {
    /**
     * Populates the database with data from CSV files.
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
                    // Handle SQL exceptions with error codes
                    if (e.getErrorCode() == 1) { // SQLITE_ERROR
                        System.err.println("Error: Table " + table + " doesn't exist or cannot be cleared.");
                    } else if (e.getErrorCode() == 8) { // SQLITE_READONLY
                        System.err.println("Error: Database is read-only. Cannot clear table " + table);
                    } else {
                        System.err.println("SQL Error " + e.getErrorCode() + " clearing table " + table + ": " + e.getMessage());
                    }
                }
            }
            
            // Reset auto-increment counters (SQLite specific)
            try {
                stmt.execute("DELETE FROM sqlite_sequence;");
            } catch (SQLException e) {
                if (e.getErrorCode() == 1) { // SQLITE_ERROR
                    System.out.println("Note: sqlite_sequence table doesn't exist (this is normal for empty databases)");
                } else {
                    System.err.println("SQL Error " + e.getErrorCode() + " clearing sqlite_sequence: " + e.getMessage());
                }
            }
            
            // Populate with new data
            for (int i = 0; i < csvFiles.length; i++) {
                boolean isUploaded = populateTable(connection, csvFiles[i], insertSQLs[i]);
                if (isUploaded) {
                    System.out.println(csvFiles[i] + " uploaded successfully.");
                }
            }
            System.out.println("Database repopulated successfully.");
        } catch (SQLException e) {
            // Handle connection errors
            if (e.getErrorCode() == 0) { // SQLITE_CANTOPEN
                System.err.println("Error: Cannot open database file. Check permissions or file path.");
            } else if (e.getErrorCode() == 14) { // SQLITE_CANTOPEN
                System.err.println("Error: Database is locked by another process.");
            } else {
                System.err.println("SQL Error " + e.getErrorCode() + " connecting to database: " + e.getMessage());
            }
            e.printStackTrace();
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
                    // Handle specific SQLite error codes
                    if (e.getErrorCode() == 19) { // SQLITE_CONSTRAINT (foreign key violation)
                        System.err.println("Foreign key constraint violation inserting row: " + line);
                        System.err.println("Details: " + e.getMessage());
                    } else if (e.getErrorCode() == 1) { // SQLITE_ERROR (general error)
                        System.err.println("SQL error inserting row: " + line);
                        System.err.println("Details: " + e.getMessage());
                    } else if (e.getErrorCode() == 1299) { // SQLITE_TOOBIG
                        System.err.println("Data too large for column in row: " + line);
                    } else {
                        System.err.println("SQL Error " + e.getErrorCode() + " inserting row: " + line);
                        System.err.println("Details: " + e.getMessage());
                    }
                }
            }
    
            isUploaded = hasData;
        } catch (FileNotFoundException e) {
            System.err.println("Error: CSV file not found: " + csvFile);
        } catch (IOException e) {
            System.err.println("Error reading CSV file " + csvFile + ": " + e.getMessage());
        } catch (SQLException e) {
            if (e.getErrorCode() == 0) { // SQLITE_CANTOPEN
                System.err.println("Error: Cannot open database file while processing " + csvFile);
            } else {
                System.err.println("SQL Error " + e.getErrorCode() + " preparing statement for " + csvFile + ": " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Unexpected error populating table from " + csvFile + ": " + e.getMessage());
            e.printStackTrace();
        }
        return isUploaded;
    }
}
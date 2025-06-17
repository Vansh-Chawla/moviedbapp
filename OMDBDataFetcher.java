import java.io.*;
import java.net.URLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;

public class OMDBDataFetcher {
    private static final String API_KEY = "203acc83";
    private static final String OMDB_URL = "http://www.omdbapi.com/?apikey=" + API_KEY + "&t=";
    public static void main(String[] args) {
        String[] movieTitles = {"The Shawshank Redemption", "Inception", "The Dark Knight", "Pulp Fiction", "Fight Club",
    "Forrest Gump", "The Matrix", "Interstellar", "Parasite", "The Godfather",
    "Gladiator", "Titanic", "Avengers: Endgame", "Jurassic Park", "The Lion King",
    "Toy Story", "The Social Network", "La La Land", "Whiplash", "Black Panther"};
        
        try (BufferedWriter actorWriter = new BufferedWriter(new FileWriter("csvfiles/actors.csv"));
             BufferedWriter movieWriter = new BufferedWriter(new FileWriter("csvfiles/movies.csv"));
             BufferedWriter directorWriter = new BufferedWriter(new FileWriter("csvfiles/directors.csv"));
             BufferedWriter awardWriter = new BufferedWriter(new FileWriter("csvfiles/awards.csv"));
             BufferedWriter movieActorWriter = new BufferedWriter(new FileWriter("csvfiles/movie_actors.csv"));
             BufferedWriter movieDirectorWriter = new BufferedWriter(new FileWriter("csvfiles/movie_director.csv"));
             BufferedWriter movieAwardWriter = new BufferedWriter(new FileWriter("csvfiles/movie_awards.csv"));
             BufferedWriter actorAwardWriter = new BufferedWriter(new FileWriter("csvfiles/actor_awards.csv"));
             BufferedWriter directorAwardWriter = new BufferedWriter(new FileWriter("csvfiles/director_awards.csv"))) {

            // Write headers to CSV files
            actorWriter.write("name,birthday\n");
            movieWriter.write("title,release_date,running_time,genre,plot,ratings\n");
            directorWriter.write("name,birthday\n");
            awardWriter.write("name,category\n");
            movieActorWriter.write("movie_id,actor_id\n");
            movieDirectorWriter.write("movie_id,director_id\n");
            movieAwardWriter.write("movie_id,award_id\n");
            actorAwardWriter.write("actor_id,award_id\n");
            directorAwardWriter.write("director_id,award_id\n");

            for (String title : movieTitles) {
                JSONObject movieData = fetchMovieData(title);
                if (movieData != null) {
                    // Write movie data
                    String movieTitle = movieData.getString("Title");
                    String releaseDate = convertDateFormat(movieData.getString("Released"));
                    String runtime = movieData.getString("Runtime").replace(" min", "");
                    String genre = movieData.getString("Genre");
                    String plot = movieData.getString("Plot");
                    String ratings = movieData.getJSONArray("Ratings").getJSONObject(0).getString("Value").split("/")[0];

                    movieWriter.write(String.format("\"%s\",%s,%s,\"%s\",\"%s\",%s\n", 
                        movieTitle, releaseDate, runtime, genre, plot, ratings));

                    // Write director data
                    String directorName = movieData.getString("Director");
                    directorWriter.write(String.format("%s,\n", directorName));

                    // Write actor data
                    String[] actors = movieData.getString("Actors").split(", ");
                    for (String actor : actors) {
                        actorWriter.write(String.format("%s,\n", actor));
                    }

                    // Write award data (assuming awards are in the response)
                    if (movieData.has("Awards")) {
                        String awards = movieData.getString("Awards");
                        awardWriter.write(String.format("%s,%s\n", awards, "Best Picture"));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static JSONObject fetchMovieData(String title) {
        try {
            URL url = new URL(OMDB_URL + title.replace(" ", "+"));
            URLConnection con = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            return new JSONObject(content.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String convertDateFormat(String oldDate) {
        try {
            SimpleDateFormat oldFormat = new SimpleDateFormat("dd MMM yyyy"); // Input format
            SimpleDateFormat newFormat = new SimpleDateFormat("yyyy-MM-dd"); // Output format

            Date date = oldFormat.parse(oldDate);
            return newFormat.format(date); // Convert to "YYYY-MM-DD"
        } catch (ParseException e) {
            e.printStackTrace();
            return oldDate; // Return original if parsing fails
        }
    }

}
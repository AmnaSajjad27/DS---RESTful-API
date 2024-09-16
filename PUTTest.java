import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.NoSuchFileException;

public class PUTTest 
{

    public boolean put_request(String file_path) 
    {
        // Start the content server to send the PUT request
        ContentServer content_server = new ContentServer();
        content_server.start();

        String string_1 = ""; // Content from content server
        String string_2 = ""; // Content from aggregation server

        try
        {
            // Wait for the aggregation server to process the request
            Thread.sleep(1000);

            // Get the contents of the input we sent from the content server
            Path contentServerPath = Path.of(file_path);
            Path aggregationServerPath = Path.of("LatestWeatherData.json");

            // Ensure both files exist before proceeding
            if (!Files.exists(contentServerPath)) {
                System.err.println("ContentServer file not found: " + file_path);
                return false;
            }
            if (!Files.exists(aggregationServerPath)) {
                System.err.println("Aggregation server file not found: LatestWeatherData.json");
                return false;
            }

            // Read the files
            string_1 = Files.readString(contentServerPath);
            string_2 = Files.readString(aggregationServerPath);

            // Remove all whitespaces and newlines before comparison
            string_1 = string_1.replaceAll("\\s+", "");
            string_2 = string_2.replaceAll("\\s+", "");

        } catch (NoSuchFileException e) {
            System.err.println("File not found: " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            System.err.println("Thread interrupted: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.toString());
            return false;
        }

        // Compare if the contents in both files match
        if (string_1.equals(string_2)) {
            System.out.println("PUT request successful. Files match!");
            return true;
        } else {
            System.out.println("PUT request failed. Files do not match.");
            return false;
        }
    }

    public static void main(String[] args) {
        PUTTest put = new PUTTest();
        boolean result = put.put_request("test/ContentServer1.json");
        System.out.println("\033[0;1mPut request works? " + result + "\033[0m");
    }
}

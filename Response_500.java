import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.InputStreamReader; // Add this import


public class Response_500 
{
    // This method sends the contents of the specified JSON file and checks if the aggregation server
    // responds with a 500 status code when the JSON is invalid.
    public boolean invalidPUTrequest(String fileLoc) 
    {
        ContentServer cs = new ContentServer();
        cs.start();

        try 
        {
            // Wait for the aggregation server to be ready to process the request
            Thread.sleep(1000);

            // Read the invalid JSON from the file
            String req = new String(Files.readAllBytes(Paths.get(fileLoc)));

            // Send the request to the aggregation server
            String res = sendPutRequestToServer("http://localhost:4567/weather", req);

            // If the response contains the 500 code, the test passed
            return res.contains("500");
        } 
        catch (IOException e) 
        {
            System.err.println("Error reading file: " + e.toString());
        } 
        catch (InterruptedException e) 
        {
            System.err.println("Thread interrupted: " + e.toString());
        } 
        catch (Exception e) 
        {
            System.err.println("Error: " + e.toString());
        }

        return false;
    }

    // Method to send a PUT request with the given JSON data to the server
    private String sendPutRequestToServer(String serverUrl, String jsonData) throws Exception 
    {
        URL url = new URL(serverUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setDoOutput(true);

        // Send the invalid JSON data to the server
        try (var os = connection.getOutputStream()) 
        {
            byte[] input = jsonData.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Get the server's response
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
        StringBuilder response = new StringBuilder();
        String responseLine;
        while ((responseLine = br.readLine()) != null) 
        {
            response.append(responseLine.trim());
        }

        return connection.getResponseCode() + " " + response.toString();
    }

    public static void main(String[] args) 
    {
        if (args.length < 1) 
        {
            System.out.println("Usage: java Response_500 <path_to_invalid_json_file>");
            return;
        }

        String fileLoc = args[0];
        Response_500 t = new Response_500();

        System.out.println("\033[0;1mInvalid JSON is blocked by Aggregation Server " + Thread.currentThread().getId() + "? "
                + t.invalidPUTrequest(fileLoc) + "\033[0m");
    }
}

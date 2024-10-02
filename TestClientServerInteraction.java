import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class TestClientServerInteraction 
{

    public static void main(String[] args) 
    {
        String serverUrl = "http://localhost:4567/weather";
        String jsonInput = "{\"stationId\": \"123\", \"temperature\": \"25\", \"humidity\": \"85\"}";

        // Execute the PUT request and handle the response
        boolean result = sendPutRequest(serverUrl, jsonInput);
        if (result) 
        {
            System.out.println("TestClientServerInteraction: SUCCESS (Data successfully sent and acknowledged)");
        } 
        else 
        {
            System.out.println("TestClientServerInteraction: FAILED (Unexpected response or behavior)");
        }
    }

    /**
     * Sends a PUT request to the specified server with the provided JSON payload.
     *
     * @param serverUrl  The URL of the server.
     * @param jsonInput  The JSON payload to send in the PUT request.
     * @return  True if the response code is 201 (Created), false otherwise.
     */
    public static boolean sendPutRequest(String serverUrl, String jsonInput)
    {
        HttpURLConnection connection = null;
        try 
        {
            // Create URL object and open HTTP connection
            URL url = new URL(serverUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Write the JSON input to the request body
            try (OutputStream os = connection.getOutputStream()) 
            {
                byte[] input = jsonInput.getBytes("utf-8");
                os.write(input, 0, input.length);
                os.flush();  // Ensure all data is sent
            }

            // Get and validate the response code
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Read and log the server's response
            String serverResponse = readServerResponse(connection);
            System.out.println("Server Response: " + serverResponse);

            // Check if the response code is what we expect
            return responseCode == HttpURLConnection.HTTP_CREATED;  // 201 Created
        }
        catch (Exception e)
        {
            System.err.println("TestClientServerInteraction: FAILED due to an exception.");
            e.printStackTrace();
            return false;
        } 
        finally 
        {
            // Disconnect and close the connection
            if (connection != null) 
            {
                connection.disconnect();
            }
        }
    }

    /**
     * Reads the server's response from the connection.
     *
     * @param connection  The active HTTP connection.
     * @return  The server's response as a String.
     * @throws Exception  If an I/O error occurs during reading.
     */
    private static String readServerResponse(HttpURLConnection connection) throws Exception {
        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) 
        {
            String inputLine;
            while ((inputLine = in.readLine()) != null) 
            {
                response.append(inputLine);
            }
        }
        return response.toString();
    }
}

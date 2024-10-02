import java.net.HttpURLConnection;
import java.net.URL;

public class TestNoUserAgent
{
    public static void main(String[] args) 
    {
        try {
            // Simulate a GET request without a User-Agent
            URL url = new URL("http://localhost:8080/weather");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Intentionally do not set the User-Agent header
            // connection.setRequestProperty("User-Agent", "test-agent");

            // Get the response code from the server
            int responseCode = connection.getResponseCode();
            
            // Evaluate the response code
            if (responseCode == 400) 
            {
                System.out.println("TestNoUserAgent: SUCCESS - received 400 Bad Request as expected.");
                System.out.println("Reason: The request did not include a User-Agent header, which is required.");
            } 
            else 
            {
                System.out.println("TestNoUserAgent: FAILED - unexpected response code: " + responseCode);
                System.out.println("Actual Response: The request was processed but did not return the expected error code.");
            }
        } 
        catch (Exception e) 
        {
            System.out.println("TestNoUserAgent: FAILED - An unexpected exception occurred.");
            System.out.println("Exception Message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

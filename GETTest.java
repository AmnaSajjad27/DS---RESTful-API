import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Vector;

import org.json.*;

public class GETTest
{
    // Extracts JSON data from the server response
    private String getData(Vector<String> request)
    {
        for (String string : request)
        {
            // Look for a string containing JSON data
            if (string.contains("{"))
            { 
                return string.strip();
            }
        }
        return null;
    }

    // Initiates a GET request and verifies the response against expected data
    public boolean getRequest()
    {
        GETClient cs = new GETClient(); 
        cs.start();

        try
        {
            cs.join();

            Path p2 = Path.of("LatestWeatherData.json"); // Path to expected data file
            String responseStr = getData(cs.getResponse()); // Get the response data

            // Check if the response is empty or null
            if (responseStr == null || responseStr.isEmpty())
            {
                System.err.println("Received empty or null response.");
                return false; // Return false on empty response
            }

            // Parse the received JSON response
            JSONObject s1 = new JSONObject(responseStr.replaceAll("\\s+", ""));
            System.out.println("Parsed response: " + s1.toString());

            // Parse the expected JSON data from the file
            JSONObject s2 = new JSONObject(Files.readString(p2).replaceAll("\\s+", ""));
            System.out.println("Expected data: " + s2.toString());

            // Check if the parsed response matches the expected data
            boolean match = s1.similar(s2);
            if (!match)
            {
                System.err.println("Response does not match expected data.");
                System.out.println("Expected: " + s2.toString());
                System.out.println("Actual: " + s1.toString());
            }
            return match; // Return whether the responses match
        }
        catch (NullPointerException npe)
        {
            System.err.println("Error in getRequest: Null response or data missing.");
            return false; // Return false on null pointer exception
        }
        catch (Exception e)
        {
            System.err.println("Error in getRequest: " + e.toString());
            return false;
        }
    }

    public static void main(String[] args)
    {
        GETTest t = new GETTest();
        // Hold threads 
        Thread[] threads = new Thread[4];

        // Create and start multiple threads to send GET requests concurrently
        for (int i = 0; i < 4; i++)
        {
            threads[i] = new Thread(() -> {
                // Print the result of the GET request
                System.out.println("\033[0;1mGET request works for Client " + Thread.currentThread().getId() + "? "
                        + t.getRequest() + "\033[0m");
            });
            threads[i].start();
        }

        // Wait for all threads to finish
        for (Thread thread : threads)
        {
            try
            {
                thread.join();
            }
            catch (InterruptedException e)
            {
                System.err.println("Thread interruption: " + e.toString());
            }
        }
    }
}

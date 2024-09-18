import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.Vector;

import org.json.*;

public class GETTest {

    // Helper method to extract data from the response
    private String getData(Vector<String> request) {
        for (String string : request) {
            if (string.contains("{")) {  // Check if the string contains JSON data
                return string.strip();
            }
        }
        return null;
    }

    // Main method that performs a GET request and compares the result
    public boolean getRequest() {
        GETClient cs = new GETClient(); // Initialize a new GETClient
        cs.start(); // Start the GETClient (it runs in a separate thread)

        try {
            // Wait for the GETClient to complete execution
            cs.join();

            // Load the expected data from the LatestWeatherData.json file
            Path p2 = Path.of("LatestWeatherData.json");
            String responseStr = getData(cs.getResponse()); // Get response data from the GETClient

            // Check if the response is empty or null
            if (responseStr == null || responseStr.isEmpty()) {
                System.err.println("Received empty or null response.");
                return false; // If the response is empty, return false
            }

            // Parse the server response and the expected data into JSONObjects
            JSONObject s1 = new JSONObject(responseStr.replaceAll("\\s+", ""));
            System.out.println("Parsed response: " + s1.toString()); // Debug log for parsed response

            JSONObject s2 = new JSONObject(Files.readString(p2).replaceAll("\\s+", ""));
            System.out.println("Expected data: " + s2.toString()); // Debug log for expected data

            // Compare the two JSONObjects
            boolean match = s1.similar(s2); // Use similar() for JSON comparison
            if (!match) {
                System.err.println("Response does not match expected data.");
                System.out.println("Expected: " + s2.toString());
                System.out.println("Actual: " + s1.toString());
            }
            return match; // Return whether the response matches the expected data
        } catch (NullPointerException npe) {
            System.err.println("Error in getRequest: Null response or data missing.");
            return false;
        } catch (Exception e) {
            System.err.println("Error in getRequest: " + e.toString());
            return false; // Return false if an error occurs
        }
    }

    // Main method to run multiple client threads
    public static void main(String[] args) {
        GETTest t = new GETTest();
        Thread[] threads = new Thread[4]; // Create an array to hold 4 threads

        // Initialize and start each thread
        for (int i = 0; i < 4; i++) {
            threads[i] = new Thread(() -> {
                System.out.println("\033[0;1mGET request works for Client " + Thread.currentThread().getId() + "? "
                        + t.getRequest() + "\033[0m"); // Execute the GET request and print the result
            });
            threads[i].start(); // Start each thread
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join(); // Ensure all threads complete before proceeding
            } catch (InterruptedException e) {
                System.err.println("Thread interruption: " + e.toString());
            }
        }
    }
}

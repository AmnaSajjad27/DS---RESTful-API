import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.Vector;

import org.json.*;

public class GETTest {

    private String getData(Vector<String> request)
    {
        for (String string : request) 
        {
            if (string.contains("{")) 
            { 
                return string.strip();
            }
        }
        return null;
    }

    public boolean getRequest()
    {
        GETClient cs = new GETClient(); 
        cs.start();

        try
        {
            cs.join();

            Path p2 = Path.of("LatestWeatherData.json");
            String responseStr = getData(cs.getResponse());

            if (responseStr == null || responseStr.isEmpty()) {
                System.err.println("Received empty or null response.");
                return false;
            }

            JSONObject s1 = new JSONObject(responseStr.replaceAll("\\s+", ""));
            System.out.println("Parsed response: " + s1.toString());

            JSONObject s2 = new JSONObject(Files.readString(p2).replaceAll("\\s+", ""));
            System.out.println("Expected data: " + s2.toString());

            boolean match = s1.similar(s2);
            if (!match) {
                System.err.println("Response does not match expected data.");
                System.out.println("Expected: " + s2.toString());
                System.out.println("Actual: " + s1.toString());
            }
            return match; 
        } catch (NullPointerException npe) {
            System.err.println("Error in getRequest: Null response or data missing.");
            return false;
        } catch (Exception e) {
            System.err.println("Error in getRequest: " + e.toString());
            return false;
        }
    }

    public static void main(String[] args) {
        GETTest t = new GETTest();
        Thread[] threads = new Thread[4]; 

        for (int i = 0; i < 4; i++) {
            threads[i] = new Thread(() -> {
                System.out.println("\033[0;1mGET request works for Client " + Thread.currentThread().getId() + "? "
                        + t.getRequest() + "\033[0m");
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.err.println("Thread interruption: " + e.toString());
            }
        }
    }
}

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class PUTTest {
    public boolean putRequest(String filePath) {
        // Start content server
        ContentServer cs = new ContentServer();
        cs.start();
        
        // Wait for server to start up and process the request
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Read files for comparison
        String s1 = "";
        String s2 = "";
        try {
            Path p1 = Path.of(filePath);
            Path p2 = Path.of("LatestWeatherData.json");
            s1 = Files.readString(p1);
            s2 = Files.readString(p2);
            
            // Debug output
            System.out.println("Sent data: " + s1);
            System.out.println("Received data: " + s2);
            
            // Remove whitespaces and newlines for comparison
            s1 = s1.replaceAll("\\s+", "");
            s2 = s2.replaceAll("\\s+", "");
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        
        return s1.equals(s2);
    }

    public static void main(String[] args) {
        PUTTest put = new PUTTest();
        System.out.println("\033[0;1mPut request works? " + put.putRequest("ContentServer1.json") + "\033[0m");
    }
}

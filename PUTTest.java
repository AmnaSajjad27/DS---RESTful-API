import java.nio.file.Files;
import java.nio.file.Path;

public class PUTTest 
{
    public boolean putRequest(String filePath) 
    {
        // Start content server
        ContentServer cs = new ContentServer();
        cs.start();

        // Wait for the server to process the request
        try {
            Thread.sleep(3000); // Increase wait time if needed
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Read files for comparison
        String s1 = "";
        String s2 = "";
        try 
        {
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
        } 
        catch (Exception e) 
        {
            System.err.println(e.toString());
        }
        
        // Stop the ContentServer
        // Ideally, you would have a proper way to stop or signal the server to stop
        // cs.stop(); // Implement this if your ContentServer class supports stopping
        
        return s1.equals(s2);
    }

    public static void main(String[] args) 
    {
        PUTTest put = new PUTTest();
        
        // List of test files
        String[] testFiles = { "ContentServer1.json", "ContentServer2.json", "ContentServer3.json" };
        
        for (String testFile : testFiles) {
            boolean result = put.putRequest(testFile);
            System.out.println("\033[0;1mPut request for " + testFile + " works? " + result + "\033[0m");
        }
    }
}

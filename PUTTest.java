import java.nio.file.Files;
import java.nio.file.Path;

public class PUTTest 
{
    // Performs a PUT request and compares sent and received data
    public boolean putRequest(String filePath) 
    {
        ContentServer content_server = new ContentServer(); // Start the content server
        content_server.start();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) 
        {
            e.printStackTrace();
        }

        String s1 = "", s2 = "";
        try 
        {
            Path p1 = Path.of(filePath);
            Path p2 = Path.of("LatestWeatherData.json");
            s1 = Files.readString(p1);
            s2 = Files.readString(p2); 

            // Debug output for comparison
            System.out.println("Sent data: " + s1);
            System.out.println("Received data: " + s2);
        } 
        catch (Exception e) 
        {
            System.err.println(e.toString());
        }

        // Return whether the sent and received data match (ignoring whitespace)
        return s1.replaceAll("\\s+", "").equals(s2.replaceAll("\\s+", ""));
    }

    // Main method to execute PUT test
    public static void main(String[] args) 
    {
        PUTTest put = new PUTTest();
        String[] testFiles = { "ContentServer1.json", "ContentServer2.json", "ContentServer3.json" };

        System.out.println("\033[0;1mPut request works? " + put.putRequest("ContentServer1.json") + "\033[0m");
    }
}

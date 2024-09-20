import org.json.*;

public class LamportClockTest 
{
    public static void main(String[] args) 
    {
        // Start the content server to handle requests and manage Lamport timestamps
        ContentServer cs = new ContentServer();
        cs.start();

        // Wait for the content server to finish
        try 
        {
            cs.join();
        }
         catch (InterruptedException e) 
        {
            System.err.println("Error while waiting for ContentServer: " + e);
        }

        // Start the client to send PUT and GET requests
        GETClient c = new GETClient();
        c.start();

        // Wait for the client to finish
        try
        {
            c.join();
        }
        catch (InterruptedException e)
        {
            System.err.println("Error while waiting for Client: " + e);
        }
    }
}


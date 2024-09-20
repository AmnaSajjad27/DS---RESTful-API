import java.util.Vector;

public class DataExpunge {
    
    // Test method for verifying data expunging functionality
    public boolean dataExpunging()
    {
        // Start a new ContentServer thread
        ContentServer content_server = new ContentServer();
        content_server.start();

        try 
        {
            // Wait for the content server to finish, then wait for 31 seconds
            content_server.join();
            Thread.sleep(31000);

            // Start a GETClient to fetch data after the wait period
            GETClient client_1 = new GETClient();
            client_1.start();
            Vector<String> response = client_1.getResponse();
            client_1.join();

            // If the response is null, data has been expunged successfully
            if (response == null)
            {
                return true;
            }
        }
        catch (Exception e) 
        {
            // Handle any exceptions during the test
            System.err.println(e.toString());
        }
        return false;
    }

    // Main method to execute the data expunging test
    public static void main(String[] args) 
    {
        DataExpunge data_expunge = new DataExpunge();
        // Output the result of the data expunging test
        System.out.println("\033[0;1mData Expunge works? " + data_expunge.dataExpunging() + "\033[0m");
    }
}

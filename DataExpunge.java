import java.util.Vector;

public class DataExpunge {
    public boolean dataExpunging()
    {
        ContentServer content_server = new ContentServer();
        content_server.start();

        try 
        {
            content_server.join();
            Thread.sleep(31000);

            GETClient client_1 = new GETClient();
            client_1.start();
            Vector<String> response = client_1.getResponse();
            client_1.join();
            // if the response from the server is null
            // then the data expunging works
            if (response == null)
            {
                return true;
            }
        }
        catch (Exception e) 
        {
            System.err.println(e.toString());
        }
        return false;
    }

    public static void main(String[] args) 
    {
        DataExpunge data_expunge = new DataExpunge();
        System.out.println("\033[0;1mData Expunge works? " + data_expunge.dataExpunging() + "\033[0m");
    }
}
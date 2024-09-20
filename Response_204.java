import java.nio.file.Files;
import java.nio.file.Path;
import org.json.JSONException;
import org.json.JSONObject;

public class Response_204 
{
    public boolean putRequest(String filePath) 
    {
        // Sends contents of EmptyJSON.json to check if
        // data is sent successfully from content server to 
        // aggregation server.
        ContentServer cs = new ContentServer();
        cs.start();

        try 
        {
            // Wait for the aggregation server to process the request
            Thread.sleep(1000);
            String res = cs.getResponse();

            // Check for 204 response code indicating success
            if (res.contains("204")) 
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
        Response_204 put = new Response_204();
        System.out.println("\033[0;1mEmpty data in put request works? " + put.putRequest("EmptyJSON.json") + "\033[0m");
    }
}

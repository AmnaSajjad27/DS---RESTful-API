import java.io.File;
import org.json.*;

public class Response_201 
{
    public static void main(String[] args) 
    {   
        // This test checks for the 201 response.
        // It deletes the LatestWeatherData.json file,
        // then sends a PUT request, expecting a 201 response from the server.
        File weatherFile = new File("LatestWeatherData.json");
        weatherFile.delete();

        ContentServer contentServer = new ContentServer();
        contentServer.start();

        try
        {
            contentServer.join();
        }
        catch (Exception e) 
        {
            System.err.println(e.toString());
        }

        boolean is201Response = contentServer.getResponse().contains("201");
        System.out.println("\033[0;1m201 response code works? " + is201Response + " \033[0m");
    }
}

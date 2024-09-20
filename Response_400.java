import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Response_400 
{
    public static void main(String[] args) 
    {
        // Send a request not GET or PUT
        // Send a POST request, the aggregation server responds with a 400 invalid request type
        try 
        {
            Socket soc = new Socket("127.0.0.1", 4567);
            PrintWriter pw = new PrintWriter(soc.getOutputStream());
            Scanner scan = new Scanner(soc.getInputStream());

            String req = "POST /weather.json HTTP/1.1\n" +
                         "User-Agent: Client1\n" +
                         "Lamport-Timestamp: 1\n" +
                         "Content-Type: application/json\n" +
                         "Content-Length: 0\n";

            pw.println(req);
            pw.flush();

            String res = "";
            while (scan.hasNextLine()) 
            {
                res += scan.nextLine() + '\n';
            }

            System.out.println(res);

            if (res.contains("400")) 
            {
                System.out.println("\033[0;1m400 response code works? true \033[0m");
            } 
            else 
            {
                System.out.println("\033[0;1m400 response code works? false \033[0m");
            }

            soc.close();
        } 
        catch (Exception e) 
        {
            System.err.println(e.toString());
        }
    }
}

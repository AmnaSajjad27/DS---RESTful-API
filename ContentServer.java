// Content Server 
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.json.*;

public class ContentServer extends Thread
{
    // Variable initalisations 
    private Socket socket;
    private DataInputStream server_response;
    private PrintWriter content_server_requests;
    // Unique id for content server 
    private String content_server_Id;
    // Clock to synchronise 
    private Integer lamport_time;
    // To store server response 
    private Vector<String> response;

    // Constructor: Initialise the content server, set id using thread's id and lamport time to 0
    public ContentServer()
    {
        this.content_server_Id = String.valueOf(Thread.currentThread().getId());
        this.lamport_time = 0;
    }

    // Method to return aggregation server response as a string
    public String getResponse() {
        return this.response.toString();
    }

    /*
    Main method 
    The server listens for input from the terminal to connect to the aggregation server
    Then sends PUT request with the weather data
    */
    @Override
    public void run() 
    {
        BufferedReader terminalinput = new BufferedReader(new InputStreamReader(System.in));
        boolean sendRequest = true;

        System.out.println("Hello ContentServer " + content_server_Id
                + "! Please enter address and port in the format address:port");

        String address = null;
        Integer port = null;

        try 
        {
            String temp = terminalinput.readLine();

            if (temp != null) 
            {
                String[] connectionDetails = temp.split(":");
                address = connectionDetails[0];
                port = Integer.parseInt(connectionDetails[1]);
            }
        }
        // Handle input errors when reading from termainl 
        catch (Exception e) 
        {
            System.err.println("Error with reading terminal input: " + e.toString());
        }
        // While loop to handle multiple requests 
        while (sendRequest)
        {
            try 
            {
                // If invalid or none given, use defualt values
                if (address == null || port == null) {
                    socket = new Socket("127.0.0.1", 4567);
                } else {
                    socket = new Socket(address, port);
                }

                // Prompt the user to input the file name containing weather data.
                System.out.println("Please input file name of weather data: ");
                String file_location = terminalinput.readLine();

                // Initialise input and output streams for server 
                server_response = new DataInputStream(socket.getInputStream());
                content_server_requests = new PrintWriter(socket.getOutputStream());

                // Perform PUT request - sending data to server 
                sendPutRequest(file_location);

                // Store the response in a Vector 
                Vector<String> line = new Vector<>();
                Scanner s = new Scanner(this.socket.getInputStream());
                while (s.hasNextLine()) 
                {
                    line.add(s.nextLine());
                }
                this.response = line;
                // Update lamport time using server response. 
                updateLamportTime(line);

                System.out.println("Server response for ContentServer " + this.content_server_Id + " : " + line + "\r\n\r\n");

                // Close the request and server response 
                content_server_requests.close();
                server_response.close();

                System.out.println("Request successful. Would you like to send another PUT request? ('true' for yes, 'false' for no)");

                // Continour request if user types "true"
                sendRequest = Boolean.parseBoolean(terminalinput.readLine());

            }
            catch (Exception e) 
            {

                System.err.println("ContentServer " + this.content_server_Id + " - Failed to connect to aggregation server: "
                                + e.toString() + "\nWould you like to try connecting again? ('true' for yes, 'false' for no)");
                try 
                {
                    sendRequest = Boolean.parseBoolean(terminalinput.readLine());
                } 
                catch (Exception err) 
                {
                    err.printStackTrace();
                }
            }
        }

        System.out.println("Goodbye ContentServer " + content_server_Id + "!");
    }

    // This method updates lamport time based on server repsonse 
    private void updateLamportTime(Vector<String> data)
    {
        String server_time = "";
        for (String string : data)
        {
            if (string.contains("Lamport-Timestamp")) 
            {
                server_time = string.split(":")[1].strip();
                this.lamport_time = Math.max(Integer.parseInt(server_time), this.lamport_time) + 1;
                break;
            }
        }
    }

    
    // This method sends a PUT request to the Aggregation Server with the provided weather data file.
    // If the server connection is closed or failed, the request is saved locally for later recovery.
    private void sendPutRequest(String file_location) 
    {
        Path path = Paths.get("ContentServer" + this.content_server_Id + "Replication.txt");

        // Check if there is a saved replication 
        if (Files.exists(path))
        {
            try 
            {
                String req = new String(Files.readAllBytes(path));
                content_server_requests.println(req);
                content_server_requests.flush();
                // Delete the replication after recovery 
                Files.delete(path);
            }
            catch (Exception e)
            {
                System.err.println(e.toString());
            }
        }
        else
        {
            String data = "";
            // if server fails, save locally 
            if (socket.isOutputShutdown() || socket.isClosed())
            {
                try
                {
                    Files.writeString(path, data);
                }
                catch (Exception err) 
                {
                    System.err.println(err.toString());
                }
            }
            else
            {
                // Read from provided file 
                try 
                {
                    String req = new String(Files.readAllBytes(Paths.get(file_location)));
                    // Format the PUT request 
                    data += "PUT /weather.json HTTP/1.1\n";
                    data += "User-Agent:ContentServer" + content_server_Id + '\n';
                    data += "Lamport-Timestamp: " + String.valueOf(this.lamport_time) + '\n';
                    data += "Content-Type: application/json\n";
                    data += "Content-Length: " + req.length() + "\n\r\n\r\n";
                    data += req.toString();
                    // Send to aggregation server 
                    content_server_requests.println(data);
                    content_server_requests.flush();
                }
                catch (Exception e)
                {
                    System.err.println(e.toString());
                }
            }
        }

    }
    // Main method to create and start the content server 
    public static void main(String[] args)
    {
        ContentServer content_server = new ContentServer();
        content_server.start();
    }
}
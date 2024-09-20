// A Java program for a Client
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.json.*;

public class ContentServer extends Thread
{
    private Socket socket;
    private DataInputStream serverResponse;
    private PrintWriter contentServerRequests;
    private String contentServerId;
    private Integer lamportTime;
    private Vector<String> response;

    // Initialize content server with thread id and lamport timestamp
    public ContentServer()
    {
        this.contentServerId = String.valueOf(Thread.currentThread().getId());
        this.lamportTime = 0;
    }

    // Retrieve the server response
    public String getResponse()
    {
        return this.response.toString();
    }

    // Main execution for the content server, managing connections and requests
    @Override
    public void run()
    {
        BufferedReader terminalinput = new BufferedReader(new InputStreamReader(System.in));
        boolean sendRequest = true;
        System.out.println("Hello ContentServer " + contentServerId + "! Please enter address and port in the format address:port");
        String address = null;
        Integer port = null;

        try
        {
            // Get server address and port from terminal input
            String temp = terminalinput.readLine();
            if (temp != null)
            {
                String[] connectionDetails = temp.split(":");
                address = connectionDetails[0];
                port = Integer.parseInt(connectionDetails[1]);
            }
        }
        catch (Exception e)
        {
            System.err.println("Error with reading terminal input: " + e.toString());
        }

        while (sendRequest)
        {
            try
            {
                // Set default connection if address/port invalid
                if (address == null || port == null)
                {
                    socket = new Socket("127.0.0.1", 3000);
                }
                else
                {
                    socket = new Socket(address, port);
                }

                // Request weather data file location from user
                System.out.println("Please input file name of weather data: ");
                String fileLoc = terminalinput.readLine();

                serverResponse = new DataInputStream(socket.getInputStream());
                contentServerRequests = new PrintWriter(socket.getOutputStream());

                // Send PUT request with the provided weather file
                sendPutRequest(fileLoc);

                // Capture server response
                Vector<String> line = new Vector<>();
                Scanner s = new Scanner(this.socket.getInputStream());
                while (s.hasNextLine())
                {
                    line.add(s.nextLine());
                }

                this.response = line;
                // Update the lamport timestamp
                updateLamportTime(line);

                System.out.println("Server response for ContentServer " + this.contentServerId + " : " + line + "\r\n\r\n");

                contentServerRequests.close();
                serverResponse.close();

                System.out.println("Request successful. Would you like to send another PUT request? ('true' for yes, 'false' for no)");
                sendRequest = Boolean.parseBoolean(terminalinput.readLine());

            }
            catch (Exception e)
            {
                // Handle errors during connection to the server
                System.err.println("ContentServer " + this.contentServerId + " - Failed to connect to aggregation server: "
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

        System.out.println("Goodbye ContentServer " + contentServerId + "!");
    }

    // Update the content server's lamport time based on server response
    private void updateLamportTime(Vector<String> data)
    {
        String serverTime = "";
        for (String string : data)
        {
            if (string.contains("Lamport-Timestamp"))
            {
                serverTime = string.split(":")[1].strip();
                this.lamportTime = Math.max(Integer.parseInt(serverTime), this.lamportTime) + 1;
                break;
            }
        }
    }

    // Send PUT request with weather data or handle server recovery
    private void sendPutRequest(String fileLoc)
    {
        Path path = Paths.get("ContentServer" + this.contentServerId + "Replication.txt");
        if (Files.exists(path))
        {
            try
            {
                String req = new String(Files.readAllBytes(path));
                contentServerRequests.println(req);
                contentServerRequests.flush();
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
                try
                {
                    // Read weather data and prepare PUT request
                    String req = new String(Files.readAllBytes(Paths.get(fileLoc)));
                    data += "PUT /weather.json HTTP/1.1\n";
                    data += "User-Agent:ContentServer" + contentServerId + '\n';
                    data += "Lamport-Timestamp: " + String.valueOf(this.lamportTime) + '\n';
                    data += "Content-Type: application/json\n";
                    data += "Content-Length: " + req.length() + "\n\r\n\r\n";
                    data += req;
                    contentServerRequests.println(data);
                    contentServerRequests.flush();
                }
                catch (Exception e)
                {
                    System.err.println(e.toString());
                }
            }
        }
    }

    public static void main(String[] args)
    {
        ContentServer cs = new ContentServer();
        cs.start();
    }
}
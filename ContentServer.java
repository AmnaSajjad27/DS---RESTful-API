/*
This server will read a weather data file, convert to JSON and send a PUT request to the Aggregation Server.
Content server verifies the acknowledgment from Aggregation server. 
*/

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
    private DataInputStream server_response;
    private PrintWriter content_server_requests;
    private String content_server_Id;
    private Integer lamport_time;
    private Vector<String> response;

    public ContentServer()
    {
        this.content_server_Id = String.valueOf(Thread.currentThread().getId());
        this.lamport_time = 0;
    }

    public String getResponse()
    {
        return this.response.toString();
    }

    @Override
    public void run()
    {
        BufferedReader user_input = new BufferedReader(new InputStreamReader(System.in));
        boolean send_request = true;

        System.out.println("Hello ContentServer " + content_server_Id + "! Please enter address and port in the format address:port");
        String address = null;
        Integer port = null;

        try 
        {
            // get the address and port from the terminal input
            String temp_string = user_input.readLine();
            if (temp_string != null)
            {
                String[] connection_details = temp_string.split(":");
                address = connection_details[0];
                port = Integer.parseInt(connection_details[1]);
            }
        }
        catch (Exception e)
        {
            System.err.println("Error with reading terminal input: " + e.toString());
        }

        while (send_request) 
        {
            try 
            {
                // if the address or port is invalid
                // then we use defaults
                if (address == null || port == null) 
                {
                    socket = new Socket("127.0.0.1", 3000);
                } 
                else 
                {
                    socket = new Socket(address, port);
                }

                // provide the content server with the file location 
                // of the weather data.
                System.out.println("Please input file name of weather data: ");
                String file_location = user_input.readLine();

                server_response = new DataInputStream(socket.getInputStream());
                content_server_requests = new PrintWriter(socket.getOutputStream());

                // perform the put request and provide the file location
                send_put_request(file_location);
                // This will process the server response into a vector
                Vector<String> line = new Vector<>();
                Scanner scanner = new Scanner(this.socket.getInputStream());

                while (scanner.hasNextLine()) 
                {
                    line.add(scanner.nextLine());
                }

                // here we capture the latest response in this variable
                this.response = line;
                // the code updates the content server's lamport time
                update_lamport_time(line);

                System.out.println("Server response for ContentServer " + this.content_server_Id + " : " + line + "\r\n\r\n");

                content_server_requests.close();
                server_response.close();

                System.out.println("Request successful. Would you like to send another PUT request? ('true' for yes, 'false' for no)");

                // we continue with requests if the user inputs 'true'
                send_request = Boolean.parseBoolean(user_input.readLine());

            }
            catch (Exception e) 
            {
                // catch & print out any errors when attempting to connect
                // to the aggregation server. 
                System.err.println("ContentServer " + this.content_server_Id + " - Failed to connect to aggregation server: "
                + e.toString() + "\nWould you like to try connecting again? ('true' for yes, 'false' for no)");
                try 
                {
                    send_request = Boolean.parseBoolean(user_input.readLine());
                }
                catch (IOException err) 
                {
                    System.err.println("Error reading input: " + err.toString());
                    send_request = false;
                }
            }
        }
        send_request = false;
        System.out.println("Goodbye ContentServer " + content_server_Id + "!");
    }

    private void update_lamport_time(Vector<String> data)
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

    private void send_put_request(String file_location)
    {
        Path path = Paths.get("ContentServer" + this.content_server_Id + "Replication.txt");

        if (Files.exists(path))
        {
            try 
            {
                String request = new String(Files.readAllBytes(path));
                content_server_requests.println(request);
                content_server_requests.flush();
                // remove the file since we have recovered the state
                // of the content server
                Files.delete(path);
            }
            catch (Exception e) 
            {
                System.err.println(e.toString());
            }
        }
        else
        {
            // if the file does not exist and the connection
            // to the aggregation server is not closed
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
                    // read weather data from file location
                    String request = new String(Files.readAllBytes(Paths.get(file_location)));
                    // put data into JSON string
                    data += "PUT /weather.json HTTP/1.1\n";
                    data += "User-Agent:ContentServer" + content_server_Id + '\n';
                    data += "Lamport-Timestamp: " + String.valueOf(this.lamport_time) + '\n';
                    data += "Content-Type: application/json\n";
                    data += "Content-Length: " + request.length() + "\n\r\n\r\n";
                    data += request.toString();
                    // send data to aggregation server.
                    content_server_requests.println(data);
                    content_server_requests.flush();
                }
                // else if the connection to the aggregation server is closed
                // then we throw exception and write to the file
                // ContentServer1Replication.txt to save the content server state
                catch (Exception e) 
                {
                    System.err.println(e.toString());
                }
            }
        }
    }

    public static void main (String[] args)
    {
        ContentServer content_server = new ContentServer();
        content_server.start();
    }
}

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.Vector;

public class GETClient extends Thread 
{
    private Socket socket;
    private BufferedReader server_response;
    private PrintWriter client_requests;
    private String clientId;
    private Integer lamportTime;
    private Vector<String> response;

    // Initialize the GET client with a random Lamport time and unique thread ID
    public GETClient() 
    {
        this.lamportTime = (int) Math.floor(Math.random() * (10 - 0 + 1) + 0);
        this.clientId = String.valueOf(Thread.currentThread().getId());
    }

    // Returns the response received from the server
    public Vector<String> getResponse() 
    {
        return this.response;
    }

    @Override
    public void run() 
    {
        BufferedReader terminalInput = new BufferedReader(new InputStreamReader(System.in));
        boolean sendRequest = true;
        System.out.println("Hello Client " + clientId + "! Please enter address and port in the format address:port");
        String address = null;
        Integer port = null;
        try 
        {
            // Read address and port from user input
            String temp = terminalInput.readLine();
            if (temp != null) {
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
                // Use default address and port if not provided
                if (address == null || port == null) 
                {
                    socket = new Socket("127.0.0.1", 4567);
                }
                else
                {
                    socket = new Socket(address, port);
                }

                // Setup reader for server response and writer for client requests
                this.server_response = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.client_requests = new PrintWriter(socket.getOutputStream(), true);

                // Ask for optional station ID and send GET request
                System.out.println("Optional: Please enter the station id (leave empty if you want latest data):");
                String stationId = terminalInput.readLine();
                sendGetRequest(stationId);

                Vector<String> responseLines = new Vector<>();
                String line;

                // Read the server response line by line
                while ((line = server_response.readLine()) != null) 
                {
                    responseLines.add(line);
                }

                // Update the Lamport time based on the server response
                updateLamportTime(responseLines);
                this.response = responseLines;

                System.out.println("Server response for Client " + this.clientId + " : " + responseLines + "\r\n");

                // Ask if the user wants to send another GET request
                System.out.println("Request successful. Would you like to send another GET request? ('true' for yes, 'false' for no)");
                sendRequest = Boolean.parseBoolean(terminalInput.readLine());

            } 
            catch (Exception e) 
            {
                // Handle connection errors and ask to retry
                System.err.println(
                        "Client " + this.clientId + " - Failed to connect to aggregation server: "
                                + e.toString()
                                + "\nWould you like to try connecting again? ('true' for yes, 'false' for no)");
                try 
                {
                    sendRequest = Boolean.parseBoolean(terminalInput.readLine());
                }
                catch (Exception err) 
                {
                    err.printStackTrace();
                }
            } 
            finally 
            {
                // Ensure all resources (socket, streams) are properly closed
                closeConnections();
            }
        }

        System.out.println("Goodbye Client " + clientId + "!");
    }

    // Close socket and I/O streams
    private void closeConnections() 
    {
        try 
        {
            if (client_requests != null) client_requests.close();
            if (server_response != null) server_response.close();
            if (socket != null) socket.close();
        }
        catch (IOException e) 
        {
            System.err.println("Error closing client resources: " + e.toString());
        }
    }

    // Update the Lamport time based on the server's timestamp in the response
    private void updateLamportTime(Vector<String> data) 
    {
        for (String string : data) 
        {
            if (string.contains("Lamport-Timestamp")) 
            {
                String serverTime = string.split(":")[1].strip();
                this.lamportTime = Math.max(Integer.parseInt(serverTime), this.lamportTime) + 1;
            }
        }
    }

    // Send the GET request to the aggregation server
    private void sendGetRequest(String stationId) 
    {
        try 
        {
            // Build the GET request based on whether a station ID is provided
            if (stationId == null || stationId.isEmpty()) 
            {
                this.client_requests.println("GET /weather.json HTTP/1.1\r\nHost: localhost\r\nUser-Agent: Client " +
                        this.clientId + "\r\nLamport-Timestamp: " +
                        this.lamportTime + "\r\nContent-Type: application/json\r\nContent-Length: 0\r\n\r\n");
            }
            else
            {
                this.client_requests.println("GET /weather.json?station-id=" + stationId + " HTTP/1.1\r\nHost: localhost\r\nUser-Agent: Client " +
                        this.clientId + "\r\nLamport-Timestamp: " +
                        this.lamportTime + "\r\nContent-Type: application/json\r\nContent-Length: 0\r\n\r\n");
            }
            this.client_requests.flush(); // Ensure the request is fully sent

        } catch (Exception e) 
        {
            System.err.println("Error sending GET request: " + e.toString());
        }
    }

    public static void main(String[] args) 
    {
        // Start the GET client thread
        GETClient client = new GETClient();
        client.start();
    }
} 

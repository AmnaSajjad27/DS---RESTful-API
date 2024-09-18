import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.Vector;

public class GETClient extends Thread {
    private Socket socket;
    private BufferedReader serverResponse;
    private PrintWriter clientRequests;
    private String clientId;
    private Integer lamportTime;
    private Vector<String> response;

    public GETClient() {
        this.lamportTime = (int) Math.floor(Math.random() * (10 - 0 + 1) + 0);
        this.clientId = String.valueOf(Thread.currentThread().getId());
    }

    public Vector<String> getResponse() {
        return this.response;
    }

    @Override
    public void run() {
        BufferedReader terminalInput = new BufferedReader(new InputStreamReader(System.in));
        boolean sendRequest = true;
        System.out.println("Hello Client " + clientId + "! Please enter address and port in the format address:port");
        String address = null;
        Integer port = null;
        try {
            // Get the address and port from terminal input
            String temp = terminalInput.readLine();
            if (temp != null) {
                String[] connectionDetails = temp.split(":");
                address = connectionDetails[0];
                port = Integer.parseInt(connectionDetails[1]);
            }
        } catch (Exception e) {
            System.err.println("Error with reading terminal input: " + e.toString());
        }

        while (sendRequest) {
            try {
                if (address == null || port == null) {
                    socket = new Socket("127.0.0.1", 4567); // Default fallback address
                } else {
                    socket = new Socket(address, port); // Establish socket connection
                }

                // Create server response reader and client request writer
                this.serverResponse = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.clientRequests = new PrintWriter(socket.getOutputStream(), true);

                System.out.println("Optional: Please enter the station id (leave empty if you want latest data):");
                String stationId = terminalInput.readLine();
                sendGetRequest(stationId);

                Vector<String> responseLines = new Vector<>();
                String line;

                // Read the response from the server line by line
                while ((line = serverResponse.readLine()) != null) {
                    responseLines.add(line);
                }

                updateLamportTime(responseLines);
                this.response = responseLines;

                System.out.println("Server response for Client " + this.clientId + " : " + responseLines + "\r\n");

                System.out.println("Request successful. Would you like to send another GET request? ('true' for yes, 'false' for no)");
                sendRequest = Boolean.parseBoolean(terminalInput.readLine());

            } catch (Exception e) {
                // Handle connection issues
                System.err.println(
                        "Client " + this.clientId + " - Failed to connect to aggregation server: "
                                + e.toString()
                                + "\nWould you like to try connecting again? ('true' for yes, 'false' for no)");
                try {
                    sendRequest = Boolean.parseBoolean(terminalInput.readLine());
                } catch (Exception err) {
                    err.printStackTrace();
                }
            } finally {
                // Ensure socket and streams are closed
                closeConnections();
            }
        }

        System.out.println("Goodbye Client " + clientId + "!");
    }

    // Properly close connections
    private void closeConnections() {
        try {
            if (clientRequests != null) clientRequests.close();
            if (serverResponse != null) serverResponse.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing client resources: " + e.toString());
        }
    }

    private void updateLamportTime(Vector<String> data) {
        for (String string : data) {
            if (string.contains("Lamport-Timestamp")) {
                String serverTime = string.split(":")[1].strip();
                this.lamportTime = Math.max(Integer.parseInt(serverTime), this.lamportTime) + 1;
            }
        }
    }

    private void sendGetRequest(String stationId) {
        try {
            if (stationId == null || stationId.isEmpty()) {
                this.clientRequests.println("GET /weather.json HTTP/1.1\r\nHost: localhost\r\nUser-Agent: Client " +
                        this.clientId + "\r\nLamport-Timestamp: " +
                        this.lamportTime + "\r\nContent-Type: application/json\r\nContent-Length: 0\r\n\r\n");
            } else {
                this.clientRequests.println("GET /weather.json?station-id=" + stationId + " HTTP/1.1\r\nHost: localhost\r\nUser-Agent: Client " +
                        this.clientId + "\r\nLamport-Timestamp: " +
                        this.lamportTime + "\r\nContent-Type: application/json\r\nContent-Length: 0\r\n\r\n");
            }
            this.clientRequests.flush(); // Ensure the request is fully sent

        } catch (Exception e) {
            System.err.println("Error sending GET request: " + e.toString());
        }
    }

    public static void main(String[] args) {
        GETClient client = new GETClient();
        client.start();
    }
}

// A Java program for a Client
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.json.*;

public class ContentServer extends Thread {
    private Socket socket;
    private DataInputStream serverResponse;
    private PrintWriter contentServerRequests;
    private String contentServerId;
    private Integer lamportTime;
    private Vector<String> response;

    // we initialise the content server with its id using the thread id
    // and also its lamport timestamp by assigning it 0.
    public ContentServer() {
        this.contentServerId = String.valueOf(Thread.currentThread().getId());
        this.lamportTime = 0;
    }

    // we provide an interface to the testing methods to 
    // get the responses that the content server receives from
    // the aggregation server
    public String getResponse() {
        return this.response.toString();
    }

    // since the content server extends Thread, we need to provide a run()
    // function. This function will take in the address for the server
    // and the file location for the weather file for each request. 
    @Override
    public void run() {
        BufferedReader terminalinput = new BufferedReader(new InputStreamReader(System.in));
        boolean sendRequest = true;
        System.out.println("Hello ContentServer " + contentServerId
                + "! Please enter address and port in the format address:port");
        String address = null;
        Integer port = null;

        try {
            // get the address and port from the terminal input
            String temp = terminalinput.readLine();
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
                // if the address or port is invalid
                // then we use defaults
                if (address == null || port == null) {
                    socket = new Socket("127.0.0.1", 4567);
                } else {
                    socket = new Socket(address, port);
                }

                // provide the content server with the file location 
                // of the weather data.
                System.out.println("Please input file name of weather data: ");
                String fileLoc = terminalinput.readLine();

                serverResponse = new DataInputStream(socket.getInputStream());
                contentServerRequests = new PrintWriter(socket.getOutputStream());

                // perform the put request and provide the file location
                sendPutRequest(fileLoc);
                // This will process the server response into a vector
                Vector<String> line = new Vector<>();
                Scanner s = new Scanner(this.socket.getInputStream());
                while (s.hasNextLine()) {
                    line.add(s.nextLine());
                }

                // here we capture the latest response in this variable
                this.response = line;
                // the code updates the content server's lamport time
                updateLamportTime(line);

                System.out.println(
                        "Server response for ContentServer " + this.contentServerId + " : " + line + "\r\n\r\n");

                contentServerRequests.close();
                serverResponse.close();

                System.out.println(
                        "Request successful. Would you like to send another PUT request? ('true' for yes, 'false' for no)");

                // we continue with requests if the user inputs 'true'
                sendRequest = Boolean.parseBoolean(terminalinput.readLine());

            } catch (Exception e) {
                // catch & print out any errors when attempting to connect
                // to the aggregation server. 
                System.err.println(
                        "ContentServer " + this.contentServerId + " - Failed to connect to aggregation server: "
                                + e.toString()
                                + "\nWould you like to try connecting again? ('true' for yes, 'false' for no)");
                try {
                    sendRequest = Boolean.parseBoolean(terminalinput.readLine());
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        }

        System.out.println("Goodbye ContentServer " + contentServerId + "!");
    }

    // this function takes the response sent from the aggregation server
    // and computes the new lamport timestmap that the content server must have
    // it parses the response string and gets the 'Lamport-Timestamp' attribute
    // to compute the new lamport time for the content server
    private void updateLamportTime(Vector<String> data) {
        String serverTime = "";
        for (String string : data) {
            if (string.contains("Lamport-Timestamp")) {
                serverTime = string.split(":")[1].strip();
                this.lamportTime = Math.max(Integer.parseInt(serverTime), this.lamportTime) + 1;
                break;
            }
        }
    }

    // this function performs the put request given the user 
    // provides a valid weather file  

    private void sendPutRequest(String fileLoc) {
        // PUT /weather.json HTTP/1.1
        // User-Agent: ATOMClient/1/0
        // Content-Type: (You should work this one out)
        // Content-Length: (And this one too)

        // {
        // "id" : "IDS60901",
        // ...
        // (data)
        // ...
        // "wind_spd_kt": 8
        // }

        // if this file exists that means the server crashed
        // therefore, we must send this to Aggregation Server
        // to recover the state of the Content Server
        Path path = Paths.get("ContentServer" + this.contentServerId + "Replication.txt");
        if (Files.exists(path)) {
            try {
                String req = new String(Files.readAllBytes(path));
                contentServerRequests.println(req);
                contentServerRequests.flush();
                // remove the file since we have recovered the state
                // of the content server
                Files.delete(path);
            } catch (Exception e) {
                System.err.println(e.toString());
            }
        } else {
            // if the file does not exist and the connection
            // to the aggregation server is not closed
            String data = "";
            if (socket.isOutputShutdown() || socket.isClosed()) {
                try {
                    Files.writeString(path, data);
                } catch (Exception err) {
                    System.err.println(err.toString());
                }
            } else {
                try {
                    // read weather data from file location
                    String req = new String(Files.readAllBytes(Paths.get(fileLoc)));
                    // put data into JSON string
                    data += "PUT /weather.json HTTP/1.1\n";
                    data += "User-Agent:ContentServer" + contentServerId + '\n';
                    data += "Lamport-Timestamp: " + String.valueOf(this.lamportTime) + '\n';
                    data += "Content-Type: application/json\n";
                    data += "Content-Length: " + req.length() + "\n\r\n\r\n";
                    data += req.toString();
                    // send data to aggregation server.
                    contentServerRequests.println(data);
                    contentServerRequests.flush();
                }
                // else if the connection to the aggregation server is closed
                // then we throw exception and write to the file
                // ContentServer1Replication.txt to save the content server state
                catch (Exception e) {
                    System.err.println(e.toString());
                }
            }
        }

    }

    public static void main(String[] args) {
        ContentServer cs = new ContentServer();
        cs.start();
    }
}
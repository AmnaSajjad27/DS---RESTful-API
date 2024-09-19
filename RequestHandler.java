import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.Vector;
import org.json.*;

/*
Process flow:
1. Init communication - use PrintWriter to send data to the client and Scanner to receive.
2. Validate request using requestIsOk().
3. Read data until a valid JSON payload is received.
4. Convert the request to a vector and add to the Producer-Consumer queue.
5. Send the appropriate response.
6. Close the socket.
*/

public class RequestHandler extends Thread 
{
    private Socket socket;
    private ProducerConsumer producer_consumer;

    // Constructor - passes socket and producer-consumer instance to the handler
    public RequestHandler(Socket socket, ProducerConsumer producer_consumer) 
    {
        this.socket = socket;
        this.producer_consumer = producer_consumer;
    }

    // Validate the incoming request contains necessary fields (PUT/GET, User-Agent, Lamport-Timestamp, etc.)
    public boolean requestIsOk(String data) 
    {
        boolean requestType = data.contains("PUT /weather.json HTTP/1.1") || data.contains("GET /weather.json HTTP/1.1");
        boolean userAgent = data.contains("User-Agent:");
        boolean lamportTimestamp = data.contains("Lamport-Timestamp:");
        boolean contentType = data.contains("Accept:");
        boolean contentLength = data.contains("Content-Length:");

        return requestType && userAgent && lamportTimestamp && contentLength && contentType;
    }

    // Convert the string data to a vector for processing in Producer-Consumer queue
    private Vector<String> stringToVector(String data) 
    {
        String[] temp = data.split("\n");
        Vector<String> ans = new Vector<>();
        for (String line : temp) {
            if (!line.equals("\n")) {
                ans.add(line);
            }
        }
        return ans;
    }

    // Validate if the request contains valid JSON data
    private boolean checkIfValidJSON(String data) 
    {
        try {
            new JSONObject(data);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Extract the value associated with a specific key (e.g., User-Agent) from the request
    public String getValue(Vector<String> request, String key) 
    {
        for (String line : request) {
            if (line.contains(key)) {
                String[] ans = line.split(":");
                return (ans.length >= 2) ? ans[1] : "";
            }
        }
        return null;
    }

    // Handle the incoming request, send it to Producer-Consumer, and respond to the client
    @Override
    public void run() 
    {
        try {
            PrintWriter pw = new PrintWriter(this.socket.getOutputStream(), true);
            Scanner scanner = new Scanner(this.socket.getInputStream()).useDelimiter("\n");
            String line = "";

            // Process request headers until a valid request is detected
            while (!requestIsOk(line)) {
                String temp = scanner.hasNextLine() ? scanner.nextLine().strip() + '\n' : "";
                line += temp;

                if (temp.isBlank()) {
                    break;
                }
            }

            // If PUT request, read additional JSON body data
            String temp = "";
            if (line.contains("PUT")) {
                while (!checkIfValidJSON(temp) && scanner.hasNextLine()) {
                    temp += scanner.nextLine().strip();
                }
            }
            line += temp;

            // Convert the request to vector and process it
            Vector<String> converted = stringToVector(line);
            System.out.println(line + "\r\n\r\n");

            // Get User-Agent, add request to queue, and retrieve response
            String id = this.producer_consumer.getValue(converted, "User-Agent");
            this.producer_consumer.addRequest(converted);
            String response = this.producer_consumer.getRequest(id);

            // Send the response back to the client
            pw.println(response);
            pw.flush();
            pw.close();

            this.socket.close();
        } 
        catch (Exception e) 
        {
            System.err.println("Aggregation Server: " + e.toString());
        }
    }
}

import java.net.*;
import java.util.Vector;
import java.io.*;
import org.json.*;

// Manages client connections and handles requests
public class ThreadConnection extends Thread 
{
    private Socket socket;
    private ProducerConsumer pc;

    // Constructor initializes the socket and producer-consumer instance
    public ThreadConnection(Socket connectionSocket, ProducerConsumer prodCon) 
    {
        this.socket = connectionSocket;
        this.pc = prodCon;
    }

    // Main method to handle incoming requests
    public void run() 
    {
        DataInputStream requests = null;
        DataOutputStream serverRes = null;

        try 
        {
            requests = new DataInputStream(this.socket.getInputStream());
            serverRes = new DataOutputStream(this.socket.getOutputStream());
            System.out.println("Server: Client is connected.");
        } 
        catch (Exception e) 
        {
            System.err.println("Server initialization error: " + e.toString());
        }

        String request = null;

        while (true) 
        {
            try 
            {
                request = requests.readUTF();

                // Exit loop if "over" command is received
                if (request.contains("over")) 
                {
                    this.socket.close();
                    Thread.currentThread().interrupt();
                    break;
                }

                // Create a request vector and add it to the producer-consumer
                Vector<String> requestVector = new Vector<>();
                requestVector.add(request);
                this.pc.addRequest(requestVector);

                // Get the response based on the request
                String responseStr = this.pc.getRequest(request);
                if (responseStr != null) 
                {
                    // Send the response back to the client
                    JSONObject response = new JSONObject(responseStr);
                    serverRes.writeUTF(response.toString());
                    serverRes.flush();
                } 
                else 
                {
                    System.err.println("No response for the given request ID.");
                }
            } 
            catch (Exception e) 
            {
                System.err.println("Server error: " + e.toString());
                break;
            }
        }
    }
}

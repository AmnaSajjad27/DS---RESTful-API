import java.net.*;
import java.util.Vector;
import java.io.*;
import org.json.*;

public class ThreadConnection extends Thread {
    private Socket socket;
    private ProducerConsumer pc;

    public ThreadConnection(Socket connectionSocket, ProducerConsumer prodCon) {
        this.socket = connectionSocket;
        this.pc = prodCon;
    }

    public void run() {
        DataInputStream requests = null;
        DataOutputStream serverRes = null;

        try {
            requests = new DataInputStream(this.socket.getInputStream());
            serverRes = new DataOutputStream(this.socket.getOutputStream());

            System.out.println("Server: Client is connected.");
        } catch (Exception e) {
            System.err.println("Server initialization error: " + e.toString());
        }

        String request = null;

        while (true) {
            try {
                request = requests.readUTF();

                if (request.contains("over")) {
                    this.socket.close();
                    Thread.currentThread().interrupt();
                    break;
                }

                // Pass a Vector<String> to addRequest
                Vector<String> requestVector = new Vector<>();
                requestVector.add(request);
                this.pc.addRequest(requestVector);  // No return value expected

                // Use the request as the ID to get the response
                String responseStr = this.pc.getRequest(request);  // getRequest returns a String
                if (responseStr != null) {
                    // Convert the String to JSONObject
                    JSONObject response = new JSONObject(responseStr);
                    serverRes.writeUTF(response.toString());
                    serverRes.flush();
                } else {
                    System.err.println("No response for the given request ID.");
                }
            } catch (Exception e) {
                System.err.println("Server error: " + e.toString());
                break;
            }
        }
    }
}

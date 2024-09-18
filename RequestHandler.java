
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.Vector;
import org.json.*;

public class RequestHandler extends Thread {
    private Socket socket;
    private ProducerConsumer pc;

    // each time a new request is performed, the socket
    // and the instance of the producer-consumer queue is passed
    // to the request handler
    public RequestHandler(Socket soc, ProducerConsumer pc) {
        this.socket = soc;
        this.pc = pc;
    }

    // this is used to check if the incoming data
    // contains the required information to provide
    // a response to the request. A request needs
    // to be either PUT or GET, contain user-agent, lamport-timestamp
    // accept (type), and content-length as well
    public boolean requestIsOk(String data) {
        boolean requestType = false, userAgent = false, lamport_timestamp = false, content_type = false,
                content_len = false;

        if (data.contains("PUT /weather.json HTTP/1.1") || data.contains("GET /weather.json HTTP/1.1")) {
            requestType = true;
        }
        if (data.contains("User-Agent:")) {
            userAgent = true;
        }
        if (data.contains("Lamport-Timestamp:")) {
            lamport_timestamp = true;
        }
        if (data.contains("Accept:")) {
            content_type = true;
        }
        if (data.contains("Content-Length:")) {
            content_len = true;
        }
        return requestType && userAgent && lamport_timestamp && content_len && content_type;
    }

    // this function converts the string input into a vector
    // to be passed to the Producer-Consumer queue
    private Vector<String> stringToVector(String data) {
        String[] temp = data.split("\n");
        Vector<String> ans = new Vector<>();
        for (int i = 0; i < temp.length; i++) {
            if (temp[i] != "\n") {
                ans.add(temp[i]);
            }
        }

        return ans;
    }

    // we use this to check if the requests
    // JSON is invalid
    private boolean checkIfValidJSON(String data) {
        try {
            JSONObject valid = new JSONObject(data);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // we get the user-agent from the request to pass to the getRequest
    // function to get the relevant response from the Producer-Consumer
    // function
    public String getValue(Vector<String> request, String val) {
        for (String string : request) {
            if (string.contains(val)) {
                String[] ans = string.split(":");
                if (ans.length >= 2) {
                    return ans[1];
                } else

                    return "";
            }
        }
        return null;
    }

    // This function will start any time a 
    // new request is sent, this processes the incoming
    // requests, passes it to the Producer-Consumer
    // and retrives the response and sends it to the
    // client/content server.
    @Override
    public void run() {
        try {
            PrintWriter pw = new PrintWriter(this.socket.getOutputStream(), isAlive());
            Scanner s = new Scanner(this.socket.getInputStream()).useDelimiter("\n");
            String line = "";
            // this while loop processes the 
            // requests comning in from the clients and/or 
            // content servers
            while (!requestIsOk(line)) {
                String temp = s.hasNextLine() ? s.nextLine().strip() + '\n' : "";
                line += temp;

                if (temp.isBlank()) {
                    break;
                }
            }

            // this loop parses the data 
            // from the PUT requests
            String temp = "";
            if (line.contains("PUT")) {
                while (!checkIfValidJSON(temp) && s.hasNextLine()) {
                    temp += s.nextLine().strip();
                }
            }
            line += temp;
            Vector<String> converted = stringToVector(line);
            System.out.println(line + "\r\n\r\n");
            // here we store the user agent id from the request
            // so that it can be retrived from the response map
            String id = this.pc.getValue(converted, "User-Agent");
            // here we add the user agent's request to the request queue 
            // for the producer-consumer to process
            this.pc.addRequest(converted);
            // here, we retrieve the response from the producer-consumer
            // using the user agent id and we sent it back to the 
            // client or content server 
            String res = this.pc.getRequest(id);
            pw.println(res);
            pw.flush();
            pw.close();

            this.socket.close();
        } catch (Exception e) {
            System.err.println("Aggregation Server: " + e.toString());
        }
    }

}
    // starts when a new request is received 

    /*
    Process flow
    1. Init communication - use print printwriter to send data to the client and scanner to receive. 
    2. Receive and validate - use valid_request() function. 
    3. Read data till a valid JSON payload is received. 
    4. Convert to a vector and add to producer-consumer queue. 
    5. Send an appropriate response. 
    6. Close socket. 
    */
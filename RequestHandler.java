/*
This class will handle each request from clients (GET requests) or content servers (PUT requests) in its own thread. 
*/

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner; 
import java.util.Vector;
import org.json.*;

public class RequestHandler extends Thread 
{
    private Socket socket;
    private ProducerConsumer ProducerConsumer;

    // Constructor
    public RequestHandler(Socket socket, ProducerConsumer ProducerConsumer)
    {
        this.socket = socket;
        this.ProducerConsumer = ProducerConsumer;
    }

    // check if incoming data has all required fields 
    // GET request or PUT request
    // Needs user agent, lamport time, accept and length
    public boolean valid_request(String request)
    {
        boolean request_type = false, user_agent = false, lamport_time = false, content_type = false, length = false;
        
        // request type 
        if (request.contains("PUT /weather.json HTTP/1.1") || request.contains("GET /weather.json HTTP/1.1"))
        {
            request_type = true;
        } 
        // user agent
        if (request.contains("User-Agent:"))
        {
            user_agent = true;
        }
        // lamport time stamp
        if (request.contains("Lamport-Timestamp:"))
        {
            lamport_time = true;
        }
        // content type
        if (request.contains("Accept:"))
        {
            content_type = true;
        }
        // length 
        if (request.contains("Content-length:"))
        {
            length = true;
        }
        return (request_type && user_agent && lamport_time && length && content_type);
    }
    
    // Convert the input into a vector and store each line into a vector
    private Vector<String> string_to_vector(String request)
    {
        String[] temp_string = request.split("\n");
        Vector<String> vector = new Vector<>();
        
        for (int i = 0; i < temp_string.length; i++)
        {
            if (!temp_string[i].equals("\n"))
            {
                vector.add(temp_string[i]);
            }
        }
        return vector;
    }

    // Function to validate if the string is properly formed JSON, else return false.
    // Using the org.json library to parse the request.
    private boolean valid_JSON(String request)
    {
        try
        {
            JSONObject valid = new JSONObject(request);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    // Get specific value 
    public String getValue(Vector<String> request, String value)
    {
        for (String string : request)
        {
            if (string.contains(value))
            {
                String[] return_value = string.split(":");
                if (return_value.length >= 2)
                {
                    return return_value[1].trim(); // trim to remove extra spaces
                }
                else 
                {
                    return "";
                }
            }
        }
        return null;
    }


    // Run function
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

    @Override
    public void run()
    {
        try
        {
            PrintWriter PrintWriter = new PrintWriter(this.socket.getOutputStream(), isAlive());
            Scanner Scanner = new Scanner(this.socket.getInputStream()).useDelimiter("\n");
            String line = "";

            // Receive and validate request
            while (!valid_request(line))
            {
                if (Scanner.hasNextLine())
                {
                    String temp = Scanner.nextLine().strip();
                    line += temp + '\n';

                    if (temp.isBlank())
                    {
                        break;
                    }
                }
                else
                {
                    break;
                }
            }

            // Convert string to a vector 
            Vector<String> converted_vector = string_to_vector(line);
            System.out.println(line + "\r\n\r\n");

            // Process the request with ProducerConsumer
            String id = this.ProducerConsumer.getValue(converted_vector, "User-Agent");

            // Add to request queue 
            this.ProducerConsumer.addRequest(converted_vector);

            // Retrieve response
            String res = this.ProducerConsumer.getRequest(id);

            // Send the response back to client/content server
            PrintWriter.println(res);
            PrintWriter.flush();
            PrintWriter.close();

            // Close the socket connection
            this.socket.close();
        }
        catch (Exception e)
        {
            System.err.println("Aggregation Server: " + e.toString());
        }
    }
}

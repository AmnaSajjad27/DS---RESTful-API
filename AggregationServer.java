// Start server 
// Initialise producer consumer object
// Start producer consumer thread 

/*
TRY 
    Create a new server socket 
    WHILE true
        wait for new client or content server connection
        accept connection and create socket

        create a new requestHandler thread with connected socket and Producer consumer 
        start request handler to handle connection 

print error statement 

end
*/

import java.net.*;

public class AggregationServer
{
    public static void main (String[] args)
    {
        ServerSocket server_socket = null;
        Socket socket = null;
        ProducerConsumer producer_consumer = new ProducerConsumer();
        producer_consumer.start();

        try 
        {
            server_socket = new ServerSocket(3000);
            System.out.println("Starting aggregation server...");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        while (true)
        {
            try
            {
                socket = server_socket.accept();
                new RequestHandler(socket, producer_consumer).start();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}


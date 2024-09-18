import java.net.*;

public class AggregationServer
{
    public static void main (String[] args)
    {
        // Declare server socket anf client socket 
        ServerSocket server_socket = null;
        Socket socket = null;

        // Create an instance of producer consumer to handle tasks
        ProducerConsumer producer_consumer = new ProducerConsumer();
        producer_consumer.start();

        // Initialise server on port 4567 to listen for oncoming connections
        try 
        {
            server_socket = new ServerSocket(4567);
            System.out.println("Starting aggregation server...");
        }
        // Print stack trace if server fails to intialiases 
        catch (Exception e)
        {
            e.printStackTrace();
        }
        while (true)
        {
            // Accept a new connection and create a new socket to handle communication with client
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


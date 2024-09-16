/*
Client will send a GET request to the server to retrieve weather data in JSON format.
Error handling
*/

// Imports
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.Vector;
import org.json.*;

public class Client extends Thread 
{
    private Socket socket;
    private DataInputStream server_response;
    private PrintWriter client_request;
    private String Client_id;
    private Integer lamport_time;
    private Vector<String> responses;

    public Client()
    {
        this.lamport_time = (int) Math.floor(Math.random() * (10 - 0 + 1) + 0);
        this.Client_id = String.valueOf(Thread.currentThread().getId());
    }

    public Vector<String> getResponse()
    {
        return this.responses;
    }

    @Override
    public void run()
    {
        BufferedReader user_input = new BufferedReader(new InputStreamReader(System.in));
        boolean send_request = true;
        System.out.println("Hello Client " + Client_id + "! Please enter address and port in the format address port");
        String address = null;
        Integer port = null;

        try
        {
            String temp_string = user_input.readLine();

            if (temp_string != null)
            {
                String[] connection_details = temp_string.split(":");
                address = connection_details[0];
                port = Integer.parseInt(connection_details[1]);
            }
        }
        catch (Exception e)
        {
            System.err.println("Error with reading terminal input: " + e.toString());
        }
        
        while (send_request)
        {
            try
            {
                if (address == null || port == null)
                {
                    socket = new Socket("127.0.0.1", 3000);
                }
                else 
                {
                    socket = new Socket(address, port);
                }

                this.server_response = new DataInputStream(socket.getInputStream());
                this.client_request = new PrintWriter(socket.getOutputStream());

                System.out.println("Optional: Please enter the station id (leave empty for latest data): ");
                String station_id = user_input.readLine();
                send_get_request(station_id);

                Vector<String> line = new Vector<>();
                Scanner scanner = new Scanner(this.socket.getInputStream());

                while (scanner.hasNextLine())
                {
                    line.add(scanner.nextLine());
                }

                update_lamport_time(line);
                this.responses = line;

                System.out.println("Server response for client" + this.Client_id + ":" + "\r\n\r\n");

                client_request.close();
                server_response.close();

                System.out.println("Request successful. type 'true' for another GET request, 'false' for no");

                send_request = Boolean.parseBoolean(user_input.readLine());
            }
            catch (Exception e)
            {
                System.err.println("Client " + this.Client_id + " - Failed to connect to aggregation server: " + e.toString() + "\nWould you like to try connecting again? ('true' for yes, 'false' for no)");

                try
                {
                    send_request =  Boolean.parseBoolean(user_input.readLine());
                }
                catch (Exception err)
                {
                    err.printStackTrace();
                }
            }
        }
        System.out.println("Goodbye Client" + Client_id + "!");
    }

    private void update_lamport_time(Vector<String> data)
    {
        String server_time = "";
        for (String string : data)
        {
            if (string.contains("Lamport-timestamp"))
            {
                server_time = string.split(":")[1].strip();
                this.lamport_time = Math.max(Integer.parseInt(server_time), this.lamport_time) + 1;
            }
        }
    }

    private void send_get_request(String station_id)
    {
        try
        {
            if (station_id == null || station_id.length() == 0)
            {
                this.client_request.println("GET /weather.json HTTP/1.1\nUser-Agent: Client "
                + this.Client_id + "\nLamport-Timestamp: "
                + String.valueOf(this.lamport_time)
                + "\nContent-Type: application/json\nContent-Length: 0\n");
            }
            else 
            {
                this.client_request.println("GET /weather.json HTTP/1.1\nUser-Agent: Client "
                + this.Client_id + "\nLamport-Timestamp: " 
                + String.valueOf(this.lamport_time) + "\nStation-ID:" + station_id
                + "\nContent-Type: application/json\nContent-Length: 0\n");
            }
            this.client_request.flush();
        }
        catch (Exception e)
        {
            System.err.println(e.toString());
        }
    }

    public static void main(String[] args)
    {
        Client client = new Client();
        client.start();
    }
}
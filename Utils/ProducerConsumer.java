import java.net.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.io.*;
import org.json.*;

public class ProducerConsumer extends Thread
{
    // priority queue to manage requests in order of Lamport time
    private PriorityQueue<Vector<String>> requestQueue;
    // Hashmap to store responses for each user agent
    private HashMap<String, String> responses;
    // Hashmap to store weather data for stations with time stamps
    private HashMap<String, HashMap<Instant, String>> station_weather;
    // Lock to ensure synchtonisation 
    private ReentrantLock Lock = new ReentrantLock();

    @Override

    // Initalise, load previous data and starts threads
    public void run()
    {
        this.requestQueue = new PriorityQueue<>(new LamportComparator());
        this.responses = new HashMap<>();
        this.station_weather = new HashMap<>();
        this.lock = new ReentrantLock();

        try
        {
            ObjectInputStream ObjStream = new ObjectInputStream(new FileInputStream("ProducerConsumerReplica.txt"));
            this.station_weather = (HashMap<String, HashMap<Instant, String>>) ObjStream.readObject();
            ObjStream.close();
        }
        catch (Exception e)
        {
            System.err.println(e.soString());
        }
    }

    // Removes data older than 30 seconds
    public void clear_data()
    {
        // Iterate through each station in the map
      for (Map.Entry<String, HashMap<Instant, String>> entry : stationWeather.entrySet()) 
      {
        HashMap<Instant, String> station = entry.getValue();
        // Iterate through each timestamp
        for (Map.Entry<Instant, String> innerEntry : station.entrySet()) 
        {
            Instant time = innerEntry.getKey();
            // check if data is older than 30 seconds
            if (Duration.between(time, Instant.now()).toSeconds() > 30) 
            {
                // reset the weather data 
                entry.setValue(new HashMap<>());
            }
        }
        }
        try 
        {
            // Save the updated map to a file 
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("ProducerConsumerReplica.txt"));
            oos.writeObject(stationWeather);
            oos.flush();
            oos.close();
        }
        catch (Exception e) 
        {
            System.err.println("Aggregation Server: " + e.toString());
        }
    }

    // Add request to queue 
    public void addRequest(vector<String> request)
    {
        // lock for synchronisation
        lock.lock();
        this.requestQueue.add(request);
        lock.unlock();
    }

    // Get an attribute
    public String getValue(Vector<String> request, String value)
    {
        for (String string : request)
        {
            if (string.contains(value))
            {
                String[] return_value = string.split(":");
                return return_value;
            }
        }
        return null;
    }

    // extract data from PUT request
    public String get_data(Vector<String> request)
    {
        for (String string : request)
        {
            if (string.contains("{}"))
            {
                return string.strip();
            }
        }
        return null;
    }

    // Retrive the request
    public String getRequest(String id)
    {
        if (this.responses.get(id) == null)
        {
            performRequest();
        }
        String return_value = this.response.get(id);
        // reset response for get agent 
        this.responses.put(id.null);

        return return_value;
    }

    // Process the request 
    public void performRequest()
    {
        
    }

    // Generate HTTP response code
    private String JSON_request(int code, String message, String newLamportTime)
    {

    }

    // 
    private String get(String newLamportTime)
    {

    }

    private String put(Vector<String> data)
    {

    }

    private String lamport_time(Vector<String> request)
    {

    }

}
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
    // HashMap to store responses for each user agent
    private HashMap<String, String> responses;
    // HashMap to store weather data for stations with time stamps
    private HashMap<String, HashMap<Instant, String>> station_weather;
    // Lock to ensure synchronization 
    private ReentrantLock lock = new ReentrantLock();

    @Override
    // safe cast, overriding warning 
    @SuppressWarnings("unchecked")

    // Initialize, load previous data and start threads
    public void run()
    {
        this.requestQueue = new PriorityQueue<>(new LamportComparator());
        this.responses = new HashMap<>();
        this.station_weather = new HashMap<>();
        this.lock = new ReentrantLock();

        try
        {
            ObjectInputStream objStream = new ObjectInputStream(new FileInputStream("ProducerConsumerReplica.txt"));
            this.station_weather = (HashMap<String, HashMap<Instant, String>>) objStream.readObject();
            objStream.close();
        }
        catch (Exception e)
        {
            System.err.println(e.toString());
        }
    }

    // Removes data older than 30 seconds
    public void clearData()
    {
        // Iterate through each station in the map
        for (Map.Entry<String, HashMap<Instant, String>> entry : station_weather.entrySet()) 
        {
            HashMap<Instant, String> station = entry.getValue();
            // Iterate through each timestamp
            for (Map.Entry<Instant, String> innerEntry : station.entrySet()) 
            {
                Instant time = innerEntry.getKey();
                // Check if data is older than 30 seconds
                if (Duration.between(time, Instant.now()).toSeconds() > 30) 
                {
                    // Reset the weather data 
                    entry.setValue(new HashMap<>());
                }
            }
        }

        try 
        {
            // Save the updated map to a file 
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("ProducerConsumerReplica.txt"));
            oos.writeObject(station_weather);
            oos.flush();
            oos.close();
        }
        catch (Exception e) 
        {
            System.err.println("Aggregation Server: " + e.toString());
        }
    }

    // Add request to queue 
    public void addRequest(Vector<String> request)
    {
        // Lock for synchronization
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
                String[] returnValue = string.split(":");
                return returnValue[1];
            }
        }
        return null;
    }

    // Extract data from PUT request
    public String getData(Vector<String> request)
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

    // Retrieve the request
    public String getRequest(String id)
    {
        if (this.responses.get(id) == null)
        {
            performRequest();
        }
        String returnValue = this.responses.get(id);
        // Reset response for get agent 
        this.responses.put(id, null);

        return returnValue;
    }

    // Process the request 
    // Processes the request from client and content servers in order
    public void performRequest()
    {
        lock.lock();
        // While loop to iterate 
        while (!requestQueue.isEmpty())
        {
            // Get the request
            Vector<String> request = this.requestQueue.poll();
            
            if (getValue(request, "User-Agent") == null || getValue(request, "Lamport-Timestamp") == null)
            {
                this.responses.put(getValue(request, "User-Agent"), JSONRequest(400, "{'message': 'No User-Agent or Lamport-Timestamp provided!'}", "Lamport-Timestamp: -1"));
            }
            else
            {
                // GET request
                if (request.contains("GET /weather.json HTTP/1.1"))
                {
                    // Check for station id then return 
                    if (getValue(request, "Station-ID") != null)
                    {
                        // If no data exists 
                        if (this.station_weather.get(getValue(request, "Station-ID")) == null)
                        {
                            this.responses.put(getValue(request, "User-Agent"), JSONRequest(404, "{'message': 'No data found for station id'}", readLamportTime(request)));
                        }
                        // Return data 
                        else
                        {
                            clearData();
                            this.responses.put(getValue(request, "User-Agent"), JSONRequest(200, this.station_weather.get(getValue(request, "Station-ID")).toString(), getName()));
                        }
                    }
                    else
                    {
                        // Send latest weather data 
                        this.responses.put(getValue(request, "User-Agent"), get(readLamportTime(request)));
                    }
                }
                // PUT request
                else if (request.contains("PUT /weather.json HTTP/1.1"))
                {
                    // Empty - reply with code 500
                    if (getData(request) == null || getData(request).length() == 0)
                    {
                        this.responses.put(getValue(request, "User-Agent"), JSONRequest(500, "{'message': 'No data field in request body!'}", readLamportTime(request)));
                    }
                    else
                    {
                        this.responses.put(getValue(request, "User-Agent"), put(request));
                    }
                }
                else 
                {
                    this.responses.put(getValue(request, "User-Agent"), JSONRequest(400, "{'message': 'Invalid request type'}", readLamportTime(request)));
                }
            }
        }
        lock.unlock();
    }

    // Generate HTTP response code
    private String JSONRequest(int code, String message, String newLamportTime)
    {
        String response = "";
        switch (code) 
        {
            case 400:
                response += "HTTP/1.1 400 Bad Request\n";
                break;
            case 204:
                response += "HTTP/1.1 204 No Content\n";
                break;
            case 201:
                response += "HTTP/1.1 201 Created File\n";
                break;
            case 500:
                response += "HTTP/1.1 500 Server Failure\n";
                break;
            case 200:
                response += "HTTP/1.1 200 OK\n";
                break;
            case 404:
                response += "HTTP/1.1 404 Not Found\n";
                break;
            default:
                break;
        }
        response += newLamportTime + "\nContent-Length: " + String.valueOf(message.length()) + "\nContent-Type: application/json\r\n\r\n" + message;
        return response;
    }

    // GET request function
    private String get(String newLamportTime)
    {
        try (FileReader fr = new FileReader("LatestWeatherData.json"))
        {
            JSONTokener jsonTokener = new JSONTokener(fr);
            JSONObject weather = new JSONObject(jsonTokener);

            return JSONRequest(200, weather.toString(), newLamportTime);
        }
        catch (Exception e)
        {
            return JSONRequest(500, "{'message': 'LatestWeatherData.json not found!'}", newLamportTime);
        }
    }

    private boolean validJSON(String request)
    {
        try
        {
            new JSONObject(request);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    // PUT request function 
    private String put(Vector<String> data)
    {
        String newLamportTime = readLamportTime(data);

        // Empty or not
        try
        {
            File fr = new File("LatestWeatherData.json");
            if (getData(data).length() != 0)
            {
                boolean fileExists = fr.createNewFile();
                FileWriter fw = new FileWriter("LatestWeatherData.json");

                if (!validJSON(getData(data)))
                {
                    return JSONRequest(500, "{'message': 'Invalid JSON string'}", newLamportTime);
                }
                else 
                {
                    JSONObject newData = new JSONObject(getData(data));
                    Instant timeNow = Instant.now();
                    HashMap<Instant, String> tempMap = new HashMap<>();
                    tempMap.put(timeNow, newData.toString());
                    this.station_weather.put(getValue(data, "User-Agent"), tempMap);
                    fw.write(getData(data));
                    fw.close();

                    if (!fileExists)
                    {
                        return JSONRequest(200, "{'message': 'Successfully updated weather file!'}", newLamportTime);
                    }
                    else 
                    {
                        return JSONRequest(201, "{'message': 'Successfully created LatestWeatherData.json!'}", newLamportTime);
                    }
                }
            }
            else 
            {
                return JSONRequest(204, "{'message': 'No content to update LatestWeatherData.json'}", newLamportTime);
            }
        }
        catch (Exception e)
        {
            return JSONRequest(500, "{'message': 'LatestWeatherData.json not found!'}", newLamportTime);
        }
    }

    private String readLamportTime(Vector<String> data)
    {
        Integer newTime = Integer.parseInt(getValue(data, "Lamport-Timestamp").strip());

        try (FileReader fr = new FileReader("AggregationServerLamport.json"))
        {
            JSONTokener jsonTokener = new JSONTokener(fr);
            JSONObject serverTime = new JSONObject(jsonTokener);
            newTime = Math.max(Integer.parseInt(serverTime.getString("AggregationServerLamport")), newTime) + 1;
            serverTime.put("AggregationServerLamport", String.valueOf(newTime));
            FileWriter fw = new FileWriter("AggregationServerLamport.json");
            fw.write(serverTime.toString());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return "Lamport-Timestamp: " + String.valueOf(newTime);
    }
}

class LamportComparator implements Comparator<Vector<String>>
{
    private String getValue(Vector<String> request, String value)
    {
        for (String string : request)
        {
            if (string.contains(value))
            {
                String[] returnValue = string.split(":");
                return returnValue[1];
            }
        }
        return null;
    }

    @Override
    public int compare(Vector<String> tempA, Vector<String> tempB)
    {
        Integer valueA = Integer.parseInt(getValue(tempA, "Lamport-Timestamp").strip());
        Integer valueB = Integer.parseInt(getValue(tempB, "Lamport-Timestamp").strip());

        if (valueA < valueB)
        {
            return -1;
        }
        else if (valueA > valueB)
        {
            return 1;
        }
        return 0;
    }
}

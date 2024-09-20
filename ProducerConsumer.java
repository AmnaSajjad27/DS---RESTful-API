// ProducerConsumer class manages weather data and requests using a priority queue
import java.net.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.io.*;
import org.json.*;

// Suppressing a warning 
@SuppressWarnings("unchecked")
public class ProducerConsumer extends Thread
{
    private PriorityQueue<Vector<String>> requestQueue;
    private HashMap<String, String> responses;
    private HashMap<String, HashMap<Instant, String>> stationWeather;
    private ReentrantLock lock = new ReentrantLock();

    @Override
    // Load data if file already exists 
    public void run()
    {
        this.requestQueue = new PriorityQueue<>(new LamportComparator());
        this.responses = new HashMap<>();
        this.stationWeather = new HashMap<>();
        this.lock = new ReentrantLock();

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("ProducerConsumerReplication.txt")))
        {
            this.stationWeather = (HashMap<String, HashMap<Instant, String>>) ois.readObject();
        }
        catch (Exception e)
        {
            System.err.println(e.toString());
        }
    }

    // Purge data older than 30 seconds and save the remaining data
    public void purgeData()
    {
        for (Map.Entry<String, HashMap<Instant, String>> entry : stationWeather.entrySet())
        {
            HashMap<Instant, String> station = entry.getValue();
            station.entrySet().removeIf(innerEntry -> Duration.between(innerEntry.getKey(), Instant.now()).toSeconds() > 30);
        }
        
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("ProducerConsumerReplication.txt")))
        {
            oos.writeObject(stationWeather);
        }
        catch (Exception e)
        {
            System.err.println("Aggregation Server: " + e.toString());
        }
    }

    // Add requests into a queue 
    public void addRequest(Vector<String> req)
    {
        lock.lock();
        try
        {
            this.requestQueue.add(req);
        }
        finally
        {
            lock.unlock();
        }
    }

    // Get attribute from requests 
    public String getValue(Vector<String> request, String val)
    {
        for (String string : request)
        {
            if (string.contains(val))
            {
                return string.split(":")[1];
            }
        }
        return null;
    }

    // Parse data from PUT request 
    public String getData(Vector<String> request)
    {
        for (String string : request)
        {
            if (string.contains("{"))
            {
                return string.strip();
            }
        }
        return null;
    }

    // Process the GET request 
    public String getRequest(String id)
    {
        if (this.responses.get(id) == null)
        {
            System.err.println("Null response for ID: " + id);
            performRequest();
        }

        String ans = this.responses.get(id);

        if (ans == null)
        {
            System.err.println("Null result after performRequest() for ID: " + id);
        }
        else
        {
            System.out.println("Response for ID " + id + ": " + ans);
        }

        // Reset the response
        this.responses.put(id, null);

        return ans;
    }

    // Processes requests, manages GET and PUT operations
    private void performRequest()
    {
        lock.lock();
        try
        {
            while (!requestQueue.isEmpty())
            {
                Vector<String> req = this.requestQueue.poll();

                String userAgent = getValue(req, "User-Agent");
                String timestamp = getValue(req, "Lamport-Timestamp");

                if (userAgent == null || timestamp == null)
                {
                    this.responses.put(userAgent, requestJSONgenerator(400,
                            "{'message':'No User-Agent or Lamport-Timestamp provided!'}", "Lamport-Timestamp: -1"));
                }
                else
                {
                    handleRequest(req, userAgent);
                }
            }
        }
        finally
        {
            lock.unlock();
        }
    }

    private void handleRequest(Vector<String> req, String userAgent)
    {
        if (req.contains("GET /weather.json HTTP/1.1"))
        {
            handleGetRequest(req, userAgent);
        }
        else if (req.contains("PUT /weather.json HTTP/1.1"))
        {
            handlePutRequest(req, userAgent);
        }
        else
        {
            this.responses.put(userAgent,
                    requestJSONgenerator(400, "{'message': 'Invalid request type'}", readLamportTime(req)));
        }
    }

    private void handleGetRequest(Vector<String> req, String userAgent)
    {
        String stationId = getValue(req, "Station-ID");

        if (stationId != null)
        {
            if (this.stationWeather.get(stationId) == null)
            {
                this.responses.put(userAgent, requestJSONgenerator(404,
                        "{'message': 'No data found for station id'}", readLamportTime(req)));
            }
            else
            {
                purgeData();
                this.responses.put(userAgent, requestJSONgenerator(200,
                        this.stationWeather.get(stationId).toString(), getName()));
            }
        }
        else
        {
            this.responses.put(userAgent, get(readLamportTime(req)));
        }
    }

    private void handlePutRequest(Vector<String> req, String userAgent)
    {
        if (getData(req) == null || getData(req).isEmpty())
        {
            this.responses.put(userAgent, requestJSONgenerator(500,
                    "{'message': 'No data field in request body!'}", readLamportTime(req)));
        }
        else
        {
            this.responses.put(userAgent, put(req));
        }
    }

    // Generate JSON response message
    private String requestJSONgenerator(int code, String message, String newLamportTime)
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
        response += newLamportTime + "\nContent-Length: " + message.length() + "\nContent-Type: application/json\r\n\r\n" + message;
        return response;
    }

    // GET function, return latest weather data 
    private String get(String newLamportTime)
    {
        try (FileReader fr = new FileReader("LatestWeatherData.json"))
        {
            JSONTokener jsTokener = new JSONTokener(fr);
            JSONObject weather = new JSONObject(jsTokener);
            return requestJSONgenerator(200, weather.toString(), newLamportTime);
        }
        catch (FileNotFoundException e)
        {
            return requestJSONgenerator(500, "{\"message\": \"LatestWeatherData.json not found!\"}", newLamportTime);
        }
        catch (JSONException e)
        {
            return requestJSONgenerator(500, "{\"message\": \"Error parsing JSON data.\"}", newLamportTime);
        }
        catch (Exception e)
        {
            return requestJSONgenerator(500, "{\"message\": \"An unexpected error occurred.\"}", newLamportTime);
        }
    }

    // Check if JSON data is valid 
    private boolean checkIfValidJSON(String data)
    {
        try
        {
            new JSONObject(data);
            return true;
        }
        catch (JSONException e)
        {
            return false;
        }
    }
    
    // PUT request handling
    private String put(Vector<String> data)
    {
        String newLamportTime = readLamportTime(data);

        try
        {
            File fr = new File("LatestWeatherData.json");
            if (getData(data).isEmpty())
            {
                return requestJSONgenerator(204, "{'message': 'No content to update LatestWeatherData.json'}", newLamportTime);
            }

            boolean fileExists = fr.createNewFile();
            if (!checkIfValidJSON(getData(data)))
            {
                return requestJSONgenerator(500, "{'message': 'Invalid JSON string'}", newLamportTime);
            }
            else
            {
                JSONObject newData = new JSONObject(getData(data));
                Instant currentTime = Instant.now();
                HashMap<Instant, String> temp = new HashMap<>();
                temp.put(currentTime, newData.toString());
                this.stationWeather.put(getValue(data, "User-Agent"), temp);
                
                try (FileWriter fw = new FileWriter("LatestWeatherData.json"))
                {
                    fw.write(getData(data));
                }

                return fileExists
                        ? requestJSONgenerator(201, "{'message': 'Successfully created LatestWeatherData.json!'}", newLamportTime)
                        : requestJSONgenerator(200, "{'message': 'Successfully updated weather file!'}", newLamportTime);
            }
        }
        catch (Exception e)
        {
            return requestJSONgenerator(500, "{'message': 'LatestWeatherData.json not found!'}", newLamportTime);
        }
    }

    // Update the server's lamport time
    private String readLamportTime(Vector<String> data)
    {
        int newTime = Integer.parseInt(getValue(data, "Lamport-Timestamp").strip());
        try (FileReader fr = new FileReader("AggregationServerLamport.json"))
        {
            JSONTokener jsTokener = new JSONTokener(fr);
            JSONObject serverTime = new JSONObject(jsTokener);

            newTime = Math.max(Integer.parseInt(serverTime.getString("AggregationServerLamport")), newTime) + 1;
            serverTime.put("AggregationServerLamport", String.valueOf(newTime));
            try (FileWriter fw = new FileWriter("AggregationServerLamport.json"))
            {
                fw.write(serverTime.toString());
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return "Lamport-Timestamp: " + newTime;
    }
}

// Comparator to sort the queue according to lamport time
class LamportComparator implements Comparator<Vector<String>>
{
    private String getValue(Vector<String> request, String val)
    {
        for (String string : request)
        {
            if (string.contains(val))
            {
                return string.split(":")[1];
            }
        }
        return null;
    }

    @Override
    public int compare(Vector<String> a, Vector<String> b)
    {
        int aVal = Integer.parseInt(getValue(a, "Lamport-Timestamp").strip());
        int bVal = Integer.parseInt(getValue(b, "Lamport-Timestamp").strip());
        return Integer.compare(aVal, bVal);
    }
}

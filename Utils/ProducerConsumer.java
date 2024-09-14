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
    // Processes the request from client and content servers in order
    public void performRequest()
    {
        lock.lock();
        // while loop to iterate 
        while (!requestQueue.isEmpty())
        {
            // get the request
            Vector<String> request = this.requestQueue.poll();
            
            if (getValue(request, "User-Agent") == null || getValue(request, "Lamport-Timestamp") == null)
            {
                this.responses.put(getValue(request, "User-Agent"), JSON_request(400, "{'message': 'No User-Agent or Lamport-Timestamp provided!'}", "Lamport-Timestamp: -1"));
            }
            else
            {
                // GET request
                if (request.contains("GET /weather.json HTTP/1.1"))
                {
                    // check for station id then return 
                    if (getValue(request, "Station-ID") != null)
                    {
                        // if no data exists 
                        if (this.station_weather.get(getValue(request, "Station-ID")) == null)
                        {
                            this.responses.put(getValue(request, "User-Agent"), JSON_request(404, "{'message': 'No data found for station id'}", read_lamport_time(req)));
                        }
                        // return data 
                        else
                        {
                            purge();
                            this.responses.put(getValue(request, "User-Agent"),JSON_request(200, this.station_weather.get(getValue(request, "Station-ID")).toString(),getName()));
                        }
                    }
                    else
                    {
                        // send latest weather data 
                        this.responses.put(getValue(request, "User-Agent"), get(read_lamport_time(request)));
                    }
                }
                // PUT request
                else if (request.contains("PUT /weather.json HTTP/1.1"))
                {
                    // Empty- reply with code 500
                    if (get_data(request) == null ||get_data(request).length() == 0)
                    {
                        this.responses.put(getValue(request, "User-Agent"), JSON_request(500, "{'message': 'No data field in request body!'}",read_lamport_time(request)));
                    }
                    else
                    {
                        this.responses.put(getValue(request, "User-Agent"), put(request));
                    }
                }
                else 
                {
                    this,responses.put(getValue(request, "User-Agent"),JSON_request(400, "{'message': 'Invalid request type'}", read_lamport_time(request)));
                }
            }
        }
        lock.unlock();
    }

    // Generate HTTP response code
    private String JSON_request(int code, String message, String newLamportTime)
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
            JSONTokener JSONTokener = new JSONTokener(fr);
            JSONObject weather = new JSONObject(JSONTokener);

            return JSON_request(200, weather.toString(), newLamportTime);
        }
        catch (Exception e)
        {
            return JSON_request(500, "{'message': 'LatestWeatherData.json not found!'}", newLamportTime);
        }
    }

    // PUT request function 
    private String put(Vector<String> data)
    {
        string newLamportTime = read_lamport_time(data);

        // empty or not
        try
        {
            File fr = new File("LatestWeatherData.json");
            if (get_data(data).length() != 0)
            {
                Boolean file_exists = fr.createNewFile();
                FileWriter fw = new FileWriter("LatestWeatherData.json");

                if (!valid_JSON(get_data(data)))
                {
                    return JSON_request(500, "{'message: 'Invalid JSON string'}", newLamportTime);
                }
                else 
                {
                    JSONObject new_data = new JSONObject(get_data(data));
                    Instant time_now = Instant.now();
                    HashMap temp_map = new HashMap<>();
                    temp_map.put(time_now, new_data.toString());
                    this.stationWeather.put(getValue(data, "User-Agent"), temp_map);
                    fw.write(get_data(data));
                    fw.close();

                    if (file_exists == false)
                    {
                        return JSON_request(200, "{'message': 'Successfully updated weather file!'}", newLamportTime);
                    }
                    else 
                    {
                        return JSON_request(201, "{'message': 'Successfully created LatestWeatherData.json!'}", newLamportTime);
                    }
                }
            }
            else 
            {
                return JSON_request(204, "{'message': 'No content to update LatestWeatherData.json'}",newLamportTime);
            }
        }
        catch (Exception e)
        {
            return JSON_request(500, "{'message': 'LatestWeatherData.json not found!'}", newLamportTime);
        }
    }

    private String read_lamport_time(Vector<String> data)
    {
        Integer new_time = Integer.parseInt(getValue(data, "Lamport-Timestamp").strip());

        try (FileReader fr = new FileReader("AggregationServerLamport.json"))
        {
            JSONTokener js_tokener = new JSONTokener(fr);
            JSONObject server_time = new JSONObject(js_tokener);
            new_time = Math.max(Integer.parseInt(server_time.getString("AggregationServerLamport")), new_time) + 1;
            server_time.put("AggregationServerLamport", String.valueOf(new_time));
            FileWriter fw = new FileWriter("AggregationServerLamport.json");
            fw.write(server_time.toString);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return "Lamport-Timestamp: " + String.valueOf(new_time);
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
                String[] return_value = string.split(":");
                return return_value[1];
            }
        }
        return null;
    }

    @Override
    public int compare(Vector<String> temp_a, Vector<string> temp_b)
    {
        Integer value_a = Integer.parseInt(getValue(temp_a, "Lamport-Timestamp").strip());
        Integer value_b = Integer.parseInt(getValue(temp_b, "Lamport-Timestamp").strip());

        if (value_a < value_b)
        {
            return -1;
        }
        else if (value_a > value_b)
        {
            return 1;
        }
        return 0;
    }
}

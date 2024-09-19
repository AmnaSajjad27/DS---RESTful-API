// // ProducerConsumer class manages weather data and requests using a priority queue
import java.net.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.io.*;
import org.json.*;

// suppressing a warning 
@SuppressWarnings("unchecked")
public class ProducerConsumer extends Thread
{
    private PriorityQueue<Vector<String>> request_queue;
    private HashMap<String, String> responses;
    private HashMap<String, HashMap<Instant, String>> stationWeather;
    private ReentrantLock lock = new ReentrantLock();

    @Override
    // Load data if file already exists 
    public void run() 
    {
        this.request_queue = new PriorityQueue<>(new LamportComparator());
        this.responses = new HashMap<>();
        this.stationWeather = new HashMap<>();
        this.lock = new ReentrantLock();

        try
        {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream("ProducerConsumerReplication.txt"));
            this.stationWeather = (HashMap<String, HashMap<Instant, String>>) ois.readObject();
            ois.close();
        }
        catch (Exception e)
        {
            System.err.println(e.toString());
        }
    }

    // This method purges data older than 30 seconds
    // Additionaly saves the data in a new file. 
    public void purgeData()
    {
        for (Map.Entry<String, HashMap<Instant, String>> entry : stationWeather.entrySet())
        {
            HashMap<Instant, String> station = entry.getValue();
            for (Map.Entry<Instant, String> innerEntry : station.entrySet()) 
            {
                Instant time = innerEntry.getKey();

                if (Duration.between(time, Instant.now()).toSeconds() > 30) 
                {
                    entry.setValue(new HashMap<>());
                }
            }
        }
        try 
        {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("ProducerConsumerReplication.txt"));
            oos.writeObject(stationWeather);
            oos.flush();
            oos.close();
        } 
        catch (Exception e) 
        {
            System.err.println("Aggregation Server: " + e.toString());
        }
    }

    // Adds requests into a queue 
    public void addRequest(Vector<String> req) 
    {
        lock.lock();

        this.request_queue.add(req);

        lock.unlock();
    }

    // Get attribute from requests 
    public String getValue(Vector<String> request, String val) 
    {
        for (String string : request) 
        {
            if (string.contains(val)) {
                String[] ans = string.split(":");
                return ans[1];
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

    // Processes requests, manages GET and PUT operations, and stores weather data and timestamp in stationweather map
    private void performRequest() 
    {
        lock.lock();
        // while the queue has requests, pop it, find out if its GET or PUT, perform request and purge old data
        while (!request_queue.isEmpty()) 
        {
            Vector<String> req = this.request_queue.poll();

            if (getValue(req, "User-Agent") == null || getValue(req, "Lamport-Timestamp") == null) 
            {
                this.responses.put(getValue(req, "User-Agent"), requestJSONgenerator(400,
                        "{'message':'No User-Agent or Lamport-Timestamp provided!'}", "Lamport-Timestamp: -1"));
            }
            else 
            {
                if (req.contains("GET /weather.json HTTP/1.1")) 
                {

                    if (getValue(req, "Station-ID") != null) 
                    {
                        // no station id found 
                        if (this.stationWeather.get(getValue(req, "Station-ID")) == null) 
                        {
                            this.responses.put(getValue(req, "User-Agent"), requestJSONgenerator(404,
                                    "{'message': 'No data found for station id'}", readLamportTime(req)));
                        }
                        else 
                        {
                            // Purge and send response
                            purgeData();
                            this.responses.put(getValue(req, "User-Agent"), requestJSONgenerator(200,
                            this.stationWeather.get(getValue(req, "Station-ID")).toString(), getName()));
                        }
                    }
                    else 
                    {
                        // If there no station id, send latest 
                        this.responses.put(getValue(req, "User-Agent"), get(readLamportTime(req)));
                    }
                }
                // PUT request - write to json file 
                else if (req.contains("PUT /weather.json HTTP/1.1"))
                {
                    // Code 500 - no data 
                    if (getData(req) == null || getData(req).length() == 0) 
                    {
                        this.responses.put(getValue(req, "User-Agent"), requestJSONgenerator(500, 
                        "{'message': 'No data field in request body!'}", readLamportTime(req)));
                    } 
                    else 
                    {
                        this.responses.put(getValue(req, "User-Agent"), put(req));
                    }
                }
                // 400 error for invalid request
                else 
                {
                    this.responses.put(getValue(req, "User-Agent"),
                            requestJSONgenerator(400, "{'message': 'Invalid request type'}", readLamportTime(req)));
                }
            }
        }
        lock.unlock();
    }

    // Pass message and updated lamport time stamp 
    private String requestJSONgenerator(int code, String message, String newLamportTime) 
    {
        String response = "";

        switch (code) {
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
        response += newLamportTime + "\nContent-Length: " + String.valueOf(message.length())
                + "\nContent-Type: application/json\r\n\r\n" + message;
        return response;
    }

    // GET function, return latest weather data 
    private String get(String newLamportTime) 
    {
    try (FileReader fr = new FileReader("LatestWeatherData.json")) 
    {
        JSONTokener jsTokener = new JSONTokener(fr);
        JSONObject weather = new JSONObject(jsTokener);

        // code 200 - successfull response 
        return requestJSONgenerator(200, weather.toString(), newLamportTime);
    }
    catch (FileNotFoundException e) 
    {
        // code 500 - file does not exist 
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

    // Check if json data is valid 
    private boolean checkIfValidJSON(String data) 
    {
        try 
        {
            JSONObject valid = new JSONObject(data);
            return true;
        }
        catch (Exception e) 
        {
            return false;
        }
    }

    // PUT request - performReqest calls put to write data and to mao 
    // reuturns 200 if success, 201 if the latest weather file, 204 for empty data, 500 for any other issues
    private String put(Vector<String> data) 
    {
        String newLamportTime = readLamportTime(data);
        try 
        {
            File fr = new File("LatestWeatherData.json");
            // Check data field 
            if (getData(data).length() != 0) 
            {
                Boolean fileExists = fr.createNewFile();
                FileWriter fw = new FileWriter("LatestWeatherData.json");

                if (!checkIfValidJSON(getData(data))) 
                {
                    return requestJSONgenerator(500, "{'message': 'Invalid JSON string'}", newLamportTime);
                } 
                else 
                {
                    JSONObject newData = new JSONObject(getData(data));
                    Instant currentTime = Instant.now();
                    HashMap temp = new HashMap<>();
                    temp.put(currentTime, newData.toString());
                    this.stationWeather.put(getValue(data, "User-Agent"), temp);
                    fw.write(getData(data));
                    fw.close();

                    if (fileExists == false) 
                    {
                        return requestJSONgenerator(200, "{'message': 'Successfully updated weather file!'}",
                                newLamportTime);
                    }
                    else 
                    {
                        return requestJSONgenerator(201, "{'message': 'Successfully created LatestWeatherData.json!'}",
                                newLamportTime);
                    }
                }

            } 
            else 
            {
                return requestJSONgenerator(204, "{'message': 'No content to update LatestWeatherData.json'}",
                        newLamportTime);
            }
        } 
        catch (Exception e) 
        {
            return requestJSONgenerator(500, "{'message': 'LatestWeatherData.json not found!'}", newLamportTime);
        }
    }
    
    // Each time GET and PUT is called, server's lamport time is updated 
    private String readLamportTime(Vector<String> data) 
    {
        Integer newTime = Integer.parseInt(getValue(data, "Lamport-Timestamp").strip());
        try (FileReader fr = new FileReader("AggregationServerLamport.json")) 
        {
            JSONTokener jsTokener = new JSONTokener(fr);
            JSONObject serverTime = new JSONObject(jsTokener);

            newTime = Math.max(Integer.parseInt(serverTime.getString("AggregationServerLamport")), newTime) + 1;
            serverTime.put("AggregationServerLamport", String.valueOf(newTime));
            FileWriter fw = new FileWriter("AggregationServerLamport.json");
            fw.write(serverTime.toString());
            fw.close();
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
        return "Lamport-Timestamp: " + String.valueOf(newTime);
    }
}

// Sort the queue acording to lamport time
class LamportComparator implements Comparator<Vector<String>> 
{

    private String getValue(Vector<String> request, String val) 
    {
        for (String string : request) 
        {
            if (string.contains(val)) 
            {
                String[] ans = string.split(":");
                return ans[1];
            }
        }
        return null;
    }

    @Override
    public int compare(Vector<String> a, Vector<String> b) 
    {
        Integer aVal = Integer.parseInt(getValue(a, "Lamport-Timestamp").strip());
        Integer bVal = Integer.parseInt(getValue(b, "Lamport-Timestamp").strip());
        if (aVal < bVal) 
        {
            return -1;
        } 
        else if (aVal > bVal) 
        {
            return 1;
        }
        return 0;
    }
}
import java.net.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.io.*;
import org.json.*;

public class ProducerConsumer extends Thread {
    private PriorityQueue<Vector<String>> requestQueue;
    private HashMap<String, String> responses;
    private HashMap<String, HashMap<Instant, String>> stationWeather;
    private ReentrantLock lock = new ReentrantLock();

    @Override
    public void run() {
        this.requestQueue = new PriorityQueue<>(new LamportComparator());
        this.responses = new HashMap<>();
        this.stationWeather = new HashMap<>();
        this.lock = new ReentrantLock();

        // load data from producer consumer replication file if it exists
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream("ProducerConsumerReplication.txt"));
            this.stationWeather = (HashMap<String, HashMap<Instant, String>>) ois.readObject();
            ois.close();
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

    public void purgeData() {
        // purge any data that is older than 30 seconds
        for (Map.Entry<String, HashMap<Instant, String>> entry : stationWeather.entrySet()) {
            HashMap<Instant, String> station = entry.getValue();
            for (Map.Entry<Instant, String> innerEntry : station.entrySet()) {
                Instant time = innerEntry.getKey();

                if (Duration.between(time, Instant.now()).toSeconds() > 30) {
                    entry.setValue(new HashMap<>());
                }
            }
        }
        try {
            // save the new data into a file in the event we need to recover data
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("ProducerConsumerReplication.txt"));
            oos.writeObject(stationWeather);
            oos.flush();
            oos.close();
        } catch (Exception e) {
            System.err.println("Aggregation Server: " + e.toString());
        }
    }

    // this function allows the request handler to
    // add requests to the request queue
    public void addRequest(Vector<String> req) {
        lock.lock();

        this.requestQueue.add(req);

        lock.unlock();
    }

    // this function gets any particular attribute from
    // the request such as lamport timestamp or the user agent
    // etc.
    public String getValue(Vector<String> request, String val) {
        for (String string : request) {
            if (string.contains(val)) {
                String[] ans = string.split(":");
                return ans[1];
            }
        }
        return null;
    }

    // this will parse the weather data from the PUT request
    public String getData(Vector<String> request) {
        for (String string : request) {
            if (string.contains("{")) {
                return string.strip();
            }
        }
        return null;
    }

    // this will process the request
    // if the request does not exist for the
    // user agent yet
 public String getRequest(String id) {
    if (this.responses.get(id) == null) {
        System.err.println("Null response for ID: " + id);
        performRequest();
    }

    String ans = this.responses.get(id);

    if (ans == null) {
        System.err.println("Null result after performRequest() for ID: " + id);
    } else {
        System.out.println("Response for ID " + id + ": " + ans);
    }

    // Reset the response
    this.responses.put(id, null);

    return ans;
}


    // this function will process the requests sent in from
    // clients and content servers in the order of the lamport
    // timestamp and be stored in the responses map and store
    // station specific weather data and its timestamp in the
    // stationWeather map
    private void performRequest() {
        lock.lock();
        while (!requestQueue.isEmpty()) {
            // pop request from the request queue
            Vector<String> req = this.requestQueue.poll();
            if (getValue(req, "User-Agent") == null || getValue(req, "Lamport-Timestamp") == null) {
                this.responses.put(getValue(req, "User-Agent"), requestJSONgenerator(400,
                        "{'message':'No User-Agent or Lamport-Timestamp provided!'}", "Lamport-Timestamp: -1"));
            } else {
                // if request is GET, read file and perform GET request
                if (req.contains("GET /weather.json HTTP/1.1")) {
                    // check if the get request has a station id then we return
                    // the weather data associated with that station id
                    if (getValue(req, "Station-ID") != null) {
                        // if we have received a station id, we send a message saying that
                        // no data exists for that station id
                        if (this.stationWeather.get(getValue(req, "Station-ID")) == null) {
                            this.responses.put(getValue(req, "User-Agent"), requestJSONgenerator(404,
                                    "{'message': 'No data found for station id'}", readLamportTime(req)));
                        }
                        // if data exists just return that data
                        else {
                            // check if data is older than 30 seconds, purge and send response
                            purgeData();
                            // return data after purging data older than 30 seconds
                            this.responses.put(getValue(req, "User-Agent"),
                                    requestJSONgenerator(200,
                                            this.stationWeather.get(getValue(req, "Station-ID")).toString(),
                                            getName()));
                        }
                    } else {
                        // since station id is optional, so if no station id exists
                        // we can just send the latest weather data
                        this.responses.put(getValue(req, "User-Agent"), get(readLamportTime(req)));
                    }
                }
                // if request is PUT, write to LatestWeatherData.json file and perform PUT
                // request
                else if (req.contains("PUT /weather.json HTTP/1.1")) {
                    // if data is empty then respond with code 500 for no data in request body
                    if (getData(req) == null || getData(req).length() == 0) {
                        this.responses.put(getValue(req, "User-Agent"),
                                requestJSONgenerator(500, "{'message': 'No data field in request body!'}",
                                        readLamportTime(req)));
                    } else {
                        this.responses.put(getValue(req, "User-Agent"), put(req));
                    }
                }
                // send back a 500 error for invalid request
                else {
                    this.responses.put(getValue(req, "User-Agent"),
                            requestJSONgenerator(400, "{'message': 'Invalid request type'}", readLamportTime(req)));
                }
            }
        }
        lock.unlock();
    }

    // this generates the JSON for the GET and PUT responses
    // it also allows to pass in a message and a new lamport timestamp
    private String requestJSONgenerator(int code, String message, String newLamportTime) {
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

    // this is the GET request function
    // when the client performs GET, the performRequest
    // function calls this to return the latest weather data
    // if the file doesn't exist, then it returns a 500 error
   private String get(String newLamportTime) {
    try (FileReader fr = new FileReader("LatestWeatherData.json")) {
        JSONTokener jsTokener = new JSONTokener(fr);
        JSONObject weather = new JSONObject(jsTokener);

        // Generate a successful response
        return requestJSONgenerator(200, weather.toString(), newLamportTime);
    } catch (FileNotFoundException e) {
        // Handle the case where the file does not exist
        return requestJSONgenerator(500, "{\"message\": \"LatestWeatherData.json not found!\"}", newLamportTime);
    } catch (JSONException e) {
        // Handle JSON parsing errors
        return requestJSONgenerator(500, "{\"message\": \"Error parsing JSON data.\"}", newLamportTime);
    } catch (Exception e) {
        // Catch any other exceptions
        return requestJSONgenerator(500, "{\"message\": \"An unexpected error occurred.\"}", newLamportTime);
    }
}

    // we use this to check if the JSON data in the PUT request is invalid
    private boolean checkIfValidJSON(String data) {
        try {
            JSONObject valid = new JSONObject(data);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // this is the PUT request function
    // when the content server performs PUT, the performRequest
    // function calls this to write the latest weather data to the file
    // and also writes to the stationWeather map as well to store the
    // weather data. It returns 200 if success, 201 if the latest weather file
    // , 204 for empty data, 500 for any other issues
    private String put(Vector<String> data) {
        String newLamportTime = readLamportTime(data);
        try {
            File fr = new File("LatestWeatherData.json");
            // check if data field is empty or not
            if (getData(data).length() != 0) {
                Boolean fileExists = fr.createNewFile();
                FileWriter fw = new FileWriter("LatestWeatherData.json");

                // check if response format is correct
                if (!checkIfValidJSON(getData(data))) {
                    return requestJSONgenerator(500, "{'message': 'Invalid JSON string'}", newLamportTime);
                } else {
                    JSONObject newData = new JSONObject(getData(data));
                    Instant currentTime = Instant.now();
                    HashMap temp = new HashMap<>();
                    temp.put(currentTime, newData.toString());
                    this.stationWeather.put(getValue(data, "User-Agent"), temp);
                    fw.write(getData(data));
                    fw.close();

                    if (fileExists == false) {
                        return requestJSONgenerator(200, "{'message': 'Successfully updated weather file!'}",
                                newLamportTime);
                    } else {
                        return requestJSONgenerator(201, "{'message': 'Successfully created LatestWeatherData.json!'}",
                                newLamportTime);
                    }
                }

            } else {
                return requestJSONgenerator(204, "{'message': 'No content to update LatestWeatherData.json'}",
                        newLamportTime);
            }
        } catch (Exception e) {
            return requestJSONgenerator(500, "{'message': 'LatestWeatherData.json not found!'}", newLamportTime);
        }
    }

    // this will update the aggregation server's lamport
    // file, which is used to store the lamport timestamp
    // it will be called each time a GET or PUT request is called
    // to update the aggregation server's lamport timestamp
    private String readLamportTime(Vector<String> data) {
        Integer newTime = Integer.parseInt(getValue(data, "Lamport-Timestamp").strip());
        try (FileReader fr = new FileReader("AggregationServerLamport.json")) {
            JSONTokener jsTokener = new JSONTokener(fr);
            JSONObject serverTime = new JSONObject(jsTokener);
            newTime = Math.max(Integer.parseInt(serverTime.getString("AggregationServerLamport")), newTime) + 1;
            serverTime.put("AggregationServerLamport", String.valueOf(newTime));
            FileWriter fw = new FileWriter("AggregationServerLamport.json");
            fw.write(serverTime.toString());
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Lamport-Timestamp: " + String.valueOf(newTime);
    }

}

// this is the custom comparator that sorts the priority queue according to the
// lamport timestamp. it will compare the lamport timestamps from the requests
// and compare and push the request with the lower value timestamp nearer to the
// front of the priority queue.
class LamportComparator implements Comparator<Vector<String>> {

    private String getValue(Vector<String> request, String val) {
        for (String string : request) {
            if (string.contains(val)) {
                String[] ans = string.split(":");
                return ans[1];
            }
        }
        return null;
    }

    @Override
    public int compare(Vector<String> a, Vector<String> b) {
        Integer aVal = Integer.parseInt(getValue(a, "Lamport-Timestamp").strip());
        Integer bVal = Integer.parseInt(getValue(b, "Lamport-Timestamp").strip());
        if (aVal < bVal) {
            return -1;
        } else if (aVal > bVal) {
            return 1;
        }
        return 0;
    }
}
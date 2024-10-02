import org.json.JSONException;
import org.json.JSONObject;

public class TestInvalidJSONParsing
{
    public static void main(String[] args) 
    {
        // Example invalid JSON input
        String jsonInput = "{\"stationId\": \"123\", \"temperature\": \"25\", \"humidity\": 85"; // Missing closing brace
        
        try 
        {
            // Attempt to parse the invalid JSON input
            JSONObject jsonObj = new JSONObject(jsonInput);
            // If parsing succeeds, the test has failed since we expected an exception
            System.out.println("TestInvalidJSONParsing: FAILED - No exception was thrown for invalid JSON input.");
        } 
        catch (JSONException e) 
        {
            // Expected exception, indicating the JSON parsing failed
            System.out.println("TestInvalidJSONParsing: SUCCESS - Caught JSONException as expected.");
            System.out.println("Exception Message: " + e.getMessage());
            System.out.println("Invalid JSON Input: " + jsonInput);
        }
    }
}

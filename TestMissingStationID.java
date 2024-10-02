import org.json.JSONObject;

public class TestMissingStationID
{
    public static void main(String[] args)
    {
        // JSON input without stationId
        String jsonInput = "{\"temperature\": \"25\", \"humidity\": \"85\"}";

        try 
        {
            // Parse the JSON input
            JSONObject jsonObj = new JSONObject(jsonInput);

            // Check for the presence of the "stationId" key
            if (!jsonObj.has("stationId")) 
            {
                System.out.println("TestMissingStationID: SUCCESS - 'stationId' is missing as expected.");
                System.out.println("Reason: The input JSON does not include a 'stationId' key.");
            } 
            else 
            {
                System.out.println("TestMissingStationID: FAILED - 'stationId' should be missing.");
                System.out.println("Actual Output: 'stationId' key found in JSON input.");
            }
        } 
        catch (Exception e) 
        {
            System.out.println("TestMissingStationID: FAILED - An unexpected exception occurred.");
            System.out.println("Exception Message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

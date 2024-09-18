import java.util.Vector;

public class DataExpunge {
    public boolean dataExpunging() {
        // here we will test whether data expunging after 30 seconds works
        // a content server will send its data to the aggregation server
        // then the client will attempt to get the content server's data
        // after 31 seconds. if the data is null, then the data expunging works
        ContentServer cs = new ContentServer();
        cs.start();

        try {
            cs.join();
            Thread.sleep(31000);

            GETClient c1 = new GETClient();
            c1.start();
            Vector<String> response = c1.getResponse();
            c1.join();
            // if the response from the server is null
            // then the data expunging works
            if (response == null) {
                return true;
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        return false;
    }

    public static void main(String[] args) {
        DataExpunge de = new DataExpunge();
        System.out.println("\033[0;1mData Expunge works? " + de.dataExpunging() + "\033[0m");
    }
}
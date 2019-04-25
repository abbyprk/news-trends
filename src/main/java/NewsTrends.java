import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Abby Parker
 * CSIS 612
 *
 * This application pulls data from the NewsAPI or from storage for the US states and territories by date and calculates trends in words over time.
 */
public class NewsTrends {
    public HashMap<String,Map<String,Integer>> cachedData;


    public static void main(String[] args) {
        //TODO: remove hard coded values and look up the results based on the query criteria

        StateNewsDataHandler handler = new StateNewsDataHandler();

        //TODO: get data from cache if it exists, if not then retrieve the data and add it to the cache
        HashMap<String, Integer> data = handler.processNewsTrendsByState(new Date(), 5);

        //TODO: need to generate html to display the results
        for (HashMap.Entry<String, Integer> entry : data.entrySet()) {
            System.out.println("Word: " + entry.getKey() + " - #" + entry.getValue());
        }
    }
}

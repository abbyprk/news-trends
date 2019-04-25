import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Abby Parker
 * CSIS 612
 *
 * This class uses thread-level and data-level parallelism to retrieve and process news data and establish trends across US States.
 */
public class StateNewsDataHandler {
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private HashMap<String, Integer> newsTrendsMap;
    private Properties properties;
    private static final String[] STATES = {"ALABAMA", "ALASKA","ARIZONA", "ARKANSAS", "CALIFORNIA",
            "COLORADO", "CONNECTICUT","DELAWARE", "FLORIDA", "GEORGIA", "HAWAII", "IDAHO", "ILLINOIS", "INDIANA",
            "IOWA", "KANSAS", "KENTUCKY", "LOUISANA", "MAINE", "MARYLAND", "MASSACHUSETTS", "MICHIGAN", "MINNESOTA",
            "MISSISSIPPI", "MISSOURI", "MONTANA", "NEBRASKA", "NEVADA", "NEW_HAMPSHIRE", "NEW_JERSEY", "NEW_MEXICO",
            "NEW_YORK", "NORTH_CAROLINA", "NORTH_DAKOTA", "OHIO", "OKLAHOMA", "OREGON", "PENNSYLVANIA", "RHODE_ISLAND",
            "SOUTH_CAROLINA", "SOUTH_DAKOTA", "TENNESSEE", "TEXAS", "UTAH", "VERMONT", "VIRGINIA", "WASHINGTON",
            "WEST_VIRGINIA", "WISCONSIN", "WYOMING"};

    protected StateNewsDataHandler() {
        this.newsTrendsMap = new HashMap<>();
        properties = new Properties();

        try {
            InputStream inputStream = this.getClass().getResourceAsStream("Application.properties");;
            properties.load(inputStream);
        } catch (IOException e) {
            System.out.println("Could not load properties to store data. Exception: " + e.getMessage());
        }
    }

    /**
     * processNewsTrendsByState
     *
     * This method accepts two parameters which tells us how many days we need to process and
     * which day to start on. The method will take the date passed in and retrieve data for X days
     * prior to that when processing.
     *
     * The method loops through each state for the dates requested and spins up a NewsProcessorThread for
     * every file containing news data that was found (data-level parallelism). If the file for the
     * date requested is not found, the method will create a new NewsAPIRetrieverThread to reach out to the
     * NewsAPI and store down the data.  These two tasks will run in parallel (as applicable) until all of the
     * requested data has been processed or retrieved.
     *
     * @param date - date to start with
     * @param totalDays - the total number of days back that we should count
     * @return newsTrendsMap - updated with all the data processed by the NewsProcessorThreads
     */
    protected HashMap<String, Integer> processNewsTrendsByState(Date date, int totalDays) {
        Calendar calDate = Calendar.getInstance();
        List<Thread> activeThreadsList = new ArrayList<>();
        String newsFilePath = properties.getProperty("path.to.data");
        String apiKey = properties.getProperty("api.key");

        try {
            for (String state : STATES) {
                System.out.println("Processing " + state + "...");
                for (int dayCount = totalDays; dayCount > 0; dayCount--) {
                    calDate.setTime(date);
                    calDate.add(Calendar.DATE, -dayCount);
                    String dateToPull = dateFormat.format(calDate.getTime());
                    File newsForDate = new File(newsFilePath + state + "/" + dateToPull + ".json");
                    if (newsForDate.exists()) {
                        String data = new String(Files.readAllBytes(Paths.get(newsForDate.getPath())));  //get the data from the file
                        JsonNode newsDataJson = new ObjectMapper().readTree(data);

                        //Spin up a new thread to process the data
                        NewsProcessorThread newsProcessorThread = new NewsProcessorThread(newsTrendsMap, newsDataJson, properties);
                        activeThreadsList.add(newsProcessorThread);
                        newsProcessorThread.start();
                    } else {
                        /*
                          If we don't currently have the data, then we need to reach out to the NewsAPI to retrieve it and store it.
                          Spin up a thread to do this work while we're processing the rest of the data.
                          The data will be available on the next run (of the application) if it was successfully retrieved and stored
                        */

                        String directoryPath = newsFilePath + state;
                        String fileName = dateToPull + ".json";
                        NewsAPIRetrieverThread retrieveNewsThread = new NewsAPIRetrieverThread(state, dateToPull, dateToPull,
                                "popularity", "en", apiKey,directoryPath, fileName, properties.getProperty("news.api.path"));
                        activeThreadsList.add(retrieveNewsThread);
                        retrieveNewsThread.start();

                        System.out.println("ALERT: There was data missing for " + state + " for " + dateToPull
                                + ". We are pulling the data while your other results are calculated. The data will be available the next time you search");
                    }
                }
            }
        } catch (Exception ie) {
            System.out.println("There was a problem processing the data: " + ie.getMessage());
        }

        while (threadsAlive(activeThreadsList)) {} //wait until all the threads are finished, then return the results

        return newsTrendsMap;
    }


    /**
     * Checks to see if any of the threads are still doing work.
     * @param threadsToCheck
     * @return true if at least one thread is alive, false if all are done
     */
    private static boolean threadsAlive(List<Thread> threadsToCheck) {
        for (Thread t : threadsToCheck) {
            if (t.isAlive()) {
                return true;
            }
        }
        return false;
    }
}

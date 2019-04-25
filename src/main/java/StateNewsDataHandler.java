import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import j2html.tags.ContainerTag;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import static j2html.TagCreator.*;

/**
 * Abby Parker
 * CSIS 612
 *
 * Handles retrieving data and farming out processing to the NewsProcessorThreads
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
     *
     * @param date - date to start with
     * @param totalDays - the total number of days back that we should count
     * @return
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
                        //If we don't currently have the data, then we need to reach out to the NewsAPI to retrieve it and store it.
                        //Spin up a thread to do this work while we're processing the rest of the data.
                        //The data will be available on the next run if it was successfully retrieved
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

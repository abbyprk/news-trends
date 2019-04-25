import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Abby Parker
 * CSIS 612
 *
 * Handles retrieving data and farming out processing to the NewsProcessorThreads
 */
public class StateNewsDataHandler {
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private HashMap<String, Integer> newsTrendsMap;
    private static String[] STATES = {"ALABAMA", "ALASKA","ARIZONA", "ARKANSAS", "CALIFORNIA",
            "COLORADO", "CONNECTICUT","DELAWARE", "FLORIDA", "GEORGIA", "HAWAII", "IDAHO", "ILLINOIS", "INDIANA",
            "IOWA", "KANSAS", "KENTUCKY", "LOUISANA", "MAINE", "MARYLAND", "MASSACHUSETTS", "MICHIGAN", "MINNESOTA",
            "MISSISSIPPI", "MISSOURI", "MONTANA", "NEBRASKA", "NEVADA", "NEW_HAMPSHIRE", "NEW_JERSEY", "NEW_MEXICO",
            "NEW_YORK", "NORTH_CAROLINA", "NORTH_DAKOTA", "OHIO", "OKLAHOMA", "OREGON", "PENNSYLVANIA", "RHODE_ISLAND",
            "SOUTH_CAROLINA", "SOUTH_DAKOTA", "TENNESSEE", "TEXAS", "UTAH", "VERMONT", "VIRGINIA", "WASHINGTON",
            "WEST_VIRGINIA", "WISCONSIN", "WYOMING"};

    protected StateNewsDataHandler() {
        this.newsTrendsMap = new HashMap<>();
    }

    /**
     *
     * @param date - date to start with
     * @param totalDays - the total number of days back that we should count
     * @return
     */
    protected HashMap processNewsTrendsByState(Date date, int totalDays) {
        Properties properties = new Properties();
        Calendar calDate = Calendar.getInstance();
        List<NewsProcessorThread> newsProcessorThreadList = new ArrayList<>();
        HttpClientBuilder client = HttpClientBuilder.create();
        HttpGet getNewsApiDataRequest;
        boolean tooManyRequests = false;


        try {
            InputStream inputStream = this.getClass().getResourceAsStream("Application.properties");;
            properties.load(inputStream);
        } catch (IOException e) {
            System.out.println("Could not load properties to store data. Exception: " + e.getMessage());
        }

        //TODO: try to get json from file, look up by date and keyword
        String newsFilePath = properties.getProperty("path.to.data");
        String apiKey = properties.getProperty("api.key");

        try {
            for (String state : STATES) {
                for (int dayCount = totalDays; dayCount > 0; dayCount--) {
                    calDate.setTime(date);
                    calDate.add(Calendar.DATE, -dayCount);
                    String dateToPull = dateFormat.format(calDate.getTime());
                    File newsForDate = new File(newsFilePath + state + "/" + dateToPull + ".json");
                    if (newsForDate.exists()) {
                        //process the data for the date
                        String data = new String(Files.readAllBytes(Paths.get(newsForDate.getPath())));
                        JsonNode newsDataJson = new ObjectMapper().readTree(data);

                        //Spin up a new thread to process the data
                        NewsProcessorThread newsProcessorThread = new NewsProcessorThread(newsTrendsMap, newsDataJson);
                        newsProcessorThreadList.add(newsProcessorThread);
                        newsProcessorThread.start();
                    } else if (!tooManyRequests){
                        //TODO: if the data does not exist in the files then hit the newsApi
                        URIBuilder builder = new URIBuilder(properties.getProperty("news.api.path"));
                        String stateParam = state.toLowerCase().replace("_", " ");
                        builder.setParameter("q", stateParam)
                                .setParameter("from", dateToPull)
                                .setParameter("to", dateToPull)
                                .setParameter("sortBy", "popularity")
                                .setParameter("pageSize", "100")
                                .setParameter("language", "en")
                                .setParameter("apiKey", apiKey);

                        getNewsApiDataRequest = new HttpGet(builder.build());
                        HttpResponse newsApiResponse = client.build().execute(getNewsApiDataRequest);

                        if (newsApiResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                            JsonNode newsDataJson = new ObjectMapper().readTree(newsApiResponse.getEntity().getContent());

                            //Spin up a new thread to process the data
                            NewsProcessorThread newsProcessorThread = new NewsProcessorThread(newsTrendsMap, newsDataJson);
                            newsProcessorThreadList.add(newsProcessorThread);
                            newsProcessorThread.start();

                            String directoryPath = newsFilePath + state;
                            String fileName = dateToPull + ".json";
                            writeNewsDataToFile(newsDataJson.toString(), directoryPath, fileName);
                        } else if (newsApiResponse.getStatusLine().getStatusCode() == 429) {
                            System.out.println("The number of requests for the developer account has been exceeeded. Do not retry");
                            tooManyRequests = true;
                        }
                    }
                }
            }
        } catch (Exception ie) {
            System.out.println("There was a problem processing the data: " + ie.getMessage());
        }

        while (threadsAlive(newsProcessorThreadList)) {} //wait until all the threads are finished, then return the results

        /*
        * https://www.baeldung.com/java-hashmap-sort
        * Sort the data by key using stream
        */
        HashMap<String, Integer> sortedResults;
        synchronized (newsTrendsMap) {
            sortedResults = newsTrendsMap.entrySet()
                    .stream()
                    .sorted((Comparator<Map.Entry<String, Integer>> & Serializable)
                            (c2, c1) -> c1.getValue().compareTo(c2.getValue()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        }


        return sortedResults;
    }

    private void writeNewsDataToFile(String dataToWrite, String directoryPath, String fileName) throws IOException {
        File directory = new File(directoryPath);
        if (! directory.exists()){
            directory.mkdirs();
        }
        File file = new File (directoryPath + "/" + fileName);
        try (BufferedWriter br = Files.newBufferedWriter(Paths.get(file.getPath()))) {
            br.write(dataToWrite);
        }
    }

    /**
     *
     * @param date - date to start with
     * @param totalDays - the total number of days back that we should count
     * @return
     */
 /*   protected HashMap processNewsTrendsByState(Date date, Integer totalDays) {
        Properties properties = new Properties();

        try {
            InputStream inputStream = this.getClass().getResourceAsStream("Application.properties");;
            properties.load(inputStream);
        } catch (IOException e) {
            System.out.println("Could not load properties to store data. Exception: " + e.getMessage());
        }

        //TODO: try to get json from file, look up by date and keyword
        String newsFilePath = properties.getProperty("path.to.data") + "newmexico"; //hardcoded
        File newsDataDir = new File(newsFilePath);
        File[] newsData = newsDataDir.listFiles();
        List<NewsProcessorThread> newsProcessorThreadList = new ArrayList<>();
        int numThreads = 0;

        try {
            if (newsData != null) {
                for (File news : newsData) {
                    //only process json data in the folder
                    if (news.getName().matches(".*\\.json")) {
                        String data = new String(Files.readAllBytes(Paths.get(news.getPath())));
                        JsonNode newsDataJson = new ObjectMapper().readTree(data);

                        //Spin up a new thread to process the data
                        NewsProcessorThread newsProcessorThread = new NewsProcessorThread(newsTrendsMap, newsDataJson);
                        newsProcessorThreadList.add(newsProcessorThread);
                        newsProcessorThread.start();
                    }
                }

            } else {
                //TODO: if the data does not exist in the files then hit the newsApi
                System.out.println("Could not find any data to process");
                //TODO: For each file, spin up a thread to process the data
            }
        } catch (IOException ie) {
            System.out.println(ie.getMessage());
        }

        while (threadsAlive(newsProcessorThreadList)) {} //wait until all the threads are finished, then return the results

        /*
        * https://www.baeldung.com/java-hashmap-sort
        * Sort the data by key using stream
        */
       /* HashMap<String, Integer> sortedResults = newsTrendsMap.entrySet()
                .stream()
                .sorted((Comparator<Map.Entry<String, Integer>> & Serializable)
                        (c2, c1) -> c1.getValue().compareTo(c2.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));


        return sortedResults;
    } */

    /**
     * Checks to see if any of the threads are still doing work.
     * @param newsProcessorThreads
     * @return true if at least one thread is alive, false if all are done
     */
    private static boolean threadsAlive(List<NewsProcessorThread> newsProcessorThreads) {
        for (NewsProcessorThread t : newsProcessorThreads) {
            if (t.isAlive()) {
                return true;
            }
        }
        return false;
    }

}

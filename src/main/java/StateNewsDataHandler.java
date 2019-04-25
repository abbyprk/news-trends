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
        List<NewsProcessorThread> newsProcessorThreadList = new ArrayList<>();
        HttpClientBuilder client = HttpClientBuilder.create();
        HttpGet getNewsApiDataRequest;
        boolean tooManyRequests = false;

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
                        //process the data for the date
                        String data = new String(Files.readAllBytes(Paths.get(newsForDate.getPath())));
                        JsonNode newsDataJson = new ObjectMapper().readTree(data);

                        //Spin up a new thread to process the data
                        NewsProcessorThread newsProcessorThread = new NewsProcessorThread(newsTrendsMap, newsDataJson, properties);
                        newsProcessorThreadList.add(newsProcessorThread);
                        newsProcessorThread.start();
                    } else if (!tooManyRequests){
                        //TODO: this should be kicked off and then processed by a new thread. takes too long to return
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
                            NewsProcessorThread newsProcessorThread = new NewsProcessorThread(newsTrendsMap, newsDataJson, properties);
                            newsProcessorThreadList.add(newsProcessorThread);
                            newsProcessorThread.start();

                            String directoryPath = newsFilePath + state;
                            String fileName = dateToPull + ".json";
                            writeNewsDataToFile(newsDataJson.toString(), directoryPath, fileName);
                        } else if (newsApiResponse.getStatusLine().getStatusCode() == 429) {
                            System.out.println("Error retrieving data for : " + stateParam + "The number of requests for the developer account has been exceeeded. Do not retry");
                            tooManyRequests = true;
                        }
                    }
                }
            }
        } catch (Exception ie) {
            System.out.println("There was a problem processing the data: " + ie.getMessage());
        }

        while (threadsAlive(newsProcessorThreadList)) {} //wait until all the threads are finished, then return the results

        return newsTrendsMap;
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

    /*public String generateResultsReport(String resultData) throws IOException {
        Date today = new Date();
        String directoryPath = properties.getProperty("path.to.reports");
        String fileLocation = directoryPath + "dataTrendsResults" + "_" + today.getTime() + ".html";
        try {

            File directory = new File(directoryPath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            File report = new File(fileLocation);
            FileOutputStream os = new FileOutputStream(report);
            OutputStreamWriter osw = new OutputStreamWriter(os);
            Writer writer = new BufferedWriter(osw);
            writer.write(generatePageReportHtml(resultData));
            writer.close();
        } catch (IOException e) {
            System.out.println("There was a problem writing the report to " + fileLocation + ", exception: " + e.getMessage());
        }

        return fileLocation;
    }

    public String generatePageReportHtml(String result) {
        try {
            ContainerTag html = html(
                head(
                    title("News Trends Stats"),
                    script().withSrc("https://code.jquery.com/jquery-3.2.1.slim.min.js"),
                    script().withSrc("https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.12.9/umd/popper.min.js"),
                    script().withSrc("https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"),
                    script().withSrc("https://code.highcharts.com/highcharts.js"),
                    script().withSrc("https://code.highcharts.com/modules/wordcloud.js"),
                    script().withType("text/javascript").withSrc("news.js"),
                    link().withRel("stylesheet").withHref("https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css"),
                    link().withRel("stylesheet").withHref("styles.css")
                ),
                body(
                    div(
                        attrs(".jumbotron"),
                        h1("News Trends Stats").attr("class=\"center\"")
                    ),
                    div(
                        attrs(".container"),
                        div(attrs("#result-alert-error.alert.alert-danger.hidden"))
                                .withText("There was a problem retrieving the data. You may have exceeded your developer account limit for the news API. Please limit the number of days and try again."),
                        div(attrs("#wordMapData.hidden")).withText("#dataToReplace"),
                        div(attrs("#wordMapContainer")),
                        div(
                                attrs("#wordLists.row"),
                                div(attrs("#top20.col-md-6")),
                                div(attrs("#bottom20.col-md-6"))
                        )
                    )
                )
            );

            return html.render().replace("#dataToReplace", result);
        } catch (Exception e) {
            System.out.println("There was an exception while generating the html report: " + e.getMessage());
            return html(body(h3("There was an exception while generating the html report: " + e.getMessage()))).render();
        }
    }*/

}

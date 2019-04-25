import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Abby Parker
 * CSIS 612
 *
 *  This class is designed for making requests to retrieve data from the NewsAPI and then store the results in a file.
 *  The class utilizes the apache http client for making the requests out to the NewsAPI
 */
public class NewsAPIRetrieverThread extends Thread {
    private static String PAGE_SIZE = "100"; //The max allowed for the NewsAPI
    String q;
    String from;
    String to;
    String sortBy;
    String language;
    String apiKey;
    String directoryToSave;
    String fileName;
    String apiPath;

    NewsAPIRetrieverThread(String q, String fromDate, String toDate, String sortby, String language, String apiKey, String directoryToSave, String fileName, String apiPath) {
       this.q = q;
       this.apiKey = apiKey;
       this.directoryToSave = directoryToSave;
       this.fileName = fileName;
       this.to = toDate;
       this.language = language;
       this.sortBy = sortby;
       this.apiPath = apiPath;
       this.from = fromDate;
    }

    public void run() {
        getDataFromApiAndSaveToFile();
    }

    /**
     * Calls the API and stores the data to a file.
     * If the API returns a 429, then we have exceeded the maximum number of requests for the account for the day
     */
    private void getDataFromApiAndSaveToFile() {
        try {
            HttpClientBuilder client = HttpClientBuilder.create();
            HttpGet getNewsApiDataRequest;

            URIBuilder builder = new URIBuilder(apiPath);
            builder.setParameter("q", q.toLowerCase().replace("_", " "))
                    .setParameter("from", from)
                    .setParameter("to", to)
                    .setParameter("sortBy", sortBy)
                    .setParameter("pageSize", PAGE_SIZE)
                    .setParameter("language", language)
                    .setParameter("apiKey", apiKey);

            getNewsApiDataRequest = new HttpGet(builder.build());
            HttpResponse newsApiResponse = client.build().execute(getNewsApiDataRequest);

            if (newsApiResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                JsonNode newsDataJson = new ObjectMapper().readTree(newsApiResponse.getEntity().getContent());
                writeNewsDataToFile(newsDataJson.toString(), directoryToSave, fileName);
            } else if (newsApiResponse.getStatusLine().getStatusCode() == 429) {
                System.out.println("Error retrieving data for : " + q + "The number of requests for the developer account has been exceeeded. Do not retry");
            }
        } catch (Exception e) {
            System.out.println("There was a problem retrieving the data from the NewsAPI. Exception: " + e.getMessage());
        }
    }

    /**
     * Writes the json data to a file.
     *
     * @param dataToWrite - the json to  write to the file
     * @param directoryPath - directory to write to
     * @param fileName - name of the file to save
     * @throws IOException
     */
    private void writeNewsDataToFile(String dataToWrite, String directoryPath, String fileName) throws IOException {
        File directory = new File(directoryPath);
        if (! directory.exists()){
            directory.mkdirs();
        }

        File file = new File (directoryPath + "/" + fileName);

        //Uses Java 8's try-with-resources feature which will automatically close the file
        //if there is an exception
        try (BufferedWriter br = Files.newBufferedWriter(Paths.get(file.getPath()))) {
            br.write(dataToWrite);
        }
    }
}

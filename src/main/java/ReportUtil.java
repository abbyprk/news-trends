import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import j2html.tags.ContainerTag;
import java.io.*;
import java.util.*;
import static j2html.TagCreator.*;
import static j2html.TagCreator.body;
import static j2html.TagCreator.h3;

/**
 * Abby Parker
 * CSIS 612
 *
 * This class is used for generating the html reports with the news trends results
 *
 * This class uses a library called h2html for generating the html
 * See: https://j2html.com/
 */

public class ReportUtil {
    Properties properties;

    ReportUtil() {
        properties = new Properties();

        try {
            InputStream inputStream = this.getClass().getResourceAsStream("Application.properties");;
            properties.load(inputStream);
        } catch (IOException e) {
            System.out.println("Could not load properties to store data. Exception: " + e.getMessage());
        }
    }

    /**
     * Generates the results report from the data passed in.
     * Writes the report to the path specified in the "path.to.reports" app property
     *
     * @param data
     * @return pathToGeneratedFile
     * @throws IOException
     */
    public String generateResultsReport(HashMap<String, Integer> data) throws IOException {
        Date today = new Date();

        String resultData = generateJsonDataForReport(data);
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

    /**
     * Uses the j2html library to create the html string
     *
     * Imports jquery, bootstrap (for styling), highcharts and this application's custom styles and javascript
     * @param result
     * @return html string
     */
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
    }

    /**
     * Custom Json is needed to display the data in the reports.
     * This method walks the map and generates json objects and then sorts the array.
     *
     * @param data
     * @return sorted json array
     */
    public static String generateJsonDataForReport(HashMap<String, Integer> data) {
        String jsonData = "";
        try {
            //First we need create the json and sort the values
            ObjectMapper mapper = new ObjectMapper();
            ArrayList<JsonNode> jsonList = new ArrayList<>();
            for (HashMap.Entry<String, Integer> entry : data.entrySet()) {
                String wordData = "{\"name\":\"" + entry.getKey() + "\",\"weight\":\"" + entry.getValue() + "\"}";
                jsonList.add(mapper.readTree(wordData));
            }

            jsonData = mapper.writeValueAsString(sortData(jsonList));
        } catch (IOException e) {
            System.out.println("Could not write the map to json. Show error message : " + e.getMessage());
        }

        return jsonData;
    }

    /**
     * Sorts the data from largest to smallest
     * @param wordList
     * @return
     */
    public static ArrayList<JsonNode> sortData(ArrayList<JsonNode> wordList) {
        Collections.sort( wordList, new Comparator<JsonNode>() {
            @Override
            public int compare(JsonNode a, JsonNode b) {
                int valA = Integer.parseInt(a.get("weight").asText());
                int valB = Integer.parseInt(b.get("weight").asText());
                return valB - valA; //Sort in order of largest to smallest
            }
        });

        return wordList;
    }
}

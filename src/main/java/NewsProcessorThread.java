import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.HashMap;
import java.util.Properties;

/**
 * Abby Parker
 * CSIS 612
 *
 * Processes json news data that it is given and populates a shared sorted hashmap with the results
 */
public class NewsProcessorThread extends Thread {
    public static final String COMMON_PUNCTUATION_MATCHER = "[\\.\\?\\*\\(\\)\\[\\]\\!,]+";
    public static final String WORD_MATCHER = "^([a-zA-Z]+('(s|ll))?)$";

    private HashMap<String, Integer> localWordTrendsMap;
    public HashMap<String, Integer> sharedSortedWordTrendsMap;
    public static String commonWordsMatcher;
    private JsonNode newsDataJson;
    private Properties properties;

    NewsProcessorThread(HashMap<String, Integer> sharedWordTrendsMap, JsonNode news, Properties properties) {
        this.sharedSortedWordTrendsMap = sharedWordTrendsMap;
        this.localWordTrendsMap = new HashMap<>();
        this.newsDataJson = news;
        this.properties = properties;
        commonWordsMatcher = properties.get("common.words").toString(); //get the list of common words to filter out from the properties
    }

    /**
     * Processes each of the news objects and stores the weighted word results in the localWordTrendsMap
     * At the end, update the sharedSortedWordTrendsMap
     */
    public void run() {
        processData();
        updateSharedSortedWordTrends();
    }

    /**
     * processData - processes each article adds the individual words and word count to the localWordTrendsMap
     *
     * The method uses regular expressions for parsing the data and filtering out punctuation, numbers & special characters,
     * and common words that do not provide a lot of value for this exercise.
     *
     * Note: The common words can be edited in the Application.properties file to be more or less restrictive
     */
    private void processData() {
        try {
            ArrayNode articles = (ArrayNode) newsDataJson.get("articles");
            for (JsonNode article : articles) {
                String content = article.get("content").asText();

                //Count meaningful words
                String[] words = content.split("\\s+");
                for (String word : words) {
                    word.replaceAll(COMMON_PUNCTUATION_MATCHER, " ");

                    //ignore common filler words
                    word = word.toLowerCase().trim();
                    if (!word.matches(commonWordsMatcher) && word.matches(WORD_MATCHER)) {
                        int count = localWordTrendsMap.get(word) == null ? 1 : localWordTrendsMap.get(word) + 1;
                        localWordTrendsMap.put(word, count);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Synchronized method that updates the shared Map with the data processed by this thread
     */
    private synchronized void updateSharedSortedWordTrends() {
        for (String word : localWordTrendsMap.keySet()) {
            Integer currentValue = sharedSortedWordTrendsMap.get(word);
            sharedSortedWordTrendsMap.put(word, currentValue == null ? localWordTrendsMap.get(word) : currentValue + localWordTrendsMap.get(word));
        }
    }
}

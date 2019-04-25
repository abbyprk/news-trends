import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.HashMap;
import java.util.SortedMap;

/**
 * Abby Parker
 * CSIS 612
 *
 * Processes json news data and populates a shared sorted hashmap with the results
 */
public class NewsProcessorThread extends Thread {
    //TODO: FIX THIS LATER TO BE BETTER
    public static String commonWordsMatcher; //This should really be shared
    public static final String NON_WORD_MATCHER = "[^a-zA-Z//s]+";
    public static final String WORD_MATCHER = "^([a-zA-Z]+('(s|ll))?)$";
    private HashMap<String, Integer> localWordTrendsMap;
    public HashMap<String, Integer> sharedSortedWordTrendsMap;
    private JsonNode newsDataJson;
    public int numThreads;

    NewsProcessorThread(HashMap sharedWordTrendsMap, JsonNode news) {
        this.sharedSortedWordTrendsMap = sharedWordTrendsMap;
        this.localWordTrendsMap = new HashMap();
        this.newsDataJson = news;

        StringBuilder commonWords = new StringBuilder();
        commonWords.append("i|a|to|for|with|or|and|that|of|if|the|not|as|this|they|she|he|an|my|all|there|it|would|their|will|her|his|theirs|had|out|only|can|many|were|also|so")
                .append("|from|in|at|about|into|after|up|by|with|on|over|is|are|was|be|has|you|but|more|who|top|when|how|its|we|than|most|been|some|which|what|some|back|off|can")
                .append("|january|february|march|april|may|june|july|august|september|october|november|december|before|made|being|today|tomorrow|yesterday|around|next|see|early|much|night|day|tonight")
                .append("|have|do|say|said|get|make|go|until|could|any|still|them|each|because|likely|since|already|seem|seemed|over|under|without|every|well|way|want|other|both|say|says|said|us")
                .append("|one|two|three|four|five|six|seven|eight|nine|ten|first|second|third|fourth|fifth|new|old|just|last|monday|tuesday|wednesday|thursday|friday|saturday|during|while|your|time|our|like|even|no|yes");
        commonWordsMatcher = commonWords.toString();
    }

    /**
     * Processes each news document and stores the weighted word results in the localWordTrendsMap
     * At the end, update the sharedSortedWordTrendsMap
     */
    public void run() {
        processData();
        updateSharedSortedWordTrends();
    }

    private void processData() {
        try {
            ArrayNode articles = (ArrayNode) newsDataJson.get("articles");
            for (JsonNode article : articles) {
                String content = article.get("content").asText();
                //content = content.replace(NON_WORD_MATCHER, ""); //remove non-words

                //Count meaningful words
                String[] words = content.split("\\s+");
                for (String word : words) {
                    word.replace("[\\.\\?\\*\\(\\)\\[\\]\\!,]+)", " ");
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
     * TODO: Will this be too slow?
     */
    private synchronized void updateSharedSortedWordTrends() {
        for (String word : localWordTrendsMap.keySet()) {
            Integer currentValue = sharedSortedWordTrendsMap.get(word);
            sharedSortedWordTrendsMap.put(word, currentValue == null ? localWordTrendsMap.get(word) : currentValue + localWordTrendsMap.get(word));
        }
    }
}

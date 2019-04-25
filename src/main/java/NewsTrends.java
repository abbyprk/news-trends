import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;

/**
 * Abby Parker
 * CSIS 612
 *
 * This application pulls data from the NewsAPI or from storage for the US states and territories by date and calculates trends in words over time.
 */
public class NewsTrends {
    private static final int MAX_DAYS_TO_PROCESS = 5;
    private static final int MIN_DAYS_TO_PROCESS = 1;

    public static void main(String[] args) {
        int totalDays = getDaysToProcess();
        StateNewsDataHandler handler = new StateNewsDataHandler();
        HashMap<String, Integer> data = handler.processNewsTrendsByState(new Date(), totalDays);

        System.out.println("Total unique words: " + data.size());
        ReportUtil reports = new ReportUtil();

        try {
            String filepath = reports.generateResultsReport(data);
            System.out.println("\n\nOpen " + filepath + " to view the results");
        } catch (IOException e) {
            System.out.println("There was a problem generating the report results");
        }
    }

    private static int getDaysToProcess() {
        Scanner scanner = new Scanner(System.in);
        boolean valid = false;
        int totalDays = 5;
        do {
            try {
                System.out.println("Please enter the number of days to process (1 to 5): ");
                totalDays = scanner.nextInt();

                if (totalDays > MAX_DAYS_TO_PROCESS) {
                    System.out.println("The maximum allowed data will be processed for each state.");
                    totalDays = MAX_DAYS_TO_PROCESS;
                } else if (totalDays < MIN_DAYS_TO_PROCESS) {
                    System.out.println("The minumum number of days to process is 1.");
                    totalDays = MIN_DAYS_TO_PROCESS;
                }

                valid = true;
            } catch (Exception e) {
                System.out.println("You entered invalid input. " + e);
                scanner.nextLine();
            }
        } while (!valid);

        return totalDays;
    }
}

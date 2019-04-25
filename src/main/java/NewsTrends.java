import java.io.IOException;
import java.util.*;

/**
 * Abby Parker
 * CSIS 612
 *
 * The NewsTrends class is the orchestrator of the application.
 * The application asks the user for the number of days they want to search and then sends a request
 * to the StateNewsDataHandler to process the data
 * After the results have been processed, it uses the ReportUtil to generate an html report with the data trend information
 *
 * This application uses thread-level parallelism and data-level parallelism for retrieving and processing
 * the news data.
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

    /**
     * Gets the user input
     * @return number of days to process
     */
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

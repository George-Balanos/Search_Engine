//Evangelos Chasanis 5058
//Georgios Mpalanos 5054

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;

public class QueryHistoryManager {
    private static final String FILE_PATH = "query_history.txt";
    private static final int MAX_SIZE = 10;
    private LinkedList<String> queries;

    public QueryHistoryManager() {
        queries = new LinkedList<>();
        loadHistory();
    }

    private void loadHistory() {
        try {
            Files.lines(Paths.get(FILE_PATH)).forEach(queries::add);
        } catch (IOException e) {
            System.out.println("Unable to load query history: " + e.getMessage());
        }
    }

    public void addQuery(String query) {
        if (query.isEmpty())
        {
            return;
        }
        queries.addFirst(query);
        while (queries.size() > MAX_SIZE) {
            queries.removeLast();
        }
        saveHistory();
    }

    private void saveHistory() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (String query : queries) {
                writer.write(query);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Unable to save query history: " + e.getMessage());
        }
    }

    public void printHistory() {
        System.out.println("Last 10 queries searched:");
        for (String query : queries) {
            System.out.println(query);
        }
    }
}
//Evangelos Chasanis 5058
//Georgios Mpalanos 5054

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.lucene.document.Document;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class Main extends Application {

    private static final String FILE_PATH = "C:\\Users\\User\\Desktop\\ΕΞΑΜΗΝΟ 8\\Ανάκτηση Πληροφορίας\\Project\\search_engine\\Search_engine\\query_history.txt";
    private static final int DOCS_PER_PAGE = 10;

    private QueryHistoryManager queryHistoryManager;
    private QueryManager queryManager;
    private int modeOfSearch;
    private String selectedField = "abstract";
    private String selectedPhraseField = "abstract";
    private Label recentlySearchedLabel;
    private TextArea textArea;

    private List<Document> docs;
    private int currentPage = 0;

    boolean ascendingOrder = true;

    @Override
    public void start(Stage primaryStage) throws Exception {
        queryHistoryManager = new QueryHistoryManager();
        queryManager = new QueryManager();

        VBox buttonContainer = new VBox();
        buttonContainer.setSpacing(10);

        reloadQueries(buttonContainer);

        ScrollPane scrollPane = new ScrollPane(buttonContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(80);

        TextField searchBar = new TextField();
        searchBar.setPromptText("Search...");
        searchBar.setMaxWidth(Double.MAX_VALUE);
        searchBar.setOnAction(event -> {
            String searchText = searchBar.getText();
            System.out.println("Search Text: " + searchText);
            searchBar.clear();
            executeSearch(searchText);
            reloadQueries(buttonContainer);
        });

        Button keyWordButton = new Button("Key Word");
        keyWordButton.setMaxWidth(Double.MAX_VALUE);
        keyWordButton.getStyleClass().add("search-button");
        keyWordButton.setOnAction(event -> {
            modeOfSearch = 0;
            updateRecentlySearchedLabel("Keyword query");
        });

        ComboBox<String> phraseComboBox = new ComboBox<>(FXCollections.observableArrayList(
                "Title", "Author", "Abstract", "Full Text", "Year"
        ));
        phraseComboBox.setPromptText("Select Field");
        phraseComboBox.setPrefWidth(150);
        phraseComboBox.setEditable(false);
        phraseComboBox.setMaxWidth(Double.MAX_VALUE);
        phraseComboBox.setOnAction(event -> {
            modeOfSearch = 2;
            selectedPhraseField = phraseComboBox.getValue();
            updateRecentlySearchedLabel("Phrase query   Selected field: " + selectedPhraseField);
        });

        ComboBox<String> fieldComboBox = new ComboBox<>(FXCollections.observableArrayList(
                "Title", "Author", "Abstract", "Full Text", "Year"
        ));
        fieldComboBox.setPromptText("Select Field");
        fieldComboBox.setPrefWidth(150);
        fieldComboBox.setEditable(false);
        fieldComboBox.setMaxWidth(Double.MAX_VALUE);
        fieldComboBox.setOnAction(event -> {
            modeOfSearch = 1;
            selectedField = fieldComboBox.getValue();
            updateRecentlySearchedLabel("Field Query   Selected field: " + selectedField);
        });

        HBox queryTypeButtons = new HBox(keyWordButton, fieldComboBox, phraseComboBox);
        queryTypeButtons.setSpacing(10);
        queryTypeButtons.setFillHeight(true);
        queryTypeButtons.setMaxWidth(1180);

        HBox.setHgrow(keyWordButton, Priority.ALWAYS);
        HBox.setHgrow(fieldComboBox, Priority.ALWAYS);
        HBox.setHgrow(phraseComboBox, Priority.ALWAYS);

        recentlySearchedLabel = new Label("\nRecently Searched:");

        textArea = new TextArea("Results: ");
        textArea.setMinWidth(800);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        Button prevButton = new Button("Previous");
        prevButton.setOnAction(event -> {
            if (docs == null || docs.isEmpty()) {
                return;
            }
            if (currentPage > 0) {
                currentPage--;
                displayCurrentPage();
            }
        });
        prevButton.getStyleClass().add("navigation-button");

        Button nextButton = new Button(" Next ");
        nextButton.setOnAction(event -> {
            if (docs == null || docs.isEmpty()) {
                return;
            }
            if (currentPage < (docs.size() - 1) / DOCS_PER_PAGE) {
                currentPage++;
                displayCurrentPage();
            }
        });
        nextButton.getStyleClass().add("navigation-button");

        Button clearButton = new Button("Clear");
        clearButton.setOnAction(event -> {
            docs = new ArrayList<>();
            textArea.clear();
            textArea.appendText("\n\n\n\n\n\n\n\n\n\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t  Cleared Search");
        });

        Button sortButton = new Button("Sort by Year (Descending)");
        sortButton.setOnAction(event -> {
            ascendingOrder = !ascendingOrder; // Toggle the order on each click
            orderByYear(); // Call the sorting method
            displayCurrentPage(); // Display the current page after sorting
            updateSortButtonText(sortButton); // Update the button text
        });

        HBox navigationButtons = new HBox(prevButton, nextButton, sortButton, clearButton);


        VBox searchContainer = new VBox();
        searchContainer.getChildren().addAll(searchBar, queryTypeButtons, recentlySearchedLabel, textArea, navigationButtons);
        searchContainer.setSpacing(10);

        BorderPane root = new BorderPane();
        root.setTop(searchContainer);
        root.setLeft(scrollPane);
        root.setCenter(textArea);
        BorderPane.setMargin(searchContainer, new Insets(10));

        root.setRight(navigationButtons);
        navigationButtons.setSpacing(10);
        navigationButtons.setTranslateY(-30);
        navigationButtons.setTranslateX(-500);

        Scene scene = new Scene(root, 1200, 600);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Text File Content Display");
        primaryStage.show();
        primaryStage.setResizable(false);
    }

    private String getFieldFromDropdown() {
        if (modeOfSearch == 1) {
            return switch (selectedField) {
                case "Title" -> "title";
                case "Author" -> "author";
                case "Abstract" -> "abstract";
                case "Full Text" -> "full_text";
                default -> "year";
            };
        } else {
            return switch (selectedPhraseField) {
                case "Title" -> "title";
                case "Author" -> "author";
                case "Abstract" -> "abstract";
                case "Full Text" -> "full_text";
                default -> "year";
            };
        }
    }

    public void executeSearch(String searchText) {
        if (searchText.startsWith("\"") && searchText.endsWith("\"")) {
            searchText = searchText.substring(1, searchText.length() - 1);
        }

        switch (modeOfSearch) {
            case 0:
                docs = queryManager.makeKeyWordQuery(searchText);
                queryHistoryManager.addQuery(searchText);
                break;
            case 1:
                docs = queryManager.makeFieldQuery(getFieldFromDropdown(), searchText);
                queryHistoryManager.addQuery(searchText);
                break;
            default:
                docs = queryManager.makePhraseQuery(getFieldFromDropdown(), "\"" + searchText + "\"");
                queryHistoryManager.addQuery(searchText);
        }

        currentPage = 0;
        displayCurrentPage();
    }

    public void executeSearchButton(String searchText) {
        if (searchText.startsWith("\"") && searchText.endsWith("\"")) {
            searchText = searchText.substring(1, searchText.length() - 1);
        }

        switch (modeOfSearch) {
            case 0:
                docs = queryManager.makeKeyWordQuery(searchText);
                break;
            case 1:
                docs = queryManager.makeFieldQuery(getFieldFromDropdown(), searchText);
                break;
            default:
                docs = queryManager.makePhraseQuery(getFieldFromDropdown(), "\"" + searchText + "\"");

        }

        currentPage = 0;
        displayCurrentPage();
    }

    private void displayCurrentPage() {
        textArea.clear();
        if (docs != null && docs.size() == 0) {
            textArea.appendText("\n\n\n\n\n\n\n\n\n\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t  No Result");
        }

        int start = currentPage * DOCS_PER_PAGE;
        int end = Math.min(start + DOCS_PER_PAGE, docs.size());

        for (int i = start; i < end; i++) {
            Document doc = docs.get(i);
            String title = doc.get("title");
            String author = doc.get("author");
            String year = doc.get("year");
            String abstract_ = doc.get("abstract");
            if (author == null) {
                textArea.appendText("Title: " + title + "\nAuthor(s): N/A, Year: " + year + "\n" + "Abstract: " + abstract_ + "\n\n");
                textArea.appendText("Related part: " + queryManager.getHighlights().get(i) + "\n\n\n\n");
            }

            else{
                textArea.appendText("Title: " + title + "\nAuthor(s): " + author + ", Year: " + year + "\n" + "Abstract: " + abstract_ + "\n\n");
                textArea.appendText("Related part: " + queryManager.getHighlights().get(i) + "\n\n\n\n");
            }


        }
        textArea.positionCaret(0);
        textArea.setScrollTop(0);
    }

    private void updateRecentlySearchedLabel(String searchText) {
        recentlySearchedLabel.setText(searchText + "\nRecently Searched: ");
    }

    private void reloadQueries(VBox buttonContainer) {
        buttonContainer.getChildren().clear();

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Button button = new Button(line);
                button.setWrapText(true);
                button.setMinWidth(200);
                button.setOnAction(event -> {
                    Button clickedButton = (Button) event.getSource();
                    System.out.println(clickedButton.getText());
                    executeSearchButton(clickedButton.getText());
                });
                buttonContainer.getChildren().add(button);
            }
        } catch (IOException e) {
            showErrorDialog("Error Reading File", "An error occurred while reading the file: " + e.getMessage());
        }
    }

    private void showErrorDialog(String title, String message) {
        System.err.println(title + ": " + message);
    }

    public void orderByYear() {
        Collections.sort(docs, new Comparator<Document>() {
            @Override
            public int compare(Document d1, Document d2) {
                int year1 = Integer.parseInt(d1.get("year"));
                int year2 = Integer.parseInt(d2.get("year"));

                // If ascending order is true, sort in ascending order
                if (ascendingOrder) {
                    return Integer.compare(year1, year2);
                } else {
                    // Otherwise, sort in descending order
                    return Integer.compare(year2, year1);
                }
            }
        });
    }

    private void updateSortButtonText(Button sortButton) {
        sortButton.setText(ascendingOrder ? "Sort by Year (Descending)" : "Sort by Year (Ascending)");
    }

    public static void main(String[] args) {
        if (!Files.exists(Path.of("./tmp/testIndex"))) {
            System.out.println("Hello");
            CSVIndexer myCSVIndexer = new CSVIndexer();
            myCSVIndexer.setupIndex("C:\\Users\\User\\Desktop\\ΕΞΑΜΗΝΟ 8\\Ανάκτηση Πληροφορίας\\Project\\merged_data.csv");
        }

        launch();
    }
}
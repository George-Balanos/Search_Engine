//Evangelos Chasanis 5058
//Georgios Mpalanos 5054

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.search.spell.LuceneLevenshteinDistance;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryManager {
    private Analyzer analyzer;
    private Directory dir;
    private IndexSearcher isearcher;
    private QueryParser parser;
    private SpellChecker spellChecker;
    private StringBuilder modifiedContent;
    private boolean hasSuggestions;
    private Map<String, Double> wordScores; // Store suggested words and their scores
    private ArrayList<String> highlights;

    public QueryManager() {
        try {
            analyzer = new StandardAnalyzer();
            dir = FSDirectory.open(Path.of("./tmp/testIndex"));
            DirectoryReader ireader = DirectoryReader.open(dir);
            isearcher = new IndexSearcher(ireader);
            spellChecker = new SpellChecker(FSDirectory.open(Path.of("./spellCheckerIndex")));
            spellChecker.setStringDistance(new LuceneLevenshteinDistance());
            wordScores = new HashMap<>();
            highlights = new ArrayList<>();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Document> makeFieldQuery(String fieldName, String fieldContent) {
        List<Document> hitDocList = new ArrayList<>();
        parser = new QueryParser(fieldName, analyzer);
        try {

            if (fieldContent == null || fieldContent.trim().isEmpty()) {
                System.out.println("Search query is empty or contains only whitespace characters.");
                return null;
            }

            String[] words = fieldContent.split("\\s+");

            makeQueryContent(words);

            if (hasSuggestions) {
                System.out.println("Modified query after spelling suggestions: " + modifiedContent.toString());
            }

            Query query = parser.parse(modifiedContent.toString().trim());

            hitDocList = executeQuery(query, fieldName);

        } catch (ParseException | InvalidTokenOffsetsException | IOException e) {
            throw new RuntimeException(e);
        }
        return hitDocList;
    }

    public void makeQueryContent(String[] words) throws IOException {
        modifiedContent = new StringBuilder();
        hasSuggestions = false;
        wordScores.clear();


        for (String word : words) {

            if (!spellChecker.exist(word)) {
                String[] suggestions = spellChecker.suggestSimilar(word, 4);
                if (suggestions.length > 0) {
                    System.out.println("Suggestions for misspelled word '" + word + "':");
                    for (String suggestion : suggestions) {
                        float distance = spellChecker.getStringDistance().getDistance(word, suggestion);
                        double score = 1.0 / (distance + 1);
                        System.out.println(suggestion + " (Score: " + score + ")");

                        wordScores.put(suggestion, score);
                    }

                    word = wordScores.entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse(word);
                    hasSuggestions = true;
                } else {
                    System.out.println("No suggestions found for misspelled word '" + word + "'.");
                }
            }

            modifiedContent.append(word).append(" ");
        }
    }

    public List<Document> makePhraseQuery(String fieldName, String phrase) {
        List<Document> hitDocList = new ArrayList<>();
        try {
            if (phrase == null || phrase.trim().isEmpty()) {
                System.out.println("Phrase query is empty or contains only whitespace characters.");
                return null;
            }

            QueryParser parser = new QueryParser(fieldName, analyzer);

            Query query = parser.parse("\"" + phrase + "\"");

            hitDocList = executeQuery(query, fieldName);

        } catch (ParseException | IOException | InvalidTokenOffsetsException e) {
            throw new RuntimeException(e);
        }
        return hitDocList;
    }

    public List<Document> makeKeyWordQuery(String keyWords) {
        String[] fields = {"author", "year", "abstract", "full_text"};
        List<Document> hitDocList = new ArrayList<>();
        try {
            if (keyWords == null || keyWords.trim().isEmpty()) {
                System.out.println("Phrase query is empty or contains only whitespace characters.");
                return null;
            }
            Analyzer analyzer = new StandardAnalyzer();
            String[] words = keyWords.split("[,\\s]+");

            for (String word : words) {
                QueryParser parser = new QueryParser("title", analyzer);
                Query query = parser.parse(word); // Enclose phrase in double quotes to make it a phrase query
                executeQuery(query, "title");
                for (String field : fields) {
                    try {
                        parser = new QueryParser(field, analyzer);
                        query = parser.parse(word);
                        hitDocList = executeQuery(query, field);
                    } catch (ParseException e) {

                        System.err.println("Failed to parse query for field: " + field);
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException | InvalidTokenOffsetsException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return hitDocList;
    }
    private List<Document> executeQuery(Query query, String fieldName) throws IOException, InvalidTokenOffsetsException {
        List<Document> hitDocList = new ArrayList<>();

        QueryScorer scorer = new QueryScorer(query);
        SimpleHTMLFormatter formatter = new SimpleHTMLFormatter();
        Highlighter highlighter = new Highlighter(formatter, scorer);
        Fragmenter fragmenter = new SimpleFragmenter(100);
        highlighter.setTextFragmenter(fragmenter);

        TopDocs topDocs = isearcher.search(query, 1000);
        ScoreDoc[] hits = topDocs.scoreDocs;

        highlights = new ArrayList<>();

        for (int i = 0; i < hits.length; i++) {
            Document hitDoc = isearcher.doc(hits[i].doc);
            System.out.print("Title: ");
            System.out.println(hitDoc.get("title"));

            String content = hitDoc.get(fieldName);

            TokenStream stream = TokenSources.getTokenStream(fieldName, content, analyzer);
            String highlightedText = highlighter.getBestFragment(stream, content);

            String resultText = highlightedText != null ? highlightedText : content;
            highlights.add(resultText);

            System.out.println(resultText);
            System.out.println("\n");

            hitDocList.add(hitDoc);
        }
        return hitDocList;
    }

    public ArrayList<String> getHighlights(){
        return highlights;
    }
}
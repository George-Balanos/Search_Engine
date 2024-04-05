import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;

public class QueryManager {

    public static void makeQuery(String fieldName, String fieldContent){
        ScoreDoc[] hits = null;

        try {
            Analyzer analyzer = new StandardAnalyzer();

            Directory dir = FSDirectory.open(Path.of("./tmp/testIndex"));

            DirectoryReader ireader = DirectoryReader.open(dir);
            IndexSearcher isearcher = new IndexSearcher(ireader);

            QueryParser parser = new QueryParser(fieldName, analyzer);

            Query query = null;
            query = parser.parse(fieldContent);

            TopDocs topDocs = isearcher.search(query, 1000);
            hits = topDocs.scoreDocs;
            System.out.println(hits.length);

            for (int i = 0; i < hits.length; i++) {
                Document hitDoc = isearcher.doc(hits[i].doc);
                System.out.println(hitDoc.get("abstract"));
                System.out.println("\n");
            }

        } catch (ParseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

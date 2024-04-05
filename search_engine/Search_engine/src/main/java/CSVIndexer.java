import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CSVIndexer {

    public void setupIndex(String fileName){

        try {
            Analyzer analyzer = new StandardAnalyzer();

            Directory dir = FSDirectory.open(Path.of("./tmp/testIndex"));

            IndexWriterConfig config = new IndexWriterConfig(analyzer);

            IndexWriter indexWriter = new IndexWriter(dir,config);

            ArrayList<Document> docs = getDocuments(fileName);

            for(Document doc: docs){
                indexWriter.addDocument(doc);
            }

            indexWriter.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static ArrayList<Document> getDocuments(String fileName){
        ArrayList<String[]> fields = readCsv(fileName);
        ArrayList<Document> docs = new ArrayList<>();

        for(String[] row: fields){

            Document doc = new Document();

            Field yearField = new TextField("year",row[2],Field.Store.YES);
            Field titleField = new TextField("title",row[3],Field.Store.YES);
            Field abstractField = new TextField("abstract",row[4],Field.Store.YES);
            Field full_text = new TextField("full_text",row[5],Field.Store.YES);

            doc.add(titleField);
            doc.add(yearField);
            doc.add(abstractField);
            doc.add(full_text);

            docs.add(doc);
        }

        return docs;
    }

    public static ArrayList<String[]> readCsv(String fileName) {
        ArrayList<String[]> myList = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(fileName))) {
            List<String[]> rows = reader.readAll();

            // Get column names from the first row
            String[] columnNames = rows.get(0);

            // Iterate over rows starting from the second row
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);

                String[] tempArray = new String[6];

                for (int j = 0; j < row.length; j++) {
                    //System.out.println("[" + columnNames[j] + ": " + row[j] + "]");
                    String tempString = row[j];
                    tempArray[j] = tempString;
                }

                myList.add(tempArray);
            }

            /*for(String[] elem: myList){
                for(String elem1: elem){
                    System.out.print(elem1+"  ");
                }
            }*/

        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }

        return myList;
    }
}
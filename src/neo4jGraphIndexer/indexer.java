package neo4jGraphIndexer;


import MeSH_Vector.helperClass;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.RelationshipIndex;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.index.lucene.ValueContext;

/**
 * Created by super-machine on 12/21/16.
 */
public class indexer {
    static Index<Node> titleIdx;
    private static GraphDatabaseService graphDb;
    static Index<Node> meshIdx;
    static RelationshipIndex dateIdx;
    private static String DB_PATH = "";
    static IndexManager index;
    static SimpleDateFormat sdf = new SimpleDateFormat("EEEE MMMM dd HH:mm:ss Z yyyy", Locale.US);

    public static void main(String[] args) {

        String filePath = "/Users/super-machine/Documents/Research/medline/output/whole.txt";//args[0];
        DB_PATH = "/Users/super-machine/Documents/Research/medline/output/dummy.db/";//args[1];
        int commitBatchSize = 100000;// Integer.parseInt(args[2]);
     //   deleteFileOrDirectory(new File(DB_PATH));
        int batchNumber = 1;
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(DB_PATH));
        registerShutdownHook(graphDb);
        Transaction tx = graphDb.beginTx();
        try  {
            index = graphDb.index();
            helperClass helper = new helperClass();
            titleIdx = index.forNodes("article");
            meshIdx = index.forNodes("meshName");
            dateIdx = index.forRelationships("dates");
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String line = "";
            int lineCounter = 0;
            while((line=br.readLine())!=null){
//                System.out.println(line);
                line=line.trim();
                if(!line.equals("")) {
                    lineCounter++;
                    int tabCounter = 0;
                    String article = "";
                    ArrayList<String> meshTerms = new ArrayList<>();
                    int year=0;

                    while (true) {
                        int indexOfTab = line.indexOf("\t");
                        if (indexOfTab == -1) {
                            if (tabCounter == 3) {
                                String subString = line.substring(0, line.length());
                                year = Integer.parseInt(subString);
                                break;
                            }
                            break;
                        }
                        if (indexOfTab != -1) {
                            if (tabCounter == 0) {
                                article = line.substring(0, indexOfTab);
                            }
                            if (tabCounter == 2) {
                                helper.extractMeshTermsFromStringArrayList(line.substring(0, indexOfTab), meshTerms);
                            }
                            line = line.substring(indexOfTab + 1, line.length());
                            tabCounter++;
                        }
                    }
                    String nodeName=article.toLowerCase().trim();
                    IndexHits<Node> nodes = titleIdx.get("article", nodeName);
                    if (nodes.hasNext()) {
                        System.out.println("found: "+nodeName);
                    }
                    else{
                        System.out.println("Inserting title nodes: "+nodeName);
                        Node titleNode = createOrGet(article, 1);
                        for (String mesh : meshTerms) {
                            mesh = mesh.trim();
                            if (!mesh.equals("")) {
                                Node meshNode = createOrGet(mesh, 2);
                                Relationship rel = meshNode.createRelationshipTo(titleNode, RelTypes.Occurs);
                                rel.setProperty("Date", year);
                                dateIdx.add(rel, "year-numeric", new ValueContext(year).indexNumeric());
                            }
                        }
                        if (lineCounter == commitBatchSize) {
                            System.out.println("committing " + lineCounter * batchNumber);
                            batchNumber++;
                            tx.success();
                            System.out.println("tx.success");
                            tx.close();
                            System.out.println("tx closed");
                            tx = graphDb.beginTx();
                            System.out.println("Database reconnected");
                            lineCounter = 0;
                        }
                    }
                }
            }
            System.out.println("handling last few records");
            tx.success();
            System.out.println("tx.success");
            tx.close();
            System.out.println("tx closed");
            graphDb.shutdown();
            System.out.println("graphDb shutdown");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private enum RelTypes implements RelationshipType
    {
        Occurs
    }
    private static Node createOrGet(String nodeName,   int type) {
        Node nd = null;
        nodeName=nodeName.toLowerCase().trim();
        try {
            if (type == 1) {
                IndexHits<Node> nodes = titleIdx.get("article", nodeName);
                if (nodes.hasNext()) {
                    nd = nodes.next();
                } else {
                    nd = graphDb.createNode(Label.label("article"));
                    nd.setProperty("article", nodeName);
                    titleIdx.add(nd, "article", nodeName);
                }
            }
            if (type == 2) {
                IndexHits<Node> nodes = meshIdx.get("meshName", nodeName);
                if (nodes.hasNext()) {
                    nd = nodes.next();
                } else {
                    nd = graphDb.createNode(Label.label("meshName"));
                    nd.setProperty("meshName", nodeName);
                    meshIdx.add(nd, "meshName", nodeName);
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return nd;
    }
    private static void registerShutdownHook(final GraphDatabaseService graphDb) {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
            }
        });
    }
    private static void deleteFileOrDirectory(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                for (File child : file.listFiles()) {
                    deleteFileOrDirectory(child);
                }
            }
            file.delete();
        }
    }
}

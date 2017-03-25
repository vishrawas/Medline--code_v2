package neo4jGraphIndexer;

import org.apache.lucene.search.NumericRangeQuery;
import org.neo4j.graphdb.*;


import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.graphdb.traversal.*;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by super-machine on 3/23/17.
 */
public class addOneLevelToExistingPaths {
    static final String pattern = "\\((.*?)\\)";
    static Pattern r = Pattern.compile(pattern);
    static Node startNode;
    private static String DB_PATH = "";
    private static GraphDatabaseService graphDb;
    static IndexManager index;
    static Index<Node> titleIdx;
    static Index<Node> meshIdx;
    static RelationshipIndex dateIdx;


    public static void main(String args[]) {
        DB_PATH = "/Users/super-machine/Documents/Research/medline/output/dummy.db";// args[0];
        String writePath = "/Users/super-machine/Documents/Research/medline/output/traversal/path_length_6/nextLevel_fo.txt";
        String ipPath = "/Users/super-machine/Documents/Research/medline/output/traversal/path_length_6/fo.txt";
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(DB_PATH));
        try (Transaction tx = graphDb.beginTx()) {

            index = graphDb.index();
            boolean indexExists = index.existsForNodes("meshName");
            System.out.println(index.nodeIndexNames().length);
            System.out.println(indexExists);
            titleIdx = index.forNodes("article");
            meshIdx = index.forNodes("meshName");
            dateIdx = index.forRelationships("dates");

            int year = 1985;
            System.out.println("Gathering all article nodes");
            Set<Node> articleNodes = getLastArticleNode(ipPath);
            System.out.println("Gathered all article Nodes");
            BufferedWriter bw = null;
            try {
                bw = new BufferedWriter(new FileWriter(writePath, true));
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (Node nd : articleNodes) {
                TraversalDescription description = graphDb.traversalDescription()
                        .breadthFirst()
                        .uniqueness(Uniqueness.NODE_PATH)
                        .expand(new addOneLevelToExistingPaths.SpecificRelsPathExpander(year))
                        .evaluator(Evaluators.toDepth(1));
                Traverser traverser = description.traverse(nd);
                ResourceIterator<Path> Paths = traverser.iterator();


                StringBuilder builder = new StringBuilder();
                while (Paths.hasNext()) {
                    Path p = Paths.next();
                    builder.append(p.toString()).append("\n");

                }
                bw.write(builder.toString());
            }
            try {
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            tx.success();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Set<Node> getLastArticleNode(String ipPath) {
        Set<Node> nodes = new HashSet<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(ipPath));
            String line = "";
            int counter = 0;
            while ((line = br.readLine()) != null) {
                if (line.contains("(")) {
                    counter++;
                    if (counter % 100000 == 0) {
                        System.out.println("counter-- " + counter);
                    }
                    String article = getLastEntry(line);
                    if (article != null) {
                        Node node = get(article, 1);
                        nodes.add(node);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nodes;
    }

    private static String getLastEntry(String s) {
        String lastEntry = null;
        Matcher m = r.matcher(s);
        int counter = 0;
        while (m.find()) {
            counter++;
            lastEntry = m.group(1);
        }
        if (counter < 4) {
            return null;
        }
        return lastEntry;
    }

    private static Node get(String nodeName, int type) {
        Node nd = null;
        nodeName = nodeName.toLowerCase().trim();
        try {
            if (type == 1) {
                IndexHits<Node> nodes = titleIdx.get("article", nodeName);
                nd = nodes.getSingle();
            }
            if (type == 2) {
                IndexHits<Node> nodes = meshIdx.get("meshName", nodeName);
                nd = nodes.getSingle();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return nd;
    }


    private static class SpecificRelsPathExpander implements PathExpander {

        private final int requiredProperty;

        public SpecificRelsPathExpander(int requiredProperty) {
            this.requiredProperty = requiredProperty;
        }

        // not used
        public PathExpander<Object> reverse() {
            return null;
        }

        @Override
        public Iterable expand(Path path, BranchState bs) {
            Node startNode = path.startNode();
            Node endNode = path.endNode();
            NumericRangeQuery<Integer> pageQueryRange = NumericRangeQuery.newIntRange("year-numeric", null, requiredProperty, true, true);
            IndexHits<Relationship> hits;
            if (endNode.hasLabel(Label.label("article"))) {
                hits = dateIdx.query(pageQueryRange, null, endNode);
            } else {
                hits = dateIdx.query(pageQueryRange, endNode, null);
            }

            if (hits == null) {
                return null;
            }
            if (!hits.iterator().hasNext()) {
                return null;
            }
            return hits;
        }

    }

}

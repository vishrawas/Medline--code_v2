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

/**
 * Created by super-machine on 1/31/17.
 */
public class bidirectionalTraversal {

    private static String DB_PATH = "";
    private static GraphDatabaseService graphDb;

    static int oneSidedDepth = 0;


    static Node startNode; //= getOrCreateNode(label, "fish oils", "D10.627.430", 1);

    static IndexManager index;
    static Index<Node> titleIdx;
    static Index<Node> meshIdx;
    static RelationshipIndex dateIdx;



    public bidirectionalTraversal(String dbPath, int year, int depthStart) {
        this.DB_PATH = dbPath;

        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(DB_PATH));
        try (Transaction tx = graphDb.beginTx()) {

            index = graphDb.index();
            boolean indexExists = index.existsForNodes("meshName");
            System.out.println(index.nodeIndexNames().length);
            System.out.println(indexExists);
            titleIdx = index.forNodes("article");
            meshIdx = index.forNodes("meshName");
            dateIdx = index.forRelationships("dates");

            this.oneSidedDepth = depthStart;

        }
    }

    public static void main(String args[]) {
        String dbPath = args[0];
        String writePath = args[1];
//        String ipPath = "/Users/super-machine/Documents/Research/medline/output/traversal/path_length_4/ipTestCases.txt";
        int year = 0;
        int depthStart = 6;



//        try {
//            BufferedReader br = new BufferedReader(new FileReader(ipPath));
            String line = "1985 fish oils   raynaud disease";
            int counter = 1;
//            while ((line = br.readLine()) != null) {

                Set<String> termSet1 = new HashSet<>();
                Set<String> termSet2 = new HashSet<>();

                String splits[] = line.split("\t");
                if (splits.length == 3) {
                    year = Integer.parseInt(splits[0]);
                    String terms1 = splits[1].toLowerCase();
                    String terms2 = splits[2].toLowerCase();
                    String termsSplit1[] = terms1.split("\\$");
                    String termsSplit2[] = terms2.split("\\$");
                    for (String term : termsSplit1) {
                        termSet1.add(term.toLowerCase());
                    }
                    for (String term : termsSplit2) {
                        termSet2.add(term.toLowerCase());
                    }
//                }

                bidirectionalTraversal traversal = new bidirectionalTraversal(dbPath, year, depthStart / 2);
                long startTime = System.nanoTime();
                for(String term1:termSet1) {
                    traversal.uniDirectionalTraverser(term1, writePath + counter + "_A", year);
                }
                long endTime = System.nanoTime();
                long duration = (endTime - startTime);
                try {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(writePath+counter+"_A", true));
                    bw.newLine();
                    bw.write("Total Time taken: " + duration);
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startTime = System.nanoTime();
                for(String term2:termSet2) {
                    traversal.uniDirectionalTraverser(term2, writePath + counter + "_A1", year);
                }
                endTime = System.nanoTime();
                duration = (endTime - startTime);
                try {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(writePath+counter+"_A1", true));
                    bw.newLine();
                    bw.write("Total Time taken: " + duration);
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                MergeUniDirectionTraversal merger = new MergeUniDirectionTraversal();
//                merger.mergeDriver(writePath+counter+"_A", writePath+counter+"_A1", writePath+counter+"_merged");

                counter++;
                graphDb.shutdown();
            }
//            br.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void uniDirectionalTraverser(String term, String writePath,int year) {
        System.out.println("Starting unidirectional traverser for " + term+"\t"+year);
        try (Transaction tx = graphDb.beginTx()) {
            registerShutdownHook(graphDb);
            startNode = get(term, 2);
            TraversalDescription description = graphDb.traversalDescription()
                    .breadthFirst()
                    .uniqueness(Uniqueness.NODE_PATH)
                    .expand(new SpecificRelsPathExpander(year))
                    .evaluator(Evaluators.toDepth(oneSidedDepth));

            Traverser traverser = description.traverse(startNode);
            ResourceIterator<Path> Paths = traverser.iterator();
            BufferedWriter bw = null;
            try {
                bw = new BufferedWriter(new FileWriter(writePath, true));
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (Paths.hasNext()) {
                Path p = Paths.next();
                bw.write(p.toString());
                bw.newLine();
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

    private enum Rels implements RelationshipType {

        KNOWS
    }

    public static void writePaths(Path path, BufferedWriter bw) {
        try {
            Label meshLabel = Label.label("meshName");
            Label articleLabel = Label.label("article");
            StringBuilder toWrite = new StringBuilder();
            for (Node n : path.nodes()) {

                if (n.hasLabel(meshLabel)) {
                    toWrite.append("\t").append(n.getProperty("meshName"));
                }
                if (n.hasLabel(articleLabel)) {
                    toWrite.append("\t").append(n.getProperty("article"));
                }
            }
            bw.write(path.toString());
            bw.newLine();
            bw.write(toWrite.toString());
            bw.newLine();

        } catch (IOException ex) {
            Logger.getLogger(bidirectionalTraversal.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

}

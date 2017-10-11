package neo4jGraphIndexer;

import org.apache.lucene.search.NumericRangeQuery;
import org.neo4j.graphdb.*;


import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.graphdb.traversal.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

    private static TraversalDescription td;

    private static String writePath1;
    private static String writePath2;
    private static String writePath3;
    static Label label = Label.label("mesh");
    static int oneSidedDepth = 0;
    static long cutoffdegree = 1000000000;
    static Set<Path> collection = new HashSet<>();

    static Node startNode; //= getOrCreateNode(label, "fish oils", "D10.627.430", 1);
    static Node endNode;// = getOrCreateNode(label, "raynaud disease", "C14.907.617.812", 1);
    static IndexManager index;
    static Index<Node> titleIdx;
    static Index<Node> meshIdx;
    static RelationshipIndex dateIdx;
    static int year ;



    public bidirectionalTraversal(String dbPath, String writePath1,String writePath2, String writePath3,int year,int depthStart) {
        this.DB_PATH = dbPath;
        this.writePath1 = writePath1;
        this.writePath2 = writePath2;
        this.writePath3 = writePath3;
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(DB_PATH));
        try (Transaction tx = graphDb.beginTx()) {

            index = graphDb.index();
            boolean indexExists = index.existsForNodes( "meshName" );
            System.out.println(index.nodeIndexNames().length);
            System.out.println(indexExists);
            titleIdx = index.forNodes("article");
            meshIdx = index.forNodes("meshName");
            dateIdx = index.forRelationships("dates");
            this.year = year;
            this.oneSidedDepth = depthStart;

        }
    }

    public static void main(String args[]){

        String term1 = "Indomethacin";
        String term2 = "Alzheimer Disease";
        String dbPath = "/Users/super-machine/Documents/Research/medline/output/dummy.db/";
        String writePath1 = "/Users/super-machine/Documents/Research/medline/output/traversal/path_length_4/in.txt";
        String writePath2 = "/Users/super-machine/Documents/Research/medline/output/traversal/path_length_4/al.txt";
        String writePath3 = "/Users/super-machine/Documents/Research/medline/output/traversal/path_length_4/in-al.txt";
        int year = 1989;
        int depthStart = 4;
        bidirectionalTraversal traversal = new bidirectionalTraversal(dbPath,writePath1,writePath2,writePath3,year,depthStart/2);
        long startTime = System.nanoTime();
        traversal.uniDirectionalTraverser(term1,writePath1);
        traversal.uniDirectionalTraverser(term2,writePath2);
        MergeUniDirectionTraversal merger = new MergeUniDirectionTraversal();
        merger.mergeDriver(writePath1,writePath2,writePath3);
        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(writePath3,true));
            bw.newLine();
            bw.write("Total Time taken: "+duration);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void uniDirectionalTraverser(String term,String writePath){
        System.out.println("Starting unidirectional traverser for "+term);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Node get(String nodeName,   int type) {
        Node nd = null;
        nodeName=nodeName.toLowerCase().trim();
        try {
            if (type == 1) {
                IndexHits<Node> nodes = titleIdx.get("article", nodeName);
                nd= nodes.getSingle();
            }
            if (type == 2) {
                IndexHits<Node> nodes = meshIdx.get("meshName", nodeName);
                nd= nodes.getSingle();
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
            NumericRangeQuery<Integer> pageQueryRange = NumericRangeQuery.newIntRange("year-numeric", null, requiredProperty,true,true);
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

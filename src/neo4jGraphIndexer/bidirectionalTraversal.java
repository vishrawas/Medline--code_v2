package neo4jGraphIndexer;

import MeSH_Vector.helperClass;
import org.apache.lucene.search.NumericRangeQuery;
import org.neo4j.graphdb.*;


import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.graphdb.traversal.*;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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


    static Node startNode;

    static IndexManager index;
    static Index<Node> titleIdx;
    static Index<Node> meshIdx;
    static RelationshipIndex dateIdx;


    public bidirectionalTraversal(String dbPath, int year, int depthStart) {


    }

    public static void main(String args[]) {
        String dbPath = "/Users/super-machine/Documents/Research/medline/output/dummy.db";
        String writePath = "/Users/super-machine/Documents/Research/medline/output/traversal/path_length_6/";
        int year = 0;
        int depthStart = 3;
        System.out.println("starting--->");
        String line = "1985\tFish oils\tRaynaud Disease";
        LinkedHashSet<String> termSet1 = new LinkedHashSet<>();
        LinkedHashSet<String> termSet2 = new LinkedHashSet<>();
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
            DB_PATH = dbPath;
            graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(DB_PATH));

            try (Transaction tx = graphDb.beginTx()) {
                index = graphDb.index();
                titleIdx = index.forNodes("article");
                meshIdx = index.forNodes("meshName");
                dateIdx = index.forRelationships("dates");

                oneSidedDepth = depthStart;
                BufferedWriter bw = new BufferedWriter(new FileWriter(writePath + "Raynaud_A1", true));
                registerShutdownHook(graphDb);
                long startTime = System.nanoTime();
                for (String term : termSet2) {
                    System.out.println("term:" + term);
                    IndexHits<Node> nodes = meshIdx.get("meshName", term);
                    startNode = nodes.getSingle();
                    if (startNode != null) {
                        System.out.println("term found: " + term + "\tdegree: " + startNode.getDegree());
                        TraversalDescription description = graphDb.traversalDescription()
                                .breadthFirst()
                                .uniqueness(Uniqueness.NODE_PATH)
                                .expand(new SpecificRelsPathExpander(year))
                                .evaluator(Evaluators.toDepth(oneSidedDepth));
                        Traverser traverser = description.traverse(startNode);
                        ResourceIterator<Path> Paths = traverser.iterator();

                        StringBuilder builder = new StringBuilder();
                        int counter = 0;
                        if (Paths != null) {
                            while (Paths.hasNext()) {
                                Path p = Paths.next();
                                builder.append(p.toString()).append("\n");
                                counter++;
                                if (counter == 100000) {
                                    bw.write(builder.toString());
                                    bw.newLine();
                                    counter = 0;
                                    builder.setLength(0);
                                    builder.trimToSize();
                                }
                            }
                            bw.write(builder.toString());
                            bw.newLine();
                        }
                    } else {
                        System.out.println("term not found " + term);
                    }
                }
                long endTime = System.nanoTime();
                long duration = (endTime - startTime);
                bw.write("Total Time taken: " + duration);
                bw.close();
                tx.success();
            } catch (IOException e) {
                e.printStackTrace();
            }
            graphDb.shutdown();
        }
    }

    private static boolean checkIfToProceed(String term, int year) {
        helperClass helper = new helperClass();
        HashMap<String, Integer> index = helper.getIndex("/Users/super-machine/Documents/Research/medline/output/index");

        int termIndex = index.get(term);
        int raynaudIndex = index.get("raynaud disease");

        try {
            BufferedReader br = new BufferedReader(new FileReader("/Users/super-machine/Documents/Research/medline/output/yearCoccur" + File.separator + raynaudIndex));
            String line = "";
            while ((line = br.readLine()) != null) {
                String splits[] = line.split("\t");
                Integer otherIndex = Integer.valueOf(splits[0]);
                if (otherIndex == termIndex) {
                    String yearPMID = splits[1];
                    String splitsTemp[] = yearPMID.split(" ");
                    int yearTemp = Integer.parseInt(splitsTemp[0]);
                    if (yearTemp < year) {
                        return false;
                    } else {
                        return true;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

//    private static void uniDirectionalTraverser(String term, String writePath, int year) {
//        System.out.println("Starting unidirectional traverser for " + term + "\t" + year);
//        BufferedWriter bw = null;
//        try {
//            bw = new BufferedWriter(new FileWriter(writePath, true));
//            startNode = get(term, 2);
//            if (startNode != null) {
//                System.out.println("term found:" + term + "\tdegree" + startNode.getDegree());
//                TraversalDescription description = graphDb.traversalDescription()
//                        .depthFirst()
//                        .uniqueness(Uniqueness.NODE_PATH)
//                        .expand(new SpecificRelsPathExpander(year))
//                        .evaluator(Evaluators.toDepth(oneSidedDepth));
//                Traverser traverser = description.traverse(startNode);
//                ResourceIterator<Path> Paths = traverser.iterator();
//
//                StringBuilder builder = new StringBuilder();
//                int counter = 0;
//                if (Paths != null) {
//                    while (Paths.hasNext()) {
//                        Path p = Paths.next();
//                        builder.append(p.toString()).append("\n");
//                        counter++;
//                        if (counter == 100000) {
//                            bw.write(builder.toString());
//                            bw.newLine();
//                            counter = 0;
//                            builder.setLength(0);
//                        }
//                    }
//                    bw.write(builder.toString());
//                    bw.newLine();
//                }
//
//            } else {
//                System.out.println("term not found " + term);
//            }
//
//
//            bw.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }


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


    private class pathEvaluator implements Evaluator {
        @Override
        public Evaluation evaluate(Path path) {
            if (path.length() > 1) {
                if (path.endNode().hasLabel(Label.label("meshName"))) {
                    Node nd = path.endNode();
                    String mesh = nd.getProperty("meshName").toString();
//                if (mesh.equalsIgnoreCase("aged") || mesh.equalsIgnoreCase("temperature") || mesh.equalsIgnoreCase("animals") || mesh.equalsIgnoreCase("time factors") || mesh.equalsIgnoreCase("humans") || mesh.equalsIgnoreCase("adult") || mesh.equalsIgnoreCase("female") || mesh.equalsIgnoreCase("male")) {
//                    return Evaluation.EXCLUDE_AND_PRUNE;
//                } else {
//                    return Evaluation.INCLUDE_AND_CONTINUE;
//                }
                    if (mesh.equalsIgnoreCase("Raynaud Disease")) {
                        return Evaluation.EXCLUDE_AND_PRUNE;
                    } else {
                        return Evaluation.INCLUDE_AND_CONTINUE;
                    }
                } else {
                    return Evaluation.INCLUDE_AND_CONTINUE;
                }
            } else {
                return Evaluation.INCLUDE_AND_CONTINUE;
            }

        }
    }
}

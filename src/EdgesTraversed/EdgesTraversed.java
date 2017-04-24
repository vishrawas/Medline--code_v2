/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EdgesTraversed;

import MeSH_Vector.helperClass;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import wordVecSimilarity.WordVectorsOperation;
import static goldenTestCases.GoldenTestCases.registerShutdownHook;
import java.util.HashSet;
import java.util.Set;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

/**
 *
 * @author super-machine
 */
public class EdgesTraversed {

    static Index<Node> titleIdx;
    static GraphDatabaseService graphDb;
    static Index<Node> meshIdx;
    static IndexManager index;
    static String dirMeSHOutputPath = "/home/super-machine/Documents/mydrive/myResearch/output";
    static final String DB_PATH = "/home/super-machine/Documents/mydrive/myResearch/output/dummy.db";
    static helperClass helper = new helperClass();
    String wordVectorBaseDir = dirMeSHOutputPath + File.separator + "invertedIndex";
    static WordVectorsOperation wordVecCosine;
    LinkedHashMap<String, String> mapClassiferOutputs = new LinkedHashMap<>();

    EdgesTraversed() {
        connectGraphDatabase();
    }

    public static void main(String args[]) {
        String dirMeSHOutputPath = "/home/super-machine/Documents/mydrive/myResearch/output";
        
        Set<String> genericTerms = new HashSet<String>();
//        genericTerms.add("humans");
//        genericTerms.add("adults");
//        genericTerms.add("male");
//        genericTerms.add("female");
//        genericTerms.add("animals");

        EdgesTraversed edgesTraversed = new EdgesTraversed();
        edgesTraversed.mapTermsToClassiferOutput(dirMeSHOutputPath);
        edgesTraversed.generateEdgesTraversed(dirMeSHOutputPath, (HashSet<String>) genericTerms);
    }

    private void generateEdgesTraversed(String dirMeSHOutputPath, HashSet<String> genericTerms) {
        try {
            BufferedReader rawPaths = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "path_length_4" + File.separator + "Schizophrenia-Cal.txt"));
            String line2 = "";
            int totalEdges = 0;
            int edgesTraversed = 0;
            try (Transaction tx = graphDb.beginTx()) {

                while ((line2 = rawPaths.readLine()) != null) {
                    if (!"".equals(line2) && line2.contains("--")) {
                        String splits[] = line2.split("--");
                        
                        if (splits.length == 9) {
                            String intermediateTerm = getMeshNameFromNodeID(Long.parseLong(splits[4].replace("(", "").replace(")", "").trim()));
                            if (!genericTerms.contains(intermediateTerm)) {
                                if (mapClassiferOutputs.containsKey(intermediateTerm)) {
                                    edgesTraversed++;
                                }
                                totalEdges++;
                            }

                        }
                    }
                }
                tx.success();
            }
            System.out.println("Total edges : " + totalEdges);
            System.out.println("Edges traversed : " + edgesTraversed);
            double percentageReduced = (double) edgesTraversed / totalEdges;
            System.out.println("Percentages edges saved : " + (100 - percentageReduced * 100));

        } catch (FileNotFoundException ex) {
            Logger.getLogger(EdgesTraversed.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(EdgesTraversed.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void mapTermsToClassiferOutput(String dirMeSHOutputPath) {
        try {
            BufferedReader rankedPaths = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "edgesTraversed" + File.separator + "schizophrenia-phospholipases" + File.separator + "Schizophrenia-Phospholipases-rankedByCosine.txt"));
            BufferedReader classiferOutputs = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "edgesTraversed" + File.separator + "schizophrenia-phospholipases" + File.separator + "Schizophrenia-Phospholipases-PredictionsOutput.txt"));
            String line = "";
            String line1 = "";

            while ((line = rankedPaths.readLine()) != null && (line1 = classiferOutputs.readLine()) != null) {
                String splits[] = line.split("\t");
                if ("target".equals(line1.trim())) {
                    mapClassiferOutputs.put(splits[2], line1.trim());
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(EdgesTraversed.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(EdgesTraversed.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static String getMeshNameFromNodeID(long nodeID) {
        String meshName = "";
        index = graphDb.index();
        Node meshNode = graphDb.getNodeById(nodeID);
        meshName = meshNode.getProperty("meshName").toString();
        return meshName.trim();
    }

    public static void connectGraphDatabase() {
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(DB_PATH));
        registerShutdownHook(graphDb);
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package openDiscovery;

import static generateTrainingSet.GraphTrainingSet.wordVectorBaseDir;
import static generateTrainingSet.GraphTrainingSet.year;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;
import wordVecSimilarity.WordVectorsOperation;

/**
 *
 * @author super-machine
 */
public class openDiscoveryRankByCosine {

    static Index<Node> titleIdx;
    static GraphDatabaseService graphDb;
    static Index<Node> meshIdx;
    static RelationshipIndex dateIdx;
    static IndexManager index;
    static String dirMeSHOutputPath = "/home/super-machine/Documents/mydrive/myResearch/output";
    static final String DB_PATH = "/home/super-machine/Documents/mydrive/myResearch/output/dummy.db";
    static int year = 1985;
    static WordVectorsOperation wordVecCosine;
    static HashMap<String, String> classifierOutput = new HashMap<>();

    static final String pattern = "\\((.*?)\\)";
    static Pattern r = Pattern.compile(pattern);
    static File file;

    openDiscoveryRankByCosine() {
        connectGraphDatabase();
        wordVecCosine = new WordVectorsOperation(wordVectorBaseDir);
        loadPredictionsOutput();
    }

    public static void main(String args[]) {
        file = new File(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "path_length_4" + File.separator + "Schizophrenia-CalRankedByCosine.txt");
        if (file.exists()) {
            file.delete();
            try {
                file.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(openDiscoveryRankByCosine.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            try {
                file.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(openDiscoveryRankByCosine.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        openDiscoveryRankByCosine opendiscoveryrankbycosine = new openDiscoveryRankByCosine();
        ArrayList<String> rankedPaths = opendiscoveryrankbycosine.rankPaths();
//        opendiscoveryrankbycosine.printRankedPaths(rankedPaths);
    }

    private ArrayList<String> rankPaths() {

        ArrayList<String> pathList = new ArrayList<>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "path_length_4" + File.separator + "Schizophrenia-Cal.txt"));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(openDiscoveryRankByCosine.class.getName()).log(Level.SEVERE, null, ex);
        }

        String line = "";
        int lineCounter = 0;
        int targetCounter = 0;
        int count = 0;
        Set<String> intermediate = new HashSet<>();
        try (Transaction tx = graphDb.beginTx()) {
            while ((line = br.readLine()) != null) {
                if (!"".equals(line) && line.contains("--")) {
                    ArrayList<String> splits = getNodeIds(line);
//                    String splits[] = line.split("--");
                    if (splits.size() == 5) {
                        String startTerm = getMeshNameFromNodeID(Long.parseLong((splits.get(0)).trim()));
                        String intermediateTerm = getMeshNameFromNodeID(Long.parseLong((splits.get(2)).trim()));
                        String endTerm = getMeshNameFromNodeID(Long.parseLong((splits.get(4)).trim()));
                        if (!intermediate.contains(intermediateTerm + "\t" + endTerm)) {
                            if (!classifierOutput.containsKey(intermediateTerm.toLowerCase())) {
                            } else {
                                double cosineSimOne = wordVecCosine.getSimilarity(startTerm.toLowerCase(), intermediateTerm, year, year) + 1;
                                double cosineSimTwo = wordVecCosine.getSimilarity(intermediateTerm, endTerm.toLowerCase(), year, year) + 1;
                                double finalCosineScore = (cosineSimOne + cosineSimTwo) / 2;
                                pathList.add(finalCosineScore + "\t" + startTerm + "\t" + intermediateTerm + "\t" + endTerm);
                                intermediate.add(intermediateTerm + "\t" + endTerm);
                            }
                        }

                        if (classifierOutput.containsKey(intermediateTerm.toLowerCase())) {
                            targetCounter++;
                        }
                    }

                }

                if (lineCounter % 100000 == 0) {
                    printRankedPaths(pathList);
                    pathList.clear();
                    System.out.println(count * 100000 + " lines processing done");
                    count++;
                }
                lineCounter++;
            }
            System.out.println("finishing up");
            printRankedPaths(pathList);
            pathList.clear();
            tx.success();

        } catch (IOException ex) {
            Logger.getLogger(openDiscoveryRankByCosine.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("Total Number of paths : " + lineCounter);
        System.out.println("Total paths traversed : " + targetCounter);
        System.out.println("Total paths saving : " + (lineCounter - targetCounter) / lineCounter * 100);

        return pathList;
    }

    private ArrayList<String> getNodeIds(String s) {
        ArrayList<String> terms = new ArrayList<>();
        Matcher m = r.matcher(s);
        while (m.find()) {
            terms.add(m.group(1));
        }
        return terms;
    }

    private void printRankedPaths(ArrayList<String> rankedPaths) {

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(file, true));
            StringBuilder builder = new StringBuilder();
            for (String path : rankedPaths) {
                builder.append(path).append("\n");
            }
            bw.append(builder.toString());
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(openDiscoveryRankByCosine.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void loadPredictionsOutput() {
        try {
            BufferedReader br_IntermediateTerms = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "path_length_open_discovery" + File.separator + "open_fo_rd_intermediateTerms.txt"));
            BufferedReader br_PredictionsOutput = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "path_length_open_discovery" + File.separator + "open_fo_rd_predictions_output.txt"));
            String line_intermediate, line_predictions = "";

            while ((line_intermediate = br_IntermediateTerms.readLine()) != null && (line_predictions = br_PredictionsOutput.readLine()) != null) {
                if (line_predictions.trim().equals("target")) {
                    classifierOutput.put(line_intermediate.toLowerCase().trim(), line_predictions.toLowerCase().trim());
                }
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(openDiscoveryRankByCosine.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(openDiscoveryRankByCosine.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static String getMeshNameFromNodeID(long nodeID) {
        String meshName = "";
        Node meshNode = graphDb.getNodeById(nodeID);
        meshName = meshNode.getProperty("meshName").toString();
        return meshName.trim();
    }

    public static void connectGraphDatabase() {
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(DB_PATH));
        index = graphDb.index();
        registerShutdownHook(graphDb);
    }

    public static void registerShutdownHook(final GraphDatabaseService graphDb) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
            }
        });
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package generateTrainingSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import wordVecSimilarity.WordVectorsOperation;

/**
 *
 * @author super-machine
 */
public class cosineJoin {

    static final String DB_PATH = "/home/super-machine/Documents/mydrive/myResearch/output/dummy.db";
    static GraphDatabaseService graphDb;
    static String dirMeSHOutputPath = "/home/super-machine/Documents/mydrive/myResearch/output";
    String wordVectorBaseDir = dirMeSHOutputPath + File.separator + "invertedIndex";
    HashMap<Integer, String> indexMeSHPath = new HashMap<>();
    TreeMap<Double, Set<Integer>> scoreIndexSet = new TreeMap<>();
    int year = 1988;
    String startTerm = "Migraine Disorders";
    String endTerm = "Magnesium";
    static WordVectorsOperation wordVecCosine;
    static String toWriteFolder = dirMeSHOutputPath + File.separator + "goldenTestCasesRankedPaths";
    static IndexManager index;
    static Index<Node> meshIdx;
    
    public static void main(String args[]) {

        cosineJoin cosine = new cosineJoin();
        try (Transaction tx = graphDb.beginTx()) {
            
//            Long nodeID = getNodeIDFromMeshName("Anti-Inflammatory Agents, Non-Steroidal");
//            System.out.println("Node id: "+nodeID);
            File file = new File(toWriteFolder);
            if (!file.isDirectory()) {
                file.mkdir();
            }
            String traversalOutput = "/home/super-machine/Documents/mydrive/myResearch/output/traversal/intermediates/mi-mg.txt";
            cosine.parsePathAndScore(traversalOutput);
        }
    }
    
    public static long getNodeIDFromMeshName(String meshName){
        long nodeID= 0;
        Label meshLabel = Label.label("mesh");
        Node nd = null;
        index = graphDb.index();
        meshIdx = index.forNodes("meshName");
        IndexHits<Node> nodes  = meshIdx.get("meshName", meshName.toLowerCase());
        nd= nodes.getSingle();
        return nd.getId();
    }

    public cosineJoin() {
        connectGraphDatabase();
        wordVecCosine = new WordVectorsOperation(wordVectorBaseDir);
    }

    public static void connectGraphDatabase() {
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(DB_PATH));
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

    public static String getMeshNameFromNodeID(long nodeID) {
        String meshName = "";
        Node meshNode = graphDb.getNodeById(nodeID);
        meshName = meshNode.getProperty("meshName").toString();
        return meshName.trim();
    }

    private void parsePathAndScore(String traversalOutput) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(traversalOutput));
            String line = "";
            int lineCounter = 0;
            while ((line = br.readLine()) != null) {

                ArrayList<String> allTermString = new ArrayList<>();
                ArrayList<String> allTerms = getAllTerms(line);
                String startTerm = allTerms.get(0);
                allTerms.remove(startTerm);
                String endTerm = allTerms.get(allTerms.size() - 1);
                allTerms.remove(endTerm);
                startTerm = getMeshNameFromNodeID(Long.parseLong(startTerm));
                endTerm = getMeshNameFromNodeID(Long.parseLong(endTerm));
                for (String interTerm : allTerms) {
                    String meshTerm = getMeshNameFromNodeID(Long.parseLong(interTerm));
                    allTermString.add(meshTerm);
                }
                String prevTerm = startTerm;
                int meshCounter = 1;
                double cosine = 0;
                for (String meshTerm : allTermString) {
                    double score = wordVecCosine.getSimilarity(prevTerm, meshTerm, year, year);
                    cosine = cosine + score;
                    prevTerm = meshTerm;
                    meshCounter++;
                }
                double lastScore = wordVecCosine.getSimilarity(prevTerm, endTerm, year, year);
                cosine = cosine + lastScore;
                cosine = cosine / meshCounter;
                indexMeSHPath.put(lineCounter, line);
                if (scoreIndexSet.containsKey(cosine)) {
                    Set<Integer> indices = scoreIndexSet.get(cosine);
                    indices.add(lineCounter);
                    scoreIndexSet.put(cosine, indices);
                } else {
                    Set<Integer> indices = new HashSet<>();
                    indices.add(lineCounter);
                    scoreIndexSet.put(cosine, indices);
                }

                if (lineCounter % 10000 == 0) {
                    System.out.println("appending to index");
                    writeIndexMeshPath();
                    indexMeSHPath.clear();
                }
                lineCounter++;
            }
            System.out.println("finishing");
            writeIndexMeshPath();
            System.out.println("writing treemap");
            writeTreeMap();

        } catch (FileNotFoundException ex) {
            Logger.getLogger(cosineJoin.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(cosineJoin.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(cosineJoin.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private static ArrayList<String> getAllTerms(String line) {
        ArrayList<String> terms = new ArrayList<>();
        
        int indexOfStart = line.indexOf("(");
        int indexOfEnd = 0;
        int termCounter = 0;
        while (indexOfStart != -1) {
            indexOfEnd = line.indexOf(")");
            if (indexOfEnd != -1) {
                String subString = line.substring(indexOfStart+1, indexOfEnd);
      
                if (termCounter % 2 == 0) {
                    terms.add(subString);
                }

                line = line.substring(indexOfEnd+1, line.length());
                
                indexOfStart = line.indexOf("(");
               
            }
            termCounter++;
        }
        return terms;
    }

    private void writeIndexMeshPath() {
        BufferedWriter bw = null;
        try {
            String fileName = startTerm + "_" + endTerm + "_index";
            bw = new BufferedWriter(new FileWriter(toWriteFolder + File.separator + fileName, true));
            StringBuilder builder = new StringBuilder();
            for (Integer index : indexMeSHPath.keySet()) {
                builder.append(index).append("\t").append(this.indexMeSHPath.get(index)).append("\n");
            }
            bw.write(builder.toString());
        } catch (IOException ex) {
            Logger.getLogger(cosineJoin.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                bw.close();
            } catch (IOException ex) {
                Logger.getLogger(cosineJoin.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private void writeTreeMap() {
        BufferedWriter bw = null;
        try {
            String fileName = startTerm + "_" + endTerm + "_treeMap";
            bw = new BufferedWriter(new FileWriter(toWriteFolder + File.separator + fileName, true));
            StringBuilder builder = new StringBuilder();
            for (Double score : scoreIndexSet.keySet()) {
                Set<Integer> indices = scoreIndexSet.get(score);
                builder.append(score).append(" ");
                for (Integer index : indices) {
                    builder.append(index).append("\n");
                }
                builder.append("\n");
            }
            bw.write(builder.toString());
        } catch (IOException ex) {
            Logger.getLogger(cosineJoin.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                bw.close();
            } catch (IOException ex) {
                Logger.getLogger(cosineJoin.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}

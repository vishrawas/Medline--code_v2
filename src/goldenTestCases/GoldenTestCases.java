/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package goldenTestCases;

import MeSH_Vector.helperClass;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import static scala.reflect.io.NoAbstractFile.file;
import scala.reflect.io.Path;
import wordVecSimilarity.WordVectorsOperation;

/**
 *
 * @author super-machine
 */
public class GoldenTestCases {

    static Index<Node> titleIdx;
    static GraphDatabaseService graphDb;
    static Index<Node> meshIdx;
    static IndexManager index;
    static String dirMeSHOutputPath = "/home/super-machine/Documents/mydrive/myResearch/output";
    static final String DB_PATH = "/home/super-machine/Documents/mydrive/myResearch/output/dummy.db";
    static helperClass helper = new helperClass();
    static String wordVectorBaseDir = dirMeSHOutputPath + File.separator + "invertedIndex";
    static int year = 1997;
    static WordVectorsOperation wordVecCosine;
    static String fileToWrite = "";
    
    GoldenTestCases() {
        connectGraphDatabase();
        wordVecCosine = new WordVectorsOperation(wordVectorBaseDir);
    }

    public static void main(String args[]) {
        fileToWrite = dirMeSHOutputPath + File.separator + "traversal" + File.separator + "goldenTestCasesRankedPaths" + File.separator + "Schizophrenia-Phospholipases" + "-rankedByCosine.txt";
       /*
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileToWrite));
            BufferedWriter bw = new BufferedWriter(new FileWriter(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "goldenTestCasesRankedPaths" + File.separator + "Schizophrenia-Phospholipases" + "-rankedByCosineFiltered.txt"));
            String line = null;
            int count = 0;
            while( ((line=br.readLine())!=null) && count<5017){
                
                if(!line.isEmpty()){
                    System.out.println(line);
                    bw.write(line);
                    bw.newLine();
                }
                
            }
            bw.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GoldenTestCases.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GoldenTestCases.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        System.exit(-1);
        
        
        File file = new File(fileToWrite);
        if (file.exists()) {
            try {
                file.delete();
                file.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(GoldenTestCases.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            try {
                file.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(GoldenTestCases.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        */
       
        GoldenTestCases goldenTestCase = new GoldenTestCases();
//        goldenTestCase.generateIntermediateMeshTerms(fileToWrite);
//        goldenTestCase.generateWordVectorsForRankedPaths(fileToWrite);
        goldenTestCase.generateWordVectorsForRankedPaths("Schizophrenia-Phospholipases-rankedByCosine.txt");
    }

    private static void generateIntermediateMeshTerms(String dirMeSHOutputPath) {
        Set<String> goldenTestCasesPathLength4 = new HashSet<String>();
        File pathToTestCases = new File(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "path_length_4");
        goldenTestCasesPathLength4 = helper.listFilesForFolder(pathToTestCases);
        int fileCounter = 1;
        for (String fileName : goldenTestCasesPathLength4) {
            String fileExtension = FilenameUtils.getExtension(fileName);
            if (fileExtension.equals("txt")) {
                HashMap<String, Double> cosineRankedPaths = calculateCosineForPaths(fileName);
//                Map<String, Double> sortedCosineRankedPaths = helper.sortByValue(cosineRankedPaths);
//                long startTime = System.nanoTime();
//                printRankedPaths(fileName, sortedCosineRankedPaths);
//                long endTime = System.nanoTime();
//                long duration = (endTime - startTime);  //divide by 1000000 to get milliseconds.
//                System.out.println("Time taken  : " + duration);
//                worldVectorForPaths(fileName);
            }

            if (goldenTestCasesPathLength4.size() == fileCounter) {
                break;
            }

            fileCounter++;

        }
    }

    private static void worldVectorForPaths(String fileName) {

        HashMap<String, Double> intermediateTerms = new HashMap<String, Double>();

        BufferedReader br = null;
        BufferedWriter bw = null;
        BufferedWriter bw1 = null;
        File file = null;
        try {
            br = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "path_length_4" + File.separator + fileName));
            String line = "";

            try {
                file = new File(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "path_length_4" + File.separator + "Schizophrenia-Cal_wordVectors.txt");
                if (file.exists()) {
                    file.delete();
                    file.createNewFile();
                } else {
                    file.createNewFile();
                }
            } catch (IOException ex) {
                Logger.getLogger(GoldenTestCases.class.getName()).log(Level.SEVERE, null, ex);
            }

            try {
                bw = new BufferedWriter(new FileWriter(file, true));
                bw1 = new BufferedWriter(new FileWriter(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "path_length_4" + File.separator + "Schizophrenia-Cal_intermediateTerms.txt", true));
            } catch (IOException ex) {
                Logger.getLogger(GoldenTestCases.class.getName()).log(Level.SEVERE, null, ex);
            }

            try (Transaction tx = graphDb.beginTx()) {
                try {
                    int lineCounter = 0;
                    int count = 1;
                    while ((line = br.readLine()) != null) {
                        if (!"".equals(line) && line.contains("--")) {
                            String splits[] = line.split("--");
                            if (splits.length == 9) {
//                                String startTerm = getMeshNameFromNodeID(Long.parseLong(splits[0].replace("(", "").replace(")", "").trim()));
                                String intermediateTerm = getMeshNameFromNodeID(Long.parseLong(splits[4].replace("(", "").replace(")", "").trim()));
//                                String endTerm = getMeshNameFromNodeID(Long.parseLong(splits[splits.length - 1].replace("(", "").replace(")", "").trim()));
                                if (intermediateTerms.containsKey(intermediateTerm)) {
                                } else {
//                                    double cosineSimOne = wordVecCosine.getSimilarity(startTerm.toLowerCase(), intermediateTerm, year, year) + 1;
//                                    double cosineSimTwo = wordVecCosine.getSimilarity(intermediateTerm, endTerm.toLowerCase(), year, year) + 1;
//                                    double finalCosineScore = (cosineSimOne + cosineSimTwo) / 2;
                                    intermediateTerms.put(intermediateTerm, 0.0);
                                    BufferedReader brWordVecFile;
                                    brWordVecFile = new BufferedReader(new FileReader(wordVectorBaseDir + File.separator + year + File.separator + intermediateTerm.toLowerCase()));
                                    String line1 = null;
                                    String dimensionContent = "";
                                    while ((line1 = brWordVecFile.readLine()) != null) {
                                        String dimensionValue[] = line1.split("\t");
                                        for (String val : dimensionValue) {
                                            dimensionContent = dimensionContent + val + " ";
                                        }
                                    }
                                    String finalContent = dimensionContent.trim();
                                    bw.write(finalContent);
                                    bw.newLine();
                                    bw1.write(intermediateTerm);
                                    bw1.newLine();
                                }
                            }
                        }

                        if (lineCounter % 100000 == 0) {
                            System.out.println(count * 100000 + " lines processing done");
                            count++;
                        }
                        lineCounter++;
                    }
                    bw.close();
                    bw1.close();
                } catch (IOException ex) {
                    Logger.getLogger(GoldenTestCases.class.getName()).log(Level.SEVERE, null, ex);
                }
                tx.success();
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(GoldenTestCases.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(GoldenTestCases.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        System.out.println("ranked Hash map path returned");
//        return rankedPaths;

    }

    private static HashMap<String, Double> calculateCosineForPaths(String fileName) {

        HashMap<String, Double> rankedPaths = new HashMap<String, Double>();
        ArrayList<String> pathList = new ArrayList<>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "path_length_4" + File.separator + fileName));
            String line = "";
            try (Transaction tx = graphDb.beginTx()) {
                try {
                    int lineCounter = 0;
                    int count = 0;
                    while ((line = br.readLine()) != null) {
                        if (!"".equals(line) && line.contains("--")) {
                            String splits[] = line.split("--");
                            if (splits.length == 9) {
                                String startTerm = getMeshNameFromNodeID(Long.parseLong(splits[0].replace("(", "").replace(")", "").trim()));
                                String intermediateTerm = getMeshNameFromNodeID(Long.parseLong(splits[4].replace("(", "").replace(")", "").trim()));
                                String endTerm = getMeshNameFromNodeID(Long.parseLong(splits[splits.length - 1].replace("(", "").replace(")", "").trim()));
                                if (pathList.contains(intermediateTerm.toLowerCase().trim())) {
                                } else {
                                    double cosineSimOne = wordVecCosine.getSimilarity(startTerm.toLowerCase(), intermediateTerm, year, year) + 1;
                                    double cosineSimTwo = wordVecCosine.getSimilarity(intermediateTerm, endTerm.toLowerCase(), year, year) + 1;
                                    double finalCosineScore = (cosineSimOne + cosineSimTwo) / 2;
                                    rankedPaths.put(intermediateTerm + "###" + startTerm + "###" + endTerm, finalCosineScore);
                                    pathList.add(intermediateTerm.toLowerCase().trim());
                                }

                            }
                        }

                        if (lineCounter % 100000 == 0) {
                            printRankedPaths(rankedPaths);
                            rankedPaths.clear();
                            System.out.println(count * 100000 + " lines processing done");
                            count++;    
                        }
                        lineCounter++;
                    }
                } catch (IOException ex) {
                    Logger.getLogger(GoldenTestCases.class.getName()).log(Level.SEVERE, null, ex);
                }
                tx.success();
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(GoldenTestCases.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(GoldenTestCases.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println("ranked Hash map path returned");
        return rankedPaths;
    }

    private static TreeMap<Double, LinkedHashSet<String>> getRankedPaths(String fileName) {
        System.out.println("inside get ranked paths");
        TreeMap<Double, LinkedHashSet<String>> rankedPaths = new TreeMap<>(Comparator.reverseOrder());
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "path_length_4" + File.separator + fileName));
            String line = "";
            try (Transaction tx = graphDb.beginTx()) {
                try {
                    while ((line = br.readLine()) != null) {
                        if (!"".equals(line) && line.contains("--")) {
                            String splits[] = line.split("--");
                            if (splits.length == 9) {
                                String startTerm = getMeshNameFromNodeID(Long.parseLong(splits[0].replace("(", "").replace(")", "").trim()));
                                String intermediateTerm = getMeshNameFromNodeID(Long.parseLong(splits[4].replace("(", "").replace(")", "").trim()));
                                String endTerm = getMeshNameFromNodeID(Long.parseLong(splits[splits.length - 1].replace("(", "").replace(")", "").trim()));
                                double cosineSimOne = wordVecCosine.getSimilarity(startTerm, intermediateTerm, year, year) + 1;
                                double cosineSimTwo = wordVecCosine.getSimilarity(intermediateTerm, endTerm, year, year) + 1;
                                double finalCosineScore = (cosineSimOne + cosineSimTwo) / 2;

                                LinkedHashSet<String> pathTerms;
                                if (rankedPaths.containsKey(finalCosineScore)) {
                                    pathTerms = rankedPaths.get(finalCosineScore);
                                    pathTerms.add(startTerm + "\t" + intermediateTerm + "\t" + endTerm);

                                } else {
                                    pathTerms = new LinkedHashSet<>();
                                    pathTerms.add(startTerm + "\t" + intermediateTerm + "\t" + endTerm);
                                }
                                rankedPaths.put(finalCosineScore, pathTerms);
                            }
                        }
                    }
                } catch (IOException ex) {
                    Logger.getLogger(GoldenTestCases.class.getName()).log(Level.SEVERE, null, ex);
                }
                tx.success();
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(GoldenTestCases.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(GoldenTestCases.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println("ranked paths HashMap returned");
        return rankedPaths;
    }

    private static void printRankedPaths(Map<String, Double> rankedPaths) {
//        System.out.println("entered print ranked paths");
        BufferedWriter bw = null;
        try {

            StringBuilder builder = new StringBuilder();
            bw = new BufferedWriter(new FileWriter(fileToWrite, true));
            for (Map.Entry<String, Double> entry : rankedPaths.entrySet()) {
                String meshPaths = entry.getKey();
                String splits[] = meshPaths.split("###");
                Double finalCosineScore = entry.getValue();
                builder.append(finalCosineScore).append("\t").append(splits[1]).append("\t").append(splits[0]).append("\t").append(splits[2]).append("\n");
            }
            bw.write(builder.toString());
            bw.newLine();
        } catch (IOException ex) {
            Logger.getLogger(GoldenTestCases.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                bw.close();
            } catch (IOException ex) {
                Logger.getLogger(GoldenTestCases.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static String getMeshNameFromNodeID(long nodeID) {
        String meshName = "";
        index = graphDb.index();
        Node meshNode = graphDb.getNodeById(nodeID);
        meshName = meshNode.getProperty("meshName").toString();
        return meshName.trim();
    }

    private void generateWordVectorsForRankedPaths(String dirMeSHOutputPath) {
//        Set<String> goldenTestCasesRankedPaths = new HashSet<String>();
//        File pathToGoldenTestCasesRankedPaths = new File(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "goldenTestCasesRankedPaths");
//        goldenTestCasesRankedPaths = helper.listFilesForFolder(pathToGoldenTestCasesRankedPaths);

//        for (String fileName : goldenTestCasesRankedPaths) {
//            String fileExtension = FilenameUtils.getExtension(fileName);
//            if (fileExtension.equals("txt")) {
                loadWordVectorsWriteToFile(dirMeSHOutputPath);
//            }
//        }
    }

    private void loadWordVectorsWriteToFile(String fileName) {
        BufferedWriter bw = null;
        BufferedReader br = null;
        File file = null;
        try {
            try {
                file = new File(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "matlabTrainingFiles" + File.separator + "traversalTesting" + File.separator + fileName.substring(0, fileName.length() - 4) + "_WordVectors.txt");
                if (file.exists()) {
                    file.delete();
                    file.createNewFile();
                } else {
                    file.createNewFile();
                }
            } catch (IOException ex) {
                Logger.getLogger(GoldenTestCases.class.getName()).log(Level.SEVERE, null, ex);
            }
            br = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "goldenTestCasesRankedPaths" + File.separator + fileName));
            String line = "";
            try {
                bw = new BufferedWriter(new FileWriter(file));
                while ((line = br.readLine()) != null) {
                    String splits[] = line.split("\t");
                    if (splits.length > 2) {
                        String term = splits[2]; // intermediate terms
                        BufferedReader brWordVecFile = new BufferedReader(new FileReader(wordVectorBaseDir + File.separator + year + File.separator + term));
                        String line1 = null;
                        StringBuilder builder = new StringBuilder();
                        while ((line1 = brWordVecFile.readLine()) != null) {
                            String dimensionValue[] = line1.split("\t");
                            for (String val : dimensionValue) {
                                builder.append(val).append(" ");
                            }
                            builder.append("\n");
                        }
                        bw.write(builder.toString());
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(GoldenTestCases.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    bw.close();
                } catch (IOException ex) {
                    Logger.getLogger(GoldenTestCases.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(GoldenTestCases.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GoldenTestCases.class.getName()).log(Level.SEVERE, null, ex);
        }
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

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package generateTrainingSet;

import MeSH_Vector.helperClass;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;
import generateTrainingSet.TrainingSetDriver;
import static generateTrainingSet.TrainingSetDriver.meshIndexMap;
import static generateTrainingSet.TrainingSetDriver.indexMeshMap;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.search.NumericRangeQuery;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.IndexHits;
import wordVecSimilarity.WordVectorsOperation;

/**
 *
 * @author super-machine
 */
public class GraphTrainingSet {

    public static Index<Node> titleIdx;
    public static GraphDatabaseService graphDb;
    public static Index<Node> meshIdx;
    public static RelationshipIndex dateIdx;
    public static IndexManager index;
    public static SimpleDateFormat sdf = new SimpleDateFormat("EEEE MMMM dd HH:mm:ss Z yyyy", Locale.US);

    public static String dirMeSHOutputPath = "/home/super-machine/Documents/mydrive/myResearch/output";
    public static final String DB_PATH = "/home/super-machine/Documents/mydrive/myResearch/output/dummy.db";
    public static String wordVectorBaseDir = dirMeSHOutputPath + File.separator + "invertedIndex";

    public static helperClass helper = new helperClass();
    public static TreeMap<Double, Set<String>> topKRanked = new TreeMap<>(Comparator.reverseOrder());
    public static WordVectorsOperation operation;
    public static HashMap<Node, Integer> otherNodeLatestYear = new HashMap<>();
    public HashMap<String, String> meshTermTreeCodeAndSemanticTypeMapping = new HashMap<>();
    public static HashMap<Node, Double> falsePositiveNeighbors = new HashMap<>();
    public static String inputTerm1 = "Schizophrenia";
    public static String inputTerm2 = "Phospholipases";
    public static int year = 1997;
    public static String similarPairspath = dirMeSHOutputPath + File.separator + "traversal" + File.separator + "matlabTrainingFiles" + File.separator + "similarPairs" + File.separator + "closedDiscovery"+ File.separator + "1997:schizophreniaphospholipases a2, calcium-independent$phospholipases a2$phospholipases.txt";
    public static String truePositiveTermsPath = dirMeSHOutputPath + File.separator + "traversal" + File.separator + "matlabTrainingFiles" + File.separator + "trainingTestingSet" + File.separator + "closedDiscovery" + File.separator + year+":"+""+inputTerm1.replace(" ", "_") + ":" + inputTerm2.replace(" ", "_") + "_truePositives.txt";;
    public static String falsePositiveTermsPath = dirMeSHOutputPath + File.separator + "traversal" + File.separator + "matlabTrainingFiles" + File.separator + "trainingTestingSet" + File.separator +"closedDiscovery" +File.separator + year+":"+""+inputTerm1.replace(" ", "_")+":"+ inputTerm2.replace(" ", "_") + "_falsePostive.txt";;

     public GraphTrainingSet(TreeMap<Double, Set<String>> topKRanked) {
        connectGraphDatabase();
        this.topKRanked = topKRanked;
        operation = new WordVectorsOperation(wordVectorBaseDir);
        loadmeshTreeCodeAndSemanticType(dirMeSHOutputPath);
    }

    public static void main(String args[]) {
        String dirMedlineInputPath = "/Users/super-machine/Documents/Research/medline/data/medline_raw_files";
        String dirMeSHOutputPath = "/home/super-machine/Documents/mydrive/myResearch/output";
        int maxDepth = 1;
        TrainingSetDriver driver = new TrainingSetDriver();
        driver.loadMeshIndex(dirMeSHOutputPath + File.separator + "index");
        
        Set<String> similarPairs = new HashSet<String>();
        File pathToSimilarPairs = new File(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "matlabTrainingFiles"+ File.separator +"similarPairs");
        similarPairs = helperClass.listFilesForFolder(pathToSimilarPairs);
//        connectGraphDatabase();
//        try (Transaction tx = graphDb.beginTx()) {
//            for (String fileName : similarPairs) {
//                String fileExtension = FilenameUtils.getExtension(fileName);
//                if (fileExtension.equals("txt")) {
//                    System.out.println("File being processed : "+fileName.replace(".txt", ""));
//                    String splits[] = fileName.replace(".txt", "").split(":");
//                    int year = Integer.parseInt(splits[0]);
//                    String inputTerm1 = splits[1].replace("_", " ");
//                    String inputTerm2 = splits[2].replace("_", " ");
//                    populate(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "matlabTrainingFiles" + File.separator + "similarPairs" + File.separator + fileName);
//                    GraphTrainingSet graphTrainingSet = new GraphTrainingSet(topKRanked);
//                    graphTrainingSet.generateTraining(maxDepth, year, inputTerm1.toLowerCase(), inputTerm2.toLowerCase());
//                }
//            }
//            tx.success();
//        }
        
        
        TreeMap<Double, Set<String>> topKRanked = populate(similarPairspath);
        GraphTrainingSet graphTrainingSet = new GraphTrainingSet(topKRanked);
        graphTrainingSet.generateTraining(maxDepth, inputTerm1, inputTerm2); 
    }


    public static TreeMap<Double, Set<String>> populate(String path) {
        try {
            String line = "";
            BufferedReader br = new BufferedReader(new FileReader(path));
            while ((line = br.readLine()) != null) {
                String split[] = line.split("\t");
                Double value = Double.parseDouble(split[0]);
                String pair = split[1] + "\t" + split[2];
                Set<String> meshTerms;
                if (topKRanked.containsKey(value)) {
                    meshTerms = topKRanked.get(value);
                } else {
                    meshTerms = new HashSet<>();
                    meshTerms.add(pair);
                }
                topKRanked.put(value, meshTerms);
            }
        } catch (IOException ex) {
            Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
        }
        return topKRanked;
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

    public static void generateTraining(int maxDepth, String inputTerm1, String inputTerm2) {
        System.out.println("TopKRankedSet size : "+topKRanked.size());
        HashMap<Node, Double> finalSortedNeighbors = new HashMap<>();
        try (Transaction tx = graphDb.beginTx()) {
            for (Map.Entry<Double, Set<String>> entry : topKRanked.entrySet()) {
                Set<String> meshPair = entry.getValue();
                Iterator iter = meshPair.iterator();
                while (iter.hasNext()) {
                    String pair = (String) iter.next();
                    String meshTerms[] = pair.split("\t");
                    String m1 = meshTerms[0];
                    String m2 = meshTerms[1];
                    boolean isFirst = true;
                    HashMap<Node, Double> neighbors = getPaths(m1, m2, year, maxDepth, isFirst, dirMeSHOutputPath);
                    insertIntoFinalNeighborsSorted(finalSortedNeighbors, neighbors);
                }
            }
            Map<Node, Double> sorted = helper.sortByValue(finalSortedNeighbors);
            Map<Node, Double> neighbors = new HashMap<>();
            Iterator iter = sorted.keySet().iterator();
            double sum = 0;
            int count = 0;
            while (iter.hasNext()) {
                Node nd = (Node) iter.next();

                double score = sorted.get(nd);
                sum = sum + score;
                count++;
            }
            double avg = sum / count;

            iter = sorted.keySet().iterator();
            while (iter.hasNext()) {
                Node nd = (Node) iter.next();
                double score = sorted.get(nd);
                if (score > avg) {
                    neighbors.put(nd, score);
                } else {
                    falsePositiveNeighbors.put(nd, score);
                }
            }
            printTruePositiveNeighbors(neighbors, inputTerm1, inputTerm2, String.valueOf(year));
            falsePositiveNeighbors.keySet().removeAll(neighbors.keySet());
            printFalsePositiveNeighbors(inputTerm1, inputTerm2, String.valueOf(year));
            tx.success();
        }
    }

    public static HashMap<Node, Double> getPaths(String term1, String term2, int year, int maxDepth, boolean isFirst, String dirMeSHOutputPath) {

        long PMID = getFirstPMID(term1, term2, year, maxDepth, isFirst, dirMeSHOutputPath);
        HashMap<Node, Double> neighbors = getNeighbours(term1, PMID, term2, year);
        return neighbors;

    }

    public static long getFirstPMID(String term1, String term2, int year, int maxDepth, boolean first, String dirMeSHOutputPath) {
        
//        System.out.println("coming here"+term1+term2);
        
        long PMID = 0;
        int index1 = meshIndexMap.get(term1);
        int index2 = meshIndexMap.get(term2);

        
        try {
            BufferedReader br = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "yearCoccur" + File.separator + index1));
            String line = "";
            while ((line = br.readLine()) != null) {
                String splits[] = line.split("\t");
                String meshTerm = splits[0].trim();
                String secondTerm = splits[1].trim();

                int temp2 = Integer.parseInt(meshTerm);
                if (index2 == temp2) {
                    String splitsInner[] = secondTerm.split(" ");
                    PMID = Integer.parseInt(splitsInner[1]);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return PMID;
    }

    public static HashMap<Node, Double> getNeighbours(String term1, long PMID, String term2, int year) {

        HashMap<Node, Double> neighbors = new HashMap<>();

        index = graphDb.index();
        titleIdx = index.forNodes("article");
        meshIdx = index.forNodes("meshName");
        dateIdx = index.forRelationships("dates");
        Node nd = null;
        IndexHits<Node> nodes = titleIdx.get("article", PMID);
        if (nodes.hasNext()) {
            nd = nodes.next();
//            System.out.println(nd.getProperty("article") + "\t" + nd.getDegree());
            IndexHits<Relationship> relationships = getRelationships(nd, year);
            HashMap<Node, Double> meshCosineScore = new HashMap<>();
            ArrayList<Double> lineDouble = new ArrayList<>();
            for (Relationship relationship : relationships) {
                Node otherNode = relationship.getOtherNode(nd);
                if (!otherNode.getProperty("meshName").equals(term1) && !otherNode.getProperty("meshName").equals(term2)) {
                    String meshPairOtherTerm = otherNode.getProperty("meshName").toString();
                    int yearArticle = (int) relationship.getProperty("Date");
                    updateNodeYearMap(otherNode, yearArticle);
                    double cosineSimOne = operation.getSimilarity(term1, meshPairOtherTerm, yearArticle, yearArticle) + 1;
                    double cosineSimTwo = operation.getSimilarity(term2, meshPairOtherTerm, yearArticle, yearArticle) + 1;
                    double finalCosineScore = (cosineSimOne + cosineSimTwo) / 2;
                    lineDouble.add(finalCosineScore);
                    meshCosineScore.put(otherNode, finalCosineScore);
                }
            }
            Collections.sort(lineDouble, Collections.reverseOrder());
            double diffCutOff = helper.calculateCutOffAverageElbowMethod(lineDouble);
//            System.out.println("diffCutOff " + diffCutOff);
            double cutOff = filterArrayListDiffCutOff(lineDouble, diffCutOff);
//            System.out.println("cutoff: " + cutOff);
            for (Node mesh : meshCosineScore.keySet()) {
                double cosineVal = meshCosineScore.get(mesh);
                mesh.setProperty("FIRST_PMID", PMID);
                if (cosineVal >= 0) { //NOTE: cutoff replaced with 0
                    neighbors.put(mesh, cosineVal);
                } else {
                    falsePositiveNeighbors.put(mesh, cosineVal);
                }
            }

        }
        

        return neighbors;
    }

    public static IndexHits<Relationship> getRelationships(Node node, int year) {
        NumericRangeQuery<Integer> pageQueryRange = NumericRangeQuery.newIntRange("year-numeric", null, year, true, true);
        IndexHits<Relationship> hits;
        if (node.hasLabel(Label.label("article"))) {
            hits = dateIdx.query(pageQueryRange, null, node);
        } else {
            hits = dateIdx.query(pageQueryRange, node, null);
        }

        if (hits == null) {
            return null;
        }
        if (!hits.iterator().hasNext()) {
            return null;
        }
        return hits;
    }

    public static void printTruePositiveNeighbors(Map<Node, Double> neighbors, String inputTerm1, String inputTerm2, String year) {
        String trainingSetFile = truePositiveTermsPath;
        BufferedWriter bw = null;
        File file = new File(trainingSetFile);
        try {
            if (file.exists()) {
                file.delete();
                file.createNewFile();
            } else {
                file.createNewFile();
            }

            bw = new BufferedWriter(new FileWriter(file, true));
            for (Node nd : neighbors.keySet()) {
                bw.write(nd.getProperty("meshName") + "\t" + neighbors.get(nd) + "\t" + year + "\t" +nd.getProperty("FIRST_PMID"));
                bw.newLine();
            }
        } catch (IOException ex) {
            Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                bw.close();
            } catch (IOException ex) {
                Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        boolean trainSet = true;
        matLabFormat_TrainingDataSet_WordVectors(trainingSetFile, trainSet);
//          String trainingSetFileOverWrite = dirMeSHOutputPath + File.separator + "traversal" + File.separator + "matlabTrainingFiles" +File.separator+"trainingTestingSet"+File.separator+"Fish_Oils-Raynaud_Disease_falsePostive.txt";
//          generateTrainingFeatureSetWordVectorsFile(trainingSetFileOverWrite, trainSet);
//          generateTrainingFeatureSetWordVectorsFile(trainingSetFile);
//          generateTrainingFeatureSetWordVectorsAndMeshTreeCodeFile(trainingSetFile);
//          generateTrainingFeatureSetWordVectors_MeshTreeCode_SemanticTypeFile(trainingSetFileOverWrite);
//          generateTrainingFeatureSetCosineScoreFile(trainingSetFileOverWrite);
    }
    public static void matLabFormat_TrainingDataSet_WordVectors(String trainingSetFile, boolean trainSet) {
        BufferedWriter bw = null;
        BufferedReader br = null;
        File file = null;
        try {
            br = new BufferedReader(new FileReader(trainingSetFile));
            try {
                file = new File(trainingSetFile.substring(0, trainingSetFile.length() - 4) + "PrTools_WordVec_Features.txt");
                if (file.exists()) {
                    file.delete();
                    file.createNewFile();
                } else {
                    file.createNewFile();
                }
            } catch (IOException ex) {
                Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
            }
            String line = "";
            try {
                bw = new BufferedWriter(new FileWriter(file, true));
                while ((line = br.readLine()) != null) {
                    String splits[] = line.split("\t");
                    String term = splits[0];
                    String yearLatest = splits[2];
                    BufferedReader brWordVecFile = new BufferedReader(new FileReader(wordVectorBaseDir + File.separator + yearLatest + File.separator + term));
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
                }
            } catch (IOException ex) {
                Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    bw.close();
                } catch (IOException ex) {
                    Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void printFalsePositiveNeighbors(String inputTerm1, String inputTerm2, String year) {
        String trainingSetFile = falsePositiveTermsPath;
        BufferedWriter bw = null;
        File file = new File(trainingSetFile);
        try {
            if (file.exists()) {
                file.delete();
                file.createNewFile();
            } else {
                file.createNewFile();
            }

            bw = new BufferedWriter(new FileWriter(file, true));
            for (Node nd : falsePositiveNeighbors.keySet()) {
                bw.write(nd.getProperty("meshName") + "\t" + falsePositiveNeighbors.get(nd) + "\t" + year +"\t" + nd.getProperty("FIRST_PMID"));
                bw.newLine();
            }
        } catch (IOException ex) {
            Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                bw.close();
            } catch (IOException ex) {
                Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        matLabFormat_TrainingDataSet_WordVectors(trainingSetFile, true);
    }

    public static void insertIntoFinalNeighborsSorted(HashMap<Node, Double> finalSortedNeighbors, HashMap<Node, Double> neighbors) {
        for (Node nd : neighbors.keySet()) {
            Double val = neighbors.get(nd);
            if (finalSortedNeighbors.containsKey(nd)) {
                double tempVal = finalSortedNeighbors.get(nd);
                tempVal = tempVal + val;
                tempVal = tempVal / 2;

                finalSortedNeighbors.put(nd, tempVal);
            } else {
                finalSortedNeighbors.put(nd, val);
            }
        }
    }

    private double pruneFinalSortedNeighbors(double diffCutoff, Map<Node, Double> finalSortedNeighbors) {
        double prevVal = 0;
        double diffCum = 0;
        int count = 0;
        Iterator iter = finalSortedNeighbors.keySet().iterator();
        while (iter.hasNext()) {
            Node nd = (Node) iter.next();
            Double val = finalSortedNeighbors.get(nd);
            if (count >= 1) {

                double diff = prevVal - val;
                if (diff > diffCutoff) {

                } else {
                    return prevVal;
                }
                prevVal = val;
            }
            if (count == 0) {
                prevVal = val;
            }
            count++;
        }
        return prevVal;
    }

    public static double filterArrayListDiffCutOff(ArrayList<Double> lineDouble, double diffCutoff) {
        Iterator iter = lineDouble.iterator();
        int count = 0, diffCount = 0;
        double prevVal = 0;
        double diffCum = 0;
        while (iter.hasNext()) {
            if (count >= 1) {
                double val = (double) iter.next();
                double diff = prevVal - val;
                if (diff > diffCutoff) {
                    return prevVal;
                }
                prevVal = val;
            }
            if (count == 0) {
                prevVal = (double) iter.next();
            }
            count++;
        }
        return prevVal;
    }

    private static void updateNodeYearMap(Node otherNode, int yearArticle) {
        if (otherNodeLatestYear.containsKey(otherNode)) {
            int year = otherNodeLatestYear.get(otherNode);
            if (yearArticle > year) {
                otherNodeLatestYear.put(otherNode, yearArticle);
            } else {
                otherNodeLatestYear.put(otherNode, yearArticle);
            }
        } else {
            otherNodeLatestYear.put(otherNode, yearArticle);
        }
    }

    private void generateTrainingFeatureSetWordVectorsFile(String trainingSetFile, boolean trainSet) {
        BufferedWriter bw = null;
        BufferedReader br = null;
        File file = null;
        try {
            br = new BufferedReader(new FileReader(trainingSetFile));
            try {
                file = new File(trainingSetFile.substring(0, trainingSetFile.length() - 4) + "WordVec_Features.txt");
                if (file.exists()) {
                    file.delete();
                    file.createNewFile();
                } else {
                    file.createNewFile();
                }
            } catch (IOException ex) {
                Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
            }
            String line = "";
            try {
                int counter = 1;
                bw = new BufferedWriter(new FileWriter(file, true));
                int clusterIDCounter = 0;
                while ((line = br.readLine()) != null) {
                    String splits[] = line.split("\t");
                    String term = splits[0];
                    String yearLatest = splits[2];
                    BufferedReader brWordVecFile = new BufferedReader(new FileReader(wordVectorBaseDir + File.separator + yearLatest + File.separator + term));
                    String line1 = null;
                    String dimensionContent = "";
                    while ((line1 = brWordVecFile.readLine()) != null) {
                        String dimensionValue[] = line1.split("\t");
                        int dimensionCounter = 1;
                        for (String val : dimensionValue) {
                            dimensionContent = dimensionContent + dimensionCounter + ":" + val + " ";
                            dimensionCounter++;
                        }
//                        clusterIDCounter = dimensionCounter++;
                    }
//                    int clusterId = getPrimaryClusterId(yearLatest,term);
//                    String finalContent = counter + " " + dimensionContent.trim()+" "+clusterIDCounter+":"+clusterId;
                    if (trainSet == false) {
                        counter = Integer.parseInt("-" + counter);
                    }

                    String finalContent = counter + " " + dimensionContent.trim();
                    bw.write(finalContent);
                    bw.newLine();
                }
            } catch (IOException ex) {
                Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    bw.close();
                } catch (IOException ex) {
                    Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void generateTrainingFeatureSetWordVectorsAndMeshTreeCodeFile(String trainingSetFile) {
        BufferedWriter bw = null;
        BufferedReader br = null;
        File file = null;
        try {
            br = new BufferedReader(new FileReader(trainingSetFile));
            try {
                file = new File(trainingSetFile.substring(0, trainingSetFile.length() - 4) + "_WordVec_MeshCode_features.txt");
                if (file.exists()) {
                    file.delete();
                    file.createNewFile();
                } else {
                    file.createNewFile();
                }
            } catch (IOException ex) {
                Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
            }
            String line = "";
            try {
                int counter = 0;
                bw = new BufferedWriter(new FileWriter(file, true));
                while ((line = br.readLine()) != null) {
                    String splits[] = line.split("\t");
                    String term = splits[0];
                    String termvalue = "", meshTreeCodes = "", semanticTypes = "";
                    if (meshTermTreeCodeAndSemanticTypeMapping.containsKey(term)) {
                        termvalue = meshTermTreeCodeAndSemanticTypeMapping.get(term);
                        String splitTermValue[] = termvalue.split("\t");
                        meshTreeCodes = splitTermValue[0];
                        semanticTypes = splitTermValue[1];
                    }

                    String yearLatest = splits[2];
                    BufferedReader brWordVecFile = new BufferedReader(new FileReader(wordVectorBaseDir + File.separator + yearLatest + File.separator + term));
                    String line1 = null;
                    String dimensionContent = "";
                    int dimensionCounter = 0;
                    while ((line1 = brWordVecFile.readLine()) != null) {
                        String dimensionValue[] = line1.split("\t");
                        for (String val : dimensionValue) {
                            dimensionContent = dimensionContent + dimensionCounter + ":" + val + "\t";
                            dimensionCounter++;
                        }
                    }

                    String splitMeshTreeCodes[] = meshTreeCodes.split(" ");
                    int meshCodeCounter = dimensionCounter++;
                    String meshTreeCodesContent = "";
                    if (splitMeshTreeCodes.length > 0) {
//                        System.out.println(splitMeshTreeCodes.toString());
                        for (String mtreeCode : splitMeshTreeCodes) {
                            meshTreeCodesContent = meshTreeCodesContent + meshCodeCounter + ":" + mtreeCode + "\t";
//                            System.out.println("Mesh Content : "+meshTreeCodesContent);
                            meshCodeCounter++;
                        }
                    }

                    String finalContent = counter + "\t" + dimensionContent.trim();
                    bw.write(finalContent + "\t" + meshTreeCodesContent.trim());
                    bw.newLine();
                    counter++;
                }
            } catch (IOException ex) {
                Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    bw.close();
                } catch (IOException ex) {
                    Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void generateTrainingFeatureSetWordVectors_MeshTreeCode_SemanticTypeFile(String trainingSetFile) {
        BufferedWriter bw = null;
        BufferedReader br = null;
        File file = null;
        try {
            br = new BufferedReader(new FileReader(trainingSetFile));
            try {
                file = new File(trainingSetFile.substring(0, trainingSetFile.length() - 4) + "_WordVec_MeshCode_SemType_features.txt");
                if (file.exists()) {
                    file.delete();
                    file.createNewFile();
                } else {
                    file.createNewFile();
                }
            } catch (IOException ex) {
                Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
            }
            String line = "";
            try {
                int counter = 0;
                bw = new BufferedWriter(new FileWriter(file, true));
                while ((line = br.readLine()) != null) {
                    String splits[] = line.split("\t");
                    String term = splits[0];
                    String termvalue = "", meshTreeCodes = "", semanticTypes = "";
                    if (meshTermTreeCodeAndSemanticTypeMapping.containsKey(term)) {
                        termvalue = meshTermTreeCodeAndSemanticTypeMapping.get(term);
                        String splitTermValue[] = termvalue.split("\t");
                        meshTreeCodes = splitTermValue[0];
                        semanticTypes = splitTermValue[1];
                    }

                    String yearLatest = splits[2];
                    BufferedReader brWordVecFile = new BufferedReader(new FileReader(wordVectorBaseDir + File.separator + yearLatest + File.separator + term));
                    String line1 = null;
                    String dimensionContent = "";
                    int dimensionCounter = 0;
                    while ((line1 = brWordVecFile.readLine()) != null) {
                        String dimensionValue[] = line1.split("\t");
                        for (String val : dimensionValue) {
                            dimensionContent = dimensionContent + dimensionCounter + ":" + val + " ";
                            dimensionCounter++;
                        }
                    }

                    String splitMeshTreeCodes[] = meshTreeCodes.split(" ");
                    int meshCodeCounter = dimensionCounter++;
                    String meshTreeCodesContent = "";
                    if (splitMeshTreeCodes.length > 0) {
                        for (String mtreeCode : splitMeshTreeCodes) {
                            meshTreeCodesContent = meshTreeCodesContent + meshCodeCounter + ":" + mtreeCode + "\t";
//                            System.out.println("Mesh Content : "+meshTreeCodesContent);
                            meshCodeCounter++;
                        }
                    }

                    String splitSemTypes[] = semanticTypes.split(" ");
                    int semTypeCounter = meshCodeCounter++;
                    String semTypeContent = "";
                    if (splitSemTypes.length > 0) {
                        for (String semType : splitSemTypes) {
                            semTypeContent = semTypeContent + semTypeCounter + ":" + semType + " ";
//                            System.out.println("Sem Type Content : "+semTypeContent);
                            semTypeCounter++;
                        }
                    }

                    String finalContent = counter + " " + dimensionContent.trim();
                    bw.write(finalContent + " " + meshTreeCodesContent.trim() + " " + semTypeContent.trim());
                    bw.newLine();
                    counter++;
                }
            } catch (IOException ex) {
                Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    bw.close();
                } catch (IOException ex) {
                    Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void loadmeshTreeCodeAndSemanticType(String dirMeSHOutputPath) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "meshSemanticMapping.txt"));
            String line = null;
            while ((line = br.readLine()) != null) {
                String splits[] = line.split("\t");
                String meshterm = splits[0];
                String meshtermTreeCode = splits[1];
                String semanticTypes = splits[2];
                meshTermTreeCodeAndSemanticTypeMapping.put(meshterm, meshtermTreeCode + "\t" + semanticTypes);
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getPrimaryClusterId(String year, String term) {

        BufferedReader brWordVecFile = null;
        try {
            brWordVecFile = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "TopKCluster" + File.separator + year + File.separator + term));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
        }
        String line1 = null;
        String dimensionContent = "";
        String primaryClusterId = "";

        try {
            while ((line1 = brWordVecFile.readLine()) != null) {
                String dimensionValue[] = line1.split(" ");
                primaryClusterId = dimensionValue[0];
            }
        } catch (IOException ex) {
            Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
        }

        return Integer.parseInt(primaryClusterId);

    }

    private void generateTrainingFeatureSetCosineScoreFile(String trainingSetFile) {
        BufferedWriter bw = null;
        BufferedReader br = null;
        File file = null;
        try {
            br = new BufferedReader(new FileReader(trainingSetFile));
            try {
                file = new File(trainingSetFile.substring(0, trainingSetFile.length() - 4) + "Cosine_Features.txt");
                if (file.exists()) {
                    file.delete();
                    file.createNewFile();
                } else {
                    file.createNewFile();
                }
            } catch (IOException ex) {
                Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
            }
            String line = "";
            try {
                int counter = 0;
                bw = new BufferedWriter(new FileWriter(file, true));
                while ((line = br.readLine()) != null) {
                    String splits[] = line.split("\t");
                    String term = splits[0];
                    String yearLatest = splits[2];
                    String cosineScore = splits[1];
                    BufferedReader brWordVecFile = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "TopKCluster" + File.separator + yearLatest + File.separator + term));
                    String line1 = null;
                    String dimensionContent = "";
                    String primaryClusterId = "";
                    while ((line1 = brWordVecFile.readLine()) != null) {
                        String dimensionValue[] = line1.split(" ");
                        primaryClusterId = dimensionValue[0];
                        int dimensionCounter = 0;
                        for (String val : dimensionValue) {
                            dimensionContent = dimensionContent + dimensionCounter + ":" + val + " ";
                            dimensionCounter++;
                        }
                    }
                    String finalContent = "1" + " " + "1" + ":" + primaryClusterId;
                    bw.write(finalContent);
                    bw.newLine();
                    counter++;
                }
            } catch (IOException ex) {
                Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    bw.close();
                } catch (IOException ex) {
                    Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GraphTrainingSet.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}

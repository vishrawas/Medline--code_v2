/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package openDiscovery;

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
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;
import wordVecSimilarity.WordVectorsOperation;
import generateTrainingSet.GraphTrainingSet;
import static generateTrainingSet.GraphTrainingSet.falsePositiveNeighbors;
import static generateTrainingSet.GraphTrainingSet.helper;
import static generateTrainingSet.GraphTrainingSet.operation;
import static generateTrainingSet.GraphTrainingSet.otherNodeLatestYear;
import static generateTrainingSet.GraphTrainingSet.printFalsePositiveNeighbors;
import static generateTrainingSet.GraphTrainingSet.printTruePositiveNeighbors;
import static generateTrainingSet.GraphTrainingSet.year;
import generateTrainingSet.TrainingSetDriver;
import static generateTrainingSet.TrainingSetDriver.getTopKRankedPairs;
import static generateTrainingSet.TrainingSetDriver.meshIndexMap;
import static java.lang.Double.isNaN;
import java.util.Collections;
import java.util.Iterator;
import org.apache.lucene.search.NumericRangeQuery;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.IndexHits;
/**
 *
 * @author super-machine
 */
public class openDiscovery {

    static Index<Node> titleIdx;
    static GraphDatabaseService graphDb;
    static Index<Node> meshIdx;
    static RelationshipIndex dateIdx;
    static IndexManager index;
    static String dirMeSHOutputPath = "/home/super-machine/Documents/mydrive/myResearch/output";
    static final String DB_PATH = "/home/super-machine/Documents/mydrive/myResearch/output/dummy.db";
    static HashMap<String, ArrayList<String>> meshSemanticTypeHashMap = new HashMap<>();
    static HashMap<String, ArrayList<String>> meshBelongingSemanticTypeHashMap = new HashMap<>();
    static HashMap<String, String> meshClusterIndexFile = new HashMap<>();
    public static TreeMap<Double, Set<String>> topKRanked;
    static GraphTrainingSet graphTrainingSet;
    public static WordVectorsOperation operation;
    public static String wordVectorBaseDir = dirMeSHOutputPath + File.separator + "invertedIndex";
    static TrainingSetDriver traingsetDriver;
    static int topK = 100;
    static String inputTerm1 = "Raynaud Disease";
    static String inputTerm2 = "Fish Oils";
    static int date = 1985;

    public openDiscovery(String dirMeSHOutputPath) {
        connectGraphDatabase();
        loadMeshSemanticTypeAbsMappingFile(dirMeSHOutputPath);
        traingsetDriver = new TrainingSetDriver();
        traingsetDriver.loadMeshIndex(dirMeSHOutputPath + File.separator + "index");
        operation = new WordVectorsOperation(wordVectorBaseDir);   
    }

    public static void main(String args[]) {

        openDiscovery opendiscovery = new openDiscovery(dirMeSHOutputPath);
        ArrayList<String> allTermsinClusterInputTerm = opendiscovery.getTermsForStartTerm(inputTerm1, date);
        
        Set<String> termsSimilar1 = new HashSet<String>();

        for(String term1:allTermsinClusterInputTerm){
            termsSimilar1.add(term1);
        }
        
        ArrayList<String> semanticTypes = meshSemanticTypeHashMap.get(inputTerm2.toLowerCase());
        LinkedHashSet<String> allTermsInCorrespondingSemType = opendiscovery.getTermsofInputSemanticType(inputTerm2, semanticTypes, date);
       
        double sum = 0;
        double count = 0;
        for (String term : allTermsInCorrespondingSemType) {
            if (meshIndexMap.containsKey(term.toLowerCase().trim())) {
                double score = operation.getSimilarity(inputTerm1.toLowerCase(), term.toLowerCase(), date, date) + 1;
                if(isNaN(score)){
                    score = 0.0;
                }
                sum = sum + score;
                count++;
            }
        }

        double avg = sum / count;

        Map<String, Double> filtered_allTermsInCorrespondingSemType = new HashMap<>();
        
        int termCounter = 0;
        for (String term : allTermsInCorrespondingSemType) {
            if (meshIndexMap.containsKey(term.toLowerCase())) {
                double score = operation.getSimilarity(inputTerm1.toLowerCase(), term.toLowerCase(), date, date) + 1;
                if (!isNaN(score) && score > avg) {
                    filtered_allTermsInCorrespondingSemType.put(term, score);
                    termCounter++;
                    if(termCounter==400){
                        break;
                    }
                }
            }
        }

        Map<String, Double> sorted = helper.sortByValue(filtered_allTermsInCorrespondingSemType);
        
        Set<String> termsSimilar2 = new HashSet<String>();
        for (Map.Entry<String, Double> entry : sorted.entrySet()) {
            termsSimilar2.add(entry.getKey().toString());
        }
        
        TreeMap<Double, Set<String>> rankedPairs = getSimilarPairs(termsSimilar1,termsSimilar2,date);
        printSimilarPairs(rankedPairs, inputTerm1, String.valueOf(date));
    }

    public static void generateTrainingOpenDiscovery(int maxDepth, String inputTerm1, String inputTerm2, int year) {

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
                    long PMID = GraphTrainingSet.getFirstPMID(m1, m2, year, maxDepth, isFirst, dirMeSHOutputPath);
                    HashMap<Node, Double> neighbors = getNeighbours(m1, PMID, m2, year);
                    GraphTrainingSet.insertIntoFinalNeighborsSorted(finalSortedNeighbors, neighbors);
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

            for (Node mesh : meshCosineScore.keySet()) {
                double cosineVal = meshCosineScore.get(mesh);
                if (cosineVal >= 0) { //NOTE: cutoff replaced with 0
                    neighbors.put(mesh, cosineVal);
                } else {
                    falsePositiveNeighbors.put(mesh, cosineVal);
                }
            }

        }

        return neighbors;
    }

    private static void printSimilarPairs(TreeMap<Double, Set<String>> topKRanked, String inputTerm1, String date) {
        File file = new File(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "matlabTrainingFiles" + File.separator + "similarPairs" + File.separator + "openDiscovery" + File.separator + date + ":" + "open_discovery" + ":" + inputTerm1.toLowerCase().replace(" ", "_") + ".txt");
        if (file.exists()) {
            file.delete();
            try {
                file.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(TrainingSetDriver.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            try {
                file.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(TrainingSetDriver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(file));
        } catch (IOException ex) {
            Logger.getLogger(TrainingSetDriver.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (Map.Entry<Double, Set<String>> entry : topKRanked.entrySet()) {
            Double key = entry.getKey();
            if (key > 0.0) {
                Set<String> value = entry.getValue();
                String values = "";
                for (String val : value) {
                    System.out.print("\t" + val);
                    values = val + "\t";
                }
                try {
                    bw.write(key + "\t" + values.trim());
                    bw.newLine();
                } catch (IOException ex) {
                    Logger.getLogger(TrainingSetDriver.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }
        try {
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(TrainingSetDriver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static TreeMap<Double, Set<String>> getSimilarPairs(Set<String> allTermsinClusterInputTerm, Set<String> finalTermsInPrimaryClusters, int targetYear) {
        TreeMap<Double, Set<String>> rankedPairs = new TreeMap<>(Comparator.reverseOrder());
        WordVectorsOperation oper = new WordVectorsOperation(dirMeSHOutputPath + File.separator + "invertedIndex");

        for (String term1 : allTermsinClusterInputTerm) {
            for (String term2 : finalTermsInPrimaryClusters) {
                if (meshIndexMap.containsKey(term1) && meshIndexMap.containsKey(term2)) {
                    int yearFirstCoccurrence = TrainingSetDriver.getFirstCooccurrenceYear(term1.toLowerCase(), term2.toLowerCase(), dirMeSHOutputPath);
                    if (yearFirstCoccurrence > 0 && yearFirstCoccurrence < targetYear) {
                        double score = oper.getSimilarity(term1, term2, yearFirstCoccurrence, yearFirstCoccurrence);
                        if (rankedPairs.containsKey(score)) {
                            Set<String> tempVals = rankedPairs.get(score);
                            tempVals.add(term1 + "\t" + term2);
                            rankedPairs.put(score, tempVals);
                        } else {
                            Set<String> tempVals = new HashSet<>();
                            tempVals.add(term1 + "\t" + term2);
                            rankedPairs.put(score, tempVals);
                        }
                    }

                }
            }

        }
        return rankedPairs;
    }


    private LinkedHashSet<String> getTermsofInputSemanticType(String inputTerm1, ArrayList<String> semanticTypes, int date) {
        LinkedHashSet<String> allTerms = new LinkedHashSet<>();

        for (String semType : semanticTypes) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "invertedindexSemanticTypeMeshTerms" + File.separator + semType + ".txt"));
                String line = "";
                try {
                    while ((line = br.readLine()) != null) {
                        allTerms.add(line.toLowerCase().trim());
                    }
                } catch (IOException ex) {
                    Logger.getLogger(openDiscovery.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(openDiscovery.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return allTerms;
    }

    private void loadMeshSemanticTypeAbsMappingFile(String dirMeSHOutputPath) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "meshSemanticAbsMapping.txt"));
            String line = "";

            while ((line = br.readLine()) != null) {
                String splits[] = line.split("\t");
                String semantics = splits[1].trim().toLowerCase();
                String semanticTerms[] = semantics.split("\\$\\$\\$");
                ArrayList semanticTypes = new ArrayList();
                for (String temp : semanticTerms) {
                    if (!temp.trim().toLowerCase().equals("")) {
                        semanticTypes.add(temp.trim().toLowerCase());
                    }
                }
                meshSemanticTypeHashMap.put(splits[0].trim().toLowerCase(), semanticTypes);
            }

            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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

    private ArrayList<String> getTermsForStartTerm(String inputTerm1, int date) {

        ArrayList<String> allTermsForInputTerm = new ArrayList<>();
        BufferedReader br = null;
        String primaryClusterID = "";
        try {
            br = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "TopKCluster" + File.separator + String.valueOf(date) + File.separator + inputTerm1.toLowerCase().trim()));
            String line = "";
            try {
                while ((line = br.readLine()) != null) {
                    String splits[] = line.split(" ");
                    primaryClusterID = splits[0];
                    break;
                }
            } catch (IOException ex) {
                Logger.getLogger(openDiscovery.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(openDiscovery.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            br = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "primaryInvertedClusterIndex" + File.separator + String.valueOf(date) + File.separator + primaryClusterID));
            String line = "";
            try {
                while ((line = br.readLine()) != null) {
                    allTermsForInputTerm.add(line.toLowerCase().trim());
                }
            } catch (IOException ex) {
                Logger.getLogger(openDiscovery.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(openDiscovery.class.getName()).log(Level.SEVERE, null, ex);
        }
        return allTermsForInputTerm;
    }


}

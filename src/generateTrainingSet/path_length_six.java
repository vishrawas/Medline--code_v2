/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package generateTrainingSet;

import static generateTrainingSet.GraphTrainingSet.filterArrayListDiffCutOff;
import static generateTrainingSet.GraphTrainingSet.getFirstPMID;
import static generateTrainingSet.GraphTrainingSet.helper;
import static generateTrainingSet.GraphTrainingSet.otherNodeLatestYear;
import static generateTrainingSet.TrainingSetDriver.indexMeshMap;
import static generateTrainingSet.TrainingSetDriver.meshIndexMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.search.NumericRangeQuery;
import org.neo4j.cypher.internal.compiler.v2_3.pipes.matching.Relationships;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;
import wordVecSimilarity.WordVectorsOperation;

/**
 *
 * @author super-machine
 */
public class path_length_six {

    static Index<Node> titleIdx;
    static GraphDatabaseService graphDb;
    static Index<Node> meshIdx;
    static RelationshipIndex dateIdx;
    static IndexManager index;
    static String dirMeSHOutputPath = "/home/super-machine/Documents/mydrive/myResearch/output";
    static final String DB_PATH = "/home/super-machine/Documents/mydrive/myResearch/output/dummy.db";
    String inputTerm1 = "Raynaud Disease";
    String inputTerm2 = "Fish Oils";
    int date = 1985;
    public static WordVectorsOperation operation;
    public static String wordVectorBaseDir = dirMeSHOutputPath + File.separator + "invertedIndex";
    private static final RelationshipType Occurs = RelationshipType.withName("Occurs");
//    int year1 = 1985;
//    int year2 = 1985;
    public static Map<String, Integer> meshIndexMap = new HashMap<>();
    static Set<String> falsePositivePathLengthSix = new HashSet<>();
    static Set<String> truePositivePathLengthSix = new HashSet<>();
    static String truePositiveSetPath = dirMeSHOutputPath + File.separator + "traversal" + File.separator + "matlabTrainingFiles" + File.separator + "path_length_6" + File.separator + "positive_training_set_path_FO-RD.txt";
    static String falsePositiveSetPath = dirMeSHOutputPath + File.separator + "traversal" + File.separator + "matlabTrainingFiles" + File.separator + "path_length_6" + File.separator + "false_positive_training_set_path_FO-RD.txt";
    static Set<String> truePositives = new HashSet<>();

    public path_length_six() {
        connectGraphDatabase();
        operation = new WordVectorsOperation(wordVectorBaseDir);
        loadMeshIndex(dirMeSHOutputPath + File.separator + "index");
    }

    public static void main(String args[]) {

        String dirIntermediateTermsPathLengthFour = dirMeSHOutputPath + File.separator + "traversal" + File.separator + "matlabTrainingFiles" + File.separator + "path_length_6" + File.separator + "1985:fish oilsraynaud disease.txt";
        path_length_six pathLengthSix = new path_length_six();
        Map<Node, Double> neighbors = new HashMap<>();

        neighbors = pathLengthSix.createTrainingSet(dirIntermediateTermsPathLengthFour);
    }

    private Map<Node, Double> createTrainingSet(String dirIntermediateTermsPathLengthFour) {

        Map<Node, Double> finalneighbors = new HashMap<>();

        try (Transaction tx = graphDb.beginTx()) {
            index = graphDb.index();
            titleIdx = index.forNodes("article");
            meshIdx = index.forNodes("meshName");
            dateIdx = index.forRelationships("dates");
            Node nd = null;
            try {
                BufferedReader br = new BufferedReader(new FileReader(dirIntermediateTermsPathLengthFour));
                String line = "";
                Set<String> meshTermsLevelSixFinalSet = new HashSet<>();

                HashMap<String, Double> meshCosineScore = new HashMap<>();
                HashMap<String, Set<String>> mapIntermediateToSimilarPairTerm = new HashMap<>();
//                HashMap<String, Set<String>> mapIntermediateToSimilarPairTerm = new HashMap<>();

                int year1 = 0;
                int year2 = 1985;
                while ((line = br.readLine()) != null) {
                    System.out.println("Line : " + line);

                    String splits[] = line.split("\t");
                    String term1 = splits[1];
                    String term2 = splits[2];
                    Long PMID = getFirstPMID(term1, term2, date, 1, true, dirMeSHOutputPath);
                    System.out.println("PMID for first pair : " + PMID);
                    IndexHits<Node> nodes = titleIdx.get("article", PMID);

                    if (nodes.hasNext()) {
                        nd = nodes.next();

                        IndexHits<Relationship> relationships = getRelationships(nd, year1, year2);

                        for (Relationship relationship : relationships) {
                            Node otherNode = relationship.getOtherNode(nd);
                            if (!otherNode.getProperty("meshName").equals(term1.toLowerCase().trim()) && !otherNode.getProperty("meshName").equals(term2.toLowerCase().trim())) {
                                String meshPairOtherTerm = otherNode.getProperty("meshName").toString();
                                int yearArticle = (int) relationship.getProperty("Date");
                                double cosineSimOne = operation.getSimilarity(term1.toLowerCase().trim(), meshPairOtherTerm, yearArticle, yearArticle) + 1;
                                double cosineSimTwo = operation.getSimilarity(term2.toLowerCase().trim(), meshPairOtherTerm, yearArticle, yearArticle) + 1;
                                double finalCosineScore = (cosineSimOne + cosineSimTwo) / 2;
                                Set<String> pairs = new HashSet<>();

                                if (meshCosineScore.containsKey(meshPairOtherTerm)) {
                                    double oldCosineValue = meshCosineScore.get(meshPairOtherTerm);
                                    meshCosineScore.put(meshPairOtherTerm, (finalCosineScore + oldCosineValue) / 2);
                                    pairs = mapIntermediateToSimilarPairTerm.get(meshPairOtherTerm);
                                    pairs.add(term1 + "\t" + term2);
                                    mapIntermediateToSimilarPairTerm.put(meshPairOtherTerm, pairs);
                                } else {
                                    meshCosineScore.put(meshPairOtherTerm, finalCosineScore);
                                    pairs.add(term1 + "\t" + term2);
                                    mapIntermediateToSimilarPairTerm.put(meshPairOtherTerm, pairs);
                                }
                            }
                        }
                    }

                }

                getFilteredMeshNodes(meshCosineScore);
                System.out.println("Neighbours of first level (True Positive) Size : " + truePositivePathLengthSix.size());
//                System.out.println("Neighbours of first level  (False Positive): " + falsePositivePathLengthSix);

                truePositives.addAll(truePositivePathLengthSix);
                truePositivePathLengthSix.clear();
                System.out.println(truePositives);
                year1 = year2 - 5;
                HashMap<String, Set<String>> meshParentMapping = new HashMap<>();
                for (String termFirstlevel : truePositives) {
                    Set<Node> articleNodesLevel21 = new HashSet<>();
                    Set<Node> articleNodesLevel22 = new HashSet<>();
                    IndexHits<Node> nodes2 = meshIdx.get("meshName", termFirstlevel);
                    Node meshNode = null;
                    if (nodes2.hasNext()) {
                        meshNode = nodes2.next();
                    }
                    IndexHits<Relationship> Relationships1 = getRelationships(meshNode, year1, year2);
                    if (Relationships1 != null) {
                        for (Relationship relationship1 : Relationships1) {
                            Node articleNode = relationship1.getOtherNode(meshNode);
                            for (Relationship mesh : articleNode.getRelationships(Occurs, Direction.INCOMING)) {
                                Node meshTermNode = mesh.getStartNode();
                                Set<String> startEndPairs = mapIntermediateToSimilarPairTerm.get(termFirstlevel);
                                ArrayList<String> startTerm = new ArrayList<>();
                                ArrayList<String> endTerm = new ArrayList<>();

                                for (String term : startEndPairs) {
                                    String splits[] = term.split("\t");
                                    startTerm.add(splits[0]);
                                    endTerm.add(splits[1]);
                                }

                                if (startTerm.contains(meshTermNode.getProperty("meshName").toString().toLowerCase())) {
                                    articleNodesLevel21.add(articleNode);
                                }

                                if (endTerm.contains(meshTermNode.getProperty("meshName").toString().toLowerCase())) {
                                    articleNodesLevel22.add(articleNode);
                                }
                            }
                        }
                    }

                    if (articleNodesLevel21.size() > 0 && articleNodesLevel22.size() > 0) {
                        Node articleNode21 = selectArticleWithMaximumOverlap(truePositives, articleNodesLevel21);
                        Node articleNode22 = selectArticleWithMaximumOverlap(truePositives, articleNodesLevel22);
                        Set<String> meshTermsLevelSix21 = getMeshTermsInArticle(articleNode21);
                        Set<String> meshTermsLevelSix22 = getMeshTermsInArticle(articleNode22);
                        Set<String> meshTermsLevelSixSet = intersection(meshTermsLevelSix21, meshTermsLevelSix22);
                        for (String term : meshTermsLevelSixSet) {
                            if (!term.equals(termFirstlevel)) {
                                if (meshParentMapping.containsKey(term)) {
                                    Set<String> parents = meshParentMapping.get(term);
                                    parents.add(termFirstlevel);
                                    meshParentMapping.put(term, parents);
                                } else {
                                    Set<String> parents = new HashSet<>();
                                    parents.add(termFirstlevel);
                                    meshParentMapping.put(term, parents);
                                }
                                meshTermsLevelSixFinalSet.add(term);
                            }
                        }
//                        meshTermsLevelSixFinalSet.addAll(meshTermsLevelSixSet);

                        System.out.println("Mesh Terms in final set : " + meshTermsLevelSixFinalSet);
                        System.out.println("Mesh Terms in final set : " + meshTermsLevelSixFinalSet.size());
                    }

                }

                filterFinalMeshTermSet(meshTermsLevelSixFinalSet, meshParentMapping);
                truePositives.addAll(truePositivePathLengthSix);
                System.out.println("FInal Set : " + truePositives);
                System.out.println("FInal Set Size : " + truePositives.size());
                printTrainingSetPathlengthSix(truePositives, truePositiveSetPath);
                printTrainingSetPathlengthSix(falsePositivePathLengthSix, falsePositiveSetPath);

            } catch (FileNotFoundException ex) {
                Logger.getLogger(path_length_six.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(path_length_six.class.getName()).log(Level.SEVERE, null, ex);
            }
            tx.success();
        }
        return finalneighbors;
    }

    private void filterFinalMeshTermSet(Set<String> meshTermsLevelSixSet, HashMap<String, Set<String>> meshParentMapping) {
        Set<String> filteredMeshTerms = new HashSet<>();
        HashMap<String, Double> meshCosineScore1 = new HashMap<String, Double>();

        for (String term : meshParentMapping.keySet()) {
            double finalCosineScore = 0;
            int parentCounter = 0;
            Set<String> parents = meshParentMapping.get(term);
            for (String parent : parents) {
                double cosineSimOne = operation.getSimilarity(parent.toLowerCase().trim(), term.trim().toLowerCase(), date, date) + 1;
//            double cosineSimTwo = operation.getSimilarity(inputTerm2.toLowerCase().trim(), term.trim().toLowerCase(), date, date) + 1;
                finalCosineScore = finalCosineScore + cosineSimOne;
                parentCounter++;
            }  // + cosineSimTwo) / 2;
            finalCosineScore = finalCosineScore / parentCounter;
            meshCosineScore1.put(term.toLowerCase().trim(), finalCosineScore);
        }

        getFinalFilteredMeshNodes(meshCosineScore1);
//        System.out.println("Neighbours of first level: " + filteredMeshTerms);

//        return filteredMeshTerms;
    }

    private Node selectArticleWithMaximumOverlap(Set<String> neighborsFirstLevel, Set<Node> articleNodes) {
        Node neighborsSecondLevel = null;
        Set<String> neighborsFirstLevelTemp = new HashSet<>();
        int min = 0;
        for (Node articleNode : articleNodes) {
            Set<String> terms = getMeshTermsInArticle(articleNode);
            neighborsFirstLevelTemp = intersection(terms, neighborsFirstLevel);
            if (neighborsFirstLevelTemp.size() > min) {
                neighborsSecondLevel = articleNode;
                min = neighborsFirstLevelTemp.size();
            }
        }
        return neighborsSecondLevel;
    }

    public <T> Set<T> intersection(Set<T> list1, Set<T> list2) {
        Set<T> list = new HashSet<T>();

        for (T t : list1) {
            if (list2.contains(t)) {
                list.add(t);
            }
        }

        return list;
    }

    private Set<String> getMeshTermsInArticle(Node articleNode) {
        Set<String> meshTerms = new HashSet<>();
        for (Relationship mesh : articleNode.getRelationships(Occurs, Direction.INCOMING)) {
            Node meshTermNode = mesh.getStartNode();
            String meshName = meshTermNode.getProperty("meshName").toString().toLowerCase().trim();
            if (!inputTerm1.equals(meshName) || !inputTerm2.equals(meshName)) {
                meshTerms.add(meshTermNode.getProperty("meshName").toString().toLowerCase().trim());
            }
        }
        return meshTerms;
    }

    private void printTrainingSetPathlengthSix(Set<String> neighborsPathLengthSix, String trainingSetFilePath) {
        try {
            BufferedWriter bw = null;
            try {
                bw = new BufferedWriter(new FileWriter(trainingSetFilePath, true));
            } catch (IOException ex) {
                Logger.getLogger(TrainingSetDriver.class.getName()).log(Level.SEVERE, null, ex);
            }
            for (String term : neighborsPathLengthSix) {
                try {
                    System.out.println(term);
                    if (!term.toLowerCase().equals(inputTerm1.toLowerCase())) {
                        bw.write(term);
                        bw.newLine();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(path_length_six.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            bw.close();

        } catch (IOException ex) {
            Logger.getLogger(path_length_six.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void getFinalFilteredMeshNodes(HashMap<String, Double> meshCosineScore) {
//        Set<String> neighbors = new HashSet<>();

        Map<String, Double> sorted = helper.sortByValue(meshCosineScore);
        Iterator iter = sorted.keySet().iterator();
        double sum = 0;
        int count = 0;
        while (iter.hasNext()) {
            String nd1 = (String) iter.next();
            double score = sorted.get(nd1);
            sum = sum + score;
            count++;
        }
        double avg = sum / count;
        for (String term : meshCosineScore.keySet()) {
            double cosineVal = meshCosineScore.get(term);
            if (cosineVal >= avg) { //NOTE: cutoff replaced with 0
                truePositivePathLengthSix.add(term);
            } else if (!truePositivePathLengthSix.contains(term)) {
                falsePositivePathLengthSix.add(term);
            }
        }
//        return neighbors;
    }

    private void getFilteredMeshNodes(HashMap<String, Double> meshCosineScore) {

        Map<String, Double> sorted = helper.sortByValue(meshCosineScore);
        Iterator iter = sorted.keySet().iterator();
        double sum = 0;
        int count = 0;
        while (iter.hasNext()) {
            String nd1 = iter.next().toString();
            double score = sorted.get(nd1);
            sum = sum + score;
            count++;
        }
        double avg = sum / count;
        for (String meshTerm : meshCosineScore.keySet()) {
            double cosineVal = meshCosineScore.get(meshTerm);
            if (cosineVal >= avg) { //NOTE: cutoff replaced with 0
                truePositivePathLengthSix.add(meshTerm);
            } else if (!truePositivePathLengthSix.contains(meshTerm)) {
                falsePositivePathLengthSix.add(meshTerm);
            }
        }

    }

    public void loadMeshIndex(String path) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line = "";
            while ((line = br.readLine()) != null) {
                String splits[] = line.split("\t");
                String meshTerm = splits[0];
                Integer index = Integer.parseInt(splits[1]);
                meshIndexMap.put(meshTerm, index);
                indexMeshMap.put(index, meshTerm);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static long getFirstPMID(String term1, String term2, int year, int maxDepth, boolean first, String dirMeSHOutputPath) {

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

    public static IndexHits<Relationship> getRelationships(Node node, int year1, int year2) {
        NumericRangeQuery<Integer> pageQueryRange = NumericRangeQuery.newIntRange("year-numeric", year1, year2, true, true);
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
//        System.out.println("hIts: " + hits.size());
        return hits;
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

package generateTrainingSet;

import wordVecSimilarity.WordVectorsOperation;
import wordVecSimilarity.wordVectorDriver;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by super-machine on 2/6/17.
 */
public class TrainingSetDriver {
    Map<Integer,String>indexMeshMap = new HashMap<>();
    Map<String,Integer>meshIndexMap = new HashMap<>();
    public static void main(String args[]){
        String dirMedlineInputPath = "/Users/super-machine/Documents/Research/medline/data/medline_raw_files";
        String dirMeSHOutputPath = "/Users/super-machine/Documents/Research/medline/output";

        String inputTerm1= "Migraine Disorders";
        String inputTerm2= "Magnesium";
        int year = 1994;
        TrainingSetDriver driver = new TrainingSetDriver();
        driver.generateTrainingSet(dirMedlineInputPath,dirMeSHOutputPath,inputTerm1.toLowerCase().trim(),inputTerm2.toLowerCase().trim(),year);

    }

    private void generateTrainingSet(String dirMedlineInputPath, String dirMeSHOutputPath, String inputTerm1, String inputTerm2,int year) {
        loadMeshIndex(dirMeSHOutputPath+File.separator+"index");
        Set<Integer> clusterIdsTerm1 = getClusterIDs(dirMeSHOutputPath,inputTerm1,year);
        Set<Integer> clusterIdsTerm2 = getClusterIDs(dirMeSHOutputPath,inputTerm2,year);
        Set<String> semanticTypeTerm1 = getTermSemanticTypes(dirMeSHOutputPath,inputTerm1);
        Set<String> semanticTypeTerm2 = getTermSemanticTypes(dirMeSHOutputPath,inputTerm2);
        Set<String>semanticRels =getAllSemanticRels(dirMeSHOutputPath,semanticTypeTerm1,semanticTypeTerm2);
        Set<String>termsSimilar1 = getTermsSimilarTo(clusterIdsTerm1,dirMeSHOutputPath,year);
        Set<String>termsSimilar2 = getTermsSimilarTo(clusterIdsTerm2,dirMeSHOutputPath,year);
        TreeMap<Double,Set<String>> rankedPairs = generateAndRankPairs(inputTerm1,inputTerm2,termsSimilar1,termsSimilar2,semanticRels,dirMeSHOutputPath,year);
        printTreeMapSet(rankedPairs);
    }

    private void printTreeMapSet(TreeMap<Double, Set<String>> rankedPairs) {
        int counter = 0;
        for(Double d:rankedPairs.keySet()){
            for(String meshTerm:rankedPairs.get(d)){
                if(counter<20) {
                    System.out.println(d + "\t" + meshTerm);
                }
            }
            counter++;
            if(counter>=20){
                break;
            }
        }
    }

    private TreeMap<Double,Set<String>> generateAndRankPairs(String ogTerm1,String ogTerm2,Set<String> termsSimilar1, Set<String> termsSimilar2, Set<String> semanticRels, String dirMeSHOutputPath,int targetYear) {
        TreeMap<Double,Set<String>> rankedPairs = new TreeMap<>(Comparator.reverseOrder());
        System.out.println(ogTerm1+"\t"+termsSimilar1.size());
        System.out.println(ogTerm2+"\t"+termsSimilar2.size());

        WordVectorsOperation oper = new WordVectorsOperation(dirMeSHOutputPath + File.separator + "invertedIndex");
        double ogSimilarity = oper.getSimilarity(ogTerm1,ogTerm2,targetYear,targetYear);
        int term1Counter = 0;
        for(String term1:termsSimilar1){
            Set<String>semType1 = getTermSemanticTypes(dirMeSHOutputPath,term1);
            term1Counter++;
                for (String term2 : termsSimilar2) {
                    int yearFirstCoccurrence = getFirstCooccurrenceYear(term1, term2, dirMeSHOutputPath);
                    if (yearFirstCoccurrence > 0 && yearFirstCoccurrence < targetYear) {
                        Set<String> semType2 = getTermSemanticTypes(dirMeSHOutputPath, term2);
                        Set<String> tempSemanticRels = getAllSemanticRels(dirMeSHOutputPath, semType1, semType2);
                        int originalSize = tempSemanticRels.size();
                        tempSemanticRels.retainAll(semanticRels);
                        int newSize = tempSemanticRels.size();
                        double ratio = (double) newSize / originalSize;
                        if (ratio >= 0.5) {
                            double term1Score= oper.getSimilarity(term1,ogTerm1,targetYear,targetYear);
                            double term2Score = oper.getSimilarity(term2,ogTerm2,targetYear,targetYear);
                            double score = oper.getSimilarity(term1, term2, yearFirstCoccurrence, yearFirstCoccurrence);
                            if(Math.abs((ogSimilarity/score)*100 - 100)<10) {
                                score = term1Score + term2Score;
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
        }
        return rankedPairs;

    }

    private int getFirstCooccurrenceYear(String term1, String term2, String dirMeSHOutputPath) {
        int year = -1;
            int index1 = meshIndexMap.get(term1);
            int index2 = meshIndexMap.get(term2);
        try {
            BufferedReader br = new BufferedReader(new FileReader(dirMeSHOutputPath+File.separator+"yearCoccur"+File.separator+index1));
            String line = "";
            while((line=br.readLine())!=null){
            String splits[]=line.split("\t");
            String meshTerm = splits[0].trim();
            String secondTerm = splits[1].trim();

            int temp2 = Integer.parseInt(meshTerm);
             if(index2==temp2){
                String splitsInner[] = secondTerm.split(" ");

                String years= splitsInner[0];
                year = Integer.parseInt(years);
             }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return year;
    }

    private Set<String> getTermsSimilarTo(Set<Integer> clusterIdsTerm, String dirMeSHOutputPath,int year) {
        Set<String>similarMeshTerms = new HashSet<>();
        for(Integer clusterId:clusterIdsTerm){
            Set<String>tempMeshTerms = getAllPrimaryMembers(dirMeSHOutputPath,clusterId,year);
            similarMeshTerms.addAll(tempMeshTerms);
        }
        return similarMeshTerms;
    }

    private Set<String> getAllPrimaryMembers(String dirMeSHOutputPath, Integer clusterId,int year) {
        Set<String>primaryMeshTerms = new HashSet<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(dirMeSHOutputPath+File.separator+"primaryInvertedClusterIndex"+File.separator+year+File.separator+clusterId));
            String line = "";
            while((line=br.readLine())!=null){
                primaryMeshTerms.add(line.trim());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return primaryMeshTerms;
    }

    private Set<String> getAllSemanticRels(String dirMeSHOutputPath, Set<String> semanticTypeTerm1, Set<String> semanticTypeTerm2) {
        HashSet<String>semanticRels = new HashSet<>();
        try {
        for(String semanticTerm1:semanticTypeTerm1){
            for(String semanticTerm2:semanticTypeTerm2){
                if(Files.exists(Paths.get(dirMeSHOutputPath+File.separator+"invertedSemanticAssociation"+File.separator+semanticTerm1+File.separator+semanticTerm2))) {
                    BufferedReader br = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "invertedSemanticAssociation" + File.separator + semanticTerm1 + File.separator + semanticTerm2));
                    String line = "";
                    while ((line = br.readLine()) != null) {
                        semanticRels.add(line);
                    }
                    br.close();
                }
//                if(Files.exists(Paths.get(dirMeSHOutputPath+File.separator+"invertedSemanticAssociation"+File.separator+semanticTerm2+File.separator+semanticTerm1))) {
//                    BufferedReader br = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "invertedSemanticAssociation" + File.separator + semanticTerm2 + File.separator + semanticTerm1));
//                    String line = "";
//                    while ((line = br.readLine()) != null) {
//                        semanticRels.add(line);
//                    }
//                    br.close();
//                }
            }

        }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return semanticRels;
    }

    private Set<String> getTermSemanticTypes(String dirMeSHOutputPath, String inputTerm) {
        HashSet<String> semanticTypes = new HashSet<>();

            if (Files.exists(Paths.get(dirMeSHOutputPath + File.separator + "invertedSemanticMeshIndex" + File.separator + inputTerm))) {
                try {
                BufferedReader br = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "invertedSemanticMeshIndex" + File.separator + inputTerm));
                String line = "";
                while ((line = br.readLine()) != null) {
                    if (!line.equals("null")) {
                        semanticTypes.add(line);
                    }
                }
            } catch(FileNotFoundException e){
                e.printStackTrace();
            } catch(IOException e){
                e.printStackTrace();
            }
        }
        return semanticTypes;
    }

    private Set<Integer> getClusterIDs(String dirMeSHOutputPath, String inputTerm, int year) {
        HashSet<Integer> clusterIds = new HashSet<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(dirMeSHOutputPath+ File.separator+"TopKCluster"+File.separator+year+File.separator+inputTerm));
            String line = "";
            while((line=br.readLine())!=null){
                String splits[] = line.split("\t");
                String clusterIDString = splits[0];
                String clusterIDTokenized[] = clusterIDString.split(" ");
                for(String temp:clusterIDTokenized){
                    clusterIds.add(Integer.parseInt(temp));
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return clusterIds;
    }
    private void loadMeshIndex(String path) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line = "";
            while((line=br.readLine())!=null){
                String splits[] = line.split("\t");
                String meshTerm = splits[0];
                Integer index = Integer.parseInt(splits[1]);
                meshIndexMap.put(meshTerm,index);
                indexMeshMap.put(index,meshTerm);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

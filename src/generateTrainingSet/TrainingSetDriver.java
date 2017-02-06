package generateTrainingSet;

import wordVecSimilarity.WordVectorsOperation;
import wordVecSimilarity.wordVectorDriver;

import java.io.*;
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

        String inputTerm1= "fish oils";
        String inputTerm2= "raynaud disease";
        int year = 1985;
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
        Set<String>termsSimilar1 = getTermsSimilarTo(clusterIdsTerm1,dirMeSHOutputPath);
        Set<String>termsSimilar2 = getTermsSimilarTo(clusterIdsTerm2,dirMeSHOutputPath);
        TreeMap<Double,Set<String>> rankedPairs = generateAndRankPairs(termsSimilar1,termsSimilar2,semanticRels,dirMeSHOutputPath,year);

    }

    private TreeMap<Double,Set<String>> generateAndRankPairs(Set<String> termsSimilar1, Set<String> termsSimilar2, Set<String> semanticRels, String dirMeSHOutputPath,int targetYear) {
        TreeMap<Double,Set<String>> rankedPairs = new TreeMap<>(Comparator.reverseOrder());
        for(String term1:termsSimilar1){
            Set<String>semType1 = getTermSemanticTypes(dirMeSHOutputPath,term1);
            for(String term2:termsSimilar2){
                int yearFirstCoccurrence = getFirstCooccurrenceYear(term1,term2,dirMeSHOutputPath);
                if(yearFirstCoccurrence>0&&yearFirstCoccurrence<targetYear) {
                    Set<String> semType2 = getTermSemanticTypes(dirMeSHOutputPath, term2);
                    Set<String> tempSemanticRels = getAllSemanticRels(dirMeSHOutputPath, semType1, semType2);
                    int originalSize = tempSemanticRels.size();
                    tempSemanticRels.retainAll(semanticRels);
                    int newSize = tempSemanticRels.size();
                    double ratio = (double) newSize / originalSize;
                    if (ratio >= 0.5) {
                        WordVectorsOperation oper = new WordVectorsOperation(dirMeSHOutputPath + File.separator + "invertedIndex");
                        double score = oper.getSimilarity(term1,term2,yearFirstCoccurrence,yearFirstCoccurrence);
                        if(rankedPairs.containsKey(score)){
                            Set<String>tempVals = rankedPairs.get(score);
                            tempVals.add(term1+"\t"+term2);
                            rankedPairs.put(score,tempVals);
                        }else{
                            Set<String>tempVals = new HashSet<>();
                            tempVals.add(term1+"\t"+term2);
                            rankedPairs.put(score,tempVals);
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
            int temp2 = Integer.parseInt(splits[1].trim());
            if(index2==temp2){
                String vals = splits[1];
                String years[] = vals.split(" ");
                year = Integer.parseInt(years[0]);
            }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return year;
    }

    private Set<String> getTermsSimilarTo(Set<Integer> clusterIdsTerm, String dirMeSHOutputPath) {
        Set<String>similarMeshTerms = new HashSet<>();
        for(Integer clusterId:clusterIdsTerm){
            Set<String>tempMeshTerms = getAllPrimaryMembers(dirMeSHOutputPath,clusterId);
            similarMeshTerms.addAll(tempMeshTerms);
        }
        return similarMeshTerms;
    }

    private Set<String> getAllPrimaryMembers(String dirMeSHOutputPath, Integer clusterId) {
        Set<String>primaryMeshTerms = new HashSet<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(dirMeSHOutputPath+File.separator+"primaryInvertedClusterIndex"+clusterId));
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
                BufferedReader br = new BufferedReader(new FileReader(dirMeSHOutputPath+File.separator+"invertedSemanticAssociation"+File.separator+semanticTerm1+File.separator+semanticTerm2));
                String line = "";
                while((line=br.readLine())!=null){
                    semanticRels.add(line);
                }
                br.close();
                 br = new BufferedReader(new FileReader(dirMeSHOutputPath+File.separator+"invertedSemanticAssociation"+File.separator+semanticTerm2+File.separator+semanticTerm1));
                line = "";
                while((line=br.readLine())!=null){
                    semanticRels.add(line);
                }
                br.close();
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
        try {
            BufferedReader br = new BufferedReader(new FileReader(dirMeSHOutputPath+ File.separator+"invertedSemanticMeshIndex"+File.separator+inputTerm));
            String line = "";
            while((line=br.readLine())!=null){
              if(!line.equals("null")){
                  semanticTypes.add(line);
              }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return semanticTypes;
    }

    private Set<Integer> getClusterIDs(String dirMeSHOutputPath, String inputTerm, int year) {
        HashSet<Integer> clusterIds = new HashSet<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(dirMeSHOutputPath+ File.separator+"TopKCluster"+File.separator+year));
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

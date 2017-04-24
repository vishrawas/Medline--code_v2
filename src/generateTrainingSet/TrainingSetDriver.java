package generateTrainingSet;

import wordVecSimilarity.WordVectorsOperation;
import wordVecSimilarity.wordVectorDriver;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by super-machine on 2/6/17.
 */
public class TrainingSetDriver {

    int topK = 100;
    int maxDepth = 1;
    static Map<Integer, String> indexMeshMap = new HashMap<>();
    public static Map<String, Integer> meshIndexMap = new HashMap<>();

    public static void main(String args[]) {
        String dirMedlineInputPath = "/Users/super-machine/Documents/Research/medline/data/medline_raw_files";
        String dirMeSHOutputPath = "/home/super-machine/Documents/mydrive/myResearch/output";
        String inputTerm1 = "";
        String inputTerm2 = "";
        int year = 0;
        TrainingSetDriver driver = new TrainingSetDriver();

        try {
            BufferedReader br = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "goldenTestCases.txt"));
            String inputLine = "";
            while ((inputLine = br.readLine()) != null) {
                String splits[] = inputLine.split("\t");
                year = Integer.parseInt(splits[0]);
                inputTerm1 = splits[1];
                inputTerm2 = splits[2];
                String term1Splits[] = inputTerm1.split("\\$");
                String term2Splits[] = inputTerm2.split("\\$");
                driver.generateTrainingSet(dirMedlineInputPath, dirMeSHOutputPath, term1Splits,term2Splits,inputTerm1.toLowerCase().trim(), inputTerm2.toLowerCase().trim(), year);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TrainingSetDriver.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TrainingSetDriver.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void generateTrainingSet(String dirMedlineInloadMeshIndexputPath, String dirMeSHOutputPath,  String term1Splits[],  String term2Splits[],String inputTerm1, String inputTerm2, int year) {

        loadMeshIndex(dirMeSHOutputPath + File.separator + "index");
        Set<Integer> clusterIdsTerm1 = new HashSet<>();
        Set<Integer> clusterIdsTerm2 = new HashSet<>();
         Set<String> semanticTypeTerm1 = new HashSet<>();
          Set<String> semanticTypeTerm2 = new HashSet<>();
        for(String t1:term1Splits){
        clusterIdsTerm1.addAll(getClusterIDs(dirMeSHOutputPath, t1.toLowerCase().trim(), year));
         semanticTypeTerm1.addAll(getTermSemanticTypes(dirMeSHOutputPath, t1.toLowerCase().trim()));
        }
        for(String t2:term2Splits){
         clusterIdsTerm2.addAll(getClusterIDs(dirMeSHOutputPath, t2.toLowerCase().trim(), year));
         semanticTypeTerm2.addAll(getTermSemanticTypes(dirMeSHOutputPath, t2.toLowerCase().trim()));
        }
      
        
        Set<String> semanticRels = getAllSemanticRels(dirMeSHOutputPath, semanticTypeTerm1, semanticTypeTerm2);
        Set<String> termsSimilar1 = getTermsSimilarTo(clusterIdsTerm1, dirMeSHOutputPath, year);
        Set<String> termsSimilar2 = getTermsSimilarTo(clusterIdsTerm2, dirMeSHOutputPath, year);
        TreeMap<Double, Set<String>> rankedPairs = generateAndRankPairs(term1Splits,term2Splits,inputTerm1, inputTerm2, termsSimilar1, termsSimilar2, semanticRels, dirMeSHOutputPath, year);
        System.out.println("Ranked pairs" + rankedPairs.size());
        TreeMap<Double, Set<String>> topKRanked = getTopKRankedPairs(rankedPairs, topK);

        File file = new File(dirMeSHOutputPath + File.separator+ "traversal" +File.separator + "matlabTrainingFiles" + File.separator + "similarPairs" + File.separator+ year+":"+inputTerm1.toLowerCase().trim()+inputTerm2.toLowerCase().trim()+".txt");
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
            Set<String> value = entry.getValue();
            System.out.print(key);
            String values = "";
            for (String val : value) {
                System.out.print("\t" + val);
                values =  val + "\t";
            }
            try {
                bw.write(key + "\t"+ values.trim());
                bw.newLine();
            } catch (IOException ex) {
                Logger.getLogger(TrainingSetDriver.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println();

        }
        try {
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(TrainingSetDriver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static TreeMap<Double, Set<String>> getTopKRankedPairs(TreeMap<Double, Set<String>> rankedPairs, int k) {
        int counter = 0;
        TreeMap<Double, Set<String>> toreturn = new TreeMap<Double, Set<String>>(Collections.reverseOrder());
        for (Double d : rankedPairs.keySet()) {
            if (counter < k) {
                toreturn.put(d, rankedPairs.get(d));
            } else {
                break;
            }
            counter++;

        }
        return toreturn;
    }

    public static TreeMap<Double, Set<String>> generateAndRankPairs(String term1Splits[],  String term2Splits[],String ogTerm1, String ogTerm2, Set<String> termsSimilar1, Set<String> termsSimilar2, Set<String> semanticRels, String dirMeSHOutputPath, int targetYear) {
        TreeMap<Double, Set<String>> rankedPairs = new TreeMap<>(Comparator.reverseOrder());
//        System.out.println(ogTerm1 + "\t" + termsSimilar1.size());
//        System.out.println(ogTerm2 + "\t" + termsSimilar2.size());

        WordVectorsOperation oper = new WordVectorsOperation(dirMeSHOutputPath + File.separator + "invertedIndex");
        double ogSimilarity = oper.getSimilaritySet(term1Splits,term2Splits,ogTerm1, ogTerm2, targetYear, targetYear);
        int term1Counter = 0;
        System.out.println(termsSimilar1.size()+"\t"+termsSimilar2.size());
        int counter=1;
        for (String term1 : termsSimilar1) {
            Set<String> semType1 = getTermSemanticTypes(dirMeSHOutputPath, term1);
            System.out.println(""+term1Counter++);
            for (String term2 : termsSimilar2) {
                int yearFirstCoccurrence = getFirstCooccurrenceYear(term1, term2, dirMeSHOutputPath);
                if (yearFirstCoccurrence > 0 && yearFirstCoccurrence < targetYear) {
                    Set<String> semType2 = getTermSemanticTypes(dirMeSHOutputPath, term2);
                    Set<String> tempSemanticRels = getAllSemanticRels(dirMeSHOutputPath, semType1, semType2);

                    tempSemanticRels.retainAll(semanticRels);
                    int newSize = tempSemanticRels.size();
                    double ratio = (double) newSize / semanticRels.size();
                    if (ratio >= 0.5) {
                        String temp[] = new String[1];
                        temp[0]=term1.toLowerCase();
                        double term1Score = oper.getSimilaritySet(temp,term1Splits,term1, ogTerm1, targetYear, targetYear);
                        
                        temp[0] = term2;
                        double term2Score = oper.getSimilaritySet(temp,term2Splits,term2, ogTerm2, targetYear, targetYear);
                        double score = oper.getSimilarity(term1, term2, yearFirstCoccurrence, yearFirstCoccurrence);
                        if (score >= ogSimilarity) {
                            score = score + term1Score + term2Score + ratio;
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

    public static int getFirstCooccurrenceYear(String term1, String term2, String dirMeSHOutputPath) {
//        System.out.println(term1+term2);
        int year = -1;
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

                    String years = splitsInner[0];
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

    private Set<String> getTermsSimilarTo(Set<Integer> clusterIdsTerm, String dirMeSHOutputPath, int year) {
        Set<String> similarMeshTerms = new HashSet<>();
        for (Integer clusterId : clusterIdsTerm) {
            Set<String> tempMeshTerms = getAllPrimaryMembers(dirMeSHOutputPath, clusterId, year);
            similarMeshTerms.addAll(tempMeshTerms);
        }
        return similarMeshTerms;
    }

    private Set<String> getAllPrimaryMembers(String dirMeSHOutputPath, Integer clusterId, int year) {
        Set<String> primaryMeshTerms = new HashSet<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "primaryInvertedClusterIndex" + File.separator + year + File.separator + clusterId));
            String line = "";
            while ((line = br.readLine()) != null) {
                primaryMeshTerms.add(line.trim());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return primaryMeshTerms;
    }

    public static Set<String> getAllSemanticRels(String dirMeSHOutputPath, Set<String> semanticTypeTerm1, Set<String> semanticTypeTerm2) {
        HashSet<String> semanticRels = new HashSet<>();
        try {
            for (String semanticTerm1 : semanticTypeTerm1) {
                for (String semanticTerm2 : semanticTypeTerm2) {
                    if (Files.exists(Paths.get(dirMeSHOutputPath + File.separator + "invertedSemanticAssociation" + File.separator + semanticTerm1 + File.separator + semanticTerm2))) {
                        BufferedReader br = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "invertedSemanticAssociation" + File.separator + semanticTerm1 + File.separator + semanticTerm2));
                        String line = "";
                        while ((line = br.readLine()) != null) {
                            semanticRels.add(line);
                        }
                        br.close();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return semanticRels;
    }

    public static Set<String> getTermSemanticTypes(String dirMeSHOutputPath, String inputTerm) {
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
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return semanticTypes;
    }

    private Set<Integer> getClusterIDs(String dirMeSHOutputPath, String inputTerm, int year) {
        HashSet<Integer> clusterIds = new HashSet<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "TopKCluster" + File.separator + year + File.separator + inputTerm));
            String line = "";
            while ((line = br.readLine()) != null) {
                String splits[] = line.split("\t");
                String clusterIDString = splits[0];
                String clusterIDTokenized[] = clusterIDString.split(" ");
                for (String temp : clusterIDTokenized) {
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

}

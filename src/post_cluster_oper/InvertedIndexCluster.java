package post_cluster_oper;

import MeSH_Vector.helperClass;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by super-machine on 2/4/17.
 */
public class InvertedIndexCluster {

    HashMap<Integer,String>indexMeshMap = new HashMap<>();
    HashMap<String,Integer>meshIndexMap = new HashMap<>();
    helperClass helper = new helperClass();

    static HashMap<Integer,Set<String>>invertedIndexPrimary=null;
    static HashMap<Integer,Set<String>>invertedIndexAll=null;
    String primaryInvertedPath ;
    String invertedPath ;
    public void inverted(String dirMedlineInputPath, String dirMeSHOutputPath,int topK) {

        primaryInvertedPath = dirMeSHOutputPath+File.separator+"primaryInvertedClusterIndex" ;
        invertedPath = dirMeSHOutputPath+File.separator+"invertedClusterIndex";
        loadMeshIndex(dirMeSHOutputPath+File.separator+"index");

        helper.deleteIfExistsCreateNewFolder(dirMeSHOutputPath+File.separator+"TopKCluster");
        helper.deleteIfExistsCreateNewFolder(primaryInvertedPath);
        helper.deleteIfExistsCreateNewFolder(invertedPath);
        try(Stream<Path> paths = Files.walk(Paths.get(dirMeSHOutputPath+ File.separator+"cluster"))) {
            paths.forEach(filePath -> {
                invertedIndexPrimary  = new HashMap<>();
                invertedIndexAll = new HashMap<>();
                if (Files.isRegularFile(filePath)) {
                    System.out.println(filePath);
                    ArrayList<ArrayList<String>>fileContents = helper.readDelimiterSeparatedFile(filePath.toString(),",");
                    ArrayList<LinkedHashMap<Integer,Double>>topKClusters = extractTopKCluster(fileContents,topK,dirMeSHOutputPath);

                    for(Integer index:invertedIndexPrimary.keySet()){
                        writeInvertedIndexCluster(index,invertedIndexPrimary.get(index),primaryInvertedPath);
                    }

                    for(Integer index:invertedIndexAll.keySet()){
                        writeInvertedIndexCluster(index,invertedIndexAll.get(index),invertedPath);
                    }


                    writeTopKClusters(topKClusters,dirMeSHOutputPath,filePath.getFileName().toString());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeTopKClusters(ArrayList<LinkedHashMap<Integer, Double>> topKClusters, String dirMeSHOutputPath,String fileName) {
        try {

            helper.deleteIfExistsCreateNewFolder(dirMeSHOutputPath+File.separator+"TopKCluster"+File.separator+fileName);

            System.out.println("writing topK cluster to "+dirMeSHOutputPath+File.separator+"TopKCluster"+File.separator+fileName);
            StringBuilder builder = new StringBuilder();
            int lineNumber = 1;
            for(LinkedHashMap<Integer,Double>vals:topKClusters){
                 builder.setLength(0);
                String meshTerm = indexMeshMap.get(lineNumber);
                BufferedWriter bw = new BufferedWriter(new FileWriter(dirMeSHOutputPath+File.separator+"TopKCluster"+File.separator+fileName+File.separator+meshTerm,true));
                for(Integer clusterId:vals.keySet()){
                    builder.append(clusterId).append(" ");
                }
                builder.append("\t");
                for(Double prob:vals.values()){
                    builder.append(prob).append(" ");
                }
                builder.append("\n");
                bw.write(builder.toString());
                bw.close();
                lineNumber++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

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

    private ArrayList<LinkedHashMap<Integer,Double>> extractTopKCluster(ArrayList<ArrayList<String>> fileContents, int topK,String dirMeSHOutputPath) {
        ArrayList< LinkedHashMap<Integer,Double> >topKClusters = new ArrayList<>();

        int lineCounter = 1;
        for(ArrayList<String>line :fileContents){
            TreeMap<Double,LinkedHashSet<Integer>> lineDouble = discretizeBasedOnVal(line);
           LinkedHashMap<Integer,Double> topKCluster = getTopK(lineDouble,topK,lineCounter,dirMeSHOutputPath);
            topKClusters.add(topKCluster);
            topKClusters.add(topKCluster);
            lineCounter++;
        }
        return topKClusters;

    }



    private LinkedHashMap<Integer,Double> getTopK(TreeMap<Double, LinkedHashSet<Integer>> lineDouble, int topK,int lineCounter,String dirMeSHOutputPath) {
        LinkedHashMap<Integer,Double> toReturn = new LinkedHashMap<>();
        int counter =0;

        double averageVal=0;
        double totalVal= 0;
        for(Double val:lineDouble.keySet()) {
            if(counter>0) {
                totalVal += val;
            }
            counter++;
        }
        averageVal = totalVal/counter;
        counter = 0;
        for (Double val:lineDouble.keySet()){
            if(counter==0)
            {
                Set<Integer>indices = lineDouble.get(val);
                for(int index:indices){
                    toReturn.put(index,val);
                    if(invertedIndexPrimary.containsKey(index)){
                        Set<String>meshterms = invertedIndexPrimary.get(index);
                        meshterms.add(indexMeshMap.get(lineCounter));
                        invertedIndexPrimary.put(index,meshterms);
                    }
                    else{
                        Set<String>meshterms = new HashSet<>();
                        meshterms.add(indexMeshMap.get(lineCounter));
                        invertedIndexPrimary.put(index,meshterms);
                    }

                    if(invertedIndexAll.containsKey(index)){
                        Set<String>meshterms = invertedIndexAll.get(index);
                        meshterms.add(indexMeshMap.get(lineCounter));
                        invertedIndexAll.put(index,meshterms);
                    }
                    else{
                        Set<String>meshterms = new HashSet<>();
                        meshterms.add(indexMeshMap.get(lineCounter));
                        invertedIndexAll.put(index,meshterms);
                    }

                }

                counter++;
            }
            else{
                if(val>averageVal&&counter<topK){
                    Set<Integer>indices = lineDouble.get(val);
                    for(int index:indices){
                        toReturn.put(index,val);
                        if(invertedIndexAll.containsKey(index)){
                            Set<String>meshterms = invertedIndexAll.get(index);
                            meshterms.add(indexMeshMap.get(lineCounter));
                            invertedIndexAll.put(index,meshterms);
                        }
                        else{
                            Set<String>meshterms = new HashSet<>();
                            meshterms.add(indexMeshMap.get(lineCounter));
                            invertedIndexAll.put(index,meshterms);
                        }
                    }
                    counter++;
                }
                else{
                    break;
                }
            }
        }
        return toReturn;
    }

    private void writeInvertedIndexCluster(int index, Set<String> meshTerms, String path) {
        try {
        BufferedWriter bw = new BufferedWriter(new FileWriter(path+File.separator+index,true));
        for(String meshTerm:meshTerms){


                bw.append(meshTerm+"\n");

            }
        bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TreeMap<Double,LinkedHashSet<Integer>>  discretizeBasedOnVal(ArrayList<String> line) {
        TreeMap<Double,LinkedHashSet<Integer>> toReturn = new TreeMap<>(Collections.reverseOrder());
        int counter = 0;
        for(String s:line){
            Double sDouble = Double.parseDouble(s);
            if(toReturn.containsKey(sDouble)){
                LinkedHashSet<Integer>indices = toReturn.get(sDouble);
                indices.add(counter);
                toReturn.put(sDouble,indices);
            }else{
                LinkedHashSet<Integer>indices = new LinkedHashSet<>();
                indices.add(counter);
                toReturn.put(sDouble,indices);
            }
            counter++;
        }
        return toReturn;
    }
}

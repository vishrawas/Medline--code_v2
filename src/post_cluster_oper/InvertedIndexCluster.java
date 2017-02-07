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

                if (Files.isRegularFile(filePath)) {
                    System.out.println(filePath);
                    ArrayList<ArrayList<String>>fileContents = helper.readDelimiterSeparatedFile(filePath.toString(),",");
                    ArrayList<LinkedHashMap<Integer,Double>>topKClusters = extractTopKCluster(fileContents,topK,dirMeSHOutputPath);

                    for(Integer index:invertedIndexPrimary.keySet()){
                        writeInvertedIndexCluster(index,invertedIndexPrimary.get(index),primaryInvertedPath,filePath.getFileName().toString());
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
            int lineNumber = 1;
            for(LinkedHashMap<Integer,Double>vals:topKClusters){
                String meshTerm = indexMeshMap.get(lineNumber);
                BufferedWriter bw = new BufferedWriter(new FileWriter(dirMeSHOutputPath+File.separator+"TopKCluster"+File.separator+fileName+File.separator+meshTerm,true));
                String key = "";
                String val = "";
                for(Map.Entry<Integer, Double> entry : vals.entrySet()){
                    key = key+" "+entry.getKey();
                }
                for(Map.Entry<Integer, Double> entry : vals.entrySet()){
                    val = val+" "+entry.getValue();
                }
                key=key+"\t"+val;
                key=key.trim();
                key=key+"\n";
                bw.write(key);
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
           LinkedHashMap<Integer,Double> topKCluster = getTopKRev(lineDouble,topK,lineCounter,dirMeSHOutputPath);
            topKClusters.add(topKCluster);
            lineCounter++;
        }
        return topKClusters;

    }


    private LinkedHashMap<Integer,Double> getTopKRev(TreeMap<Double,LinkedHashSet<Integer>>lineDouble,int topK, int lineCounter, String dirMeSHOutputPath){
        LinkedHashMap<Integer,Double> toReturn = new LinkedHashMap<>();

        double cutoff = calculateCutOffAverage(lineDouble);
        TreeMap<Double,LinkedHashSet<Integer>>topKSelected = filter(lineDouble,topK,cutoff);
        Iterator iter = topKSelected.keySet().iterator();

        int counter =0;
        while(iter.hasNext()){
            double val = (double) iter.next();
            LinkedHashSet<Integer>clusterIds = topKSelected.get(val);
            for(Integer clustId:clusterIds){
                toReturn.put(clustId,val);
                if(counter==0) {
                    if (invertedIndexPrimary.containsKey(clustId)) {
                        Set<String> meshterms = invertedIndexPrimary.get(clustId);
                        meshterms.add(indexMeshMap.get(lineCounter));
                        invertedIndexPrimary.put(clustId, meshterms);
                    } else {
                        Set<String> meshterms = new HashSet<>();
                        meshterms.add(indexMeshMap.get(lineCounter));
                        invertedIndexPrimary.put(clustId, meshterms);
                    }
                }
            }
            counter++;
        }
        return toReturn;
    }

    private TreeMap<Double,LinkedHashSet<Integer>> filter(TreeMap<Double, LinkedHashSet<Integer>> lineDouble, int topK, double cutoff) {
        TreeMap<Double,LinkedHashSet<Integer>> toReturn = new TreeMap<>(Collections.reverseOrder());
        Iterator iter = lineDouble.keySet().iterator();
        int count = 0;
        double prevVal  = 0;
        int topKCounter = 0;
        while (iter.hasNext()){
            Double val = (Double) iter.next();
            LinkedHashSet<Integer> clusterIds = lineDouble.get(val);
            if(count==0){
                toReturn.put(val,clusterIds);
                topKCounter++;
            }
            if(count>1){
                if((prevVal-val)<cutoff&&topKCounter<topK){
                    prevVal=val;
                    toReturn.put(val,clusterIds);
                    topKCounter++;
                }
                else{
                    break;
                }
            }
            if(count==1){
                prevVal = val;
                toReturn.put(val,clusterIds);
                topKCounter++;
            }
            count++;
        }
        return toReturn;
    }

    private double calculateCutOffAverage(TreeMap<Double, LinkedHashSet<Integer>> lineDouble) {
        double cutoff=0;
        Iterator iter = lineDouble.keySet().iterator();
        int count = 0,diffCount = 0;
        double prevVal = 0;
        double diffCum = 0;
        while(iter.hasNext()){
            if(count>1){
                double val = (double)iter.next();
                diffCum = prevVal-val;
                diffCount++;
            }
            if(count==1){
                prevVal = (double) iter.next();
            }
            count++;
        }
        cutoff = diffCum/diffCount;
        return cutoff;
    }


    private void writeInvertedIndexCluster(int index, Set<String> meshTerms, String path,String fileName) {
        try {
            File file = new File(path+File.separator+fileName);
            if(!file.isDirectory()){
                file.mkdir();
            }
        BufferedWriter bw = new BufferedWriter(new FileWriter(path+File.separator+fileName+File.separator+index,true));
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

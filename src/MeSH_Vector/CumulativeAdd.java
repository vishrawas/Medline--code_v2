package MeSH_Vector;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
/**
 * Created by super-machine on 12/8/16.
 */
public class CumulativeAdd {
    public void cooccur(String inputPath, String dirMeSHOutputPath, TreeSet<Integer> yearSet, int minYear, int maxYear, int startPoint) {

        int fileCounter= 0;
        helperClass helper = new helperClass();
        Path opPath = Paths.get(dirMeSHOutputPath);
        if (Files.exists(opPath)) {
            try {
                System.out.println("Deleting previous instance of "+dirMeSHOutputPath);
                helper.deleteFolder(new File(dirMeSHOutputPath));
                Files.createDirectories(opPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            Files.createDirectories(opPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        HashMap<String, HashMap<String,Double>>previousYearHashMap = new HashMap<>();
        Iterator iter = yearSet.iterator();
        while(iter.hasNext()){
            int year = (int) iter.next();
            String input = inputPath+File.separator+year;
            String output = dirMeSHOutputPath+File.separator+year;
            Path ip = Paths.get(input);
            if(fileCounter==0){

                    helper.loadFileIntoHashMap(previousYearHashMap,ip);
                    computeDiagonalElements(previousYearHashMap);
                    helper.writeHashMap(previousYearHashMap,output);


            }
            else{
                    helper.appendFileIntoHashMap(previousYearHashMap,ip);
                    computeDiagonalElements(previousYearHashMap);
                    helper.writeHashMap(previousYearHashMap,output);
            }
            fileCounter++;
        }
    }

    private void computeDiagonalElements(HashMap<String, HashMap<String, Double>> previousYearHashMap) {
        double totalD = 0;
        for(String oKey:previousYearHashMap.keySet()){
            if(!oKey.equals("$$TOTAL$$")) {
                HashMap<String, Double> innerHashMap = previousYearHashMap.get(oKey);
                double total = 0;
                for (String ikey : innerHashMap.keySet()) {
                    if (!oKey.equals(ikey)) {
                        total = total + innerHashMap.get(ikey);
                    }
                }
                innerHashMap.put(oKey, total);
                totalD = totalD + total;
                previousYearHashMap.put(oKey, innerHashMap);
            }
        }
        HashMap<String,Double>temp = new HashMap<>();
        temp.put("$$TOTAL$$",totalD);
        previousYearHashMap.put("$$TOTAL$$",temp);
    }


}

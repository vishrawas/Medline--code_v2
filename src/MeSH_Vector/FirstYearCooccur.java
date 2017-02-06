package MeSH_Vector;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.TreeSet;

/**
 * Created by super-machine on 12/24/16.
 */
public class FirstYearCooccur {
    public void cooccur(String inputPath, String dirMeSHOutputPath, TreeSet<Integer> yearSet, int minYear, int maxYear, int startPoint) {

        helperClass helper = new helperClass();
        HashMap<String,Integer> meshIndex = helper.getIndex(dirMeSHOutputPath+File.separator+"index");
        Path opPath = Paths.get(dirMeSHOutputPath+File.separator+"yearCoccur");
        if (Files.exists(opPath)) {

                System.out.println("Deleting previous instance of "+opPath.toString());
                helper.deleteFolder(new File(opPath.toString()));

        }
        try {
            Files.createDirectories(opPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        HashMap<Integer, HashMap<Integer,String>>allMeshCoccur = new HashMap<>();
        for(int year:yearSet){
            try {
                System.out.println("Year --> "+year);
                Scanner sc =new Scanner(new File(inputPath+File.separator+year));

                String line = "";
                while(sc.hasNext()){
                    line = sc.nextLine();
                    String meSHTerms_1="", meSHTerms_2="";
                    String PMID = "";
                    int tabCounter=0;
                    while (true) {

                        int indexOfTab = line.indexOf("\t");
                        if (indexOfTab == -1) {
                            if (tabCounter == 2) {
                                meSHTerms_2 = line.substring(0, line.length());
                                insertIntoMeSHCoccur(allMeshCoccur,year,PMID,meshIndex.get(meSHTerms_1),meshIndex.get(meSHTerms_2));
                            }
                            break;
                        }
                        if (indexOfTab != -1) {
                            String subString = line.substring(0, indexOfTab);
                            line = line.substring(indexOfTab + 1, line.length());
                            if(tabCounter==0){
                                PMID = subString;
                            }
                            if (tabCounter == 1) {
                                meSHTerms_1 = subString;
                            }
                            tabCounter++;
                        }
                    }
                }
                sc.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        helper.writeHashMapByKey(allMeshCoccur,opPath);
    }

    private void insertIntoMeSHCoccur(HashMap<Integer, HashMap<Integer, String>> allMeshCoccur, int year, String pmid, int meSHTerms_1, int meSHTerms_2) {
        if(allMeshCoccur.containsKey(meSHTerms_1)){
            HashMap<Integer,String>innerMap=allMeshCoccur.get(meSHTerms_1);
            if(innerMap.containsKey(meSHTerms_2)){

            }
            else{
                innerMap.put(meSHTerms_2,year+" "+pmid);
                allMeshCoccur.put(meSHTerms_1,innerMap);
            }
        }
        else{
            HashMap<Integer,String>innerMap = new HashMap<>();
            innerMap.put(meSHTerms_2,year+" "+pmid);
            allMeshCoccur.put(meSHTerms_1,innerMap);
        }
    }

}

package MeSH_Vector;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Created by super-machine on 12/8/16.
 */
public class splitByDate {
    public void split(String inputPath, String dirMeSHOutputPath) {

        helperClass helper = new helperClass();
        dirMeSHOutputPath=dirMeSHOutputPath+ File.separator+"Splits";
        Path path = Paths.get(dirMeSHOutputPath);
        if(!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
        String line = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(inputPath));
            int lineCounter = 0;
            while ((line = br.readLine()) != null) {
                String meSHTerms = "";
                lineCounter++;
                if(lineCounter%10000==0){
                    System.out.println(lineCounter);
                }
                int tabCounter = 0;
                String PMID = "";
                while (true) {
                    int indexOfTab = line.indexOf("\t");
                    if (indexOfTab == -1) {
                        if (tabCounter == 3) {
                            String subString = line.substring(0, line.length());
                            int tempyear = Integer.parseInt(subString);
                            ArrayList<String> tempMeshTerms = new ArrayList<>();
                            processStringMeshTermsArrayList(meSHTerms, tempMeshTerms);
                            helper.selfCartesianProductandWrite(PMID,tempMeshTerms,dirMeSHOutputPath+File.separator+tempyear);
                            
                        }
                        break;
                    }
                    if (indexOfTab != -1) {
                        String subString = line.substring(0, indexOfTab);
                        line = line.substring(indexOfTab + 1, line.length());
                        if(tabCounter==0){
                            PMID = subString;
                        }
                        if (tabCounter == 2) {
                            meSHTerms = subString;
                        }
                        tabCounter++;
                    }
                }
            }
            br.close();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

    }


    private void processStringMeshTermsArrayList(String meSHTerms, ArrayList<String> tempMeshTerms) {
        if (!meSHTerms.isEmpty()) {
            helperClass helper = new helperClass();
            helper.extractMeshTermsFromStringArrayList(meSHTerms, tempMeshTerms);
        }
    }


}

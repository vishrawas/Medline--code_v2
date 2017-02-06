package MeSH_Vector;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.TreeSet;

/**
 * Created by super-machine on 12/28/16.
 */
public class invertedIndex {
    public void createInvertedIndex(String inputPath, String dirMeSHOutputPath, TreeSet<Integer> yearSet, int minYear, int maxYear, int startPoint) {
        helperClass helper = new helperClass();
        Path opPath = Paths.get(dirMeSHOutputPath+File.separator+"invertedIndex");
        if (Files.exists(opPath)) {

            System.out.println("Deleting previous instance of "+opPath.toString());
            helper.deleteFolder(new File(opPath.toString()));

        }
        try {
            Files.createDirectories(opPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(Integer year:yearSet){
            try {
                BufferedReader br = new BufferedReader(new FileReader(new File(inputPath+ File.separator+year)));
                String line = "";
                while((line=br.readLine())!=null){
                    int index =line.indexOf("\t",0);
                    String meshTerm = "";
                    String vector = "";

                    if(index!=-1){
                        meshTerm = line.substring(0,index);
                        vector = line.substring(index+1,line.length());
                        meshTerm = meshTerm.trim();
                        vector = vector.trim();
                        if (!Files.exists(Paths.get(opPath.toString()+File.separator+year))) {
                            Files.createDirectories(Paths.get(opPath.toString()+File.separator+year));
                        }
                            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(opPath.toString()+File.separator+year+File.separator+meshTerm)));
                        bw.write(vector);
                        bw.close();
                    }
                }
                br.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

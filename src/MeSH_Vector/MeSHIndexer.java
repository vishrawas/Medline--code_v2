package MeSH_Vector;

import java.io.*;
import java.util.HashMap;

/**
 * Created by super-machine on 12/10/16.
 */
public class MeSHIndexer {
    public void index(String inputPath, String dirMeSHOutputPath) {
        HashMap<String,Integer> index = new HashMap<>();
        File dir = new File(inputPath);
        File[] directoryListing = dir.listFiles();
        int indexCounter = 1;
        if (directoryListing != null) {
            for (File child : directoryListing) {
                System.out.println(child);
                try {
                    BufferedReader br = new BufferedReader(new FileReader(child));
                    String line = "";
                    while((line=br.readLine())!=null){
                        String splits[] = line.split("\t");
                        if(!index.containsKey(splits[0].trim())&&!splits[0].trim().equals("$$TOTAL$$")){
                            index.put(splits[0].trim(),indexCounter++);
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        helperClass helper = new helperClass();
        helper.writeIndex(index,dirMeSHOutputPath+File.separator+"index");

    }
}

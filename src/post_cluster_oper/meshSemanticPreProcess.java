package post_cluster_oper;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by super-machine on 2/6/17.
 */
public class meshSemanticPreProcess {
    HashMap<String,String> semanticCodeTermMapping = new HashMap<>();
    HashMap<String,Set<String>>meshSemanticMapping = new HashMap<>();
    public void meshSemanticMappingExpander(String dirMedlineInputPath, String dirMeSHOutputPath ){
        try {
            BufferedReader br = new BufferedReader(new FileReader(dirMeSHOutputPath+ File.separator+"semanticTypeCodes.txt"));
            String line = "";

            while((line=br.readLine())!=null){
                String splits[] = line.split("\\|");
                String code = splits[0];
                String term = splits[1];
                semanticCodeTermMapping.put(code.toLowerCase(),term);
            }

            br = new BufferedReader(new FileReader(dirMeSHOutputPath+File.separator+"meshSemanticMapping.txt"));
            while((line=br.readLine())!=null){
                String splits[] = line.split("\t");
                String meshterm = splits[0];
                String semanticTerms  = splits[2];
                String semanticType[] = semanticTerms.split(" ");
                Set<String>semantics = new HashSet<>();
                for(String s:semanticType){
                    semantics.add(s);
                }
                meshSemanticMapping.put(meshterm,semantics);
            }
            br.close();
            writeMeSHSemanticMappings(dirMeSHOutputPath,meshSemanticMapping);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeMeSHSemanticMappings(String dirMeSHOutputPath, HashMap<String, Set<String>> meshSemanticMapping) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(dirMeSHOutputPath+File.separator+"meshSemanticAbsMapping.txt"));
            StringBuilder builder = new StringBuilder();
            for(String mesh:meshSemanticMapping.keySet()){
                builder.setLength(0);
                builder.append(mesh).append("\t");
                Set<String>semantics = meshSemanticMapping.get(mesh);
                for(String semantic:semantics){
                    builder.append(semanticCodeTermMapping.get(semantic)).append("$$$");
                }
                bw.append(builder.toString().trim());
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

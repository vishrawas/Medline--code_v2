package post_cluster_oper;

import MeSH_Vector.helperClass;

import java.io.*;
import java.nio.file.Files;

/**
 * Created by super-machine on 2/4/17.
 */
public class PostClusterDriver {
    static int topK = 5;
    public static void main(String args[]){
        PostClusterDriver driver = new PostClusterDriver();
        String dirMedlineInputPath = "/Users/super-machine/Documents/Research/medline/data/medline_raw_files";
        String dirMeSHOutputPath = "/Users/super-machine/Documents/Research/medline/output";
//        driver.createInvertedIndex(dirMedlineInputPath,dirMeSHOutputPath);
//        driver.createSemanticMapping(dirMedlineInputPath,dirMeSHOutputPath);
//        driver.createInvertedIndexSemanticType(dirMedlineInputPath,dirMeSHOutputPath);
        driver.createInvertedIndexSemanticAssociation(dirMedlineInputPath,dirMeSHOutputPath);
    }

    private void createInvertedIndexSemanticAssociation(String dirMedlineInputPath, String dirMeSHOutputPath) {
        try {
            helperClass helper = new helperClass();
            BufferedReader br = new BufferedReader(new FileReader("/Users/super-machine/Documents/Research/medline/output/semanticNetworkFile.txt"));
            helper.deleteIfExistsCreateNewFolder(dirMeSHOutputPath+File.separator+"invertedSemanticAssociation");
            String line = "";
            while((line=br.readLine())!=null){
                String splits[] =  line.split("\\|");
                String semTerm1 = splits[0].toLowerCase().trim();
                String rel = splits[1].toLowerCase().trim();
                String semTerm2 = splits[2].toLowerCase().trim();
                File file = new File(dirMeSHOutputPath+File.separator+"invertedSemanticAssociation"+File.separator+semTerm1);
                if(!file.isDirectory()){
                    file.mkdir();
                }
                BufferedWriter bw = new BufferedWriter(new FileWriter(dirMeSHOutputPath+File.separator+"invertedSemanticAssociation"+File.separator+semTerm1+File.separator+semTerm2,true));
                bw.append(rel);
                bw.newLine();
                bw.close();
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createInvertedIndexSemanticType(String dirMedlineInputPath, String dirMeSHOutputPath) {
        try {
            helperClass helper = new helperClass();
            BufferedReader br = new BufferedReader(new FileReader(dirMeSHOutputPath+ File.separator+"meshSemanticAbsMapping.txt"));
            helper.deleteIfExistsCreateNewFolder(dirMeSHOutputPath+File.separator+"invertedSemanticMeshIndex");
            String line = "";
            while((line=br.readLine())!=null){
                String splits[] =  line.split("\t");
                BufferedWriter bw = new BufferedWriter(new FileWriter(dirMeSHOutputPath+File.separator+"invertedSemanticMeshIndex"+File.separator+splits[0].toLowerCase().trim(),true));
                String semantics = splits[1].trim().toLowerCase();
                String semanticTerms[] = semantics.split("\\$\\$\\$");
                for(String temp:semanticTerms){
                    if(!temp.trim().toLowerCase().equals("")){
                        bw.append(temp);
                        bw.newLine();
                    }
                }
                bw.close();
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createSemanticMapping(String dirMedlineInputPath, String dirMeSHOutputPath) {
        meshSemanticPreProcess semanticPreProcess = new meshSemanticPreProcess();
        semanticPreProcess.meshSemanticMappingExpander(dirMedlineInputPath,dirMeSHOutputPath);
    }

    private void createInvertedIndex(String dirMedlineInputPath, String dirMeSHOutputPath) {

        InvertedIndexCluster invertedIndexCluster = new InvertedIndexCluster();
        invertedIndexCluster.inverted(dirMedlineInputPath,dirMeSHOutputPath,topK);
    }
}

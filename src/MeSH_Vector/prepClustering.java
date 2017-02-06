package MeSH_Vector;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by super-machine on 12/31/16.
 */
public class prepClustering {
    public void cluster(String inputPath, String dirMeSHOutputPath, TreeSet<Integer> yearSet, int minYear, int maxYear, int startPoint) {
        helperClass helper = new helperClass();
        HashMap<String,Integer>index = helper.getIndex(inputPath+ File.separator+"index");
        System.out.println("Begin creating pre-corpus");
        createPreCorpusFile(inputPath,dirMeSHOutputPath,yearSet,index);
        System.out.println("Pre-Corpus created successfully");
        System.out.println("Corpus creation begins");
        createCorpusFile(inputPath,dirMeSHOutputPath,yearSet,index);
        System.out.println("Corpus creation completed successfully");
        System.out.println("Begin create word embeddings files for clustering");
        createWordEmbeddingFiles(inputPath,dirMeSHOutputPath,yearSet);
        System.out.println("Compelted creating word embeddings files for clustering");
    }

    private void createCorpusFile(String inputPath, String dirMeSHOutputPath, TreeSet<Integer> yearSet, HashMap<String, Integer> index) {
        String corpusdirMeSHOutputPath = dirMeSHOutputPath+File.separator+"Corpus";
        helperClass helper = new helperClass();
        Path path = Paths.get(corpusdirMeSHOutputPath);
        if (Files.exists(path)) {

            System.out.println("Deleting previous instance of "+path.toString());
            helper.deleteFolder(new File(path.toString()));

        }
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int first = 0;
        int prevYear = 0;
        for(int year:yearSet){

            if(first==0){
                String opFilePath =corpusdirMeSHOutputPath+File.separator+year;
                try {
                    OutputStream out = new FileOutputStream(opFilePath);
                    byte[] buf = new byte[6048];
                    InputStream in = new FileInputStream(dirMeSHOutputPath+File.separator+"Pre-Corpus"+File.separator+year);
                    int b = 0;
                    while ( (b = in.read(buf)) >= 0) {
                        out.write(buf, 0, b);
                        out.flush();
                    }
                    out.close();
                    in.close();
                    first=1;
                    prevYear = year;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else{
                String opFilePath =corpusdirMeSHOutputPath+File.separator+year;

                try {
                    OutputStream out = new FileOutputStream(opFilePath);
                    byte[] buf = new byte[6048];
                    InputStream in1 = new FileInputStream(dirMeSHOutputPath+File.separator+"Corpus"+File.separator+(prevYear));
                    int b = 0;
                    while ( (b = in1.read(buf)) >= 0) {
                        out.write(buf, 0, b);
                        out.flush();
                    }
                    InputStream in2 = new FileInputStream(dirMeSHOutputPath+File.separator+"Pre-Corpus"+File.separator+(year));
                     b = 0;
                    while ( (b = in2.read(buf)) >= 0) {
                        out.write(buf, 0, b);
                        out.flush();
                    }

                    out.close();
                    in1.close();
                    in2.close();
                    prevYear = year;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void createWordEmbeddingFiles(String inputPath, String dirMeSHOutputPath, TreeSet<Integer> yearSet) {
        dirMeSHOutputPath = dirMeSHOutputPath+File.separator+"EmbeddingFiles";
        helperClass helper = new helperClass();
        Path path = Paths.get(dirMeSHOutputPath);
        if (Files.exists(path)) {

            System.out.println("Deleting previous instance of "+path.toString());
            helper.deleteFolder(new File(path.toString()));

        }
        try {
            Files.createDirectories(path
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(int year:yearSet){
            String filePath = inputPath+File.separator+"wordVector"+File.separator+year;
            try {
                BufferedReader br = new BufferedReader(new FileReader(filePath));
                String line = "";

                BufferedWriter bw = new BufferedWriter(new FileWriter(new File(dirMeSHOutputPath+File.separator+year)));
                while((line=br.readLine())!=null){
                    int index =line.indexOf("\t",0);
                    String vector = "";

                    if(index!=-1){
                        vector = line.substring(index+1,line.length()).trim();
                        vector = vector.replaceAll("\t"," ");
                        bw.write(vector);
                        bw.newLine();
                    }
                }
                br.close();
                bw.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createPreCorpusFile(String inputPath, String dirMeSHOutputPath, TreeSet<Integer> yearSet,HashMap<String,Integer>index) {
        String filePath = inputPath+File.separator+"whole.txt";
        dirMeSHOutputPath=dirMeSHOutputPath+ File.separator+"Pre-Corpus";
        helperClass helper = new helperClass();
        Path path = Paths.get(dirMeSHOutputPath);
        if (Files.exists(path)) {

            System.out.println("Deleting previous instance of "+path.toString());
            helper.deleteFolder(new File(path.toString()));

        }
        try {
            Files.createDirectories(path
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String line = "";
            while((line=br.readLine())!=null){
                String meSHTerms = "";
                int tabCounter = 0;
                while (true) {
                    int indexOfTab = line.indexOf("\t");
                    if (indexOfTab == -1) {
                        if (tabCounter == 3) {
                            String subString = line.substring(0, line.length());
                            int tempyear = Integer.parseInt(subString);
                            ArrayList<Integer> tempMeshTerms = new ArrayList<>();
                            processStringMeshTermsArrayList(meSHTerms, tempMeshTerms,index);
                            write(dirMeSHOutputPath+File.separator+tempyear,tempMeshTerms);
                        }
                        break;
                    }
                    if (indexOfTab != -1) {
                        String subString = line.substring(0, indexOfTab);
                        line = line.substring(indexOfTab + 1, line.length());
                        if (tabCounter == 2) {
                            meSHTerms = subString;

                        }
                        tabCounter++;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void write(String outputFile, ArrayList<Integer> tempMeshTerms) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile,true));
            StringBuilder builder = new StringBuilder();
            for(Integer temp:tempMeshTerms){
                builder.append(temp+" ");
            }
            String toWrite = builder.toString().trim();
            bw.append(toWrite);
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processStringMeshTermsArrayList(String meSHTerms, ArrayList<Integer> tempMeshTerms, HashMap<String, Integer> index) {
        if (!meSHTerms.isEmpty()) {
            int start = 0;
            while (true) {
                int found = meSHTerms.indexOf("$$", start);
                if (found != -1) {
                    String meshTerm = meSHTerms.substring(start, found);
                    int meshTermsIndex = index.get(meshTerm)-1;
                    tempMeshTerms.add(meshTermsIndex);
                    start = found + 2;
                }
                if (found == -1) {
                    break;
                }
            }
        }
    }
}

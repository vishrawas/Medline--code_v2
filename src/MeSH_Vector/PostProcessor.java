package MeSH_Vector;

import jeigen.DenseMatrix;
import jeigen.SparseMatrixLil;

import java.io.*;
import java.nio.DoubleBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.TreeSet;
import static jeigen.Shortcuts.*;
/**
 * Created by super-machine on 12/16/16.
 */
public class PostProcessor {
    static int topD = 300;
    public void process(String inputPath, String dirMeSHOutputPath, TreeSet<Integer> yearSet, int minYear, int maxYear, int startPoint) {
        CovertRedSVDFormat convert = new CovertRedSVDFormat();
        String opPathS= dirMeSHOutputPath + File.separator + "wordVector" ;
        Path opPath = Paths.get(opPathS);
        if (!Files.exists(opPath)) {
            try {
                Files.createDirectories(opPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        helperClass helper = new helperClass();
        HashMap<Integer,String> meshIndex = readInvertedMeSHIndex(dirMeSHOutputPath+File.separator+"index");
        for(int year:yearSet) {
            System.out.println("processing -->"+year);
            SparseMatrixLil dm1 = spzeros(topD,topD);
            DenseMatrix dm2 = zeros(meshIndex.keySet().size(),topD);
            readSigmaMatrix(inputPath+File.separator+year+".S",dm1);
            dm1 = dm1.pow(0.5);

            readUMatrix(inputPath+File.separator+year+".U",dm2);
            DenseMatrix wordVectors = dm2.mmul(dm1);
            writeWordVector(wordVectors,meshIndex,opPathS+File.separator+year);
            Driver.updateLogEntry(startPoint + "\t" + minYear + "\t" + year, dirMeSHOutputPath);
        }
    }

    private void writeWordVector(DenseMatrix wordVectors, HashMap<Integer, String> meshIndex, String opPath) {
        helperClass helper = new helperClass();

        for(int i=0;i<wordVectors.rows;i++) {
            DenseMatrix wordVector = wordVectors.row(i);
            String wordVectorString = convertDenseMatrixToString(wordVector);
            String word = meshIndex.get(i+1);
            String toWrite = word +"\t"+wordVectorString+"\n";
            helper.appendStringToFile(toWrite,opPath);
        }
    }

    private String convertDenseMatrixToString(DenseMatrix wordVector) {
        StringBuilder builder =new StringBuilder();
        for(int col=0;col<wordVector.cols;col++){
            builder.append("\t").append(wordVector.get(0,col));
        }
        builder.append("\n");
        return builder.toString().trim();

    }

    private void readUMatrix(String ipPath, DenseMatrix dm2) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(ipPath)));
        String line = "";
        int counter = 0;

            while((line=br.readLine())!=null){

                String splits[] = line.split(" ");
                for(int i=0;i<splits.length;i++){
                    dm2.set(counter,i, Double.parseDouble(splits[i]));
                }
                counter++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readSigmaMatrix(String ipPath, SparseMatrixLil dm1) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(ipPath)));
            String line = "";
            int counter = 0;
            while((line=br.readLine())!=null){
                 dm1.append(counter,counter,Double.parseDouble(line.trim()));
                 counter++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private HashMap<Integer, String> readInvertedMeSHIndex(String indexPath) {
        helperClass helper = new helperClass();
        HashMap<Integer, String> index = helper.getInvertedIndex(indexPath);
        return index;
    }
}

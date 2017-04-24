/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wordVecSimilarity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import wordVecSimilarity.WordVectorsOperation;
import static generateTrainingSet.GraphTrainingSet.wordVectorBaseDir;
import java.io.BufferedWriter;
import java.io.FileWriter;
/**
 *
 * @author super-machine
 */
public class WordVectorEvolution {
    
    static String dirMeSHOutputPath = "/home/super-machine/Documents/mydrive/myResearch/output";
    
    public static void main(String args[]) {
         
        WordVectorsOperation oper = new WordVectorsOperation(wordVectorBaseDir);
         
        BufferedReader br = null;
        File file = null;
        try {
            br = new BufferedReader(new FileReader(dirMeSHOutputPath+File.separator+"goldenTestCases.txt"));
            String line = "";
            try {
                while( (line=br.readLine())!=null){
                    System.out.println(line);
                    file = new File(dirMeSHOutputPath + File.separator + "evolutionWordVectors" + File.separator + line.replace(" ", "_")+".txt");
                    if(file.exists()){
                        file.delete();
                        file.createNewFile();
                    }else{
                        file.createNewFile();
                    }
                    
                    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                    String splits[]= line.split("\t");
                    String inputTermThree = splits[0];
                    String inputTermOne = splits[1];
                    String inputTermTwo = splits[2];
                    
                    for(int date=1965;date<=2000;date=date+1){
                    
                        double score1 = oper.getSimilarity(inputTermOne.toLowerCase().trim(),inputTermTwo.toLowerCase().trim(),date, date);
                        double score2 = oper.getSimilarity(inputTermThree.toLowerCase().trim(),inputTermTwo.toLowerCase().trim(),date, date);
                        double score = score1+score2;
//                        System.out.println(date+" "+score1);  
                        bw.write(date+" "+score);
                        bw.newLine();
                    }
                    bw.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(WordVectorEvolution.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(WordVectorEvolution.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(WordVectorEvolution.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}

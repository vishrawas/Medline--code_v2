package wordVecSimilarity;

/**
 * Created by super-machine on 12/28/16.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author super-machine
 */
public class WordVectorsOperation {

    /**
     * @param args the command line arguments
     */
    static String baseDirectory = "";
    public WordVectorsOperation(String baseDirectory){
        this.baseDirectory = baseDirectory;
    }
    public static void main(String[] args) {

    }

    public double getSimilarity(String token1, String token2, int year1,int year2) {
        String dir1 = baseDirectory+File.separator+year1+File.separator+token1;
        String dir2 = baseDirectory+File.separator+year2+File.separator+token2;
        String wordVec1 = getWordVector(dir1);
        String wordVec2 = getWordVector(dir2);
        double cosine = getCosineSimilarity(wordVec1,wordVec2);
        return cosine;
    }


    private String getWordVector(String dir) {
        BufferedReader br = null;
        StringBuilder builder = new StringBuilder();
        try {
            br = new BufferedReader(new FileReader(new File(dir)));

            String line = "";
            while((line=br.readLine())!=null){
                builder = builder.append(line);
            }


        } catch (FileNotFoundException ex) {
            Logger.getLogger(WordVectorsOperation.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(WordVectorsOperation.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(WordVectorsOperation.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return builder.toString().trim();
    }

    private double getCosineSimilarity(String wordVec1, String wordVec2) {
        if(wordVec1.isEmpty()||wordVec2.isEmpty()){
            return -1.0;
        }
        else
        {
            double cosine = 0;
            cosine = calcCosine(wordVec1,wordVec2);
            return cosine;
        }

    }

    private double calcCosine(String wordVec1, String wordVec2) {
        double score=0;
        String splits1[] = wordVec1.split("\t");
        String splits2[] = wordVec2.split("\t");
        List<String> split1Set = Arrays.asList(splits1);
        List<String> split2Set = Arrays.asList(splits2);
        if(split1Set.size()==split2Set.size()){
            double numerator=0;
            double denominator1 = 0;
            double denominator2 = 0;

            for(int i=0;i<split1Set.size();i++){
                numerator = numerator +(Double.parseDouble(split1Set.get(i))*Double.parseDouble(split2Set.get(i)));
                denominator1=denominator1+(Math.pow(Double.parseDouble(split1Set.get(i)), 2));
                denominator2=denominator2+(Math.pow(Double.parseDouble(split2Set.get(i)), 2));
            }
            denominator1 = Math.sqrt(denominator1);
            denominator2 = Math.sqrt(denominator2);
            score = numerator/(denominator1*denominator2);
        }
        else
        {
            System.out.println("need to pad the smaller length wordVector");

        }
        return score;
    }

    public double getSimilaritySet(String[] temp, String[] term1Splits, String term1, String ogTerm1, int targetYear, int targetYear0) {
       double total = 0;
       int counter = 0;
       for(String t:temp)
       {
           for(String t1:term1Splits){
               double score = getSimilarity(t.toLowerCase(), t1.toLowerCase(), targetYear, targetYear0);
               counter++;
               total=total+score;
           }
       }
       return total/counter;
    }

}

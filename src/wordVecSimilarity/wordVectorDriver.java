package wordVecSimilarity;

/**
 * Created by super-machine on 12/28/16.
 */
public class wordVectorDriver {
    public static void main(String[] args) {
        // TODO code application logic here
        String wordVectorBaseDir = "/Users/super-machine/Documents/Research/medline/output/invertedIndex";
        WordVectorsOperation oper = new WordVectorsOperation(wordVectorBaseDir);
        String token1 = "humans";
        String token2 = "infertility";

        int year1 = 1985;
        int year2 = 1985;
        double score1 = oper.getSimilarity(token1,token2,year1,year2);


        System.out.println(score1);
//        token1 = token2;
//        token2 = "Raynaud Disease";
//
//        double score2 = oper.getSimilarity(token1,token2,year1,year2);
//        if(score2<0){
//            score2=0;
//        }
//        System.out.println(score2);
    }
}

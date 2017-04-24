/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package generateTrainingSet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;
import wordVecSimilarity.WordVectorsOperation;
import static generateTrainingSet.GraphTrainingSet.wordVectorBaseDir;


/**
 *
 * @author super-machine
 */
public class path_length_six_wordVector {

    static Index<Node> titleIdx;
    static GraphDatabaseService graphDb;
    static Index<Node> meshIdx;
    static RelationshipIndex dateIdx;
    static IndexManager index;
    static String dirMeSHOutputPath = "/home/super-machine/Documents/mydrive/myResearch/output";
    static final String DB_PATH = "/home/super-machine/Documents/mydrive/myResearch/output/dummy.db";
    static WordVectorsOperation wordVecCosine;
    static int year = 1985;
    
    path_length_six_wordVector() {
        connectGraphDatabase();
         wordVecCosine = new WordVectorsOperation(wordVectorBaseDir);
    }

    public static void main(String args[]) {
        
//        loadWordVectorsWriteToFile(dirMeSHOutputPath);
//        System.exit(-1);
        
//        File file = null;
//        file = new File(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "matlabTrainingFiles" + File.separator +  "path_length_6" +File.separator+"fo_path_length6_wordvectors.txt");
//        if(file.exists()){
//            file.delete();
//            try {
//                file.createNewFile();
//            } catch (IOException ex) {
//                Logger.getLogger(path_length_six_wordVector.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }else{
//            try {
//                file.createNewFile();
//            } catch (IOException ex) {
//                Logger.getLogger(path_length_six_wordVector.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
        
        String path_length_six_fo_freq = dirMeSHOutputPath + File.separator + "traversal" + File.separator + "matlabTrainingFiles" + File.separator + "path_length_6" + File.separator + "positive_training_set_path_FO-RD.txt";
        path_length_six_wordVector pathLengthSixWordVector = new path_length_six_wordVector();
        pathLengthSixWordVector.generateWordVectors(path_length_six_fo_freq);

    }

    private void generateWordVectors(String pathLengthSixWordVector) {
        try (Transaction tx = graphDb.beginTx()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(pathLengthSixWordVector));
                String line = "";
                while ((line = br.readLine()) != null) {
                    System.out.println("line : "+line);
//                    String splits[] = line.split("\t");
//                    String meshID = splits[0];
//                    String meshFrequency = splits[1];
//                    Node meshNode = graphDb.getNodeById(Long.parseLong(meshID));
//                    String meshName = meshNode.getProperty("meshName").toString();
//                    System.out.println("MeshNode : "+meshNode.getProperty("meshName"));
                    WriteWordVectorsWriteToFile(line);
//                    WriteIntermediateNodeToFile(meshName);
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(path_length_six_wordVector.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(path_length_six_wordVector.class.getName()).log(Level.SEVERE, null, ex);
            }
            tx.success();

        }
    }
    
    private void WriteIntermediateNodeToFile(String meshTerm) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "matlabTrainingFiles" + File.separator + "path_length_6" + File.separator + "rd_path_length6_intermediate.txt", true));
            StringBuilder builder = new StringBuilder();
            builder.append(meshTerm.toLowerCase().trim());
            builder.append("\n");
            bw.write(builder.toString());
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(path_length_six_wordVector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void WriteWordVectorsWriteToFile(String meshTerm) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "matlabTrainingFiles" + File.separator + "path_length_6" + File.separator + "positive_training_set_path_FO-RD_wordvectors.txt", true));
            BufferedReader brWordVecFile = new BufferedReader(new FileReader(wordVectorBaseDir + File.separator + year + File.separator + meshTerm));
            String line1 = null;
            StringBuilder builder = new StringBuilder();
            while ((line1 = brWordVecFile.readLine()) != null) {
                String dimensionValue[] = line1.split("\t");
                for (String val : dimensionValue) {
                    builder.append(val).append(" ");
                }
                builder.append("\n");
            }
            bw.write(builder.toString());
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(path_length_six_wordVector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void loadWordVectorsWriteToFile(String fileName) {
        BufferedWriter bw = null;
        BufferedReader br = null;
        File file = null;
        try {
            try {
                file = new File(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "matlabTrainingFiles" + File.separator + "path_length_6" + File.separator + "false_positive_training_set_path_FO-RD_wordvectors.txt");
                if (file.exists()) {
                    file.delete();
                    file.createNewFile();
                } else {
                    file.createNewFile();
                }
            } catch (IOException ex) {
                Logger.getLogger(path_length_six_wordVector.class.getName()).log(Level.SEVERE, null, ex);
            }
            br = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "matlabTrainingFiles" + File.separator + "path_length_6" + File.separator + "false_positive_training_set_path_FO-RD.txt"));
            String line = "";
            try {
                bw = new BufferedWriter(new FileWriter(file,true));
                while ((line = br.readLine()) != null) {
                        BufferedReader brWordVecFile = new BufferedReader(new FileReader(wordVectorBaseDir + File.separator + year + File.separator + line.toLowerCase().trim()));
                        String line1 = null;
                        StringBuilder builder = new StringBuilder();
                        while ((line1 = brWordVecFile.readLine()) != null) {
                            String dimensionValue[] = line1.split("\t");
                            for (String val : dimensionValue) {
                                builder.append(val).append(" ");
                            }
                            builder.append("\n");
                        }
                        bw.write(builder.toString());
                    
                }
            } catch (IOException ex) {
                Logger.getLogger(path_length_six_wordVector.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    bw.close();
                } catch (IOException ex) {
                    Logger.getLogger(path_length_six_wordVector.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(path_length_six_wordVector.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(path_length_six_wordVector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void connectGraphDatabase() {
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(DB_PATH));
        registerShutdownHook(graphDb);
    }

    public static void registerShutdownHook(final GraphDatabaseService graphDb) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
            }
        });
    }

}

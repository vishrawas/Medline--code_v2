/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.search.NumericRangeQuery;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;

/**
 *
 * @author super-machine
 */
public class Evaluation {

    static Index<Node> titleIdx;
    static GraphDatabaseService graphDb;
    static Index<Node> meshIdx;
    static RelationshipIndex dateIdx;
    static IndexManager index;
    static String dirMeSHOutputPath = "/home/super-machine/Documents/mydrive/myResearch/output";
    static final String DB_PATH = "/home/super-machine/Documents/mydrive/myResearch/output/dummy.db";

    public Evaluation() {
        connectGraphDatabase();
    }

    public static void main(String args[]) {

        Evaluation evaluation = new Evaluation();
        String inputTerm1 = "Fish Oils";
        String inputTerm2 = "Raynaud Disease";
        int date = 1986;
        evaluation.evaluateOutPut(dirMeSHOutputPath, inputTerm1, inputTerm2, date);
    }

    public void evaluateOutPut(String dirMeSHOutputPath, String inputTerm1, String inputTerm2, int date) {
        try {
            String fileName = dirMeSHOutputPath + File.separator + "evaluation" + File.separator + "Fo-RD" + File.separator + "Fo-RD-Bicob.txt";
            String line = "";
            BufferedReader br = null;
            br = new BufferedReader(new FileReader(fileName));

            while ((line = br.readLine()) != null) {
                System.out.println("Term : " + line);
                String intermediateTerm = line.toLowerCase().trim();
                Set<Node> articleNodes_Intermediate = getMeshNodeIDFromTermName(intermediateTerm.toLowerCase(), date);
//                System.out.println("Intermediate processing done : " + articleNodes_Intermediate.size());
                Set<Node> articleNodes_Startterm = getMeshNodeIDFromTermName(inputTerm1.toLowerCase(), date);
//                System.out.println("Start term processing done : " + articleNodes_Startterm.size());
                Set<Node> articleNodes_EndTerm = getMeshNodeIDFromTermName(inputTerm2.toLowerCase(), date);
//                System.out.println("End term processing done : " + articleNodes_EndTerm.size());
                Set<Node> intersection_intermediate_startterm = new HashSet<Node>(articleNodes_Intermediate);
                Set<Node> intersection_intermediate_endterm = new HashSet<Node>(articleNodes_Intermediate);
                intersection_intermediate_startterm.retainAll(articleNodes_Startterm);
                intersection_intermediate_endterm.retainAll(articleNodes_EndTerm);
                
                double intersect_start_intermediate_size = (double) intersection_intermediate_startterm.size();
                double intersect_end_intermediate_size = (double) intersection_intermediate_endterm.size();
                
                System.out.println("Total count : A intersection B : " + intersection_intermediate_startterm.size());
                System.out.println("Total count : A union B : " + articleNodes_Startterm.size()+articleNodes_Intermediate.size());
                System.out.println("Total count : B intersection C : " + intersection_intermediate_endterm.size());
                System.out.println("Total count : B union C : " + articleNodes_EndTerm.size()+articleNodes_Intermediate.size());
                
                /**
                * Total probability in future : P(AB) = P(AB)/P(A)+P(B), P(BC) = P(BC)/P(B)+P(C) 
                */
                
                double probability_AB = (double) intersect_start_intermediate_size/(articleNodes_Startterm.size()+articleNodes_Intermediate.size());
                double probability_BC = (double) intersect_end_intermediate_size/(articleNodes_EndTerm.size()+articleNodes_Intermediate.size());

                System.out.println("Probability_AB : " + probability_AB);
                System.out.println("Probability_BC : " + probability_BC);
                double total_probability = probability_AB+probability_BC;
                System.out.println("Total Probability : " + total_probability);
            }

        } catch (IOException ex) {
            Logger.getLogger(Evaluation.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public Set<Node> getMeshNodeIDFromTermName(String term, int date) {

        Set<Node>articleA = new HashSet<>();
            
        try (Transaction tx = graphDb.beginTx()) {
            index = graphDb.index();
            titleIdx = index.forNodes("article");
            meshIdx = index.forNodes("meshName");
            dateIdx = index.forRelationships("dates");
            Node nd = null;
            IndexHits<Node> nodes = meshIdx.get("meshName", term);
            
            if (nodes.hasNext()) {
                nd = nodes.next();
            }
           
            IndexHits<Relationship> relationships = getRelationships(nd, date);
            
            for (Relationship relationship : relationships) {
                Node otherNode = relationship.getOtherNode(nd);
                articleA.add(otherNode);    
            }
           
            tx.success();
        }
        return  articleA;
    }

    private IndexHits<Relationship> getRelationships(Node node, int year) {
        NumericRangeQuery<Integer> pageQueryRange = NumericRangeQuery.newIntRange("year-numeric", year+1, 2016, true, true);
        IndexHits<Relationship> hits;
        if (node.hasLabel(Label.label("article"))) {
            hits = dateIdx.query(pageQueryRange, null, node);
        } else {
            hits = dateIdx.query(pageQueryRange, node, null);
        }

        if (hits == null) {
            return null;
        }
        if (!hits.iterator().hasNext()) {
            return null;
        }
        return hits;
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

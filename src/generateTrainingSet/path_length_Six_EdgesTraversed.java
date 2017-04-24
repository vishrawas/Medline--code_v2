/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package generateTrainingSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author super-machine
 */
public class path_length_Six_EdgesTraversed {

    LinkedHashMap<String, String> mapClassiferOutputs = new LinkedHashMap<>();
    LinkedHashMap<String, String> mapPMIDToIntermediate = new LinkedHashMap<>();

    static String dirMeSHOutputPath = "/home/super-machine/Documents/mydrive/myResearch/output";

    public static void main(String args[]) {
        
        Set<String> genericTerms = new HashSet<String>();
        genericTerms.add("humans");
        genericTerms.add("adults");
        genericTerms.add("male");
        genericTerms.add("female");
        genericTerms.add("animals");
        
        path_length_Six_EdgesTraversed pathLengthSixEdgesTraversed = new path_length_Six_EdgesTraversed();
        String classifierOutputPath = dirMeSHOutputPath + File.separator + "traversal" + File.separator + "matlabTrainingFiles" + File.separator + "path_length_6" + File.separator + "fo_freq_predictionsOutput.txt";
        String intermediateTermPath = dirMeSHOutputPath + File.separator + "traversal" + File.separator + "matlabTrainingFiles" + File.separator + "path_length_6" + File.separator + "fo_path_length6_intermediate.txt";
        String rawPath = dirMeSHOutputPath + File.separator + "traversal" + File.separator + "matlabTrainingFiles" + File.separator + "path_length_6" + File.separator + "fo_freq.txt";
        pathLengthSixEdgesTraversed.mapTermsToClassiferOutput(intermediateTermPath, classifierOutputPath);
        pathLengthSixEdgesTraversed.mapPMIDTOIntermdiate(intermediateTermPath, rawPath);
        pathLengthSixEdgesTraversed.generateEdgesTraversed(rawPath, genericTerms);
    }
    
    private void generateEdgesTraversed(String rawPath, Set<String> genericTerms) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(rawPath));
            String line = "";
            long pathTraversed = 0;
            long totalPaths = 0;

            while ((line = br.readLine()) != null) {
                String splits[] = line.split("\t");
                String meshTerm = mapPMIDToIntermediate.get(splits[0]);
                String frequency = splits[1];
                if (!genericTerms.contains(meshTerm)) {
                    if (mapClassiferOutputs.containsKey(meshTerm)) {
                        pathTraversed = pathTraversed + Long.parseLong(frequency);
                    }
                    totalPaths = totalPaths + Long.parseLong(frequency);
                }
            }
            System.out.println("Total paths : " + totalPaths);
            System.out.println("Paths traversed : " + pathTraversed);
            double percentageTraversed = (double) pathTraversed / totalPaths;
            System.out.println("Percentages edges saved : " + (100 - percentageTraversed * 100));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(path_length_Six_EdgesTraversed.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(path_length_Six_EdgesTraversed.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(path_length_Six_EdgesTraversed.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private void mapPMIDTOIntermdiate(String intermediateTermPath, String rawPath) {
        try {
            BufferedReader intermediateTerms = new BufferedReader(new FileReader(intermediateTermPath));
            BufferedReader rawPaths = new BufferedReader(new FileReader(rawPath));
            String intermediateTerm = "";
            String pmids = "";
            while ((intermediateTerm = intermediateTerms.readLine()) != null && (pmids = rawPaths.readLine()) != null) {
                String splits[] = pmids.split("\t");
                String PMID = splits[0];
                mapPMIDToIntermediate.put(PMID.toLowerCase().trim(), intermediateTerm.toLowerCase().trim());
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(path_length_Six_EdgesTraversed.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(path_length_Six_EdgesTraversed.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void mapTermsToClassiferOutput(String intermediateTermPath, String classifierOutputPath) {
        try {
            BufferedReader intermediateTerms = new BufferedReader(new FileReader(intermediateTermPath));
            BufferedReader classiferOutputs = new BufferedReader(new FileReader(classifierOutputPath));

            String term = "";
            String classifierOutput = "";

            while ((term = intermediateTerms.readLine()) != null && (classifierOutput = classiferOutputs.readLine()) != null) {
                if ("target".equals(classifierOutput.trim())) {
                    mapClassiferOutputs.put(term, classifierOutput.trim());
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(path_length_Six_EdgesTraversed.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(path_length_Six_EdgesTraversed.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package openDiscovery;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author super-machine
 */
public class openDiscoverRankByMeshTree {

    static String dirMeSHOutputPath = "/home/super-machine/Documents/mydrive/myResearch/output";
    static HashMap<String, Set<String>> meshSemanticMapping = new HashMap<>();
    static LinkedHashMap<String, String> treeCodeToShortestTreeCodeMapping = new LinkedHashMap<>();
    static HashMap<String, Set<String>> invertedTreeMeSHIndex = new HashMap<>();
    static HashMap<String, Set<String>> termReplacementMap = new HashMap<>();
    static ArrayList<String> toWrite = new ArrayList<>();

    public static void main(String args[]) {
        loadMeshTreeCode();
        openDiscoverRankByMeshTree opendisRank = new openDiscoverRankByMeshTree();
        opendisRank.populateHashMap(dirMeSHOutputPath);
        opendisRank.calculateShortestTreeMapping();
        opendisRank.replace();
        opendisRank.write();
    }

    private static void loadMeshTreeCode() {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(dirMeSHOutputPath + File.separator + "meshSemanticMapping.txt"));
            String line = null;

            while ((line = br.readLine()) != null) {
                String splits[] = line.split("\t");
                String meshTerm = splits[0];
                String semType = splits[1];

                Set<String> semT = new HashSet<>();
                String treeSplits[] = semType.split(" ");
                for (String tree : treeSplits) {
                    if (meshSemanticMapping.containsKey(meshTerm.toLowerCase().trim())) {
                        Set<String> semTypes = meshSemanticMapping.get(meshTerm.toLowerCase().trim());
                        semTypes.add(tree.toLowerCase());
                        meshSemanticMapping.put(meshTerm, semTypes);
                    } else {
                        semT.add(tree.toLowerCase().trim());
                        meshSemanticMapping.put(meshTerm, semT);
                    }
                }

            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(openDiscoveryRankByCosine.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(openDiscoverRankByMeshTree.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void populateHashMap(String dirMeSHOutputPath) {
        String filePath = dirMeSHOutputPath + File.separator + "traversal" + File.separator + "path_length_open_discovery" + File.separator + "sortedFo_RD_Open.txt";
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String line1 = "";
            while ((line1 = br.readLine()) != null) {
                String splits[] = line1.split("\t");
                String endTerm = splits[3];

                Set<String> treeCodes = meshSemanticMapping.get(endTerm);
                for (String treeCode : treeCodes) {
                    treeCodeToShortestTreeCodeMapping.put(treeCode, treeCode);
                    if (invertedTreeMeSHIndex.containsKey(treeCode)) {
                        Set<String> meshTerms = invertedTreeMeSHIndex.get(treeCode);
                        meshTerms.add(endTerm);
                        invertedTreeMeSHIndex.put(treeCode, meshTerms);
                    } else {
                        Set<String> meshTerms = new HashSet<>();
                        meshTerms.add(endTerm);
                        invertedTreeMeSHIndex.put(treeCode, meshTerms);
                    }

                }

            }
            br.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(openDiscoverRankByMeshTree.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(openDiscoverRankByMeshTree.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void calculateShortestTreeMapping() {
        Set<String> treeCodes = treeCodeToShortestTreeCodeMapping.keySet();
        for (String treeCode : treeCodes) {
            String shortest = treeCodeToShortestTreeCodeMapping.get(treeCode);
            for (String treeCodeTemp : treeCodes) {
                boolean isSubstring = isSubstring(shortest, treeCodeTemp);
                if (isSubstring) {
                    shortest = treeCodeTemp;

                }
            }
            treeCodeToShortestTreeCodeMapping.put(treeCode, shortest);
        }
    }

    private boolean isSubstring(String shortest, String treeCodeTemp) {
        if (shortest.contains(treeCodeTemp)) {
            return true;
        } else {
            return false;
        }
    }

    private void replace() {
        String filePath = dirMeSHOutputPath + File.separator + "traversal" + File.separator + "path_length_open_discovery" + File.separator + "sortedFo_RD_Open.txt";
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String line1 = "";
            while ((line1 = br.readLine()) != null) {
                String splits[] = line1.split("\t");
                String endTerm = splits[3];

                Set<String> treeCodes = meshSemanticMapping.get(endTerm);
                String longest = "";
                for (String treeCode : treeCodes) {
                    String shortest = treeCodeToShortestTreeCodeMapping.get(treeCode);
                    int longestLength = 1;
                    int shortestLength = 1;
                    if (longest.contains(".")) {
                        String longestSplits[] = longest.split(".");
                        longestLength = longestSplits.length;
                    }

                    if (shortest.contains(".")) {
                        String shortestSplits[] = shortest.split("\\.");
                        shortestLength = shortestSplits.length;
                    }
                    if (longestLength < shortestLength) {
                        longest = shortest;
                    }

                }
                Set<String> shortestMeSHTerms = invertedTreeMeSHIndex.get(longest);
                termReplacementMap.put(endTerm, shortestMeSHTerms);
            }
            br.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(openDiscoverRankByMeshTree.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(openDiscoverRankByMeshTree.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void write() {
        String filePath = dirMeSHOutputPath + File.separator + "traversal" + File.separator + "path_length_open_discovery" + File.separator + "sortedFo_RD_Open.txt";
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String line1 = "";
            StringBuilder builder = new StringBuilder();
            Set<String>finalEndTerm = new HashSet<>();
            while ((line1 = br.readLine()) != null) {
                String splits[] = line1.split("\t");
                String cosine = splits[0];
                String firstTerm = splits[1];
                String middleTerm = splits[2];
                String endTerm = splits[3];
                Set<String> replacement = termReplacementMap.get(endTerm);
                for (String term : replacement) {
                    if(!finalEndTerm.contains(term)){
                    String toWriteString = cosine + "\t" + firstTerm + "\t" + middleTerm + "\t" + term + "\n";
                    System.out.println(toWriteString);
                    builder.append(toWriteString);
                    finalEndTerm.add(term);
                    }
                }

            }
            br.close();
            BufferedWriter bw = new BufferedWriter(new FileWriter(dirMeSHOutputPath + File.separator + "traversal" + File.separator + "path_length_open_discovery" + File.separator + "sortedFo_RD_Open_shortest.txt"));
            bw.append(builder.toString());
            bw.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(openDiscoverRankByMeshTree.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(openDiscoverRankByMeshTree.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}

package MeSH_Vector;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.TreeSet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by super-machine on 12/15/16.
 */
public class CovertRedSVDFormat {

    public void format(String inputPath, String dirMeSHOutputPath, TreeSet<Integer> yearSet, int minYear, int maxYear, int startPoint) {
        CovertRedSVDFormat convert = new CovertRedSVDFormat();
        String opPathS= dirMeSHOutputPath + File.separator + "redsvd" ;
        Path opPath = Paths.get(opPathS);
        if (!Files.exists(opPath)) {
            try {
                Files.createDirectories(opPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        helperClass helper = new helperClass();
        HashMap<String,Integer>meshIndex = readMeSHIndex(dirMeSHOutputPath+File.separator+"index");
        Map<String, Integer> sortedMap = sortByValue(meshIndex);
        Iterator iter = yearSet.iterator();
        while(iter.hasNext()){
            int year = (int) iter.next();
            String ipPathFile = inputPath+File.separator+year;
            System.out.println(ipPathFile);
            convert.convert(sortedMap, meshIndex, ipPathFile,opPathS+File.separator+year);
            Driver.updateLogEntry(startPoint + "\t" + minYear + "\t" + year, dirMeSHOutputPath);
        }
    }
    private HashMap<String, Integer> readMeSHIndex(String indexPath) {
        helperClass helper = new helperClass();
        HashMap<String, Integer> index = helper.getIndex(indexPath);
        return index;
    }
    public static <K, V extends Comparable<? super V>> Map<K, V>
    sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list
                = new LinkedList<>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private void loadIntoSparseMatrix(HashMap<Integer, HashMap<Integer, Double>> sparseEncoding, String filePath, HashMap<String, Integer> meshIndex) throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
        String line = "";
        while ((line = br.readLine()) != null) {
            String from = "";
            String to = "";
            double val = 0.0;
            int counter = 0;
            while (true) {
                int found = line.indexOf("\t");
                String part = line.substring(0, found);
                line = line.substring(found + 1, line.length());
                if (counter == 0) {
                    from = part;
                }
                if (counter == 1) {
                    to = part;
                    val = Double.parseDouble(line);
                    break;
                }

                counter++;
            }

            if (val > 0) {
                int fromIndex = meshIndex.get(from);
                int toIndex = meshIndex.get(to);

                insertIntoSparseEncoding(fromIndex, toIndex, val, sparseEncoding);
            }
        }

    }
    private void convert(Map<String, Integer> meshIndex, HashMap<String, Integer> meshIndexHashMap, String ipFilePath, String opFilePath) {
        try {

            HashMap<Integer, HashMap<Integer, Double>> sparseEncoding = new HashMap<>();
            loadIntoSparseMatrix(sparseEncoding, ipFilePath, meshIndexHashMap);
            writeSparseMatrix(sparseEncoding, opFilePath, meshIndex);

        } catch (FileNotFoundException ex) {
            Logger.getLogger(CovertRedSVDFormat.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CovertRedSVDFormat.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void writeSparseMatrix(HashMap<Integer, HashMap<Integer, Double>> sparseEncoding, String opFilePath, Map<String, Integer> meshIndex) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(new File(opFilePath)));
            for (int rowIndex : meshIndex.values()) {
                if (sparseEncoding.containsKey(rowIndex)) {
                    HashMap<Integer, Double> colVals = sparseEncoding.get(rowIndex);
                    String row = "";
                    StringBuilder sb = new StringBuilder();
                    for (int colIndex : colVals.keySet()) {
                        sb.append(" ").append(colIndex).append(":").append(colVals.get(colIndex));
                    }
                    row = sb.toString().trim();
                    bw.write(row);
                    bw.newLine();
                } else {
                    bw.newLine();
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(CovertRedSVDFormat.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                bw.close();
            } catch (IOException ex) {
                Logger.getLogger(CovertRedSVDFormat.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    private void insertIntoSparseEncoding(int fromIndex, int toIndex, Double val, HashMap<Integer, HashMap<Integer, Double>> sparseEncoding) {
        HashMap<Integer, Double> innerHashMap;
        if (sparseEncoding.containsKey(fromIndex)) {
            innerHashMap = sparseEncoding.get(fromIndex);
        } else {
            innerHashMap = new HashMap<>();
        }
        innerHashMap.put(toIndex, val);
        sparseEncoding.put(fromIndex, innerHashMap);
    }
}

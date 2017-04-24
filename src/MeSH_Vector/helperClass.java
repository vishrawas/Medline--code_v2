package MeSH_Vector;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.neo4j.graphdb.Node;

/**
 * Created by super-machine on 11/30/16.
 */
public class helperClass {

    public String tail(File file) {
        RandomAccessFile fileHandler = null;
        try {
            fileHandler = new RandomAccessFile(file, "r");
            long fileLength = fileHandler.length() - 1;
            StringBuilder sb = new StringBuilder();

            for (long filePointer = fileLength; filePointer != -1; filePointer--) {
                fileHandler.seek(filePointer);
                int readByte = fileHandler.readByte();

                if (readByte == 0xA) {
                    if (filePointer == fileLength) {
                        continue;
                    }
                    break;

                } else if (readByte == 0xD) {
                    if (filePointer == fileLength - 1) {
                        continue;
                    }
                    break;
                }

                sb.append((char) readByte);
            }

            String lastLine = sb.reverse().toString();
            return lastLine;
        } catch (java.io.FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (fileHandler != null) {
                try {
                    fileHandler.close();
                } catch (IOException e) {
                    /* ignore */
                }
            }
        }
    }

    public void loadFileIntoHashMap(HashMap<String, HashMap<String, Double>> previousYearHashMap, Path ip) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(ip.toString()));
            String line = "";
            while ((line = br.readLine()) != null) {
                String splits[] = line.split("\t");
                String from = splits[0];
                String to = splits[1];
                double freq = Double.parseDouble(splits[2]);
                insertIntoHashMap(from, to, freq, previousYearHashMap);

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void insertIntoHashMap(String from, String to, double freq, HashMap<String, HashMap<String, Double>> previousYearHashMap) {
        if (previousYearHashMap.containsKey(from)) {
            HashMap<String, Double> innerHashMap = previousYearHashMap.get(from);
            innerHashMap.put(to, freq);
            previousYearHashMap.put(from, innerHashMap);
        } else {
            HashMap<String, Double> innerHashMap = new HashMap<>();
            innerHashMap.put(to, freq);
            previousYearHashMap.put(from, innerHashMap);
        }
    }

    public void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    public void selfCartesianProductandWrite(String PMID, ArrayList<String> tempMeshTerms, String path) {

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(path, true));
            for (String oString : tempMeshTerms) {
                for (String iString : tempMeshTerms) {
                    if (!oString.equals(iString)) {
                        bw.append(PMID + "\t" + oString + "\t" + iString);
                        bw.newLine();
                    }
                }
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void writeHashMap(HashMap<String, HashMap<String, Double>> matrixR, String opPathS) {
        try {
            System.out.println("Writing --> " + opPathS);
            Iterator iter = matrixR.keySet().iterator();
            BufferedWriter bw = new BufferedWriter(new FileWriter(opPathS));
            while (iter.hasNext()) {
                String oKey = iter.next().toString();
                HashMap<String, Double> innerHashMap = matrixR.get(oKey);
                Iterator innerIterator = innerHashMap.keySet().iterator();
                while (innerIterator.hasNext()) {
                    String iKey = innerIterator.next().toString();
                    Double val = innerHashMap.get(iKey);
                    bw.append(oKey + "\t" + iKey + "\t" + val + "\n");
                }
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getMaxYear(String inputPath) {
        int maxYear = 0;
        int minYear = 1000000;
        try {
            BufferedReader br = new BufferedReader(new FileReader(inputPath));
            String line = "";
            while ((line = br.readLine()) != null) {
                int tabCounter = 0;
                while (true) {
                    int indexOfTab = line.indexOf("\t");
                    if (indexOfTab == -1) {
                        if (tabCounter == 3) {
                            String subString = line.substring(0, line.length());
                            int tempyear = Integer.parseInt(subString);
                            if (maxYear < tempyear) {
                                maxYear = tempyear;
                            }
                            if (minYear > tempyear) {
                                minYear = tempyear;
                            }
                        }
                        break;
                    }
                    if (indexOfTab != -1) {
                        line = line.substring(indexOfTab + 1, line.length());
                        tabCounter++;
                    }
                }

            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return minYear + "\t" + maxYear;
    }

    public void appendFileIntoHashMap(HashMap<String, HashMap<String, Double>> previousYearHashMap, Path ip) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(ip.toString()));
            String line = "";
            while ((line = br.readLine()) != null) {
                String splits[] = line.split("\t");
                String from = splits[0];
                String to = splits[1];
                double freq = Double.parseDouble(splits[2]);
                if (previousYearHashMap.containsKey(from)) {
                    HashMap<String, Double> innerHashMap = previousYearHashMap.get(from);
                    if (innerHashMap.containsKey(to)) {
                        Double val = innerHashMap.get(to);
                        val = val + freq;
                        innerHashMap.put(to, val);
                        previousYearHashMap.put(from, innerHashMap);
                    } else {
                        innerHashMap.put(to, freq);
                        previousYearHashMap.put(from, innerHashMap);
                    }
                } else {
                    HashMap<String, Double> innerHashMap = new HashMap<>();
                    innerHashMap.put(to, freq);
                    previousYearHashMap.put(from, innerHashMap);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getYears(String logPath, Set<Integer> completedYearsCode4, Set<Integer> completedYearsCode6, Set<Integer> completedYears8, Set<Integer> completedYears9, Set<Integer> completedYears10) {

        String toReturn = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(logPath));
            String line = "";
            while ((line = br.readLine()) != null) {
                String code = line.split("\t")[0];
                if (code.equals("2")) {
                    String minYear = line.split("\t")[1];
                    String maxYear = line.split("\t")[2];
                    toReturn = minYear + "\t" + maxYear;

                }
                if (code.equals("4")) {
                    if (!line.contains("Done")) {
                        int year = Integer.parseInt(line.split("\t")[2]);
                        completedYearsCode4.add(year);
                    }
                }
                if (code.equals("6")) {
                    if (!line.contains("Done")) {
                        int year = Integer.parseInt(line.split("\t")[2]);
                        completedYearsCode6.add(year);
                    }
                }
                if (code.equals("8")) {
                    if (!line.contains("Done")) {
                        int year = Integer.parseInt(line.split("\t")[2]);
                        completedYears8.add(year);
                    }
                }
                if (code.equals("9")) {
                    if (!line.contains("Done")) {
                        int year = Integer.parseInt(line.split("\t")[2]);
                        completedYears9.add(year);
                    }
                }
                if (code.equals("10")) {
                    if (!line.contains("Done")) {
                        int year = Integer.parseInt(line.split("\t")[2]);
                        completedYears9.add(year);
                    }
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return toReturn;

    }

    public void appendStringToFile(String toWrite, String path) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(path, true));
            bw.write(toWrite);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void appendStringToFileBufferedWriter(String toWrite, BufferedWriter bw) {
        try {

            bw.write(toWrite);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeIndex(HashMap<String, Integer> index, String outPath) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(outPath));
            for (String oKey : index.keySet()) {
                int val = index.get(oKey);
                bw.write(oKey + "\t" + val + "\n");
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, Integer> getIndex(String indexPath) {
        HashMap<String, Integer> index = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(indexPath));
            String line = "";
            while ((line = br.readLine()) != null) {
                String splits[] = line.split("\t");
                index.put(splits[0].trim(), Integer.parseInt(splits[1].trim()));
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return index;
    }

    public HashMap<Integer, String> getInvertedIndex(String indexPath) {
        HashMap<Integer, String> index = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(indexPath));
            String line = "";
            while ((line = br.readLine()) != null) {
                String splits[] = line.split("\t");
                index.put(Integer.parseInt(splits[1].trim()), splits[0].trim());
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return index;
    }

    public void extractMeshTermsFromStringArrayList(String subString, ArrayList<String> meshTerms) {
        int start = 0;
        while (true) {
            int found = subString.indexOf("$$", start);
            if (found != -1) {
                String meshTerm = subString.substring(start, found);
                meshTerms.add(meshTerm.trim());
                start = found + 2;
            }
            if (found == -1) {
                break;
            }
        }
    }

    public void writeHashMapByKey(HashMap<Integer, HashMap<Integer, String>> allMeshCoccur, Path opPath) {
        Iterator iter = allMeshCoccur.keySet().iterator();
        while (iter.hasNext()) {
            Integer key = (Integer) iter.next();
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(opPath.toString() + File.separator + key));
                StringBuilder toWrite = new StringBuilder();
                HashMap<Integer, String> innerVals = allMeshCoccur.get(key);
                Iterator innerIter = innerVals.keySet().iterator();
                while (innerIter.hasNext()) {
                    Integer innerKey = (Integer) innerIter.next();
                    String year_pmid = innerVals.get(innerKey);
                    toWrite.append(innerKey + "\t" + year_pmid + "\n");
                }
                bw.write(toWrite.toString());
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * *
     * Reads a delimiter separated file and returns an arraylist
     *
     * @param path
     * @param delimiter
     * @return
     */
    public ArrayList<ArrayList<String>> readDelimiterSeparatedFile(String path, String delimiter) {
        ArrayList<ArrayList<String>> contents = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line = "";
            while ((line = br.readLine()) != null) {
                ArrayList<String> splits = splitLine(line, delimiter);
                contents.add(splits);
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contents;
    }

    /**
     * *
     * Splits a line based on a delimiter passed
     *
     * @param line
     * @param delimiter
     * @return
     */
    public ArrayList<String> splitLine(String line, String delimiter) {
        ArrayList<String> splits = new ArrayList<>();
        int indexof = line.indexOf(delimiter, 0);
        while (indexof != -1) {
            String substring = line.substring(0, indexof);
            line = line.substring(indexof + 1, line.length());
            splits.add(substring);
            indexof = line.indexOf(delimiter, 0);
        }
        splits.add(line);
        return splits;
    }

    /**
     * *
     * Recursively deletes a folder if present and creates a new one regardless
     *
     * @param dirpath
     */
    public void deleteIfExistsCreateNewFolder(String dirpath) {
        Path path = Paths.get(dirpath);
        if (Files.exists(path)) {
            System.out.println("Deleting previous instance of " + path.toString());
            deleteFolder(new File(path.toString()));
        }
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double calculateCutOffAverageHashMap(Map<Node, Double> lineDouble) {
        double cutoff = 0;
        Iterator iter = lineDouble.keySet().iterator();
        int count = 0, diffCount = 0;
        double prevVal = 0;
        double diffCum = 0;
        while (iter.hasNext()) {
            Node nd = (Node) iter.next();
            double val = lineDouble.get(nd);
            if (count >= 1) {

                diffCum = diffCum + (val - prevVal);
                diffCount++;
                prevVal = val;
            }
            if (count == 0) {

                prevVal = val;
            }
            count++;
        }
        cutoff = diffCum / count;
        return cutoff;
    }

    public double calculateCutOffAverageElbowMethod(ArrayList<Double> lineDouble) {
        double cutoff = 0;
        Iterator iter = lineDouble.iterator();
        int count = 0, diffCount = 0;
        double prevVal = 0;
        double diffCum = 0;
        while (iter.hasNext()) {
            if (count >= 1) {
                double val = (double) iter.next();
                diffCum = diffCum + (prevVal - val);
                diffCount++;
                prevVal = val;
            }
            if (count == 0) {
                prevVal = (double) iter.next();
            }
            count++;
        }
        cutoff = diffCum / count;
        return cutoff;
    }

    public <K, V extends Comparable<? super V>> Map<K, V> sortByValueASC(Map<K, V> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())//Collections.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }
      public <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public static Set<String> listFilesForFolder(final File folder) {
        Set<String> allFiles= new HashSet<String>();
        
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
//                System.out.println(fileEntry.getName());
//                allFiles.add(fileEntry.getAbsolutePath());
                allFiles.add(fileEntry.getName());
            }
        }
        return allFiles;
    }

}

package neo4jGraphIndexer;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static neo4jGraphIndexer.addOneLevelToExistingPaths.getLastEntry;

/**
 * Created by super-machine on 3/28/17.
 */
public class joinOneLevel {
    static final String pattern = "\\((.*?)\\)";
    static Pattern r = Pattern.compile(pattern);

    public static void main(String args[]) {
        String leftIpPath = "/Users/super-machine/Documents/Research/medline/output/traversal/path_length_4/1_A1";
        String rightIpPath = "/Users/super-machine/Documents/Research/medline/output/traversal/path_length_6/nextLevel_rd.txt";
        String opPath = "/Users/super-machine/Documents/Research/medline/output/traversal/path_length_6/rd.txt";
        try {
            BufferedReader br = new BufferedReader(new FileReader(leftIpPath));
            HashMap<String, Set<String>> leftTerm = getLastTerm(br);
            br.close();
            BufferedWriter bw = new BufferedWriter(new FileWriter(opPath));
            br = new BufferedReader(new FileReader(rightIpPath));
            String line = "";
            int counter = 0;
            StringBuilder builder = new StringBuilder();
            int batchCounter =1;
            while ((line = br.readLine()) != null) {
                String firstTermSplit = getFirstTerm(line);
                if (firstTermSplit != null) {
                    String splits[] = firstTermSplit.split("\t");
                    String firstTerm = splits[0].trim();
                    String toJoin = splits[1].trim();
                    if (leftTerm.containsKey(firstTerm)) {
                        Set<String> leftString = leftTerm.get(firstTerm);
                        for (String left : leftString) {
                            String joined = left + toJoin;
                            builder.append(joined).append("\n");
                            counter++;
                            if (counter == 100000) {
                                System.out.println(batchCounter*counter);
                                batchCounter++;
                                bw.write(builder.toString());
                                bw.newLine();
                                counter = 0;
                                builder.setLength(0);
                                builder.trimToSize();
                            }
                        }
                    }
                }
            }
            System.out.println("finishing up");
            bw.write(builder.toString());
            bw.newLine();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getFirstTerm(String line) {

        String firstTerm = "";
        Matcher m = r.matcher(line);
        int counter = 0;
        while (m.find()) {
            counter++;
            firstTerm = m.group(1);
            if (counter > 0) {
                if (line.contains("--")) {
                    int indexOf = line.indexOf(")");
                    line = line.substring(indexOf + 1, line.length());
                    return firstTerm + "\t" + line;
                }
            }
        }
        return null;
    }

    private static HashMap<String, Set<String>> getLastTerm(BufferedReader br) {
        HashMap<String, Set<String>> toReturn = new HashMap<>();
        String line = "";
        try {
            while ((line = br.readLine()) != null) {
                String article = getLastEntry(line);
                if (toReturn.containsKey(article)) {
                    Set<String> temp = toReturn.get(article);
                    temp.add(line);
                    toReturn.put(article, temp);
                } else {
                    Set<String> temp = new HashSet<>();
                    temp.add(line);
                    toReturn.put(article, temp);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return toReturn;
    }
}

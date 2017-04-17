package neo4jGraphIndexer;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by super-machine on 3/29/17.
 */
public class extractMeSHTermsFromUni {
    static final String pattern = "\\((.*?)\\)";
    static Pattern r = Pattern.compile(pattern);
    static String ipPath = "/Users/super-machine/Documents/Research/medline/output/traversal/path_length_6/rd.txt";
    static String opPath = "/Users/super-machine/Documents/Research/medline/output/traversal/path_length_6/rd_freq.txt";

    public static void main(String args[]) {
        try {
            HashMap<String, String> termFreq = new HashMap<>();
            BufferedReader br = new BufferedReader(new FileReader(ipPath));
            int lineCounter = 1;
            String line = "";
            while ((line = br.readLine()) != null) {
                ArrayList<String> terms = getLastEntry(line);
                if(lineCounter%100000==0)
                {
                    System.out.println(lineCounter);
                }
                lineCounter++;
                int counter = 0;
                for (String term : terms) {
                    counter++;
                    if (counter > 1 && counter % 2 != 0) {
                        if (termFreq.containsKey(term)) {
                            String freq = termFreq.get(term);

                            termFreq.put(term, Long.toUnsignedString(Long.parseUnsignedLong(freq)+1));
                        } else {

                            termFreq.put(term, Long.toUnsignedString(Long.parseUnsignedLong("1")));
                        }
                    }
                }
            }
            br.close();
            BufferedWriter bw = new BufferedWriter(new FileWriter(opPath));
            for (String term : termFreq.keySet()) {
                String freq = termFreq.get(term);
//                String l1Str = Long.toUnsignedString(freq);
                bw.append(term + "\t" + freq);
                bw.newLine();
            }
            bw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<String> getLastEntry(String s) {
        ArrayList<String> terms = new ArrayList<>();
        Matcher m = r.matcher(s);

        while (m.find()) {

            terms.add(m.group(1));
        }
        return terms;
    }
}

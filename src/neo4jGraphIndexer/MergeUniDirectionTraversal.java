package neo4jGraphIndexer;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by super-machine on 2/4/17.
 */
public class MergeUniDirectionTraversal {
    static final String pattern = "\\((.*?)\\)";
    static Pattern r = Pattern.compile(pattern);

    public static void main(String args[]){
//        String inputPath1="/Users/super-machine/Documents/Research/medline/output/traversal/path_length_4/mi.txt";
//        String inputPath2="/Users/super-machine/Documents/Research/medline/output/traversal/path_length_4/mg.txt";
//        String writePath = "/Users/super-machine/Documents/Research/medline/output/traversal/path_length_4/mi-mg_1.txt";
//        MergeUniDirectionTraversal merger = new MergeUniDirectionTraversal();
//        merger.mergeDriver(inputPath1,inputPath2,writePath);

    }

    public void mergeDriver(String inputPath1, String inputPath2, String writePath) {
        try {
            System.out.println("joining "+inputPath1+"\t"+inputPath2);
            BufferedWriter bw = new BufferedWriter(new FileWriter(writePath, true));
            HashMap<String,Set<String>>mappingsForLastTerms = getMappings(inputPath1);
            BufferedReader br = new BufferedReader(new FileReader(inputPath2));
            String line = "";
            while((line=br.readLine())!=null){
                String term = getLastEntry(line);
                if(mappingsForLastTerms.containsKey(term)){
                    line = removeLastToken(line);
                    String reversedLine = reverseWords2(line);
                    mergeAndWrite(mappingsForLastTerms.get(term),reversedLine,bw);

                }
            }
            br.close();


            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private  void mergeAndWrite(Set<String> inputSet, String reversedLine, BufferedWriter bw) throws IOException {
        StringBuilder builder = new StringBuilder();

        for(String input:inputSet){
          builder.append(input).append("--").append(reversedLine).append("\n");

        }
        bw.append(builder.toString());
    }

    private  HashMap<String,Set<String>> getMappings(String inputPath) {
        HashMap<String,Set<String>>mappingsForLastTerms=new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(inputPath));
            String line = "";

                while ((line = br.readLine()) != null) {
                    if (line.contains("(")) {
                        String token = getLastEntry(line);
                        if (mappingsForLastTerms.containsKey(token)) {
                            Set<String> lines = mappingsForLastTerms.get(token);
                            lines.add(line);
                            mappingsForLastTerms.put(token, lines);
                        } else {
                            Set<String> lines = new HashSet<>();
                            lines.add(line);
                            mappingsForLastTerms.put(token, lines);
                        }
                    }
                }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mappingsForLastTerms;
    }


    private  String getLastEntry(String s) {
        String lastEntry = null;
        Matcher m = r.matcher(s);
        while (m.find()) {
            lastEntry = m.group(1);
        }
        return lastEntry;
    }

    private  String reverseWords2(String sentence) {
        StringBuilder sb = new StringBuilder(sentence.length() + 1);
        String[] words = sentence.split("--");
        for (int i = words.length - 1; i >= 0; i--) {
            sb.append(words[i]).append("--");
        }
        sb.setLength(sb.length() - 2);  // Strip trailing space
        return sb.toString();
    }

    private  String removeLastToken(String line) {
        String s1 = line;
        String s2 = line;
        int i = s1.indexOf("(");
        String lastS1 = "";
        while (i >= 0) {
            int m = s1.indexOf(")");
            lastS1 = s1.substring(i + 1, m);
            s1 = s1.substring(m + 1, s1.length());
            i = s1.indexOf("(");
        }
        lastS1 = "(" + lastS1 + ")";
//        System.out.println(line+"\t"+lastS1);
//            System.out.println(line +     "\t" + lastS1 + "\t" + line.indexOf(lastS1));
        if(line.indexOf(lastS1)!=-1) {
            s1 = line.substring(0, line.indexOf(lastS1));
        }
        return s1;
    }
}

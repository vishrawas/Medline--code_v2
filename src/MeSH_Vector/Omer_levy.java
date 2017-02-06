package MeSH_Vector;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by vishrawa on 12/9/2016.
 */

public class Omer_levy {
    static int D = 300;


    public void factorize(String inputPath, String dirMeSHOutputPath, TreeSet<Integer> years, int minYear, int maxYear, int startPoint) {

        try {

            System.out.println("Years from :" + minYear + "\t" + maxYear);
            Path opPath = Paths.get(dirMeSHOutputPath + File.separator + "SVD");
            if (!Files.exists(opPath)) {
                Files.createDirectories(opPath);
            }


            for (int year : years) {
                String tempinputPath = inputPath + File.separator + year;
                Process process = new ProcessBuilder("/usr/local/bin/redsvd", "-i", tempinputPath, "-o", opPath.toString() + File.separator + year, "-r", "300", "-f", "sparse", "-m", "SVD").start();

                InputStream errorStream = process.getErrorStream();
                InputStream ipStream = process.getInputStream();
                InputStreamReader isr = new InputStreamReader(errorStream);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
                isr = new InputStreamReader(ipStream);
                br = new BufferedReader(isr);
                line = null;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }

                Driver.updateLogEntry(startPoint + "\t" + minYear + "\t" + year, dirMeSHOutputPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, Integer> readMeSHIndex(String indexPath) {
        helperClass helper = new helperClass();
        HashMap<String, Integer> index = helper.getIndex(indexPath);
        return index;
    }


}


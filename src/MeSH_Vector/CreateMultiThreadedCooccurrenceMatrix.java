package MeSH_Vector;

/**
 * Created by super-machine on 12/5/16.
 */

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

class MyThread implements Runnable {

    String inputPath, dirMeSHOutputPath;
    int  startPoint,year,minYear;



    public MyThread(int minYear,int year, String inputPath, String dirMeSHOutputPath, int startPoint) {

        this.inputPath = inputPath;
        this.dirMeSHOutputPath = dirMeSHOutputPath;
        this.startPoint = startPoint;
        this.year=year;
        this.minYear=minYear;
    }

    @Override
    public void run() {
        try {
            helperClass helper = new helperClass();
            HashMap<String,HashMap<String,Double>>matrixR = new HashMap<>();

            String opPathS= dirMeSHOutputPath + File.separator + "cooccur" ;
            Path opPath = Paths.get(opPathS);
            if (!Files.exists(opPath)) {
                Files.createDirectories(opPath);
            }
            System.out.println("Year ----> "+inputPath);
            createCoocccurenceMatrix(inputPath,matrixR);
            helper.writeHashMap(matrixR,opPathS+File.separator+year);
//            synchronized (this) {
                Driver.updateLogEntry(startPoint + "\t" + minYear + "\t" + year, dirMeSHOutputPath);
//            }
        } catch (Exception ex) {
            Logger.getLogger(MyThread.class.getName()).log(Level.SEVERE, null, ex);
        }

    }


    private void createCoocccurenceMatrix(String inputPath, HashMap<String, HashMap<String, Double>> matrixR) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(inputPath));
            String line = "";
            while((line=br.readLine())!=null) {
                String splits[] = line.split("\t");
                if (splits.length == 3) {
                    String from = splits[1];
                    String to = splits[2];
                    if (matrixR.containsKey(from)) {
                        HashMap<String, Double> innerHashMap = matrixR.get(from);
                        if (innerHashMap.containsKey(to)) {
                            double val = innerHashMap.get(to);
                            val++;
                            innerHashMap.put(to, val);
                            matrixR.put(from, innerHashMap);
                        } else {
                            innerHashMap.put(to, 1.0);
                            matrixR.put(from, innerHashMap);
                        }
                    } else {
                        HashMap<String, Double> innerHashMap = new HashMap<>();
                        innerHashMap.put(to, 1.0);
                        matrixR.put(from, innerHashMap);
                    }
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

public class CreateMultiThreadedCooccurrenceMatrix {

    public void cooccur(String inputPath, String dirMeSHOutputPath, Set<Integer> years, int minYear, int maxYear, int startPoint) {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            System.out.println("Years from :" + minYear + "\t" + maxYear);
            Path opPath = Paths.get(dirMeSHOutputPath + File.separator + "cooccur" + File.separator);
            if (!Files.exists(opPath)) {
                Files.createDirectories(opPath);
            }
            for (int year : years) {
                String tempinputPath = inputPath+File.separator+year;
                MyThread worker = new MyThread(minYear,year,tempinputPath, dirMeSHOutputPath, startPoint);
                executor.execute(worker);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        executor.shutdown();
        System.out.println("thread shutdown");

        try {
            executor.awaitTermination(6000000, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Finished all threads");
    }

}

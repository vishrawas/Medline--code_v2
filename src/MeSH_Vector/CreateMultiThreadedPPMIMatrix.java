/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MeSH_Vector;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author vishrawa
 */
class MyThreadS implements Runnable {

    String inputPath, dirMeSHOutputPath;
    int minYear, year, startPoint;

    public MyThreadS(int year, int minYear, String inputPath, String dirMeSHOutputPath, int startPoint) {
        this.year = year;
        this.minYear = minYear;
        this.inputPath = inputPath;
        this.dirMeSHOutputPath = dirMeSHOutputPath;
        this.startPoint = startPoint;
    }

    @Override
    public void run() {
        String cooccurPath = dirMeSHOutputPath + File.separator + "PostCooccur" + File.separator + year;
        System.out.println("processing ---> "+year);
        HashMap<String,HashMap<String,Double>>cooccur = new HashMap<>();
        helperClass helper = new helperClass();
        helper.loadFileIntoHashMap(cooccur,Paths.get(cooccurPath));
        createPPMIMatrix(cooccur,helper,dirMeSHOutputPath + File.separator + "PPMI_cooccur" + File.separator + year);
//        helper.writeHashMap(matrixPPMI,dirMeSHOutputPath + File.separator + "PPMI_cooccur" + File.separator + year);
//        synchronized (this) {
            Driver.updateLogEntry(startPoint + "\t" + minYear + "\t" + year, dirMeSHOutputPath);
//        }
    }

    private void createPPMIMatrix(HashMap<String,HashMap<String,Double>> matrix,  helperClass helper, String opPath) {
        BufferedWriter bw = null;
        try {
          bw  = new BufferedWriter(new FileWriter(opPath,true));
        } catch (IOException e) {
            e.printStackTrace();
        }

        double d = matrix.get("$$TOTAL$$").get("$$TOTAL$$");
        Iterator oIter = matrix.keySet().iterator();
        while(oIter.hasNext()){
            String oKey = (String) oIter.next();
            if(!oKey.contains("$$TOTAL$$")) {
                double oVal = matrix.get(oKey).get(oKey);
                HashMap<String, Double> innerHashMap = matrix.get(oKey);
                Iterator iIter = innerHashMap.keySet().iterator();
                while (iIter.hasNext()) {
                    String iKey = (String) iIter.next();
                    if (!oKey.equals(iKey)) {
                        double iVal = matrix.get(iKey).get(iKey);
                        double freq = innerHashMap.get(iKey);
                        double ppmi = Math.log(freq * d / (oVal * iVal));
                        if(ppmi<0){
                            ppmi=0;
                        }
                        helper.appendStringToFileBufferedWriter(oKey+"\t"+iKey+"\t"+ppmi+"\n", bw);
                    }
                }
            }
        }
        try {
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}

class CreateMultiThreadedPPMIMatrix {

    public void createPPMI(String inputPath, String dirMeSHOutputPath, Set<Integer> years, int minYear, int maxYear, int startPoint) {
        ExecutorService executor = Executors.newFixedThreadPool(1);

        try {
            System.out.println("Years from :" + minYear + "\t" + maxYear);
            Path opPath = Paths.get(dirMeSHOutputPath + File.separator + "PPMI_cooccur" + File.separator);
            if (!Files.exists(opPath)) {
                Files.createDirectories(opPath);
            }
            for (int year : years) {
                MyThreadS worker = new MyThreadS(year, minYear, inputPath, dirMeSHOutputPath, startPoint);
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

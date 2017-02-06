package MeSH_Vector;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FilenameUtils;
/**
 * Created by super-machine on 11/30/16.
 */
public class Driver {
    public static void main(String args[])
    {
        String dirMedlineInputPath = "/Users/super-machine/Documents/Research/medline/data/medline_raw_files";
        String dirMeSHOutputPath = "/Users/super-machine/Documents/Research/medline/output";
        Driver driver = new Driver();
        while(true) {
            String startPointLine = driver.determineStartPoint(dirMeSHOutputPath + File.separator + "log.log");
            String code = startPointLine.split("\t")[0];
            int startPoint = Integer.parseInt(code);
            if (startPoint == 0) {
                System.out.println("Beginning Medline Dump Processor ");
                driver.extractMeSHTerms(dirMedlineInputPath, dirMeSHOutputPath);
                startPoint++;
                updateLogEntry(startPoint + "\tDone", dirMeSHOutputPath);
                System.out.println("Medline Dump Processing Complete");
            }
            else if (startPoint == 1) {
                System.out.println("Beginning Identification of Max and Min Years in the Dump ");
                String yearS = driver.getMaxMinYear(dirMeSHOutputPath + File.separator + "whole.txt", dirMeSHOutputPath);
                int minYear = Integer.parseInt(yearS.split("\t")[0]);
                int maxYear = Integer.parseInt(yearS.split("\t")[1]);

                startPoint++;
                updateLogEntry(startPoint + "\t" + minYear + "\t" + maxYear, dirMeSHOutputPath);
                System.out.println("Identification of Max and Min Years in the Dump Complete");

            }
            else if(startPoint ==2 ){
                System.out.println("Splitting the dump by Date");
                driver.splitDump(dirMeSHOutputPath + File.separator + "whole.txt", dirMeSHOutputPath);
                startPoint++;
                updateLogEntry(startPoint + "\tDone", dirMeSHOutputPath);
                System.out.println("Splitting of the Dump by Date Complete");
            }
            else if (startPoint == 3) {
                System.out.println("Beginning Pre-Step Creation of Co-occurrence Matrix ");
                driver.createMultiThreadedCooccurrenceMatrix(dirMeSHOutputPath + File.separator + "Splits",dirMeSHOutputPath, ++startPoint);
                Driver.updateLogEntry(startPoint + "\tDone", dirMeSHOutputPath);
                System.out.println("Pre-Step of Co-occurrence Matrix Complete");
            }
            else if (startPoint == 4) {
                if (!startPointLine.equals("4" + "\t" + "Done")) {
                    System.out.println("Resume Pre-Step Creation of Co-occurrence Matrix ");
                    driver.createMultiThreadedCooccurrenceMatrix(dirMeSHOutputPath + File.separator + "Splits", dirMeSHOutputPath, startPoint);
                    Driver.updateLogEntry(startPoint + "\tDone", dirMeSHOutputPath);
                    System.out.println("Pre-Step of Co-occurrence Matrix Complete");

                } else if (startPointLine.equals("4" + "\t" + "Done")) {
                    System.out.println("Creation of cooccurrence  Matrix - cumulative adding");
                    driver.cumulativeAdd(dirMeSHOutputPath + File.separator + "cooccur", dirMeSHOutputPath, ++startPoint);
                    Driver.updateLogEntry(startPoint + "\tDone", dirMeSHOutputPath);
                    System.out.println("Cumulative adding of Co-occurrence Matrix Complete");
                }
            }
            else if(startPoint==5) {
                if (startPointLine.equals("5" + "\t" + "Done")) {
                    System.out.println("Begin Creation of PMI Matrix");
                    driver.createMultiThreadedPPMIMatrix(dirMeSHOutputPath + File.separator + "PostCooccur", dirMeSHOutputPath, ++startPoint);
                    Driver.updateLogEntry(startPoint + "\tDone", dirMeSHOutputPath);
                    System.out.println("Creation of PMI Matrix Complete");
                }
            }
            else if(startPoint==6) {
                if (!startPointLine.equals("6" + "\t" + "Done")) {
                    System.out.println("Resuming Creation of PMI Matrix");
                    driver.createMultiThreadedPPMIMatrix(dirMeSHOutputPath + File.separator + "PostCooccur", dirMeSHOutputPath, startPoint);
                    Driver.updateLogEntry(startPoint + "\tDone", dirMeSHOutputPath);
                    System.out.println("Creation of PMI Matrix Complete");
                } else if (startPointLine.equals("6" + "\t" + "Done")) {
                    System.out.println("Create index of MeSH Terms");
                    driver.indexMeSHTerms(dirMeSHOutputPath + File.separator + "PostCooccur",dirMeSHOutputPath);
                    Driver.updateLogEntry(++startPoint + "\tDone", dirMeSHOutputPath);
                    System.out.println("Index Done!");
                }
            }
            else if(startPoint==7){
                System.out.println("Converting into sparse matrix for redSVD");
                driver.convertSVD(dirMeSHOutputPath + File.separator + "PPMI_cooccur",dirMeSHOutputPath,++startPoint);
                Driver.updateLogEntry(startPoint + "\tDone", dirMeSHOutputPath);
                System.out.println("Conversion Done!");
            }
             else if(startPoint==8){
                if (!startPointLine.equals("8" + "\t" + "Done")) {
                    driver.convertSVD(dirMeSHOutputPath + File.separator + "PPMI_cooccur",dirMeSHOutputPath,startPoint);
                    Driver.updateLogEntry(startPoint + "\tDone", dirMeSHOutputPath);
                    System.out.println("Conversion Done!");
                }
                else if (startPointLine.equals("8" + "\t" + "Done")) {
                    System.out.println("Begin Matrix Factorization - SVD");
                    driver.matrixFactorizationSVD(dirMeSHOutputPath + File.separator + "redsvd", dirMeSHOutputPath, ++startPoint);
                    Driver.updateLogEntry(startPoint + "\tDone", dirMeSHOutputPath);
                    System.out.println(" Matrix Factorization Complete");

                }
            }
            else if(startPoint==9){
                if (!startPointLine.equals("9" + "\t" + "Done")) {
                    System.out.println("Continuing Matrix Factorization - SVD");
                    driver.matrixFactorizationSVD(dirMeSHOutputPath + File.separator + "redsvd", dirMeSHOutputPath, ++startPoint);
                    Driver.updateLogEntry(startPoint + "\tDone", dirMeSHOutputPath);
                    System.out.println(" Matrix Factorization Complete");
                }
                else{
                    System.out.println("Taking Squareroot of Sigma and multiplying it with U");
                    driver.postProcess(dirMeSHOutputPath+File.separator+"SVD",dirMeSHOutputPath,++startPoint);
                    Driver.updateLogEntry(startPoint + "\tDone",dirMeSHOutputPath);
                    System.out.println("Congratulations! Word Vectors are successfully created");


                }
            }
            else if(startPoint==10) {
                if (!startPointLine.equals("10" + "\t" + "Done")) {
                    System.out.println("Resuming taking square-root of sigma and multiplying it with U");
                    driver.postProcess(dirMeSHOutputPath + File.separator + "SVD", dirMeSHOutputPath, startPoint);
                    Driver.updateLogEntry(startPoint + "\tDone", dirMeSHOutputPath);
                    System.out.println("Congratulations! Word Vectors are successfully created");
                } else {
                    System.out.println("Creating first cooccurrence year matrix");
                    driver.createFirstYearCooccur(dirMeSHOutputPath + File.separator + "Splits", dirMeSHOutputPath, ++startPoint);
                    Driver.updateLogEntry(startPoint + "\tDone", dirMeSHOutputPath);
                    System.out.println("Completed the creation of year based first cooccurrence matrix");
                }
            }
            else if(startPoint==11){
                    if (startPointLine.equals("11" + "\t" + "Done")) {
                        System.out.println("creating inverted index of word vectors");
                        driver.createInvertedIndex(dirMeSHOutputPath+File.separator+"wordVector",dirMeSHOutputPath,++startPoint);
                        Driver.updateLogEntry(startPoint + "\tDone",dirMeSHOutputPath);
                        System.out.println("Inverted Index created");
                    }
                }
                else if(startPoint==12){
                if(startPointLine.equals("12" + "\t" + "Done")) {
                    System.out.println("preping for topic clustering");
                    driver.createClusters(dirMeSHOutputPath,dirMeSHOutputPath,++startPoint);
                    Driver.updateLogEntry(startPoint + "\tDone",dirMeSHOutputPath);
                    System.out.println("preping for cluster completed");
                }
            }

            else{
                    System.out.println("All process successfully completed. If you wish to re-run a step please edit the log file and restart");
                    break;
                }


        }
    }

    private void createClusters(String inputPath, String dirMeSHOutputPath, int startPoint) {
        helperClass helper = new helperClass();
        Set<Integer>completedYears4 = new HashSet<>();
        Set<Integer>completedYears6 = new HashSet<>();
        Set<Integer>completedYears8 = new HashSet<>();
        Set<Integer>completedYears9 = new HashSet<>();
        Set<Integer>completedYears10 = new HashSet<>();
        String startEndYears= helper.getYears(dirMeSHOutputPath + File.separator + "log.log",completedYears4,completedYears6,completedYears8,completedYears9,completedYears10);
        int minYear = Integer.parseInt(startEndYears.split("\t")[0]);
        int maxYear = Integer.parseInt(startEndYears.split("\t")[1]);
        TreeSet<Integer> yearSet = generateYearSetFromFolder(dirMeSHOutputPath+File.separator+"wordVector");
        prepClustering prep = new prepClustering();
        prep.cluster(inputPath,dirMeSHOutputPath,yearSet,minYear,maxYear,startPoint);
    }

    private void createInvertedIndex(String inputPath, String dirMeSHOutputPath, int startPoint) {
        invertedIndex inverted =new invertedIndex();
        helperClass helper = new helperClass();
        Set<Integer>completedYears4 = new HashSet<>();
        Set<Integer>completedYears6 = new HashSet<>();
        Set<Integer>completedYears8 = new HashSet<>();
        Set<Integer>completedYears9 = new HashSet<>();
        Set<Integer>completedYears10 = new HashSet<>();
        String startEndYears= helper.getYears(dirMeSHOutputPath + File.separator + "log.log",completedYears4,completedYears6,completedYears8,completedYears9,completedYears10);
        int minYear = Integer.parseInt(startEndYears.split("\t")[0]);
        int maxYear = Integer.parseInt(startEndYears.split("\t")[1]);
        TreeSet<Integer> yearSet = generateYearSetFromFolder(inputPath);
        inverted.createInvertedIndex(inputPath,dirMeSHOutputPath,yearSet,minYear,maxYear,startPoint);
    }

    private void createFirstYearCooccur(String inputPath, String dirMeSHOutputPath, int startPoint) {
        FirstYearCooccur firstYearCooccur = new FirstYearCooccur();
        helperClass helper = new helperClass();
        Set<Integer>completedYears4 = new HashSet<>();
        Set<Integer>completedYears6 = new HashSet<>();
        Set<Integer>completedYears8 = new HashSet<>();
        Set<Integer>completedYears9 = new HashSet<>();
        Set<Integer>completedYears10 = new HashSet<>();
        String startEndYears= helper.getYears(dirMeSHOutputPath + File.separator + "log.log",completedYears4,completedYears6,completedYears8,completedYears9,completedYears10);
        int minYear = Integer.parseInt(startEndYears.split("\t")[0]);
        int maxYear = Integer.parseInt(startEndYears.split("\t")[1]);
        TreeSet<Integer> yearSet = generateYearSetFromFolder(inputPath);
        firstYearCooccur.cooccur(inputPath,dirMeSHOutputPath,yearSet,minYear,maxYear,startPoint);
    }

    private void postProcess(String inputPath, String dirMeSHOutputPath, int startPoint) {
        PostProcessor postProcess = new PostProcessor();
        helperClass helper = new helperClass();
        Set<Integer>completedYears4 = new HashSet<>();
        Set<Integer>completedYears6 = new HashSet<>();
        Set<Integer>completedYears8 = new HashSet<>();
        Set<Integer>completedYears9 = new HashSet<>();
        Set<Integer>completedYears10 = new HashSet<>();
        String startEndYears= helper.getYears(dirMeSHOutputPath + File.separator + "log.log",completedYears4,completedYears6,completedYears8,completedYears9,completedYears10);
        int minYear = Integer.parseInt(startEndYears.split("\t")[0]);
        int maxYear = Integer.parseInt(startEndYears.split("\t")[1]);
        TreeSet<Integer> yearSet = generateYearSetFromFolder(inputPath);
        yearSet.removeAll(completedYears10);
        postProcess.process(inputPath,dirMeSHOutputPath,yearSet,minYear,maxYear,startPoint);
    }

    private void convertSVD(String inputPath, String dirMeSHOutputPath, int startPoint) {
        CovertRedSVDFormat redSVDFormatter = new CovertRedSVDFormat();
        helperClass helper = new helperClass();
        Set<Integer>completedYears4 = new HashSet<>();
        Set<Integer>completedYears6 = new HashSet<>();
        Set<Integer>completedYears8 = new HashSet<>();
        Set<Integer>completedYears9 = new HashSet<>();
        Set<Integer>completedYears10 = new HashSet<>();
        String startEndYears= helper.getYears(dirMeSHOutputPath + File.separator + "log.log",completedYears4,completedYears6,completedYears8,completedYears9,completedYears10);
        int minYear = Integer.parseInt(startEndYears.split("\t")[0]);
        int maxYear = Integer.parseInt(startEndYears.split("\t")[1]);
        TreeSet<Integer> yearSet = generateYearSetFromFolder(inputPath);
        yearSet.removeAll(completedYears8);
        redSVDFormatter.format(inputPath,dirMeSHOutputPath,yearSet,minYear,maxYear,startPoint);
    }

    private void indexMeSHTerms(String inputPath, String dirMeSHOutputPath) {
        MeSHIndexer indexer = new MeSHIndexer();
        indexer.index(inputPath,dirMeSHOutputPath);
    }

    private void matrixFactorizationSVD(String inputPath, String dirMeSHOutputPath, int startPoint) {
        Omer_levy omerLevy = new Omer_levy();
        helperClass helper = new helperClass();
        Set<Integer>completedYears4 = new HashSet<>();
        Set<Integer>completedYears6 = new HashSet<>();
        Set<Integer>completedYears8 = new HashSet<>();
        Set<Integer>completedYears9 = new HashSet<>();
        Set<Integer>completedYears10 = new HashSet<>();
        String startEndYears= helper.getYears(dirMeSHOutputPath + File.separator + "log.log",completedYears4,completedYears6,completedYears8,completedYears9,completedYears10);
        int minYear = Integer.parseInt(startEndYears.split("\t")[0]);
        int maxYear = Integer.parseInt(startEndYears.split("\t")[1]);
        TreeSet<Integer> yearSet = generateYearSetFromFolder(inputPath);
        yearSet.removeAll(completedYears9);
        omerLevy.factorize(inputPath,dirMeSHOutputPath,yearSet,minYear,maxYear,startPoint);
    }

    private void cumulativeAdd(String inputPath, String dirMeSHOutputPath, int startPoint) {
       CumulativeAdd cooccurAdd = new CumulativeAdd();
        helperClass helper = new helperClass();
        Set<Integer>completedYears4 = new HashSet<>();
        Set<Integer>completedYears6 = new HashSet<>();
        Set<Integer>completedYears8 = new HashSet<>();
        Set<Integer>completedYears9 = new HashSet<>();
        Set<Integer>completedYears10 = new HashSet<>();
        String startEndYears= helper.getYears(dirMeSHOutputPath + File.separator + "log.log",completedYears4,completedYears6,completedYears8,completedYears9,completedYears10);
        int minYear = Integer.parseInt(startEndYears.split("\t")[0]);
        int maxYear = Integer.parseInt(startEndYears.split("\t")[1]);
        TreeSet<Integer> yearSet = generateYearSetFromFolder(inputPath);
        cooccurAdd.cooccur(inputPath,dirMeSHOutputPath+File.separator+"PostCooccur",yearSet,minYear,maxYear,startPoint);
    }

    private void splitDump(String inputPath, String dirMeSHOutputPath) {
        splitByDate splitter = new splitByDate();
        splitter.split(inputPath,dirMeSHOutputPath);
    }

    private void createMultiThreadedCooccurrenceMatrix(String inputPath, String dirMeSHOutputPath, int startPoint) {
        CreateMultiThreadedCooccurrenceMatrix cooccur = new CreateMultiThreadedCooccurrenceMatrix();
        helperClass helper = new helperClass();
        Set<Integer>completedYears4 = new HashSet<>();
        Set<Integer>completedYears6 = new HashSet<>();
        Set<Integer>completedYears8 = new HashSet<>();
        Set<Integer>completedYears9 = new HashSet<>();
        Set<Integer>completedYears10 = new HashSet<>();
        String startEndYears= helper.getYears(dirMeSHOutputPath + File.separator + "log.log",completedYears4,completedYears6,completedYears8,completedYears9,completedYears10);
        int minYear = Integer.parseInt(startEndYears.split("\t")[0]);
        int maxYear = Integer.parseInt(startEndYears.split("\t")[1]);
        TreeSet<Integer> yearSet = generateYearSetFromFolder(inputPath);
        yearSet.removeAll(completedYears4);
        cooccur.cooccur(inputPath,dirMeSHOutputPath,yearSet,minYear,maxYear,startPoint);
    }

    private TreeSet<Integer> generateYearSetFromFolder(String inputPath) {
        TreeSet<Integer> yearSet =new TreeSet<>();
        File folder = new File(inputPath);
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                if(!listOfFiles[i].getName().equals(".DS_Store")){
                    yearSet.add(Integer.parseInt(FilenameUtils.removeExtension(listOfFiles[i].getName())));
                }
                System.out.println("File " + listOfFiles[i].getName());
            }
        }

        return yearSet;
    }

      private String getMaxMinYear(String inputPath, String dirMeSHOutputPath) {
        helperClass helper = new helperClass();
        String yearS=helper.getMaxYear(inputPath);
       return yearS;
    }

    public static void updateLogEntry(String logEntry,String dirMeSHOutputPath) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(dirMeSHOutputPath+ File.separator+"log.log",true));
            String toWrite = ""+ logEntry;
            bw.append(toWrite);
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String determineStartPoint(String logPath) {
        String startPoint = "";
        helperClass helper = new helperClass();
        if(!Files.exists(Paths.get(logPath))) {
            startPoint="0";
        }
        else{
            startPoint  = helper.tail(new File(logPath));
        }
        return startPoint;
    }

    private void extractMeSHTerms(String dirMedlineInputPath, String dirMeSHOutputPath) {
        MedlineDumpProcessor dumpProcessor = new MedlineDumpProcessor();
        dumpProcessor.dumpProcessor(dirMedlineInputPath,dirMeSHOutputPath);
    }
    private void createMultiThreadedPPMIMatrix(String inputPath, String dirMeSHOutputPath, int startPoint) {
        CreateMultiThreadedPPMIMatrix ppmi = new CreateMultiThreadedPPMIMatrix();
        helperClass helper = new helperClass();
        Set<Integer>completedYears4 = new HashSet<>();
        Set<Integer>completedYears6 = new HashSet<>();
        Set<Integer>completedYears8 = new HashSet<>();
        Set<Integer>completedYears9 = new HashSet<>();
        Set<Integer>completedYears10 = new HashSet<>();
        String startEndYears= helper.getYears(dirMeSHOutputPath + File.separator + "log.log",completedYears4,completedYears6,completedYears8,completedYears9,completedYears10);
        int minYear = Integer.parseInt(startEndYears.split("\t")[0]);
        int maxYear = Integer.parseInt(startEndYears.split("\t")[1]);
        Set<Integer> yearSet = generateYearSetFromFolder(inputPath);
        yearSet.removeAll(completedYears6);
        ppmi.createPPMI(inputPath,dirMeSHOutputPath,yearSet,minYear,maxYear,startPoint);
    }

}

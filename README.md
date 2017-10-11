# Medline--code_v2
There are 5 major modules provided in this package.

Module-1 Creation of Knowledge base: 
1. To create the knowledge base run the jar file in the out directory with Driver.java in the package MeSH_Vector as the main class.
   It accepts two parameters:
   Parameter-1: Path to the folder containing XML dumps from MEDLINE
   Parameter-2: Output director where all the KB files will be generated.	

2. The program creates a log file at the end of each major step which will allow you to restart the program from that point.

3. On an i5 machine with 3.6GHz, it takes close to a day to finish creating all the files from this step. 

4. Installation of RedSvd is a prerequisite - instructions for installation can be found here: https://code.google.com/archive/p/redsvd/wikis/English.wiki

5. The program will finish with all the preparatory files need for running GMM clustering
   In the output directory provided to the Driver.java, you will find folder named 	EmbeddingFiles. Set the input directory in gmm.m to point to that directory and an output directory to a folder named “cluster”. This MATLAB script will create the required clusters.

6. Following an exactly similar process as before, run “PostClusterDriver.java” under post_cluster_oper. This file will create the necessary forward and inverted indexes for the cluster. You would need two additional files: semanticNetworkFile.txt and meshSemanticAbsMapping.txt. The first file can be downloaded from UMLS and the second one is obtained by running MetaMap on all the individual MeSH terms to get the semantic types. 
*************************************************************************************
Module-2 Creation of Graph Database: 
1. To create the graph, run the indexer.java from neo4jGraphIndexer. The first parameter is the path to whole.txt is the output folder created and the second parameter is the path  of the graph database output.
*************************************************************************************
Module-3 Training Set Creation for Closed Discovery
1. To generate similar pairs run GraphTrainingSet.java
2. To generate training set run TrainingSetDriver.java
Both these files require the user to set the paths manually. This module is up next for sanitization by the developers.
3. Use DD_tools Matlab package to perform SOM based single class classification. 
*************************************************************************************
Module-4 Training Set Creation for Open Discovery
1) To perform experiments in open discovery setting run openDiscovery.java. The mode to run it is similar. This module is also up next for sanitization by the developers.
*************************************************************************************



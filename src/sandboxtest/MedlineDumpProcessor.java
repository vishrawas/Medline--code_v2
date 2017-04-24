package sandboxtest;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
//import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by super-machine on 11/30/16.
 */
public class MedlineDumpProcessor {

    public void dumpProcessor(String ipPath, String opPath) {
        try {
            String opPathTemp = opPath + File.separator + "whole.txt";
//            helperClass helper = new helperClass();
//            helper.deleteFolder(new File(opPathTemp));

            File folder = new File(ipPath);
            File[] listOfFiles = folder.listFiles();
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    if (!listOfFiles[i].getName().equals(".DS_Store")) {
                        processIndividualXMLFile(listOfFiles[i].toPath(), opPath + File.separator + "whole.txt");
                    }
                    System.out.println("File " + listOfFiles[i].getName());
                }
            }

//            try(Stream<Path> paths = Files.walk(Paths.get(ipPath))) {
//                paths.forEach(filePath -> {
//                    if (Files.isRegularFile(filePath)) {
//                        processIndividualXMLFile(filePath,opPath+File.separator+"whole.txt");
//                    }
//                });
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processIndividualXMLFile(Path filePath, String opPath) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            System.out.println(filePath);
            File f = new File(filePath.toString());
            ArrayList<String> linesToWrite = new ArrayList<>();
            org.w3c.dom.Document doc = null;

            doc = dBuilder.parse(f);
            doc.getDocumentElement().normalize();
            NodeList nList = returnNodeList("MedlineCitation", doc);
            for (int citationCounter = 0; citationCounter < nList.getLength(); citationCounter++) {
                xmlHelper xHelper = new xmlHelper(filePath, nList, citationCounter);
                int date = xHelper.getDate();
                String pubMedID = xHelper.getId();
                String title = xHelper.getTitle();
                ArrayList<String> meshTerms = xHelper.getMeshTerms();
                if (!meshTerms.isEmpty() && date != 0) {
                    StringBuilder sb = new StringBuilder();

                    for (String s : meshTerms) {
                        sb.append(s);
                        sb.append("$$");
                    }
                    String toWrite = pubMedID + "\t" + title + "\t" + sb.toString() + "\t" + date;
                    linesToWrite.add(toWrite);
                    if (linesToWrite.size() % 5000 == 0) {
                        appendStringToFile(linesToWrite, opPath);
                        linesToWrite.clear();
                    }
                }
            }
            appendStringToFile(linesToWrite, opPath);
            linesToWrite.clear();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void appendStringToFile(ArrayList<String> linesToWrite, String opPath) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(opPath, true));
            for (String line : linesToWrite) {
                bw.append(line);
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static NodeList returnNodeList(String tagName, org.w3c.dom.Document doc) {
        return doc.getElementsByTagName(tagName);
    }

    public class xmlHelper {

        String id;

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getAbstractText() {
            return abstractText;
        }

        public int getDate() {
            return date;
        }

        public ArrayList<String> getMeshTerms() {
            return meshTerms;
        }

        String title = "";
        String abstractText = "";
        int date;
        ArrayList<String> meshTerms;

        public xmlHelper(Path path, NodeList nList, int temp) {

            Node nNode = nList.item(temp);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                NodeList nodeList = null;
                Element eElement = (Element) nNode;
                this.id = returnSingleNodeVal("PMID", nodeList, eElement);
                this.title = returnSingleNodeVal("ArticleTitle", nodeList, eElement);
                this.abstractText = returnAbstract(nodeList, eElement);
                this.date = returnDate(nodeList, eElement);
                this.meshTerms = new ArrayList<>();

                nodeList = eElement.getElementsByTagName("MeshHeadingList");
                if (nodeList.getLength() > 0) {
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        NodeList childList = nodeList.item(i).getChildNodes();
                        for (int j = 0; j < childList.getLength(); j++) {
                            Node childNode = childList.item(j);
                            if ("MeshHeading".equals(childNode.getNodeName())) {
                                NodeList names = childNode.getChildNodes();
                                for (int k = 0; k < names.getLength(); k++) {
                                    Node tempNode = names.item(k);
                                    if ("DescriptorName".equals(tempNode.getNodeName())) {
                                        if (tempNode.getTextContent().trim().equals("") == false) {
                                            String val = tempNode.getAttributes().getNamedItem("MajorTopicYN").getNodeValue();
                                            meshTerms.add(tempNode.getTextContent().toLowerCase().trim());
                                        }
                                    }
                                }
                            }
                        }

                    }
                }

            }

        }

        private String returnSingleNodeVal(String name, NodeList nodeList, Element eElement) {
            nodeList = eElement.getElementsByTagName(name);
            if (nodeList.getLength() > 0) {
                return (eElement.getElementsByTagName(name).item(0).getTextContent());
            }
            return null;
        }

        private String returnAbstract(NodeList nodeList, Element eElement) {
            nodeList = eElement.getElementsByTagName("Abstract");
            String abstractText = "";
            if (nodeList.getLength() > 0) {
                Element innerAbstractElement = (Element) nodeList.item(0);
                if (innerAbstractElement.getElementsByTagName("AbstractText").getLength() > 0) {
                    abstractText = innerAbstractElement.getElementsByTagName("AbstractText").item(0).getTextContent();
                }
            }
            return abstractText;
        }

        private int returnDate(NodeList nodeList, Element eElement) {
            nodeList = eElement.getElementsByTagName("PubDate");
            int Year = 0;

            if (nodeList.getLength() > 0) {
                Element innerAbstractElement = (Element) nodeList.item(0);
                if (innerAbstractElement.getElementsByTagName("Year").getLength() > 0) {
                    Year = Integer.parseInt(innerAbstractElement.getElementsByTagName("Year").item(0).getTextContent());
                }
            }
            return Year;
        }
    }
}

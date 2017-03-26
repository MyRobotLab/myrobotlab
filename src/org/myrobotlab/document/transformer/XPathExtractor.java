package org.myrobotlab.document.transformer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.myrobotlab.document.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This stage will load a config file that contains a field name to xpath
 * expression mapping. The xpaths will be applied to xml data on the document
 * and the extracted values will be mapped to the appropriate fields on the
 * document based on the config
 * 
 * @author kwatters
 *
 */
public class XPathExtractor extends AbstractStage {

  protected String xmlField = "xml";
  protected String configFile = "config/xpaths.txt";
  // mapping of field name to the xpaths that evaluate for its extraction
  protected HashMap<XPathExpression, ArrayList<String>> xpaths = new HashMap<XPathExpression, ArrayList<String>>();
  protected boolean useNamespaces = true;
  private DocumentBuilderFactory factory;
  private DocumentBuilder builder;
  private XPathFactory xpathFactory;
  private XPath xpath;
  // TODO: move this to the base class.
  private boolean debug = false;

  @Override
  public void startStage(StageConfiguration config) {

    if (config != null) {
      xmlField = config.getProperty("xmlField", "xml");
      configFile = config.getProperty("configFile", "config/xpaths.txt");
      useNamespaces = Boolean.valueOf(config.getProperty("useNamespaces", "true"));
    }

    factory = DocumentBuilderFactory.newInstance();
    // TODO: do we really care about name spaces (they can be a pain sometimes)
    factory.setNamespaceAware(useNamespaces);
    try {
      builder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    xpathFactory = XPathFactory.newInstance();
    xpath = xpathFactory.newXPath();
    // TODO Auto-generated method stub
    try {
      xpaths = loadConfig(configFile);
    } catch (XPathExpressionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  @Override
  public List<Document> processDocument(Document doc) {
    // TODO Auto-generated method stub

    for (Object o : doc.getField(xmlField)) {
      // TODO: this is bad , lets cast
      String xml = (String) o;
      try {
        processXml(xml, doc);
      } catch (XPathExpressionException | SAXException | IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        continue;
      }

    }
    return null;
  }

  private void processXml(String xml, Document doc) throws SAXException, IOException, XPathExpressionException {
    // Ok. now for each of the configured xpaths, we want to parse the xml
    // evaluate the xpaths expressions and put the values into the mrl documnet
    // object.
    InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    org.w3c.dom.Document xmldoc = builder.parse(stream);
    // TODO: iterate the xpaths..
    for (XPathExpression xpath : xpaths.keySet()) {
      NodeList nodes = (NodeList) xpath.evaluate(xmldoc, XPathConstants.NODESET);
      for (int i = 0; i < nodes.getLength(); i++) {
        for (String fieldName : xpaths.get(xpath)) {
          // add the evaluated xpath to the fields that this xpath maps to.

          doc.addToField(fieldName, nodes.item(i).getTextContent());
        }
      }
    }
  }

  protected HashMap<XPathExpression, ArrayList<String>> loadConfig(String filename) throws XPathExpressionException {

    HashMap<XPathExpression, ArrayList<String>> configMap = new HashMap<XPathExpression, ArrayList<String>>();
    FileInputStream fstream;
    try {
      fstream = new FileInputStream(filename);
    } catch (FileNotFoundException e) {
      System.out.println("XPATH Extractor config file not found: " + filename);
      e.printStackTrace();
      return null;
    }
    DataInputStream in = new DataInputStream(fstream);
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    String strLine;
    // Read File Line By Line
    try {
      while ((strLine = br.readLine()) != null) {
        // ignore white space
        strLine = strLine.trim();
        // ignore commented out lines
        if (strLine.matches("^#.*")) {
          continue;
        }
        // skip blank lines
        if (strLine.length() == 0) {
          continue;
        }
        String fieldName = strLine.split(",")[0];
        int offset = fieldName.length() + 1;
        String strXPath = strLine.substring(offset, strLine.length());

        // compile the
        XPathExpression xPath = xpath.compile(strXPath);

        if (debug) {
          System.out.println("Adding XPATH " + strXPath + " Maps To : " + fieldName);
        }

        if (configMap.containsKey(xPath)) {
          configMap.get(xPath).add(fieldName);
        } else {
          ArrayList<String> fields = new ArrayList<String>();
          fields.add(fieldName);
          configMap.put(xPath, fields);
        }
      }
    } catch (IOException e) {
      System.out.println("IO Exception reading from file " + filename);
      e.printStackTrace();
      // return what we can...
      return configMap;
    }
    // try to not leak some file handles.
    try {
      br.close();
    } catch (IOException e) {
      System.out.println("Exception occured when trying to close the config file..");
      e.printStackTrace();
    }

    return configMap;
  }

  @Override
  public void stopStage() {
    // TODO Auto-generated method stub

  }

  @Override
  public void flush() {
    // no batching in this transformer. no need to flush?
  }

  public String getXmlField() {
    return xmlField;
  }

  public void setXmlField(String xmlField) {
    this.xmlField = xmlField;
  }

  public String getConfigFile() {
    return configFile;
  }

  public void setConfigFile(String configFile) {
    this.configFile = configFile;
  }

  public boolean isUseNamespaces() {
    return useNamespaces;
  }

  public void setUseNamespaces(boolean useNamespaces) {
    this.useNamespaces = useNamespaces;
  }

}

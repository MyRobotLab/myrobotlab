package org.myrobotlab.document.transformer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.myrobotlab.document.transformer.StageConfiguration;
import org.myrobotlab.document.Document;

public class XPathExtractor extends AbstractStage {


	protected String xmlField = "xml";
	protected String configFile = "config/xpaths.txt";
	protected final String TRIM_OUTPUT_KEY_VALUE = "xpath_vars_to_exclude_trim";
	protected final String TRIM_OUTPUT_KEY_VALUE_SEPARATOR = ",";
	protected HashMap<String, ArrayList<String>> xpaths = new HashMap<String, ArrayList<String>>();
	protected HashSet<String> xpathFilterList = new HashSet<String>();
	protected boolean useNamespaces = false;

	private boolean debug = false;
	@Override
	public void startStage(StageConfiguration config) {
		// TODO Auto-generated method stub
		 xpaths = loadConfig(configFile);
		 
	}

	@Override
	public void processDocument(Document doc) {
		// TODO Auto-generated method stub

		for (Object o : doc.getField(xmlField)) {
			// TODO: this is bad , lets cast
			String xml = (String)o;
		    processXml(xml, doc);
		
		}
		
	}

	
    private void processXml(String xml, Document doc) {
		// TODO Auto-generated method stub
		
	}

	protected HashMap<String, ArrayList<String>> loadConfig(String filename) {

        HashMap<String, ArrayList<String>> configMap = new HashMap<String, ArrayList<String>>();
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
                if (strLine.matches("^#")) {
                    continue;
                }
                // skip blank lines
                if (strLine.length() == 0) {
                    continue;
                }
                String fieldName = strLine.split(",")[0];
                int offset = fieldName.length() + 1;
                String xPath = strLine.substring(offset, strLine.length());
                if (debug) {
                    System.out.println("Adding XPATH " + xPath + " Maps To : " + fieldName);
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
		// TODO Auto-generated method stub

	}

}

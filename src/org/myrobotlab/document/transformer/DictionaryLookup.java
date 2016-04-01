package org.myrobotlab.document.transformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.myrobotlab.document.Document;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;


public class DictionaryLookup extends AbstractStage {
	
	public final static Logger log = LoggerFactory.getLogger(DictionaryLookup.class.getCanonicalName());
	
	private String inputField;
	private String outputField;
	private String dictionaryFile;
	private HashMap<String,String> dictionary;
	
	@Override
	public void startStage(StageConfiguration config) {
		
		if (config != null) {
			inputField = config.getProperty("inputField", "text");
			outputField = config.getProperty("outputField", "entity");
			dictionaryFile = config.getProperty("dictionaryFile", "mydict.csv");
		}
		try {
			dictionary = DictionaryLoader.getInstance().loadDictionary(dictionaryFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	@Override
	public List<Document> processDocument(Document doc) {
		// TODO Auto-generated method stub
		if (!doc.hasField(inputField)) {
			// no values.
			return null;
		}
		
		ArrayList<String> lookedupValues = new ArrayList<String>();
		for (Object o : doc.getField(inputField)) {
			//log.info("Looking up {}", o);
			String val = dictionary.get(o.toString());
			if (val != null) {
				//log.info("Looked up value {}", val);
				lookedupValues.add(val);
//				doc.addToField(outputField, val);
			} else {
				// TODO: revisit all of this logic
				// we shouldnt add the unmatched keys to the lookedup values.
				// log.info("Did not match lookup value {}", val);
				lookedupValues.add(o.toString());
			}
		}
		
		// TODO:generalize the input/output overwrite behavior in these stages.
		if (inputField.equals(outputField)) {
			// overwrite the values with the looked up values?!
			doc.removeField(outputField);
		}
		
		for (String value : lookedupValues) {
			// log.info("Setting Looked up value: {}", value);
			doc.addToField(outputField, value);
		}
		// this stage doesn't emit child docs.
		return null;
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

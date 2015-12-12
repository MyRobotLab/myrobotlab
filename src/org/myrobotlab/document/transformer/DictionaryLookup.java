package org.myrobotlab.document.transformer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.myrobotlab.document.Document;


public class DictionaryLookup extends AbstractStage {
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
		for (Object o : doc.getField(inputField)) {
			String val = dictionary.get(o.toString());
			if (val != null) {
				doc.addToField(outputField, val);
			}
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

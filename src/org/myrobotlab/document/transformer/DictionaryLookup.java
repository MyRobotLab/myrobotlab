package org.myrobotlab.document.transformer;

import java.io.IOException;
import java.util.HashMap;

import org.myrobotlab.document.Document;


public class DictionaryLookup extends AbstractStage {
	private String inputField;
	private String outputField;
	private String dictionaryFile;
	private HashMap<String,String> dictionary;
	
	@Override
	public void startStage(StageConfiguration config) {
		// TODO Auto-generated method stub
		try {
			dictionary = DictionaryLoader.getInstance().loadDictionary(dictionaryFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	@Override
	public void processDocument(Document doc) {
		// TODO Auto-generated method stub
		for (Object o : doc.getField(inputField)) {
			String val = dictionary.get(o.toString());
			if (val != null) {
				doc.addToField(outputField, val);
			}
		}
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

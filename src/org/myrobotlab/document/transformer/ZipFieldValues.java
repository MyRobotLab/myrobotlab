package org.myrobotlab.document.transformer;

import java.util.List;

import org.eclipse.jetty.util.log.Log;
import org.myrobotlab.document.Document;

public class ZipFieldValues extends AbstractStage {

	private String fieldA;
	private String fieldB;
	private String outputField;
	
	@Override
	public void startStage(StageConfiguration config) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processDocument(Document doc) {
		// TODO : double check the logics here. is this what we actually want?
		List<Object> a = doc.getField(fieldA);
		List<Object> b = doc.getField(fieldB);		
		if (a.size() != b.size()) {
			// This isn't good!
			//Log.warn("field a and field b are different lengths");
			return;
		}
		for (int i = a.size(); i < a.size() ; i++) {
			doc.addToField(outputField, a.get(i));
			doc.addToField(outputField, b.get(i));
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

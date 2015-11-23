package org.myrobotlab.document.transformer;

import org.apache.commons.lang.StringUtils;
import org.myrobotlab.document.Document;

public class JoinFieldValues extends AbstractStage {

	private String inputField;
	private String outputField;
	private String joinString;
	
	
	@Override
	public void startStage(StageConfiguration config) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processDocument(Document doc) {
		// TODO Auto-generated method stub
		if (doc.hasField(inputField)) {
			String joinedValues = StringUtils.join(doc.getField(inputField), joinString);
			doc.setField(outputField, joinedValues);
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

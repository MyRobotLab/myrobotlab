package org.myrobotlab.document.transformer;

import java.util.HashSet;

import org.myrobotlab.document.Document;

public class UniqueFieldValues extends AbstractStage {

	private String fieldName;
	
	@Override
	public void startStage(StageConfiguration config) {
		// NoOp
	}

	@Override
	public void processDocument(Document doc) {
		
		HashSet<Object> unique = new HashSet<Object>();
		for (Object o : doc.getField(fieldName)) {
			unique.add(o);
		}
		doc.removeField(fieldName);
		for (Object o : unique) {
			doc.addToField(fieldName, o);
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

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

}

package org.myrobotlab.document.transformer;

import org.myrobotlab.document.transformer.StageConfiguration;

import java.util.List;

import org.myrobotlab.document.Document;

/**
 * This will set a field on a document with a value
 * 
 * @author kwatters
 *
 */
public class SetStaticFieldValue extends AbstractStage {

	private String fieldName = null;
	private String value = null;
	
	@Override
	public void startStage(StageConfiguration config) {
		
		if (config != null) {
			fieldName = config.getStringParam("fieldName");
			value = config.getStringParam("value");
		}
	}

	@Override
	public List<Document> processDocument(Document doc) {
		doc.addToField(fieldName, value);
		return null;
	}

	@Override
	public void stopStage() {
		// TODO Auto-generated method stub
	}

	@Override
	public void flush() {
		// Only required if this stage does any batching.  NO-OP here.
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}

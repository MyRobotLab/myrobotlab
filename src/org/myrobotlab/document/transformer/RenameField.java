package org.myrobotlab.document.transformer;

import java.util.List;

import org.myrobotlab.document.Document;

/**
 * This stage will rename the field on a document.
 * @author kwatters
 *
 */
public class RenameField extends AbstractStage {

	private String oldName = "fielda";
	private String newName = "fieldb";
	@Override
	public void startStage(StageConfiguration config) {
		// TODO Auto-generated method stub
		if (config != null) {
			oldName = config.getProperty("oldName");
			newName = config.getProperty("newName");
		}
	}

	@Override
	public List<Document> processDocument(Document doc) {
		if (!doc.hasField(oldName)) {
			return null;
		}
		for (Object o : doc.getField(oldName)) {
			doc.addToField(newName, o);
		}
		doc.removeField(oldName);
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

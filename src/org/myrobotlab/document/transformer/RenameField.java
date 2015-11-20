package org.myrobotlab.document.transformer;

import org.myrobotlab.document.transformer.StageConfiguration;
import org.myrobotlab.document.Document;

public class RenameField extends AbstractStage {

	private String oldName = "fielda";
	private String newName = "fieldb";
	@Override
	public void startStage(StageConfiguration config) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processDocument(Document doc) {

		if (!doc.hasField(oldName)) {
			return;
		}
		doc.addToField(newName, doc.getField(oldName));
		doc.removeField(oldName);
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

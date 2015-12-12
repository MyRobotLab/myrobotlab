package org.myrobotlab.document.transformer;


import java.util.List;
import org.myrobotlab.document.Document;

public abstract class AbstractStage {

	public abstract void startStage(StageConfiguration config);
	public abstract List<Document> processDocument(Document doc);
	public abstract void stopStage();
	public abstract void flush();

}

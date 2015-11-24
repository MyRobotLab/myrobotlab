package org.myrobotlab.document.connector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.myrobotlab.document.Document;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.interfaces.DocumentConnector;
import org.myrobotlab.service.interfaces.DocumentListener;
import org.myrobotlab.service.interfaces.DocumentPublisher;
import org.python.modules.synchronize;

/**
 * 
 * AbstractConnector - base class for implementing a new document connector service.
 * 
 */
public abstract class AbstractConnector extends Service implements DocumentPublisher, DocumentConnector {

	private static final long serialVersionUID = 1L;

	protected ConnectorState state = ConnectorState.STOPPED;
	private int batchSize = 1;
	private List<Document> batch = Collections.synchronizedList(new ArrayList<Document>());

	private String docIdPrefix = "";
	
	public AbstractConnector(String name) {
		super(name);
	}

	public void feed(Document doc) {
		// System.out.println("Feeding document " + doc.getId());
		// TODO: add batching and change this to publishDocuments (as a list)
		// Batching for this sort of stuff is a very good thing.
		if (batchSize <= 1) {
			invoke("publishDocument", doc);
		} else {
			// handle the batch
			// TODO: make this synchronized and thread safe!
			batch.add(doc);
			if (batch.size() >= batchSize) {
				flush();
			}
		}
	}

	public void flush() {
		// flush any partial batch
		// TODO: make this thread safe!
		invoke("publishDocuments", batch);
		// reset/clear the batch.
		batch = new ArrayList<Document>();
		while (getOutbox().size() > 0) {
			// TODO: wait until the outbox is empty.
			log.info("Draining out box Size: {}", getOutbox().size());
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			continue;
		}
	}

	public ConnectorState getState() {
		return state;
	}
	public void setState(ConnectorState state) {
		this.state = state;
	}

	public Document publishDocument(Document doc) {
		return doc;
	}

	public List<Document> publishDocuments(List<Document> batch) {
		return batch;
	}

	public void addDocumentListener(DocumentListener listener) {
		addListener("publishDocument", listener.getName(), "onDocument");
		addListener("publishDocuments", listener.getName(), "onDocuments");
	}

	public ConnectorState getConnectorState() {
		return state;
	}

	@Override
	public String[] getCategories() {
		return new String[]{"data"};
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public String getDocIdPrefix() {
		return docIdPrefix;
	}

	public void setDocIdPrefix(String docIdPrefix) {
		this.docIdPrefix = docIdPrefix;
	}

}

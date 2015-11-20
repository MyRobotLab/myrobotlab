package org.myrobotlab.service.interfaces;

import org.myrobotlab.document.Document;
import org.myrobotlab.document.connector.ConnectorState;
import org.myrobotlab.framework.Service;

/**
 * 
 * AbstractConnector - base class for implementing a new document connector service.
 * 
 */
public abstract class AbstractConnector extends Service implements DocumentPublisher, DocumentConnector {

	private static final long serialVersionUID = 1L;
	
	protected ConnectorState state = ConnectorState.STOPPED;

	public AbstractConnector(String name) {
		super(name);
	}
	
	public void feed(Document doc) {
		// System.out.println("Feeding document " + doc.getId());
		// TODO: add batching and change this to publishDocuments (as a list)
		// Batching for this sort of stuff is a very good thing.
		invoke("publishDocument", doc);
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

	public void addDocumentListener(DocumentListener listener) {
		System.out.println("FOO:" + listener.getName());
		this.addListener("publishDocument", listener.getName(), "onDocument");
	}
	
	public ConnectorState getConnectorState() {
		return state;
	}
}

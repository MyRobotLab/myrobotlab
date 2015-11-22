package org.myrobotlab.document.transformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;

import org.myrobotlab.document.transformer.StageConfiguration;
import org.myrobotlab.document.Document;

public class SendToSolr extends AbstractStage {

	private String idField = "id";
	private String fieldsField = "fields";
	private SolrServer solrServer = null;
	private String solrUrl = "http://localhost:8983/solr/collection1";
	private boolean issueCommit = true;

	private int batchSize = 100;
	//private LinkedBlockingQueue<SolrInputDocument> batch = new LinkedBlockingQueue<SolrInputDocument>(); 
	// Synchronized list. needed for thread safety.
	private List<SolrInputDocument> batch = Collections.synchronizedList(new ArrayList<SolrInputDocument>()); 
	
	// Batch size +/-
	
	@Override
	public void startStage(StageConfiguration config) {
		solrUrl = config.getProperty("solrUrl", solrUrl);
		System.out.println("Init connection to solr at : " + solrUrl);
		// Initialize a connection to the solr server on startup.
		if (solrServer == null) {
			// TODO: support an embeded solr instance
			solrServer = new HttpSolrServer( solrUrl );
		} else {
			System.out.println("Solr instance already created.");
		}
	}

	@Override
	public void processDocument(Document doc) {
		SolrInputDocument solrDoc = new SolrInputDocument();

		// set the id field on the solr doc
		String docId = doc.getId();

		// HashSet<String> fields = new HashSet<String>();
		for (String fieldName : doc.getFields()) {
			for (Object value: doc.getField(fieldName)) {
				solrDoc.addField(fieldName, value);
			}
			solrDoc.addField(fieldsField, fieldName);
			// fields.add(fieldName);
		}

		// prevent id field duplicate values.
		// remove the id field if it was set,
		solrDoc.removeField("id");
		// make sure we add it back
		solrDoc.setField("id", docId);

		// I guess we have the full document, we should send it
		//ArrayList<SolrInputDocument> solrDocs = new ArrayList<SolrInputDocument>();
		//solrDocs.add(solrDoc);
		try {
			synchronized (batch) {
				batch.add(solrDoc);
				if (batch.size() >= batchSize) {
					//System.out.println("Solr Server Flush Batch...");
					// you are blocking?!
					solrServer.add(batch);
					//System.out.println("Solr batch sent..");
					batch.clear();
				} else {
					//System.out.println("Batch Size " + batch.size());
				}
			}
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		// TODO: NO COMMITS HERE!
		// solrServer.commit();


	}

	@Override
	public void stopStage() {
		// TODO Auto-generated method stub
		flush();

	}

	public synchronized void flush() {

		// Is this where I should flush the last batch?
		if (solrServer!=null && batch.size() > 0) {
			try {
				System.out.println("flushing last batch.");
				solrServer.add(batch);
			} catch (SolrServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				batch.clear();
			}
		}

		// TODO: should we commit on flush?
		try {
			if (issueCommit) {
				System.out.println("Committing solr");
				solrServer.commit();
			}
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// super.flush();


	}
}
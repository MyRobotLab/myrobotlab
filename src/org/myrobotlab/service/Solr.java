package org.myrobotlab.service;

import java.io.IOException;
import java.util.Collection;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * SolrService - MyRobotLab This is an integration of Solr into MyRobotLab. Solr
 * is the popular, blazing-fast, open source enterprise search platform built on
 * Apache Lucene.
 * 
 * This service exposes a the solrj client to be able to add documents and query
 * a solr server that is running.
 * 
 * For More info about Solr see http://lucene.apache.org/solr/
 * 
 * @author kwatters
 *
 */
public class Solr extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Solr.class);

	public String solrUrl = "http://localhost:8983/solr";

	transient private HttpSolrServer solrServer;

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			Solr solr = (Solr) Runtime.start("solr", "Solr");
			Runtime.start("gui", "GUIService");

			// Create a test document
			SolrInputDocument doc = new SolrInputDocument();
			doc.setField("id", "Doc1");
			doc.setField("title", "My title");
			doc.setField("content", "This is the text field, for a sample document in myrobotlab.  ");

			// add the document to the index
			solr.addDocument(doc);
			// commit the index
			solr.commit();

			// search for the word myrobotlab
			String queryString = "myrobotlab";
			QueryResponse resp = solr.search(queryString);

			for (int i = 0; i < resp.getResults().size(); i++) {
				System.out.println("---------------------------------");
				System.out.println("-- Printing Result number :" + i);
				// grab a document out of the result set.
				SolrDocument d = resp.getResults().get(i);
				// iterate over the fields on the returned document
				for (String fieldName : d.getFieldNames()) {

					System.out.print(fieldName + "\t");
					// fields can be multi-valued
					for (Object value : d.getFieldValues(fieldName)) {
						System.out.print(value);
						System.out.print("\t");
					}
					System.out.println("");
				}
			}
			System.out.println("---------------------------------");
			System.out.println("Done.");

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public Solr(String n) {
		super(n);
	}

	public void addDocument(SolrInputDocument doc) {
		try {

			solrServer.add(doc);
		} catch (SolrServerException e) {
			// TODO : retry?
			log.warn("An exception occurred when trying to add document to the index.", e);
		} catch (IOException e) {
			// TODO : maybe retry?
			log.warn("A network exception occurred when trying to add document to the index.", e);
		}
	}

	/**
	 * Add a solr document to the index
	 * 
	 * @param docs
	 */
	public void addDocuments(Collection<SolrInputDocument> docs) {
		try {
			solrServer.add(docs);
		} catch (SolrServerException e) {
			log.warn("An exception occurred when trying to add documents to the index.", e);
		} catch (IOException e) {
			log.warn("A network exception occurred when trying to add documents to the index.", e);
		}
	}

	/**
	 * Commit the solr index and make documents that have been submitted become
	 * searchable.
	 */
	public void commit() {
		try {
			solrServer.commit();
		} catch (SolrServerException e) {
			log.warn("An exception occurred when trying to commit the index.", e);
		} catch (IOException e) {
			log.warn("A network exception occurred when trying to commit the index.", e);
		}
	}

	public void deleteDocument(String docId) {
		try {
			solrServer.deleteById(docId);
		} catch (Exception e) {
			// TODO better error handling/reporting?
			log.warn("An exception occurred when deleting doc", e);
		}
	}

	@Override
	public String[] getCategories() {
		return new String[] { "data", "search" };
	}

	@Override
	public String getDescription() {
		return "Solr Service - Open source search engine.";
	}

	/**
	 * The url for the solr instance you wish to query. Defaults to
	 * http://localhost:8983/solr
	 * 
	 * @return
	 */

	public String getSolrUrl() {
		return solrUrl;
	}

	/**
	 * Optimize the index, if the index gets very fragmented, this helps
	 * optimize performance and helps reclaim some disk space.
	 */
	public void optimize() {
		try {
			// TODO: expose the num segements and stuff?
			solrServer.optimize();
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			log.warn("An error occurred when optimizing the index.", e);
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.warn("A network error occurred when optimizing the index, solr down?", e);
			e.printStackTrace();
		}
	}

	/**
	 * Pass in custom solr query parameters and execute that query.
	 * 
	 * @param query
	 * @return
	 */
	public QueryResponse search(SolrQuery query) {
		QueryResponse resp = null;
		try {
			resp = solrServer.query(query);
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return resp;
	}

	/**
	 * Default query to fetch the top 10 documents that match the query request.
	 * 
	 * @param queryString
	 * @return
	 */
	public QueryResponse search(String queryString) {
		// default to 10 hits returned.
		return search(queryString, 10, 0);
	}

	/**
	 * Default query to fetch the top 10 documents that match the query request.
	 * 
	 * @param queryString
	 * @return
	 */
	public QueryResponse search(String queryString, int rows, int start) {
		SolrQuery query = new SolrQuery();
		query.set("q", queryString);
		query.setRows(rows);
		query.setStart(start);
		QueryResponse resp = null;
		try {
			resp = solrServer.query(query);
		} catch (SolrServerException e) {
			log.warn("Search failed with exception", e);
		}
		return resp;
	}

	/**
	 * Set the url for the solr instance to communicate with.
	 * 
	 * @param solrUrl
	 */
	public void setSolrUrl(String solrUrl) {
		this.solrUrl = solrUrl;
		// TODO: this isn't good to include behavior here but
		// if someone switches the url, we want to re-create the solr server.
		// this breaks the bean pattern a bit..
		if (solrServer != null) {
			solrServer = new HttpSolrServer(solrUrl);
		}
	}

	@Override
	public void startService() {
		super.startService();
		solrServer = new HttpSolrServer(solrUrl);
	}

	@Override
	public Status test() {
		// TODO: ?
		return super.test();
	}

}

package org.myrobotlab.deeplearning4j;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.datavec.api.io.filters.PathFilter;
import org.datavec.api.split.BaseInputSplit;
import org.datavec.api.split.InputSplit;
import org.myrobotlab.service.Solr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An inputsplit for deeplearning that uses a solr query to provide the training
 * and testing datasets
 * 
 * @author kwatters
 *
 */
// TODO: change this to an InputStreamInputSplit instead!!!
public class SolrInputSplit extends BaseInputSplit {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Solr solr;
  private final SolrQuery query;
  private final String bytesField = "bytes";
  private final String labelField;
  private QueryResponse response;
  private HashMap<String, byte[]> byteMap;
  private HashMap<String, String> labelMap;
  private final List<String> labels;

  // we probably need a constructor ? that takes a solr server in mrl?
  public SolrInputSplit(Solr solr, SolrQuery query, List<String> labels, String labelField) {
    this.solr = solr;
    this.query = query;
    // TODO: i guess we're going to infer this so maybe we don't need it passed
    // in?
    this.labels = labels;
    this.labelField = labelField;
    // We should execute a result set
    response = solr.search(query);
    // should compile this down to make it faster
    byteMap = new HashMap<String, byte[]>();
    labelMap = new HashMap<String, String>();
    for (SolrDocument doc : response.getResults()) {
      String id = doc.getFirstValue("id").toString();
      byte[] bytes = (byte[]) doc.getFirstValue(bytesField);
      byteMap.put(id, bytes);
      String label = (String) doc.getFirstValue(labelField);
      labelMap.put(id, label);
    }
    log.info("Found {} example documents", response.getResults().size());
  }

  @Override
  public void bootStrapForWrite() {
    // TODO Auto-generated method stub
    log.info("Bootstrap for write.");
  }

  @Override
  public boolean needsBootstrapForWrite() {
    // TODO Auto-generated method stub
    log.info("needs bootstrap for write.");
    return false;
  }

  @Override
  public InputStream openInputStreamFor(String location) throws Exception {
    // I guess this is supposed to fetch a document by ID and return it as a
    // stream?
    // in the case of an image , i guess it's the
    // log.info("Open input stream for {}", location);
    // here we want to fetch a doc by id from the result set.
    // for now. just always returning the first result?!
    // need to parse the doc id out of the location
    // TODO: this sucks.
    String locationId = location.split("\\\\")[2];
    // TODO: Ick! we need to iterate the result set to find which one is this
    // doc
    byte[] bytes = byteMap.get(locationId);
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    return bais;
  }

  @Override
  public OutputStream openOutputStreamFor(String location) throws Exception {
    // TODO: writeable locations! no!!
    log.info("Open output stream {}", location);
    return null;
  }

  @Override
  public void reset() {
    // TODO:set current offset to zero ?
    log.info("Reset...");

  }

  @Override
  public boolean resetSupported() {
    // TODO Auto-generated method stub
    log.info("reset supported?");
    // why not.. just reset the current offset i guess?
    return true;
  }

  @Override
  public void updateSplitLocations(boolean arg0) {
    log.info("Update split locations?!");
  }

  @Override
  public boolean canWriteToLocation(URI location) {
    log.info("Can write to uri: {}", location);
    return super.canWriteToLocation(location);
  }

  @Override
  public String addNewLocation() {
    log.info("Add new location?");
    return super.addNewLocation();
  }

  @Override
  public String addNewLocation(String location) {
    log.info("Add new location?!");
    return super.addNewLocation(location);
  }

  @Override
  public URI[] locations() {
    log.info("Are we called anymore?!");
    // TODO Auto-generated method stub
    log.info("Locations Return all?!");
    this.uriStrings = new ArrayList<String>();
    URI[] locations = new URI[(int) length()];
    for (int i = 0; i < length(); i++) {
      // create a uri from the doc id.
      String docId = response.getResults().get(i).getFieldValue("id").toString();
      Collection<Object> labels = response.getResults().get(i).getFieldValues(labelField);
      String label = labels.iterator().next().toString();
      label = cleanLabel(label);
      try {
        // URI docUri = new URI("file", "core1", "/"+label +"/"+docId, null);
        URI docUri = new URI("file:///" + label + "/" + docId);
        log.info("DocID : {} Label: {}", docId, label);
        locations[i] = docUri;
        uriStrings.add(docUri.toString());
      } catch (URISyntaxException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return locations;
    // return super.locations();
  }

  private String cleanLabel(String label) {
    // TODO : some way to avoid needing this method!
    String clean = label.replaceAll(" ", "_");
    return clean;
  }

  @Override
  public Iterator<URI> locationsIterator() {
    // TODO Auto-generated method stub
    log.info("URI Iterator for locations..");
    return super.locationsIterator();
  }

  @Override
  public Iterator<String> locationsPathIterator() {
    // TODO: some better / smarter iterator with pagination
    // to limit memory usage for training /testing data
    log.info("Locations path iterator");
    return super.locationsPathIterator();
  }

  @Override
  public long length() {
    // TODO : This really should be the size of the entire result set.
    // log.info("Length called");
    return response.getResults().size();
    // return response.getResults().getNumFound();
  }

  @Override
  public InputSplit[] sample(PathFilter pathFilter, double... weights) {
    log.info("Sample..");
    // TODO: Is this needed? should we continue to use it?
    // more balanced sampling could be a good thing.
    return super.sample(pathFilter, weights);
  }

  public String resolveLabelForID(String docID) {
    // we should error check this ?
    return labelMap.get(docID);
  }

}

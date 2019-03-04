package org.myrobotlab.deeplearning4j;

import java.net.URI;
import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.writable.Text;
import org.datavec.api.writable.Writable;

/**
 * This label generator just asks the solr input split for the lable given a
 * docId
 * 
 * @author kwatters
 *
 */
public class SolrLabelGenerator extends ParentPathLabelGenerator {

  private static final long serialVersionUID = 1L;
  private SolrInputSplit inputSplit;

  public void setSolrInputSplit(SolrInputSplit solrInputSplit) {
    this.inputSplit = solrInputSplit;
  }

  @Override
  public Writable getLabelForPath(String path) {
    // here we could ask the input split what it's label is for a given doc id.
    // TODO: we only want the doc id passed in here!
    String[] parts = path.split("\\\\");
    // last part is the one we want.
    String docId = parts[parts.length - 1];
    // something like (pass path. rather doc id!"
    String label = inputSplit.resolveLabelForID(docId);
    return new Text(label);
  }

  @Override
  public Writable getLabelForPath(URI uri) {
    Writable w = super.getLabelForPath(uri);
    // log.info("URL: " + uri.toString());
    return w;
  }

}

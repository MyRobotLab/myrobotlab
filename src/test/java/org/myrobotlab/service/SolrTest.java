package org.myrobotlab.service;

import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvLoadImage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Assert;
import org.myrobotlab.document.Document;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.Util;
import org.myrobotlab.programab.Response;

public class SolrTest extends AbstractServiceTest {

  // @Test
  public void testImageStoreFetch() throws SolrServerException, IOException {
    Solr solr = (Solr) Runtime.start("solr", "Solr");
    String solrHome = SolrTest.testFolder.getRoot().getAbsolutePath();
    solr.startEmbedded(solrHome);
    solr.deleteEmbeddedIndex();
    // make a document with an IplImage serialized into / out of it. (maybe used
    // a buffered image instead?!)
    String docId = "test_image_doc_1";
    SolrInputDocument imageDoc = makeImageDoc(solr, docId, loadDefaultImage());
    solr.addDocument(imageDoc);
    solr.commit();
    QueryResponse qr = solr.search("id:" + docId);
    // solr.
    IplImage img = solr.fetchImage("id:" + docId);
    Assert.assertNotNull(img);
  }

  private SolrInputDocument makeImageDoc(Solr solr, String docId, IplImage image) throws IOException {
    SolrInputDocument doc = new SolrInputDocument();
    doc.setField("id", docId);
    // load an image from file/resource
    byte[] bytes = Util.imageToBytes(image);
    doc.setField("bytes", bytes);
    return doc;
  }

  private IplImage loadDefaultImage() {
    String path = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "OpenCV" + File.separator + "lena.png";
    IplImage image = cvLoadImage(path);
    return image;
  }

  @Override
  public Service createService() {
    Runtime.install("Solr", true);
    Solr solr = (Solr) Runtime.start("solr", "Solr");
    return solr;
  }

  @Override
  public void testService() throws Exception {
    // LoggingFactory.init("INFO"); please do not do this
    Solr solr = (Solr) service;
    // String solrHome = SolrTest.testFolder.getRoot().getAbsolutePath();
    solr.startEmbedded();
    solr.deleteEmbeddedIndex();
    solr.addDocument(makeTestDoc("doc_1"));
    solr.commit();
    QueryResponse resp = solr.search("*:*");
    Assert.assertEquals(1, resp.getResults().getNumFound());

    solr.deleteDocument("doc_1");
    solr.commit();
    resp = solr.search("*:*");
    Assert.assertEquals(0, resp.getResults().getNumFound());

    ArrayList<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
    docs.add(makeTestDoc("doc_1"));
    docs.add(makeTestDoc("doc_2"));
    solr.addDocuments(docs);
    solr.commit();
    SolrDocument res = solr.getDocById("doc_2");
    Assert.assertEquals("doc_2", res.get("id"));

    Document mrlDoc = new Document("doc_3");
    mrlDoc.setField("title", "Mrl Rocks!");
    // Object o = Arrays.asList(1.0f, 2.5f, 3.7f, 4.1f);
    //Object o2 = Arrays.asList(makeVector(384));
    mrlDoc.setField("vector", makeVector(384));

    solr.onDocument(mrlDoc);
    solr.commit();

    res = solr.getDocById("doc_3");
    Assert.assertEquals("doc_3", res.get("id"));

    // create a message
    Message mrlMessage = Message.createMessage(solr.getName(), null, "fooMethod", new String[] { "foo", "bar" });

    solr.onMessage(mrlMessage);

    solr.commit();

    SolrQuery query = new SolrQuery("message_id:" + mrlMessage.msgId);
    resp = solr.search(query);

    Assert.assertEquals(1, resp.getResults().getNumFound());

    Response programABResponse = new Response("joe", "lloyd", "this is a test response.", null);
    solr.onResponse(programABResponse);

    // what other ones?

    solr.onText("this is some text that was published");

    solr.commit();

    query = new SolrQuery("username:joe");
    resp = solr.search(query);
    Assert.assertEquals(1, resp.getResults().getNumFound());
    
    
    // let's search for our vector
    ArrayList<Float> v = makeVector(384);
   String queryVec = vecToString(v);
    
    
    query = new SolrQuery("{!knn f=vector topK=10}"+queryVec);
    resp = solr.search(query);
    
    // System.out.println(resp);
    Assert.assertEquals(1, resp.getResults().getNumFound());

  }

  private String vecToString(ArrayList<Float> v) {
    // TODO Auto-generated method stub
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int i = 0 ; i < v.size(); i++) {
      sb.append(v.get(i));
      if (i != v.size()-1) {
        sb.append(",");
      }
    }    
    sb.append("]");
    return sb.toString();
  }

  private ArrayList<Float> makeVector (int length) {
    ArrayList<Float> result = new ArrayList<Float>();
    for (int i = 0 ; i < length; i++) {
      result.add( 0.5f );
    }
    return result;
  }
  private SolrInputDocument makeTestDoc(String docId) {
    // TODO Auto-generated method stub
    SolrInputDocument doc = new SolrInputDocument();
    doc.setField("id", docId);
    return doc;
  }

}

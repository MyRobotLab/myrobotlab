package org.myrobotlab.service;

import static org.bytedeco.opencv.global.opencv_imgcodecs.cvLoadImage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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
import org.myrobotlab.framework.repo.Repo;
import org.myrobotlab.logging.LoggingFactory;

// @Ignore
public class SolrTest extends AbstractServiceTest {
  //@Test
  public void testImageStoreFetch() throws SolrServerException, IOException {
    Solr solr = (Solr) Runtime.createAndStart("solr", "Solr");
    String solrHome = SolrTest.testFolder.getRoot().getAbsolutePath();
    solr.startEmbedded(solrHome);
    solr.deleteEmbeddedIndex();
    // make a document with an IplImage serialized into / out of it.  (maybe used a buffered image instead?!)
    String docId = "test_image_doc_1";
    SolrInputDocument imageDoc = makeImageDoc( solr , docId, loadDefaultImage());
    solr.addDocument(imageDoc);
    solr.commit();
    QueryResponse qr = solr.search("id:"+docId);
    //    solr.
    IplImage img = solr.fetchImage("id:"+docId);
    Assert.assertNotNull(img);
    
    //System.out.println("Press the any key");
    //System.out.flush();
    //System.in.read();
    
  }


  
  private SolrInputDocument makeImageDoc(Solr solr , String docId, IplImage image) throws IOException {
    SolrInputDocument doc = new SolrInputDocument();
    doc.setField("id", docId);
    // load an image from file/resource
    byte[] bytes = solr.imageToBytes(image);
    doc.setField("bytes", bytes);
    return doc;
  }

  private IplImage loadDefaultImage() {
    String path = "src"+File.separator+"test"+File.separator+"resources"+File.separator+"OpenCV" + File.separator +"lena.png";
    IplImage image = cvLoadImage(path);
    return image;
  }

  @Override
  public Service createService() {
    Repo.getInstance().install("Solr");
    Solr solr = (Solr)Runtime.start("solr", "Solr");
    return solr;
  }

  @Override
  public void testService() throws Exception {
    // LoggingFactory.init("INFO"); please do not do this
    Solr solr = (Solr)service;
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

    solr.onDocument(mrlDoc);
    solr.commit();
    
    res = solr.getDocById("doc_3");
    Assert.assertEquals("doc_3", res.get("id"));
    
    // create a message 
    Message mrlMessage = Message.createMessage(solr.getName(), null, "fooMethod", new String[] {"foo", "bar"});
    
    solr.onMessage(mrlMessage);
    
    solr.commit();

    SolrQuery query = new SolrQuery("message_id:" + mrlMessage.msgId);
    resp = solr.search(query);
    
    Assert.assertEquals(1, resp.getResults().getNumFound());
    
    ProgramAB.Response programABResponse = new ProgramAB.Response("joe", "lloyd", "this is a test response.", null, new Date());
    solr.onResponse(programABResponse);
    
    // what other ones?
    
    solr.onText("this is some text that was published");
    
    solr.commit();
    
    query = new SolrQuery("username:joe");
    resp = solr.search(query);
    Assert.assertEquals(1, resp.getResults().getNumFound());
    
  }



  private SolrInputDocument makeTestDoc(String docId) {
    // TODO Auto-generated method stub
    SolrInputDocument doc = new SolrInputDocument();
    doc.setField("id", docId);
    return doc;
  }
  
}

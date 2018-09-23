package org.myrobotlab.deeplearning4j;

import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;

import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.apache.solr.client.solrj.SolrQuery.ORDER;

import org.datavec.api.split.InputSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.junit.Test;
import org.myrobotlab.service.Solr;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.VGG16ImagePreProcessor;
import org.nd4j.linalg.factory.Nd4jBackend;
import org.nd4j.linalg.factory.Nd4jBackend.NoAvailableBackendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Deeplearning4j;
import org.myrobotlab.service.Runtime;

//@Ignore
public class SolrDataSetIteratorTest {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected static long seed = 42;
  // VGG16 specifications.
  protected static int channels = 3;
  protected static int height = 224;
  protected static int width = 224;
  // training params
  protected static int maxEpochs = 5;
  protected static int batch = 20;
  // the output layer name for vgg16
  protected static String featureExtractionLayer = "fc2";
  // training details
  double trainPerc = 0.5;
  double targetAccuracy = 0.90;

  // The solr and dl4j services
  private Solr solr;
  private Deeplearning4j dl4j;

  private void initServices() throws SolrServerException, IOException {
    solr = (Solr)Runtime.start("solr", "Solr");
    solr.startEmbedded();
    dl4j = (Deeplearning4j)Runtime.start("dl4j", "Deeplearning4j");
  }

  @Test
  public void testSolrTransferLearningVGG16() throws IOException, NoAvailableBackendException, SolrServerException {
    LoggingFactory.init("INFO");
    System.out.println(System.getProperty("java.library.path"));
    Nd4jBackend.load();
    // start solr and dl4j service.
    initServices();
    // consider increasing this if there's enough training data.
    // Phase 1 . get the metadata from the solr query about how many classes there are, and how many results there are.
    String queryString = "+has_bytes:true -label:unknown";
    String labelField = "label";
    SolrQuery datasetQuery = makeTrainQuery(queryString, labelField);
    // run that query.. get the number of items and the labels
    QueryResponse resp = solr.search(datasetQuery);
    long numFound = resp.getResults().getNumFound();
    // sorted list (according to solr) of the labels for this data set
    List<String> labels = resolveLabels(resp);
    
    SolrQuery trainQuery = datasetQuery;
    // TODO: fix me! i shouldn't be the same query.. 
    SolrQuery testQuery = datasetQuery;
    // Random sort ascending for a percentage
    long trainMaxOffset = (long)((double)numFound * trainPerc);
    long testMaxOffset = (long)((double)numFound * (1.0 - trainPerc));
    // we could just add start/rows for the train set vs the test set?
    trainQuery.addSort("random_"+seed, ORDER.asc);
    trainQuery.setRows((int)trainMaxOffset);
    // change sort order, grab the last N
    testQuery.addSort("random_"+seed, ORDER.desc);
    testQuery.setRows((int)testMaxOffset);

    DataSetIterator trainIter = makeSolrInputSplitIterator(trainQuery, numFound, labels);
    DataSetIterator testIter = makeSolrInputSplitIterator(testQuery, numFound, labels);
    
    // loop for each epoch?
    ComputationGraph vgg16Transfer = dl4j.createVGG16TransferModel(featureExtractionLayer, labels.size());
    vgg16Transfer.addListeners(new ScoreIterationListener(1));
    for (int i = 0 ; i < maxEpochs; i++) {
      dl4j.runFitter(trainIter, vgg16Transfer);
      double accuracy = dl4j.evaluateModel(testIter, vgg16Transfer);
      if (accuracy > targetAccuracy) {
        // ok. if we got here this is a good model.. let's save it
        dl4j.saveModel(vgg16Transfer, labels, "my_new_model.bin");
        break;
      }
    }
    
    testNewModel();
    
  }

  
  private void testNewModel() throws IOException {
    // Ok. now let's see can we load the model up and ask it to predict?
    
    CustomModel newMod = dl4j.loadComputationGraph("my_new_model.bin");
    
    // TODO: load am image!
    // a test image
    String path = "C:\\dev\\workspace\\myrobotlab\\src\\main\\resources\\resource\\OpenCV\\testData\\rachel.jpg";
    IplImage image = cvLoadImage(path);
    
    Map<String, Double> results =  dl4j.classifyImageCustom(image, newMod.getModel(), newMod.getLabels());
    for (String key : results.keySet()) {
      log.info("label: {} : {} ", key, results.get(key));
    }
  }

  // return the sorted set of labels for this training set.
  private List<String> resolveLabels(QueryResponse resp) {
    FacetField labelFacet =  resp.getFacetField("label");
    // maintain sort order with a linked hash set
    List<String> labels = new ArrayList<String>();
    for (Count c : labelFacet.getValues()) {
      labels.add(c.getName());
    }
    Collections.sort(labels);
    return labels;
  }

  private DataSetIterator makeSolrInputSplitIterator(SolrQuery datasetQuery, long numFound, List<String> labels) throws IOException {
    // TODO: pass in the record reader and the preprocessor
    // training set iterator
    SolrInputSplit split = new SolrInputSplit(solr, datasetQuery, labels);
    SolrLabelGenerator labelMaker = new SolrLabelGenerator();
    labelMaker.setSolrInputSplit(split);
    SolrImageRecordReader recordReader = new SolrImageRecordReader(height,width,channels,labelMaker);
    recordReader.setLabels(labels);
    // TODO: This initializes the locations?! ouch.  avoid that.. just use an iterator!
    recordReader.initialize(split);
    DataSetIterator iter = new RecordReaderDataSetIterator(recordReader, batch, 1, labels.size());
    iter.setPreProcessor( new VGG16ImagePreProcessor());
    return iter;    
  }

  // this query returns the superset of all data that will be used for training and testing.
  public SolrQuery makeTrainQuery(String queryString, String labelField) {
    SolrQuery solrQuery = new SolrQuery(queryString);
    // TODO: avoid this and use cursor mark pagination.
    solrQuery.setRows(0);
    // add a facet on the label field so we know what's in the training dataset.
    solrQuery.addFacetField(labelField);
    solrQuery.setFacetMinCount(1);
    solrQuery.setFacet(true);
    return solrQuery;
  }
  
}

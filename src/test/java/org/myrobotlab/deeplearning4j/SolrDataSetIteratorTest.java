package org.myrobotlab.deeplearning4j;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.LinkedHashSet;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.split.InputSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.junit.Ignore;
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
    // start a dl4j service
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
    SolrQuery datasetQuery = makeTrainQuery();
    // run that query.. get the number of items and the labels
    QueryResponse resp = solr.search(datasetQuery);
    long numFound = resp.getResults().getNumFound();
    // sorted list (according to solr) of the labels for this data set
    LinkedHashSet<String> labels = resolveLabels(resp);
    // Still don't know why the F this is -1 !?!
    int numClasses =  labels.size() - 1;
    // TODO: a different label generator!  probably the solr input split itself?!
    ParentPathLabelGenerator labelMaker = new ParentPathLabelGenerator();
    DataSetIterator[] dataIters = makeSolrInputSplitIterators(datasetQuery, numFound, numClasses, trainPerc, labelMaker);
    DataSetIterator trainIter = dataIters[0];
    DataSetIterator testIter = dataIters[1];
    // loop for each epoch?
    ComputationGraph vgg16Transfer = dl4j.createVGG16TransferModel(featureExtractionLayer, numClasses);
    vgg16Transfer.addListeners(new ScoreIterationListener(1));
    for (int i = 0 ; i < maxEpochs; i++) {
      dl4j.runFitter(trainIter, vgg16Transfer);
      double accuracy = dl4j.evaluateModel(testIter, vgg16Transfer);
      if (accuracy > targetAccuracy) {
        break;
      }
    }    
  }

  private LinkedHashSet<String> resolveLabels(QueryResponse resp) {
    FacetField labelFacet =  resp.getFacetField("label");
    // maintain sort order with a linked hash set
    LinkedHashSet<String> labels = new LinkedHashSet<String>();
    for (Count c : labelFacet.getValues()) {
      labels.add(c.getName());
    }
    return labels;
  }

  private DataSetIterator[] makeSolrInputSplitIterators(SolrQuery datasetQuery, long numFound, int numClasses, double trainPerc, ParentPathLabelGenerator labelMaker) throws IOException {
    // we need to figure out what percentage of the result set the training query
    //and the test query returns.
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
    // how to filter on a random percentage?!
    // TODO: what the f.. why not just a solr dataset iterator directly?!
    //  I guess there's some value in letting the split do the sampleing? but i'm not even doing that here.
    // gah..
    SolrInputSplit trainData = new SolrInputSplit(solr, trainQuery);
    DataSetIterator trainIterator = makeIterator(trainData, numClasses, labelMaker);
    SolrInputSplit testData = new SolrInputSplit(solr, testQuery);
    DataSetIterator testIterator = makeIterator(testData, numClasses, labelMaker);
    // TODO: aren't these the same dataset iterators at this point?!?!
    DataSetIterator[] iters = new DataSetIterator[]{trainIterator,testIterator};
    return iters;    
  }
  
  private DataSetIterator makeIterator(InputSplit split, int numClasses,ParentPathLabelGenerator labelMaker) throws IOException {
    SolrImageRecordReader recordReader = null;
    // TODO: how do we avoid passing labelMaker down here!
    recordReader = new SolrImageRecordReader(height,width,channels,labelMaker);
    // TODO: fix me for solr so we don't have to iterate the full result set to infer stuff
    recordReader.initialize(split);
    //int numClasses = recordReader.numLabels();
    DataSetIterator iter = new RecordReaderDataSetIterator(recordReader, batch, 1, numClasses);
    iter.setPreProcessor( new VGG16ImagePreProcessor());
    return iter;
  }

  // this query returns the superset of all data that will be used for training and testing.
  public SolrQuery makeTrainQuery() {
    SolrQuery solrQuery = new SolrQuery("+has_bytes:true -label:unknown");
    // TODO: avoid this and use cursor mark pagination.
    solrQuery.setRows(0);
    // add a facet on the label field so we know what's in the training dataset.
    solrQuery.addFacetField("label");
    solrQuery.setFacetMinCount(1);
    solrQuery.setFacet(true);
    return solrQuery;
  }
  
}

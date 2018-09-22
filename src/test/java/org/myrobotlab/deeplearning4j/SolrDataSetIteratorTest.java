package org.myrobotlab.deeplearning4j;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Random;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.datavec.api.io.filters.BalancedPathFilter;
import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.split.FileSplit;
import org.datavec.api.split.InputSplit;
import org.datavec.image.loader.BaseImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
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
  // VGG16 specifications. (why do i set this to 1 channel?!
  protected static int channels = 3;
  protected static int height = 224;
  protected static int width = 224;
  // protected static int nCores = 8;
  protected static int epochs = 5;
  int batch = 20;

  private Solr solr;
  private Deeplearning4j dl4j;
  // the output layer name for vgg16
  String featureExtractionLayer = "fc2";
  // private static final 
  private boolean useSolr = true;

  private void initServices() throws SolrServerException, IOException {
    solr = (Solr)Runtime.start("solr", "Solr");
    initializeSolr(solr);
    // start a dl4j service
    dl4j = (Deeplearning4j)Runtime.start("dl4j", "Deeplearning4j");
  }

  private DataSetIterator[] makeSolrInputSplitIterators(String trainingDir, double trainPerc, int numClasses) throws IOException {

    
    // TOOD: some other method for label generation?
    ParentPathLabelGenerator labelMaker = new ParentPathLabelGenerator();
    labelMaker.inferLabelClasses();
    // String[] allowedFormats = BaseImageLoader.ALLOWED_FORMATS;
    // SolrInput split
    
    // we need to figure out what percentage of the result set the training query
    //and the test query returns.
    
    // maybe we should get the total hit count.
    SolrQuery trainQuery = makeTrainQuery();
    SolrQuery testQuery = makeTrainQuery();
    
    // let's get the total hit count.
    QueryResponse resp = solr.search(trainQuery);
    long numFound = resp.getResults().getNumFound();
    
    // Random sort ascending for a percentage
    long trainMaxOffset = (long)((double)numFound * trainPerc);
    long testMaxOffset = (long)((double)numFound * (1 - trainPerc));
    
    // grab the first N
    trainQuery.addSort("random_55", ORDER.asc);
    trainQuery.setRows((int)trainMaxOffset);
    
    // change sort order, grab the last N
    testQuery.addSort("random_55", ORDER.desc);
    testQuery.setRows((int)testMaxOffset);
    // how to filter on a random percentage?!
    // trainQuery.addFacetField("labels");
    // SolrQuery testQuery = makeTestQuery();
    
    SolrInputSplit trainData = new SolrInputSplit(solr, trainQuery);
    SolrInputSplit testData = new SolrInputSplit(solr, testQuery);
    DataSetIterator[] iters = new DataSetIterator[]{
        makeIterator(trainData, numClasses, labelMaker),
        makeIterator(testData, numClasses, labelMaker)};
    return iters;    
  }

  private DataSetIterator[] makeTrainingAndTestIterators(String trainingDir, double trainPerc, int numClasses) throws IOException {
    ParentPathLabelGenerator labelMaker = new ParentPathLabelGenerator();
    labelMaker.inferLabelClasses();
    String[] allowedFormats = BaseImageLoader.ALLOWED_FORMATS;
    Random rng  = new Random(seed);
    File parentDir = new File(trainingDir);
    FileSplit filesInDir = new FileSplit(parentDir, allowedFormats, rng);
    BalancedPathFilter pathFilter = new BalancedPathFilter(rng, allowedFormats, labelMaker);
    if (trainPerc >= 100) {
      throw new IllegalArgumentException("Percentage of data set aside for training has to be less than 100%. Test percentage = 100 - training percentage, has to be greater than 0");
    }
    InputSplit[] filesInDirSplit = filesInDir.sample(pathFilter, trainPerc, 100-trainPerc);
    DataSetIterator[] iters = new DataSetIterator[]{makeIterator(filesInDirSplit[0], numClasses, labelMaker),
        makeIterator(filesInDirSplit[1], numClasses, labelMaker)};
    return iters;    
  }

  @Test
  public void testDirBasedTransferLearningVGG16() throws IOException, NoAvailableBackendException, SolrServerException {
    LoggingFactory.init("INFO");
    System.out.println(System.getProperty("java.library.path"));
    Nd4jBackend.load();
    // start solr and dl4j service.
    initServices();
    String trainingDir = "solrtraining";
    // consider increasing this if there's enough training data.
    double trainPerc = 0.8;
    // Ok assuming we've called testSolrExportTrainingData...   
    // How many classes do we have.
    // TODO: dervive this from the directory 
    int numClasses = 6;
    ComputationGraph vgg16Transfer = dl4j.createVGG16TransferModel(featureExtractionLayer, numClasses);
    //Dataset iterators
    DataSetIterator[] dataIters = null;
    if (useSolr) {
      // The training percentage is completely ignored!
      dataIters = makeSolrInputSplitIterators(trainingDir,trainPerc, numClasses);
    } else { 
      dataIters = makeTrainingAndTestIterators(trainingDir, trainPerc, numClasses);
    }
    
    vgg16Transfer.addListeners(new ScoreIterationListener(1));
    //DataSetIterator trainIter = dataIters[0];
    // TODO: gah..use test iterator instead?!
    DataSetIterator trainIter = dataIters[0];
    DataSetIterator testIter = dataIters[1];
    // loop for each epoch?
    double target = 0.90;
    for (int i = 0 ; i < epochs; i++) {
     runFitter(trainIter, vgg16Transfer);
     double accuracy = evaluateModel(testIter, vgg16Transfer);
     if (accuracy > target) {
       break;
     }
    }    
  }

  public void runFitter(DataSetIterator trainIter, ComputationGraph vgg16Transfer) {
    trainIter.reset();
    while(trainIter.hasNext()) {
      vgg16Transfer.fit(trainIter.next());
    }
  }

  private double evaluateModel(DataSetIterator testIter, ComputationGraph vgg16Transfer) {
    testIter.reset();
    Evaluation eval = vgg16Transfer.evaluate(testIter);
    log.info(eval.stats());
    return eval.accuracy();
  }

  private DataSetIterator makeIterator(InputSplit split, int numClasses,ParentPathLabelGenerator labelMaker) throws IOException {
    ImageRecordReader recordReader = null;
    if (useSolr ) {
      recordReader = new SolrImageRecordReader(height,width,channels,labelMaker);
    } else { 
      recordReader = new ImageRecordReader(height,width,channels,labelMaker);
    }
    recordReader.initialize(split);
    DataSetIterator iter = new RecordReaderDataSetIterator(recordReader, batch, 1, numClasses);
    iter.setPreProcessor( new VGG16ImagePreProcessor());
    return iter;
  }

  public SolrQuery makeTrainQuery() {
    // TODO: return a solr query object here.
    SolrQuery solrQuery = new SolrQuery("+has_bytes:true -label:unknown");
    // TODO: avoid this and use cursor mark pagination.
    solrQuery.setRows(0);
    return solrQuery;
  }

  private void initializeSolr(Solr solr) throws SolrServerException, IOException {
    solr.startEmbedded();
    // solr.deleteEmbeddedIndex();    
  }

}

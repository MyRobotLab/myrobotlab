package org.myrobotlab.deeplearning4j;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Random;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.datavec.api.io.filters.BalancedPathFilter;
import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.split.FileSplit;
import org.datavec.api.split.InputSplit;
import org.datavec.image.loader.BaseImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.datasets.iterator.MultipleEpochsIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.distribution.Distribution;
import org.deeplearning4j.nn.conf.distribution.GaussianDistribution;
import org.deeplearning4j.nn.conf.distribution.NormalDistribution;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.LocalResponseNormalization;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.transferlearning.FineTuneConfiguration;
import org.deeplearning4j.nn.transferlearning.TransferLearning;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.zoo.ZooModel;
import org.deeplearning4j.zoo.model.VGG16;
import org.junit.Ignore;
//import org.deeplearning4j.ui.api.UIServer;
//import org.deeplearning4j.ui.stats.StatsListener;
//import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.junit.Test;
import org.myrobotlab.service.Solr;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.dataset.api.preprocessor.VGG16ImagePreProcessor;
import org.nd4j.linalg.factory.Nd4jBackend;
import org.nd4j.linalg.factory.Nd4jBackend.NoAvailableBackendException;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Deeplearning4j;
import org.myrobotlab.service.Runtime;
// import org.deeplearning4j.nn.modelimport.keras.trainedmodels.TrainedModelHelper;

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

  private void initServices() throws SolrServerException, IOException {
    solr = (Solr)Runtime.start("solr", "Solr");
    initializeSolr(solr);
    // start a dl4j service
    dl4j = (Deeplearning4j)Runtime.start("dl4j", "Deeplearning4j");
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
    //InputSplit[] iterators = new InputSplit[]{filesInDirSplit[0], filesInDirSplit[1]};
    DataSetIterator[] iters = new DataSetIterator[]{makeIterator(filesInDirSplit[0], numClasses, labelMaker),
                                                    makeIterator(filesInDirSplit[1], numClasses, labelMaker)};
    //    trainData = filesInDirSplit[0];
    //    testData = filesInDirSplit[1];
    return iters;    
  }

  @Test
  public void testDirBasedTransferLearningVGG16() throws IOException, NoAvailableBackendException {

    
    System.out.println(System.getProperty("java.library.path"));

    Nd4jBackend.load();
    
    String trainingDir = "solrtraining";
    // consider increasing this if there's enough training data.
    double trainPerc = 0.9;
    // Ok assuming we've called testSolrExportTrainingData...   
    // How many classes do we have.
    // TODO: dervive this from the directory 
    int numClasses = 6;

    log.info("Loading org.deeplearning4j.transferlearning.vgg16...\n\n");
    ZooModel zooModel = VGG16.builder().build();
    ComputationGraph vgg16 = (ComputationGraph) zooModel.initPretrained();
    log.info(vgg16.summary());
    //Decide on a fine tune configuration to use.
    //In cases where there already exists a setting the fine tune setting will
    //  override the setting for all layers that are not "frozen".
    FineTuneConfiguration fineTuneConf = new FineTuneConfiguration.Builder()
        .updater(new Nesterovs(5e-5))
        .seed(seed)
        .build();
    //Construct a new model with the intended architecture and print summary
    ComputationGraph vgg16Transfer = new TransferLearning.GraphBuilder(vgg16)
        .fineTuneConfiguration(fineTuneConf)
        .setFeatureExtractor(featureExtractionLayer) //the specified layer and below are "frozen"
        .removeVertexKeepConnections("predictions") //replace the functionality of the final vertex
        .addLayer("predictions",
            new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
            .nIn(4096).nOut(numClasses)
            .weightInit(WeightInit.DISTRIBUTION)
            .dist(new NormalDistribution(0,0.2*(2.0/(4096+numClasses)))) //This weight init dist gave better results than Xavier
            .activation(Activation.SOFTMAX).build(),
            "fc2")
        .build();
    log.info(vgg16Transfer.summary());

    //Dataset iterators
    DataSetIterator[] dataIters = makeTrainingAndTestIterators(trainingDir, trainPerc, numClasses);
    //DataSetIterator trainIter = dataIters[0];
    // TODO: gah..use test iterator instead?!
    DataSetIterator trainIter = dataIters[1];
    DataSetIterator testIter = dataIters[1];
    log.info("About to evaluate the dataset before fit..");
    Evaluation eval;
    eval = vgg16Transfer.evaluate(testIter);
    log.info("Eval stats BEFORE fit.....");
    log.info(eval.stats() + "\n");
    for (int i = 1 ; i <= epochs; i++) {
      log.info("Training Epoch {} of {}", i, epochs);
      testIter.reset();
      int iter = 0;
      while(trainIter.hasNext()) {

        vgg16Transfer.fit(trainIter.next());
        if (iter % 10 == 0) {
          log.info("Evaluate model at iter "+iter +" ....");
          eval = vgg16Transfer.evaluate(testIter);
          log.info(eval.stats());
          testIter.reset();
        }
        iter++;
      }

    }
    log.info("Model build complete");
  }


  private DataSetIterator makeIterator(InputSplit split, int numClasses,ParentPathLabelGenerator labelMaker) throws IOException {
    ImageRecordReader recordReader = new ImageRecordReader(height,width,channels,labelMaker);
    recordReader.initialize(split);
    DataSetIterator iter = new RecordReaderDataSetIterator(recordReader, batch, 1, numClasses);
    iter.setPreProcessor( new VGG16ImagePreProcessor());
    return iter;
  }


  // @Test
  public void testSolrExportTrainingData() throws IOException, SolrServerException {
    initServices();
    String directory = "solrtraining";
    SolrQuery solrQuery = new SolrQuery("+has_bytes:true -label:unknown");
    // max result set.
    // TODO: switch to cursormark
    solrQuery.setStart(0);
    solrQuery.setRows(1000);
    solr.createTrainingDataDir(solrQuery, directory);
  }

  // @Test
  public void testSolrDataSetIterator() throws SolrServerException, IOException {
    LoggingFactory.init("INFO");
    // create solr with a training dataset in it's index
    initServices();
    // move the model creation to dl4j service.
    // dl4j.loadModel();
    // now.. we want to train a dl4j model based on a solr dataset iterator... :-/
    //DataNormalization scaler = new ImagePreProcessingScaler(0, 1);

    DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
    // scaler.fit(sdsi);
    String queryString = "+has_bytes:true -label:unknown";
    SolrDataSetIterator sdsi = new SolrDataSetIterator(batch, queryString, solr, height, width, channels);
    //scaler.fit(sdsi);
    //sdsi.reset();
    //sdsi.setPreProcessor(scaler);
    sdsi.setPreProcessor(new VGG16ImagePreProcessor());
    int numLabels = sdsi.totalOutcomes();
    // a model to train.

    // alex net.
    //MultiLayerNetwork model = createModel(numLabels);
    // lenet
    // MultiLayerNetwork model = lenetModel(numLabels);



    ComputationGraph model = transferLearning(featureExtractionLayer, numLabels, loadVGG16Pretrained());

    //    UIServer uiServer = UIServer.getInstance();
    //    StatsStorage statsStorage = new InMemoryStatsStorage();
    //    uiServer.attach(statsStorage);
    //    network.setListeners(new StatsListener( statsStorage),new ScoreIterationListener(1));
    // fit the model to the data.
    // TODO: add some iterators for epochs.


    // for (int i = 0 ; i < epochs; i++) {
    //	System.out.println("Running epoch " + i);
    //sdsi.reset();

    //   model.addListeners(new ScoreIterationListener(10));
    MultipleEpochsIterator trainIter = new MultipleEpochsIterator(epochs, sdsi);
    //    model.fit(trainIter);
    //sdsi.reset();
    //   	 model.fit(sdsi);

    //    	model.addListeners(new ScoreIterationListener());
    //    	model.fit(sdsi);
    //      evalModel(sdsi, model);
    //  }
    //evalModel(sdsi, model);
    //String modelFilename = "trained.model.bin";    
    //dl4j.setNetwork(model);

    // TODO: figure a way to save the model with it's labels. 
    //dl4j.setNetworkLabels(sdsi.getLabels());
    //dl4j.saveModel(modelFilename);

    // dl4j.trainModel();
    //    sdsi.reset();
    //    Evaluation eval = model.evaluate(sdsi);
    //    System.out.println(eval.stats(true));


    System.out.println("Evaluate model....");
    //recordReader.initialize(testData);
    //dataIter = new RecordReaderDataSetIterator(recordReader, batchSize, 1, numLabels);
    //scaler.fit(dataIter);
    //dataIter.setPreProcessor(scaler);
    sdsi.reset();

    Evaluation eval = model.evaluate(sdsi);
    System.out.println(eval.stats(true));

    // Example on how to get predict results with trained model. Result for first example in minibatch is printed
    //dataIter.reset();
    sdsi.reset();



    int iter = 0;
    while(sdsi.hasNext()) {

      model.fit(trainIter.next());
      // if (iter % 10 == 0) {
      log.info("Evaluate model at iter "+iter +" ....");
      eval = model.evaluate(sdsi);
      log.info(eval.stats());
      sdsi.reset();
      //  }
      iter++;
    }

    // ensure we have at least 10
    //    int numTests = 10;
    //    for (int i = 0 ; i < numTests; i++) {
    //      DataSet testDataSet = sdsi.next();
    //      List<String> allClassLabels = sdsi.getLabels();
    //      int labelIndex = testDataSet.getLabels().argMax(1).getInt(0);
    //      int[] predictedClasses = model.predict(testDataSet.getFeatures());
    //      String expectedResult = allClassLabels.get(labelIndex);
    //      String modelPrediction = allClassLabels.get(predictedClasses[0]);
    //      System.out.print("\nFor a single example that is labeled " + expectedResult + " the model predicted " + modelPrediction + "\n\n");
    //    }

    System.out.println("Done...");
  }

  private void evalModel(SolrDataSetIterator sdsi, MultiLayerNetwork model) {
    sdsi.reset();
    Evaluation eval = model.evaluate(sdsi);
    System.out.println(eval.stats(true));
  }

  private void initializeSolr(Solr solr) throws SolrServerException, IOException {
    // TODO Auto-generated method stub
    solr.startEmbedded();
    // solr.deleteEmbeddedIndex();    
    // TODO: load some test documents into the index!!
    //SolrInputDocument doc = new SolrInputDocument();
    //doc.setField("id", "doc_" + 1);

  }



  // pretraining stuff.
  public ComputationGraph loadVGG16Pretrained() throws IOException {
    ZooModel zooModel = VGG16.builder().build();
    ComputationGraph vgg16 = (ComputationGraph) zooModel.initPretrained();
    log.info(vgg16.summary());
    return vgg16;
    //    TrainedModelHelper modelImportHelper = new TrainedModelHelper(TrainedModels.VGG16);
    //    log.info("\n\nLoading org.deeplearning4j.transferlearning.vgg16...\n\n");
    //    ComputationGraph vgg16 = modelImportHelper.loadModel();
    //    log.info(vgg16.summary());
  }

  // now we want to specify a transfer learning model
  public ComputationGraph transferLearning(String featureExtractionLayer, int numClasses, ComputationGraph vgg16) {
    FineTuneConfiguration fineTuneConf = new FineTuneConfiguration.Builder()
        .updater(new Nesterovs(5e-5))
        .seed(seed)
        .build();

    //Construct a new model with the intended architecture and print summary
    ComputationGraph vgg16Transfer = new TransferLearning.GraphBuilder(vgg16)
        .fineTuneConfiguration(fineTuneConf)
        .setFeatureExtractor(featureExtractionLayer) //the specified layer and below are "frozen"
        .removeVertexKeepConnections("predictions") //replace the functionality of the final vertex
        .addLayer("predictions",
            new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
            .nIn(4096).nOut(numClasses)
            .weightInit(WeightInit.DISTRIBUTION)
            .dist(new NormalDistribution(0,0.2*(2.0/(4096+numClasses)))) //This weight init dist gave better results than Xavier
            .activation(Activation.SOFTMAX).build(),
            "fc2")
        .build();
    log.info(vgg16Transfer.summary());
    return vgg16Transfer;
  }


  public MultiLayerNetwork createModel(int numLabels) {
    /**
     * AlexNet model interpretation based on the original paper ImageNet Classification with Deep Convolutional Neural Networks
     * and the imagenetExample code referenced.
     * http://papers.nips.cc/paper/4824-imagenet-classification-with-deep-convolutional-neural-networks.pdf
     **/

    double nonZeroBias = 1;
    double dropOut = 0.5;
    MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
        .seed(seed)
        .weightInit(WeightInit.DISTRIBUTION)
        .dist(new NormalDistribution(0.0, 0.01))
        .activation(Activation.RELU)
        .updater(Updater.NESTEROVS)
        .gradientNormalization(GradientNormalization.RenormalizeL2PerLayer) // normalize to prevent vanishing or exploding gradients
        .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
        .l2(5 * 1e-4)
        .miniBatch(false)
        .list()
        .layer(0, convInit("cnn1", channels, 96, new int[]{11, 11}, new int[]{4, 4}, new int[]{3, 3}, 0))
        .layer(1, new LocalResponseNormalization.Builder().name("lrn1").build())
        .layer(2, maxPool("maxpool1", new int[]{3,3}))
        .layer(3, conv5x5("cnn2", 256, new int[] {1,1}, new int[] {2,2}, nonZeroBias))
        .layer(4, new LocalResponseNormalization.Builder().name("lrn2").build())
        .layer(5, maxPool("maxpool2", new int[]{3,3}))
        .layer(6,conv3x3("cnn3", 384, 0))
        .layer(7,conv3x3("cnn4", 384, nonZeroBias))
        .layer(8,conv3x3("cnn5", 256, nonZeroBias))
        .layer(9, maxPool("maxpool3", new int[]{3,3}))
        .layer(10, fullyConnected("ffn1", 4096, nonZeroBias, dropOut, new GaussianDistribution(0, 0.005)))
        .layer(11, fullyConnected("ffn2", 4096, nonZeroBias, dropOut, new GaussianDistribution(0, 0.005)))
        .layer(12, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
            .name("output")
            .nOut(numLabels)
            .activation(Activation.SOFTMAX)
            .build())
        .backprop(true)
        .pretrain(false)
        .setInputType(InputType.convolutional(height, width, channels))
        .build();
    return new MultiLayerNetwork(conf);
  }


  public MultiLayerNetwork lenetModel(int numLabels) {
    /**
     * Revisde Lenet Model approach developed by ramgo2 achieves slightly above random
     * Reference: https://gist.github.com/ramgo2/833f12e92359a2da9e5c2fb6333351c5
     **/
    // double learningRate = 0.0001;
    double learningRate = 0.01;

    MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
        .seed(seed)
        .l2(0.005)
        .activation(Activation.RELU)
        .weightInit(WeightInit.XAVIER)
        .updater(new Nesterovs(learningRate,0.9))
        .list()
        .layer(0, convInit("cnn1", channels, 50 ,  new int[]{5, 5}, new int[]{1, 1}, new int[]{0, 0}, 0))
        .layer(1, maxPool("maxpool1", new int[]{2,2}))
        .layer(2, conv5x5("cnn2", 100, new int[]{5, 5}, new int[]{1, 1}, 0))
        .layer(3, maxPool("maxool2", new int[]{2,2}))
        .layer(4, new DenseLayer.Builder().nOut(500).build())
        .layer(5, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
            .nOut(numLabels)
            .activation(Activation.SOFTMAX)
            .build())
        .backprop(true).pretrain(false)
        .setInputType(InputType.convolutional(height, width, channels))
        .build();

    return new MultiLayerNetwork(conf);

  }

  private ConvolutionLayer convInit(String name, int in, int out, int[] kernel, int[] stride, int[] pad, double bias) {
    return new ConvolutionLayer.Builder(kernel, stride, pad).name(name).nIn(in).nOut(out).biasInit(bias).build();
  }

  private ConvolutionLayer conv3x3(String name, int out, double bias) {
    return new ConvolutionLayer.Builder(new int[]{3,3}, new int[] {1,1}, new int[] {1,1}).name(name).nOut(out).biasInit(bias).build();
  }

  private ConvolutionLayer conv5x5(String name, int out, int[] stride, int[] pad, double bias) {
    return new ConvolutionLayer.Builder(new int[]{5,5}, stride, pad).name(name).nOut(out).biasInit(bias).build();
  }

  private SubsamplingLayer maxPool(String name,  int[] kernel) {
    return new SubsamplingLayer.Builder(kernel, new int[]{2,2}).name(name).build();
  }

  private DenseLayer fullyConnected(String name, int out, double bias, double dropOut, Distribution dist) {
    return new DenseLayer.Builder().name(name).nOut(out).biasInit(bias).dropOut(dropOut).dist(dist).build();
  }


}

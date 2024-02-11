package org.myrobotlab.service;

import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2RGB;
import static org.bytedeco.opencv.global.opencv_imgproc.cvResize;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.AbstractIplImage;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Rect;
import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.split.FileSplit;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.datavec.image.transform.ColorConversionTransform;
import org.datavec.image.transform.FlipImageTransform;
import org.datavec.image.transform.ImageTransform;
import org.datavec.image.transform.WarpImageTransform;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.datasets.iterator.MultipleEpochsIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.distribution.Distribution;
import org.deeplearning4j.nn.conf.distribution.GaussianDistribution;
import org.deeplearning4j.nn.conf.distribution.NormalDistribution;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.inputs.InvalidInputTypeException;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.LocalResponseNormalization;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.layers.objdetect.DetectedObject;
import org.deeplearning4j.nn.layers.objdetect.YoloUtils;
import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;
import org.deeplearning4j.nn.modelimport.keras.exceptions.InvalidKerasConfigurationException;
import org.deeplearning4j.nn.modelimport.keras.exceptions.UnsupportedKerasConfigurationException;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.transferlearning.FineTuneConfiguration;
import org.deeplearning4j.nn.transferlearning.TransferLearning;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.deeplearning4j.zoo.PretrainedType;
import org.deeplearning4j.zoo.ZooModel;
import org.deeplearning4j.zoo.model.Darknet19;
import org.deeplearning4j.zoo.model.TextGenerationLSTM;
import org.deeplearning4j.zoo.model.TinyYOLO;
import org.deeplearning4j.zoo.model.VGG16;
import org.deeplearning4j.zoo.util.ClassPrediction;
import org.deeplearning4j.zoo.util.Labels;
import org.deeplearning4j.zoo.util.darknet.DarknetLabels;
import org.deeplearning4j.zoo.util.darknet.VOCLabels;
import org.deeplearning4j.zoo.util.imagenet.ImageNetLabels;
import org.myrobotlab.deeplearning4j.CustomModel;
import org.myrobotlab.deeplearning4j.MRLLabelGenerator;
import org.myrobotlab.deeplearning4j.SolrImageRecordReader;
import org.myrobotlab.deeplearning4j.SolrInputSplit;
import org.myrobotlab.deeplearning4j.SolrLabelGenerator;
import org.myrobotlab.framework.Service;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.CloseableFrameConverter;
import org.myrobotlab.opencv.YoloDetectedObject;
import org.myrobotlab.service.config.Deeplearning4jConfig;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.dataset.api.preprocessor.VGG16ImagePreProcessor;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.factory.Nd4jBackend;
import org.nd4j.linalg.factory.Nd4jBackend.NoAvailableBackendException;
import org.nd4j.linalg.learning.config.AdaDelta;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;

/**
 * Deeplearning4j wrapper service to expose the deep learning. This is basically
 * derived from the AnimialClassifier example in the dl4j examples. More info
 * at:
 * https://github.com/deeplearning4j/dl4j-examples/blob/master/dl4j-examples/src/main/java/org/deeplearning4j/examples/convolution/AnimalsClassification.java
 * 
 * DO NOT USE THIS SERVICE. IT DOESN'T REALLY WORK YET. This is a work in
 * progress and is very very far from being done.
 * 
 * @author kwatters
 *
 */
public class Deeplearning4j extends Service<Deeplearning4jConfig>
{

  private static final long serialVersionUID = 1L;
  transient public final static Logger log = LoggerFactory.getLogger(Deeplearning4j.class);
  // TODO: update these based on the input sample data...
  protected static int height = 100;
  protected static int width = 100;
  protected static int channels = 3;
  // TODO: the number of labels for the current model
  protected static int modelNumLabels = 0;
  // TODO: what does this actually control?!
  protected static int batchSize = 20;
  protected static long seed = 42;
  transient protected static Random rng = new Random(seed);
  protected static int listenerFreq = 1;
  protected static int iterations = 1;
  // protected static int epochs = 50;
  public static int epochs = 2;
  protected static int nCores = 8;
  protected static String modelType = "AlexNet"; // LeNet, AlexNet or Custom but
  // you need to fill it out
  public String modelDir = "models";
  public String modelFilename = "model.bin";
  // This is the "model" to be trained and used
  transient private MultiLayerNetwork network;
  // these are the labels that relate to the output of the model.
  private List<String> networkLabels;

  // TODO: clean all of this up! for now
  // we're just going to hack in the deeplearning4j zoo , in particular vgg16
  // model
  // pretrained from imagenet.
  transient private ImageNetLabels imageNetLabels = null;
  transient private ComputationGraph vgg16 = null;

  transient private ComputationGraph darknet = null;
  transient private ComputationGraph tinyyolo = null;
  transient private TinyYOLO tinyYOLOModel = null;

  transient private ComputationGraph miniXCEPTION = null;

  transient private TextGenerationLSTM textGenLSTM = null;

  transient OpenCVFrameConverter.ToIplImage converterToImage = new OpenCVFrameConverter.ToIplImage();

  // constructor.
  public Deeplearning4j(String reservedKey, String id) {
    super(reservedKey, id);
    // initialize the nd4j backend once up front.
    // not sure if you can call this multiple times.
    try {
      Nd4jBackend.load();
    } catch (NoAvailableBackendException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      warn("ND4j backend initialization error. Deeplearning4j did not start properly.");
    }

  }

  /**
   * Train a model based on a directory of training data. Each subdirectory is
   * named for it's label the files in that subdirectory are examples of that
   * label. Normally a directory will contain a bunch of training images.
   * 
   * @param trainingDataDir
   *                        the directory that contains the subdirectories of
   *                        labels.
   * @throws IOException
   *                     e
   */
  public void trainModel(String trainingDataDir) throws IOException {
    log.info("Load data....");
    /**
     * Data Setup -&gt; organize and limit data file paths: - mainPath = path to
     * image files - fileSplit = define basic dataset split with limits on
     * format - pathFilter = define additional file load filter to limit size
     * and balance batch content
     **/
    ParentPathLabelGenerator labelMaker = new MRLLabelGenerator();
    // load up the path where the training data lives.
    File mainPath = new File(trainingDataDir);
    FileSplit fileSplit = new FileSplit(mainPath, NativeImageLoader.ALLOWED_FORMATS, rng);
    ImageRecordReader recordReader = new ImageRecordReader(height, width, channels, labelMaker);
    DataSetIterator dataIter;

    /**
     * Data Setup -&gt; transformation - Transform = how to tranform images and
     * generate large dataset to train on
     **/
    List<ImageTransform> transforms = createImageTransformList();

    /**
     * Data Setup -&gt; normalization - how to normalize images and generate
     * large dataset to train on Data Setup -&gt; define how to load data into
     * net: - recordReader = the reader that loads and converts image data pass
     * in inputSplit to initialize - dataIter = a generator that only loads one
     * batch at a time into memory to save memory - trainIter = uses
     * MultipleEpochsIterator to ensure model runs through the data for all
     * epochs
     **/
    MultipleEpochsIterator trainIter;

    log.info("Train network....");
    // Train without transformations
    recordReader.initialize(fileSplit, null);

    log.info("Create network....{}", networkLabels);

    // training set metadata , the labels and how many there are
    networkLabels = recordReader.getLabels();
    int numLabels = networkLabels.size();
    log.info("Network Labels: {}", networkLabels);

    // an interator for that dataset.
    dataIter = new RecordReaderDataSetIterator(recordReader, batchSize, 1, numLabels);
    DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
    // pre process / fit the data with the scaler.
    scaler.fit(dataIter);
    // build the neural network (specify structure, etc..)
    network = createNetwork(numLabels);
    // our preprocessor ... TODO: is this necessary here?
    dataIter.setPreProcessor(scaler);
    // TODO: learn more about what this does.
    trainIter = new MultipleEpochsIterator(epochs, dataIter, nCores);
    // The magic.. train the model!
    network.fit(trainIter);
    // Train with transformations
    for (ImageTransform transform : transforms) {
      log.info("Training on transformation: {}", transform.getClass().toString());
      recordReader.initialize(fileSplit, transform);
      dataIter = new RecordReaderDataSetIterator(recordReader, batchSize, 1, numLabels);
      scaler.fit(dataIter);
      dataIter.setPreProcessor(scaler);
      trainIter = new MultipleEpochsIterator(epochs, dataIter, nCores);
      // train the model even more with some transpositions of the original
      // image.
      network.fit(trainIter);
    }
    log.info("Done training model..");
  }

  public void trainModel() throws IOException {
    trainModel("./training");
  }

  /*
   * This provides a list of various transforms to attempt on the image as part
   * of the training process this synthetically generates a larger training set.
   * I think it's typically used to train in rotational and scale invariance in
   * the matching of the network. But, that's just a guess :)
   */
  private List<ImageTransform> createImageTransformList() {
    // TODO: consider a bunch of opencv filter based transforms here!
    ImageTransform flipTransform1 = new FlipImageTransform(rng);
    ImageTransform flipTransform2 = new FlipImageTransform(new Random(123));
    ImageTransform warpTransform = new WarpImageTransform(rng, 42);
    List<ImageTransform> transforms = Arrays
        .asList(new ImageTransform[] { flipTransform1, warpTransform, flipTransform2 });
    return transforms;
  }

  private MultiLayerNetwork createNetwork(int numLabels) {
    MultiLayerNetwork network = null;
    switch (modelType) {
      case "LeNet":
        network = lenetModel(numLabels);
        break;
      case "AlexNet":
        network = alexnetModel(numLabels);
        break;
      case "custom":
        network = customModel(numLabels);
        break;
      default:
        throw new InvalidInputTypeException("Incorrect model provided.");
    }
    network.init();
    network.setListeners(new ScoreIterationListener(listenerFreq));
    return network;
  }

  public void saveModel(CustomModel custModel, String filename) throws IOException {
    // ComputationGraph model
    // List<String> labels
    File dir = new File(modelDir);
    if (!dir.exists()) {
      dir.mkdirs();
      log.info("Creating models directory {}", dir);
    }
    File f = new File(filename);
    log.info("Saving DL4J computation graph model to {}", f.getAbsolutePath());
    ModelSerializer.writeModel(custModel.getModel(), filename, true);
    // also need to save the labels!
    String labelFilename = filename + ".labels";
    FileWriter fw = new FileWriter(new File(labelFilename));
    fw.write(StringUtils.join(custModel.getLabels(), "|"));
    fw.flush();
    fw.close();
    log.info("Model saved: {}", f.getAbsolutePath());
  }

  // save the current model and it's set of labels
  public void saveModel(String filename) throws IOException {
    File dir = new File(modelDir);
    if (!dir.exists()) {
      dir.mkdirs();
      log.info("Creating models directory {}", dir);
    }
    File f = new File(filename);
    log.info("Saving DL4J model to {}", f.getAbsolutePath());
    ModelSerializer.writeModel(network, filename, true);
    // also need to save the labels!
    String labelFilename = filename + ".labels";
    FileWriter fw = new FileWriter(new File(labelFilename));
    fw.write(StringUtils.join(networkLabels, "|"));
    fw.flush();
    fw.close();
    log.info("Model saved.");
  }

  public void saveModel() throws IOException {
    saveModel(modelDir + File.separator + modelFilename);
  }

  // load the default model
  public void loadModel() throws IOException {
    loadModel(modelDir + File.separator + modelFilename);
  }

  // load a model based on its filename and the labels from an associated
  // filename.labels file..
  public void loadModel(String filename) throws IOException {
    File f = new File(filename);
    log.info("Loading network from : {}", f.getAbsolutePath());
    network = ModelSerializer.restoreMultiLayerNetwork(f, true);
    log.info("Network restored. {}", network);
    // TODO: there has to be a better way to manage the labels!?!?!! Gah
    networkLabels = new ArrayList<String>();
    for (String s : StringUtils.split(FileIO.toString(new File(filename + ".labels")), "\\|")) {
      networkLabels.add(s);
    }
    modelNumLabels = networkLabels.size();
    log.info("Network labels {} objects", modelNumLabels);
  }

  public CustomModel loadComputationGraph(String filename) throws IOException {
    File f = new File(filename);
    log.info("Loading network from : {}", f.getAbsolutePath());
    // ModelSerializer.re
    ComputationGraph model = ModelSerializer.restoreComputationGraph(f, true);
    log.info("Network restored. {}", model);
    // TODO: there has to be a better way to manage the labels!?!?!! Gah
    List<String> labels = new ArrayList<String>();
    for (String s : StringUtils.split(FileIO.toString(new File(filename + ".labels")), "\\|")) {
      labels.add(s);
    }
    int numLabels = labels.size();
    log.info("Network labels {} objects", numLabels);

    CustomModel cusModel = new CustomModel(model, labels);
    return cusModel;

  }

  public void evaluateModel(File file) throws IOException {
    log.info("Evaluate model....");
    NativeImageLoader nativeImageLoader = new NativeImageLoader(height, width, channels);
    INDArray image = nativeImageLoader.asMatrix(file); // testImage is of Mat
    // format
    // 0-255 to 0-1
    DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
    scaler.transform(image);
    // Pass through to neural Net
    INDArray output = network.output(image);
    log.info("File: {}", output);
    log.info("Labels: {}", networkLabels);
    // TODO: I suppose we could create a map of probabilities and return that
    // ...
    // this map could be large
  }

  /* From the animals classification example */
  public MultiLayerNetwork lenetModel(int numLabels) {
    /**
     * Revisde Lenet Model approach developed by ramgo2 achieves slightly above
     * random Reference:
     * https://gist.github.com/ramgo2/833f12e92359a2da9e5c2fb6333351c5
     **/
    MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(seed).l2(0.005).activation(Activation.RELU)
        .weightInit(WeightInit.XAVIER)
        // .updater(new Nadam(1e-4))
        .updater(new AdaDelta()).list()
        .layer(0, convInit("cnn1", channels, 50, new int[] { 5, 5 }, new int[] { 1, 1 }, new int[] { 0, 0 }, 0))
        .layer(1, maxPool("maxpool1", new int[] { 2, 2 }))
        .layer(2, conv5x5("cnn2", 100, new int[] { 5, 5 }, new int[] { 1, 1 }, 0))
        .layer(3, maxPool("maxool2", new int[] { 2, 2 })).layer(4, new DenseLayer.Builder().nOut(500).build())
        .layer(5,
            new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD).nOut(numLabels)
                .activation(Activation.SOFTMAX).build())
        .setInputType(InputType.convolutional(height, width, channels)).build();

    return new MultiLayerNetwork(conf);
  }

  /* From the animals classification example */
  public MultiLayerNetwork alexnetModel(int numLabels) {
    /**
     * AlexNet model interpretation based on the original paper ImageNet
     * Classification with Deep Convolutional Neural Networks and the
     * imagenetExample code referenced.
     * http://papers.nips.cc/paper/4824-imagenet-classification-with-deep-convolutional-neural-networks.pdf
     **/

    double nonZeroBias = 1;
    double dropOut = 0.5;

    MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(seed)
        .weightInit(new NormalDistribution(0.0, 0.01)).activation(Activation.RELU).updater(new AdaDelta())
        .gradientNormalization(GradientNormalization.RenormalizeL2PerLayer) // normalize
                                                                            // to
                                                                            // prevent
                                                                            // vanishing
                                                                            // or
                                                                            // exploding
                                                                            // gradients
        .l2(5 * 1e-4).list()
        .layer(convInit("cnn1", channels, 96, new int[] { 11, 11 }, new int[] { 4, 4 }, new int[] { 3, 3 }, 0))
        .layer(new LocalResponseNormalization.Builder().name("lrn1").build())
        .layer(maxPool("maxpool1", new int[] { 3, 3 }))
        .layer(conv5x5("cnn2", 256, new int[] { 1, 1 }, new int[] { 2, 2 }, nonZeroBias))
        .layer(new LocalResponseNormalization.Builder().name("lrn2").build())
        .layer(maxPool("maxpool2", new int[] { 3, 3 })).layer(conv3x3("cnn3", 384, 0))
        .layer(conv3x3("cnn4", 384, nonZeroBias)).layer(conv3x3("cnn5", 256, nonZeroBias))
        .layer(maxPool("maxpool3", new int[] { 3, 3 }))
        .layer(fullyConnected("ffn1", 4096, nonZeroBias, dropOut, new GaussianDistribution(0, 0.005)))
        .layer(fullyConnected("ffn2", 4096, nonZeroBias, dropOut, new GaussianDistribution(0, 0.005)))
        .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD).name("output").nOut(numLabels)
            .activation(Activation.SOFTMAX).build())
        .setInputType(InputType.convolutional(height, width, channels)).build();

    return new MultiLayerNetwork(conf);
  }

  /* From the animals classification example */
  public static MultiLayerNetwork customModel(int numLabels) {
    /**
     * Use this method to build your own custom model.
     **/
    log.error("Not implemented!!!");
    return null;
  }

  private ConvolutionLayer convInit(String name, int in, int out, int[] kernel, int[] stride, int[] pad, double bias) {
    return new ConvolutionLayer.Builder(kernel, stride, pad).name(name).nIn(in).nOut(out).biasInit(bias).build();
  }

  private ConvolutionLayer conv3x3(String name, int out, double bias) {
    return new ConvolutionLayer.Builder(new int[] { 3, 3 }, new int[] { 1, 1 }, new int[] { 1, 1 }).name(name).nOut(out)
        .biasInit(bias).build();
  }

  private ConvolutionLayer conv5x5(String name, int out, int[] stride, int[] pad, double bias) {
    return new ConvolutionLayer.Builder(new int[] { 5, 5 }, stride, pad).name(name).nOut(out).biasInit(bias).build();
  }

  private SubsamplingLayer maxPool(String name, int[] kernel) {
    return new SubsamplingLayer.Builder(kernel, new int[] { 2, 2 }).name(name).build();
  }

  private DenseLayer fullyConnected(String name, int out, double bias, double dropOut, Distribution dist) {
    return new DenseLayer.Builder().name(name).nOut(out).biasInit(bias).dropOut(dropOut).dist(dist).build();
  }

  public void loadTextGenLSTM() throws IOException {
    textGenLSTM = TextGenerationLSTM.builder().build();
    // textGenLSTM.
    // TODO: we have to build a model.. and load it in mrl..
    // there is no model available as far as i can tell.
    Model m = textGenLSTM.initPretrained();

    // TODO: add all the good stuff here. and refactor it out into useful
    // methods later.
    System.out.println(m);

  }

  // This is for the Model Zoo support to load in the VGG16 model.
  public void loadVGG16() throws IOException {
    log.info("Loading the VGG16 Model.  Download is large 500+ MB.. this will be cached after it downloads");
    // ZooModel zooModel = new VGG16();
    ZooModel zooModel = VGG16.builder().build();
    vgg16 = (ComputationGraph) zooModel.initPretrained(PretrainedType.IMAGENET);
    // TODO: return true/false if the model was loaded properly/successfully.
    imageNetLabels = new ImageNetLabels();
  }

  public List<List<ClassPrediction>> darknetFile(String filename) throws IOException {
    NativeImageLoader loader = new NativeImageLoader(224, 224, 3, new ColorConversionTransform(COLOR_BGR2RGB));
    FileInputStream fis = new FileInputStream(filename);
    INDArray image = loader.asMatrix(fis);
    // set up input and feedforward
    DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
    scaler.transform(image);
    INDArray result = darknet.outputSingle(image);
    Labels labels = new DarknetLabels();
    List<List<ClassPrediction>> predictions = labels.decodePredictions(result, 10);
    // check output labels of result
    log.info(predictions.toString());
    return predictions;
  }

  public void loadDarknet() throws IOException {
    ZooModel zooModel = Darknet19.builder().build();
    darknet = (ComputationGraph) zooModel.initPretrained();
  }

  public void loadTinyYOLO() throws IOException {
    tinyYOLOModel = TinyYOLO.builder().build(); // num labels doesn't matter
    // since we're getting
    // pretrained imagenet
    tinyyolo = (ComputationGraph) tinyYOLOModel.initPretrained();
  }

  public void loadMiniEXCEPTION()
      throws IOException, UnsupportedKerasConfigurationException, InvalidKerasConfigurationException {
    // load it up!
    String filename = "models" + File.separator + "miniXCEPTION" + File.separator + "_mini_XCEPTION.102-0.66.hdf5";

    miniXCEPTION = KerasModelImport.importKerasModelAndWeights(filename);
  }

  public HashMap<String, Double> classifyImageMiniEXCEPTION(IplImage iplImage, Double confidence) throws IOException {

    CloseableFrameConverter converter = new CloseableFrameConverter();
    // resize the image to the target size of 64x64
    // resize to 64x64
    IplImage ret = AbstractIplImage.create(64, 64, iplImage.depth(), iplImage.nChannels());
    cvResize(iplImage, ret, Imgproc.INTER_AREA);

    // show(ret, "resized");
    // log.info("Resized Image is : height {} width {}", ret.height(),
    // ret.width());
    // ok.. here we probably need to re-size the input? possibly some other
    // input transforms?
    BufferedImage buffImg = converter.toBufferedImage(ret);

    // Mat mat = OpenCV.toMat(ret);
    // OpenCVFilter.show(buffImg, "buff");
    NativeImageLoader loader = new NativeImageLoader(64, 64, 1, new ColorConversionTransform(COLOR_BGR2GRAY));
    /// used to work like this.. i guess we'd have to run permute on the
    /// resultimg image

    // This asMatrix call returns the data in the wrong format now as of DL4j
    // beta7.
    INDArray image = loader.asMatrix(buffImg);
    // This is to convert the channel order to support this model..
    // reference: https://github.com/eclipse/deeplearning4j/issues/8975
    image = image.permute(0, 2, 3, 1); // NCHW to NHWC

    // INDArray image = loader.asMatrix(bais, false);

    DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
    scaler.transform(image);

    HashMap<String, Double> emotionMap = new HashMap<String, Double>();
    INDArray[] out = miniXCEPTION.output(image);
    // presumably the first column is the output for the first input.
    // System.out.println("Output Size" + out.length);
    // Now.. what do we do with this output?
    String[] emotionLabels = new String[] { "angry", "disgust", "scared", "happy", "sad", "surprised", "neutral" };
    for (int i = 0; i < out.length; i++) {
      // System.out.println(out[i].toString());
      INDArray result = out[i];
      int j = -1;
      double[] results = result.toDoubleVector();
      for (String label : emotionLabels) {
        j++;
        Double val = results[j];
        if (val >= confidence) {
          emotionMap.put(label, val);
          log.debug("Emotion {} Val {}", label, val);
        } else {
          log.debug("Filtered Emotion {} Val {}", label, val);
          // not a good classification.. throw it away.
        }
      }
      // now for each label, we have the confidence
    }
    // free up the converter
    converter.close();
    // sort the resulting map accoring to the confidence
    return sortHashMapByValues(emotionMap);
  }

  // TODO: this was taken from stackoverflow
  // https://stackoverflow.com/questions/8119366/sorting-hashmap-by-values
  // seems like there should be a better way to sort a hashmap.
  public LinkedHashMap<String, Double> sortHashMapByValues(HashMap<String, Double> passedMap) {
    List<String> mapKeys = new ArrayList<>(passedMap.keySet());
    List<Double> mapValues = new ArrayList<>(passedMap.values());
    Collections.sort(mapValues);
    Collections.sort(mapKeys);

    Collections.reverse(mapValues);
    Collections.reverse(mapKeys);

    LinkedHashMap<String, Double> sortedMap = new LinkedHashMap<>();

    Iterator<Double> valueIt = mapValues.iterator();
    while (valueIt.hasNext()) {
      Double val = valueIt.next();
      Iterator<String> keyIt = mapKeys.iterator();

      while (keyIt.hasNext()) {
        String key = keyIt.next();
        Double comp1 = passedMap.get(key);
        Double comp2 = val;

        if (comp1.equals(comp2)) {
          keyIt.remove();
          sortedMap.put(key, val);
          break;
        }
      }
    }
    return sortedMap;
  }

  public CanvasFrame show(final IplImage image, final String title) {
    CanvasFrame canvas = new CanvasFrame(title);
    // canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    canvas.showImage(converterToImage.convert(image));
    return canvas;
  }

  public List<List<ClassPrediction>> classifyImageDarknet(IplImage iplImage) throws IOException {
    CloseableFrameConverter converter = new CloseableFrameConverter();
    NativeImageLoader loader = new NativeImageLoader(224, 224, 3, new ColorConversionTransform(COLOR_BGR2RGB));
    BufferedImage buffImg = converter.toBufferedImage(iplImage);
    INDArray image = loader.asMatrix(buffImg);
    DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
    scaler.transform(image);
    INDArray result = darknet.outputSingle(image);
    Labels labels = new DarknetLabels();
    List<List<ClassPrediction>> predictions = labels.decodePredictions(result, 10);
    // check output labels of result
    log.info(predictions.toString());
    converter.close();
    return predictions;
  }

  public ArrayList<YoloDetectedObject> classifyImageTinyYolo(IplImage iplImage, int frameIndex) throws IOException {
    // set up input and feed forward
    CloseableFrameConverter converter = new CloseableFrameConverter();
    // TODO: what the heck? how do we know what these are?!
    int gridWidth = 13;
    int gridHeight = 13;

    NativeImageLoader loader = new NativeImageLoader(416, 416, 3, new ColorConversionTransform(COLOR_BGR2RGB));
    BufferedImage buffImg = converter.toBufferedImage(iplImage);
    INDArray image = loader.asMatrix(buffImg);
    DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
    scaler.transform(image);
    INDArray outputs = tinyyolo.outputSingle(image);

    List<DetectedObject> objs = YoloUtils.getPredictedObjects(Nd4j.create(tinyYOLOModel.getPriorBoxes()), outputs, 0.6,
        0.4);
    // List<DetectedObject> objs =
    // YoloUtils.getPredictedObjects(Nd4j.create(TinyYOLO.priorBoxes), outputs,
    // 0.6, 0.4);
    // check output labels of result
    Labels labels = new VOCLabels();
    ArrayList<YoloDetectedObject> results = new ArrayList<YoloDetectedObject>();
    double threshold = 0.5;
    for (DetectedObject obj : objs) {
      INDArray preds = obj.getClassPredictions();
      if (preds == null) {
        log.info("null preds?!");
        continue;
      }
      List<List<ClassPrediction>> fullDecode = decodePredictions(labels, preds, 1);
      if (fullDecode == null) {
        log.info("Full decode is null");
        continue;
      }
      if (fullDecode.get(0) == null) {
        log.info("Full decode first was null.");
        continue;
      }
      ClassPrediction classPrediction = fullDecode.get(0).get(0);
      if (classPrediction == null) {
        log.info("Null decode...");
        continue;
      }
      // log.info(obj.toString() + " " + classPrediction);
      if (classPrediction.getProbability() > threshold) {
        // this is a good one..
        int w = iplImage.width();
        int h = iplImage.height();

        double[] xy1 = obj.getTopLeftXY();
        double[] xy2 = obj.getBottomRightXY();
        int x1 = (int) Math.round(w * xy1[0] / gridWidth);
        int y1 = (int) Math.round(h * xy1[1] / gridHeight);
        int x2 = (int) Math.round(w * xy2[0] / gridWidth);
        int y2 = (int) Math.round(h * xy2[1] / gridHeight);

        int xLeftBottom = x1;
        int yLeftBottom = y1;
        int yRightTop = x2;
        int xRightTop = y2;

        Rect boundingBox = new Rect(xLeftBottom, yLeftBottom, xRightTop - xLeftBottom, yRightTop - yLeftBottom);
        YoloDetectedObject yoloobj = new YoloDetectedObject(boundingBox, (float) (classPrediction.getProbability()),
            classPrediction.getLabel(), frameIndex, null, null);
        log.info("Yolo Object: {}", yoloobj);
        results.add(yoloobj);
      }
    }
    converter.close();
    return results;
  }

  public DataSetIterator makeSolrInputSplitIterator(Solr solr, SolrQuery datasetQuery, long numFound,
      List<String> labels, int batch, int height, int width, int channels,
      String labelField) throws IOException {
    // TODO: don't depend on the solr service, but rather some datasource that
    // can produce input splits...
    // TODO: pass in the record reader and the preprocessor
    // training set iterator
    SolrInputSplit split = new SolrInputSplit(solr, datasetQuery, labels, labelField);
    SolrLabelGenerator labelMaker = new SolrLabelGenerator();
    labelMaker.setSolrInputSplit(split);
    SolrImageRecordReader recordReader = new SolrImageRecordReader(height, width, channels, labelMaker);
    recordReader.setLabels(labels);
    // TODO: This initializes the locations?! ouch. avoid that.. just use an
    // iterator!
    recordReader.initialize(split);
    DataSetIterator iter = new RecordReaderDataSetIterator(recordReader, batch, 1, labels.size());
    iter.setPreProcessor(new VGG16ImagePreProcessor());
    return iter;
  }

  public CustomModel trainModel(List<String> labels, DataSetIterator trainIter, DataSetIterator testIter,
      String filename, int maxEpochs, double targetAccuracy,
      String featureExtractionLayer) throws IOException {
    // loop for each epoch?
    ComputationGraph model = createVGG16TransferModel(featureExtractionLayer, labels.size());
    model.addListeners(new ScoreIterationListener(1));
    for (int i = 0; i < maxEpochs; i++) {
      runFitter(trainIter, model);
      double accuracy = evaluateModel(testIter, model);
      if (accuracy > targetAccuracy) {
        // ok. if we got here this is a good model.. let's save it
        CustomModel custModel = new CustomModel(model, labels);
        return custModel;
      }
    }
    log.info("Model didn't converge to desired accuracy.");
    return new CustomModel(model, labels);
  }

  public List<List<ClassPrediction>> decodePredictions(Labels labels, INDArray predictions, int n) {
    long rows = predictions.size(0);
    long cols = predictions.size(1);
    if (predictions.isColumnVector()) {
      predictions = predictions.ravel();
      rows = predictions.size(0);
      cols = predictions.size(1);
    }
    List<List<ClassPrediction>> descriptions = new ArrayList<>();
    for (int batch = 0; batch < rows; batch++) {
      INDArray result = predictions.getRow(batch);
      result = Nd4j.vstack(Nd4j.linspace(0, cols, cols), result);
      result = Nd4j.sortColumns(result, 1, false);
      List<ClassPrediction> current = new ArrayList<>();
      for (int i = 0; i < n; i++) {
        int label = result.getInt(0, i);
        if (label > 19) {
          log.info("Uh oh..");
          return null;
        }
        log.info("Label ID:" + label);
        double prob = result.getDouble(1, i);
        current.add(new ClassPrediction(label, labels.getLabel(label), prob));
      }
      descriptions.add(current);
    }
    return descriptions;
  }

  public Map<String, Double> classifyImageCustom(IplImage iplImage, ComputationGraph model, List<String> labels)
      throws IOException {
    // this height width channel info is for VGG16 based models.
    CloseableFrameConverter converter = new CloseableFrameConverter();
    NativeImageLoader loader = new NativeImageLoader(224, 224, 3);
    BufferedImage buffImg = converter.toBufferedImage(iplImage);
    INDArray image = loader.asMatrix(buffImg);
    DataNormalization scaler = new VGG16ImagePreProcessor();
    scaler.transform(image);
    INDArray[] output = model.output(false, image);
    converter.close();
    return decodePredictions(output[0], labels);
  }

  public Map<String, Double> decodePredictions(INDArray predictions, List<String> labels) throws IOException {

    LinkedHashMap<String, Double> recognizedObjects = new LinkedHashMap<String, Double>();
    // ArrayList<String> labels;
    String predictionDescription = "";
    int[] top5 = new int[5];
    float[] top5Prob = new float[5];
    // brute force collect top 5
    int i = 0;
    for (int batch = 0; batch < predictions.size(0); batch++) {
      if (predictions.size(0) > 1) {
        predictionDescription += String.valueOf(batch);
      }
      predictionDescription += " :";
      INDArray currentBatch = predictions.getRow(batch).dup();
      while (i < 5) {
        top5[i] = Nd4j.argMax(currentBatch, 1).getInt(0, 0);
        top5Prob[i] = currentBatch.getFloat(batch, top5[i]);
        // interesting, this cast looses precision.. float to double.
        recognizedObjects.put(labels.get(top5[i]), (double) top5Prob[i]);
        currentBatch.putScalar(0, top5[i], 0);
        predictionDescription += "\n\t" + String.format("%3f", top5Prob[i] * 100) + "%, " + labels.get(top5[i]);
        i++;
      }
    }
    invoke("publishClassification", (Map<String, Double>) recognizedObjects);
    return recognizedObjects;
  }

  public Map<String, Double> classifyImageVGG16(IplImage iplImage) throws IOException {
    CloseableFrameConverter converter = new CloseableFrameConverter();
    NativeImageLoader loader = new NativeImageLoader(224, 224, 3);
    BufferedImage buffImg = converter.toBufferedImage(iplImage);
    INDArray image = loader.asMatrix(buffImg);
    // TODO: we should consider the model as not only the model, but also the
    // input transforms
    // for that model.
    DataNormalization scaler = new VGG16ImagePreProcessor();
    scaler.transform(image);
    INDArray[] output = vgg16.output(false, image);

    log.info("Complete with output from vgg16..");
    // TODO: return a more native datastructure!
    // String predictions = TrainedModels.VGG16.decodePredictions(output[0]);
    // log.info("Image Predictions: {}", predictions);
    converter.close();
    return decodeVGG16Predictions(output[0]);
  }

  public Map<String, Double> classifyImageFileVGG16(String filename) throws IOException {
    File file = new File(filename);
    NativeImageLoader loader = new NativeImageLoader(224, 224, 3);
    INDArray image = loader.asMatrix(file);
    // TODO: we should consider the model as not only the model, but also the
    // input transforms
    // for that model.
    DataNormalization scaler = new VGG16ImagePreProcessor();
    scaler.transform(image);
    INDArray[] output = vgg16.output(false, image);
    // TODO: return a more native datastructure!
    // String predictions = TrainedModels.VGG16.decodePredictions(output[0]);
    // log.info("Image Predictions: {}", predictions);
    return decodeVGG16Predictions(output[0]);
  }

  // adapted from dl4j TrainedModels.VGG16 class.
  public Map<String, Double> decodeVGG16Predictions(INDArray predictions) throws IOException {
    LinkedHashMap<String, Double> recognizedObjects = new LinkedHashMap<String, Double>();
    // ArrayList<String> labels;
    List<ClassPrediction> classes = imageNetLabels.decodePredictions(predictions, 10).get(0);
    for (ClassPrediction label : classes) {
      log.info(label.getLabel() + ":" + label.getProbability());
      recognizedObjects.put(label.getLabel(), label.getProbability());
    }
    invoke("publishClassification", (Map<String, Double>) recognizedObjects);
    return recognizedObjects;
  }

  public Map<String, Double> publishClassification(Map<String, Double> classifications) {
    return classifications;
  }

  public MultiLayerNetwork getNetwork() {
    return network;
  }

  public void setNetwork(MultiLayerNetwork network) {
    this.network = network;
  }

  public List<String> getNetworkLabels() {
    return networkLabels;
  }

  public void setNetworkLabels(List<String> networkLabels) {
    this.networkLabels = networkLabels;
  }

  /**
   * Create a transfer learning representation of VGG16 Imagenet model. Choose
   * where to freeze the pretrained model. To freeze all layers except for the
   * output layer, choose "fc2" The numClasses property will specify the size of
   * the new output layer of the transfer learned model
   * 
   * @param featureExtractionLayer
   *                               specifies the frozen layer of the model
   * @param numClasses
   *                               the new number of outputs for the model.
   * @return the loaded computation graph
   * @throws IOException
   *                     if there was an error reading the model
   */
  public ComputationGraph createVGG16TransferModel(String featureExtractionLayer, int numClasses) throws IOException {
    log.info("Loading org.deeplearning4j.transferlearning.vgg16...\n\n");
    ZooModel zooModel = VGG16.builder().build();
    ComputationGraph vgg16 = (ComputationGraph) zooModel.initPretrained();
    log.info(vgg16.summary());
    // Decide on a fine tune configuration to use.
    // In cases where there already exists a setting the fine tune setting will
    // override the setting for all layers that are not "frozen".
    FineTuneConfiguration fineTuneConf = new FineTuneConfiguration.Builder().updater(new Nesterovs(5e-5)).seed(seed)
        .build();
    // Construct a new model with the intended architecture and print summary
    ComputationGraph vgg16Transfer = new TransferLearning.GraphBuilder(vgg16).fineTuneConfiguration(fineTuneConf)
        .setFeatureExtractor(featureExtractionLayer) // the
        // specified
        // layer
        // and
        // below
        // are
        // "frozen"
        .removeVertexKeepConnections("predictions") // replace the functionality
        // of the final vertex
        .addLayer("predictions",
            new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD).nIn(4096).nOut(numClasses)
                .weightInit(WeightInit.DISTRIBUTION)
                .dist(new NormalDistribution(0, 0.2 * (2.0 / (4096 + numClasses)))) // This
                // weight
                // init
                // dist
                // gave
                // better
                // results
                // than
                // Xavier
                .activation(Activation.SOFTMAX).build(),
            "fc2")
        .build();
    log.info(vgg16Transfer.summary());
    return vgg16Transfer;
  }

  /**
   * Fit a model against a training set Note: iterator is reset before fitting
   * 
   * @param trainIter
   *                  training set iterator
   * @param model
   *                  the model itself
   * 
   */
  public void runFitter(DataSetIterator trainIter, ComputationGraph model) {
    trainIter.reset();
    while (trainIter.hasNext()) {
      model.fit(trainIter.next());
    }
  }

  /**
   * Evaluate a model against a given testing dataset Note iterator is reset
   * before evaluating.
   * 
   * @param testIter
   *                 interator
   * @param model
   *                 model
   * @return accuracy score
   * 
   */
  public double evaluateModel(DataSetIterator testIter, ComputationGraph model) {
    testIter.reset();
    Evaluation eval = model.evaluate(testIter);
    log.info(eval.stats());
    return eval.accuracy();
  }

  public static void main(String[] args) throws IOException {
    LoggingFactory.init("info");

    Deeplearning4j dl4j = (Deeplearning4j) Runtime.createAndStart("dl4j", "Deeplearning4j");
    // this is how many generations to iterate on training the dataset. larger
    // number means longer training time.
    // dl4j.loadDarknet();
    // dl4j.loadTinyYOLO();
    dl4j.loadTextGenLSTM();
    System.out.println("done.");
    // dl4j.classifyImageTinyYolo();
    // dl4j.epochs = 50;
    // dl4j.trainModel("test/resources/animals");
    // //dl4j.trainModel("training");
    // // save the model out
    // dl4j.saveModel();
    // dl4j.loadModel();
    // //File testIm = new
    // File("test/resources/animals/turtle/Baby_sea_turtle.jpg");
    // // File testIm = new
    // File("test/resources/animals/deer/BlackTailed_Deer_Doe.jpg");
    // File testIm = new
    // File("test/resources/animals/turtle/Western_Painted_Turtle.jpg");
    // // BufferedImage img = ImageIO.read(testIm);
    // // Frame frame = grabberConverter.convert(img);
    // dl4j.evaluateModel(testIm);
    // // dl4j.evaluateModel(img);
    // log.info("Done evaluating the model.");

  }

}

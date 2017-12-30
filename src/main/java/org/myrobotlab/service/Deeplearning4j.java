package org.myrobotlab.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.LinkedHashMap;

import org.myrobotlab.deeplearning4j.MRLLabelGenerator;
import org.myrobotlab.framework.Service;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.datavec.api.berkeley.StringUtils;
import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.split.FileSplit;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.datavec.image.transform.FlipImageTransform;
import org.datavec.image.transform.ImageTransform;
import org.datavec.image.transform.WarpImageTransform;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.datasets.iterator.MultipleEpochsIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.LearningRatePolicy;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
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
import org.deeplearning4j.nn.modelimport.keras.trainedmodels.TrainedModels;
import org.deeplearning4j.nn.modelimport.keras.trainedmodels.Utils.ImageNetLabels;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.deeplearning4j.zoo.PretrainedType;
import org.deeplearning4j.zoo.ZooModel;
import org.deeplearning4j.zoo.model.VGG16;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.io.FileIO;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.dataset.api.preprocessor.VGG16ImagePreProcessor;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

/**
 * Deeplearning4j wrapper service to expose the deep learning.  This is basically derived from the AnimialClassifier example in the dl4j examples.
 * More info at: https://github.com/deeplearning4j/dl4j-examples/blob/master/dl4j-examples/src/main/java/org/deeplearning4j/examples/convolution/AnimalsClassification.java
 *  
 *  DO NOT USE THIS SERVICE.  IT DOESN'T REALLY WORK YET.  This is a work in progress and is very very far from being done.
 *   
 * @author kwatters
 *
 */
public class Deeplearning4j extends Service {

  private static final long serialVersionUID = 1L;
  // TODO: update these based on the input sample data...
  protected static int height = 100;
  protected static int width = 100;
  protected static int channels = 3;
  // TODO: the number of labels for the current model
  protected static int modelNumLabels = 0;
  // TODO: what does this actually control?!
  protected static int batchSize = 20;
  protected static long seed = 42;
  protected static Random rng = new Random(seed);
  protected static int listenerFreq = 1;
  protected static int iterations = 1;
  // protected static int epochs = 50;
  public static int epochs = 2;
  protected static int nCores = 8;
  protected static String modelType = "AlexNet"; // LeNet, AlexNet or Custom but you need to fill it out
  public String modelDir = "models";
  public String modelFilename = "model.bin";
  // This is the "model" to be trained and used 
  private MultiLayerNetwork network;
  // these are the labels that relate to the output of the model.
  private List<String> networkLabels; 

  // TODO: clean all of this up!  for now
  // we're just going to hack in the deeplearning4j zoo , in particular vgg16 model
  // pretrained from imagenet.
  
  private ComputationGraph vgg16 = null;
  
  
  // constructor.
  public Deeplearning4j(String reservedKey) {
    super(reservedKey);
  }

  /**
   * Train a model based on a directory of training data. Each subdirectory is named for it's label
   * the files in that subdirectory are examples of that label.  Normally a directory will contain a bunch
   * of training images.
   * 
   * @param trainingDataDir the directory that contains the subdirectories of labels.
   * @throws IOException e 
   */
  public void trainModel(String trainingDataDir) throws IOException {
    log.info("Load data....");
    /**
     * Data Setup -&gt; organize and limit data file paths:
     *  - mainPath = path to image files
     *  - fileSplit = define basic dataset split with limits on format
     *  - pathFilter = define additional file load filter to limit size and balance batch content
     **/
    ParentPathLabelGenerator labelMaker = new MRLLabelGenerator();
    // load up the path where the training data lives.
    File mainPath = new File(trainingDataDir);
    FileSplit fileSplit = new FileSplit(mainPath, NativeImageLoader.ALLOWED_FORMATS, rng);
    ImageRecordReader recordReader = new ImageRecordReader(height, width, channels, labelMaker);
    DataSetIterator dataIter;

    /**
     * Data Setup -&gt; transformation
     *  - Transform = how to tranform images and generate large dataset to train on
     **/
    List<ImageTransform> transforms = createImageTransformList();

    /**
     * Data Setup -&gt; normalization
     *  - how to normalize images and generate large dataset to train on
     * Data Setup -&gt; define how to load data into net:
     *  - recordReader = the reader that loads and converts image data pass in inputSplit to initialize
     *  - dataIter = a generator that only loads one batch at a time into memory to save memory
     *  - trainIter = uses MultipleEpochsIterator to ensure model runs through the data for all epochs
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
      log.info("Training on transformation: {}" , transform.getClass().toString());
      recordReader.initialize(fileSplit, transform);
      dataIter = new RecordReaderDataSetIterator(recordReader, batchSize, 1, numLabels);
      scaler.fit(dataIter);
      dataIter.setPreProcessor(scaler);
      trainIter = new MultipleEpochsIterator(epochs, dataIter, nCores);
      // train the model even more with some transpositions of the original image.
      network.fit(trainIter);
    }
    log.info("Done training model..");
  }

  public void trainModel() throws IOException {
    trainModel("./training");
  }

  /*
   * This provides a list of various transforms to attempt on the image as part of the training process
   * this synthetically generates a larger training set.  I think it's typically used to train in
   * rotational and scale invariance in the matching of the network.  But, that's just a guess :)
   */
  private List<ImageTransform> createImageTransformList() {
    // TODO: consider a bunch of opencv filter based transforms here!
    ImageTransform flipTransform1 = new FlipImageTransform(rng);
    ImageTransform flipTransform2 = new FlipImageTransform(new Random(123));
    ImageTransform warpTransform = new WarpImageTransform(rng, 42);
    List<ImageTransform> transforms = Arrays.asList(new ImageTransform[]{flipTransform1, warpTransform, flipTransform2});
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

  // save the current model and it's set of labels
  public void saveModel(String filename) throws IOException {
    File dir = new File(modelDir);
    if (!dir.exists()) {
      dir.mkdirs();
      log.info("Creating models directory {}" , dir);
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

  // load a model based on its filename and the labels from an associated filename.labels file..
  public void loadModel(String filename) throws IOException {
    File f = new File(filename);
    log.info("Loading network from : {}", f.getAbsolutePath());
    network = ModelSerializer.restoreMultiLayerNetwork(f, true);
    log.info("Network restored. {}", network);
    // TODO: there has to be a better way to manage the labels!?!?!! Gah
    networkLabels = StringUtils.split(FileIO.toString(new File(filename + ".labels")), "\\|");
    modelNumLabels = networkLabels.size();
    log.info("Network labels {} objects", modelNumLabels);
  }

  public void evaluateModel(File file) throws IOException {
    log.info("Evaluate model....");
    NativeImageLoader nativeImageLoader = new NativeImageLoader(height, width, channels);
    INDArray image = nativeImageLoader.asMatrix(file);   // testImage is of Mat format
    // 0-255 to 0-1
    DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
    scaler.transform(image);
    // Pass through to neural Net
    INDArray output = network.output(image);
    System.out.println(output);
    System.out.println(networkLabels);
    // TODO: I suppose we could create a map of probabilities and return that ...
    // this map could be large
  }

  /* From the animals classification example */
  public MultiLayerNetwork lenetModel(int numLabels) {
    /**
     * Revisde Lenet Model approach developed by ramgo2 achieves slightly above random
     * Reference: https://gist.github.com/ramgo2/833f12e92359a2da9e5c2fb6333351c5
     **/
    MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
        .seed(seed)
        .iterations(iterations)
        .regularization(false).l2(0.005) // tried 0.0001, 0.0005
        .activation(Activation.RELU)
        .learningRate(0.0001) // tried 0.00001, 0.00005, 0.000001
        .weightInit(WeightInit.XAVIER)
        .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
        .updater(Updater.RMSPROP).momentum(0.9)
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

  /* From the animals classification example */
  public MultiLayerNetwork alexnetModel(int numLabels) {
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
        .iterations(iterations)
        .gradientNormalization(GradientNormalization.RenormalizeL2PerLayer) // normalize to prevent vanishing or exploding gradients
        .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
        .learningRate(1e-2)
        .biasLearningRate(1e-2*2)
        .learningRateDecayPolicy(LearningRatePolicy.Step)
        .lrPolicyDecayRate(0.1)
        .lrPolicySteps(100000)
        .regularization(true)
        .l2(5 * 1e-4)
        .momentum(0.9)
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
  
  // This is for the Model Zoo support to load in the VGG16 model.  
  public void loadVGG16() throws IOException {
    log.info("Loading the VGG16 Model.  Download is large 500+ MB.. this will be cached after it downloads");
    ZooModel zooModel = new VGG16();
    vgg16 = (ComputationGraph) zooModel.initPretrained(PretrainedType.IMAGENET);
    // TODO: return true/false if the model was loaded properly/successfully.
  }

  
  public Map<String, Double> classifyImageVGG16(IplImage iplImage) throws IOException {
    NativeImageLoader loader = new NativeImageLoader(224, 224, 3);
    BufferedImage buffImg = OpenCV.IplImageToBufferedImage(iplImage);
    INDArray image = loader.asMatrix(buffImg);
    // TODO: we should consider the model as not only the model, but also the input transforms
    // for that model.
    DataNormalization scaler = new VGG16ImagePreProcessor();
    scaler.transform(image);
    INDArray[] output = vgg16.output(false,image);
    // TODO: return a more native datastructure!
    //String predictions = TrainedModels.VGG16.decodePredictions(output[0]);
    // log.info("Image Predictions: {}", predictions);
    return decodeVGG16Predictions(output[0]);
  }
  
  public Map<String, Double> classifyImageFileVGG16(String filename) throws IOException {
    File file = new File(filename);
    NativeImageLoader loader = new NativeImageLoader(224, 224, 3);
    INDArray image = loader.asMatrix(file);
    // TODO: we should consider the model as not only the model, but also the input transforms
    // for that model.
    DataNormalization scaler = new VGG16ImagePreProcessor();
    scaler.transform(image);
    INDArray[] output = vgg16.output(false,image);
    // TODO: return a more native datastructure!
    //String predictions = TrainedModels.VGG16.decodePredictions(output[0]);
    //log.info("Image Predictions: {}", predictions);
    return decodeVGG16Predictions(output[0]);
  }
  
  
  // adapted from dl4j TrainedModels.VGG16 class.
  public Map<String, Double> decodeVGG16Predictions(INDArray predictions) {
    
    LinkedHashMap<String, Double> recognizedObjects = new LinkedHashMap<String, Double>(); 
    ArrayList<String> labels;
    String predictionDescription = "";
    int[] top5 = new int[5];
    float[] top5Prob = new float[5];
    labels = ImageNetLabels.getLabels();
    //brute force collect top 5
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
            recognizedObjects.put(labels.get(top5[i]), (double)top5Prob[i]);
            currentBatch.putScalar(0, top5[i], 0);
            predictionDescription += "\n\t" + String.format("%3f", top5Prob[i] * 100) + "%, " + labels.get(top5[i]);
            i++;
        }
    }
    return recognizedObjects;
  }
  
  static public ServiceType getMetaData() {
    ServiceType meta = new ServiceType(Deeplearning4j.class.getCanonicalName());
    meta.addDescription("A wrapper service for the Deeplearning4j framework.");
    meta.addCategory("ai");
    meta.addDependency("org.deeplearning4j", "0.8");
    // this also depends on opencv
    meta.addDependency("org.bytedeco.javacv", "1.3");
    // and this is just a good frame grabber.. might as well pull it in.
    meta.addDependency("pl.sarxos.webcam", "0.3.10");
    meta.addDependency("org.apache.commons.io", "2.3.0");
    return meta;
  }

  public static void main(String[] args) throws IOException {
    Deeplearning4j dl4j = (Deeplearning4j)Runtime.createAndStart("dl4j", "Deeplearning4j");
    // this is how many generations to iterate on training the dataset. larger number means longer training time.
    dl4j.epochs = 50;
    dl4j.trainModel("test/resources/animals");
    //dl4j.trainModel("training");
    // save the model out
    dl4j.saveModel();
    dl4j.loadModel();
    //File testIm = new File("test/resources/animals/turtle/Baby_sea_turtle.jpg");
    // File testIm = new File("test/resources/animals/deer/BlackTailed_Deer_Doe.jpg");
    File testIm = new File("test/resources/animals/turtle/Western_Painted_Turtle.jpg");
    // BufferedImage img = ImageIO.read(testIm);
    // Frame frame = grabberConverter.convert(img);
    dl4j.evaluateModel(testIm);
    // dl4j.evaluateModel(img);
    System.out.println("Done evaluating the model.");
    System.exit(0);
  }

}

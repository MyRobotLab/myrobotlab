package org.myrobotlab.service;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.myrobotlab.deeplearning4j.CustomModel;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVFilterDL4JTransfer;
import org.myrobotlab.programab.OOBPayload;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.slf4j.Logger;

/**
 * This is a reference implementation of Harry. Harry is an InMoov. (Harry /
 * Lloyd.. it's all the same.)
 * 
 * Very much a WIP
 * 
 * @author kwatters
 *
 */
public class Lloyd extends Service {

  public final static Logger log = LoggerFactory.getLogger(Lloyd.class);
  private static final long serialVersionUID = 1L;
  // TODO: mark for transient needed
  // All of the services that make up harry.
  private ProgramAB brain;

  private Solr memory;
  private Solr cloudMemory;

  // the cortex is the part of the brain responsible for image recognition..
  // this seems like a fitting name
  private Deeplearning4j visualCortex;

  // speech recognition (the ear!)
  private SpeechRecognizer ear;
  // Speech Synthesis
  private SpeechSynthesis mouth;
  // left and right eyes (camera)
  private OpenCV leftEye;
  private int leftEyeCameraIndex = 1;
  private OpenCV rightEye;
  private int rightEyeCameraIndex = 0;
  // the oculus for connecting to a remote inmoov
  private OculusRift oculusRift;

  private boolean record = true;
  private boolean enableSpeech = true;
  private boolean enableEyes = true;
  // these are probably mutually exclusive. but maybe not?!
  private boolean enableOculus = false;
  private boolean enableIK = false;

  private transient WebGui webgui;

  // Ok.. let's create 2 ik solvers one for the left hand, one for the right
  // hand.
  // "motor control"
  private InverseKinematics3D leftIK;
  private InverseKinematics3D rightIK;

  // oculus settings for telepresence
  public String leftEyeURL = "http://192.168.4.105:8080/?action=stream";
  public String rightEyeURL = "http://192.168.4.105:8081/?action=stream";

  public String cloudSolrUrl = "http://phobos:8983/solr/wikipedia";
  // TODO: add all of the servos and other mechanical stuff.

  // for telemanipulation
  // The URL to the remote MRL instance that is controlling the servos.
  public String skeletonBaseUrl = "http://192.168.4.108:8888/";

  // used by the cortex
  public String imageRecognizerModelFilename = "visual_cortex.bin";

  public Lloyd(String name) {
    super(name);
  }

  @Override
  public void startService() {
    super.startService();
    // additional initialization here i guess?
    if (enableSpeech) {
      startEar();
      startMouth();
    }
    if (enableEyes) {
      startEyes();
    }
    startBrain();
    startCortex();
    // start memory last :( can't attach eyes until the eyes exist.
    startMemory();

    // If we're in telepresence mode start the oculus service.
    if (enableOculus) {
      startOculus();
    }

    if (enableIK) {
      startIK();
    }
  }

  public void startEar() {
    ear = (WebkitSpeechRecognition) Runtime.start("ear", "WebkitSpeechRecognition");
  }

  public void startMouth() {
    mouth = (MarySpeech) Runtime.start("mouth", "MarySpeech");
  }

  public void startBrain() {
    brain = (ProgramAB) Runtime.start("brain", "ProgramAB");
    // TODO: setup the AIML / chat bot directory for all of this.
    brain.startSession("ProgramAB", "person", "lloyd");
  }

  public void initializeBrain() {
    // programmatically add the aiml instead of from the file system
    // TODO: add some actual aiml files
    brain.addCategory("*", "Default");
    brain.addCategory("Hello", "Hi");
    // we need a category to turn on the camera.
    // we need a category to set the training label
    String trainingPattern = "THIS IS *";
    // OOB tag here.
    int numExamples = 50;
    String trainingTemplate = "Learning <star/><oob><mrl><service>memory</service><method>setTrainingLabel</method><param><star/></param><param>" + numExamples
        + "</param></mrl></oob>";
    brain.addCategory(trainingPattern, trainingTemplate);

    trainingPattern = "THIS IS A *";
    // OOB tag here.
    trainingTemplate = "Learning <star/><oob><mrl><service>memory</service><method>setTrainingLabel</method><param><star/></param><param>" + numExamples + "</param></mrl></oob>";
    brain.addCategory(trainingPattern, trainingTemplate);

    trainingPattern = "THIS IS AN *";
    // OOB tag here.
    trainingTemplate = "Learning <star/><oob><mrl><service>memory</service><method>setTrainingLabel</method><param><star/></param><param>" + numExamples + "</param></mrl></oob>";
    brain.addCategory(trainingPattern, trainingTemplate);

    trainingPattern = "THIS IS THE *";
    // OOB tag here.
    trainingTemplate = "Learning <star/><oob><mrl><service>memory</service><method>setTrainingLabel</method><param><star/></param><param>" + numExamples + "</param></mrl></oob>";
    brain.addCategory(trainingPattern, trainingTemplate);

    // I want a category that will tell my to rebuild my brain!
    String reloadPattern = "BUILD A MODEL";
    String buildModelTemplate = "Re-compiling my visual cortex. <oob><mrl><service>lloyd</service><method>updateRecognitionModel</method></mrl></oob>";
    brain.addCategory(reloadPattern, buildModelTemplate);

  }

  public void addWikiLookups() {
    // Start working on questions.
    inventorLookup();
    // add more "lookups" ?
    // What questions ?
    whatTypeLookup();
    // Where questions ?
    tellMeAboutLookup();
  }

  public void inventorLookup() {
    // Who questions.
    String pattern = "WHO IS THE INVENTOR OF THE *";
    String fieldName = "infobox_inventor_ss";
    String prefix = "The inventor was ";
    String suffix = ".  ";
    createKnowledgeLookup(pattern, fieldName, prefix, suffix);
  }

  public void whatTypeLookup() {
    String pattern = "WHAT IS *";
    String fieldName = "infobox_type";
    String prefix = "<star/> is a ";
    String suffix = ". ";
    createKnowledgeLookup(pattern, fieldName, prefix, suffix);
  }

  public void tellMeAboutLookup() {
    String pattern = "TELL ME ABOUT *";
    String fieldName = "text";
    String prefix = "";
    String suffix = ". ";
    createKnowledgeLookup(pattern, fieldName, prefix, suffix);
  }

  public void createKnowledgeLookup(String pattern, String fieldName, String prefix, String suffix) {
    OOBPayload oobTag = createSolrFieldSearchOOB(fieldName);
    // TODO: handle (in the template) a zero hit result ?)
    String template = prefix + OOBPayload.asBlockingOOBTag(oobTag) + suffix;
    brain.addCategory(pattern, template);
  }

  private OOBPayload createSolrFieldSearchOOB(String fieldName) {
    String serviceName = "cloudMemory";
    // TODO: make this something that's completely abstracted out from here.
    String methodName = "fetchFirstResultSentence";
    ArrayList<String> params = new ArrayList<String>();
    // TODO: add the "qf" parameter to improve precision/recall
    // TODO: add the has infobox as a filter query
    // params.add("title:<star/> text:<star/> +" + fieldName + ":*
    // +has_infobox:true");
    params.add("infobox_name_ss:<star/> title:<star/> text:<star/> +" + fieldName + ":* +has_infobox:true");
    params.add(fieldName);
    OOBPayload oobTag = new OOBPayload(serviceName, methodName, params);
    return oobTag;
  }

  public void startMemory() {
    memory = (Solr) Runtime.start("memory", "Solr");
    // TODO: add config to use embedded or external
    try {
      memory.startEmbedded();
    } catch (SolrServerException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    // next we want to subscribe to specific data. initially just opencv.
    // TODO: we can only attach after eyes are started.
    if (record) {
      memory.attach(leftEye);
      memory.attach(brain);
    }
    cloudMemory = (Solr) Runtime.start("cloudMemory", "Solr");
    cloudMemory.setSolrUrl(cloudSolrUrl);
  }

  public void startCortex() {
    visualCortex = (Deeplearning4j) Runtime.start("visualCortex", "Deeplearning4j");
    // TODO: ?? any other initialization? load the current image recognition
    // model?

  }

  //
  public void updateRecognitionModel() throws IOException {
    // Here we should train a new image recognition model..
    // when that's done.. have it update the current model in use by the
    // dl4jtranfer filter.
    int seed = 42;
    double trainPerc = 0.5;
    // vgg16 specific values
    int channels = 3;
    int height = 224;
    int width = 224;
    // training mini batch size.
    int batch = 20;
    // target accuracy
    double targetAccuracy = 0.90;
    String featureExtractionLayer = "fc2";
    int maxEpochs = 5;

    String queryString = "+has_bytes:true -label:unknown";
    String labelField = "label";
    SolrQuery datasetQuery = memory.makeDatasetQuery(queryString, labelField);
    // run that query.. get the number of items and the labels
    QueryResponse resp = memory.search(datasetQuery);
    long numFound = resp.getResults().getNumFound();
    // sorted list (according to solr) of the labels for this data set
    FacetField labelFacet = resp.getFacetField("label");
    // maintain sort order with a linked hash set
    List<String> labels = new ArrayList<String>();
    for (Count c : labelFacet.getValues()) {
      labels.add(c.getName());
    }
    Collections.sort(labels);
    long trainMaxOffset = (long) ((double) numFound * trainPerc);
    long testMaxOffset = (long) ((double) numFound * (1.0 - trainPerc));

    // training query
    SolrQuery trainQuery = memory.makeDatasetQuery(queryString, labelField);
    trainQuery.addSort("random_" + seed, ORDER.asc);
    trainQuery.setRows((int) trainMaxOffset);
    DataSetIterator trainIter = visualCortex.makeSolrInputSplitIterator(memory, trainQuery, numFound, labels, batch, height, width, channels);

    // testing query
    SolrQuery testQuery = memory.makeDatasetQuery(queryString, labelField);
    testQuery.addSort("random_" + seed, ORDER.desc);
    testQuery.setRows((int) testMaxOffset);
    DataSetIterator testIter = visualCortex.makeSolrInputSplitIterator(memory, testQuery, numFound, labels, batch, height, width, channels);
    //
    // String filename = "my_new_model.bin";
    // TODO: make this runnable?
    // At this point we should null out the current model so it stops
    // classifying.
    ((OpenCVFilterDL4JTransfer) leftEye.getFilter("dl4jTransfer")).unloadModel();

    CustomModel imageRecognizer = visualCortex.trainAndSaveModel(labels, trainIter, testIter, imageRecognizerModelFilename, maxEpochs, targetAccuracy, featureExtractionLayer);

    // now we want to get a hold of the dl4j transfer filter and set the model
    // on it with this one.
    // OpenCVFilterDL4JTransfer("dl4jTransfer");
    ((OpenCVFilterDL4JTransfer) leftEye.getFilter("dl4jTransfer")).loadCustomModel(imageRecognizerModelFilename);

    // ok. once that model is saved.

  }

  public void startEyes() {
    // TODO: enable right eye / config
    // rightEye = (OpenCV)Runtime.start("rightEye", "OpenCV");
    leftEye = (OpenCV) Runtime.start("leftEye", "OpenCV");
    // let's start up the trained transfer learning model here.
    OpenCVFilterDL4JTransfer dl4jTransfer = new OpenCVFilterDL4JTransfer("dl4jTransfer");
    dl4jTransfer.loadCustomModel(imageRecognizerModelFilename);
    leftEye.addFilter(dl4jTransfer);
    leftEye.cameraIndex = leftEyeCameraIndex;
    // TODO: ?
    leftEye.capture();
  }

  public void startOculus() {
    oculusRift = (OculusRift) Runtime.start("oculusRift", "OculusRift");
    oculusRift.setLeftEyeURL(leftEyeURL);
    oculusRift.setRightEyeURL(rightEyeURL);
    oculusRift.leftCameraAngle = 0;
    oculusRift.leftCameraDy = -25;
    oculusRift.rightCameraAngle = 180;
    oculusRift.rightCameraDy = 25;
    // call this once you've updated the affine stuff?
    oculusRift.updateAffine();
    oculusRift.initContext();
    oculusRift.logOrientation();
  }

  public void startIK() {
    leftIK = (InverseKinematics3D) Runtime.start("leftIK", "InverseKinematics3D");
    rightIK = (InverseKinematics3D) Runtime.start("rightIK", "InverseKinematics3D");

    // TODO : proper IK models for left & right.
    leftIK.setCurrentArm(InMoovArm.getDHRobotArm());
    rightIK.setCurrentArm(InMoovArm.getDHRobotArm());

    // specify the input scaling factors TODO: what should these be?
    // z axis is inverted!
    // invert z again?!
    leftIK.createInputScale(1000.0, 1000.0, 1000.0);
    rightIK.createInputScale(1000.0, 1000.0, 1000.0);

    // create the translation/rotation input matrix.
    leftIK.createInputMatrix(0, 0, 0, 0, 0, 0);
    rightIK.createInputMatrix(0, 0, 0, 0, 0, 0);

    // we should probably put the joint angles in the middle.
    leftIK.centerAllJoints();
    rightIK.centerAllJoints();

    // TODO: our palm position when we're centered.. probably could be
    // calibration for input translate/rotate / scale matrix?
    Point leftHandPos = leftIK.getCurrentArm().getPalmPosition();
    Point rightHandPos = rightIK.getCurrentArm().getPalmPosition();

    // Initial position of left hand
    log.info("Initial Left Hand Position : {}", leftHandPos);
    log.info("Initial Right Hand Position : {}", rightHandPos);

    oculusRift.addListener("publishLeftHandPosition", leftIK.getName(), "onPoint");
    // TODO: re-enable me.. for now .. just left hand as we're debugging.
    // oculusRift.addListener("publishRightHandPosition", rightIK.getName(),
    // "onPoint");

    leftIK.addListener("publishJointAngles", getName(), "onLeftJointAngles");
    rightIK.addListener("publishJointAngles", getName(), "onRightJointAngles");

  }

  public void launchWebGui() {
    // # now start the webgui
    webgui = (WebGui) Runtime.create("webgui", "WebGui");
    webgui.autoStartBrowser(false);
    webgui.startService();
    webgui.startBrowser("http://localhost:8888/#/service/" + ear.getName());
  }

  public void attachCallbacks() {
    // ear callback to programb
    // ear.attach(brain);
    ear.addTextListener(brain);
    // ear knows when it's speaking
    // mouth.addEar(ear);
    ear.addMouth(mouth);
    // ear.attach(mouth);
    // brain speaks to the mouth
    brain.addTextListener(mouth);
  }

  // TODO: similar approach for sending the oculus head tracking info to the
  // remote !

  public void onLeftJointAngles(Map<String, Double> angleMap) {
    // This is our callback for the IK stuff!
    // calibrate between DH model and real world model.
    angleMap = calibrateLeftAngles(angleMap);
    log.info("Left Joint Angles updated! {}", angleMap);
    for (String key : angleMap.keySet()) {
      log.info("Left Send update to {} set to {}", key, angleMap.get(key));
      try {
        // /api/service/i01.head.neck/moveTo/
        String servoRestUri = skeletonBaseUrl + "api/service/i01.leftArm." + key + "/moveTo/" + angleMap.get(key);
        log.info("Invoking URL : {}", servoRestUri);
        URL uri = new URL(servoRestUri);
        uri.openConnection().getInputStream().close();
        // gah..
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    }
  }

  public static Map<String, Double> calibrateLeftAngles(Map<String, Double> angleMap) {

    HashMap<String, Double> calibratedMap = new HashMap<String, Double>();

    // we map the servo 90 degrees to be 0 degrees.
    HashMap<String, Double> phaseShiftMap = new HashMap<String, Double>();

    // When reviewing the current dh model..
    phaseShiftMap.put("omoplate", -90.0);
    phaseShiftMap.put("shoulder", -90.0);
    phaseShiftMap.put("rotate", 90.0);
    phaseShiftMap.put("bicep", -90.0);

    // TODO: is the direction of rotation correct?
    HashMap<String, Double> gainMap = new HashMap<String, Double>();
    gainMap.put("omoplate", 1.0);
    gainMap.put("shoulder", 1.0);
    gainMap.put("rotate", 1.0);
    gainMap.put("bicep", 1.0);

    for (String key : angleMap.keySet()) {
      //
      Double calibrated = (angleMap.get(key) * gainMap.get(key) + phaseShiftMap.get(key)) % 360;
      log.info("Target Servo: {} Input Value: {} and Calibrated Value: {}", key, angleMap.get(key), calibrated);
      calibratedMap.put(key, calibrated);
    }

    return calibratedMap;

  }

  public void onRightJointAngles(Map<String, Double> angleMap) {
    // This is our callback for the IK stuff!
    log.info("Right Joint Angles updated! {}", angleMap);
    for (String key : angleMap.keySet()) {
      log.info("Right Send update to {} set to {}", key, angleMap.get(key));
    }

  }

  static public ServiceType getMetaData() {
    ServiceType meta = new ServiceType(Lloyd.class.getCanonicalName());
    meta.addDescription("Lloyd an evolved InMoov.");
    meta.addCategory("robot");
    // TODO: add pears
    return meta;
  }

  public static void main(String[] args) throws SolrServerException, IOException {
    //
    LoggingFactory.init("INFO");
    Lloyd lloyd = (Lloyd) Runtime.start("lloyd", "Lloyd");
    SwingGui gui = (SwingGui) Runtime.start("gui", "SwingGui");
    // start python
    Runtime.start("python", "Python");
    gui.undockTab("memory");
    gui.undockTab("brain");
    gui.undockTab("leftEye");
    // gui.undockTab("leftEye");
    // opencvdata_214c7381-ddfe-406a-adfa-f1bf9aebd367
    lloyd.initializeBrain();
    lloyd.addWikiLookups();
    // Initialize a clean index!
    // lloyd.memory.deleteEmbeddedIndex();
    // lloyd.memory.fetchImage("id:opencvdata_214c7381-ddfe-406a-adfa-f1bf9aebd367");
    lloyd.attachCallbacks();
    // TODO: re-enable?
    // lloyd.launchWebGui();
  }

}

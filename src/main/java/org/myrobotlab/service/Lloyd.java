package org.myrobotlab.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.solr.client.solrj.SolrServerException;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.programab.OOBPayload;
import org.slf4j.Logger;

/**
 * This is a reference implementation of Harry.
 * Harry is an InMoov.  (Harry / Lloyd.. it's all the same.)
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
  
  private WebkitSpeechRecognition ear;
  private MarySpeech mouth;
  private OpenCV leftEye;
  private int leftEyeCameraIndex = 1;
  private OpenCV rightEye;
  private int rightEyeCameraIndex = 0;
  private OculusRift oculusRift;
  private boolean record = false;
  private boolean enableSpeech = true;
  private boolean enableEyes = false;
  // these are probably mutually exclusive.  but maybe not?!
  private boolean enableOculus = true;
  
  private boolean enableIK = true;
  
  private transient WebGui webgui;
  
  // Ok.. let's create 2 ik solvers one for the left hand, one for the right hand.
  private InverseKinematics3D leftIK;
  private InverseKinematics3D rightIK;
  
  public String leftEyeURL  = "http://192.168.4.104:8080/?action=stream";
  public String rightEyeURL = "http://192.168.4.104:8081/?action=stream";
  
  public String cloudSolrUrl = "http://phobos:8983/solr/wikipedia";
  // TODO: add all of the servos and other mechanical stuff.
  
  
  // The URL to the remote MRL instance that is controlling the servos.
  public String skeletonBaseUrl = "http://192.168.4.107:8888/";
  
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
    // start memory last :(  can't attach eyes until the eyes exist.
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
    ear = (WebkitSpeechRecognition)Runtime.start("ear", "WebkitSpeechRecognition");
  }
  
  public void startMouth() {
    mouth = (MarySpeech)Runtime.start("mouth", "MarySpeech");
  }
  
  public void startBrain() {
    brain = (ProgramAB)Runtime.start("brain", "ProgramAB");
    // TODO: setup the AIML / chat bot directory for all of this.
    brain.startSession("ProgramAB",  "person", "lloyd");
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
    String trainingTemplate = "Learning <star/><oob><mrl><service>memory</service><method>setTrainingLabel</method><param><star/></param><param>"+numExamples+"</param></mrl></oob>";
    brain.addCategory(trainingPattern, trainingTemplate);
    
    trainingPattern = "THIS IS A *";
    // OOB tag here. 
    trainingTemplate = "Learning <star/><oob><mrl><service>memory</service><method>setTrainingLabel</method><param><star/></param><param>"+numExamples+"</param></mrl></oob>";
    brain.addCategory(trainingPattern, trainingTemplate);
    

    trainingPattern = "THIS IS AN *";
    // OOB tag here. 
    trainingTemplate = "Learning <star/><oob><mrl><service>memory</service><method>setTrainingLabel</method><param><star/></param><param>"+numExamples+"</param></mrl></oob>";
    brain.addCategory(trainingPattern, trainingTemplate);

    
    trainingPattern = "THIS IS THE *";
    // OOB tag here. 
    trainingTemplate = "Learning <star/><oob><mrl><service>memory</service><method>setTrainingLabel</method><param><star/></param><param>"+numExamples+"</param></mrl></oob>";
    brain.addCategory(trainingPattern, trainingTemplate);
    
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
  
  public void  whatTypeLookup() {
    String pattern = "WHAT IS *";
    String fieldName = "infobox_type";
    String prefix = "<star/> is a ";
    String suffix = ". ";
    createKnowledgeLookup(pattern, fieldName, prefix, suffix);
  }
  
  
  public void  tellMeAboutLookup() {
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
    // params.add("title:<star/> text:<star/> +" + fieldName + ":* +has_infobox:true");
    params.add("infobox_name_ss:<star/> title:<star/> text:<star/> +" + fieldName + ":* +has_infobox:true");
    params.add(fieldName);
    OOBPayload oobTag = new OOBPayload(serviceName, methodName, params);
    return oobTag;
  }
  
  public void startMemory() {
    memory = (Solr)Runtime.start("memory", "Solr");
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
    cloudMemory = (Solr)Runtime.start("cloudMemory", "Solr");
    cloudMemory.setSolrUrl(cloudSolrUrl);
  }
  
  public void startEyes() {
    
    // TODO: enable right eye / config
    // rightEye = (OpenCV)Runtime.start("rightEye", "OpenCV");
    leftEye = (OpenCV)Runtime.start("leftEye", "OpenCV");
    
    leftEye.cameraIndex = leftEyeCameraIndex;
    // TODO: ?
    leftEye.capture();
  }
  
  public void startOculus() {
    oculusRift = (OculusRift)Runtime.start("oculusRift", "OculusRift");
    
    
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
    leftIK = (InverseKinematics3D)Runtime.start("leftIK", "InverseKinematics3D");
    rightIK = (InverseKinematics3D)Runtime.start("rightIK", "InverseKinematics3D");
    
    // TODO : proper IK models for left & right.
    leftIK.setCurrentArm(InMoovArm.getDHRobotArm());
    rightIK.setCurrentArm(InMoovArm.getDHRobotArm());

    // specify the input scaling factors  TODO: what should these be?
    leftIK.createInputScale(500.0, 500.0, 500.0);
    rightIK.createInputScale(500.0, 500.0, 500.0);

    
    // create the translation/rotation input matrix.
    leftIK.createInputMatrix(50, 250, 500, 0, 0, 0);
    rightIK.createInputMatrix(50, 250, 500, 0, 0, 0);
    
    // we should probably put the joint angles in the middle.
    leftIK.centerAllJoints();
    rightIK.centerAllJoints();
    
    // TODO: our palm position when we're centered.. probably could be calibration for input translate/rotate / scale matrix?
    Point leftHandPos = leftIK.getCurrentArm().getPalmPosition();
    Point rightHandPos = rightIK.getCurrentArm().getPalmPosition();
    
    // Initial position of left hand
    log.info("Initial Left Hand Position : {}", leftHandPos );
    log.info("Initial Right Hand Position : {}", rightHandPos );
    
    oculusRift.addListener("publishLeftHandPosition", leftIK.getName(), "onPoint");
    // TODO: re-enable me.. for now .. just left hand as we're debugging.
    // oculusRift.addListener("publishRightHandPosition", rightIK.getName(), "onPoint");
    
    leftIK.addListener("publishJointAngles", getName(), "onLeftJointAngles");
    rightIK.addListener("publishJointAngles", getName(), "onRightJointAngles");
    
    
    
  }
  
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(Lloyd.class.getCanonicalName());
    meta.addDescription("Lloyd an evolved InMoov.");
    meta.addCategory("robot");
    // TODO: add pears
    return meta;
  }
  
  public void launchWebGui() {
    // # now start the webgui
    webgui = (WebGui)Runtime.create("webgui", "WebGui");
    webgui.autoStartBrowser(false);
    webgui.startService();
    webgui.startBrowser("http://localhost:8888/#/service/"+ear.getName());
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
  
  public static void main(String[] args) throws SolrServerException, IOException, JAXBException {
    // 
    LoggingFactory.init("INFO");
    Lloyd lloyd = (Lloyd)Runtime.start("lloyd", "Lloyd");
    SwingGui gui = (SwingGui)Runtime.start("gui", "SwingGui");
    // start python
    Runtime.start("python", "Python");
    gui.undockTab("memory");
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

  // TODO: similar approach for sending the oculus head tracking info to the remote !
  
  public void onLeftJointAngles(Map<String, Double> angleMap) {
    // This is our callback for the IK stuff! 
    log.info("Left Joint Angles updated! {}", angleMap);
    for (String key : angleMap.keySet()) {
      log.info("Left Send update to {} set to {}", key, angleMap.get(key));
      try {
        //  /api/service/i01.head.neck/moveTo/
        String servoRestUri = skeletonBaseUrl + "api/service/i01.leftArm." +key+ "/moveTo/" + angleMap.get(key);
        log.info("Invoking URL : {}" , servoRestUri);
        URL uri = new URL(servoRestUri);
        uri.openConnection().getInputStream().close();
        // gah.. 
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
    }
  }

  public void onRightJointAngles(Map<String, Double> angleMap) {
    // This is our callback for the IK stuff!  
    log.info("Right Joint Angles updated! {}", angleMap);
    for (String key : angleMap.keySet()) {
      log.info("Right Send update to {} set to {}", key, angleMap.get(key));
    }

  }

  
}

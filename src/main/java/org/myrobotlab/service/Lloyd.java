package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.bind.JAXBException;

import org.apache.solr.client.solrj.SolrServerException;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
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
  // TODO: mark for transienta s needed
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
  
  private boolean record = false;
  private boolean enableSpeech = true;
  private boolean enableEyes = false;
  
  private transient WebGui webgui;
  
  
  String cloudSolrUrl = "http://phobos:8983/solr/wikipedia";
  // TODO: add all of the servos and other mechanical stuff.
  
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
    String methodName = "fetchFirstResultField";
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
    //LoggingFactory.init("INFO");
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

    
    
    lloyd.launchWebGui();
    
    // opencvdata_214c7381-ddfe-406a-adfa-f1bf9aebd367 is an image!
  }


}

package org.myrobotlab.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.OakDConfig;
import org.myrobotlab.service.data.Classification;
import org.slf4j.Logger;

/**
 * 
 * https://github.com/luxonis/depthai
 * python3 depthai_demo.py -cb callbacks.py
 * 
 * https://github.com/luxonis/depthai-experiments/tree/master/gen2-face-recognition
 * 
 * @author GroG
 *
 */
public class OakD extends Service<OakDConfig> {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OakD.class);

  private transient Py4j py4j = null;

  public OakD(String n, String id) {
    super(n, id);
  }

  public void startService() {
    super.startService();

    py4j = (Py4j) startPeer("py4j");

    if (config.py4jInstall) {
      installDepthAi();
    }

  }

  /**
   * starting install of depthapi
   */
  public void publishInstallStart() {
  }

  public Status publishInstallFinish() {
    return Status.error("depth ai install was not successful");
  }

  /**
   * For depthai we need to clone its repo and install requirements
   * 
   */
  public void installDepthAi() {
    try {

      // git.clone("./", config.depthaiCloneUrl)
      List<String> packages = new ArrayList<>();
      packages.add("depthai==2.20.2.0");
      packages.add("blobconverter==1.3.0");
      packages.add("opencv-python");
      packages.add("numpy");
      py4j.installPipPackages(packages);
    } catch (IOException e) {
      error(e);
    }
    // py4j.exec("");
  }
  
  public void startRecognition() {
    
  }
  
  public void stopRecognition() {
    
  }
  
  /**
   * FIXME - turn into interface
   * Will publish processing messages to the processor(s) currently 
   * subscribed.
   * @param method
   * @param data
   */
  public void processMessage(String method, Object data) {
    String processor = getPeerName("py4j");
    Message msg = Message.createMessage(getName(), processor, method, data);
    invoke("publishProcessMessage", msg);
  }

  /**
   * FIXME - turn into interface
   * Processing publishing point, where everything InMoov2 wants to be processed
   * is turned into a message and published.
   * 
   * @param msg
   * @return
   */
  public Message publishProcessMessage(Message msg) {
    return msg;
  }

  
  public Classification publishClassification(Classification classification) {
    classification.src = getName();
    // we have a detection or recognition event ... publish the associated image
    config.displayWeb = true;
    if (config.displayWeb) {
      // imageToWeb("classification.png");
      invoke("imageToWeb", "classification.png");
    }
    
    return classification;
  }
  
  public String imageToWeb(String filePath) {
    byte[] content = null;
    try (FileInputStream fileInputStream = new FileInputStream(new File(filePath))) {
        content = new byte[(int) new File(filePath).length()];
        fileInputStream.read(content);
        String img = Base64.getEncoder().encodeToString(content);
        return String.format("data:image/png;base64,%s", img);
    } catch (IOException e) {
        error(e);
    }
    return null;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("oakd", "OakD");
      Runtime.start("servo", "Servo");
      Runtime.start("webgui", "WebGui");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}

package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
 * 
 * 
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
  
  public Classification publishClassification(Classification classification) {
    return classification;
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

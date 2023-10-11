package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.process.GitHub;
import org.myrobotlab.service.config.OakDConfig;
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
  private transient Git git = null;
  
  public OakD(String n, String id) {
    super(n, id);
  }
  
  public void startService() {
    super.startService();
    
    py4j = (Py4j)startPeer("py4j");
    git = (Git)startPeer("git");
    
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
    
    //git.clone("./", config.depthaiCloneUrl)
    py4j.exec("");
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("camera", "OakD");
      Runtime.start("servo", "Servo");
      Runtime.start("webgui", "WebGui");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}

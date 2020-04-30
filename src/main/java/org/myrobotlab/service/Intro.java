package org.myrobotlab.service;

import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.repo.Repo;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;
import org.myrobotlab.service.interfaces.ServoControl;

public class Intro extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Intro.class);

  public class TutorialInfo {

    public String id;
    public String title;
    public boolean isInstalled;
    public String[] servicesRequired;
    public String script;

  }

  Map<String, TutorialInfo> tutorials = new TreeMap<>();

  public Intro(String n, String id) {
    super(n, id);
    try {
      Runtime runtime = Runtime.getInstance();
      Repo repo = runtime.getRepo();
      TutorialInfo tuto = new TutorialInfo();
      tuto.id = "servo-hardware";
      tuto.title = "Servo with Arduino Hardware";
      tuto.servicesRequired = new String[] { "Servo", "Arduino" };
      tuto.isInstalled = repo.isInstalled(Servo.class) && repo.isInstalled(Arduino.class);

      tuto.script = getResourceAsString(Servo.class, "Servo.py"); //FileIO.toString(getResource(Servo.class,"Servo.py"));
      tutorials.put(tuto.title, tuto);
    } catch (Exception e) {
      log.error("Intro constructor threw", e);
    }
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(Intro.class);
    meta.addDescription("Introduction to MyRobotlab");
    meta.setAvailable(true);
    meta.addCategory("general");
    meta.addPeer("servo", "Servo", "servo");
    meta.addPeer("controller", "Arduino", "Arduino controller for this servo");
    return meta;
  }

  public void checkInstalled(String forTutorial, String serviceType) {
    Runtime runtime = Runtime.getInstance();
    Repo repo = runtime.getRepo();

    TutorialInfo tutorial = new TutorialInfo();
    tutorial.title = forTutorial;
    tutorial.isInstalled = repo.isInstalled(serviceType);
  }

  /**
	 * This method will load a python file into the python interpreter.
	 */
	public static boolean loadFile(String file) {
		File f = new File(file);
		Python p = (Python) Runtime.getService("python");
		log.info("Loading  Python file {}", f.getAbsolutePath());
		if (p == null) {
			log.error("Python instance not found");
			return false;
		}
		String script = null;
		try {
			script = FileIO.toString(f.getAbsolutePath());
		} catch (IOException e) {
			log.error("IO Error loading file : ", e);
			return false;
		}
		// evaluate the scripts in a blocking way.
		boolean result = p.exec(script, true);
		if (!result) {
			log.error("Error while loading file {}", f.getAbsolutePath());
			return false;
		} else {
			log.debug("Successfully loaded {}", f.getAbsolutePath());
		}
		return true;
	}

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);
      
      Runtime.main(new String[] { "--interactive", "--id", "admin", "-s", "intro", "Intro", "python", "Python" });
      
      // Arduino arduino = (Arduino)Runtime.start("arduino", "Arduino");
      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      // webgui.setSsl(true);
      webgui.autoStartBrowser(false);
      webgui.setPort(8888);
      webgui.startService();


    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  transient ServoControl servo;

  boolean isServoActivated = false;

  public boolean isServoActivated() {
    return isServoActivated;
  }

  public ServoControl startServo(String port) {
    return startServo(port, 3);
  }

  public ServoControl startServo(String port, int pin) {
  
    if (servo == null) {
      speakBlocking("starting servo");
      isServoActivated = true;

      servo = (ServoControl) startPeer("servo");

      if (port != null) {
        try {
          speakBlocking(port);
          Arduino controller = (Arduino) startPeer("controller");
          controller.connect(port);
          controller.attach(controller, pin);
        } catch (Exception e) {
          error(e);
        }
      }
    }
    return servo;
  }

  public void stopServo() {
    speakBlocking("stopping servo");
    releasePeer("servo");
    isServoActivated = false;
  }

}

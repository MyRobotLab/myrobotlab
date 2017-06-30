package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.framework.Message;
/**
*This class exists as a proxy to control native MRL services connected through the web API. 
*Methods of that native service are called through this class's {@link #exec(String, Object[])} method.
**/
public class PythonProxy extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(_TemplateService.class);

  public static WebGui webgui;

  public PythonProxy(String n) {
    super(n);
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

    ServiceType meta = new ServiceType(PythonProxy.class.getCanonicalName());
    meta.addDescription("Provides an API hook point to services written in native Python.");
    meta.setAvailable(true); // false if you do not want it viewable in a gui
    // add dependency if necessary
    // meta.addDependency("org.coolproject", "1.0.0");
    meta.addPeer("webgui", "WebGui", "This is used as the API entrance point");
    meta.addCategory("bridge");
    return meta;
  }

  public void startService() {
	super.startService();
	webgui = (WebGui) Runtime.getService("webgui");
	if (webgui == null) {
		webgui = (WebGui) createPeer("webgui");
		webgui.startService();
	}
  }

  /**
  * Execute method with params dat on the native service this class represents.
  **/
  public void exec(String method, Object[] dat) {
	//Create message
	Message msg = new Message();
	msg.name = getName();
	msg.method = method;
	msg.data = dat;
	exec(msg);
  }


  /**
  * Execute Message msg on the native service this class represents.
  **/
  public void exec(Message msg) {
	webgui.broadcast(msg);
  }

  /***
  *This is used to start the native service. Currently only Linux is supported
  *
  **/
  public void startNativeService(String location) {
	String os = (String) System.getProperty("os.name");
	if (os.toLowerCase().equals("linux")) {
		try {
			java.lang.Runtime.getRuntime().exec("/usr/bin/python " + location + " " + getName());
		} catch (Exception e) {
			Logging.logError(e);
		}
	} else {
		log.error("Native services are only supported on linux.");
	}
  }


  /**
  *This is called by the native service to start the handshake sequence.
  *After the handshake is completed, both this class and the native service should remain in sync
  **/
  public void handshake() {
	exec("handshake", (Object[]) null);
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("test", "PythonProxy");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}

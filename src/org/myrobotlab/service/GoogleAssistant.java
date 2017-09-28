package org.myrobotlab.service;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
/**
*This class exists as a proxy to control native MRL services connected through the web API. 
*Methods of that native service are called through this class's {@link #exec(String, Object[])} method.
**/
public class GoogleAssistant extends PythonProxy {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public GoogleAssistant(String n) {
    super(n);
  }

  public Object test(String testString) {
	return exec("test", new Object[] {(Object) testString});
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

    ServiceType meta = new ServiceType(GoogleAssistant.class.getCanonicalName());
    meta.addDescription("Access Google Assistant through voice interaction");
    meta.setAvailable(true); // false if you do not want it viewable in a gui
    meta.addCategory("ai");
    meta.setCloudService(true);
    return PythonProxy.addMetaData(meta);
  }

  public void startService() {
	super.startService();
	start();
  }

  public void start() {
	exec("start", (Object[]) null);
  }

  public void stop() {
	exec("stop", (Object[]) null);
  }


  //Required because of Java reflection weirdness
  public void handshake() {
	super.handshake();
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      GoogleAssistant test = (GoogleAssistant) Runtime.start("test", "GoogleAssistant");
      test.test("This is a test");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}

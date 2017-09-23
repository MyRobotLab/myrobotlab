package org.myrobotlab.service;

import org.myrobotlab.service.PythonProxy;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.framework.Message;
import java.io.File;
/**
*This class exists as a proxy to control native MRL services connected through the web API. 
*Methods of that native service are called through this class's {@link #exec(String, Object[])} method.
**/
public class _TemplateProxy extends PythonProxy {

  public _TemplateProxy(String n) {
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

    ServiceType meta = new ServiceType(_TemplateProxy.class.getCanonicalName());
    meta.addDescription("Template proxy service");
    meta.setAvailable(false);
    return PythonProxy.addMetaData(meta); //This is used so that the dependencies and peers are automatically added
  }


  //Required because of Java reflection weirdness
  public void handshake() {
	super.handshake();
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      _TemplateProxy test = (_TemplateProxy) Runtime.start("test", "_TemplateProxy");
      test.test("This is a test");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}

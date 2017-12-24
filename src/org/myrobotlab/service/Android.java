package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Android extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Android.class);

  // TODO FUTURE - LIST OF ALL SENSORS
  // List<Sensor> sensors = null;

  public static class Motion {
    public double x;
    public double y;
    public double z;

    public Motion(double x, double y, double z) {
      this.x = x;
      this.y = y;
      this.z = z;
    }
  }

  // SO the Webgui Does NOT USE THE INBOX BUT INVOKES
  // DIRECTLY !!!! ON THE SERVICE !! ONE DOWNSIDE OF THIS IS
  // THE RESULT IS NOT PUT ON THE BUS !!! - PERHAPS IT SHOULD USE THE INBOX !!!!
  public void motion(double x, double y, double z) {
    log.info("x {} y {} z {}", x, y, z);

    invoke("publishMotion", new Motion(x, y, z));
    // return publishMotion();
  }

  public void proximity(Integer proximity) {
    log.info("proximity {}", proximity);

    invoke("publishProximity", proximity);
    // return publishMotion();
  }

  public Motion publishMotion(Motion m) {
    return m;
  }

  public Integer publishProximity(Integer m) {
    return m;
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      // Android template = (Android) Runtime.start("template",
      // "_TemplateService");

      Runtime.start("anrdoi", "Android");
      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public Android(String n) {
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

    ServiceType meta = new ServiceType(Android.class.getCanonicalName());
    meta.addDescription("re-publishes Android proximity and position information");
    meta.addCategory("sensor");
    meta.setLicenseApache();
    
    return meta;
  }

}

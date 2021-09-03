package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class SerialMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(SerialMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   * @param name
   *          n
   * 
   */
  public SerialMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addDescription("reads and writes data to a serial port");
    addCategory("sensors", "control");
    addDependency("io.github.java-native", "jssc", "2.9.2");
    setLicenseGplV3(); // via jssc

  }

}

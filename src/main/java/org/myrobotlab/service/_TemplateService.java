package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class _TemplateService extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(_TemplateService.class);

  public _TemplateService(String n, String id) {
    super(n, id);
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

    ServiceType meta = new ServiceType(_TemplateService.class);
    meta.addDescription("used as a general template");
    meta.setAvailable(true); // false if you do not want it viewable in a gui
    // add dependency if necessary

    // TEMPORARY CORE DEPENDENCIES !!! (for uber-jar)
    // meta.addDependency("orgId", "artifactId", "2.4.0");
    // meta.addDependency("org.bytedeco.javacpp-presets", "artoolkitplus",
    // "2.3.1-1.4");
    // meta.addDependency("org.bytedeco.javacpp-presets",
    // "artoolkitplus-platform", "2.3.1-1.4");

    // meta.addDependency("com.twelvemonkeys.common", "common-lang", "3.1.1");

    meta.setAvailable(false);
    meta.addCategory("general");
    return meta;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("template", "_TemplateService");
      Runtime.start("servo", "Servo");
      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}

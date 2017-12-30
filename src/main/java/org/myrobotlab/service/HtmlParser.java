package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class HtmlParser extends Service {
  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(HtmlParser.class);

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      Runtime.start("jsoup", "Jsoup");
      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public HtmlParser(String n) {
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

    ServiceType meta = new ServiceType(HtmlParser.class.getCanonicalName());
    meta.addDependency("org.jsoup", "1.8.3");
    meta.addDescription("html parser");
    meta.addCategory("document");
    // Set to false since no JSoup service exists
    meta.setAvailable(false);
    return meta;
  }

}

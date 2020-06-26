package org.myrobotlab.service.meta;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class TesseractOcrMeta {
  
  public final static Logger log = LoggerFactory.getLogger(TesseractOcrMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {
    ServiceType meta = new ServiceType("org.myrobotlab.service.TesseractOcr");
    meta.addDescription("Optical character recognition - the ability to read");
    meta.addCategory("ai","vision");
    meta.addDependency("org.bytedeco", "tesseract", "4.1.1-1.5.3");
    meta.addDependency("org.bytedeco", "tesseract-platform", "4.1.1-1.5.3");
    meta.addDependency("tesseract", "tessdata", "0.0.2", "zip");
    return meta;
  }
  
}


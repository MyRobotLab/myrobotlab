package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class TesseractOcrMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(TesseractOcrMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * @param name n
   * 
   */
  public TesseractOcrMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addDescription("Optical character recognition - the ability to read");
    addCategory("ai", "vision");
    addDependency("org.bytedeco", "tesseract", "4.1.1-1.5.5");
    addDependency("org.bytedeco", "tesseract-platform", "4.1.1-1.5.5");
    addDependency("tesseract", "tessdata", "0.0.2", "zip");

  }

}

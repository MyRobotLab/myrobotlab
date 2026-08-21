package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class TesseractOcrMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(TesseractOcrMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public TesseractOcrMeta() {
    String javaCvVersion = "1.5.13";

    String tesseractVersion = "5.5.2-" + javaCvVersion;
    addDescription("Optical character recognition using Tesseract 5");
    addCategory("ai", "vision");
    setAvailable(true);
    setLicenseApache();
    addDependency("org.bytedeco", "tesseract", tesseractVersion);
    addDependency("org.bytedeco", "tesseract-platform", tesseractVersion);
    addDependency("tesseract", "tessdata", "0.0.2", "zip");
    addDependency("org.bytedeco", "openblas", "0.3.31-" + javaCvVersion);

  }

}

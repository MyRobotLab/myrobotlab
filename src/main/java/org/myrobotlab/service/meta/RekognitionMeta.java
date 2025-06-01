package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class RekognitionMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(RekognitionMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public RekognitionMeta() {

    addDescription("Amazon visual recognition cloud service");
    setAvailable(true); // false if you do not want it viewable in a gui
    // add dependency if necessary
    addDependency("com.amazonaws", "aws-java-sdk-rekognition", "1.11.263");
    setCloudService(true);
    setRequiresKeys(true);
    addCategory("vision", "cloud");

  }

}

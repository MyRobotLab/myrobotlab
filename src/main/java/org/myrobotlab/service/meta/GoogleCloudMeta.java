package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class GoogleCloudMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(GoogleCloudMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public GoogleCloudMeta() {
    addDescription("google api client service");
    setAvailable(true);
    addDependency("com.google.cloud", "google-cloud-vision", "1.14.0");
    addCategory("cloud", "vision");
    setCloudService(true);
    setRequiresKeys(true);

  }

}

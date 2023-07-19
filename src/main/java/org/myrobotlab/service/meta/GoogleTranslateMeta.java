package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class GoogleTranslateMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(GoogleTranslateMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public GoogleTranslateMeta() {

    // add a cool description
    addDescription("used as a general template");

    // add dependencies if necessary
    // addDependency("com.twelvemonkeys.common", "common-lang", "3.1.1");

    setAvailable(false);
    setCloudService(true);

    addCategory("translation", "programming");
    
    addDependency("com.google.cloud", "google-cloud-translate", "2.2.0");
    addDependency("com.google.auth", "google-auth-library-credentials", "1.3.0");

  }

} 

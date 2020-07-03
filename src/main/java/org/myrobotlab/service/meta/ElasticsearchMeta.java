package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class ElasticsearchMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(ElasticsearchMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public ElasticsearchMeta() {

    Platform platform = Platform.getLocalInstance();

    addDescription("used as a general template");
    setAvailable(true); // false if you do not want it viewable in a gui
    // add dependency if necessary

    // TEMPORARY CORE DEPENDENCIES !!! (for uber-jar)
    // addDependency("orgId", "artifactId", "2.4.0");
    // addDependency("org.bytedeco.javacpp-presets", "artoolkitplus",
    // "2.3.1-1.4");
    // addDependency("org.bytedeco.javacpp-presets",
    // "artoolkitplus-platform", "2.3.1-1.4");

    // addDependency("com.twelvemonkeys.common", "common-lang", "3.1.1");

    addDependency("pl.allegro.tech", "embedded-elasticsearch", "2.7.0");
    setAvailable(false);
    addCategory("general");

  }

}

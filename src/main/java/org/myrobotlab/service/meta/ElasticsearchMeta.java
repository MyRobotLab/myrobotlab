package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class ElasticsearchMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(ElasticsearchMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public ElasticsearchMeta() {

    addDescription("used as a general template");
    setAvailable(true); // false if you do not want it viewable in a gui
    // add dependency if necessary

    // TEMPORARY CORE DEPENDENCIES !!! (for uber-jar)
    // addDependency("orgId", "artifactId", "2.4.0");
    // addDependency("org.bytedeco.javacpp-presets", "artoolkitplus",
    // "2.3.1-1.4");
    // addDependency("org.bytedeco.javacpp-presets",
    // "artoolkitplus-platform", "2.3.1-1.4");

    // addDependency("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", "2.13.3");

    addDependency("pl.allegro.tech", "embedded-elasticsearch", "2.7.0");
    // exclude("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml");




    setAvailable(false);
    addCategory("general");

  }

}

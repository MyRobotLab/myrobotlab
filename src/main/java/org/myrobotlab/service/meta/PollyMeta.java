package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Plan;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.PollyConfig;
import org.myrobotlab.service.meta.abstracts.AbstractSpeechSynthesisMeta;
import org.slf4j.Logger;

public class PollyMeta extends AbstractSpeechSynthesisMeta {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(PollyMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public PollyMeta() {
    addDescription("Amazon speech synthesis - requires keys");
    setAvailable(true); // false if you do not want it viewable in a
    // gui
    // add dependency if necessary
    addCategory("speech", "sound");

    addDependency("com.fasterxml.jackson.core", "jackson-core", "2.14.0");
    addDependency("com.fasterxml.jackson.core", "jackson-databind", "2.14.0");
    addDependency("com.fasterxml.jackson.core", "jackson-annotations", "2.14.0");

    addDependency("com.amazonaws", "aws-java-sdk-polly", "1.12.253");

    exclude("com.fasterxml.jackson.core", "jackson-core");
    exclude("com.fasterxml.jackson.core", "jackson-databind");
    exclude("com.fasterxml.jackson.core", "jackson-annotations");

    exclude("org.apache.httpcomponents", "httpcore");
    exclude("org.apache.httpcomponents", "httpclient");

    addDependency("org.apache.commons", "commons-lang3", "3.3.2");
    // force using Runtimes httpclient version exclude here

    addCategory("speech", "cloud");
    setCloudService(true);
    setRequiresKeys(true);

  }

}

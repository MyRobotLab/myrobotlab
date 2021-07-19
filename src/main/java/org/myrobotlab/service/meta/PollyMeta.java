package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.AbstractSpeechSynthesisMeta;
import org.slf4j.Logger;

public class PollyMeta extends AbstractSpeechSynthesisMeta {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(PollyMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * @param name n
   * 
   */
  public PollyMeta(String name) {

    super(name);
    addDescription("Amazon speech synthesis - requires keys");
    setAvailable(true); // false if you do not want it viewable in a
    // gui
    // add dependency if necessary
    addCategory("speech", "sound");

    addDependency("com.fasterxml.jackson.core", "jackson-core", "2.10.1");
    addDependency("com.fasterxml.jackson.core", "jackson-databind", "2.10.5.1");
    addDependency("com.fasterxml.jackson.core", "jackson-annotations", "2.10.1");
    addDependency("com.amazonaws", "aws-java-sdk-polly", "1.11.512");
    addDependency("org.apache.commons", "commons-lang3", "3.3.2");
    // force using httpClient service httpcomponents version
    exclude("org.apache.httpcomponents", "httpcore");
    exclude("org.apache.httpcomponents", "httpclient");

    // <!-- https://mvnrepository.com/artifact/com.amazonaws/aws-java-sdk-polly
    // -->
    // <dependency org="com.amazonaws" name="aws-java-sdk-polly"
    // rev="1.11.118"/>

    addCategory("speech", "cloud");
    setCloudService(true);
    setRequiresKeys(true);

  }

}

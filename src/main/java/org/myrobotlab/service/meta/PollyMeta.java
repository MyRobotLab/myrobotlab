package org.myrobotlab.service.meta;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.slf4j.Logger;

public class PollyMeta {
  public final static Logger log = LoggerFactory.getLogger(PollyMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = AbstractSpeechSynthesis.getMetaData("org.myrobotlab.service.Polly");

    meta.addDescription("Amazon speech synthesis - requires keys");
    meta.setAvailable(true); // false if you do not want it viewable in a
    // gui
    // add dependency if necessary
    meta.addCategory("speech", "sound");

    meta.addDependency("com.fasterxml.jackson.core", "jackson-core", "2.9.9");
    meta.addDependency("com.fasterxml.jackson.core", "jackson-databind", "2.9.10.3");
    meta.addDependency("com.fasterxml.jackson.core", "jackson-annotations", "2.9.9");
    meta.addDependency("com.amazonaws", "aws-java-sdk-polly", "1.11.512");
    meta.addDependency("org.apache.commons", "commons-lang3", "3.3.2");
    // force using httpClient service httpcomponents version
    meta.exclude("org.apache.httpcomponents", "httpcore");
    meta.exclude("org.apache.httpcomponents", "httpclient");
    meta.addPeer("httpClient", "HttpClient", "httpClient");

    // <!-- https://mvnrepository.com/artifact/com.amazonaws/aws-java-sdk-polly
    // -->
    // <dependency org="com.amazonaws" name="aws-java-sdk-polly"
    // rev="1.11.118"/>

    meta.addCategory("speech","cloud");
    meta.setCloudService(true);
    meta.setRequiresKeys(true);
    return meta;
  }

  
}


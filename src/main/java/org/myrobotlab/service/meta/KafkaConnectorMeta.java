package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class KafkaConnectorMeta {
  public final static Logger log = LoggerFactory.getLogger(KafkaConnectorMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.KafkaConnector");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("Provides a string/string consumer for a kafka topic.");
    meta.addCategory("cloud");
    meta.addDependency("org.apache.kafka", "kafka-clients", "1.0.1");
    return meta;
  }
  
}


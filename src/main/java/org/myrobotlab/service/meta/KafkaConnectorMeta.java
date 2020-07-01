package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class KafkaConnectorMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(KafkaConnectorMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return MetaData - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.KafkaConnector");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("Provides a string/string consumer for a kafka topic.");
    meta.addCategory("cloud");
    meta.addDependency("org.apache.kafka", "kafka-clients", "1.0.1");
    return meta;
  }
  
}


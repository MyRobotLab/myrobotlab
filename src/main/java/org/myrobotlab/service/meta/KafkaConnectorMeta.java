package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class KafkaConnectorMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(KafkaConnectorMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public KafkaConnectorMeta() {
    addDescription("Provides a string/string consumer for a kafka topic.");
    addCategory("cloud");
    addDependency("org.apache.kafka", "kafka-clients", "1.0.1");
  }

}

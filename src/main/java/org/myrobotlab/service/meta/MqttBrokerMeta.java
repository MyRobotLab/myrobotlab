package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class MqttBrokerMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(MqttBrokerMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public MqttBrokerMeta() {

    addDescription(
        "This is an Mqtt client based on the Paho Mqtt client library. Mqtt is a machine-to-machine (M2M)/'Internet of Things' connectivity protocol. See http://mqtt.org");
    addCategory("cloud", "network");

    addDependency("com.fasterxml.jackson.core", "jackson-core", "2.13.3");
    addDependency("com.fasterxml.jackson.core", "jackson-databind", "2.13.3");
    addDependency("com.fasterxml.jackson.core", "jackson-annotations", "2.13.3");

    addDependency("io.moquette", "moquette-broker", "0.15");

    exclude("com.fasterxml.jackson.core", "jackson-core");
    exclude("com.fasterxml.jackson.core", "jackson-databind");
    exclude("com.fasterxml.jackson.core", "jackson-annotations");

    exclude("org.slf4j", "slf4j-log4j12");

    setCloudService(true);

  }

}

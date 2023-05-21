package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class MqttMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(MqttMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public MqttMeta() {
    addDescription(
        "This is an Mqtt client based on the Paho Mqtt client library. Mqtt is a machine-to-machine (M2M)/'Internet of Things' connectivity protocol. See http://mqtt.org");
    addCategory("cloud", "network");
    /*
     * <!--
     * https://mvnrepository.com/artifact/org.eclipse.paho/org.eclipse.paho.
     * client. mqttv3 --> <dependency org="org.eclipse.paho"
     * name="org.eclipse.paho.client.mqttv3" rev="1.2.0"/>
     */
    addDependency("org.eclipse.paho", "org.eclipse.paho.client.mqttv3", "1.2.1");
    setCloudService(false);

  }

}

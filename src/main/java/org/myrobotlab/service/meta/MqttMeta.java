package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class MqttMeta {
  public final static Logger log = LoggerFactory.getLogger(MqttMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.Mqtt");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription(
        "This is an Mqtt client based on the Paho Mqtt client library. Mqtt is a machine-to-machine (M2M)/'Internet of Things' connectivity protocol. See http://mqtt.org");
    meta.addCategory("cloud","network");
    /*
     * <!--
     * https://mvnrepository.com/artifact/org.eclipse.paho/org.eclipse.paho.
     * client. mqttv3 --> <dependency org="org.eclipse.paho"
     * name="org.eclipse.paho.client.mqttv3" rev="1.2.0"/>
     */
    meta.addDependency("org.eclipse.paho", "org.eclipse.paho.client.mqttv3", "1.2.1");
    meta.setCloudService(true);
    return meta;
  }
  
}


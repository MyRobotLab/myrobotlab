package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.myrobotlab.framework.Service;

public class MqttBrokerTest extends AbstractServiceTest {

  @Override
  public Service createService() {
    MqttBroker broker = (MqttBroker) Runtime.start("broker", "MqttBroker");
    return broker;
  }

  @Override
  public void testService() throws Exception {

    Python python = (Python) Runtime.start("python", "Python");
    Clock c = (Clock) Runtime.start("clockTest", "Clock");
    // insert python callback test which sets test_value from mqtt client
    python.exec("test_value = None\ndef test(msg):\n\tglobal test_value\n\ttest_value = msg\n\tprint(msg)");

    MqttBroker broker = (MqttBroker) service;
    broker.listen();

    Mqtt mqtt = (Mqtt) Runtime.start("mqtt", "Mqtt");

    assertTrue(!mqtt.isConnected());
    mqtt.connect("mqtt://localhost:1883");
    assertTrue(mqtt.isConnected());

    // fire mqtt msg to execute python test and set value of test_value
    mqtt.publish("api/service/python/exec/\"test(\\\"worky!\\\")\"");

    // wait for script to end
    long start = System.currentTimeMillis();
    // python.waitFor("python", "finishedExecutingScript", 10000);
    Service.sleep(1000); // lame
    log.info("delta time for mqtt localhost execution {} ms", System.currentTimeMillis() - start);
    String test_value = python.get("test_value").toString();
    assertEquals("worky!", test_value);

    // start it
    mqtt.publish("api/service/clockTest/startClock");
    // add a subscription
    // mqtt.subscribe("api/service/clockTest/pulse");
    Service.sleep(1000); // lame
    // make sure its running
    assertTrue(c.isClockRunning());

    log.info("here");

  }
}

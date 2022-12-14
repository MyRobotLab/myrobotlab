package org.myrobotlab.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Adafruit16CServoDriver;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.InMoov2;
import org.myrobotlab.service.LocalSpeech;
import org.myrobotlab.service.Pid;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Serial;
import org.myrobotlab.service.Tracking;
import org.myrobotlab.service.config.ArduinoConfig;
import org.myrobotlab.service.config.WebGuiConfig;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class ServiceLifeCycleTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(ServiceLifeCycleTest.class);

  @Test
  public void serviceLifeCycleTest() throws Exception {

    // clear plan
    Runtime.clearPlan();

    // load a simple plan
    Runtime.load("c1", "Clock");
    Plan plan = Runtime.getPlan();
    // 1 clock and 1 runtime
    assertEquals(2, plan.size());

    // clear plan
    Runtime.clearPlan();

    plan = Runtime.getPlan();
    assertEquals(1, plan.size());

    plan = Runtime.load("controller", "Arduino");
    ArduinoConfig ac = (ArduinoConfig) plan.get("controller");
    assertFalse(ac.connect);
    // 1 arduino 1 serial
    assertEquals(3, plan.size());
    assertEquals(ac, plan.get("controller"));

    Arduino arduino = (Arduino) Runtime.start("controller", "Arduino");
    assertNotNull(arduino);
    arduino = null;
    Serial serial = (Serial) Runtime.getService("controller.serial");
    assertNotNull(serial);
    serial = null;
    Runtime.release("controller");

    assertNull(Runtime.getService("controller"));
    assertNull(Runtime.getService("controller.serial"));

    Runtime.clearPlan();

    /**
     * use case - load a default config - modify it substantially then start the
     * service, with worky peers
     */

    // load the default track config
    plan = Runtime.load("track", "Tracking");
    assertEquals(8, plan.size());

    // remove the default controller
    assertNotNull(plan.remove("track.controller"));

    // add an adafruit controller
    Runtime.load("track.controller", "Adafruit16CServoDriver");

    Tracking track = (Tracking) Runtime.start("track", "Tracking"); // FIXME -
                                                                    // you can't
                                                                    // run
    // Runtime.start() ...
    // CAN YOU BAN IT ?
    assertNotNull(track);
    // better get an adafruit back
    Adafruit16CServoDriver ada = (Adafruit16CServoDriver) Runtime.getService("track.controller");
    assertNotNull(ada);

    // track.releaseService();
    Runtime.release("track");
    assertNull(Runtime.getService("track.controller"));

    Runtime.clearPlan();
    plan = Runtime.load("i02", "InMoov2");

    log.info("plan has {} services", plan.size());
    MetaData md = MetaData.get("InMoov2");
    // assertTrue(md.getPeers().size() < plan.size());

    List<ServiceInterface> sis = Runtime.getServices();
    assertTrue(sis.size() < plan.size());

    InMoov2 i02 = (InMoov2) Runtime.start("i02", "InMoov2");
    assertNotNull(i02);
    assertNull(Runtime.getService("i02.headTracking"));
    i02.startPeer("left");
    i02.startPeer("headTracking");
    assertNotNull(Runtime.getService("i02.headTracking"));

    Pid pid = (Pid) Runtime.getService("i02.pid");
    assertNotNull(pid);

    i02.startPeer("eyeTracking");
    assertEquals("i02.eyeTracking", i02.getPeerName("eyeTracking"));

    Runtime.load("webgui", "WebGui");
    WebGuiConfig webgui = (WebGuiConfig) plan.get("webgui");
    webgui.autoStartBrowser = false;
    // start it up
    Runtime.startConfig("webgui");

    Tracking eye = (Tracking) i02.getPeer("eyeTracking");
    ServoControl tilt = (ServoControl) eye.getPeer("tilt");
    assertEquals("i02.head.eyeY", tilt.getName());

    i02.setSpeechType("LocalSpeech");
    i02.startPeer("mouth");

    // better be local speech
    LocalSpeech mouth = (LocalSpeech) i02.getPeer("mouth");
    assertNotNull(mouth);

    // FIXME i02.releasePeers()

    log.info("done");

  }

}
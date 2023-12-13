package org.myrobotlab.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Clock;
import org.myrobotlab.service.InMoov2Head;
import org.myrobotlab.service.LocalSpeech;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.config.ClockConfig;
import org.myrobotlab.service.config.InMoov2Config;
import org.myrobotlab.service.config.LocalSpeechConfig;
import org.myrobotlab.service.config.MarySpeechConfig;
import org.myrobotlab.service.config.OpenCVConfig;
import org.myrobotlab.service.config.PollyConfig;
import org.myrobotlab.service.config.ServiceConfig.Listener;
import org.myrobotlab.service.config.ServoConfig;
import org.myrobotlab.service.config.TrackingConfig;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class ConfigTest extends AbstractTest {
  
  
  @BeforeClass
  public static void setUpBeforeClass() {
      System.out.println("Runs before any test method in the class");
  }

  @AfterClass
  public static void tearDownAfterClass() {
      System.out.println("Runs after all test methods in the class");
  }

  @Before /* before each test */
  public void setUp() throws IOException {
    // remove all services - also resets config name to DEFAULT effectively
    Runtime.releaseAll(true, true);
      // clean our config directory
    Runtime.removeConfig(CONFIG_NAME);
    // set our config
    Runtime.setConfig(CONFIG_NAME);
  }

  @After
  public void tearDown() {
      System.out.println("Runs after each test method");
  }
  

  // --- config set related ---
  // setConfigPath(fullpath)
  // setConfig(name)
  // startConfig(name)
  // saveConfig(name)
  // releaseConfig(name)
  // clearConfig()

  // --- runtime ----
  // load(name, type)
  // getPlan()
  // savePlan(name)

  // save()
  // load() load it into plan / doesn't apply it
  // export() - deprecated
  // apply() loads it into plan and applies it

  public final static Logger log = LoggerFactory.getLogger(ConfigTest.class);

  final String CONFIG_NAME = "junit-test-01";

  final String CONFIG_PATH = "data" + File.separator + "config" + File.separator + CONFIG_NAME;


  @Test
  public void testStartNoConfig() throws Exception {

    Runtime runtime = Runtime.getInstance();
    assertNotNull(runtime);
    
    // complete teardown, release runtime, block
    Runtime.releaseAll(true, true);
    
    String[] names = Runtime.getServiceNames();
    assertEquals("complete teardown should be 0", 0, names.length);
    
    // nothing to start - should be empty config
    Runtime.startConfig(CONFIG_NAME);
    
    // starting an empty config automatically needs a runtime, and runtime
    // by default starts the singleton security service
    names = Runtime.getServiceNames();
    assertEquals("complete teardown should be 2 after trying to start a config runtime and security", 2, names.length);
   
  }
  
  @Test
  public void testSwitchingPeer() throws IOException {
    
    Runtime runtime = Runtime.getInstance();
    assertNotNull(runtime);

    // loading a composite service should save default config
    // to the current config directory
    Plan plan = Runtime.load("eyeTracking", "Tracking");
    assertNotNull(plan);
    
    // load eyeTracking.yml config - verify default state
    TrackingConfig eyeTracking = (TrackingConfig)runtime.getConfig(CONFIG_NAME, "eyeTracking");
    TrackingConfig defaultTracking = new TrackingConfig();
    assertEquals("eyeTracking.yml values should be the same as default", defaultTracking.enabled, eyeTracking.enabled);
    assertEquals("eyeTracking.yml type should be the same as default", defaultTracking.type, eyeTracking.type);

    eyeTracking = (TrackingConfig)runtime.getConfig("eyeTracking");
    assertEquals("eyeTracking.yml values should be the same as default", defaultTracking.enabled, eyeTracking.enabled);
    assertEquals("eyeTracking.yml type should be the same as default", defaultTracking.type, eyeTracking.type);
    
    // load single opencv
    OpenCVConfig cv = (OpenCVConfig)Runtime.load("cv", "OpenCV").get("cv");
    // default capturing is false
    assertFalse(cv.capturing);

    // save as true
    cv.capturing = true;
    Runtime.saveConfig("cv", cv);
    
    Runtime.load("pid", "Pid");
    eyeTracking = (TrackingConfig)runtime.getConfig("eyeTracking");
    
    eyeTracking.getPeer("cv").name = "cv";
    Runtime.saveConfig("eyeTracking", eyeTracking);
    
    // verify the peer was updated to cv
    eyeTracking = (TrackingConfig)runtime.getConfig("eyeTracking");
    cv = (OpenCVConfig)runtime.getPeerConfig("eyeTracking","cv");
    // from previous save
    assertTrue(cv.capturing);

  }
  
  @Test
  public void testChangeType() throws IOException {
    Runtime runtime = Runtime.getInstance();    
    Runtime.load("mouth", "MarySpeech");
    MarySpeechConfig mouth = (MarySpeechConfig)runtime.getConfig("mouth");
    mouth.listeners = new ArrayList<Listener>();    
    mouth.listeners.add(new Listener("publishStartSpeaking", "fakeListener"));
    Runtime.saveConfig("mouth", mouth);
    MarySpeechConfig mary = (MarySpeechConfig)runtime.getConfig("mouth");
    assertNotNull(mary);
    assertEquals(1, mary.listeners.size());
    // save it
    runtime.changeType("mouth", "LocalSpeech");
    LocalSpeechConfig local = (LocalSpeechConfig)runtime.getConfig("mouth");
    assertEquals("must have the listener", 1, local.listeners.size());
    assertTrue(local.listeners.get(0).listener.equals("fakeListener"));
  }

  @Test
  public void testInitialLoad() {
    Runtime runtime = Runtime.getInstance();
    Runtime.load("service", "Clock");
    ClockConfig clock = (ClockConfig)runtime.getConfig("service");
    assertNotNull(clock);
    // replace load
    Runtime.load("service", "Tracking");
    TrackingConfig tracking = (TrackingConfig)runtime.getConfig("service");
    assertNotNull(tracking);
  }
  
  @Test
  public void testChangePeerName() throws IOException {
    Runtime runtime = Runtime.getInstance();
    Plan plan = Runtime.load("pollyMouth", "Polly");
    PollyConfig polly = (PollyConfig)plan.get("pollyMouth");    
    Runtime.load("i01", "InMoov2");
    InMoov2Config i01 = (InMoov2Config)runtime.getConfig("i01");
    // default
    MarySpeechConfig mary = (MarySpeechConfig)runtime.getPeer("i01", "mouth");
    assertNotNull(mary);
    polly.listeners = mary.listeners;
    Runtime.saveConfig("pollyMouth", polly);
    Peer peer = i01.getPeers().get("mouth");
    peer.name = "pollyMouth";
    Runtime.saveConfig("i01", i01);
    // switch to pollyMouth
    PollyConfig p = (PollyConfig)runtime.getPeer("i01", "mouth");
    
    // FIXME - was going to test moving of subscriptions, however, unfortunately
    // SpeechSynthesis services use a "recognizers" data instead of just simple subscriptions
    // This should be fixed in the future to use standard subscriptions
    
  }  
  
  @Test
  public void testSimpleServiceStart() {
    Clock clock = (Clock)Runtime.start("track", "Clock");
    clock.startClock();
    clock.releaseService();
    // better be a tracking service
    LocalSpeech track = (LocalSpeech)Runtime.start("track", "LocalSpeech");
    assertNotNull(track);
    track.releaseService();
    // better be a clock
    clock = (Clock)Runtime.create("track", "Clock");
    log.info("start");
  }

  @Test
  public void testPeers() {
    InMoov2Head head = (InMoov2Head)Runtime.start("track", "InMoov2Head");
    Servo neck = (Servo)Runtime.getService("track.neck");
    assertNotNull(neck);
    head.releaseService();
    assertNull(Runtime.getService("track.neck"));
    
  }
  
  @Test
  public void testSaveApply() throws IOException {
    Runtime runtime = Runtime.getInstance();
    Servo neck = (Servo)Runtime.start("neck", "Servo");
    ServoConfig config = neck.getConfig();
    
    // Where config is "different" than member variables it
    // takes an apply(config) of the config to make the service
    // update its member variables, vs changing config and
    // immediately getting the service behavior change.     
    config.idleTimeout = 5000;
    // the fact this takes and additional method to process
    // i think is legacy and should be changed for Servo to use
    // its config "directly"
    neck.apply(config);
    neck.save();
    neck.releaseService();
    neck = (Servo)Runtime.start("neck", "Servo");
    assertTrue("preserved value", 5000  == neck.getConfig().idleTimeout);

    Servo servo = (Servo)Runtime.start("servo", "Servo");
    config = (ServoConfig)Runtime.load("default", "Servo").get("default");
    assertNull(config.idleTimeout);
    config.idleTimeout = 7000;
    Runtime.saveConfig("servo", config);
    servo.apply();
    assertTrue(servo.getConfig().idleTimeout == 7000);
    
    config.idleTimeout = 8000;
    servo.apply(config);
    assertTrue(servo.getIdleTimeout() == 8000);
    servo.apply();
    assertTrue("filesystem servo.yml applied", servo.getIdleTimeout() == 7000);
    
  }
  

}
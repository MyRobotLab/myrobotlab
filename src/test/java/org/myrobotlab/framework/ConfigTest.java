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
import java.util.Comparator;

import org.junit.Test;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Clock;
import org.myrobotlab.service.Pid.PidData;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Tracking;
import org.myrobotlab.service.WebGui;
import org.myrobotlab.service.config.ClockConfig;
import org.myrobotlab.service.config.PidConfig;
import org.myrobotlab.service.config.ServoConfig;
import org.myrobotlab.service.config.TrackingConfig;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class ConfigTest extends AbstractTest {

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

  public void removeConfigData() throws IOException {
    File check = new File("data" + File.separator + "config" + File.separator + CONFIG_NAME);

    if (check.exists()) {
      Path pathToBeDeleted = Paths.get(check.getAbsolutePath());
      Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }

  }

  @Test
  public void configTest() throws Exception {

    Runtime runtime = Runtime.getInstance();
    Plan plan = null;

    Runtime.clearPlan();
    removeConfigData();
    Runtime.setConfig(CONFIG_NAME);

    // load default config TODO test Runtime.useDefaults(false) also
    Runtime.load("eyeTracking", "Tracking");
    plan = Runtime.load("headTracking", "Tracking");
    Runtime.load("cv", "OpenCV");

    TrackingConfig eyeTracking = (TrackingConfig) plan.get("eyeTracking");
    TrackingConfig headTracking = (TrackingConfig) plan.get("headTracking");

    Runtime.load("pid", "Pid");
    PidConfig pid = (PidConfig) plan.get("pid");
    
    // TODO - check defaults
//    eyeTrackingc.getPeer("cv").autoStart = false;
//    headTrackingc.getPeer("cv").autoStart = false;
  
    // map both names to single cv
    eyeTracking.getPeer("cv").name = "cv";
    headTracking.getPeer("cv").name = "cv";

    eyeTracking.getPeer("pid").name = "pid";
    headTracking.getPeer("pid").name = "pid";

    eyeTracking.getPeer("controller").name = "left";
    headTracking.getPeer("controller").name = "left";

    eyeTracking.getPeer("pan").name = "eyeX";
    headTracking.getPeer("pan").name = "rothead";
    
    eyeTracking.getPeer("tilt").name = "eyeY";
    headTracking.getPeer("tilt").name = "neck";
    
    Runtime.load("eyeY", "Servo");
    Runtime.load("eyeX", "Servo");
    Runtime.load("neck", "Servo");
    Runtime.load("rothead", "Servo");
    ServoConfig eyeY = (ServoConfig)plan.get("eyeY");
    ServoConfig eyeX = (ServoConfig)plan.get("eyeX");
    ServoConfig neck = (ServoConfig)plan.get("neck");
    ServoConfig rothead = (ServoConfig)plan.get("rothead");
    eyeY.pin = "24";
    eyeX.pin = "22";
    neck.pin = "12";
    rothead.pin = "13";

    eyeY.autoDisable = true;
    eyeX.autoDisable = true;
    neck.autoDisable = true;
    rothead.autoDisable = true;

    eyeX.minIn = 60.0;
    eyeY.minIn = 60.0;
    neck.minIn = 20.0;
    rothead.minIn = 20.0;

    eyeX.maxIn = 120.0;
    eyeY.maxIn = 120.0;
    neck.maxIn = 160.0;
    rothead.maxIn = 160.0;
    
    eyeX.minOut = 60.0;
    eyeY.minOut = 60.0;
    neck.minOut = 20.0;
    rothead.minOut = 20.0;

    eyeX.maxOut = 120.0;
    eyeY.maxOut = 120.0;
    neck.maxOut = 160.0;
    rothead.maxOut = 160.0;

    
    eyeY.controller = "left";
    eyeX.controller = "left";
    neck.controller = "left";
    rothead.controller = "left";
    
    PidData pidData = new PidData();
    pidData.ki = 0.001;
    pidData.kp = 30.0;
    pid.data.put("eyeX", pidData);

    pidData = new PidData();
    pidData.ki = 0.001;
    pidData.kp = 30.0;
    pid.data.put("eyeY", pidData);

    // test Runtime.useDefaults(true/false)
    
    
    // prune the children we don't want
    plan.remove("eyeTracking.cv");
    plan.remove("headTracking.cv");

    plan.remove("eyeTracking.pan");
    plan.remove("headTracking.pan");

    plan.remove("eyeTracking.tilt");
    plan.remove("headTracking.tilt");
    
    plan.remove("eyeTracking.pid");
    plan.remove("headTracking.pid");
    
    plan.remove("eyeTracking.controller");
    plan.remove("headTracking.controller");

    plan.remove("eyeTracking.controller.serial");
    plan.remove("headTracking.controller.serial");
    
    // FIXME - THANKFULLY THIS DID NOT WORK AT ALL
    // ServiceReservation sr = plan.getPeers().get("eyeTracking").get("cv");
    // sr.actualName = "cv";
    //
    // sr = plan.getPeers().get("headTracking").get("cv");
    // sr.actualName = "cv";

    // set a communal opencv source
//    eyeTrackingc.cv = "cv";
//    headTrackingc.cv = "cv";

    // assertEquals("2x tracking with merged opencv expecting 13 (7 + 7 - 2 + 1) services", 14, plan.size());
    // save the plan
    Runtime.savePlan(CONFIG_NAME);
    // clear the in memory plan
    Runtime.clearPlan();

    Runtime.setAllVirtual(false);
    
    // start the plan
    Runtime.startConfig(CONFIG_NAME);

    assertNotNull(Runtime.getService("cv"));
    assertNotNull(Runtime.getService("eyeTracking"));
    assertNotNull(Runtime.getService("headTracking"));

    WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
    webgui.autoStartBrowser(false);
    webgui.startService();
    
    assertNull(Runtime.getService("eyeTracking.cv"));
    assertNull(Runtime.getService("headTracking.cv"));

    Runtime.releaseConfig(CONFIG_NAME);
    assertNull(Runtime.getService("cv"));
    assertNull(Runtime.getService("eyeTracking"));
    assertNull(Runtime.getService("headTracking"));

    // given unknown plan and config
    // when i clear the plan
    // its clear with no config or peers

    // clear plan
    Runtime.clearPlan();
    removeConfigData();

    plan = Runtime.getPlan();
    assertEquals("cleared plan should be 0", 1, plan.size());
    // assertEquals("cleared plan peers be 0", 0, plan.getPeers().size());

    // simple single plan
    // load is attempt to load from file - if not available load from memory
    Runtime.clearPlan();
    Runtime.load("c1", "Clock");
    plan = Runtime.getPlan();
    ClockConfig clock = (ClockConfig) plan.get("c1");
    clock.interval = 3555;
    assertNotNull(clock);
    assertEquals(2, plan.size());

    // FIXME - use case test individual services being saved - test setting
    // configPath to non default

    // given using static runtime method saveConfig
    // when called with a valid config name
    // then creates /data/config/{configName}
    Runtime.setConfig(CONFIG_NAME);
    Clock c2 = (Clock) Runtime.start("c2", "Clock");
    c2.setInterval(5000);
    c2.save();
    
    File check = new File( runtime.getConfigPath() + File.separator + "c2.yml");
    assertTrue(check.exists());
    ClockConfig clockConfig = CodecUtils.fromYaml(FileIO.toString(runtime.getConfigPath() + File.separator + "c2.yml"), ClockConfig.class);
    assertTrue(clockConfig.interval == 5000);

    // given the system is new and no previous definition exists
    // when i load a default service config
    // then a valid plan will exist

    Runtime.savePlan(CONFIG_NAME);
    check = new File(CONFIG_PATH + File.separator + "c1.yml");
    assertTrue(check.exists());
    clockConfig = CodecUtils.fromYaml(FileIO.toString(CONFIG_PATH + File.separator + "c1.yml"), ClockConfig.class);
    assertTrue(clockConfig.interval == 3555);

    Clock c1 = (Clock) Runtime.start("c1");
    assertTrue(c1.getInterval() == 3555);
    c1.setInterval(3333);
    c1.save();

    clockConfig = CodecUtils.fromYaml(FileIO.toString(CONFIG_PATH + File.separator + "c1.yml"), ClockConfig.class);
    assertTrue(clockConfig.interval == 3333);

    c1.setInterval(4444);
    assertTrue(c1.getInterval() == 4444);

    c1.apply();
    assertTrue(c1.getInterval() == 3333);

    clockConfig = (ClockConfig) Runtime.getPlan().get("c1");
    assertTrue(clockConfig.interval == 3333);

    // FIXME - use case - when a parent is not created, but some of the children
    // are !

    // FIXME - use case Runtime.start('myservice')

    // use case - start 2 services with overlapping peers - remove one
    // .. the shared services
    // should last until "both" services are released

    // given i have a composite service
    // when i load it
    // then all services will configured by its default will be loaded
    Runtime.clearPlan();
    plan = Runtime.load("track", "Tracking");
    TrackingConfig track = (TrackingConfig) plan.get("track");
    assertNotNull(track);
    assertEquals("tracking is 1 service and currently has 6 subservices", 8, plan.size());

    // Create creates it does not start - there is no starting of sub systems if
    // you are creating !
    Tracking tracking = (Tracking) Runtime.create("track", "Tracking");
    assertNotNull(tracking);
    tracking.startService();

    Runtime.start("track", "Tracking");

    // FIXME !!! - check to make sure a composite config like Tracking
    // can point a peer to a different actual name e.g. opencv vs
    // tracking.opencv

    // FIXME - release and verify release of plan

    ServoConfig track_pan = (ServoConfig) plan.get("track.pan");
    assertNotNull(track_pan);

    if (!Runtime.saveConfig(null)) {
      // good should not be able to save to null
      // TODO : round robin buffer and index into
      Status error = runtime.getLastError();
      log.info(error.toString());
    }

    assertFalse(Runtime.saveConfig(null));

    // given i have a valid plan
    // then i save the plan
    // the files exist
    Runtime.savePlan(CONFIG_NAME);

    check = new File(CONFIG_PATH);

    assertTrue(check.exists());
    assertTrue(check.isDirectory());

    check = new File(Runtime.getInstance().getConfigPath() + File.separator + "track.yml");
    assertTrue(check.exists());
    check = new File(Runtime.getInstance().getConfigPath() + File.separator + "track.cv.yml");
    assertTrue(check.exists());

    // TODO - check file details
    // for (File f : configFiles) {
    //
    // }

    // TODO -verify that config yml files do not affect a Runtime.start()

    // remove the config data
    removeConfigData();

    // given a valid plan
    // when Runtime.start()
    // the defined services all start - FIXME - they better not be pulling in
    // filesystem data !

    Runtime.saveConfig(CONFIG_NAME);

    check = new File("data" + File.separator + "config" + File.separator + CONFIG_NAME);

    assertTrue(check.exists());
    assertTrue(check.isDirectory());

    // given a Clock config exists and matches c1.yml
    // when I load c1
    // then the c1.yml gets loaded into the plan

    // given i have a config file called c1 on the file system "THAT IS" a
    // LocalSpeech !
    // when I Runtime.start( c1, Clock)
    // a clock is loaded

    // given i have a valid plan
    // when there is no yml file data
    // then i can start a service from the plan

    // load a simple plan - no config set specified - name and type supplied
    // load default embedded Clock meta data from code to memory

    // validate default config in plan
    // for (String s : plan.keySet()) {
    // assertEquals("c1", s);
    // ClockConfig cc = (ClockConfig) plan.get("c1");
    // assertNotNull(cc);
    // assertEquals("Clock", cc.type);
    // }

    // TODO - saveAll vs runtime.save()

    // Runtime.start("webgui", "WebGui");

    // FIXME implement & test these
    // Runtime.loadConfigPath(CONFIG_NAME);
    // Runtime.start();

  }

}
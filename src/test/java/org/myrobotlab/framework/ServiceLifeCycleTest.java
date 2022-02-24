package org.myrobotlab.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.TestCatcher;
import org.myrobotlab.service.TestThrower;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class ServiceLifeCycleTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(ServiceLifeCycleTest.class);

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    // make sure the testing services do not exist
    Runtime.release("catcher01");
    Runtime.release("catcher02");
    Runtime.release("thower01");
    Runtime.release("thower02");
  }

  @AfterClass
  public static void setUpAfterClass() throws Exception {
    // make sure the testing services do not exist
    Runtime.release("catcher01");
    Runtime.release("catcher02");
    Runtime.release("thower01");
    Runtime.release("thower02");
  }

  @Test
  public void serviceLifeCycleTest() throws Exception {

    // push the meta data into the planStore - this will persist
    // overrides if the meta data definition has them
    ServiceData.setMetaData("i01", "InMoov2");

    // pull it back out
    MetaData data = ServiceData.getMetaData("i01", "InMoov2");
    log.info("static meta data {}", data);
    
    // assertEquals("i01.head", data.getPeer("head").actualName);

    Plan plan = ServiceData.getPlan("i01", "InMoov2");
    log.info("static plan {}", plan);
    assertNotNull(plan);

    ServiceInterface si = null;

    // static template info
    MetaData metaData = ServiceData.getMetaData("TestCatcher");
    log.info("static meta data {}", metaData);

    // instance info
    metaData = ServiceData.getMetaData("catcher01", "TestCatcher");
    log.info("instance meta data (with servie name) {}", metaData);

    // show plan
    plan = Runtime.getPlan("catcher01", "TestCatcher");
    log.info("the plan {}", plan);

    // verify subkey
//    ServiceReservation sr = metaData.getPeer("subpeer");
//    assertNotNull(sr);
//    assertEquals("subpeer", sr.key);
//    assertEquals("TestThrower", sr.type);
//    assertEquals("catcher01.subpeer", sr.actualName);

    // change plan type - plan changes must occur BEFORE - services are created
    // !!!!
    Runtime.setPeer("catcher01.subpeer", "catcher01.subpeer", "Servo");
    metaData = ServiceData.getMetaData("catcher01", "TestCatcher");
    log.info("current plan {}", metaData);

    TestCatcher catcher01 = (TestCatcher) Runtime.start("catcher01", "TestCatcher");
    assertNotNull(catcher01);

    ServiceInterface globalPeer = null;
    ServiceInterface subpeer = null;

    // test global peer is global
    globalPeer = catcher01.startPeer("globalPeer");
    assertNotNull(globalPeer);
    assertEquals("thrower01", globalPeer.getName());

    // verify
    subpeer = catcher01.startPeer("subpeer");
    assertEquals("Servo", subpeer.getSimpleName());

    ServiceInterface get = catcher01.getPeer("subpeer");
    assertEquals(subpeer, get);

    // release
    catcher01.releasePeers();

    // verify
//    si = Runtime.getService("catcher01.subpeer");
//    assertNull(si);

    // clear
    ServiceData.clearOverrides();

    // should be back to default
//    sr = ServiceData.getMetaData("catcher01", "TestCatcher").getPeer("subpeer");
//    assertEquals("subpeer", sr.key);
//    assertEquals("catcher01.subpeer", sr.actualName);
//    assertEquals("TestThrower", sr.type);

    // show current plan
    Plan masterPlan = ServiceData.getPlan("catcher01", "TestCatcher");
    log.info("current plan {}", masterPlan);
    // change plan name
    Runtime.setPeer("catcher01.subpeer", "rootTracking", "Tracking");
    // show modified plan
    masterPlan = Runtime.getPlan("catcher01", "TestCatcher");
    log.info("current plan {}", masterPlan);

    metaData = ServiceData.getMetaData("catcher01", "TestCatcher");

    // release
    Runtime.release("catcher01");

    // verify
    catcher01 = (TestCatcher) Runtime.start("catcher01", "TestCatcher");
    si = catcher01.startPeer("subpeer");
    assertNotNull(si);
    assertEquals("rootTracking", si.getName());
    assertEquals("Tracking", si.getSimpleName());

    // release
    Runtime.release("catcher01");

    // verify a plan exists with a subpeer
    MetaData i01MetaData = Runtime.getMetaData("i01", "InMoov2");
    log.info("i01 meta data {}", i01MetaData);

    // release
    Runtime.release("catcher01");

    // modifying master plan ????
    // FIXME FIXME FIXME - will this work ???
    Runtime.clearPlan(); // <--- FIXME verify
    Runtime.setPeer("catcher01", "catcher02", "TestCatcher");
    masterPlan = Runtime.getPlan("catcher01", "TestCatcher");
    log.info("current plan {}", masterPlan);
    catcher01 = (TestCatcher) Runtime.start("catcher01", "TestCatcher");
    assertNull(Runtime.getService("catcher02"));

    // checking master plan
    // MetaData masterPlan =
    // ServiceData.buildMetaData("catcher01","TestCatcher");
    log.info("masterPlan {}", masterPlan);

    // verifying override
    String t = masterPlan.get("catcher01");
    assertEquals("TestCatcher", t);

    // modifying master plan ????
    Runtime.setPeer("i01.left", "i01.left", "Sabertooth");
    log.info("masterPlan {}", masterPlan);

    // retrieving i01 plan
    i01MetaData = ServiceData.getMetaData("i01", "InMoov2");
    log.info("i01Plan {}", i01MetaData);

    // clean up
    Runtime.clearPlan();
    catcher01 = (TestCatcher) Runtime.create("catcher01", "TestCatcher");

//    TestThrower subPeer = (TestThrower) catcher01.startPeer("subpeer");
//    assertNotNull(subPeer);
    // runtime was created before - is responsibility of service to
    // iterate through services for previously created services they
    // are interested in
    assertFalse(catcher01.onCreated.contains("runtime"));

    TestCatcher catcher02 = (TestCatcher) Runtime.create("catcher02", "TestCatcher");
    assertFalse(catcher02.onCreated.contains("runtime"));

    String catcher01FullName = catcher01.getFullName();
    String catcher02FullName = catcher02.getFullName();

    /**
     * <pre>
      * perhaps all these subscriptions is a bad idea
     
     // verify registered cross-pollination
     assertNotNull(catcher01.onRegistered.get(catcher02FullName)); 
     // catcher01 was created before catcher02
     assertNull(catcher02.onRegistered.get(catcher01FullName)); 
    
     // no created cross-pollination
     assertTrue(catcher01.onCreated.contains(catcher02FullName)); 
     assertFalse(catcher02.onCreated.contains(catcher01FullName)); 
     
     // verify not started
     assertFalse(catcher01.onStarted.contains(catcher02FullName)); 
     assertFalse(catcher02.onStarted.contains(catcher01FullName));
     
     WebGui webgui = (WebGui)Runtime.create("webgui", "WebGui");
     webgui.autoStartBrowser(false);
     webgui.startService();
     
     catcher02.startService();
     assertTrue(catcher01.onStarted.contains(catcher02FullName)); 
     assertTrue(catcher02.onStarted.contains(catcher02FullName)); 
     
     
       // stopped
     assertFalse(catcher01.onStopped.contains(catcher02FullName));
     catcher02.stopService();
     assertTrue(catcher01.onStopped.contains(catcher02FullName));
     
     // released
     
     assertFalse(catcher01.onReleased.contains(catcher02FullName));
     catcher02.releaseService();
     assertTrue(catcher01.onStopped.contains(catcher02FullName));
     * 
     * </pre>
     */

    // release peer
    log.info("plan {}", metaData);
    log.info("plan {}", i01MetaData);

  }

}
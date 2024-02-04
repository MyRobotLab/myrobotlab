package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.framework.DescribeQuery;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.service.interfaces.Gateway;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class RuntimeTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(RuntimeTest.class);

  @Before
  public void setUp() {
    // LoggingFactory.init("WARN");
  }

  @Test
  public void testGetExternalIPAddress() throws Exception {
    if (hasInternet()) {
      try {
        String externalIP = Runtime.getExternalIp();
        Assert.assertNotNull(externalIP);
        Assert.assertEquals(4, externalIP.split("\\.").length);
      } catch (Exception e) {
        log.error("testGetExternalIPAddress failed", e);
      }
    }
  }

  @Test
  public void testGetLocalAddresses() {
    List<String> addresses = Runtime.getIpAddresses();
    Assert.assertNotNull(addresses);
  }

  @Test
  public void testGetLocalHardwareAddresses() {
    List<String> addresses = Runtime.getLocalHardwareAddresses();
    Assert.assertNotNull(addresses);
  }

  @Test
  public void registerRemoteService() {

    Registration registration = new Registration("remoteId", "clock", "Clock");
    Runtime.register(registration);

    Clock clock = (Clock) Runtime.getService("clock@remoteId");
    Assert.assertNotNull(clock);

    // cleanup
    Runtime.release("clock@remoteId");
  }

  @Test
  public void testGetLocalServices() {
    Map<String, ServiceInterface> se = Runtime.getLocalServices();
    Assert.assertNotNull(se);
  }

  @Test
  public void testGetUptime() {
    String res = Runtime.getUptime();
    Assert.assertTrue(res.contains("hour"));
  }

  // @Test
  // public void testGetLocalServicesForExport() {
  // ServiceEnvironment se = Runtime.getLocalServicesForExport();
  // Assert.assertNotNull(se);
  // Assert.assertNotNull(se.platform.getArch());
  // Assert.assertNotNull(se.platform.getOS());
  // Assert.assertNotNull(se.platform.getBitness());
  // }

  @Test
  public void testRuntimeLocale() {

    long curr = 1479044758691L;
    Date d = new Date(curr);

    Runtime runtime = Runtime.getInstance();
    runtime.setLocale("fr-FR");
    assertEquals("expecting concat fr-FR", "fr-FR", runtime.getLocale().getTag());

    assertEquals("fr", runtime.getLanguage());
    Locale l = runtime.getLocale();
    assertEquals("fr-FR", l.toString());

  }
  
  @Test 
  public void testRuntimeIsAvailable() {
    Runtime runtime = Runtime.getInstance();
    assertTrue(Runtime.isAvailable());
    Runtime.releaseAll(true, true);
    assertFalse(Runtime.isAvailable());
  }
  

  @Test
  public void testGetDescribeMessage() {
    Message msg = Runtime.get().getDescribeMsg("testUUID");
    assertEquals("Incorrect method", "describe", msg.method);
    assertEquals("Incorrect data length", 2, msg.data.length);
    assertEquals("Incorrect UUID for describe message", Gateway.FILL_UUID_MAGIC_VAL, msg.data[0]);
    assertTrue("Incorrect message second parameter type", DescribeQuery.class.isAssignableFrom(msg.data[1].getClass()));
    assertEquals("Incorrect UUID in describe query", "testUUID", ((DescribeQuery) msg.data[1]).uuid);
  }

}

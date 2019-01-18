package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.framework.ServiceEnvironment;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class RuntimeTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(RuntimeTest.class);

  @Before
  public void setUp() {
    LoggingFactory.init("WARN");
  }

  @Test
  public void testGetUptime() {
    String res = Runtime.getUptime();
    Assert.assertTrue(res.contains("hour"));
  }

  @Test
  public void testGetLocalServices() {
    ServiceEnvironment se = Runtime.getLocalServices();
    Assert.assertNotNull(se);
  }

  @Test
  public void testGetLocalAddresses() {
    List<String> addresses = Runtime.getLocalAddresses();
    Assert.assertNotNull(addresses);
  }

  @Test
  public void testGetLocalHardwareAddresses() {
    List<String> addresses = Runtime.getLocalHardwareAddresses();
    Assert.assertNotNull(addresses);
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
    runtime.setLocale("fr", "FR");
    // TODO: how do i test this?

    // you can't test default reliably
    DateFormat formatter = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault());
    String today = formatter.format(d);

    // you cant test default reliably
    // assertEquals("13 novembre 2016", today);

    runtime.setLocale("en");
    formatter = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault());
    today = formatter.format(d);

    assertEquals("November 13, 2016", today);

  }

}
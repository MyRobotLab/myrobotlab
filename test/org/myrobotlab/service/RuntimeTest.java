package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;
import org.myrobotlab.framework.ServiceEnvironment;

public class RuntimeTest {

  @Test
  public void testRuntime() throws Exception {
    System.out.println("This is a junit test... woot!");
    Runtime testService = new Runtime("testruntime");
    // try to start the service
    testService.startService();

    // make sure the service knows it's name...
    assertEquals("testruntime", testService.getIntanceName());

    // try to stop the service.
    testService.stopService();
    // we assume we get here. if not runtime didn't start...
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
    String externalIP = Runtime.getExternalIp();
    Assert.assertNotNull(externalIP);
    Assert.assertEquals(4, externalIP.split("\\.").length);
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

    
    Runtime.setLocale("fr", "FR");
    // TODO: how do i test this?
    
    // you can't test default reliably
    DateFormat formatter = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault());
    String today = formatter.format(d);

    // you cant test default reliably
    // assertEquals("13 novembre 2016", today);

    Runtime.setLocale("en");
    formatter = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault());
    today = formatter.format(d);

    assertEquals("November 13, 2016", today);
    
    
  }

}

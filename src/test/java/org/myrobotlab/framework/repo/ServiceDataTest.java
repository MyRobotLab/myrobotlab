package org.myrobotlab.framework.repo;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.ArduinoTest;
import org.slf4j.Logger;

public class ServiceDataTest {

  public final static Logger log = LoggerFactory.getLogger(ArduinoTest.class);

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    LoggingFactory.init("WARN");
  }

  @Test
  public void testGetLocalInstance() {
    ServiceData sd = ServiceData.getLocalInstance();
    String[] srn = sd.getServiceTypeNames();
    log.info("{}", srn.length);
    assertNotNull(srn);
    assertTrue(srn.length > 0);
  }

  @Test
  public void testGenerate() throws IOException {
    ServiceData sd = ServiceData.getLocalInstance();
    ServiceData generated = ServiceData.generate();
    assertNotNull(sd);
    assertNotNull(generated);
  }

  @Test
  public void testAdd() {
    ServiceData sd = ServiceData.getLocalInstance();
    // TODO: add a valid assert for this test.
    List<ServiceType> types = sd.getAvailableServiceTypes();
    assertTrue(types.size() > 0);
  }

}
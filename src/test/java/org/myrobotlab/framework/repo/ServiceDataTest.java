package org.myrobotlab.framework.repo;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class ServiceDataTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(ServiceDataTest.class);

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @Test
  public void testAdd() {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    ServiceData sd = ServiceData.getLocalInstance();
    // TODO: add a valid assert for this test.
    List<ServiceType> types = sd.getAvailableServiceTypes();
    assertTrue(types.size() > 0);
  }

  @Test
  public void testGenerate() throws IOException {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    ServiceData sd = ServiceData.getLocalInstance();
    ServiceData generated = ServiceData.generate();
    assertNotNull(sd);
    assertNotNull(generated);
  }

  @Test
  public void testGetLocalInstance() {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    ServiceData sd = ServiceData.getLocalInstance();
    String[] srn = sd.getServiceTypeNames();
    log.info("{}", srn.length);
    assertNotNull(srn);
    assertTrue(srn.length > 0);
  }

}
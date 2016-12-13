package org.myrobotlab.framework.repo;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.ArduinoTest;
import org.slf4j.Logger;

public class ServiceDataTest {

  public final static Logger log = LoggerFactory.getLogger(ArduinoTest.class);

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testGetLocalInstance() {
    ServiceData sd = ServiceData.getLocalInstance();
    String[] srn = sd.getServiceTypeNames();
    log.info("{}", srn.length);
  }

  @Test
  public void testGenerate() throws IOException {
    ServiceData sd = ServiceData.getLocalInstance();
    ServiceData generated = ServiceData.generate();

    assertNotNull(sd);
    assertNotNull(generated);
  }

  @Test
  public void testServiceData() {
  }

  @Test
  public void testAdd() {
    ServiceData sd = ServiceData.getLocalInstance();
    // TODO: add a valid assert for this test.
    // List<ServiceType> types = sd.getAvailableServiceTypes();
    sd.add(new ServiceType("test"));

  }

  @Test
  public void testContainsServiceType() {
    // fail("Not yet implemented");
  }

  @Test
  public void testGetAvailableServiceTypes() {
    // fail("Not yet implemented");
  }

  @Test
  public void testGetCategory() {
    // fail("Not yet implemented");
  }

  @Test
  public void testGetCategoryNames() {
    // fail("Not yet implemented");
  }

  @Test
  public void testGetServiceTypeDependencyKeys() {
    // fail("Not yet implemented");
  }

  @Test
  public void testGetServiceTypeNames() {
    // fail("Not yet implemented");
  }

  @Test
  public void testGetServiceTypeNamesString() {
    // fail("Not yet implemented");
  }

  @Test
  public void testGetServiceType() {
    // fail("Not yet implemented");
  }

  @Test
  public void testGetServiceTypes() {
    // fail("Not yet implemented");
  }

  @Test
  public void testSave() {
    // fail("Not yet implemented");
  }

  @Test
  public void testSaveString() {
    // fail("Not yet implemented");
  }

  @Test
  public void testGetCategories() {
    // fail("Not yet implemented");
  }

  @Test
  public void testMain() {
    // fail("Not yet implemented");
  }

  @Test
  public void testGetDependencyKeys() {
    // fail("Not yet implemented");
  }

}

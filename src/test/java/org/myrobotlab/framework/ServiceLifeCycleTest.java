package org.myrobotlab.framework;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class ServiceLifeCycleTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(ServiceLifeCycleTest.class);

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {

  }

  @AfterClass
  public static void setUpAfterClass() throws Exception {

  }

  @Test
  public void serviceLifeCycleTest() throws Exception {
    
    // test saving out runtime.yml
    
    // clearing and reloading 
    
  }

}
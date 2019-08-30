package org.myrobotlab.codec;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.framework.MethodCacheTest;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class ApiCliTest extends AbstractTest {
  
  public final static Logger log = LoggerFactory.getLogger(ApiCliTest.class);

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
  public void test() {
    fail("Not yet implemented");
  }
  
  public static void main(String[] args) {
    try {
      LoggingFactory.init(Level.DEBUG);

      // input could be a connection
      // input could be a json msg
      // input could be a strong typed msg
      // input could be a string
      
      // Api api = Api.getApiKey(uri);
      // api.


    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}

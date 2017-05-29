package org.myrobotlab.framework;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class TaskTest {

  public final static Logger log = LoggerFactory.getLogger(TaskTest.class);

  
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
  public void addTask() {
    Runtime runtime = Runtime.getInstance();
    runtime.addTask(1000, "getUptime");
    
    log.info("here");
  }


  public static void main(String[] args) {
    try {

      LoggingFactory.init();

      TaskTest.setUpBeforeClass();
      TaskTest test = new TaskTest();
      test.setUp();
      
      test.addTask();

      // structured testing begins
      // test.doSomething();

      /*
      JUnitCore junit = new JUnitCore();
      Result result = junit.run(TaskTest.class);
      log.info("Result: {}", result);
      */
      
      

      // Runtime.dump();

    } catch (Exception e) {
      log.error("test threw", e);
    }
  }
}

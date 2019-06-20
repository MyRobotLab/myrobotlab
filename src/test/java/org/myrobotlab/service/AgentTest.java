package org.myrobotlab.service;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AgentTest {

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
  
  @Test // FIXME - picocli --version is correct and -v
  public void testVersion() {
    String[] cmdLine = new String[] {"-version"};
    Agent.main(cmdLine);
  }

  @Test
  public void test() {
    String[] cmdLine = new String[] {};
    Agent.main(cmdLine);
  }

}

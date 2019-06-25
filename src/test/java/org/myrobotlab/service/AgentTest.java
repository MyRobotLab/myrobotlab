package org.myrobotlab.service;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;

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
  
  /**
   * FIXME ! - Agent wants a jar to run - no jar is made !!!! BEFORE !!!! tests are run
   * 
   * agent error no source agent jar can be found checked:
      classes
      /opt/jenkins-slave/workspace/ab-multibranch_agent_auto_update/target/myrobotlab.jar
      are you using ide? please package a build (mvn package -DskipTest)
      20:15:37.797 INFO [main] c.m.s.Agent [Agent.java:219] on branch develop copying agent's current jar to appropriate location classes -> branches/develop-unknown/myrobotlab.jar
      20:15:37.804 ERROR [main] c.m.s.Runtime [Runtime.java:415] createService failed
   */
  
  @Ignore // FIXME - picocli --version is correct and -v
  public void testVersion() {
    String[] cmdLine = new String[] {"-version"};
    Agent.main(cmdLine);
  }

  @Ignore
  public void test() {
    String[] cmdLine = new String[] {};
    Agent.main(cmdLine);
  }

}

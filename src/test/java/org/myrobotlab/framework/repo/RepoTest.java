package org.myrobotlab.framework.repo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.interfaces.StatusPublisher;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class RepoTest implements StatusPublisher {

  public final static Logger log = LoggerFactory.getLogger(RepoTest.class);
  ArrayList<Status> status = new ArrayList<Status>();

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
    Repo repo = Repo.getInstance();
    repo.clear();
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testGetLocalInstance() {
    Repo repo = Repo.getInstance();
    assertTrue(repo != null);
  }

  /*
   * public constructor only because serialization might require it - otherwise
   * this should be private.
   * 
   * @Test public void testRepo() { // fail("Not yet implemented"); }
   */

  @Test
  public void testAddStatusListener() throws ParseException, IOException {
    Repo repo = Repo.getInstance();
    repo.addStatusPublisher(this);
    repo.install("Arduino");
  }

  /*
   * @Test public void testErrorException() { Repo repo = Repo.getInstance();
   * repo.getErrors().clear(); repo.error(new IOException("io exception test"));
   * assertTrue(repo.getErrors().size() > 0); }
   */

  /*
   * @Test public void explode(){ boolean explode = true; assertFalse(explode);
   * }
   */

  /*
   * 
   * @Test public void testInstall() throws ParseException, IOException { Repo
   * repo = Repo.getInstance(); repo.install();
   * 
   * ServiceData sd = ServiceData.getInstance(); String[] typeNames =
   * sd.getServiceTypeNames(); for (int i = 0; i < typeNames.length; ++i) {
   * assertTrue(repo.isInstalled("Arduino")); }
   * 
   * assertFalse(repo.hasErrors()); }
   * 
   */

  @Test
  public void testClear() {
    Repo repo = Repo.getInstance();
    repo.clear();
    File check = new File("libraries");
    assertFalse(check.exists());
  }

  @Test
  public void testResolveArtifacts() {
    // fail("Not yet implemented");
  }

  @Test
  public void testSave() {
    Repo repo = Repo.getInstance();
    FileIO.rm(Repo.REPO_STATE_FILE_NAME);
    assertFalse(new File(Repo.REPO_STATE_FILE_NAME).exists());
    // Repo repo = Repo.getInstance();
    repo.save();
    assertTrue(new File(Repo.REPO_STATE_FILE_NAME).exists());
  }

  @Test
  public void testAddDependency() {
    // fail("Not yet implemented");
  }

  @Test
  public void testGenerateLibrariesFromRepo() {
    // fail("Not yet implemented");
  }

  @Test
  public void testSetInstalled() {
    // fail("Not yet implemented");
  }

  @Test
  public void testGetKey() {
    // fail("Not yet implemented");
  }

  @Test
  public void testGetUnfulfilledDependencies() {
    Repo repo = Repo.getInstance();
    repo.clear();
    Set<ServiceDependency> deps = repo.getUnfulfilledDependencies("Serial");
    deps.size();
    // may change - but unlikely....
    assertTrue(deps.size() > 0);
  }

  @Test
  public void testIsInstalled() throws ParseException, IOException {
    Repo repo = Repo.getInstance();
    repo.clear();
    repo.install("Arduino");
    assertTrue(repo.isInstalled("Arduino"));
  }

  public static void main(String[] args) {
    try {

      // LoggingFactory.init(Level.INFO);

      /*
       * 
       * Repo repo = Repo.getInstance(); repo.clear();
       * 
       * RepoTest.setUpBeforeClass(); RepoTest test = new RepoTest();
       * test.testGetUnfulfilledDependencies();
       */

      JUnitCore junit = new JUnitCore();
      Result result = junit.run(RepoTest.class);
      log.info("Result: {}", result);

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  @Override
  public Status publishStatus(Status status) {
    log.info(status.toString());
    return status;
  }

  @Override
  public void broadcastStatus(Status status) {
    log.info(status.toString());
  }

}

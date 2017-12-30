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
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.RepoInstallListener;
import org.slf4j.Logger;

public class RepoTest implements RepoInstallListener {

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
    Repo repo = Repo.getLocalInstance();
    repo.clear();
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testGetLocalInstance() {
    Repo repo = Repo.getLocalInstance();
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
    Repo repo = Repo.getLocalInstance();
    repo.addStatusListener(this);
    repo.install("Arduino");
  }

  /*
   * @Test public void testErrorException() { Repo repo =
   * Repo.getLocalInstance(); repo.getErrors().clear(); repo.error(new
   * IOException("io exception test")); assertTrue(repo.getErrors().size() > 0);
   * }
   */

  /*
   * @Test public void explode(){ boolean explode = true; assertFalse(explode);
   * }
   */

  @Test
  public void testErrorStringObjectArray() {
    Repo repo = Repo.getLocalInstance();
    repo.error("%s is a test of errors", "myError");
    assertTrue(repo.getErrors().size() > 0);
  }

  @Test
  public void testGetErrors() {
    Repo repo = Repo.getLocalInstance();
    repo.error("this is an error");
    assertTrue(repo.getErrors().size() > 0);
  }

  @Test
  public void testHasErrors() {
    Repo repo = Repo.getLocalInstance();
    repo.error("this is an error");
    assertTrue(repo.hasErrors());
  }

  @Test
  public void testInfo() {
    Repo repo = Repo.getLocalInstance();
    repo.info("this is an info status");
    assertFalse(repo.hasErrors());
  }

  /*
   * 
   * @Test public void testInstall() throws ParseException, IOException { Repo
   * repo = Repo.getLocalInstance(); repo.install();
   * 
   * ServiceData sd = ServiceData.getLocalInstance(); String[] typeNames =
   * sd.getServiceTypeNames(); for (int i = 0; i < typeNames.length; ++i) {
   * assertTrue(repo.isInstalled("Arduino")); }
   * 
   * assertFalse(repo.hasErrors()); }
   * 
   */

  @Test
  public void testClear() {
    Repo repo = Repo.getLocalInstance();
    repo.clear();
    File check = new File("libraries");
    assertFalse(check.exists());
  }

  @Test
  public void testInstallString() throws ParseException, IOException {
    // repo.clear() ??
    Repo repo = Repo.getLocalInstance();
    repo.addStatusListener(this);
    repo.install("Arduino");
    assertTrue(repo.isInstalled("Arduino"));
  }

  @Test
  public void testIsServiceTypeInstalled() throws ParseException, IOException {

    Repo repo = Repo.getLocalInstance();
    assertFalse(repo.isInstalled("Arduino"));

    repo.addStatusListener(this);
    repo.install("Arduino");
    assertTrue(repo.isInstalled("Arduino"));
  }

  @Test
  public void testResolveArtifacts() {
    // fail("Not yet implemented");
  }

  @Test
  public void testSave() {
    Repo repo = Repo.getLocalInstance();
    FileIO.rm(Repo.REPO_STATE_FILE_NAME);
    assertFalse(new File(Repo.REPO_STATE_FILE_NAME).exists());
    // Repo repo = Repo.getLocalInstance();
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
    Repo repo = Repo.getLocalInstance();
    repo.clear();
    Set<Library> deps = repo.getUnfulfilledDependencies("Serial");
    deps.size();
    // may change - but unlikely....
    assertTrue(deps.size() > 0);
  }

  @Test
  public void testIsInstalled() throws ParseException, IOException {
    Repo repo = Repo.getLocalInstance();
    repo.clear();
    repo.install("Arduino");
    assertTrue(repo.isInstalled("Arduino"));
  }

  @Override
  public void onInstallProgress(Status status) {
    this.status.add(status);
    log.info(status.toString());
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      /*
       * 
       * Repo repo = Repo.getLocalInstance(); repo.clear();
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

}

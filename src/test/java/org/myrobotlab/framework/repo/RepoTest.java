package org.myrobotlab.framework.repo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.interfaces.StatusPublisher;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class RepoTest extends AbstractTest implements StatusPublisher {

  public final static Logger log = LoggerFactory.getLogger(RepoTest.class);
  ArrayList<Status> status = new ArrayList<Status>();

  @AfterClass
  public static void lastCleanup() {
    Repo repo = Repo.getInstance();
    repo.clear();
    installed = false;
  }
  
  public String getName() {
    return "RepoTest";
  }

  @Override
  public void broadcastStatus(Status status) {
    log.info(status.toString());
  }

  @Override
  public Status publishStatus(Status status) {
    log.info(status.toString());
    return status;
  }

  @Before
  public void setUp() throws Exception {
    // LoggingFactory.init("WARN");
    Repo repo = Repo.getInstance();
    repo.clear();
  }

  @Test
  public void testAddStatusListener() throws Exception {
    Repo repo = Repo.getInstance();
    repo.addStatusPublisher(this);
    repo.install("Arduino");
  }

  @Test
  public void testClear() {
    Repo repo = Repo.getInstance();
    repo.clear();
    File check = new File("libraries");
    assertFalse(check.exists());
  }

  @Test
  public void testGetLocalInstance() {
    Repo repo = Repo.getInstance();
    assertTrue(repo != null);
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
  public void testIsInstalled() throws Exception {
    Repo repo = Repo.getInstance();
    repo.clear();
    repo.install("Arduino");
    assertTrue(repo.isInstalled("Arduino"));
  }

  @Test
  public void testSave() {
    Repo repo = Repo.getInstance();
    FileIO.rm(repo.getRepoPath());
    assertFalse(new File(repo.getRepoPath()).exists());
    // Repo repo = Repo.getInstance();
    repo.save();
    assertTrue(new File(repo.getRepoPath()).exists());
  }

}

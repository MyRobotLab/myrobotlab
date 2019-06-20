package org.myrobotlab.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.cmdline.CmdLine;
import org.myrobotlab.codec.CodecJson;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.MrlException;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ProcessData;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.repo.GitHub;
import org.myrobotlab.framework.repo.Repo;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.image.Util;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.Http;
import org.slf4j.Logger;

import com.google.gson.internal.LinkedTreeMap;

/**
 * 
 * @author GroG
 * 
 *         Agent is responsible for managing running instances of myrobotlab. It
 *         can start, stop and update myrobotlab.
 * 
 * 
 *         FIXME - all update functionality will need to be moved to Runtime it
 *         should take parameters such that it will be possible at some point to
 *         do an update from a child process and update the agent :)
 *
 *         FIXME - testing test - without version test - remote unaccessable
 *         FIXME - spawn must be synchronized 2 threads (the timer and the user)
 *
 *
 */
public class Agent extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Agent.class);

  final Map<String, ProcessData> processes = new ConcurrentHashMap<String, ProcessData>();

  transient static SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd.HHmmssSSS");

  Platform platform = Platform.getLocalInstance();

  /**
   * command line to be relayed to the the first process the Agent spawns
   */
  static CmdLine cmdline;

  transient WebGui webgui = null;
  int port = 8887;
  String address = "127.0.0.1";

  /**
   * command line for the Agent process
   */
  static CmdLine agentCmdline;

  String branchAgent = Platform.getLocalInstance().getBranch();
  String branchLast = null;
  static String branchRequested = null;

  String versionAgent = Platform.getLocalInstance().getVersion();
  String versionLast = null;
  String versionLatest = null;
  static String versionRequested = null;

  /**
   * auto update - automatically checks for updates and WILL update any running
   * mrl instance automatically
   */
  static boolean autoUpdate = false;

  /**
   * autoCheckForUpdate - checks automatically checks for updates after some
   * interval but does not automatically update - it publishes events of new
   * availability of updates but does not update
   */
  static boolean autoCheckForUpdate = false;

  static HashSet<String> possibleVersions = new HashSet<String>();

  // for more info -
  // http://build.myrobotlab.org:8080/job/myrobotlab-multibranch/job/develop/api/json

  @Deprecated /* not needed - use more general urls with filter functions */
  final static String REMOTE_LAST_SUCCESSFUL_BUILD_JAR = "http://build.myrobotlab.org:8080/job/myrobotlab-multibranch/job/%s/lastSuccessfulBuild/artifact/target/myrobotlab.jar";

  final static String REMOTE_BUILDS_URL = "http://build.myrobotlab.org:8080/job/myrobotlab-multibranch/job/%s/api/json?tree=builds[number,status,timestamp,id,result]";

  final static String REMOTE_JAR_URL = "http://build.myrobotlab.org:8080/job/myrobotlab-multibranch/job/%s/%s/artifact/target/myrobotlab.jar";

  @Deprecated /* not needed - use more general urls with filter functions */
  final static String REMOTE_LAST_SUCCESSFUL_VERSION = "http://build.myrobotlab.org:8080/job/myrobotlab-multibranch/job/%s/api/json?tree=lastSuccessfulBuild[number,status,timestamp,id,result]";

  boolean checkRemoteVersions = false;

  String versionPrefix = "1.1.";

  /**
   * singleton for security purposes
   */
  static Agent agent;

  String rootBranchDir = "branches";

  /**
   * development variable to force version "unknown" to be either greatest or
   * smallest version for development
   */
  private static boolean unknownIsGreatest = false;

  public static class WorkflowJob {
    WorkflowRun lastSuccessfulBuild;
    WorkflowRun[] builds;
  }

  public static class WorkflowRun {
    String id;
    Integer number;
    String result;
    Long timestamp;
  }

  public static String BRANCHES_ROOT = "branches";

  public Agent(String n) throws IOException {
    super(n);
    log.info("Agent {} Pid {} is alive", n, Platform.getLocalInstance().getPid());

    if (branchRequested == null) {
      branchRequested = branchAgent;
    }

    // basic setup - minimally we make a directory
    // and instance folder of the same branch & version as the
    // agent jar
    setup();

    // user has decided to look for updates ..
    if (autoUpdate || checkRemoteVersions) {
      invoke("getPossibleVersions", branchAgent);
    }
  }

  public String getDir(String branch, String version) {
    if (branch == null) {
      branch = branchAgent; // FIXME - or lastBranch ? or currentBranch !!!
    }
    if (version == null) {
      try {
        version = getLatestVersion(branch, autoUpdate);
      } catch (Exception e) {
        log.error("getDir threw", e);
      }
    }
    return BRANCHES_ROOT + File.separator + branch + "-" + version;
  }

  public String getFilePath(String branch, String version) {
    return getDir(branch, version) + File.separator + "myrobotlab.jar";
  }

  private void setup() throws IOException {
    // FIXME - this stuff needs to be outside the contructor !!!
    // initialize perhaps ? setup ? oneTime ? initialInstall ?

    // location of the agent's branch (and version)
    String agentVersionPath = getDir(branchAgent, versionAgent);

    if (!new File(agentVersionPath).exists()) {
      File branchDir = new File(agentVersionPath);
      branchDir.mkdirs();
    }

    String agentMyRobotLabJar = getFilePath(branchAgent, versionAgent);
    if (!new File(agentMyRobotLabJar).exists()) {

      String agentJar = new java.io.File(Agent.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();

      if (!new File(agentJar).exists() || !agentJar.endsWith(".jar")) {
        // not operating in released runtime mode - probably operating in ide
        String ideTargetJar = new File(System.getProperty("user.dir") + File.separator + "target" + File.separator + "myrobotlab.jar").getAbsolutePath();
        if (!new File(ideTargetJar).exists()) {
          error("no source agent jar can be found checked:\n%s\n%s\nare you using ide? please package a build (mvn package -DskipTest)", agentJar, ideTargetJar);
        } else {
          agentJar = ideTargetJar;
        }
      }

      log.info("on branch {} copying agent's current jar to appropriate location {} -> {}", branchRequested, agentJar, agentMyRobotLabJar);
      Files.copy(Paths.get(agentJar), Paths.get(agentMyRobotLabJar), StandardCopyOption.REPLACE_EXISTING);
    }
  }

  public void startWebGui() {
    try {

      if (webgui == null) {
        webgui = (WebGui) Runtime.create("webadmin", "WebGui");
        webgui.autoStartBrowser(false);
        webgui.setPort(port);
        webgui.setAddress(address);
        webgui.startService();
      } else {
        log.info("webgui already started");
      }

    } catch (Exception e) {
      log.error("startWebGui threw", e);
    }
  }

  public void autoUpdate(boolean b) {
    if (b) {
      addTask("processUpdates", 1000 * 60, 0, "processUpdates");
    } else {
      purgeTask("processUpdates");
    }
  }

  /**
   * called by the autoUpdate task which is scheduled every minute to look for
   * updates from the build server
   */
  public void processUpdates() {
    for (String key : processes.keySet()) {
      ProcessData process = processes.get(key);

      if (!process.autoUpdate) {
        continue;
      }
      try {

        // processUpdates(process.id, process.branch, version, allowRemote);
        processUpdates(process.id, process.branch, null, true);

        if (process.isRunning()) {
          restart(process.id);
        }
      } catch (Exception e) {
        log.error("proccessing updates from scheduled task threw", e);
      }
    }
  }

  /**
   * max complexity method to process and update
   * 
   * @throws IOException
   *           e
   * @throws URISyntaxException
   *           e
   * @throws InterruptedException
   *           e
   * @throws MrlException
   * 
   */
  synchronized public void processUpdates(String id, String branch, String version, Boolean allowRemote)
      throws IOException, URISyntaxException, InterruptedException, MrlException {

    getLatest(branch);

    /**
     * <pre>
     * for all running instances - see if they can be updated ... on their
     * appropriate branch - restart if necessary
     */

  }

  public void getLatest(String branch) {
    try {
      // check for updates
      String version = getLatestVersion(branch, autoUpdate);

      // check if branch and version exist locally
      if (!existsLocally(branch, version)) {
        getJar(branch, version); // FIXME - make part file .unconfirmed
        // download latest to the appropriate directory
        // mkdirs
        // download file
        if (!verifyJar(branch, version)) {
          // removeJar(branch, version + ".unconfirmed");
        }
      }
    } catch (Exception e) {
      error(e);
    }
  }

  // FIXME - implement
  private boolean verifyJar(String branch, String version) {
    return true;
  }

  public void getJar(String branch, String version) {
    new File(getDir(branch, version)).mkdirs();
    String build = getBuildId(version);
    Http.get(String.format(REMOTE_JAR_URL, branch, build), getFilePath(branch, version));
  }

  public String getBuildId(String version) {
    String[] parts = version.split("\\.");
    return parts[2];
  }

  public String getLatestVersion(String branch, Boolean allowRemote) throws MrlException {
    Set<String> versions = getVersions(branch, allowRemote);
    return getLatestVersion(versions);
  }

  public String getLatestVersion(Set<String> versions) throws MrlException {
    String latest = null;
    for (String version : versions) {
      if (latest == null) {
        latest = version;
        continue;
      }
      if (isGreaterThan(version, latest)) {
        latest = version;
      }
    }
    return latest;
  }

  /**
   * checks to see if a branch / version jar exists on the local filesystem
   * 
   * @param branch
   * @param version
   * @return
   */
  public boolean existsLocally(String branch, String version) {
    return new File(getFilePath(branch, version)).exists();
  }

  /**
   * if there is a single instance - just restart it ...
   * 
   * @throws IOException
   *           e
   * @throws URISyntaxException
   *           e
   * @throws InterruptedException
   *           e
   * 
   */
  public synchronized void restart(String id) throws IOException, URISyntaxException, InterruptedException {
    log.info("restarting process {}", id);
    // ProcessData pd2 = copy(id);
    // pd.setRestarting();
    kill(id); // FIXME - kill should include prepare to shutdown ...
    sleep(2000);
    spawn2(id);
  }

  private void spawn2(String id) {
    try {
      if (processes.containsKey(id)) {
        spawn2(processes.get(id));
      } else {
        log.error("agent does not know about process id {}", id);
      }
    } catch (Exception e) {
      log.error("spawn2({}) threw ", id, e);
    }
  }

  /**
   * return a non-running process structure from an existing one with a new id
   * 
   * @param id
   *          id
   * @return process data
   * 
   */
  public ProcessData copy(String id) {
    if (!processes.containsKey(id)) {
      log.error("cannot copy %s does not exist", id);
      return null;
    }
    ProcessData pd = processes.get(id);
    ProcessData pd2 = new ProcessData(pd);
    pd2.startTs = null;
    pd2.stopTs = null;
    String[] parts = id.split("\\.");
    if (parts.length == 4) {
      try {
        int instance = Integer.parseInt(parts[3]);
        ++instance;
      } catch (Exception e) {
      }
    } else {
      pd2.id = id + ".0";
    }

    processes.put(pd2.id, pd2);
    if (agent != null) {
      agent.broadcastState();
    }
    return pd2;
  }

  public void copyAndStart(String id) throws IOException {
    // returns a non running copy with new process id
    // on the processes list
    ProcessData pd2 = copy(id);
    spawn2(pd2);
    if (agent != null) {
      agent.broadcastState();
    }
  }

  public void downloadLatest(String branch) throws IOException {
    String version = getLatestRemoteVersion(branch);
    log.info("downloading version {} /{}", version, branch);
    byte[] myrobotlabjar = getLatestRemoteJar(branch);
    if (myrobotlabjar == null) {
      throw new IOException("could not download");
    }
    log.info("{} bytes", myrobotlabjar.length);

    /*
     * File archive = new File(String.format("%s/archive", branch));
     * archive.mkdirs();
     */

    FileOutputStream fos = new FileOutputStream(String.format("%s/myrobotlab.%s.jar", branch, version));
    fos.write(myrobotlabjar);
    fos.close();
  }

  /**
   * gets id from name
   * 
   * @param name
   *          name
   * @return integer
   * 
   */
  public String getId(String name) {
    for (String pid : processes.keySet()) {
      if (pid.equals(name)) {
        return processes.get(pid).id;
      }
    }
    return null;
  }

  // FIXME - should just be be saveRemoteJar() - but shouldn't be from
  // multiple threads
  static public byte[] getLatestRemoteJar(String branch) {
    return Http.get(String.format(REMOTE_LAST_SUCCESSFUL_BUILD_JAR, branch));
  }

  public String getLatestRemoteVersion(String branch) {
    try {
      byte[] data = Http.get(String.format(REMOTE_LAST_SUCCESSFUL_VERSION, branch));
      if (data != null) {
        String json = new String(data);
        CodecJson decoder = new CodecJson();
        WorkflowJob job = (WorkflowJob) decoder.decode(json, WorkflowJob.class);

        return versionPrefix + job.lastSuccessfulBuild.id;
      }
    } catch (Exception e) {
      log.error("getLatestRemoteVersion threw", e);
    }
    return null;
  }

  /**
   * gets name from id
   * 
   * @param id
   *          e
   * @return string
   */
  public String getName(String id) {
    for (String pid : processes.keySet()) {
      if (pid.equals(id)) {
        return processes.get(pid).name;
      }
    }

    return null;
  }

  /**
   * FIXME this should be build server not github ... github has not artifacts
   * 
   * @return
   */
  static public Set<String> getBranches() {
    Set<String> possibleBranches = new HashSet<String>();
    try {
      // TODO - all http gets use HttpClient static methods and promise
      // for asynchronous
      // get gitHub's branches
      byte[] r = Http.get(GitHub.BRANCHES);
      if (r != null) {
        String branches = new String(r);
        CodecJson decoder = new CodecJson();
        // decoder.decodeArray(Branch)
        Object[] array = decoder.decodeArray(branches);
        for (int i = 0; i < array.length; ++i) {
          @SuppressWarnings("unchecked")
          LinkedTreeMap<String, String> m = (LinkedTreeMap<String, String>) array[i];
          if (m.containsKey("name")) {
            possibleBranches.add(m.get("name").toString());
          }
        }
      }
    } catch (Exception e) {
      log.error("getRemoteBranches threw", e);
    }
    return possibleBranches;
  }

  static boolean isGreaterThan(String version1, String version2) throws MrlException {
    if (version1 == null) {
      return false;
    }

    if (version2 == null) {
      return true;
    }

    // special development behavior
    if (version1.equals("unknown")) {
      return (unknownIsGreatest) ? true : false;
    }
    if (version2.equals("unknown")) {
      return !((unknownIsGreatest) ? true : false);
    }

    String[] parts1 = version1.split("\\.");
    String[] parts2 = version2.split("\\.");

    if (parts1.length != 3 || parts2.length != 3) {
      throw new MrlException("invalid version isGreaterThan(%s, %s)", version1, version2);
    }

    for (int i = 0; i < 3; ++i) {
      int v1 = Integer.parseInt(parts1[i]);
      int v2 = Integer.parseInt(parts2[i]);
      if (v1 != v2) {
        return v1 > v2;
      }
    }

    throw new MrlException("invalid isGreaterThan(%s, %s)", version1, version2);
  }

  /**
   * This method gets all the version on a particular branch, if allowed remote
   * access it will ask the build server what successful builds exist
   * 
   * @param branch
   * @param allowRemote
   * @return
   */
  synchronized public Set<String> getVersions(String branch, Boolean allowRemote) {
    Set<String> versions = new HashSet<String>();
    versions.addAll(getLocalVersions(branch));
    if (allowRemote) {
      versions.addAll(getRemoteVersions(branch));
    }
    invoke("publishVersions", versions);
    return versions;
  }

  public Set<String> publishVersions(HashSet<String> versions) {
    return versions;
  }

  public Set<String> getRemoteVersions(String branch) {
    Set<String> versions = new HashSet<String>();
    try {

      byte[] data = Http.get(String.format(REMOTE_BUILDS_URL, branch));
      if (data != null) {
        CodecJson decoder = new CodecJson();
        String json = new String(data);
        WorkflowJob job = (WorkflowJob) decoder.decode(json, WorkflowJob.class);
        if (job.builds != null) {
          for (WorkflowRun build : job.builds) {
            if ("SUCCESS".equals(build.result)) {
              versions.add(versionPrefix + build.id);
            }
          }
        }
      }
    } catch (Exception e) {
      error(e);
    }
    return versions;
  }

  public Set<String> getLocalVersions(String branch) {
    Set<String> versions = new HashSet<>();
    // get local file system versions
    File branchDir = new File(BRANCHES_ROOT);
    // get local existing versions
    File[] listOfFiles = branchDir.listFiles();
    for (int i = 0; i < listOfFiles.length; ++i) {
      File file = listOfFiles[i];
      if (file.isDirectory()) {
        if (file.getName().startsWith(branch)) {
          String version = file.getName().substring(branch.length() + 1);// getFileVersion(file.getName());
          if (version != null) {
            versions.add(version);
          }
        }
      }
    }
    return versions;
  }

  static public String getFileVersion(String name) {
    if (!name.startsWith("myrobotlab.")) {
      return null;
    }

    String[] parts = name.split("\\.");
    if (parts.length != 5) {
      return null;
    }

    String version = String.format("%s.%s.%s", parts[1], parts[2], parts[3]);

    return version;
  }

  /**
   * get a list of all the processes currently governed by this Agent
   * 
   * @return hash map, int to process data
   */
  public Map<String, ProcessData> getProcesses() {
    return processes;
  }

  // by id (or by pid?)
  public String kill(String id) {
    // FIXME !!! - "ask" all child processes to kindly Runtime.shutdown via msgs
    // !!
    if (processes.containsKey(id)) {
      if (agent != null) {
        agent.info("terminating %s", id);
      }
      ProcessData process = processes.get(id);
      process.process.destroy();
      process.state = ProcessData.STATE_STOPPED;

      if (process.monitor != null) {
        process.monitor.interrupt();
        process.monitor = null;
      }
      // remove(processes.get(name));
      if (agent != null) {
        agent.info("%s haz beeen terminated", id);
        agent.broadcastState();
      }
      return id;
    } else {
      try {
        // FIXME make operating system independent
        String cmd = "taskkill /F /PID " + id;
        java.lang.Runtime.getRuntime().exec(cmd);
      } catch (Exception e) {
        log.error("kill threw", e);
      }
    }

    log.warn("%s? no sir, I don't know that punk...", id);

    return null;
  }

  /*
   * BAD IDEA - data type ambiguity is a drag public Integer kill(String name) {
   * return kill(getId(name)); }
   */

  public void killAll() {
    // FIXME !!! - "ask" all child processes to kindly Runtime.shutdown via msgs
    // !!
    for (String id : processes.keySet()) {
      kill(id);
    }
    log.info("no survivors sir...");
    if (agent != null) {
      agent.broadcastState();
    }
  }

  public void killAndRemove(String id) {
    if (processes.containsKey(id)) {
      kill(id);
      processes.remove(id);
      if (agent != null) {
        agent.broadcastState();
      }
    }
  }

  /**
   * list processes
   * 
   * @return lp ?
   */
  public String[] lp() {
    Object[] objs = processes.keySet().toArray();
    String[] pd = new String[objs.length];
    for (int i = 0; i < objs.length; ++i) {
      Integer id = (Integer) objs[i];
      ProcessData p = processes.get(id);
      pd[i] = String.format("%s - %s [%s - %s]", id, p.name, p.branch, p.version);
    }
    return pd;
  }

  public String publishTerminated(String id) {
    log.info("publishTerminated - terminated %s %s - restarting", id, getName(id));

    if (!processes.containsKey(id)) {
      log.error("processes {} not found");
      return id;
    }

    // if you don't fork with Agent allowed to
    // exist without instances - then
    if (!cmdline.containsKey("-fork")) {
      // spin through instances - if I'm the only
      // thing left - terminate
      boolean processesStillRunning = false;
      for (ProcessData pd : processes.values()) {
        if (pd.isRunning() || pd.isRestarting()) {
          processesStillRunning = true;
          break;
        }
      }

      if (!processesStillRunning) {
        shutdown();
      }
    }

    if (agent != null) {
      agent.broadcastState();
    }

    return id;
  }

  /**
   * This is a great idea &amp; test - because we want complete control over
   * environment and dependencies - the ability to purge completely - and start
   * from the beginning - but it should be in another service and not part of
   * the Agent. The 'Test' service could use Agent as a peer
   * 
   * @return list of status
   * 
   */
  public List<Status> serviceTest() {

    List<Status> ret = new ArrayList<Status>();
    // CLEAN FOR TEST METHOD

    // FIXME DEPRECATE !!!
    // RUNTIME is responsible for running services
    // REPO is responsible for possible services
    // String[] serviceTypeNames =
    // Runtime.getInstance().getServiceTypeNames();

    HashSet<String> skipTest = new HashSet<String>();

    skipTest.add("org.myrobotlab.service.Runtime");
    skipTest.add("org.myrobotlab.service.OpenNi");

    /*
     * skipTest.add("org.myrobotlab.service.Agent");
     * skipTest.add("org.myrobotlab.service.Incubator");
     * skipTest.add("org.myrobotlab.service.InMoov"); // just too big and
     * complicated at the moment skipTest.add("org.myrobotlab.service.Test");
     * skipTest.add("org.myrobotlab.service.Cli"); // ?? No ?
     */

    long installTime = 0;
    Repo repo = Runtime.getInstance().getRepo();
    ServiceData serviceData = ServiceData.getLocalInstance();
    List<ServiceType> serviceTypes = serviceData.getServiceTypes();

    ret.add(Status.info("serviceTest will test %d services", serviceTypes.size()));
    long startTime = System.currentTimeMillis();
    ret.add(Status.info("startTime", "%d", startTime));

    for (int i = 0; i < serviceTypes.size(); ++i) {

      ServiceType serviceType = serviceTypes.get(i);

      // TODO - option to disable
      if (!serviceType.isAvailable()) {
        continue;
      }
      // serviceType = "org.myrobotlab.service.OpenCV";

      if (skipTest.contains(serviceType.getName())) {
        log.info("skipping %s", serviceType.getName());
        continue;
      }

      try {

        // agent.serviceTest(); // WTF?
        // status.addInfo("perparing clean environment for %s",
        // serviceType);

        // clean environment
        // FIXME - optimize clean

        // SUPER CLEAN - force .repo to clear !!
        // repo.clearRepo();

        // less clean but faster
        // repo.clearLibraries();
        // repo.clearServiceData();

        // comment all out for dirty

        // install Test dependencies
        long installStartTime = System.currentTimeMillis();
        repo.install("org.myrobotlab.service.Test");
        repo.install(serviceType.getName());
        installTime += System.currentTimeMillis() - installStartTime;
        // clean test.json part file

        // spawn a test - attach to cli - test 1 service end to end
        // ,"-invoke", "test","test","org.myrobotlab.service.Clock"
        Process process = spawn(
            new String[] { "-runtimeName", "testEnv", "-service", "test", "Test", "-logLevel", "WARN", "-noEnv", "-invoke", "test", "test", serviceType.getName() });

        process.waitFor();

        // destroy - start again next service
        // wait for partFile report .. test.json
        // NOT NEEDED - foreign process has ended
        byte[] data = FileIO.loadPartFile("test.json", 60000);
        if (data != null) {
          String test = new String(data);
          Status testResult = CodecUtils.fromJson(test, Status.class);
          if (testResult.isError()) {
            ret.add(testResult);
          }
        } else {
          Status.info("could not get results");
        }
        // destroy env
        kill(getId("testEnv"));

      } catch (Exception e) {

        ret.add(Status.error(e));
        continue;
      }
    }

    ret.add(Status.info("installTime", "%d", installTime));

    ret.add(Status.info("installTime %d", installTime));
    ret.add(Status.info("testTimeMs %d", System.currentTimeMillis() - startTime));
    ret.add(Status.info("testTimeMinutes %d", TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - startTime)));
    ret.add(Status.info("endTime %d", System.currentTimeMillis()));

    try {
      FileIO.savePartFile(new File("fullTest.json"), CodecUtils.toJson(ret).getBytes());
    } catch (Exception e) {
      log.error("serviceTest threw", e);
    }

    return ret;
  }

  public Process spawn(String[] args) throws IOException, URISyntaxException, InterruptedException {
    return spawn(null, null, args);
  }

  public String setBranch(String branch) {
    branchRequested = branch;
    return branchRequested;
  }

  static public Map<String, String> setEnv(Map<String, String> env) {
    Platform platform = Platform.getLocalInstance();
    String platformId = platform.getPlatformId();
    if (platform.isLinux()) {
      String ldPath = String.format("'pwd'/libraries/native:'pwd'/libraries/native/%s:${LD_LIBRARY_PATH}", platformId);
      env.put("LD_LIBRARY_PATH", ldPath);
    } else if (platform.isMac()) {
      String dyPath = String.format("'pwd'/libraries/native:'pwd'/libraries/native/%s:${DYLD_LIBRARY_PATH}", platformId);
      env.put("DYLD_LIBRARY_PATH", dyPath);
    } else if (platform.isWindows()) {
      // this just borks the path in Windows - additionally (unlike Linux)
      // - i don't think you need native code on the PATH
      // and Windows does not have a LD_LIBRARY_PATH
      // String path =
      // String.format("PATH=%%CD%%\\libraries\\native;PATH=%%CD%%\\libraries\\native\\%s;%%PATH%%",
      // platformId);
      // env.put("PATH", path);
      // we need to sanitize against a non-ascii username
      // work around for Jython bug in 2.7.0...
      env.put("APPDATA", "%%CD%%");
    } else {
      log.error("unkown operating system");
    }

    return env;
  }

  public void shutdown() {
    // FIXME !!! - "ask" all child processes to kindly Runtime.shutdown via msgs
    // !!
    log.info("terminating others");
    killAll();
    log.info("terminating self ... goodbye...");
    // Runtime.exit();
    Runtime.shutdown();
  }

  public synchronized Process spawn() throws IOException, URISyntaxException, InterruptedException {
    return spawn(null, null, new String[] {});
  }

  public synchronized Process spawn(String branch, String version, String[] in) throws IOException, URISyntaxException, InterruptedException {
    return spawn(getFilePath(branch, version), in);
  }

  public Process spawn(String jarPath, String[] in) throws IOException, URISyntaxException, InterruptedException {

    File jarPathDir = new File(jarPath);

    ProcessData pd = new ProcessData(agent, jarPathDir.getAbsolutePath(), in, branchAgent, versionAgent);

    CmdLine cmdline = new CmdLine(in);
    if (cmdline.hasSwitch("-autoUpdate") || cmdline.hasSwitch("--autoUpdate")) {
      autoUpdate(true);
    }

    log.info("Agent starting spawn {}", formatter.format(new Date()));
    log.info("in args {}", Arrays.toString(in));

    return spawn2(pd);
  }

  /**
   * max complexity spawn
   * 
   * @param pd
   * @return
   * @throws IOException
   */
  public synchronized Process spawn2(ProcessData pd) throws IOException {

    log.info("============== spawn begin ==============");

    String runtimeName = pd.name;

    // this needs cmdLine
    String[] cmdLine = pd.buildCmdLine();
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < cmdLine.length; ++i) {
      sb.append(cmdLine[i]);
      sb.append(" ");
    }

    log.info("spawning -> [{}]", sb.toString());

    ProcessBuilder builder = new ProcessBuilder(cmdLine);
    // handle stderr as a direct pass through to System.err
    builder.redirectErrorStream(true);
    // setting working directory to wherever the jar is...
    String spawnDir = new File(pd.jarPath).getParent();
    builder.directory(new File(spawnDir));

    log.info("in {} spawning -> [{}]", spawnDir, sb.toString());

    // environment variables setup
    setEnv(builder.environment());

    Process process = builder.start();
    pd.process = process;
    pd.startTs = System.currentTimeMillis();
    pd.monitor = new ProcessData.Monitor(pd);
    pd.monitor.start();

    pd.state = ProcessData.STATE_RUNNING;
    if (pd.id == null) {
      log.error("id should not be null!");
    }
    if (processes.containsKey(pd.id)) {
      if (agent != null) {
        agent.info("restarting %s %s", pd.id, pd.name);
      }
    } else {
      if (agent != null) {
        agent.info("starting new %s %s", pd.id, pd.name);
      }
      processes.put(pd.id, pd);
    }

    // FIXME !!! - remove stdin/stdout !!!! use sockets only
    // attach our cli to the latest instance
    // *** interesting - not processing input/output will block the thread
    // in the spawned process ***
    // which I assume is the beginning main thread doing a write to std::out
    // and it blocking before anything else can happen

    log.info("Agent finished spawn {}", formatter.format(new Date()));
    if (agent != null) {
      Cli cli = Runtime.getCli();
      cli.add(runtimeName, process.getInputStream(), process.getOutputStream());
      cli.attach(runtimeName);
      agent.broadcastState();
    }
    return process;
  }

  /**
   * DEPRECATE ? spawn2 should do this checking ?
   * 
   * @param id
   *          i
   * @throws IOException
   *           e
   * @throws URISyntaxException
   *           e
   * @throws InterruptedException
   *           e
   * 
   */
  public void start(String id) throws IOException, URISyntaxException, InterruptedException {
    if (!processes.containsKey(id)) {
      log.error("start process %s can not start - process does not exist", id);
      return;
    }

    ProcessData p = processes.get(id);
    if (p.isRunning()) {
      log.warn("process %s already started", id);
      return;
    }
    spawn2(p);
  }

  public void update() throws IOException {
    Platform platform = Platform.getLocalInstance();
    update(platform.getBranch());
  }

  public void update(String branch) throws IOException {
    log.info("update({})", branch);
    // so we need to get the version of the jar contained in the {branch}
    // directory ..
    FileIO.extract(String.format("%s/myrobotlab.jar", branch), Util.getResourceDir() + "/version.txt", String.format("%s/version.txt", branch));

    String currentVersion = FileIO.toString(String.format("%s/version.txt", branch));
    if (currentVersion == null) {
      log.error("{}/version.txt current version is null", branch);
      return;
    }
    // compare that with the latest http://s3/current/{branch}/version.txt
    // and figure

    String latestVersion = getLatestRemoteVersion(branch);
    if (latestVersion == null) {
      log.error("s3 version.txt current version is null", branch);
      return;
    }

    if (!latestVersion.equals(currentVersion)) {
      log.info("latest %s > current %s - updating", latestVersion, currentVersion);
      downloadLatest(branch);
    }

    // FIXME - restart processes
    // if (updateRestartProcesses) {

    // }

  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(Agent.class.getCanonicalName());
    meta.addDescription("responsible for spawning a MRL process. Agent can also terminate, respawn and control the spawned process");
    meta.addCategory("framework");
    meta.setSponsor("GroG");
    meta.setLicenseApache();

    meta.addDependency("commons-cli", "commons-cli", "1.4");
    meta.includeServiceInOneJar(true);

    return meta;
  }

  public void startService() {
    super.startService();
    // addTask(1000, "scanForMsgs");
  }

  /**
   * First method JVM executes when myrobotlab.jar is in jar form.
   * 
   * -agent "-logLevel DEBUG -service webgui WebGui"
   * 
   * @param args
   *          args
   */
  // FIXME - test when internet is not available
  // FIXME - test over multiple running processes
  // FIXME - add -help
  // TODO - add jvm memory other runtime info
  // FIXME - a way to route parameters from command line to Agent vs Runtime -
  // the current concept is ok - but it does not work ..
  // make it work if necessary prefix everything by -agent-<...>
  // FIXME - replace by PicoCli !!!
  public static void main(String[] args) {
    try {

      Logging logging = LoggingFactory.getInstance();

      // FIXME convert to picocmd or apachecli
      // -agent \"-params -service ... \" string encoded
      cmdline = new CmdLine(args);
      logging.setLevel(cmdline.getSafeArgument("-logLevel", 0, "INFO"));

      log.info("agent cmdline [{}] will be relayed ", cmdline);

      Platform platform = Platform.getLocalInstance();

      if (cmdline.containsKey("--autoUpdate")) {
        autoUpdate = true;
      }

      if (cmdline.containsKey("--branch")) {
        branchRequested = cmdline.getArgument("--branch", 0);
      }

      if (cmdline.containsKey("--version")) {
        versionRequested = cmdline.getArgument("--version", 0);
      }

      // FIXME - have a list versions ... command line !!!

      // FIXME - the most common use case is the version of the spawned instance
      // -
      // if that is the case its needed to determine what is the "proposed"
      // branch & version if no
      // special command parameters were given
      if (cmdline.containsKey("-h") || cmdline.containsKey("--help")) {
        // FIXME - add all possible command descriptions ..
        System.out.println(String.format("%s branch %s version %s", platform.getBranch(), platform.getPlatformId(), platform.getVersion()));
        return;
      }

      // "agent" command line - must be in quotes since the rest of the command
      // line
      // is relayed to the service
      // Start with the default cmdline for the agent
      String[] agentArgs = new String[] { "-isAgent", "-id", String.format("agent.%s.%s", formatter.format(new Date()), Platform.getLocalInstance().getPid()) };
      if (cmdline.containsKey("-agent")) {
        String str = cmdline.getArgument("-agent", 0);
        String[] tmp = str.split(" ");
        agentArgs = new String[tmp.length + 1];
        for (int i = 0; i < agentArgs.length - 1; ++i) {
          agentArgs[i] = tmp[i];
        }
        // -isAgent parameter is REQUIRED for Agent
        agentArgs[agentArgs.length - 1] = "-isAgent";

      }

      agentCmdline = new CmdLine(agentArgs);

      Process p = null;

      log.info("agent args [{}]", agentCmdline);
      Runtime.setLogLevel("WARN");
      // agents runtime
      Runtime.main(agentArgs);
      if (agent == null) {
        agent = (Agent) Runtime.start("agent", "Agent");
      }

      if (cmdline.containsKey("-webadmin") || cmdline.containsKey("--webadmin")) {
        agent.startWebGui();
        // webgui.setAddress("127.0.0.1"); - for security...
      }

      if (cmdline.containsKey("--autoUpdate")) {
        // FIXME - call directly - update if possible - then spawn
        // agent.processUpdates(null, branchRequested, versionRequested,
        // autoUpdate);
        agent.getLatest(branchRequested);
        agent.autoUpdate(true);
      }

      // FIXME - use wsclient for remote access
      if (!cmdline.containsKey("--client")) {
        p = agent.spawn(args); // <-- agent's is now in charge of first
      } else {
        Runtime.start("cli", "Cli");
      }

      // deprecate non-standard -install use --install short-version would be -i
      if (cmdline.containsKey("-install") || cmdline.containsKey("--install")) {
        // wait for mrl instance to finish installing
        // then shutdown (addendum: check if supporting other processes)
        p.waitFor();
        agent.shutdown();
      }

    } catch (Exception e) {
      log.error("unsuccessful spawn", e);
    }
  }
}

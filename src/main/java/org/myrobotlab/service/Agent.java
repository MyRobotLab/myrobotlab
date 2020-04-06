package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.MrlException;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ProcessData;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.io.StreamGobbler;
import org.myrobotlab.lang.NameGenerator;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.net.Http;
import org.myrobotlab.service.Runtime.CmdOptions;
import org.slf4j.Logger;

import picocli.CommandLine;

/**
 * <pre>
 * 
 *         Agent is responsible for managing running instances of myrobotlab. It
 *         can start, stop and update myrobotlab.
 *         
 *         FIXME - remove all "building" - should be a new service Builder .. which can build mrl - Agent should have seperation of concerns
 *         and only deal with "starting" and "running" of built mrl processes
 * 
 * 
 *         FIXME - test switching branches and remaining on the branch for multiple updates
 *         FIXME - tes multiple instances on different branches
 *         FIXME - ws client connectivity and communication !!! 
 *         FIXME - capability to update Agent from child
 *         FIXME - move CmdLine defintion to Runtime 
 *         FIXME - convert Runtime's cmdline processing to CmdOptions Fixme - remove CmdLine
 *         FIXME !!! - remove stdin/stdout !!!! use sockets only
 * 
 *         FIXME - there are at least 3 different levels of updating 
 *          1. a global thread which only "checks" for updates 
 *          2. the possibility of just downloading an update (per instance) 
 *          3. the possibility of auto-restarting after a download is completed (per instance)
 * 
 *         FIXME - auto update log .. sparse log of only updates and their results ...
 *         FIXME - test changing version prefix .. e.g. 1.2.
 *         FIXME - testing test - without version test - remote unaccessable
 *         FIXME - spawn must be synchronized 2 threads (the timer and the user)
 *         FIXME - test naming an instance FIXME - test starting an old version
 *         FIXME - make hidden check latest version interval and make default interval check large 
 *         FIXME - change Runtime's cli !!!
 *         FIXME - check user define services for Agent
 *
 * </pre>
 */
public class Agent extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Agent.class);

  final Map<String, ProcessData> processes = new ConcurrentHashMap<String, ProcessData>();

  transient static SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd.HHmmssSSS");

  Platform platform = Platform.getLocalInstance();

  transient WebGui webgui = null;
  
  int port = 8887;
  
  String address = "127.0.0.1";

  String currentBranch;

  String currentVersion;

  /**
   * auto update - automatically checks for updates and WILL update any running
   * mrl instance automatically
   */
  boolean autoUpdate = false;

  /**
   * autoCheckForUpdate - checks automatically checks for updates after some
   * interval but does not automatically update - it publishes events of new
   * availability of updates but does not update
   */
  boolean autoCheckForUpdate = false;

  Set<String> possibleVersions = new TreeSet<String>();

  final static String REMOTE_BUILDS_URL_HOME = "http://build.myrobotlab.org:8080/job/myrobotlab-multibranch/";

  // for more info -
  // myrobotlab-multibranch/job/develop/api/json
  // WARNING Jenkins url api format for multi-branch pipelines is different from
  // maven builds !
  final static String REMOTE_BUILDS_URL = "job/%s/api/json?tree=builds[number,status,timestamp,id,result]";

  final static String REMOTE_JAR_URL = "job/%s/%s/artifact/target/myrobotlab.jar";

  final static String REMOTE_MULTI_BRANCH_JOBS = "api/json";

  boolean checkRemoteVersions = false;

  /**
   * command line options for the agent
   */
  static CmdOptions globalOptions;

  String versionPrefix = "1.1.";

  static String banner = "   _____         __________      ___.           __  .____          ___.    \n"
      + "  /     \\ ___.__.\\______   \\ ____\\_ |__   _____/  |_|    |   _____ \\_ |__  \n"
      + " /  \\ /  <   |  | |       _//  _ \\| __ \\ /  _ \\   __\\    |   \\__  \\ | __ \\ \n"
      + "/    Y    \\___  | |    |   (  <_> ) \\_\\ (  <_> )  | |    |___ / __ \\| \\_\\ \\\n" + "\\____|__  / ____| |____|_  /\\____/|___  /\\____/|__| |_______ (____  /___  /\n"
      + "        \\/\\/             \\/           \\/                    \\/    \\/    \\/ \n            " + Platform.getLocalInstance().getMotd();

  /**
   * singleton for security purposes
   */
  transient static Agent agent;

  /**
   * development variable to force version "unknown" to be either greatest or
   * smallest version for development
   */
  private boolean unknownIsGreatest = false;

  public static class WorkflowMultiBranchProject {
    String name;
    WorkflowJob[] jobs;
  }

  /**
   * Jenkins data structure to describe jobs
   */
  public static class WorkflowJob {
    String name;
    String url;
    String color;
    WorkflowRun lastSuccessfulBuild;
    WorkflowRun[] builds;
  }

  /**
   * Jenkins data structure to describe builds
   */
  public static class WorkflowRun {
    String id;
    Integer number;
    String result;
    Long timestamp;
  }

  // FIXME - change this to hour for production ...
  // long updateCheckIntervalMs = 60 * 60 * 1000; // every hour
  long updateCheckIntervalMs = 60 * 1000; // every minute

  List<Status> updateLog = new ArrayList<>();

  /**
   * Update thread - we cannot use addTask as a long update could pile up a
   * large set of updates to process quickly in series. Instead, we have a
   * simple single class which is always single threaded to process updates.
   *
   */
  class Updater implements Runnable {

    transient Agent agent = null;
    transient Thread thread = null;
    ProcessData.stateType state = ProcessData.stateType.stopped;

    public Updater(Agent agent) {
      this.agent = agent;
    }

    @Override
    public void run() {
      state = ProcessData.stateType.running;
      updateLog("info", "updater running");
      autoUpdate = true;
      broadcastState();
      try {
        while (true) {
          state = ProcessData.stateType.sleeping;
          updateLog("info", "updater sleeping");
          Thread.sleep(updateCheckIntervalMs);
          state = ProcessData.stateType.updating;
          updateLog("info", "updater updating");
          agent.update();
        }
      } catch (Exception e) {
        log.error("updater threw", e);
      }
      log.info("updater stopping");
      updateLog("info", "updater stopping");
      autoUpdate = false;
      state = ProcessData.stateType.stopped;
      broadcastState();
    }

    synchronized public void start() {
      if (state == ProcessData.stateType.stopped) {
        thread = new Thread(this, getName() + ".updater");
        thread.start();
        updateLog("info", "updater starting");
      } else {
        log.warn("updater busy in state = {}", state);
      }
    }

    synchronized public void stop() {
      if (state != ProcessData.stateType.stopped) {
        // we'll wait if its in the middle of an update
        while (state == ProcessData.stateType.updating) {
          log.warn("updater currently updating, waiting for 5 seconds...");
          sleep(5000);
        }
        // most likely the thread is a sleeping state
        // so, we wake it up quickly to die ;)
        thread.interrupt();
      }
    }

  }

  public static String VERSION_ROOT = "./";

  Updater updater;

  public Agent(String n, String id) throws IOException {
    super(n, id);
    updater = new Updater(this);
    currentBranch = Platform.getLocalInstance().getBranch();
    currentVersion = Platform.getLocalInstance().getVersion();

    log.info("Agent {} Pid {} is alive", n, Platform.getLocalInstance().getPid());

    // basic setup - minimally we make a directory
    // and instance folder of the same branch & version as the
    // agent jar
    setup();

    // user has decided to look for updates ..
    if (autoUpdate || checkRemoteVersions) {
      invoke("getVersions", currentBranch);
    }
    
    Runtime runtime = Runtime.getInstance();
    runtime.startInteractiveMode();
  }

  public String getDir(String branch, String version) {
    return System.getProperty("user.dir");
  }

  public String getJarName(String branch, String version) {
	// FIXME !!! branch name is completely unreliable :(
	// version info which includes build number can distinguish for what we need
	  return "myrobotlab-" + version + ".jar";
    // return "myrobotlab-" + branch + "-" + version + ".jar";
    // return getDir(branch, version) + File.separator + "myrobotlab.jar";
  }

  private void setup() throws IOException {
    Platform platform = Platform.getLocalInstance();
    String agentBranch = platform.getBranch();
    String agentVersion = platform.getVersion();

    // location of the agent's branch (and version)
    String agentVersionPath = getDir(agentBranch, agentVersion);

    if (!new File(agentVersionPath).exists()) {
      File branchDir = new File(agentVersionPath);
      branchDir.mkdirs();
    }

    String agentMyRobotLabJar = getJarName(agentBranch, agentVersion);
    if (!new File(agentMyRobotLabJar).exists()) {
      log.info("agent versioned {} does not exist", agentMyRobotLabJar);
      String agentJar = new java.io.File(Agent.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
      if (!agentJar.endsWith(".jar")) {
        String cwd = System.getProperty("user.dir");
        log.info("agent is not in a jar - i suspect your using and ide - path is [{}]", agentJar);
        log.info("i will build a jar for you on src from {}", cwd);
        // guessing the src root is in cwd
        // globalOptions.src
        // FIXME - what do we do on failure of compile ?
        String newVersion = mvn(cwd, agentBranch, null);
        if (newVersion != null) {
          log.info("built version {}", newVersion);
        } else {
          log.error("could not build");
          return;
        }
        log.info("agents expects its own version {} to be available version - copying from {}", agentVersion, newVersion);
        String newJar = getJarName(agentBranch, newVersion);
        String newJarLoc = getJarName(agentBranch, agentVersion);
        log.info("copy {} to {}", newJar, newJarLoc);
        Files.copy(Paths.get(newJar), Paths.get(newJarLoc), StandardCopyOption.REPLACE_EXISTING);
      } else {
        log.info("new directory or new version perhaps - we'll copy ./myrobotlab.jar to {}", agentJar);
        Files.copy(Paths.get("myrobotlab.jar"), Paths.get(agentMyRobotLabJar), StandardCopyOption.REPLACE_EXISTING);
      }
    }
  }

  public void startWebGui() {
    startWebGui(null);
  }

  public void startWebGui(String addressPort) {
    if (addressPort == null) {
      startWebGui(null, null);
    }

    Integer port = null;
    String address = null;

    try {
      port = Integer.parseInt(addressPort);
    } catch (Exception e) {
    }

    try {
      if (addressPort.contains(":")) {
        String[] anp = addressPort.split(":");
        port = Integer.parseInt(anp[1]);
        addressPort = anp[0];
      }
      InetAddress ip = InetAddress.getByName(addressPort);
      address = ip.getHostAddress();
    } catch (Exception e2) {
    }

    startWebGui(address, port);
  }

  public void startWebGui(String address, Integer port) {
    try {

      if (webgui == null) {
        if (address != null) {
          this.address = address;
        }
        if (port != null) {
          this.port = port;
        } else {
          port = 8887;
        }
        webgui = (WebGui) Runtime.create("webgui", "WebGui");
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
      // addTask("update", 1000 * 60, 0, "update");
      updater.start();
    } else {
      // purgeTask("update");
      updater.stop();
    }
  }

  /**
   * FIXME !!! - i believe in task for these pipe up !!! NOT GOOD _ must have
   * its own thread then !!
   * 
   * called by the autoUpdate task which is scheduled every minute to look for
   * updates from the build server
   */
  public void update() {
    for (String key : processes.keySet()) {
      ProcessData process = processes.get(key);
      if (!process.options.autoUpdate) {
        log.info("not autoUpdate");
        continue;
      }
      try {

        // FIXME - if options.src != null GITHUB
        if (globalOptions.src != null) {
          log.info("checking for github updates on branch {}", process.options.branch);
          String newVersion = getLatestSrc(process.options.branch);
          if (newVersion != null && process.isRunning()) {
            warn("updating process [%s] from %s -to-> %s", process.options.id, process.options.version, newVersion);
            // FIXME set currentVersion ???
            currentVersion = newVersion;
            process.options.version = newVersion;
            process.jarPath = new File(getJarName(process.options.branch, process.options.version)).getAbsolutePath();
            restart(process.options.id);
            log.info("restarted");
          }
        } else {
          log.info("checking for updates on jenkins");
          // getRemoteVersions
          log.info("getting version");
          String newVersion = getLatestVersion(process.options.branch, true);
          if (newVersion == null || newVersion.equals(process.options.version)) {
            log.info("same version {}", newVersion);
            continue;
          }

          // we have a possible update
          log.info("WOOHOO ! updating to version {}", newVersion);
          process.options.version = newVersion;
          process.jarPath = new File(getJarName(process.options.branch, process.options.version)).getAbsolutePath();

          getLatestJar(process.options.branch);

          log.info("WOOHOO ! updated !");
          if (process.isRunning()) {
            log.info("its running - we should restart");
            restart(process.options.id);
            log.info("restarted");
          }
          warn("updating process [%s] from %s -to-> %s", process.options.id, process.options.version, newVersion);
        }
      } catch (TransportException e) {
        log.info("cannot connect to - are we connected to the internet ? {}", e.getMessage());
      } catch (Exception e) {
        log.error("proccessing updates from scheduled task threw", e);
      }
    }
  }

  /**
   * gets the latest jar if allowed to check remote ....
   * 
   * @param branch
   */
  public void getLatestJar(String branch) {
    try {
      // check for latest
      String version = getLatestVersion(branch, true);

      // check if branch and version exist locally
      if (!existsLocally(branch, version)) {
        log.info("found update - getting new jar {} {}", branch, version);
        getJar(branch, version);
        // download latest to the appropriate directory
        // mkdirs
        // download file
        if (!verifyJar(branch, version)) {
        }
        log.info("successfully downloaded {} {}", branch, version);
      }
    } catch (Exception e) {
      error(e);
    }
  }

  // FIXME - implement :)
  private boolean verifyJar(String branch, String version) {
    return true;
  }

  synchronized public void getJar(String branch, String version) {
    new File(getDir(branch, version)).mkdirs();
    String build = getBuildId(version);
    // this
    Http.getSafePartFile(String.format(REMOTE_BUILDS_URL_HOME + REMOTE_JAR_URL, branch, build), getJarName(branch, version));
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
    return new File(getJarName(branch, version)).exists();
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
    kill(id); // FIXME - kill should include prepare to shutdown ...
    sleep(2000);
    spawnId(id);
  }

  /**
   * For respawning an existing ProcessData object
   * 
   * @param id
   */
  public void spawnId(String id) {
    try {
      if (processes.containsKey(id)) {
        spawn(processes.get(id));
      } else {
        log.error("agent does not know about process id {}", id);
      }
    } catch (Exception e) {
      log.error("spawn({}) threw ", id, e);
    }
  }

  /**
   * Will spawn a new process of mrl using defaults.
   * 
   * @return
   * @throws IOException
   * @throws URISyntaxException
   * @throws InterruptedException
   * @throws IllegalAccessException
   * @throws IllegalArgumentException
   */
  public Process spawn() throws IOException, URISyntaxException, InterruptedException, IllegalArgumentException, IllegalAccessException {
    CmdOptions options = new CmdOptions();
    new CommandLine(options).parseArgs(new String[] {});
    return spawn(options);
  }

  /**
   * Spawn a process give a argument command line in single string form
   * 
   * @param args
   * @return
   * @throws IOException
   * @throws URISyntaxException
   * @throws InterruptedException
   * @throws IllegalAccessException
   * @throws IllegalArgumentException
   */
  public Process spawn(String args) throws IOException, URISyntaxException, InterruptedException, IllegalArgumentException, IllegalAccessException {
    CmdOptions options = new CmdOptions();
    new CommandLine(options).parseArgs(args.split(" "));
    return spawn(options);
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
      pd2.options.id = id + ".0";
    }

    processes.put(pd2.options.id, pd2);
    if (agent != null) {
      agent.broadcastState();
    }
    return pd2;
  }

  /**
   * Copies an existing ProcessData, makes a new id and spawns it.
   * 
   * @param id
   * @throws IOException
   */
  public void copyAndStart(String id) throws IOException {
    // returns a non running copy with new process id
    // on the processes list
    ProcessData pd2 = copy(id);
    spawn(pd2);
    if (agent != null) {
      agent.broadcastState();
    }
  }

  /**
   * get the current branches being built in a Jenkins multi-branch pipeline job
   * 
   * @return
   */
  static public Set<String> getBranches() {
    Set<String> possibleBranches = new TreeSet<String>();
    try {
      byte[] r = Http.get(REMOTE_BUILDS_URL_HOME + REMOTE_MULTI_BRANCH_JOBS);
      if (r != null) {
        String json = new String(r);
        WorkflowMultiBranchProject project = (WorkflowMultiBranchProject) CodecUtils.fromJson(json, WorkflowMultiBranchProject.class);
        for (WorkflowJob job : project.jobs) {
          possibleBranches.add(job.name);
        }
      }
    } catch (Exception e) {
      log.error("getRemoteBranches threw", e);
    }
    return possibleBranches;
  }

  /**
   * Used to compare semantic versions
   * 
   * @param version1
   * @param version2
   * @return
   * @throws MrlException
   */
  boolean isGreaterThan(String version1, String version2) throws MrlException {
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
    Set<String> versions = new TreeSet<String>();
    versions.addAll(getLocalVersions(branch));
    if (allowRemote) {
      versions.addAll(getRemoteVersions(branch));
    }
    if (versions.size() != possibleVersions.size()) {
      possibleVersions = versions;
      broadcastState();
    }
    return versions;
  }

  /**
   * Get remote versions from jenkins
   * 
   * @param branch
   * @return
   */
  public Set<String> getRemoteVersions(String branch) {
    Set<String> versions = new TreeSet<String>();
    try {

      byte[] data = Http.get(String.format(REMOTE_BUILDS_URL_HOME + REMOTE_BUILDS_URL, branch));
      if (data != null) {
        String json = new String(data);
        WorkflowJob job = (WorkflowJob) CodecUtils.fromJson(json, WorkflowJob.class);
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

  /**
   * Checks in the branches directory for the latest version of desired "branch"
   * 
   * @param branch
   * @return
   * @throws MrlException
   */
  public String getLatestLocalVersion(String branch) throws MrlException {
    Set<String> allLocal = getLocalVersions(branch);
    String latest = null;
    for (String version : allLocal) {
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
   * A version to be unique is both {branch}-{version}. This finds all currently
   * available versions.
   * 
   * @return
   */
  public Set<String> getLocalVersions() {
    Set<String> versions = new TreeSet<>();
    // get local file system versions
    File branchDir = new File(VERSION_ROOT);
    // get local existing versions
    File[] listOfFiles = branchDir.listFiles();
    for (int i = 0; i < listOfFiles.length; ++i) {
      File file = listOfFiles[i];

      if (file.getName().startsWith("myrobotlab-") && file.getName().endsWith(".jar")) {
        String version = file.getName().substring(("myrobotlab-").length() + 1, file.getName().length() - ".jar".length());
        log.info("found {} branch-version {}", file.getName(), version);
        versions.add(version);
      }
    }
    return versions;
  }

  /**
   * Get the local versions available for the selected branch.
   * 
   * @param branch
   * @return
   */
  public Set<String> getLocalVersions(String branch) {
    Set<String> versions = new TreeSet<>();
    // get local file system versions
    File branchDir = new File(VERSION_ROOT);
    // get local existing versions
    File[] listOfFiles = branchDir.listFiles();
    for (int i = 0; i < listOfFiles.length; ++i) {
      File file = listOfFiles[i];

      if (file.getName().startsWith("myrobotlab-" + branch) && file.getName().endsWith(".jar")) {
        String version = file.getName().substring(("myrobotlab-" + branch).length() + 1, file.getName().length() - ".jar".length());
        log.info("found {} on branch {} with version {}", file.getName(), branch, version);
        versions.add(version);
      }
    }
    return versions;
  }

  /**
   * get a list of all the processes currently governed by this Agent
   * 
   * @return hash map, int to process data
   */
  public Map<String, ProcessData> getProcesses() {
    return processes;
  }

  /**
   * Kills requested process.
   * 
   * @param id
   * @return
   */
  public String kill(String id) {
    // FIXME !!! - "ask" all child processes to kindly Runtime.shutdown via msgs
    // !!
    if (processes.containsKey(id)) {
      if (agent != null) {
        agent.info("terminating %s", id);
      }
      ProcessData process = processes.get(id);
      process.process.destroy();
      process.state = ProcessData.stateType.stopped;

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
    }

    error("kill unknown process id {}", id);
    return null;
  }

  /**
   * kill all processes
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

  /**
   * kills and clears
   * 
   * @param id
   */
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
      String id = (String) objs[i];
      ProcessData p = processes.get(id);
      pd[i] = String.format("%s - %s [%s - %s]", id, p.options.id, p.options.branch, p.options.version);
    }
    return pd;
  }

  /**
   * Publishing point when a process dies.
   * 
   * @param id
   * @return
   */
  public String publishTerminated(String id) {
    log.info("publishTerminated - terminated {} - restarting", id);

    if (!processes.containsKey(id)) {
      log.error("processes {} not found");
      return id;
    }

    // if you don't fork with Agent allowed to
    // exist without instances - then
    if (!globalOptions.fork) {
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
   * Max complexity spawn - with all possible options - this will create a
   * ProcessData object and send it to spawn. ProcessData contains all the
   * unique data related to starting an instance.
   * 
   * Convert command line parameter options into a ProcessData which can be
   * spawned
   * 
   * @param inOptions
   *          - cmd options
   * @return a process
   * @throws IOException
   * @throws URISyntaxException
   * @throws InterruptedException
   * @throws IllegalAccessException
   * @throws IllegalArgumentException
   */
  public Process spawn(CmdOptions inOptions) throws IOException, URISyntaxException, InterruptedException, IllegalArgumentException, IllegalAccessException {
    if (ProcessData.agent == null) {
      ProcessData.agent = this;
    }
    // create a ProcessData then spawn it !
    ProcessData pd = new ProcessData();
    pd.options = new CmdOptions(inOptions);
    CmdOptions options = pd.options;

    if (options.id == null) {
      options.id = NameGenerator.getName();
    }

    if (options.branch == null) {
      options.branch = Platform.getLocalInstance().getBranch();
    }

    if (options.version == null) {
      try {
        options.version = getLatestVersion(options.branch, autoUpdate);
      } catch (Exception e) {
        log.error("getDir threw", e);
      }
    }

    pd.jarPath = new File(getJarName(options.branch, options.version)).getAbsolutePath();

    // javaExe
    String fs = File.separator;
    Platform platform = Platform.getLocalInstance();
    String exeName = platform.isWindows() ? "javaw" : "java";
    pd.javaExe = String.format("%s%sbin%s%s", System.getProperty("java.home"), fs, fs, exeName);

    String jvmArgs = String.format("-Djava.library.path=%s/native -Djna.library.path=%s/native -Dfile.encoding=UTF-8", pd.options.libraries, pd.options.libraries);
    if (platform.isWindows()) {
      jvmArgs = jvmArgs.replace("/", "\\");
    }
    
    if (pd.options.proxy != null) {
      URI uri = new URI(pd.options.proxy); 
      String host = uri.getHost();
      Integer port = uri.getPort();
      jvmArgs += " -Dhttp.proxyHost=" + host + " -Dhttp.proxyPort=" + port + " " + "-Dhttps.proxyHost=" + host + " -Dhttps.proxyPort=" + port;
    }

    if (pd.options.memory != null) {
      jvmArgs += String.format(" -Xms%s -Xmx%s ", pd.options.memory, pd.options.memory);
    }
    pd.jvm = jvmArgs.split(" ");
    
    pd.options.fromAgent = true;

    // user override
    if (options.jvm != null) {
      pd.jvm = options.jvm.split(" ");
    }

    if (options.services.size() == 0) {
      options.services.add("log");
      options.services.add("Log");
      options.services.add("webgui");
      options.services.add("WebGui");      
      options.services.add("intro");
      options.services.add("Intro");      
      options.services.add("gui");
      options.services.add("SwingGui");
      options.services.add("python");
      options.services.add("Python");
    }

    return spawn(pd);
  }

  public String setBranch(String branch) {
    currentBranch = branch;
    return currentBranch;
  }

  static public Map<String, String> setEnv(ProcessData pd, Map<String, String> env) {
    Platform platform = Platform.getLocalInstance();
    String platformId = platform.getPlatformId();
    if (platform.isLinux()) {
      File f = new File(pd.options.libraries);
      String ldPath = String.format("%s/native:%s/native/%s:${LD_LIBRARY_PATH}", pd.options.libraries, pd.options.libraries, platformId);
      // String ldPath =
      // String.format("'pwd'/libraries/native:'pwd'/libraries/native/%s:${LD_LIBRARY_PATH}",
      // platformId);
      env.put("LD_LIBRARY_PATH", ldPath);
    } else if (platform.isMac()) {
      // String dyPath =
      // String.format("'pwd'/libraries/native:'pwd'/libraries/native/%s:${DYLD_LIBRARY_PATH}",
      // platformId);
      String dyPath = String.format("%s/native:%s/native/%s:${DYLD_LIBRARY_PATH}", pd.options.libraries, pd.options.libraries, platformId);
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

  /**
   * Kills all connected processes, then shuts down itself. FIXME - should send
   * shutdown to other processes instead of killing them
   */
  public void shutdown() {
    log.info("terminating others");
    killAll();
    log.info("terminating self ... goodbye...");
    Runtime.shutdown();
  }

  /**
   * Constructs a command line from a ProcessData object which can directly be
   * run to spawn a new instance of mrl
   * 
   * FIXME is this ProcessData.toString()
   *
   * @param pd
   * @return
   */
  public String[] buildCmdLine(ProcessData pd) {

    // command line to be returned
    List<String> cmd = new ArrayList<String>();

    cmd.add(pd.javaExe);

    if (pd.jvm != null) {
      for (int i = 0; i < pd.jvm.length; ++i) {
        cmd.add(pd.jvm[i]);
      }
    }

    cmd.add("-cp");

    // step 1 - get current env data
    Platform platform = Platform.getLocalInstance();
    // File f = new File(pd.options.libraries);
    // String libraries = f.getAbsolutePath();
    // String cpTemplate = "%s%s%s/jar/*%s./bin%s./build/classes";

    String ps = File.pathSeparator;
    String fs = File.separator;

    // order of cp paths - higher precedence first
    // develop "could do" (".."+fs+".."+fs+"build"+fs+"classes") .. but we
    // already have --src and a whole framework to build
    // the latest
    String classpath = (pd.jarPath) + ps + (pd.options.libraries + fs + "jar" + fs + "*");
    // String classpath = String.format(cpTemplate, pd.jarPath, ps, libraries,
    // ps, libraries, ps, ps);
    if (platform.isWindows()) {
      classpath = classpath.replace("/", "\\");
    }
    cmd.add(classpath);

    cmd.add("org.myrobotlab.service.Runtime");

    if (pd.options.services.size() > 0) {
      if (pd.options.services.size()%2 != 0) {
        error("--service requires {name} {Type} {name} {Type} even number of entries - you have %d", pd.options.services.size());
        Runtime.shutdown();
      }
      cmd.add("--service");
      for (int i = 0; i < pd.options.services.size(); i += 2) {
        cmd.add(pd.options.services.get(i));
        cmd.add(pd.options.services.get(i + 1));
      }
    }

    // FIXME !!!! - this less than ideal !
    // "MOST" of the flags are "relayed" through so CmdOptions are handled in
    // the spawned process - but a few are handled by the agent and should not
    // be
    // relayed
    cmd.add("--id");
    cmd.add(pd.options.id);

    cmd.add("--data-dir");
    cmd.add(pd.options.dataDir);

    if (globalOptions.logLevel != null) {
      cmd.add("--log-level");
      cmd.add(globalOptions.logLevel);
    }

    // FIXME - shouldn't 'everything' simply be relayed on that doesn't
    // directly affect Agent?
    if (pd.options.install != null) {
      cmd.add("--install");
      for (String serviceType : globalOptions.install) {
        cmd.add(serviceType);
      }
    }
    
    if (pd.options.installDependency != null) {
        cmd.add("--install-dependency");
        for (String serviceType : globalOptions.installDependency) {
        cmd.add(serviceType);
      }
    }

    // FIXME - adding new CmdOption
    if (pd.options.cfg != null) {
      cmd.add("-c");
      cmd.add(globalOptions.cfg);
    }

    if (pd.options.addKeys != null) {
      cmd.add("-k");
      for (String keyPart : globalOptions.addKeys) {
        cmd.add(keyPart);
      }
    }

    if (pd.options.invoke != null) {
      cmd.add("--invoke");
      for (String keyPart : globalOptions.invoke) {
        cmd.add(keyPart);
      }
    }

    // cmd.add("--libraries");
    // cmd.add(pd.options.libraries);

    if (pd.options.virtual) {
      cmd.add("--virtual");
    }
    
    cmd.add("--spawned-from-agent");

    return cmd.toArray(new String[cmd.size()]);
  }

  /**
   * The final spawn - all other data types as parameters make a ProcesData
   * which is used by this method to start the process.
   * 
   * @param pd
   * @return
   * @throws IOException
   */
  public synchronized Process spawn(ProcessData pd) throws IOException {

    log.info("============== spawn begin ==============");

    // this needs cmdLine
    String[] cmdLine = buildCmdLine(pd);

    ProcessBuilder builder = new ProcessBuilder(cmdLine);
    // handle stderr as a direct pass through to System.err
    builder.redirectErrorStream(true);
    // setting working directory to wherever the jar is...
    String spawnDir = new File(pd.jarPath).getParent();
    builder.directory(new File(spawnDir));

    log.info("cd {}", spawnDir);
    StringBuilder spawning = new StringBuilder();
    for (String c : cmdLine) {
      spawning.append(c);
      spawning.append(" ");
    }
    log.info("SPAWNING ! --> [{}]", spawning);

    // environment variables setup
    setEnv(pd, builder.environment());

// new    
//    builder.inheritIO();
    
    Process process = builder.start();
    pd.process = process;
    pd.startTs = System.currentTimeMillis();
    pd.stdout = new StreamGobbler(pd.options.id, process.getInputStream());
    pd.stdout.start();
    pd.monitor = new ProcessData.Monitor(pd);
    pd.monitor.start();

    pd.state = ProcessData.stateType.running;

    if (pd.options.id == null) {
      log.error("id cannot be null!");
    }
    if (processes.containsKey(pd.options.id)) {
      if (agent != null) {
        agent.info("restarting %s", pd.options.id);
      }
    } else {
      if (agent != null) {
        agent.info("starting new %s", pd.options.id);
      }
      processes.put(pd.options.id, pd);
    }

    log.info("Agent finished spawn {}", formatter.format(new Date()));
    if (agent != null) {
      /** FIXME - integrate with cli api
      Cli cli = Runtime.getCli();
      cli.add(pd.options.id, process.getInputStream(), process.getOutputStream());
      cli.attach(pd.options.id);
      agent.broadcastState();
      */
    }
    return process;
  }

  /**
   * DEPRECATE ? spawn should do this checking ?
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
    spawn(p);
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

    // meta.includeServiceInOneJar(true);

    return meta;
  }

  /**
   * First method JVM executes when myrobotlab.jar is in jar form.
   * 
   * --agent "--logLevel DEBUG --service webgui WebGui"
   * 
   * @param args
   *          args
   */
  public static void main(String[] args) {
    try {

      globalOptions = new CmdOptions();

      new CommandLine(globalOptions).parseArgs(args);

      if (globalOptions.help) {
        Runtime.mainHelp();
        return;
      }

      List<String> agentArgs = new ArrayList<>();

      if (globalOptions.agent != null) {
        agentArgs.addAll(Arrays.asList(globalOptions.agent.split(" ")));
      } else {
        agentArgs.add("--id");
        agentArgs.add("agent-" + NameGenerator.getName());
        agentArgs.add("-s");
        agentArgs.add("agent");
        agentArgs.add("Agent");
        agentArgs.add("security");
        agentArgs.add("Security");

        agentArgs.add("--log-level");
        agentArgs.add(globalOptions.logLevel);

        // agentArgs.add("webgui"); FIXME - soon .. but not yet ...
        // agentArgs.add("WebGui");
      }

      Process p = null;

      if (!globalOptions.noBanner) {
        System.out.println(banner);
        System.out.println("");
      }

      log.info("user  args {}", Arrays.toString(args));
      log.info("agent args {}", Arrays.toString(agentArgs.toArray()));

      Runtime.main(agentArgs.toArray(new String[agentArgs.size()]));
      agent = (Agent) Runtime.getService("agent");

      if (globalOptions.listVersions) {
        System.out.println("available local versions");
        for (String bv : agent.getLocalVersions()) {
          System.out.println(bv);
        }
        agent.shutdown();
      }

      if ("".equals(globalOptions.version)) {
        Map<String, String> manifest = Platform.getManifest();
        System.out.println("manifest");
        for (String name : manifest.keySet()) {
          System.out.println(String.format("%s=%s", name, manifest.get(name)));
        }
        agent.shutdown();
      }

      Platform platform = Platform.getLocalInstance();

      if (globalOptions.branch == null) {
        globalOptions.branch = platform.getBranch();
      }

      if (globalOptions.version == null) {
        globalOptions.version = platform.getVersion();
      }

      agent.setBranch(globalOptions.branch);
      agent.setVersion(globalOptions.version);

      // FIXME - have a list versions ... command line !!!

      // FIXME - the most common use case is the version of the spawned instance
      // -
      // if that is the case its needed to determine what is the "proposed"
      // branch & version if no
      // special command parameters were given
      // FIXME HELP !!!! :D
      // if (cmdline.containsKey("-h") || cmdline.containsKey("--help")) {
      // // FIXME - add all possible command descriptions ..
      // System.out.println(String.format("%s branch %s version %s",
      // platform.getBranch(), platform.getPlatformId(),
      // platform.getVersion()));
      // return;
      // }

      if (globalOptions.webgui != null) {
        agent.startWebGui(globalOptions.webgui);
      }

      // the user set auto-update to true
      if (globalOptions.autoUpdate) {
        // options.fork = true;
        // lets check and get the latest jar if there is new one

        try {
          if (globalOptions.src == null) {
            // get the latest from Jenkins
            agent.getLatestJar(agent.getBranch());
          } else {
            // get the latest from GitHub
            agent.getLatestSrc(agent.getBranch());
          }
        } catch (TransportException e) {
          log.info("could not get latest myrobotlab - {}", e.getMessage());
        } catch (Exception e) {
          log.error("trying to update failed", e);
        }

        // the "latest" should have been downloaded
        globalOptions.version = agent.getLatestLocalVersion(agent.getBranch());
        log.info("will attempt to spawn the latest local version [{}-{}]", agent.getBranch(), globalOptions.version);
      }

      // FIXME - use wsclient for remote access
      if (globalOptions.client != null) {
        
        return;
      }

      // TODO - build command line ...
      // FIXME - if another instances is spawned agent should wait for all
      // instances to stop
      // list of flags we want to by-pass spawning
      if (globalOptions.fork && globalOptions.services.size() > 0 || !globalOptions.fork) {
        p = agent.spawn(globalOptions); // <-- agent's is now in charge of first
      }

      // we start a timer to process future updates
      if (globalOptions.autoUpdate) {
        // if you update a single process
        // it kills it and restarts - and this will kill the
        // agent unless its forked
        globalOptions.fork = true;
        agent.autoUpdate(true);
      }

      if (globalOptions.install != null) {
        // wait for mrl instance to finish installing
        // then shutdown (addendum: check if supporting other processes)
        p.waitFor();
        agent.shutdown();
        // START AGAIN IF --install WAS SUPPLIED
        if (globalOptions.autoUpdate) {
          globalOptions.install = null;
          p = agent.spawn(globalOptions);
        }
      }

    } catch (Exception e) {
      log.error("agent main exception", e);
    }
  }

  public String getLatestSrc(String branch) throws WrongRepositoryStateException, InvalidConfigurationException, DetachedHeadException, InvalidRemoteException, CanceledException,
      RefNotFoundException, NoHeadException, TransportException, IOException, GitAPIException {

    Agent agent = (Agent) Runtime.getService("agent");

    RevCommit latestCommit = agent.gitPull(branch);
    if (latestCommit != null) {
      log.info("latest {} - will attempt to build", latestCommit);
      String version = agent.mvn(null, branch, (long) latestCommit.getCommitTime());
      log.info("successfully build version {} - {}", latestCommit.getCommitTime(), latestCommit.getFullMessage());
      return version;
    }
    return null;
  }

  public String getBranch() {
    return currentBranch;
  }

  public String getVersion() {
    return currentVersion;
  }

  public String setVersion(String version) {
    currentVersion = version;
    return version;
  }

  public String mvn(String branch) {
    return mvn(null, branch, null);
  }

  public String mvn(String src, String branch, Long buildNumber) {
    try {

      if (src == null) {
        src = "data" + File.separator + branch + ".src";
      }
      String fs = File.separator;
      File myroborlabJar = new File(src + fs + "target" + fs + "myrobotlab");
      if (myroborlabJar.exists()) {
        myroborlabJar.delete();
      }
      File snapshot = new File(src + fs + "target" + fs + "mrl-0.0.1-SNAPSHOT.jar");
      if (snapshot.exists()) {
        snapshot.delete();
      }

      if (buildNumber == null) {
        // epoch minute build time number
        buildNumber = System.currentTimeMillis() / 1000;
      }

      String version = versionPrefix + buildNumber;

      Platform platform = Platform.getLocalInstance();
      List<String> cmd = new ArrayList<>();

      cmd.add((platform.isWindows()) ? "cmd" : "/bin/bash");
      cmd.add((platform.isWindows()) ? "/c" : "-c");

      // when you send a command to be interpreted by cmd or bash - you get more
      // consistent results
      // when you wrap the command in quotes - that's why we use a StringBuilder
      StringBuilder sb = new StringBuilder();
      sb.append((platform.isWindows()) ? "mvn" : "mvn"); // huh .. thought it
                                                         // was
      sb.append(" "); // mvn.bat
      sb.append("-DskipTests");
      sb.append(" ");
      sb.append("-Dbuild.number=" + buildNumber);
      sb.append(" ");
      sb.append("-DGitBranch=" + branch);
      sb.append(" ");
      sb.append("compile");
      sb.append(" ");
      sb.append("prepare-package");
      sb.append(" ");
      sb.append("package");
      sb.append(" ");
      // cmd.add("-f");
      // cmd.add(pathToPom);
      // cmd.add("-o"); // offline
      sb.append("-o"); // offline

      // cmd.add("\"" + sb.toString() + "\"");
      cmd.add(sb.toString());

      StringBuilder sb1 = new StringBuilder();
      for (String c : cmd) {
        sb1.append(c);
        sb1.append(" ");
      }

      // src path ..
      log.info("build [{}]", sb1);
      // ProcessBuilder pb = new
      // ProcessBuilder("mvn","exec:java","-Dexec.mainClass="+"FunnyClass");
      ProcessBuilder pb = new ProcessBuilder(cmd);
      Map<String, String> envs = pb.environment();
      log.info("PATH={}", envs.get("PATH"));

      pb.directory(new File(src));

      // handle stderr as a direct pass through to System.err
      pb.redirectErrorStream(true);
      // pb.environment().putAll(System.getenv());

      pb.inheritIO().start().waitFor();

      // FIXME LOOK FOR --> "BUILD FAILURE"

      String newJar = src + File.separator + "target" + File.separator + "myrobotlab.jar";
      String newJarLoc = getJarName(branch, version);
      File p = new File(newJarLoc).getAbsoluteFile().getParentFile();
      p.mkdirs();

      Files.copy(Paths.get(newJar), Paths.get(newJarLoc), StandardCopyOption.REPLACE_EXISTING);

      return versionPrefix + buildNumber + "";
    } catch (Exception e) {
      log.error("mvn threw ", e);
    }
    return null;
  }

  public RevCommit gitPull(String branch) throws WrongRepositoryStateException, InvalidConfigurationException, DetachedHeadException, InvalidRemoteException, CanceledException,
      RefNotFoundException, NoHeadException, TransportException, IOException, GitAPIException {
    return gitPull(null, branch);
  }

  public RevCommit gitPull(String src, String branch) throws IOException, WrongRepositoryStateException, InvalidConfigurationException, DetachedHeadException,
      InvalidRemoteException, CanceledException, RefNotFoundException, NoHeadException, TransportException, GitAPIException {

    if (branch == null) {
      branch = currentBranch;
    }

    if (src == null) {
      src = getRootDataDir() + File.separator + branch + ".src";
    }

    List<String> branches = new ArrayList<String>();
    branches.add("refs/heads/" + branch);

    File repoParentFolder = new File(src);

    Git git = null;

    TextProgressMonitor textmonitor = new TextProgressMonitor(new PrintWriter(System.out));

    Repository repo = null;
    if (!repoParentFolder.exists()) {
      // String branch = "master";
      git = Git.cloneRepository().setProgressMonitor(textmonitor).setURI("https://github.com/MyRobotLab/myrobotlab.git").setDirectory(new File(src)).setBranchesToClone(branches)
          .setBranch("refs/heads/" + branch).call();

    } else {
      // Open an existing repository
      String gitDir = repoParentFolder.getAbsolutePath() + "/.git";
      repo = new FileRepositoryBuilder().setGitDir(new File(gitDir)).build();
      git = new Git(repo);
    }

    repo = git.getRepository();

    /**
     * <pre>
     * CheckoutCommand checkout = git.checkout().setCreateBranch(true).setName(branch).setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).setStartPoint("origin/" + branch)
     *     .call();
     * </pre>
     */

    // git.pull().setCredentialsProvider(user).call();
    // FIXME if currentBranch != branch - then checkout .. set current branch
    if (!branch.equals(currentBranch)) {
      git.branchCreate().setForce(true).setName(branch).setStartPoint("origin/" + branch).call();
      git.checkout().setName(branch).call();
    }

    // FIXME - if auto-update or auto-fetch ie .. remote allowed and cache
    // remote changes
    git.fetch().setProgressMonitor(new TextProgressMonitor(new PrintWriter(System.out))).call();

    List<RevCommit> localLogs = getLogs(git, "origin/" + branch, 1);
    List<RevCommit> remoteLogs = getLogs(git, "remotes/origin/" + branch, 1);

    RevCommit localCommit = localLogs.get(0);
    RevCommit remoteCommit = remoteLogs.get(0);

    BranchTrackingStatus status = BranchTrackingStatus.of(repo, branch);

    // if (localCommit.getCommitTime() < remoteCommit.getCommitTime()) {
    if (status.getBehindCount() > 0) {
      log.info("local ts {}, remote {} - {} updating", localCommit.getCommitTime(), remoteCommit.getCommitTime(), remoteCommit.getFullMessage());
      PullCommand pullCmd = git.pull();
      pullCmd.setProgressMonitor(textmonitor);
      pullCmd.call();
      return remoteCommit;
    } else {
      log.info("no new commits on branch {}", branch);
    }

    return null;
  }

  private List<RevCommit> getLogs(Git git, String ref, int maxCount)
      throws RevisionSyntaxException, NoHeadException, MissingObjectException, IncorrectObjectTypeException, AmbiguousObjectException, GitAPIException, IOException {
    List<RevCommit> ret = new ArrayList<>();
    Repository repository = git.getRepository();
    Iterable<RevCommit> logs = git.log().setMaxCount(maxCount).add(repository.resolve(ref)).call();
    for (RevCommit rev : logs) {
      ret.add(rev);
    }
    return ret;
  }

  // FIXME - move to enums for status level !
  public void updateLog(String level, String msg) {
    if (updateLog.size() > 100) {
      updateLog.remove(updateLog.size() - 1);
    }
    if ("info".equals(level)) {
      updateLog.add(Status.info((new Date()).toString() + " " + msg));
    } else if ("error".equals(level)) {
      updateLog.add(Status.error((new Date()).toString() + " " + msg));
    }
  }
}

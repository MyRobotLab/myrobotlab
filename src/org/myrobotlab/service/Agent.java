package org.myrobotlab.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.cmdline.CmdLine;
import org.myrobotlab.codec.CodecJson;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ProcessData;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.repo.GitHub;
import org.myrobotlab.framework.repo.Repo;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.net.Http;
import org.slf4j.Logger;

import com.google.gson.internal.LinkedTreeMap;

/**
 * @author GroG
 *
 *         Agent Smith is responsible for all Processes Just as Runtime is
 *         responsible for all Services Agent starts and prepares all processes
 *         for running MRL This includes environment variables, jvm arguments
 *         and cmdline arguments
 * 
 *         Agent can not use any JNI JNA or typically other Services with
 *         dependencies as the expectation is this remain a thin wrapper which
 *         controls other / more complex processes.
 * 
 *         java -jar myrobotlab.jar will start the one (and probably only) Agent
 *         process
 * 
 *         Although Runtime primarily processes command line parameters there
 *         are a few specific for Process directives
 * 
 * 
 *         Since -test needs a specialized cleaned environment its handled here
 * 
 *         Several modes exist - normal = set env and keep process in map, with
 *         re-directed stdin stdout & stderr streams envOnly = set the correct
 *         environment then terminate
 * 
 * 
 *         default is start a new process with relayed cmdline and redirect
 *         stdin stout & stderr streams, terminate if no subprocesses exist
 * 
 *         =================================================================== *
 *         References :
 *
 *         http://www.excelsior-usa.com/articles/java-to-exe.html
 *
 *         possible small wrappers mac / linux / windows
 *         http://mypomodoro.googlecode
 *         .com/svn-history/r89/trunk/src/main/java/org
 *         /mypomodoro/util/Restart.java
 *
 *         http://java.dzone.com/articles/programmatically-restart-java
 *         http://stackoverflow
 *         .com/questions/3468987/executing-another-application-from-java
 *
 *
 *         TODO - ARMV 6 7 8 ??? -
 *         http://www.binarytides.com/linux-cpu-information/ - lscpu
 *
 *         Architecture: armv7l Byte Order: Little Endian CPU(s): 4 On-line
 *         CPU(s) list: 0-3 Thread(s) per core: 1 Core(s) per socket: 1
 *         Socket(s): 4
 *
 *
 *         TODO - soft floating point vs hard floating point readelf -A
 *         /proc/self/exe | grep Tag_ABI_VFP_args soft = nothing hard =
 *         Tag_ABI_VFP_args: VFP registers
 *
 *         PACKAGING jsmooth - windows only javafx - 1.76u - more dependencies ?
 *         http://stackoverflow.com/questions/1967549/java-packaging-tools-
 *         alternatives-for-jsmooth-launch4j-onejar
 *
 *         TODO classpath order - for quick bleeding edge updates? rsync
 *         exploded classpath
 *
 *         TODO - check for Java 1.7 or > addShutdownHook check for network
 *         connectivity TODO - proxy -Dhttp.proxyHost=webproxy
 *         -Dhttp.proxyPort=80 -Dhttps.proxyHost=webproxy -Dhttps.proxyPort=80
 *         -Dhttp.proxyUserName="myusername" -Dhttp.proxyPassword="mypassword"
 * 
 *         TODO? how to get vm args http:*
 *         stackoverflow.com/questions/1490869/how-to-get
 *         -vm-arguments-from-inside-of-java-application http:*
 *         java.dzone.com/articles/programmatically-restart-java http:*
 *         stackoverflow.com
 *         /questions/9911686/getresource-some-jar-returns-null-although
 *         -some-jar-exists-in-geturls RuntimeMXBean runtimeMxBean =
 *         ManagementFactory.getRuntimeMXBean(); List<String> arguments =
 *         runtimeMxBean.getInputArguments();
 * 
 *         TODO - on java -jar myrobotlab.jar | make a copy if agent.jar does
 *         not exist.. if it does then spawn the Agent there ... it would make
 *         upgrading myrobotlab.jar "trivial" !!!
 * 
 *         TO TEST - -agent "-test -logLevel WARN"
 */
public class Agent extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Agent.class);

  HashSet<String> dependencies = new HashSet<String>();

  HashMap<Integer, ProcessData> processes = new HashMap<Integer, ProcessData>();

  List<String> agentJVMArgs = new ArrayList<String>();
  transient SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd:HH:mm:ss");

  // proxy info
  // CloseableHttpClient httpclient =
  // HttpClients.custom().useSystemProperties().build();
  // java -Dhttp.proxyHost=webproxy -Dhttp.proxyPort=8000
  String proxyHost = null;
  Integer proxyPort = null;

  String currentBranch = null;
  String currentVersion = null;

  Platform platform = Platform.getLocalInstance();

  String agentBranch = platform.getBranch();
  String agentVersion = platform.getVersion();

  // FIXME - all update functionality will need to be moved to Runtime
  // it should take parameters such that it will be possible at some point to
  // do an update
  // from a child process & update the agent :)

  HashSet<String> possibleBranches = new HashSet<String>();
  HashSet<String> possibleVersions = new HashSet<String>();

  // String lastBranch = null;
  // WebGui webAdmin = null; can't have a peer untile nettosphere is part of
  // base build
  // boolean updateRestartProcesses = false;

  String updateUrl = "http://mrl-bucket-01.s3.amazonaws.com/current/%s";
  String jarUrlTemplate = "http://mrl-bucket-01.s3.amazonaws.com/current/%s/myrobotlab.jar";
  String versionUrlTemplate = "http://mrl-bucket-01.s3.amazonaws.com/current/%s/version.txt";
  String versionsListUrlTemplate = "http://mrl-bucket-01.s3.amazonaws.com/";

  boolean checkRemoteBranchesOnStartup = true;

  String latestRemote;

  public Agent(String n) {
    super(n);
    log.info("Agent {} PID {} is alive", n, Runtime.getPid());
    agentJVMArgs = Runtime.getJVMArgs();
    if (currentBranch == null) {
      currentBranch = platform.getBranch();
    }
    if (currentVersion == null) {
      currentVersion = platform.getVersion();
    }

    // add my branch
    possibleBranches.add(agentBranch);

    if (checkRemoteBranchesOnStartup) {
      // TODO - turn into asynchronous call
      // so no connection will not lead to an irritating 'wait' timeout
      getRemoteBranches();
    }
    setBranch(currentBranch);
  }

  // revert ! only 1 global autoUpdate - all processes - not Agent (yet)
  public void autoUpdate(boolean b) {

    String name = String.format("%s.timer.processUpdates", getName());

    if (b) {
      addTask(name, 1000 * 60, "processUpdates");
    } else {
      purgeTask(name);
    }
  }

  public void startWebGui() {
    try {
      // no reference at all to WebGui
      // look ma no reference !
      Runtime.create("webAdmin", "WebGui");
      send("webAdmin", "setPort", 8887);
      Runtime.start("webAdmin", "WebGui");

    } catch (Exception e) {
      Logging.logError(e);
    }

  }

  /**
   * checks the current branch looks if the verstion.txt has been changed
   * 
   * @throws IOException
   */
  synchronized public void processUpdates() throws IOException {

    String remoteVersion = getLatestRemoteVersion(currentBranch);
    if (remoteVersion == null) {
      error("checkForUpdates %s is null", currentBranch);
    } else {
      log.info("found remote version {}", remoteVersion);
      File checkIfWeHaveJar = new File(String.format("%s/myrobotlab.%s.jar", currentBranch, remoteVersion));
      if (!checkIfWeHaveJar.exists()) {
        log.info("downloading remote version {}", remoteVersion);
        downloadLatest(currentBranch);
      }

      for (Integer key : processes.keySet()) {
        ProcessData process = processes.get(key);
        if (!currentBranch.equals(process.branch)) {
          log.info("skipping update of {} because its on branch {}", process.id, process.branch);
          continue;
        }

        if (remoteVersion.equals(process.version)) {
          log.info("skipping update of {} {} because its already version {}", process.id, process.name, process.version);
          continue;
        }

        // FIXME - it would be nice to send a SIG_TERM to
        // the process before we kill the jvm
        // process.process.getOutputStream().write("/Runtime/releaseAll".getBytes());
        process.version = remoteVersion;
        if (process.isRunning()) {
          restart(process.id);
        }
      }
    }
  }

  public synchronized void restart(Integer id) throws IOException {
    kill(id);
    spawn2(processes.get(id));
  }

  /**
   * return a non-running process structure from an existing one with a new id
   * 
   * @param id
   * @return
   */
  public ProcessData copy(Integer id) {
    if (!processes.containsKey(id)) {
      error("cannot copy %d does not exist", id);
      return null;
    }
    ProcessData pd = processes.get(id);
    ProcessData pd2 = new ProcessData(pd);
    pd2.startTs = null;
    pd2.stopTs = null;
    pd2.id = getNextProcessId();
    processes.put(pd2.id, pd2);
    broadcastState();
    return pd2;
  }

  public void copyAndStart(Integer id) throws IOException {
    // returns a non running copy with new process id
    // on the processes list
    ProcessData pd2 = copy(id);
    spawn2(pd2);
    broadcastState();
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

  public String formatList(ArrayList<String> args) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < args.size(); ++i) {
      log.info(args.get(i));
      sb.append(String.format("%s ", args.get(i)));
    }
    return sb.toString();
  }

  /**
   * gets id from name
   * 
   * @param name
   * @return
   */
  public Integer getId(String name) {
    for (Integer pid : processes.keySet()) {
      if (pid.equals(name)) {
        return processes.get(pid).id;
      }
    }
    return null;
  }

  // FIXME - should just be be saveRemoteJar() - but shouldn't be from
  // multiple threads
  public byte[] getLatestRemoteJar(String branch) {
    return Http.get(String.format(jarUrlTemplate, branch));
  }

  public String getLatestRemoteVersion(String branch) {
    byte[] data = Http.get(String.format(versionUrlTemplate, branch));
    if (data != null) {
      return new String(data);
    }
    return null;
  }

  /*
   * public ArrayList<String> getLocalVersions(String branch) throws IOException
   * { File m = new File(String.format("%s/myrobotlab.jar", branch)); String
   * checkVersion = String.format("./checkVersion.%d.txt",
   * System.currentTimeMillis()); //FileIO.extract(m.getAbsolutePath(),
   * "resource/version.txt", checkVersion); byte[] v = FileIO.toByteArray(new
   * File(checkVersion)); File cv = new File(checkVersion); if (!cv.delete()) {
   * log.warn("could not delete {}", m.getAbsolutePath()); }
   * 
   * String version = null; if (v == null) { error(
   * "failed attempt of checking version for %s", m.getAbsolutePath()); } else {
   * version = new String(v);
   * 
   * info("found local version %s", version); }
   * 
   * return null; }
   */

  /**
   * gets name from id
   * 
   * @param id
   * @return
   */
  public String getName(Integer id) {
    for (Integer pid : processes.keySet()) {
      if (pid.equals(id)) {
        return processes.get(pid).name;
      }
    }

    return null;
  }

  synchronized public Integer getNextProcessId() {
    Integer ret = 0;
    for (int i = 0; i < processes.size(); ++i) {
      if (!processes.containsKey(ret)) {
        return ret;
      }
      ret += 1;
    }
    return ret;
  }

  public HashSet<String> getRemoteBranches() {
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
          LinkedTreeMap m = (LinkedTreeMap) array[i];
          if (m.containsKey("name")) {
            possibleBranches.add(m.get("name").toString());
          }
        }
      }
    } catch (Exception e) {
      Logging.logError(e);
    }
    return possibleBranches;
  }

  synchronized public HashSet<String> getPossibleVersions() {
    // only in the context of the currentBranch

    // clear versions
    possibleVersions.clear();

    // get my version if i'm on same branch
    if (agentBranch.equals(currentBranch)) {
      possibleVersions.add(agentVersion);
    }

    // get local versions
    File branchFolder = new File(currentBranch);
    if (!branchFolder.isDirectory()) {
      error("%s not a directory", currentBranch);
    } else {
      File[] listOfFiles = branchFolder.listFiles();
      for (int i = 0; i < listOfFiles.length; ++i) {
        File file = listOfFiles[i];
        if (!file.isDirectory()) {
          if (file.getName().startsWith("myrobotlab.")) {
            String version = getFileVersion(file.getName());
            if (version != null) {
              possibleVersions.add(version);
            }
          }
        }
      }
    }

    // TODO !!! make asynchronous promise !!!!
    String remote = getLatestRemoteVersion(currentBranch);
    if (remote != null) {
      if (!possibleVersions.contains(remote)) {
        possibleVersions.add(remote);
        invoke("newVersionAvailable", remote);
        latestRemote = remote;
      }
    }
    return possibleVersions;
  }

  public String newVersionAvailable() {
    return latestRemote;
  }

  public String getFileVersion(String name) {
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
   * @return
   */
  public HashMap<Integer, ProcessData> getProcesses() {
    return processes;
  }

  /*
   * - REMOVE only Runtime should install public List<Status> install(String
   * fullType) { List<Status> ret = new ArrayList<Status>();
   * ret.add(Status.info("install %s", fullType)); try { Repo repo =
   * Repo.getLocalInstance();
   * 
   * if (!repo.isServiceTypeInstalled(fullType)) { repo.install(fullType); if
   * (repo.hasErrors()) { ret.addAll(repo.getErrors()); }
   * 
   * } else { log.info("installed {}", fullType); } } catch (Exception e) {
   * ret.add(Status.error(e)); } return ret; }
   */

  public Integer kill(Integer id) {
    if (processes.containsKey(id)) {
      info("terminating %s", id);
      ProcessData process = processes.get(id);
      process.process.destroy();
      process.state = ProcessData.STATE_STOPPED;

      if (process.monitor != null) {
        process.monitor.interrupt();
        process.monitor = null;
      }
      // remove(processes.get(name));
      info("%s haz beeen terminated", id);
      broadcastState();
      return id;
    }

    warn("%s? no sir, I don't know that punk...", id);
    return null;
  }

  /*
   * BAD IDEA - data type ambiguity is a drag public Integer kill(String name) {
   * return kill(getId(name)); }
   */

  public void killAll() {
    for (Integer id : processes.keySet()) {
      kill(id);
    }
    log.info("no survivors sir...");
    broadcastState();
  }

  public void killAndRemove(Integer id) {
    if (processes.containsKey(id)) {
      kill(id);
      processes.remove(id);
      broadcastState();
    }
  }

  /**
   * list processes
   */
  public String[] lp() {
    Object[] objs = processes.keySet().toArray();
    String[] pd = new String[objs.length];
    for (int i = 0; i < objs.length; ++i) {
      Integer id = (Integer) objs[i];
      ProcessData p = processes.get(id);
      pd[i] = String.format("%d - %s [%s - %s]", id, p.name, p.branch, p.version);
    }
    return pd;
  }

  public Integer publishTerminated(Integer id) {
    info("terminated %d %s", id, getName(id));
    broadcastState();
    return id;
  }

  /**
   * This is a great idea & test - because we want complete control over
   * environment and dependencies - the ability to purge completely - and start
   * from the beginning - but it should be in another service and not part of
   * the Agent. The 'Test' service could use Agent as a peer
   * 
   * @return
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
    ArrayList<ServiceType> serviceTypes = serviceData.getServiceTypes();

    ret.add(info("serviceTest will test %d services", serviceTypes.size()));
    long startTime = System.currentTimeMillis();
    ret.add(info("startTime", "%d", startTime));

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
          info("could not get results");
        }
        // destroy env
        kill(getId("testEnv"));

      } catch (Exception e) {

        ret.add(Status.error(e));
        continue;
      }
    }

    ret.add(info("installTime", "%d", installTime));

    ret.add(info("installTime %d", installTime));
    ret.add(info("testTimeMs %d", System.currentTimeMillis() - startTime));
    ret.add(info("testTimeMinutes %d", TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - startTime)));
    ret.add(info("endTime %d", System.currentTimeMillis()));

    try {
      FileIO.savePartFile(new File("fullTest.json"), CodecUtils.toJson(ret).getBytes());
    } catch (Exception e) {
      Logging.logError(e);
    }

    return ret;
  }

  public String setBranch(String branch) {
    currentBranch = branch;
    getPossibleVersions();
    return currentBranch;
  }

  public Map<String, String> setEnv(Map<String, String> env) {
    Platform platform = Platform.getLocalInstance();
    String platformId = platform.getPlatformId();
    if (platform.isLinux()) {
      String ldPath = String.format("'pwd'/libraries/native:'pwd'/libraries/native/%s:${LD_LIBRARY_PATH}", platformId);
      env.put("LD_LIBRARY_PATH", ldPath);
    } else if (platform.isMac()) {
      String dyPath = String.format("'pwd'/libraries/native:'pwd'/libraries/native/%s:${DYLD_LIBRARY_PATH}", platformId);
      env.put("DYLD_LIBRARY_PATH", dyPath);
    } else if (platform.isWindows()) {
      String path = String.format("PATH=%%CD%%\\libraries\\native;PATH=%%CD%%\\libraries\\native\\%s;%%PATH%%", platformId);
      env.put("PATH", path);
      // we need to sanitize against a non-ascii username
      // work around for Jython bug in 2.7.0...
      env.put("APPDATA", "%%CD%%");
    } else {
      log.error("unkown operating system");
    }

    return env;
  }

  public void shutdown() {
    log.info("terminating others");
    killAll();
    log.info("terminating self ... goodbye...");
    Runtime.exit();
  }

  // FIXME - spawn the current version
  public Process spawn() throws IOException, URISyntaxException, InterruptedException {
    return spawn(new String[] { "-runtimeName", "runtime" }); // FIXME - do
    // latest
    // version
  }

  /**
   * create new "named" MRL instance
   * 
   * @param name
   *          - unique runtime name
   * @throws InterruptedException
   * @throws URISyntaxException
   * @throws IOException
   */
  public Process spawn(String name) throws IOException, URISyntaxException, InterruptedException {
    return spawn(new String[] { "-runtimeName", name });
  }

  /**
   * Responsibility - This method will always call Runtime. To start Runtime
   * correctly environment must correctly be setup
   */
  public synchronized Process spawn(String[] in) throws IOException, URISyntaxException, InterruptedException {

    ProcessData pd = new ProcessData(this, in, currentBranch, currentVersion);

    CmdLine cmdline = new CmdLine(in);
    if (cmdline.hasSwitch("-autoUpdate")) {
      autoUpdate(true);
    }

    log.info(String.format("Agent starting spawn %s", formatter.format(new Date())));
    log.info("in args {}", Arrays.toString(in));

    // String jvmMemory = "-Xmx2048m -Xms256m";
    long totalMemory = Runtime.getTotalPhysicalMemory();
    if (totalMemory == 0) {
      log.info("could not get total physical memory");
    } else {
      log.info("total physical memory returned is {} Mb", totalMemory / 1048576);
    }

    // need to fill it out as best you can before submitting to spawn2
    return spawn2(pd);
  }

  public synchronized Process spawn2(ProcessData pd) throws IOException {

    log.info("============== spawn begin ==============");

    String runtimeName = pd.name; // cmdline.getSafeArgument("-runtimeName",
    // 0, "runtime");
    // String branch = pd.branch;// = cmdline.getSafeArgument("-branch", 0,
    // Platform.getLocalInstance().getBranch());
    // String version = pd.version;
    // currentBranch = (pd.branch != null) ? pd.branch : currentBranch;
    // currentVersion = (pd.version != null) ? pd.version : currentVersion;

    // this needs cmdLine
    String[] cmdLine = pd.buildCmdLine();
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < cmdLine.length; ++i) {
      sb.append(cmdLine[i]);
      sb.append(" ");
    }

    File b = new File(pd.branch);
    b.mkdirs();

    log.info(String.format("in %s spawning -> [%s]", b.getAbsolutePath(), sb.toString()));
    ProcessBuilder builder = new ProcessBuilder(cmdLine);// .inheritIO();

    // check to see if myrobotlab.jar is in the directory
    String filename = String.format("%s/myrobotlab.%s.jar", pd.branch, pd.version);
    File m = new File(filename);

    // if I'm a jar and target jar does not exist
    if (!m.exists()) {
      if (FileIO.isJar()) {
        log.info(String.format("cloning self to %s", filename));
        FileIO.copy(new File("myrobotlab.jar"), new File(filename));
      } else {
        log.info("I am not a jar - must be develop time");
        log.info(String.format("copying last build to %s", filename));
        File recentlyBuilt = new File("build/lib/myrobotlab.jar");
        if (!recentlyBuilt.exists()) {
          log.error("umm .. I need to start a jar - would you mind building one with build.xml");
          log.error("perhaps in the future I can change all the classpaths etc to start an instances with the bin classes - but no time to do that now");
          log.error("adios... hope we meet again...");
          System.exit(-1);
        }
        FileIO.copy(new File("build/lib/myrobotlab.jar"), new File(filename));
      }
    }

    // move process to start in that directory
    builder.directory(b);

    // environment variables setup
    setEnv(builder.environment());

    // kill pd.process if not null ?

    Process process = builder.start();
    // ProcessData pd = new ProcessData(this, branch,
    // getLocalVersion(branch), runtimeName, outArgs, process);
    pd.process = process;
    pd.startTs = System.currentTimeMillis();
    // FIXME - break out inner class defintion
    /*
     * if (pd.monitor == null) { pd.monitor = new ProcessData.Monitor(pd); }
     * pd.monitor.start();
     */

    pd.monitor = new ProcessData.Monitor(pd);
    pd.monitor.start();

    pd.state = ProcessData.STATE_RUNNING;
    if (pd.id == null) {
      pd.id = getNextProcessId();
    }
    if (processes.containsKey(pd.id)) {
      info("restarting %d %s", pd.id, pd.name);
    } else {
      info("starting new %d %s", pd.id, pd.name);
      processes.put(pd.id, pd);
    }

    // attach our cli to the latest instance
    // *** interesting - not processing input/output will block the thread
    // in the spawned process ***
    // which I assume is the beginning main thread doing a write to std::out
    // and it blocking before anything else can happen
    Cli cli = Runtime.getCli();
    cli.add(runtimeName, process.getInputStream(), process.getOutputStream());
    cli.attach(runtimeName);

    log.info("Agent finished spawn {}", formatter.format(new Date()));
    broadcastState();
    return process;

  }

  /*
   * public void autoUpdate(String name, boolean b){ autoUpdate(getId(name), b);
   * }
   */

  /**
   * DEPRECATE ? spawn2 should do this checking ?
   * 
   * @param id
   * @throws IOException
   * @throws URISyntaxException
   * @throws InterruptedException
   */
  public void start(Integer id) throws IOException, URISyntaxException, InterruptedException {
    if (!processes.containsKey(id)) {
      error("start process %s can not start - process does not exist", id);
      return;
    }

    ProcessData p = processes.get(id);
    if (p.isRunning()) {
      warn("process %s already started", id);
      return;
    }

    spawn2(p);
    // spawn(p.cmdLine.toArray(new String[p.cmdLine.size()]));
  }

  public void terminateSelfOnly() {
    log.info("goodbye .. cruel world");
    System.exit(0);
  }

  public void update() throws IOException {
    Platform platform = Platform.getLocalInstance();
    update(platform.getBranch());
  }

  public void update(String branch) throws IOException {
    info("update({})", branch);
    // so we need to get the version of the jar contained in the {branch}
    // directory ..
    FileIO.extract(String.format("%s/myrobotlab.jar", branch), "resource/version.txt", String.format("%s/version.txt", branch));

    String currentVersion = FileIO.toString(String.format("%s/version.txt", branch));
    if (currentVersion == null) {
      error("{}/version.txt current version is null", branch);
      return;
    }
    // compare that with the latest http://s3/current/{branch}/version.txt
    // and figure

    String latestVersion = getLatestRemoteVersion(branch);
    if (latestVersion == null) {
      error("s3 version.txt current version is null", branch);
      return;
    }

    if (!latestVersion.equals(currentVersion)) {
      info("latest %s > current %s - updating", latestVersion, currentVersion);
      downloadLatest(branch);
    }

    // FIXME - restart processes
    // if (updateRestartProcesses) {

    // }

  }

  /**
   * First method JVM executes when myrobotlab.jar is in jar form.
   * 
   * -agent "-logLevel DEBUG -service webgui WebGui"
   * 
   * @param args
   */
  // FIXME - add -help
  // TODO - add jvm memory other runtime info
  // FIXME - a way to route parameters from command line to Agent vs Runtime -
  // the current concept is ok - but it does not work ..
  // make it work if necessary prefix everything by -agent-<...>
  public static void main(String[] args) {
    try {
      System.out.println("Agent.main starting");

      // FIXME - I think the basic idea is to have
      // parameters route to Agent or to the target instance
      // initially I was thinking of having all agent parameters
      // in a -agent \"-param1 value1 -param2 value2\" -services gui GUI
      // .. instance params
      // but that didn't work due to the parsing of CmdLine ...
      // need a good solution

      // split agent commands from runtime commands
      // String[] agentArgs = new String[0];
      ArrayList<String> inArgs = new ArrayList<String>();
      // -agent \"-params -service ... \" string encoded
      CmdLine runtimeArgs = new CmdLine(args);
      // -service for Runtime -process a b c d :)
      if (runtimeArgs.containsKey("-agent")) {
        // List<String> list = runtimeArgs.getArgumentList("-agent");

        String tmp = runtimeArgs.getArgument("-agent", 0);
        String[] agentPassedArgs = tmp.split(" ");
        if (agentPassedArgs.length > 1) {
          for (int i = 0; i < agentPassedArgs.length; ++i) {
            inArgs.add(agentPassedArgs[i]);
          }
        } else {
          if (tmp != null) {
            inArgs.add(tmp);
          }
        }
        /*
         * agentArgs = new String[list.size()]; for (int i = 0; i < list.size();
         * ++i){ agentArgs[i] = String.format("-%s",list.get(i)); }
         */
      }

      // default args passed to runtime from Agent
      inArgs.add("-isAgent");

      String[] agentArgs = inArgs.toArray(new String[inArgs.size()]);
      // CmdLine agentCmd = new CmdLine(agentArgs);

      // FIXME -isAgent identifier sent -- default to setting log name to
      // agent.log !!!
      Runtime.setRuntimeName("bootstrap");
      Runtime.main(agentArgs);
      Agent agent = (Agent) Runtime.start("agent", "Agent");

      // FIXME - if "-install" - then install a version ?? minecraft way ?
      if (!runtimeArgs.containsKey("-headless") && !runtimeArgs.containsKey("-install")) {
        // FIXME - NOT READY FOR PRIMETIME
        // THIS IS THE AGENT - it does not require special parameters to
        // start
        // the expectation is there is a simple set of params for the
        // "target" instance
        // WHICH MEANS ALL DEPENDENCIES HAVE TO BE PACKAGED WITH
        // MYROBOTLAB.JAR
        // "AND" - the fact that dependencies are bundled means they
        // should be removed from the service classMeta !!!!
        // agent.startWebGui();
      }

      Process p = null;

      if (runtimeArgs.containsKey("-test")) {
        agent.serviceTest();

      } else {
        p = agent.spawn(args); // <-- agent's is now in charge of first
        // mrl
        // instance
      }

      // handle things which are supposed to terminate
      // after completion
      if (runtimeArgs.containsKey("-install")) {
        p.waitFor();
        agent.shutdown();
      }

    } catch (Exception e) {
      e.printStackTrace(System.out);
    } finally {
      // big hammer
      System.out.println("Agent.main leaving");
      // System.exit(0);
    }
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
    meta.addDescription("Agent - responsible for creating the environment and maintaining, tracking and terminating all processes");
    meta.addCategory("framework");
    meta.setSponsor("GroG");
    // meta.addPeer("webadmin", "WebGui", "webgui for the Agent");
    return meta;
  }

}

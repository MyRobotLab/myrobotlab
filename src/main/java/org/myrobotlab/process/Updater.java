package org.myrobotlab.process;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.MrlException;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.Http;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(mixinStandardHelpOptions = true, version = "Update utilities - 1.0")
public class Updater implements Runnable, FilenameFilter {

  public final static Logger log = LoggerFactory.getLogger(Updater.class);

  public static String VERSION_ROOT = "./";

  final static String REMOTE_BUILDS_URL_HOME = "http://build.myrobotlab.org:8080/job/myrobotlab-multibranch/";

  String versionPrefix = "1.1.";
  protected boolean isRunning = false;
  protected long interval = 5000;

  @Option(names = { "-b", "--branch" }, arity = "0..1", description = "branch - default develop")
  protected String branch = "develop";

  // requested version ???
  @Option(names = { "-r", "--requested-version" }, arity = "0..1", description = "version - if not explicitly set assume latest")
  protected String version = null;

  @Option(names = { "--current-version" }, arity = "0..1", description = "current version - currently installed version")
  protected String currentVersion = null;

  @Option(names = { "-t", "--type" }, arity = "0..1", description = "type - needs arity |jenkins|local source|github release")
  protected String type = null;

  @Option(names = { "--auto-cache" }, arity = "0..1", description = "automatically cache update")
  protected boolean autoCache = true;

  @Option(names = { "-a", "--auto-update" }, arity = "0..1", description = "automatically apply update")
  protected boolean autoUpdate = true;

  final public static String MULTI_BRANCH_LATEST_BUILD_URL = "http://build.myrobotlab.org:8080/job/myrobotlab/job/%s/lastSuccessfulBuild/artifact/target/myrobotlab.jar";

  // for more info -
  // myrobotlab-multibranch/job/develop/api/json
  // WARNING Jenkins url api format for multi-branch pipelines is different from
  // maven builds !
  final static String REMOTE_BUILDS_URL = "job/%s/api/json?tree=builds[number,status,timestamp,id,result]";

  final static String REMOTE_JAR_URL = "job/%s/%s/artifact/target/myrobotlab.jar";

  final static String REMOTE_MULTI_BRANCH_JOBS = "api/json";

  Set<UpdateListener> listeners = new HashSet<>();
  
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
  
  /**
   * singleton use getUpdater()
   */
  private Updater() {    
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

  public void addUpdateListener(UpdateListener listener) {
    listeners.add(listener);
  }

  public void removeUpdateListener(UpdateListener listener) {
    listeners.remove(listener);
  }

  public void clearUpdateListeners() {
    listeners.clear();
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
    // if (versions.size() != possibleVersions.size()) {
    // possibleVersions = versions;
    // broadcastState();
    // }
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
      log.error("getRemoteVersions threw", e);
    }
    return versions;
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

  public String getLatestVersion(String branch, Boolean allowRemote) throws MrlException {
    Set<String> versions = getVersions(branch, allowRemote);
    return getLatestVersion(versions);
  }

  private static Updater updater = null;
  private transient Thread worker = null;

  public synchronized static Updater getUpdater() {
    return getUpdater(null);
  }

  public synchronized static Updater getUpdater(String[] args) {
    if (updater == null) {
      updater = new Updater();
      new CommandLine(updater).parseArgs(args);
    }
    return updater;
  }

  public synchronized static void start() {

    if (updater.worker == null) {
      updater.worker = new Thread(updater, "updater");
      updater.worker.start();
    }
  }

  public synchronized static void stop() {
    if (updater.worker != null) {
      updater.isRunning = false;
    }
  }

  protected static final String MULTI_BRANCH_VERSION = "http://build.myrobotlab.org:8080/job/myrobotlab/job/%s/api/xml?xpath=/*/lastStableBuild/number";

  public String getRemoteVersion(String branch) throws UnsupportedEncodingException {
    byte[] v = Http.get(String.format(MULTI_BRANCH_VERSION, branch));
    if (v != null) {
      String jenkinsLatest = new String(v, "UTF-8");
      int p0 = jenkinsLatest.indexOf("<number>");
      int p1 = jenkinsLatest.indexOf("</number>", p0);
      if (p0 != -1 & p1 != -1) {
        return versionPrefix + jenkinsLatest.substring("<number>".length(), jenkinsLatest.length() - "</number>".length());
      }
    }
    return null;
  }


  public void copyToRepo(String srcFile, String branch, String version) throws IOException {      
      File check = new File(String.format("repo/myrobotlab-%s-%s.jar", branch, version));
      if (!check.exists()) {
        FileIO.copy(srcFile, "repo/myrobotlab-%s-%s.jar");
      }
  }
  
  public void writeBytesFromRepo(String src, String dst) throws IOException {
    FileIO.copyBytes(src, dst);
  }

  @Override
  public void run() {
    isRunning = true;
    log.info("starting updater {} {} with interval {} s", branch, currentVersion, interval / 1000);
    while (isRunning) {
      try {
        log.info("checking for update");

        // remote or local

        // if remote get binary put in repo

        File repo = new File("repo");
        if (!repo.exists()) {
          repo.mkdir();
        }

        // get local or remote latest version
        // compare with current version
        // do callbacks if we have an update

        // get version 3 different potential sources jenkins/local(built)/github
        // releases
        String remoteVersion = getRemoteVersion(branch);
        String repoFile = String.format("repo/myrobotlab-%s-%s.jar", branch, remoteVersion);

        if (isGreaterThan(remoteVersion, currentVersion) && autoCache && !(new File(repoFile).exists())) {
          log.info("available update exists remotely {} is greater than {}", remoteVersion, currentVersion);
          // FIXME - make Http.getPart file ?
          // FIXME !!!! MAKE PART FILE !!!!!!!
          Http.getFile(String.format(MULTI_BRANCH_LATEST_BUILD_URL, branch), repoFile);
          log.info("successfully downloaded %s", repoFile);
        }
        // Http.get(String.format(MULTI_BRANCH_LATEST_BUILD_URL, branch));
        // config on launcher - auto-relaunch ?

        // determine if ready to update (important this is all done in a single
        // thread)
        if (autoUpdate) {
          String currentJar = FileIO.getRoot();
          // find the latest in the repo - compare with current version - update
          // if appropriate
          String latestRepoFileName = getLatestRepoFile(branch);
          
          String repoVersion = parseVersion(latestRepoFileName); 
          if (isGreaterThan(repoVersion, currentVersion)) {
            log.info("found applicable update {}", latestRepoFileName);
            // update ready (nonDismissed) event
            
            // we have ability to update now (all clearance to proceed)
            
            // request for export.all
            
            /**<pre>
             * 1. make copy of current jar if applicable (isJar) into repo if not already there..
             * 2.
             * </pre>
             */
            
            // see if we need to make a copy of ourselves to the repo
            if (FileIO.isJar() && currentJar != null) {
              copyToRepo(currentJar, branch, repoVersion);
            }
            
            // export current state
            Runtime.getInstance().exportAll("export.py");
            
            
            // replace our current jar (classes ? build?)
            writeBytesFromRepo(String.format("repo/%s", latestRepoFileName), "./myrobotlab.jar");
            
            
            // prepare launcher command to delete current file and replace with target, then start
            log.info("preparing launcher command");
                        
            Launcher launcher = new Launcher();
            // Process ps = launcher.buildUpdate(latestRepoFileName, "./myrobotlab.jar", "-i"
            
          }
        }

        Thread.sleep(interval);
      } catch (Exception e) {
        log.error("UpdateUtils threw", e);
      }
    }

    log.info("stopping updater");
    isRunning = false;
    updater.worker = null;
  }
  
  public String parseVersion(String filename) {
    if (filename == null) {
      return null;
    }
    String[] parts = filename.split("-");
    if (parts.length == 3) {
      String v = parts[2];
      String[] p = v.split("\\.");
      if (p.length == 4) {
        return String.format("%s.%s.%s", p[0], p[1], p[2]);
      }
    }
    log.error("non standard filename {}", filename);
    return null;    
  }
  
  public String parseBranch(String filename) {
    if (filename == null) {
      return null;
    }
    String[] parts = filename.split("-");
    if (parts.length == 3) {
      return parts[1];
    }
    log.error("non standard filename {}", filename);
    return null;
  }

  public String getLatestRepoFile(String branch) {
    try {
      if (branch == null) {
        log.error("branch must be specified");
        return null;
      }
      File repo = new File("repo");
      String latest = null;
      String latestFile = null;
      String[] possibleUpdates = repo.list(this);
      for (int i = 0; i < possibleUpdates.length; ++i) {
        String filename = possibleUpdates[i];
        String fileBranch = parseBranch(filename);
        String fileVersion = parseVersion(filename);
        if (fileBranch != null && fileBranch.equals(branch) && isGreaterThan(fileVersion, latest)) {
          latest = fileVersion;
          latestFile = filename;
        }
      }
      return latestFile;
    } catch (Exception e) {
      log.error("getRepoVersion threw", e);
    }
    return null;
  }

  @Override
  public boolean accept(File dir, String name) {
    String[] parts = name.split("-");
    if (name.endsWith(".jar") && parts.length == 3 && parts[1].equals(branch)) {
      return true;
    }
    return false;
  }
  
  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      Updater.getUpdater(new String[] { "--current-version", "1.1.241" });

      // "standardized" meta version extraction

      // polling github, localfile, release binary

      // downloading, or building, caching

      // "standard" local archive/myrobotlab-{branch}-{version}.jar
      // "standard" local archive/myrobotlab-develop-1.1.993.jar

      Updater.start();

      // Updater.stop();

      // http://build.myrobotlab.org:8080/job/myrobotlab/job/develop/lastSuccessfulBuild/artifact/target/myrobotlab.jar

      // what version ????
      // /archive/target/version.xml /txt/json/manifest ?
      // Http.get(url

      // download jar from branch

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
  
}

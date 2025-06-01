package org.myrobotlab.service;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

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
import org.myrobotlab.framework.CmdOptions;
import org.myrobotlab.framework.MrlException;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.Http;
import org.myrobotlab.process.Launcher;
import org.myrobotlab.process.UpdateListener;
import org.slf4j.Logger;

import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 * 
 * @author GroG
 * 
 *         Updater - updates instances of myrobotlab
 * 
 *         Use cases : * update running instance * notify user of ready update *
 *         notify services of restart * saving existing data in export.py *
 *         dismiss notification of update * not ready notification * update non
 *         running system * update from different branch * update from remote
 *         binary * update from remote source * update from local binary
 * 
 * 
 *         challenges : file locking process spawning
 *
 */

public class Updater extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Updater.class);

  public static String VERSION_ROOT = "./";

  final static String REMOTE_BUILDS_URL_HOME = "http://build.myrobotlab.org:8080/job/myrobotlab-multibranch/";

  protected long interval = 5000;

  protected transient Process updated = null;

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

  public Updater(String reservedKey, String inId) {
    super(reservedKey, inId);
  }

  /**
   * Checks in the branches directory for the latest version of desired "branch"
   * 
   * @param branch
   *          the branch to check
   * @return the latest version
   * @throws MrlException
   *           boom
   * 
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
   *          the branch
   * @param allowRemote
   *          true/false
   * @return a set
   * 
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
   *          the branch
   * @return the set of local versions
   * 
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
   *          the branch name
   * @return the set of remote versions
   * 
   */
  public Set<String> getRemoteVersions(String branch) {
    Set<String> versions = new TreeSet<String>();
    try {

      byte[] data = Http.get(String.format(REMOTE_BUILDS_URL_HOME + REMOTE_BUILDS_URL, branch));
      if (data != null) {
        String json = new String(data);
        WorkflowJob job = CodecUtils.fromJson(json, WorkflowJob.class);
        if (job.builds != null) {
          for (WorkflowRun build : job.builds) {
            if ("SUCCESS".equals(build.result)) {
              versions.add(Platform.VERSION_PREFIX + build.id);
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

  private Boolean offline = false;

  long updateCheckIntervalMs = 5000;

  public synchronized void start() {
    addTask(updateCheckIntervalMs, "checkForUpdates");
  }

  public synchronized void stop() {
    log.info("stopping updater");
    purgeTask("checkForUpdates");
  }

  protected static final String MULTI_BRANCH_VERSION = "http://build.myrobotlab.org:8080/job/myrobotlab/job/%s/api/xml?xpath=/*/lastStableBuild/number";

  public String getRemoteVersion(String branch) throws UnsupportedEncodingException {
    byte[] v = Http.get(String.format(MULTI_BRANCH_VERSION, branch));
    if (v != null) {
      String jenkinsLatest = new String(v, "UTF-8");
      int p0 = jenkinsLatest.indexOf("<number>");
      int p1 = jenkinsLatest.indexOf("</number>", p0);
      if (p0 != -1 & p1 != -1) {
        String version = Platform.VERSION_PREFIX + jenkinsLatest.substring("<number>".length(), jenkinsLatest.length() - "</number>".length());
        log.info("jenkins returned version {}", version);
        return version;
      }
    }
    return null;
  }

  public void writeBytesFromTarget(String src, String dst) throws IOException {
    FileIO.copyBytes(src, dst);
  }

  static public class UpdateVersion {
    public UpdateVersion(String version, String file, Long timestamp) {
      this.version = version;
      this.file = file;
      this.timestamp = timestamp;
    }

    public String version;
    public Long timestamp;
    public String file;
  }

  public UpdateVersion versionAvailable(String version, String file, Long timestamp) {
    return new UpdateVersion(version, file, timestamp);
  }

  public void checkForUpdates() {
    log.info("starting updater {} {} with interval {} s", branch, currentVersion, interval / 1000);
    try {
      log.info("checking for update");

      // remote or local

      // if remote get binary put in target
      // check if various parts exist
      Properties gitProps = Platform.gitProperties();

      // create local repo if it does not exist
      File target = new File("target");
      if (!target.exists()) {
        target.mkdir();
      }

      /**
       * <pre>
       * TWO SOURCES EXISTS 
       *    1. Remote Binaries 
       *    2. Local Sources
       * 
       * Local sources will always be considered "later" than Remote Binaries
       * so the mode will switch to only checking Git - This "switch" is
       * determined if a src directory exists.
       * 
       * Update life cycle
       *    1. check if available update (jenkins url, or git pull)
       *    2. publish availability status
       *    3. if auto prepare - download (jenkins binary or mvn build)
       *    4. publish status
       *    6. if auto update
       *         create export.py
       *         if isSrcMode
       * 
       * 
       * 
       * </pre>
       */

      // get local or remote latest version
      // compare with current version
      // do callbacks if we have an update

      boolean isSrcMode = new File(".git").exists();
      log.info("{} mode checking for updates", (isSrcMode ? "SOURCE" : "BINARY"));

      if (isSrcMode) {
        String cwd = System.getProperty("user.dir");
        boolean makeBuild = false;

        Repository repo = new FileRepositoryBuilder().setGitDir(new File(System.getProperty("user.dir"))).build();
        org.eclipse.jgit.api.Git git = new org.eclipse.jgit.api.Git(repo);
        String branch = git.getRepository().getBranch();
        log.info("current source branch is \"{}\"", branch);

        int commitsBehind = pull(null, branch);

        if (gitProps == null) {
          log.info("target/classes/git.properties does not exist - will build");
          makeBuild = true;
        } else {
          // compare last built commit with current commit?
          if (commitsBehind > 0) {
            log.info("local \"{}\" is behind by {} commits", branch, commitsBehind);
            log.info("build and repo do not match - build required");
            makeBuild = true;
          }
        }

        if (makeBuild) {
          // FIXME - download mvn if it does not exist ??

          // remove git properties before compile
          File props = new File(System.getProperty("user.dir") + File.separator + "target" + File.separator + "classes" + File.separator + "git.properties");
          props.delete();

          // FIXME - compile or package mode !
          String ret = Maven.mvn(cwd, branch, "compile", System.currentTimeMillis() / 1000, offline);
          if (ret != null & ret.contains("BUILD SUCCESS")) {
            offline = true;
          }

          // package ??

          // FIXME - export & launch

          if (autoUpdate) {

            // FIXME !!! - default option to tear down if running webgui (single
            // resource)
            // FIXME - if running as standalone handle other case
            if (updated == null) {
              // FIXME check for null etc.. singleton
              // FIXME - merge original
              CmdOptions options = new CmdOptions();
              new CommandLine(options).parseArgs(new String[] { "-I", "python", "execFile", "export.py" });
              ProcessBuilder builder = Launcher.createBuilder(options);
              updated = builder.start();

              // FIXME check if alive etc...
              // Process ps = launcher.buildUpdate("myrobotlab.jar", "-i",
              // "python", "execFile", "export.py");
            } else {
              // process already exists - tear down and restart FIXME - export !
              updated.destroy();
              // FIXME - merge original
              CmdOptions options = new CmdOptions();
              new CommandLine(options).parseArgs(new String[] { "-I", "python", "execFile", "export.py" });
              ProcessBuilder builder = Launcher.createBuilder(options);
              updated = builder.start();
            }
          }
        }
        /*
         * if (updated == null) { String[] updatedArgs = new String[] { "-I",
         * "python", "execFile", "export.py" }; String[] cmdLine =
         * Launcher.createSpawnArgs(updatedArgs); ProcessBuilder builder =
         * Launcher.createBuilder(cwd, cmdLine); updated = builder.start(); }
         */

        // if last not ! current check git properties !!
        log.info("updater loop");

      } else {
        // REMOTE BINARIES UPDATE FROM JENKINS

        String remoteVersion = getRemoteVersion(branch);
        String targetFile = String.format("target/myrobotlab-%s-%s.jar", branch, remoteVersion);

        if (isGreaterThan(remoteVersion, currentVersion) && autoCache && !(new File(targetFile).exists())) {
          log.info("available update exists remotely {} is greater than current version {}", remoteVersion, currentVersion);
          // FIXME - make Http.getPart file ?
          // FIXME !!!! MAKE PART FILE !!!!!!!
          Http.getFile(String.format(MULTI_BRANCH_LATEST_BUILD_URL, branch), targetFile);
          log.info("successfully downloaded %s", targetFile);

        }
        // Http.get(String.format(MULTI_BRANCH_LATEST_BUILD_URL, branch));
        // config on launcher - auto-relaunch ?

        // determine if ready to update (important this is all done in a
        // single
        // thread)

        // find the latest in the target - compare with current version -
        // update
        // if appropriate
        String latestFile = getLatestTargetFile(branch);

        String latestVersion = parseVersion(latestFile);
        if (isGreaterThan(latestVersion, currentVersion)) {
          log.info("found applicable update {}", latestFile);
          broadcast("versionAvailable", latestVersion, latestFile, System.currentTimeMillis());
          // update ready (nonDismissed) event

          // we have ability to update now (all clearance to proceed)

          // request for export.all

          /**
           * <pre>
            * 1. make copy of current jar if applicable (isJar) into target if not already there..
            * 2.
           * </pre>
           */

          if (autoUpdate) {
            // see if we need to make a copy of ourselves to the target
            String currentJar = FileIO.getRoot();
            if (FileIO.isJar() && currentJar != null && currentVersion != null) {
              log.info("writing backup of curent jar to {} to myrobotlab.jar", latestFile);
              FileIO.copy(currentJar, String.format("target/myrobotlab-%s-%s.jar", branch, currentVersion));
            }

            // FIXME - re-implement
            // export current state
            // if (Runtime.exists()) {
            // Runtime.getInstance().save("last-restart/runtime.yml");
            // }

            // replace our current jar (classes ? build?)
            log.info("writing {} to myrobotlab.jar", latestFile);
            writeBytesFromTarget(String.format("target/%s", latestFile), "./myrobotlab.jar");

            // prepare launcher command to delete current file and replace
            // with
            // target, then start
            log.info("preparing launcher command");

            if (updated == null) {
              // FIXME check for null etc.. singleton
              // FIXME - ability to merge more commands !!!
              CmdOptions options = new CmdOptions();
              new CommandLine(options).parseArgs(new String[] { "-I", "python", "execFile", "export.py" });
              ProcessBuilder builder = Launcher.createBuilder(options);
              updated = builder.start();

              // FIXME check if alive etc...
              // Process ps = launcher.buildUpdate("myrobotlab.jar", "-i",
              // "python", "execFile", "export.py");
            }

          }
        }
      }
      Thread.sleep(interval);
    } catch (Exception e) {
      log.error("UpdateUtils threw", e);
    }
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

  public String getLatestTargetFile(String branch) {
    try {
      if (branch == null) {
        log.error("branch must be specified");
        return null;
      }
      File target = new File("target");
      String latest = null;
      String latestFile = null;
      String[] possibleUpdates = target.list((FilenameFilter) this);
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
      log.error("getTargetVersion threw", e);
    }
    return null;
  }

  // file filter
  public boolean accept(File dir, String name) {
    String[] parts = name.split("-");
    if (name.endsWith(".jar") && parts.length == 3 && parts[1].equals(branch)) {
      return true;
    }
    return false;
  }

  TextProgressMonitor monitor = new TextProgressMonitor();

  public int pull(String src, String branch) throws IOException, WrongRepositoryStateException, InvalidConfigurationException, DetachedHeadException, InvalidRemoteException,
      CanceledException, RefNotFoundException, NoHeadException, TransportException, GitAPIException {

    if (src == null) {
      src = System.getProperty("user.dir");
    }

    if (branch == null) {
      log.warn("branch is not set - setting to default develop");
      branch = "develop";
    }

    List<String> branches = new ArrayList<String>();
    branches.add("refs/heads/" + branch);

    File repoParentFolder = new File(src);

    org.eclipse.jgit.api.Git git = null;
    Repository repo = null;

    // Open an existing repository FIXME Try Git.open(dir)
    String gitDir = repoParentFolder.getAbsolutePath() + "/.git";
    repo = new FileRepositoryBuilder().setGitDir(new File(gitDir)).build();
    git = new org.eclipse.jgit.api.Git(repo);

    repo = git.getRepository();
    git.branchCreate().setForce(true).setName(branch).setStartPoint(branch).call();
    git.checkout().setName(branch).call();

    git.fetch().setProgressMonitor(monitor).call();

    List<RevCommit> localLogs = getLogs(git, "origin/" + branch, 1);
    List<RevCommit> remoteLogs = getLogs(git, "remotes/origin/" + branch, 1);

    RevCommit localCommit = localLogs.get(0);
    RevCommit remoteCommit = remoteLogs.get(0);

    BranchTrackingStatus status = BranchTrackingStatus.of(repo, branch);

    // FIXME - Git.close() file handles

    if (status.getBehindCount() > 0) {
      log.info("local ts {}, remote {} - {} pulling", localCommit.getCommitTime(), remoteCommit.getCommitTime(), remoteCommit.getFullMessage());
      PullCommand pullCmd = git.pull();
      pullCmd.setProgressMonitor(monitor);
      pullCmd.call();
      git.close();
      return status.getBehindCount();
    }
    log.info("no new commits on branch {}", branch);
    git.close();
    return 0;
  }

  private List<RevCommit> getLogs(org.eclipse.jgit.api.Git git, String ref, int maxCount)
      throws RevisionSyntaxException, NoHeadException, MissingObjectException, IncorrectObjectTypeException, AmbiguousObjectException, GitAPIException, IOException {
    List<RevCommit> ret = new ArrayList<>();
    Repository repository = git.getRepository();
    Iterable<RevCommit> logs = git.log().setMaxCount(maxCount).add(repository.resolve(ref)).call();
    for (RevCommit rev : logs) {
      ret.add(rev);
    }
    return ret;
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      // use cases - used as a command line tool

      // Runtime connects to public or main mrl instance ...
      // put in update mode (master) --> |(runtime + updater) --> (worke) |

      // - used by runtime ... in theory runtime could call/configure updater
      // through main

      Updater updater = (Updater) Runtime.start("updater", "Updater");

      CmdOptions options = new CmdOptions();
      new CommandLine(options).parseArgs(new String[] { "-I", "python", "execFile", "export.py" });
      ProcessBuilder builder = Launcher.createBuilder(options);
      updater.updated = builder.start();

      new CommandLine(updater).parseArgs(args);
      if (updater.autoUpdate) {
        updater.start();
      }

      // Updater.stop();

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}

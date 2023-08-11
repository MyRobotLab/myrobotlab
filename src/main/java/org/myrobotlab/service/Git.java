package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.ServiceConfig;
import org.slf4j.Logger;

public class Git extends Service<ServiceConfig> {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Git.class);

  transient static TextProgressMonitor monitor = new TextProgressMonitor();

  Map<String, RepoData> repos = new TreeMap<>();

  // FIXME - addBlockingTask and change it to default
  long checkStatusIntervalMs = 60000; // every minute

  public static class RepoData {
    String branch;
    String location;
    String url;
    List<String> branches;
    String checkout;
    transient org.eclipse.jgit.api.Git git;

    public RepoData(String location, String url, List<String> branches, String checkout, org.eclipse.jgit.api.Git git) {
      this.location = location;
      this.url = url;
      this.branches = branches;
      this.checkout = checkout;
      this.git = git;
    }

  }

  public Git(String n, String id) {
    super(n, id);
  }

  // max complexity clone
  public void clone(String location, String url, List<String> inbranches, String incheckout) throws InvalidRemoteException, TransportException, GitAPIException, IOException {

    File repoLocation = new File(location);
    org.eclipse.jgit.api.Git git = null;
    Repository repo = null;

    List<String> branches = new ArrayList<>();
    for (String b : inbranches) {
      if (!b.contains("refs")) {
        branches.add("refs/heads/" + b);
      }
    }

    String checkout = (incheckout.contains("refs")) ? incheckout : "refs/heads/" + incheckout;

    if (!repoLocation.exists()) {
      // clone
      log.info("cloning {} {} into {}", url, incheckout, location);
      git = org.eclipse.jgit.api.Git.cloneRepository().setProgressMonitor(monitor).setURI(url).setDirectory(repoLocation).setBranchesToClone(branches).setBranch(checkout).call();

    } else {
      // Open an existing repository
      String gitDir = repoLocation.getAbsolutePath() + File.separator + ".git";
      log.info("opening repo {} from {}", gitDir, url);
      repo = new FileRepositoryBuilder().setGitDir(new File(gitDir)).build();
      git = new org.eclipse.jgit.api.Git(repo);
    }

    repo = git.getRepository();

    // checkout
    log.info("checking out {}", incheckout);
    // git.branchCreate().setForce(true).setName(incheckout).setStartPoint("origin/"
    // + incheckout).call();
    git.branchCreate().setForce(true).setName(incheckout).setStartPoint(incheckout).call();
    git.checkout().setName(incheckout).call();

    repos.put(location, new RepoData(location, url, inbranches, incheckout, git));

  }

  // max complexity sync
  public void sync(String location, String url, List<String> branches, String checkout) throws InvalidRemoteException, TransportException, GitAPIException, IOException {
    // initial clone
    clone(location, url, branches, checkout);

    addTask(checkStatusIntervalMs, "checkStatus");
  }

  public void sync(String location, String url, String checkout) throws InvalidRemoteException, TransportException, GitAPIException, IOException {
    sync(location, url, Arrays.asList(checkout), checkout);
  }

  public RevCommit checkStatus() throws WrongRepositoryStateException, InvalidConfigurationException, InvalidRemoteException, CanceledException, RefNotFoundException,
      RefNotAdvertisedException, NoHeadException, TransportException, GitAPIException, IOException {
    for (RepoData repository : repos.values()) {
      org.eclipse.jgit.api.Git git = repository.git;
      String branch = repository.checkout;

      // List<RevCommit> localLogs = getLogs(git, "origin/" + branch, 1);
      List<RevCommit> remoteLogs = getLogs(git, "remotes/origin/" + branch, 1);

      // RevCommit localCommit = localLogs.get(0);
      RevCommit remoteCommit = remoteLogs.get(0);
      log.info("last remote commit " + remoteCommit.getAuthorIdent() + " " + remoteCommit.getShortMessage());

      Repository repo = git.getRepository();
      BranchTrackingStatus status = BranchTrackingStatus.of(repo, branch);

      // TODO - handle fetch with a published status, and lifecycle return input
      // from user to pull/merge
      // if (localCommit.getCommitTime() < remoteCommit.getCommitTime()) {
      if (status.getBehindCount() > 0) {
        // log.info("local ts {}, remote {} - {} updating",
        // localCommit.getCommitTime(), remoteCommit.getCommitTime(),
        // remoteCommit.getFullMessage());
        PullCommand pullCmd = git.pull();
        pullCmd.setProgressMonitor(monitor);
        pullCmd.call();
        invoke("publishPull", remoteCommit);
        return remoteCommit;
      } else {
        log.info("no new commits on branch {}", branch);
      }
    }
    return null;
  }

  public RevCommit publishPull(RevCommit commit) {
    return commit;
  }

  private static List<RevCommit> getLogs(org.eclipse.jgit.api.Git git, String ref, int maxCount)
      throws RevisionSyntaxException, NoHeadException, MissingObjectException, IncorrectObjectTypeException, AmbiguousObjectException, GitAPIException, IOException {
    List<RevCommit> ret = new ArrayList<>();
    Repository repository = git.getRepository();
    Iterable<RevCommit> logs = git.log().setMaxCount(maxCount).add(repository.resolve(ref)).call();
    for (RevCommit rev : logs) {
      ret.add(rev);
    }
    return ret;
  }

  public void sync(String location, String url) throws InvalidRemoteException, TransportException, GitAPIException, IOException {
    sync(location, url, "master");
  }

  public Long setInterval(Long interval) {
    checkStatusIntervalMs = interval;
    purgeTasks();
    addTask(interval, "checkStatus");
    return checkStatusIntervalMs;
  }

  public void stopSync() {
    purgeTasks();
  }

  static public int pull() throws WrongRepositoryStateException, InvalidConfigurationException, DetachedHeadException, InvalidRemoteException, CanceledException,
      RefNotFoundException, NoHeadException, TransportException, IOException, GitAPIException {
    return pull(null, null);
  }

  static public int pull(String branch) throws WrongRepositoryStateException, InvalidConfigurationException, DetachedHeadException, InvalidRemoteException, CanceledException,
      RefNotFoundException, NoHeadException, TransportException, IOException, GitAPIException {
    return pull(null, branch);
  }

  static org.eclipse.jgit.api.Git getGit(String rootFolder) throws IOException {
    if (rootFolder == null) {
      rootFolder = System.getProperty("user.dir");
    }

    File root = new File(rootFolder);
    if (!root.exists() || !root.isDirectory()) {
      throw new IOException(rootFolder + " invalid - must be git root folder");
    }

    String gitDir = root.getAbsolutePath() + "/.git";
    File check = new File(gitDir);
    if (!check.exists()) {
      throw new IOException(gitDir + " does not exist");
    }

    Repository repo = new FileRepositoryBuilder().setGitDir(new File(gitDir)).build();
    org.eclipse.jgit.api.Git git = new org.eclipse.jgit.api.Git(repo);
    return git;
  }

  static public int pull(String src, String branch) throws IOException, WrongRepositoryStateException, InvalidConfigurationException, DetachedHeadException, InvalidRemoteException,
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

  static public void init() throws IllegalStateException, GitAPIException {
    init(null);
  }

  static public void init(String directory) throws IllegalStateException, GitAPIException {
    if (directory == null) {
      directory = System.getProperty("user.dir");
    }
    File dir = new File(directory);
    log.info("git init {}", dir);
    org.eclipse.jgit.api.Git git = org.eclipse.jgit.api.Git.init().setDirectory(dir).call();
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Properties properties = Platform.gitProperties();
      Git.removeProps();
      log.info("{}", properties);

      /*
       * // start the service Git git = (Git) Runtime.start("git", "Git");
       * 
       * // check out and sync every minute // git.sync("test",
       * "https://github.com/MyRobotLab/WorkE.git", "master"); //
       * git.sync("/lhome/grperry/github/mrl/myrobotlab", //
       * "https://github.com/MyRobotLab/myrobotlab.git", "agent-removal");
       * git.gitPull("agent-removal"); //
       * git.sync(System.getProperty("user.dir"), //
       * "https://github.com/MyRobotLab/myrobotlab.git", "agent-removal");
       */
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  public static String getBranch() throws IOException {
    return getBranch(null);
  }

  public static String getBranch(String src) throws IOException {
    org.eclipse.jgit.api.Git git = getGit(src);
    return git.getRepository().getBranch();
  }

  public static Status status() throws NoWorkTreeException, IOException, GitAPIException {
    return status(null);
  }

  public static Status status(String src) throws IOException, NoWorkTreeException, GitAPIException {
    org.eclipse.jgit.api.Git git = getGit(src);
    Status status = git.status().call();
    return status;
  }

  public static void removeProps() {
    removeProps(null);
  }

  public static void removeProps(String rootFolder) {
    if (rootFolder == null) {
      rootFolder = System.getProperty("user.dir");
    }

    File props = new File(rootFolder + File.separator + "target" + File.separator + "classes" + File.separator + "git.properties");
    props.delete();

  }

}

package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.GitConfig;
import org.slf4j.Logger;

public class Git extends Service<GitConfig>
{

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Git.class);

  transient ProgressMonitor monitor = new ProgressMonitor();

  Map<String, RepoData> repos = new TreeMap<>();

  // FIXME - addBlockingTask and change it to default
  long checkStatusIntervalMs = 60000; // every minute

  public static class RepoData {
    String branch;
    String location;
    String url;
    String checkout;
    transient org.eclipse.jgit.api.Git git;

    public RepoData(String location, String url, String checkout, org.eclipse.jgit.api.Git git) {
      this.location = location;
      this.url = url;
      this.checkout = checkout;
      this.git = git;
    }

  }

  // TODO - overload updates to publish
  public class ProgressMonitor extends TextProgressMonitor {

  }

  public Git(String n, String id) {
    super(n, id);
  }

  public void clone(String location, String url, String branch, String checkout)
      throws InvalidRemoteException, TransportException, GitAPIException, IOException {
    clone(location, url, branch, checkout, false);
  }

  // max complexity sync
  public void sync(String location, String url, String branch, String checkout)
      throws InvalidRemoteException, TransportException, GitAPIException, IOException {
    // initial clone
    clone(location, url, branch, checkout);

    addTask(checkStatusIntervalMs, "checkStatus");
  }

  public void sync(String location, String url, String checkout)
      throws InvalidRemoteException, TransportException, GitAPIException, IOException {
    sync(location, url, checkout, checkout);
  }

  public RevCommit checkStatus() throws WrongRepositoryStateException, InvalidConfigurationException,
      InvalidRemoteException, CanceledException, RefNotFoundException,
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

  private List<RevCommit> getLogs(org.eclipse.jgit.api.Git git, String ref, int maxCount)
      throws RevisionSyntaxException, NoHeadException, MissingObjectException, IncorrectObjectTypeException,
      AmbiguousObjectException, GitAPIException, IOException {
    List<RevCommit> ret = new ArrayList<>();
    Repository repository = git.getRepository();
    Iterable<RevCommit> logs = git.log().setMaxCount(maxCount).add(repository.resolve(ref)).call();
    for (RevCommit rev : logs) {
      ret.add(rev);
    }
    return ret;
  }

  public void sync(String location, String url)
      throws InvalidRemoteException, TransportException, GitAPIException, IOException {
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

  public int pull() throws WrongRepositoryStateException, InvalidConfigurationException, DetachedHeadException,
      InvalidRemoteException, CanceledException, RefNotFoundException,
      NoHeadException, TransportException, IOException, GitAPIException {
    return pull(null, null);
  }

  public int pull(String branch) throws WrongRepositoryStateException, InvalidConfigurationException,
      DetachedHeadException, InvalidRemoteException, CanceledException,
      RefNotFoundException, NoHeadException, TransportException, IOException, GitAPIException {
    return pull(null, branch);
  }

  org.eclipse.jgit.api.Git getGit(String rootFolder) throws IOException {
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

  public int pull(String src, String branch) throws IOException, WrongRepositoryStateException,
      InvalidConfigurationException, DetachedHeadException, InvalidRemoteException,
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
      log.info("local ts {}, remote {} - {} pulling", localCommit.getCommitTime(), remoteCommit.getCommitTime(),
          remoteCommit.getFullMessage());
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

  public void init() throws IllegalStateException, GitAPIException {
    init(null);
  }

  public void init(String directory) throws IllegalStateException, GitAPIException {
    if (directory == null) {
      directory = System.getProperty("user.dir");
    }
    File dir = new File(directory);
    log.info("git init {}", dir);
    org.eclipse.jgit.api.Git git = org.eclipse.jgit.api.Git.init().setDirectory(dir).call();
  }

  public String getBranch() throws IOException {
    return getBranch(null);
  }

  public String getBranch(String src) throws IOException {
    org.eclipse.jgit.api.Git git = getGit(src);
    return git.getRepository().getBranch();
  }

  public Status status() throws NoWorkTreeException, IOException, GitAPIException {
    return status(null);
  }

  public Status status(String src) throws IOException, NoWorkTreeException, GitAPIException {
    org.eclipse.jgit.api.Git git = getGit(src);
    Status status = git.status().call();
    return status;
  }

  public void removeProps() {
    removeProps(null);
  }

  public void removeProps(String rootFolder) {
    if (rootFolder == null) {
      rootFolder = System.getProperty("user.dir");
    }

    File props = new File(
        rootFolder + File.separator + "target" + File.separator + "classes" + File.separator + "git.properties");
    props.delete();

  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Properties properties = Platform.gitProperties();
      // Git.removeProps();
      log.info("{}", properties);
      Git git = (Git) Runtime.start("git", "Git");
      git.clone("./depthai", "https://github.com/luxonis/depthai.git", "main", "refs/tags/v1.13.1-sdk", true);
      log.info("here");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  // max complexity clone and checkout
  public void clone(String location, String url, String branch, String checkout, boolean recursive)
      throws InvalidRemoteException, TransportException, GitAPIException, IOException {

    File repoLocation = new File(location);
    org.eclipse.jgit.api.Git git = null;
    Repository repo = null;

    // git clone

    if (!repoLocation.exists()) {
      // clone
      log.info("cloning {} {} checking out {} into {}", url, branch, checkout, location);
      git = org.eclipse.jgit.api.Git.cloneRepository().setProgressMonitor(monitor).setURI(url)
          .setDirectory(repoLocation).setBranch(branch).call();

    } else {
      // Open an existing repository
      String gitDir = repoLocation.getAbsolutePath() + File.separator + ".git";
      log.info("opening repo {} from {}", gitDir, url);
      repo = new FileRepositoryBuilder().setGitDir(new File(gitDir)).build();
      git = new org.eclipse.jgit.api.Git(repo);
    }

    repo = git.getRepository();

    // git pull

    PullCommand pullCmd = git.pull()
        // .setRemote(remoteName)
        // .setCredentialsProvider(new
        // UsernamePasswordCredentialsProvider(username, password))
        .setRemoteBranchName(branch);

    // Perform the pull operation
    pullCmd.call();

    // recursive
    if (recursive) {

      // Recursively fetch and checkout submodules if they exist
      SubmoduleWalk submoduleWalk = SubmoduleWalk.forIndex(repo);
      while (submoduleWalk.next()) {
        String submodulePath = submoduleWalk.getPath();
        org.eclipse.jgit.api.Git submoduleGit = org.eclipse.jgit.api.Git.open(new File(location, submodulePath));
        submoduleGit.fetch()
            .setRemote("origin")
            .call();
        submoduleGit.checkout()
            .setName(branch) // Replace with the desired branch name
            .call();
      }

    }

    if (checkout != null) {
      // checkout
      log.info("checking out {}", checkout);
      // git.branchCreate().setForce(true).setName(incheckout).setStartPoint("origin/"
      // + incheckout).call();
      git.branchCreate().setForce(true).setName(branch).setStartPoint(checkout).call();
      git.checkout().setName(checkout).call();
    }

    repos.put(location, new RepoData(location, url, checkout, git));

  }

}

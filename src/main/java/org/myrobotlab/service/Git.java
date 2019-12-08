package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.errors.CanceledException;
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
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Git extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Git.class);

  transient TextProgressMonitor monitor = new TextProgressMonitor();

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
    
    String checkout = (incheckout.contains("refs"))?incheckout:"refs/heads/"+incheckout;   

    if (!repoLocation.exists()) {
      // clone
      log.info("cloning {} {} into {}", url, incheckout, location);
      git = org.eclipse.jgit.api.Git.cloneRepository().setProgressMonitor(monitor).setURI(url).setDirectory(repoLocation).setBranchesToClone(branches)
          .setBranch(checkout).call();
      
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
    git.branchCreate().setForce(true).setName(incheckout).setStartPoint("origin/" + incheckout).call();
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

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(Git.class);
    meta.addDescription("used to manage source code");
    meta.addCategory("programming");
    return meta;
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

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      // start the service
      Git git = (Git) Runtime.start("git", "Git");

      // check out and sync every minute
      git.sync("react", "https://github.com/MyRobotLab/myrobotlab-react.git");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}

package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import picocli.CommandLine.Option;

public class Builder extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Builder.class);

  String versionPrefix = "1.1.";

  String currentBranch;

  String currentVersion;

  // for AGENT used to sync to the latest via source and build
  @Option(names = { "--src", "--use-source" }, arity = "0..1", description = "use latest source")
  public String src;

  public Builder(String n, String id) {
    super(n, id);
  }

  public String mvn(String branch) {
    return mvn(null, branch, null);
  }

  public String getLatestSrc(String branch) throws WrongRepositoryStateException, InvalidConfigurationException, DetachedHeadException, InvalidRemoteException, CanceledException,
      RefNotFoundException, NoHeadException, TransportException, IOException, GitAPIException {

    // Agent agent = (Agent) Runtime.getService("agent");

    RevCommit latestCommit = gitPull(branch);
    if (latestCommit != null) {
      log.info("latest {} - will attempt to build", latestCommit);
      String version = mvn(null, branch, (long) latestCommit.getCommitTime());
      log.info("successfully build version {} - {}", latestCommit.getCommitTime(), latestCommit.getFullMessage());
      return version;
    }
    return null;
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
     * CheckoutCommand checkout = git.checkout().setCreateBranch(true).setName(branch).setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
     *     .setStartPoint("origin/" + branch).call();
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

  public String getJarName(String branch, String version) {
    // FIXME !!! branch name is completely unreliable :(
    // version info which includes build number can distinguish for what we need
    return "myrobotlab-" + version + ".jar";
    // return "myrobotlab-" + branch + "-" + version + ".jar";
    // return getDir(branch, version) + File.separator + "myrobotlab.jar";
  }

  public static void main(String[] args) {
    try {

      // options.fork = true;
      // lets check and get the latest jar if there is new one

      /**
       * <pre>
       
       try {
         
         if (globalOptions.src == null) {
           // get the latest from Jenkins
           getLatestJar(getBranch());
         } else {
           // get the latest from GitHub
           getLatestSrc(getBranch());
         }
         
       } catch (TransportException e) {
         log.info("could not get latest myrobotlab - {}", e.getMessage());
       } catch (Exception e) {
         log.error("trying to update failed", e);
       }
       
       
       
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
         ...
       * </pre>
       */

      LoggingFactory.init(Level.INFO);

      Runtime.start("template", "_TemplateService");
      Runtime.start("servo", "Servo");
      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}

package org.myrobotlab.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.MavenConfig;
import org.slf4j.Logger;

import picocli.CommandLine.Option;

public class Maven extends Service<MavenConfig> {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Maven.class);

  static String currentBranch;

  static String currentVersion;

  static boolean offline = false;

  // for AGENT used to sync to the latest via source and build
  @Option(names = { "--src", "--use-source" }, arity = "0..1", description = "use latest source")
  public String src;

  public Maven(String n, String id) {
    super(n, id);
  }

  public String mvn(String branch, String phase) {
    return mvn(null, branch, phase, null, null);
  }

  static public String mvn(String src, String branch, String phase, Long buildNumber, Boolean off) {

    String results = null;

    try {

      // FIXME - check for pom.xml
      //
      if (off != null) {
        offline = off;
      }

      if (src == null) {
        src = System.getProperty("user.dir");
      }

      if (branch == null) {
        branch = "develop";
      }

      if (phase == null) {
        phase = "package";
      }

      String fs = File.separator;
      File myrobotlabJar = new File(src + fs + "target" + fs + "myrobotlab.jar");
      if (myrobotlabJar.exists()) {
        log.info("removing {}", myrobotlabJar.getAbsolutePath());
        myrobotlabJar.delete();
      }
      File snapshot = new File(src + fs + "target" + fs + "mrl-0.0.1-SNAPSHOT.jar");
      if (snapshot.exists()) {
        snapshot.delete();
      }

      if (buildNumber == null) {
        // epoch minute build time number
        buildNumber = System.currentTimeMillis() / 1000;
      }

      String version = Platform.VERSION_PREFIX + buildNumber;

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
      // sb.append("compile");
      // sb.append(" ");
      // sb.append("prepare-package");
      // sb.append(" ");
      // sb.append("package");
      sb.append(phase);
      sb.append(" ");
      // cmd.add("-f");
      // cmd.add(pathToPom);
      // cmd.add("-o"); // offline
      if (offline) {
        sb.append("-o"); // offline
      }

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

      File buildResults = new File("mvn-build.out");
      if (buildResults.exists()) {
        buildResults.delete();
      }

      pb.directory(new File(src));

      // handle stderr as a direct pass through to System.err
      pb.redirectErrorStream(true);
      // pb.environment().putAll(System.getenv());
      // pb.redirectOutput(new File("blah"));
      pb.redirectOutput(buildResults);
      // pb.inheritIO().start().waitFor();
      Process process = pb.start();
      int retCode = process.waitFor();
      if (retCode != 0) {
        log.error("process terminated with {} return code", retCode);
      }

      if (buildResults.exists()) {
        results = FileIO.toString(buildResults);
        if (results != null && results.contains("BUILD SUCCESS")) {
          log.info("BUILD SUCCESS");
          // set next build in offline mode for faster builds
          offline = true;
        } else {
          log.error("BUILD FAILURE");
          log.error(results);
          offline = false;
        }
      }

      String newJar = src + File.separator + "target" + File.separator + "myrobotlab.jar";
      /// String newJarLoc = getJarName(branch, version);
      String finalName = "target" + File.separator + String.format("myrobotlab-%s-%s.jar", branch, version);
      File p = new File(newJar).getAbsoluteFile().getParentFile();
      p.mkdirs();

      if (phase.equals("package")) {
        Files.move(Paths.get(newJar), Paths.get(finalName), StandardCopyOption.REPLACE_EXISTING);
      }

      return Platform.VERSION_PREFIX + buildNumber + "";
    } catch (Exception e) {
      log.error("mvn threw ", e);
    }
    return results;
  }

  public static void main(String[] args) {
    try {

      Maven builder = (Maven) Runtime.start("builder", "Maven");
      builder.mvn("agent-removal", "compile");
      // builder.mvn("agent-removal", "package");

      // check for mvn
      // download mvn (where ???)

      // build

      // rename

      // provide info/reports

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

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}

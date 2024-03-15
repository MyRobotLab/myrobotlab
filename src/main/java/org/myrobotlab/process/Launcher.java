package org.myrobotlab.process;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.myrobotlab.framework.CmdOptions;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

import picocli.CommandLine;

public class Launcher {

  public static String banner = "   _____         __________      ___.           __  .____          ___.    \n"
      + "  /     \\ ___.__.\\______   \\ ____\\_ |__   _____/  |_|    |   _____ \\_ |__  \n"
      + " /  \\ /  <   |  | |       _//  _ \\| __ \\ /  _ \\   __\\    |   \\__  \\ | __ \\ \n"
      + "/    Y    \\___  | |    |   (  <_> ) \\_\\ (  <_> )  | |    |___ / __ \\| \\_\\ \\\n" + "\\____|__  / ____| |____|_  /\\____/|___  /\\____/|__| |_______ (____  /___  /\n"
      + "        \\/\\/             \\/           \\/                    \\/    \\/    \\/ \n            " + Platform.getLocalInstance().getMotd();

  public final static Logger log = LoggerFactory.getLogger(Launcher.class);

  // process handles give to runtime
  static Process process = null;

  // location of repo - is target (as maven expects output)
  static final public String TARGET_LOCATION = "target";

  public static File NULL_FILE = new File((System.getProperty("os.name").startsWith("Windows") ? "NUL" : "/dev/null"));
  public static File STD_OUT = new File("std.out");

  static public ProcessBuilder createBuilder(CmdOptions options) throws IOException {
    return createBuilder(null, options);
  }

  static public ProcessBuilder createBuilder(String cwd, CmdOptions options) throws IOException {

    Platform platform = Platform.getLocalInstance();

    // command line to be returned
    List<String> cmd = new ArrayList<String>();

    // prepare exe
    String fs = File.separator;
    String ps = File.pathSeparator;

    String exeName = platform.isWindows() ? "javaw" : "java";
    String javaExe = String.format("%s%sbin%s%s", System.getProperty("java.home"), fs, fs, exeName);

    String jvmArgs = String.format("-Djava.library.path=libraries/native -Djna.library.path=libraries/native -Dfile.encoding=UTF-8");
    if (platform.isWindows()) {
      jvmArgs = jvmArgs.replace("/", "\\");
    }

    cmd.add(javaExe);

    if (options.memory != null) {
      jvmArgs += String.format(" -Xms%s -Xmx%s ", options.memory, options.memory);
    }

    cmd.add(jvmArgs);

    if (options.jvm != null) {
      String[] jvm = options.jvm.split(" ");
      for (int i = 0; i < jvm.length; ++i) {
        cmd.add(jvm[i]);
      }
    }

    cmd.add("-cp");

    // classes cp first for newly built with builder
    // Rules of classloading : first wins ...
    // so highest priority
    // 1. target/classes (local builds)
    // 2. target/myrobotlab.jar (local packages)
    // 3. myrobotlab.jar (normal package)

    String classpath = "target" + fs + "classes" + ps + "target" + fs + "myrobotlab.jar" + ps + "myrobotlab.jar" + ps + ("libraries" + fs + "jar" + fs + "*");
    cmd.add(classpath);

    // main class
    cmd.add("org.myrobotlab.service.Runtime");

    cmd.addAll(options.getOutputCmd());

    // FIXME - daemonize? does that mean handle stream differently?
    // FIXME - reporting from different levels .. one is stdout the other is the
    // os before this
    log.info("SPAWN {}", toString(cmd));
    System.out.print("SPAWN ");
    System.out.println(toString(cmd));
    ProcessBuilder builder = new ProcessBuilder(cmd);
    builder.redirectErrorStream(true);
    // builder.inheritIO(); # LAME - JDK BUG FIXED THEN NOT FIXED ...

    // one of the nastiest bugs had to do with std out, or std err not
    // being consumed ... now we don't bother with it - instead
    // we have to use this clever redirect to /dev/null (os dependent) :(
    // and be done with the whole silly issue

    // builder.redirectOutput(new File("stdout.txt"));
    /*
     * if (options.stdout) { builder.redirectOutput(STD_OUT); } else {
     * builder.redirectOutput(NULL_FILE); }
     */

    // setting working directory to wherever the jar is...

    File spawnDir = (cwd == null) ? new File(System.getProperty("user.dir")) : new File(cwd);
    if (!spawnDir.exists() || !spawnDir.isDirectory()) {
      log.error("{} not a directory", spawnDir.getAbsolutePath());
    }

    builder.directory(spawnDir);
    log.info("WORKING DIR {}", spawnDir.getAbsolutePath());
    log.info("SPAWNING ! -->{}", toString(cmd));

    // environment variables setup
    setEnv(builder.environment());

    return builder;
  }

  public static String[] toArray(List<String> list) {
    return list.toArray(new String[list.size()]);
  }

  public static List<String> toList(String[] array) {
    return new ArrayList<String>(Arrays.asList(array));
  }

  public static String toString(List<String> cmdLine) {
    return toString(cmdLine.toArray(new String[cmdLine.size()]));
  }

  public static String toString(String[] cmdLine) {
    StringBuilder spawning = new StringBuilder();
    for (String c : cmdLine) {
      spawning.append(c);
      spawning.append(" ");
    }
    return spawning.toString();
  }

  /**
   * prints help to the console
   * 
   * @return the help
   */
  static public String mainHelp() {
    String help = new CommandLine(new CmdOptions()).getUsageMessage();
    System.out.print(help);
    return help;
  }

  static public Map<String, String> setEnv(Map<String, String> env) {
    Platform platform = Platform.getLocalInstance();
    String platformId = platform.getPlatformId();
    if (platform.isLinux()) {
      String ldPath = String.format("libraries/native:libraries/native/%s:${LD_LIBRARY_PATH}", platformId);
      env.put("LD_LIBRARY_PATH", ldPath);
    } else if (platform.isMac()) {
      String dyPath = String.format("libraries/native:libraries/native/%s:${DYLD_LIBRARY_PATH}", platformId);
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
      log.error("unknown operating system");
    }

    return env;
  }

  static boolean contains(List<String> l, String flag) {
    for (String f : l) {
      if (f.equals(flag)) {
        return true;
      }
    }
    return false;
  }

  /**
   * FIXME - should not be in main because of the void return :( ProcessBuilder
   * or Process return ...
   * 
   * Start class for myrobotlab.jar. Its primary concern is to build and launch
   * a myrobotlab instance, depending on flags it might also start a client as
   * an interface to the spawned instance
   * 
   * @param args
   *          args
   */
  public static void main(String[] args) {
    try {

      // FIXME - attempt to connect to localhost first ???
      // if unsuccessful then spawn

      CmdOptions options = new CmdOptions();
      new CommandLine(options).parseArgs(args);

      LoggingFactory.init(options.logLevel);
      log.info("in args {}", toString(args));

      log.info("\n" + banner);

      // help and exit
      if (options.help) {
        mainHelp();
        return;
      }

      // install services and exit
      if (options.install != null) {
        if (options.install.length == 0) {
          Runtime.install();
        } else {
          for (String serviceType : options.install)
            Runtime.install(serviceType);
        }
        return;
      }

      log.info("spawning new instance");
      ProcessBuilder builder = createBuilder(options);
      process = builder.start();
      if (process.isAlive()) {
        log.info("process is alive");
      } else {
        log.error("process died");
      }

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}

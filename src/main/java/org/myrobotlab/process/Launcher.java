package org.myrobotlab.process;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.myrobotlab.client.Client;
import org.myrobotlab.framework.CmdOptions;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.lang.NameGenerator;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

import picocli.CommandLine;

public class Launcher {

  static String banner = "   _____         __________      ___.           __  .____          ___.    \n"
      + "  /     \\ ___.__.\\______   \\ ____\\_ |__   _____/  |_|    |   _____ \\_ |__  \n"
      + " /  \\ /  <   |  | |       _//  _ \\| __ \\ /  _ \\   __\\    |   \\__  \\ | __ \\ \n"
      + "/    Y    \\___  | |    |   (  <_> ) \\_\\ (  <_> )  | |    |___ / __ \\| \\_\\ \\\n" + "\\____|__  / ____| |____|_  /\\____/|___  /\\____/|__| |_______ (____  /___  /\n"
      + "        \\/\\/             \\/           \\/                    \\/    \\/    \\/ \n            " + Platform.getLocalInstance().getMotd();

  public final static Logger log = LoggerFactory.getLogger(Launcher.class);

  static Process process = null;

  // location of repo - is target (as maven expects output)
  static final public String TARGET_LOCATION = "target";

  public static File NULL_FILE = new File((System.getProperty("os.name").startsWith("Windows") ? "NUL" : "/dev/null"));
  // public static File NULL_FILE = new File("myrobotlab.log");

  static public ProcessBuilder createBuilder(String cwd, String[] cmdLine) throws IOException {
    return createBuilder(cwd, new ArrayList<String>(Arrays.asList(cmdLine)));
  }

  static public ProcessBuilder createBuilder(String cwd, List<String> cmdLine) throws IOException {

    // FIXME - reporting from different levels .. one is stdout the other is the
    // os before this
    ProcessBuilder builder = new ProcessBuilder(cmdLine);

    // one of the nastiest bugs had to do with std out, or std err not
    // being consumed ... now we don't bother with it - instead
    // we have to use this clever redirect to /dev/null (os dependent) :(
    // and be done with the whole silly issue

    builder.redirectErrorStream(true);

    // builder.redirectOutput(new File("stdout.txt"));
    builder.redirectOutput(NULL_FILE);

    // setting working directory to wherever the jar is...

    File spawnDir = (cwd == null) ? new File(System.getProperty("user.dir")) : new File(cwd);
    if (!spawnDir.exists() || !spawnDir.isDirectory()) {
      log.error("{} not a directory", spawnDir.getAbsolutePath());
    }

    builder.directory(spawnDir);
    log.info("SPAWNING ! -->{}$ \n{}", spawnDir.getAbsolutePath(), toString(cmdLine));

    // environment variables setup
    setEnv(builder.environment());

    return builder;
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

  public static List<String> createSpawnArgs(List<String> args)
      throws IllegalArgumentException, IllegalAccessException, IOException, URISyntaxException, InterruptedException, ParseException {
    return createSpawnArgs(args.toArray(new String[args.size()]));
  }

  /**
   * Takes a list of arguments and turns them into a start cmd line to start an
   * instance of myrobotlab
   * 
   * @param options
   * @return
   * @throws IOException
   * @throws URISyntaxException
   * @throws InterruptedException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws ParseException
   */
  public static List<String> createSpawnArgs(String[] args)
      throws IOException, URISyntaxException, InterruptedException, IllegalArgumentException, IllegalAccessException, ParseException {

    CmdOptions options = new CmdOptions();
    new CommandLine(options).parseArgs(args);

    Platform platform = Platform.getLocalInstance();

    if (options.id == null) {
      options.id = NameGenerator.getName();
    }

    options.spawnedFromLauncher = true;

    // default service if non specified
    if (options.services.size() == 0) {
      options.services.add("webgui");
      options.services.add("WebGui");
      options.services.add("intro");
      options.services.add("Intro");
      options.services.add("python");
      options.services.add("Python");
    }

    // SETUP COMMAND !!!!!
    String fs = File.separator;
    String ps = File.pathSeparator;

    String exeName = platform.isWindows() ? "javaw" : "java";
    String javaExe = String.format("%s%sbin%s%s", System.getProperty("java.home"), fs, fs, exeName);

    String jvmArgs = String.format("-Djava.library.path=libraries/native -Djna.library.path=libraries/native -Dfile.encoding=UTF-8");
    if (platform.isWindows()) {
      jvmArgs = jvmArgs.replace("/", "\\");
    }

    if (options.memory != null) {
      jvmArgs += String.format(" -Xms%s -Xmx%s ", options.memory, options.memory);
    }

    // command line to be returned
    List<String> cmd = new ArrayList<String>();

    cmd.add(javaExe);

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
    // String classpath = String.format(cpTemplate, jarPath, ps, libraries,
    // ps, libraries, ps, ps);
    if (platform.isWindows()) {
      classpath = classpath.replace("/", "\\");
    }
    cmd.add(classpath);

    // main class
    cmd.add("org.myrobotlab.service.Runtime");

    if (options.services.size() > 0) {
      if (options.services.size() % 2 != 0) {
        log.error("--service requires {name} {Type} {name} {Type} even number of entries - you have {}", options.services.size());
      }
      cmd.add("--service");
      for (int i = 0; i < options.services.size(); i += 2) {
        cmd.add(options.services.get(i));
        cmd.add(options.services.get(i + 1));
      }
    }

    // FIXME !!!! - this less than ideal !
    // "MOST" of the flags are "relayed" through so CmdOptions are handled in
    // the spawned process - but a few are handled by the agent and should not
    // be
    // relayed
    cmd.add("--id");
    cmd.add(options.id);

    cmd.add("--data-dir");
    cmd.add(options.dataDir);

    if (options.logLevel != null) {
      cmd.add("--log-level");
      cmd.add(options.logLevel);
    }

    // FIXME - shouldn't 'everything' simply be relayed on that doesn't
    // directly affect Agent?
    if (options.install != null) {
      cmd.add("--install");
      for (String serviceType : options.install) {
        cmd.add(serviceType);
      }
    }

    // FIXME THIS IS TERRIBLE !!! IT SHOULD SIMPLY BE PASS THROUGH !!
    // FIXME - adding new CmdOption
    if (options.cfg != null) {
      cmd.add("-c");
      cmd.add(options.cfg);
    }

    if (options.addKeys != null) {
      cmd.add("-k");
      for (String keyPart : options.addKeys) {
        cmd.add(keyPart);
      }
    }

    // TERRRIBLE !!!
    if (options.connect != null) {
      cmd.add("--connect");
      cmd.add(options.connect);
    }

    if (options.invoke != null) {
      cmd.add("--invoke");
      for (String keyPart : options.invoke) {
        cmd.add(keyPart);
      }
    }

    if (options.virtual) {
      cmd.add("--virtual");
    }

    // add if not there
    cmd.add("--spawned-from-agent");

    log.info("spawn {}", toString(cmd));

    return cmd;

  }

  public static CmdOptions getCmdOptions(String[] args) throws IllegalArgumentException, IllegalAccessException, IOException, URISyntaxException, InterruptedException {
    CmdOptions options = new CmdOptions();
    new CommandLine(options).parseArgs(args);
    return options;
  }

  /**
   * A version to be unique is both {branch}-{version}. This finds all currently
   * available versions.
   * 
   * @return
   */
  public static Set<String> getLocalVersions() {
    Set<String> versions = new TreeSet<>();
    // get local file system versions
    File branchDir = new File(TARGET_LOCATION);
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
   * prints help to the console
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

  public String buildUpdate(String classpath, String targetJar, long delay) {

    // "--invoke python exec"
    return null;
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
   */
  public static void main(String[] args) {
    try {

      // FIXME - attempt to connect to localhost first ???
      // if unsuccessful then spawn

      CmdOptions options = new CmdOptions();
      new CommandLine(options).parseArgs(args);

      LoggingFactory.init(options.logLevel);
      log.info("in args {}", Arrays.toString(args));

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

      boolean instanceAlreadyRunning = false;

      try {
        URI uri = new URI(options.connect);
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(uri.getHost(), uri.getPort()), 1000);
        socket.close();
        instanceAlreadyRunning = true;
      } catch (Exception e) {
        log.info("could not connect to {}", options.connect);
      }

      if (instanceAlreadyRunning && options.connect.equals(options.DEFAULT_CLIENT)) {
        log.error("zombie instance already running at {}", options.DEFAULT_CLIENT);
        return;
      }

      if (!instanceAlreadyRunning && !options.print && options.connect.equals(options.DEFAULT_CLIENT)) {
        log.info("spawning new instance");
        // process the incoming args into spawn args
        List<String> spawnArgs = createSpawnArgs(args);

        ProcessBuilder builder = createBuilder(options.cwd, spawnArgs);
        process = builder.start();
        if (process.isAlive()) {
          log.info("process is alive");
        } else {
          log.error("process died");
        }
      }

      // FIXME - use wsclient for remote access
      if (!options.noLauncherClient) {
        // FIXME - delay & auto connect
        Client.main(new String[] { "-c", options.connect });
      } else {
        // terminating - "if" runtime exists - if not no biggy
        Runtime.shutdown();
      }

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  public static ProcessBuilder createBuilder(String[] args) throws IOException {
    return createBuilder(null, new ArrayList<String>(Arrays.asList(args)));
  }

  public static ProcessBuilder createBuilder(List<String> args) throws IOException {
    return createBuilder(null, args);
  }

}

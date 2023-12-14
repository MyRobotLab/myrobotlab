package org.myrobotlab.framework;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * <pre>
 * Command options for picocli library. This encapsulates all the available
 * command line flags and their details. arity attribute is for specifying in
 * an array or list the number of expected attributes after the flag. Short
 * versions of flags e.g. -i must be unique and have only a single character.
 * 
 * FIXME - make it callable so it does a callback and does some post proccessing .. i think that's why its callable ?
 * FIXME - have it capable of toString or buildCmdLine that in turn can be used as input to generate the CmdOptions again, ie.
 *         test serialization
 *         
 * FIXME - there are in parameters - ones supplied by the user of ProcessUtils, and out params which will become 
 * behavior changes in the Runtime
 * 
 * </pre>
 */
@Command(name = "java -jar myrobotlab.jar ")
public class CmdOptions {  

  public final String DEFAULT_CONNECT = "http://localhost:8888";

  static boolean contains(List<String> l, String flag) {
    for (String f : l) {
      if (f.equals(flag)) {
        return true;
      }
    }
    return false;
  }

  // launcher ??
  @Option(names = { "-a", "--auto-update" }, description = "auto updating - this feature allows mrl instances to be automatically updated when a new version is available")
  public boolean autoUpdate = false;

  // launcher
  @Option(names = { "-c",
      "--config" }, fallbackValue="default", description = "Specify a configuration set to start. The config set is a directory which has all the necessary configuration files. It loads runtime.yml first, and subsequent service configuration files will then load. \n example: --config data/config/my-config-dir")
  public String config = null;

  @Option(names = {
      "--connect" }, arity = "0..*", /*
                                      * defaultValue = DEFAULT_CONNECT,
                                      */ fallbackValue = DEFAULT_CONNECT, description = "connects this mrl instance to another mrl instance - default is " + DEFAULT_CONNECT)
  public String connect = null;

  @Option(names = { "-h", "-?", "--help" }, description = "shows help")
  public boolean help = false;
  
  @Option(names = { "-r", "--config-root" }, description = "sets configuration root, the root for which all config directories are in")
  public String configRoot = null;


  @Option(names = { "--id" }, description = "process identifier to be mdns or network overlay name for this instance - one is created at random if not assigned")
  public String id;

  @Option(names = { "-i",
      "--install" }, arity = "0..*", description = "installs all dependencies for all services, --install {serviceType} installs dependencies for a specific service, if no type is specified then all services are installed")
  public String install[];

  @Option(names = { "-I",
      "--invoke" }, arity = "0..*", description = "invokes a method on a service --invoke {serviceName} {method} {param0} {param1} ... : --invoke python execFile myFile.py")
  public String invoke[];

  // for launcher
  @Option(names = { "-j", "--jvm" }, arity = "0..*", description = "jvm parameters for the instance of mrl")
  public String jvm;

  @Option(names = { "-l", "--log-level" }, description = "log level - helpful for troubleshooting [debug info warn error]")
  public String logLevel = "info";

  @Option(names = { "--log-file" }, description = "log file name [myrobotlab.log]")
  public String logFile = "myrobotlab.log";

  // FIXME - highlight or italics for examples !!
  // launcher
  @Option(names = { "-m", "--memory" }, description = "adjust memory can e.g. -m 2g \n -m 128m")
  public String memory = null;

  @Option(names = { "-s", "--service",
      "--services" }, arity = "0..*", description = "services requested on startup, the services must be {name} {Type} paired, e.g. gui SwingGui webgui WebGui servo Servo ...")
  public List<String> services = new ArrayList<>();

  @Option(names = { "-V", "--virtual" }, description = "sets global environment as virtual - all services which support virtual hardware will create virtual hardware")
  public boolean virtual = false;

  public CmdOptions() {
  }

  // copy constructor for people who don't like continued maintenance ;) -
  // potentially dangerous for arrays and containers
  public CmdOptions(CmdOptions other) throws IllegalArgumentException, IllegalAccessException {
    Field[] fields = this.getClass().getDeclaredFields();
    for (Field field : fields) {
      field.set(this, field.get(other));
    }
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
   * Command options data object will return the options in List form to be
   * appended to the ProcessBuilder(List)
   * 
   * @return the list of output command
   * @throws IOException
   *           boom
   * 
   */
  public List<String> getOutputCmd() throws IOException {

    List<String> cmd = new ArrayList<>();

    if (autoUpdate) {
      cmd.add("-a");
    }

    if (config != null) {
      cmd.add("--config");
      cmd.add(config);
    }

    if (connect != null) {
      cmd.add("-c");
      cmd.add(connect);
    }

    if (invoke != null) {
      cmd.add("-I");
      for (int i = 0; i < invoke.length; ++i) {
        cmd.add(invoke[i]);
      }
    }

    if (help) {
      cmd.add("-h");
    }

    if (id != null) {
      cmd.add("--id");
      cmd.add(id);
    }

    if (install != null) {
      cmd.add("-i");
      for (int i = 0; i < install.length; ++i) {
        cmd.add(install[i]);
      }
    }

    if (logLevel != null) {
      cmd.add("--log-level");
      cmd.add(logLevel);
    }

    if (memory != null) {
      cmd.add("-m");
      cmd.add(memory);
    }

    // default service if non specified
    // more logic here when preforming some other activity like adding keys?
    if (services.size() == 0) {
      services.add("webgui");
      services.add("WebGui");
      services.add("intro");
      services.add("Intro");
      services.add("python");
      services.add("Python");
    }

    if (services.size() % 2 != 0) {
      throw new IOException("invalid choice - services must be -s {name} {type} ...");
    }
    cmd.add("-s");
    for (String s : services) {
      cmd.add(s);
    }

    if (virtual) {
      cmd.add("-v");
    }

    return cmd;
  }

}

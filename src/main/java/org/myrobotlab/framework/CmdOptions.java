package org.myrobotlab.framework;

import java.lang.reflect.Field;
import java.util.ArrayList;
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

  // launcher
  @Option(names = {
  "--cwd" }, arity = "0..1", description = "working directory to spawn process in")
  public String cwd = null;
  
  // FIXME - override for classpath of spawn target
  
  // launcher
  @Option(names = { "-a", "--auto-update" }, description = "auto updating - this feature allows mrl instances to be automatically updated when a new version is available")
  public boolean autoUpdate = false;

  public final String DEFAULT_CLIENT = "http://localhost:8888"; 

  // launcher
  @Option(names = { "-c", "--client" }, description = "connect to another mrl instance - default is " + DEFAULT_CLIENT)
  public String client = DEFAULT_CLIENT;
  
  // launcher
  @Option(names = {
      "--no-client" }, arity = "0..1", description = "starts a command line interface and optionally connects to a remote instance - default with no host param connects to agent process --client [host]")
  public boolean noClient = false;

  @Option(names = { "-p","--print" }, description = "print command line instead of spawning instance")
  public boolean print = false;
  
  // deprecated ??? - only interaction is on a network now ???
  @Option(names = { "--interactive" }, description = "starts in interactive mode - reading from stdin")
  public boolean interactive = false;

  @Option(names = { "--spawned-from-agent" }, description = "starts in interactive mode - reading from stdin")
  public boolean spawnedFromAgent = false;

  @Option(names = { "-h", "-?", "--?", "--help" }, description = "shows help")
  public boolean help = false;

  @Option(names = { "-I",
      "--invoke" }, arity = "0..*", description = "invokes a method on a service --invoke {serviceName} {method} {param0} {param1} ... : --invoke python execFile myFile.py")
  public String invoke[];

  // FIXME - should work with a startup ...
  @Option(names = { "-k", "--add-key" }, arity = "2..*", description = "adds a key to the key store\n"
      + "@bold,italic java -jar myrobotlab.jar -k amazon.polly.user.key ABCDEFGHIJKLM amazon.polly.user.secret Fidj93e9d9fd88gsakjg9d93")
  public String addKeys[];

  @Option(names = { "-j", "--jvm" }, arity = "0..*", description = "jvm parameters for the instance of mrl")
  public String jvm;

  @Option(names = { "--id" }, description = "process identifier to be mdns or network overlay name for this instance - one is created at random if not assigned")
  public String id;

  @Option(names = { "--config" }, description = "Configuration file. If specified all configuration from the file will be used as a \"base\" of configuration. "
      + "All configuration of last run is saved to {data-dir}/lastOptions.json. This file can be used as a starter config for subsequent --cfg config.json. "
      + "If this value is set, all other configuration flags are ignored.")
  public String cfg = null;

  // FIXME - highlight or italics for examples !!
  @Option(names = { "-m", "--memory" }, description = "adjust memory can e.g. -m 2g \n -m 128m")
  public String memory = null;

  @Option(names = { "-l", "--log-level" }, description = "log level - helpful for troubleshooting " + " [debug info warn error]")
  public String logLevel = "info";

  @Option(names = { "-i",
      "--install" }, arity = "0..*", description = "installs all dependencies for all services, --install {MetaData} installs dependencies for a specific service")
  public String install[];

  @Option(names = { "-V", "--virtual" }, description = "sets global environment as virtual - all services which support virtual hardware will create virtual hardware")
  public boolean virtual = false;

  @Option(names = { "-s", "--service",
      "--services" }, arity = "0..*", description = "services requested on startup, the services must be {name} {Type} paired, e.g. gui SwingGui webgui WebGui servo Servo ...")
  public List<String> services = new ArrayList<>();

  @Option(names = { "--data-dir" }, description = "sets the location of the data directory")
  public String dataDir = "data";

  @Option(names = { "-x", "--extract-resources" }, description = "force extraction of resources tot he resource dir")
  public boolean extractResources = false;

  // launcher
  @Option(names = {"--inherit-io" }, description = "inherit the io streams from the spawned process - default false")
  public boolean inheritIO = false;

}

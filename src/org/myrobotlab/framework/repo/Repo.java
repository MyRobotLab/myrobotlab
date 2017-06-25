package org.myrobotlab.framework.repo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.Filter;
import org.apache.ivy.util.filter.NoFilter;
import org.apache.ivy.util.url.URLHandler;
import org.apache.ivy.util.url.URLHandlerDispatcher;
import org.apache.ivy.util.url.URLHandlerRegistry;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceReservation;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.io.FindFile;
import org.myrobotlab.io.Zip;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.RepoInstallListener;
import org.slf4j.Logger;

// FIXME
// clearRepo - whipes out all (calls other methods) <- not static
// clearRepoCache - wipes out .repo <- static since it IS static - only 1 on machine
// clearLibraries <- not static - this is per instance/installation
// clearServiceData <- not static - this is per instance/installation 

/**
 * This class is responsible for maintaining the "local" repo state for the MRL
 * instance running. It could have "potentially" the knowledge of the gitHub
 * repo using the github api. But at the moment, it maintains a local file
 * specifying the state of the requested dependencies. For example. If the
 * Arduino Service is requested, then an attempt is made to download the
 * appropriate depenencies for the service. This would include some version of
 * jssc.
 * 
 * The attempt resolves &amp; retrieves or doesn't - the requested dependency and
 * its resultant state is written to the .myrobotlab repo.json file
 * 
 * @author GroG
 *
 */

public class Repo implements Serializable {

  private static final long serialVersionUID = 1L;

  public transient final static Logger log = LoggerFactory.getLogger(Repo.class);

  public static final Filter NO_FILTER = NoFilter.INSTANCE;

  private static Repo localInstance = getLocalInstance();

  TreeMap<String, Library> libraries = new TreeMap<String, Library>();

  final public static String INSTALL_START = "install start";
  final public static String INSTALL_PROGRESS = "install progress";
  final public static String INSTALL_FINISHED = "install finished";

  static String REPO_STATE_FILE_NAME;

  synchronized static public Repo getLocalInstance() {
    if (localInstance == null) {

      try {

        // REPO_STATE_FILE_NAME = String.format("%s%srepo.json",
        // FileIO.getCfgDir(), File.separator);
        REPO_STATE_FILE_NAME = String.format("repo.json");
        String data = FileIO.toString(REPO_STATE_FILE_NAME);
        localInstance = CodecUtils.fromJson(data, Repo.class);
      } catch (Exception e) {
        log.info("{} file not found", REPO_STATE_FILE_NAME);
        localInstance = new Repo();
      }
    }

    return localInstance;
  }

  final public String REPO_DIR = "repo";

  ArrayList<Status> errors = new ArrayList<Status>();

  private transient Ivy ivy = null;

  /**
   * call back notification of progress
   */
  private transient RepoInstallListener listener = null;

  public Repo() {
  }

  public void addStatusListener(RepoInstallListener listener) {
    this.listener = listener;
  }

  public List<Status> getErrors() {
    return errors;
  }

  public boolean hasErrors() {
    return (errors.size() > 0) ? true : false;
  }

  /**
   * info call back
   * @param format format
   * @param args args
   */
  public void info(String format, Object... args) {
    Status status = Status.info(format, args);
    status.name = Repo.class.getSimpleName();
    log.info(status.toString());
    installProgress(status);
  }

  /**
   * error callback
   * @param format format
   * @param args args
   */
  public void error(String format, Object... args) {
    Status status = Status.error(format, args);
    status.name = Repo.class.getSimpleName();
    log.error(status.toString());
    errors.add(status);
    installProgress(status);
  }

  /**
   * creates a installation start status this is primarily for calling services
   * which want a status of repo starting an install
   * @param format format
   * @param args args
   * @return status
   */
  static public Status createStartStatus(String format, Object... args) {
    Status status = Status.info(format, args);
    status.key = Repo.INSTALL_START;
    return status;
  }

  /**
   * creates a installation finished status this is primarily for calling
   * services which want a status of repo starting finishing an install
   * @param format format
   * @param args args
   * @return status
   */
  static public Status createFinishedStatus(String format, Object... args) {
    Status status = Status.info(format, args);
    status.key = Repo.INSTALL_FINISHED;
    return status;
  }

  /**
   * call back for listeners
   * @param status the status object?
   */
  public void installProgress(Status status) {
    if (listener != null) {
      listener.onInstallProgress(status);// .onStatus(status);
    }
  }

  /**
   * installs all currently defined service types and their dependencies
   * @throws ParseException e
   * @throws IOException e
   */
  public void install() throws ParseException, IOException {
    clearErrors();
    ServiceData sd = ServiceData.getLocalInstance();
    String[] typeNames = sd.getServiceTypeNames();
    for (int i = 0; i < typeNames.length; ++i) {
      install(typeNames[i]);
    }
  }

  public void clearErrors() {
    errors.clear();
  }

  /**
   * Install the all dependencies for a service if it has any. This uses Ivy
   * programmatically to resolve and retrieve all necessary dependencies for a
   * service.
   * 
   * Steps :
   * 
   * 1. check if .myrobotlab/repo.json file loaded - if not create it repo.json
   * represents current state of installed libraries (local repo) 2. get list of
   * dependecies from service type (this comes from the serviceData.json /
   * classMeta) these are what need to be resolved 3. retrieve - and update
   * state in memory and repo.json
   * @param fullTypeName f
   * @throws ParseException e 
   * @throws IOException e
   */
  public void install(String fullTypeName) throws ParseException, IOException {
    log.info("installing {}", fullTypeName);

    if (!fullTypeName.contains(".")) {
      fullTypeName = String.format("org.myrobotlab.service.%s", fullTypeName);
    }

    Set<Library> unfulfilled = getUnfulfilledDependencies(fullTypeName); // serviceData.getDependencyKeys(fullTypeName);

    for (Library dep : unfulfilled) {
      libraries.put(dep.getKey(), dep);
      resolveArtifacts(dep.getOrg(), dep.getRevision(), true);
    }
  }

  /**
   * searches through dependencies directly defined by the service and all Peers
   * - recursively searches for their dependencies if any are not found -
   * returns false
   * @param fullTypeName f
   * @return true/false
   */
  public boolean isServiceTypeInstalled(String fullTypeName) {
    ServiceData sd = ServiceData.getLocalInstance();

    if (!sd.containsServiceType(fullTypeName)) {
      log.error("unknown service {}", fullTypeName);
      return false;
    }

    Set<Library> libraries = getUnfulfilledDependencies(fullTypeName);
    if (libraries.size() > 0) {
      // log.info("{} is NOT installed", fullTypeName);
      return false;
    }

    // log.info("{} is installed", fullTypeName);
    return true;
  }

  /**
   * resolveArtifact does an Ivy resolve with a URLResolver to MRL's repo at
   * github. The equivalent command line is -settings ivychain.xml -dependency
   * "gnu.io.rxtx" "rxtx" "2.1-7r2" -confs "runtime,x86.64.windows"
   * @param org org
   * @param version version 
   * @param retrieve boolean
   * @return  the resolution report
   * @throws ParseException e
   * @throws IOException e
   */

  synchronized public ResolveReport resolveArtifacts(String org, String version, boolean retrieve) throws ParseException, IOException {
    info("%s %s.%s", (retrieve) ? "retrieving" : "resolve", org, version);
    // clear errors for this install
    errors.clear();

    Library library = new Library(org, version);
    libraries.put(library.getKey(), library);
    // creates clear ivy settings
    // IvySettings ivySettings = new IvySettings();
    String module;
    int p = org.lastIndexOf(".");
    if (p != -1) {
      module = org.substring(p + 1, org.length());
    } else {
      module = org;
    }

    // creates an Ivy instance with settings
    // Ivy ivy = Ivy.newInstance(ivySettings);
    if (ivy == null) {
      ivy = Ivy.newInstance();
      ivy.getLoggerEngine().pushLogger(new DefaultMessageLogger(Message.MSG_DEBUG));

      // PROXY NEEDED ?
      // CredentialsStore.INSTANCE.addCredentials(realm, host, username,
      // passwd);

      URLHandlerDispatcher dispatcher = new URLHandlerDispatcher();
      URLHandler httpHandler = URLHandlerRegistry.getHttp();
      dispatcher.setDownloader("http", httpHandler);
      dispatcher.setDownloader("https", httpHandler);
      URLHandlerRegistry.setDefault(dispatcher);

      // File communication is used
      // for ivy - the url branch info is in ivychain.xml
      // theoretically this would never change
      File ivychain = new File("ivychain.xml");
      if (!ivychain.exists()) {
        try {
          String xml = FileIO.resourceToString("framework/ivychain.xml");
          Platform platform = Platform.getLocalInstance();
          xml = xml.replace("{release}", platform.getBranch());
          FileOutputStream fos = new FileOutputStream(ivychain);
          fos.write(xml.getBytes());
          fos.close();
        } catch (Exception e) {
          Logging.logError(e);
        }
      }
      ivy.configure(ivychain);
      ivy.pushContext();

    }

    IvySettings settings = ivy.getSettings();
    // GAP20151208 settings.setDefaultCache(new
    // File(System.getProperty("user.home"), ".repo"));
    settings.setDefaultCache(new File(REPO_DIR));
    settings.addAllVariables(System.getProperties());

    File cache = new File(settings.substitute(settings.getDefaultCache().getAbsolutePath()));

    if (!cache.exists()) {
      cache.mkdirs();
    } else if (!cache.isDirectory()) {
      log.error(cache + " is not a directory");
    }

    Platform platform = Platform.getLocalInstance();
    String platformConf = String.format("runtime,%s.%s.%s", platform.getArch(), platform.getBitness(), platform.getOS());
    log.info(String.format("requesting %s", platformConf));

    String[] confs = new String[] { platformConf };
    String[] dep = new String[] { org, module, version };

    File ivyfile = File.createTempFile("ivy", ".xml");
    ivyfile.deleteOnExit();

    DefaultModuleDescriptor md = DefaultModuleDescriptor.newDefaultInstance(ModuleRevisionId.newInstance(dep[0], dep[1] + "-caller", "working"));
    DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md, ModuleRevisionId.newInstance(dep[0], dep[1], dep[2]), false, false, true);
    for (int i = 0; i < confs.length; i++) {
      dd.addDependencyConfiguration("default", confs[i]);
    }
    md.addDependency(dd);
    XmlModuleDescriptorWriter.write(md, ivyfile);
    confs = new String[] { "default" };

    ResolveOptions resolveOptions = new ResolveOptions().setConfs(confs).setValidate(true).setResolveMode(null).setArtifactFilter(NO_FILTER);
    // resolve & retrieve happen here ...
    ResolveReport report = ivy.resolve(ivyfile.toURI().toURL(), resolveOptions);
    List<?> err = report.getAllProblemMessages();

    if (err.size() > 0) {
      for (int i = 0; i < err.size(); ++i) {
        String errStr = err.get(i).toString();
        error(errStr);
      }
    } else {
      // set as installed & save state
      info("%s %s.%s for %s", (retrieve) ? "retrieved" : "installed", org, version, platform.getPlatformId());
      library.setInstalled(true);
      save();
    }
    // TODO - no error
    if (retrieve && err.size() == 0) {

      // TODO check on extension here - additional processing

      String retrievePattern = "libraries/[type]/[artifact].[ext]";// settings.substitute(line.getOptionValue("retrieve"));

      String ivyPattern = null;
      int ret = ivy.retrieve(md.getModuleRevisionId(), retrievePattern, new RetrieveOptions().setConfs(confs).setSync(false)// check
          .setUseOrigin(false).setDestIvyPattern(ivyPattern).setArtifactFilter(NO_FILTER).setMakeSymlinks(false).setMakeSymlinksInMass(false));

      log.info("retrieve returned {}", ret);

      setInstalled(getKey(org, version));
      save();

      // TODO - retrieve should mean unzip from local cache -> to root of
      // execution
      ArtifactDownloadReport[] artifacts = report.getAllArtifactsReports();
      for (int i = 0; i < artifacts.length; ++i) {
        ArtifactDownloadReport ar = artifacts[i];
        Artifact artifact = ar.getArtifact();
        File file = ar.getLocalFile();
        log.info("{}", file.getAbsoluteFile());
        // FIXME - native move up one directory !!! - from denormalized
        // back to normalized Yay!
        // maybe look for PlatformId in path ?
        // ret > 0 && <-- retrieved -
        if ("zip".equalsIgnoreCase(artifact.getType())) {
          String filename = String.format("libraries/zip/%s.zip", artifact.getName());
          info("unzipping %s", filename);
          Zip.unzip(filename, "./");
          info("unzipped %s", filename);
        }
      }
    }

    return report;
  }

  /**
   * saves repo to file
   */
  public void save() {
    try {
      FileOutputStream fos = new FileOutputStream(REPO_STATE_FILE_NAME);
      fos.write(CodecUtils.toJson(this).getBytes());
      fos.close();
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  /**
   * adds a library initially as unresolved to the local repo information if the
   * library becomes resolved - the state changes, and will be used to prevent
   * fetch or resolving the library again
   * @param org the org 
   * @param version the version of that lib
   */
  public void addLibrary(String org, String version) {
    Library dep = new Library(org, version);
    libraries.put(String.format("%s/%s", org, version), dep);
    save();
  }

  /**
   * generates instance of all dependencies from a repo directory would be
   * useful for checking validity - not used during runtime libraries
   * 
   * @param repoDir the directory to load from
   * @return map 
   */
  static public Map<String, Library> generateLibrariesFromRepo(String repoDir) {
    try {

      HashMap<String, Library> libraries = new HashMap<String, Library>();

      // get all third party libraries
      // give me all the first level directories of the repo
      // this CAN BE DONE REMOTELY TOO !!! - using v3 githup json api !!!
      List<File> dirs = FindFile.find(repoDir, "^[^.].*[^-_.]$", false, true);
      log.info("found {} files", dirs.size());
      for (int i = 0; i < dirs.size(); ++i) {
        File f = dirs.get(i);
        if (f.isDirectory()) {
          try {
            // log.info("looking in {}", f.getAbsolutePath());
            List<File> subDirsList = FindFile.find(f.getAbsolutePath(), ".*", false, true);
            ArrayList<File> filtered = new ArrayList<File>();
            for (int z = 0; z < subDirsList.size(); ++z) {
              File dir = subDirsList.get(z);
              if (dir.isDirectory()) {
                filtered.add(dir);
              }
            }

            File[] subDirs = filtered.toArray(new File[filtered.size()]);
            Arrays.sort(subDirs);
            // get latest version
            File ver = subDirs[subDirs.length - 1];
            log.info("adding third party library {} {}", f.getName(), ver.getName());
            libraries.put(getKey(f.getName(), ver.getName()), new Library(getKey(f.getName(), ver.getName())));
          } catch (Exception e) {
            log.error("folder {} is hosed !", f.getName());
            Logging.logError(e);
          }

        } else {
          log.info("skipping file {}", f.getName());
        }
      }
      return libraries;
    } catch (Exception e) {
      Logging.logError(e);
    }
    return null;
  }

  public void setInstalled(String key) {
    Library library = null;
    if (!libraries.containsKey(key)) {
      libraries.put(key, new Library(key));
    }

    library = libraries.get(key);
    library.setInstalled(true);
  }

  public static String getKey(String org, String version) {
    return String.format("%s/%s", org, version);
  }

  public Set<Library> getUnfulfilledDependencies(String type) {
    if (!type.contains(".")) {
      type = String.format("org.myrobotlab.service.%s", type);
    }
    HashSet<Library> ret = new HashSet<Library>();

    // get the dependencies required by the type
    ServiceData sd = ServiceData.getLocalInstance();
    if (!sd.containsServiceType(type)) {
      log.error(String.format("%s not found", type));
      return ret;
    }

    ServiceType st = sd.getServiceType(type);

    // look through our repo and resolve
    // if we dont have it - we need it
    Set<String> d = st.getDependencies();

    if (d != null && d.size() > 0) {
      for (String key : d) {
        if (!libraries.containsKey(key) || !libraries.get(key).isInstalled()) {
          ret.add(new Library(key));
        }
      }
    }

    TreeMap<String, ServiceReservation> peers = st.getPeers();
    if (peers != null) {
      for (String key : peers.keySet()) {
        ServiceReservation sr = peers.get(key);
        ret.addAll(getUnfulfilledDependencies(sr.fullTypeName));
      }
    }

    return ret;
  }

  public void clear() {
    log.info("Repo.clear - clearing libraries");
    FileIO.rm("libraries");
    log.info("Repo.clear - clearing repo");
    FileIO.rm("repo");
    log.info("Repo.clear - {}", REPO_STATE_FILE_NAME);
    FileIO.rm(REPO_STATE_FILE_NAME);
    log.info("Repo.clear - clearing memory");
    localInstance.libraries.clear();
    // localInstance = new Repo();
    log.info("clearing errors");
    clearErrors();
  }

  public boolean isInstalled(String typeName) {
    String fullTypeName = CodecUtils.makeFullTypeName(typeName);
    Set<Library> libraries = getUnfulfilledDependencies(fullTypeName);
    return libraries.size() == 0;
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init(Level.INFO);

      /**
       * TODO - test with all directories missing test as "one jar"
       * 
       * Use Cases : jar / no jar serviceData.json - none, local, remote (no
       * communication) / proxy / no proxy updateJar - no connection /
       * connection / preserve main args - jvm parameters update repo - no
       * connection / dependency affects others / single Service type / single
       * Dependency update repo - new Service Type purge respawner - use always
       * 
       */

      // FIXME - sync serviceData with ivy cache & library

      // get local instance

      Repo repo = Repo.getLocalInstance();
      repo.install("OpenCV");

      /*
       * String[] versions = { "1.0.100", "1.0.101", "1.0.102", "1.0.104",
       * "1.0.105", "1.0.106", "1.0.107", "1.0.92", "1.0.93", "1.0.94",
       * "1.0.95", "1.0.96", "1.0.97", "1.0.98", "1.0.99" };
       * 
       * String latest = repo.getLatestVersion(versions); log.info(latest);
       */

      // assert "1.0.107" == latest ->

      if (!repo.isServiceTypeInstalled("org.myrobotlab.service.InMoov")) {
        log.info("not installed");
      } else {
        log.info("is installed");
      }

      repo.install("org.myrobotlab.service.Arduino");

      /*
       * Updates updates = repo.checkForUpdates(); log.info(String.format(
       * "updates %s", updates)); if (updates.hasJarUpdate()) {
       * repo.getLatestJar(); }
       */

      // resolve All
      repo.install();

      // repo.clear(org, revision) // whipes out cache for 1 dep
      // repo.clear() // whipes out cache

      // FIXME - no serviceData.json = get from remote - will lose local
      // cache
      // info

      // iterate through them see

      // resolve dependency for 1

      // resolve all dependencies

      // update jar

      // resolving
      repo.install("org.myrobotlab.service.Arduino");

      // repo.getAllDepenencies();

      // remote tests

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}

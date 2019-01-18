package org.myrobotlab.framework.repo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.ServiceReservation;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.interfaces.StatusPublisher;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public abstract class Repo {

  public static final String DEFAULT_INSTALL_DIR = "libraries/jar";

  // Repo is an interface to a singleton of each "type" of repo
  private static String defaultRepoManagerType = "IvyWrapper";

  public static final String INSTALL_FINISHED = "installationStop";

  public static final String INSTALL_START = "installationStart";

  static protected transient Set<StatusPublisher> installStatusPublishers = new HashSet<StatusPublisher>();

  static protected transient Map<String, Repo> localInstances = new HashMap<String, Repo>();

  public final static Logger log = LoggerFactory.getLogger(Repo.class);

  protected List<RemoteRepo> remotes;

  public static final String REPO_STATE_FILE_NAME = "repo.json";

  public static final String INSTALL_PROGRESS = "installProgress";

  List<Status> errors = new ArrayList<Status>();

  Map<String, ServiceDependency> installedLibraries = new TreeMap<String, ServiceDependency>();

  public void error(String format, Object... args) {
    publishStatus(Status.error(format, args));
  }

  static public Repo getInstance() {
    return getInstance(defaultRepoManagerType);
  }

  public static Repo getInstance(String simpleType) {

    String type = makeFullTypeName(simpleType);

    if (localInstances.containsKey(type)) {
      return localInstances.get(type);
    } else {
      try {

        // FIXME - string only specifier - no class import info in this file (or
        // perhaps wrapper is ok?)
        // Maven.getInstance();
        Class<?> theClass = Class.forName(type);

        // getPeers
        Method method = theClass.getMethod("getTypeInstance");
        Repo repo = (Repo) method.invoke(null);
        localInstances.put(simpleType, repo);
        return repo;
      } catch (Exception e) {
        log.error("instanciating {} failed", type, e);
      }
      return null;
    }
  }

  public final static String makeFullTypeName(String type) {
    if (type == null) {
      return null;
    }
    if (!type.contains(".")) {
      return String.format("org.myrobotlab.framework.repo.%s", type);
    }
    return type;
  }

  protected Repo() {

    try {

      // FIXME reduce down to maven central bintray & repo.myrobotlab.org
      remotes = new ArrayList<RemoteRepo>();
      remotes.add(new RemoteRepo("central", "https://repo.maven.apache.org/maven2", "the mother load"));
      remotes.add(new RemoteRepo("bintray", "https://jcenter.bintray.com", "the big kahuna"));
      remotes.add(new RemoteRepo("bintray2", "https://dl.bintray.com", "more big kahuna"));
      remotes.add(new RemoteRepo("myrobotlab", "http://repo.myrobotlab.org/artifactory/myrobotlab", "all other mrl deps"));

      // DO NOT INCLUDE - messed up repo !
      // remotes.add(new RemoteRepo("dcm4che", "http://www.dcm4che.org/maven2",
      // "for
      // jai_imageio")); - do not use
      remotes.add(new RemoteRepo("eclipse-release", "https://repo.eclipse.org/content/groups/releases"));

      remotes.add(new RemoteRepo("jmonkey", "https://dl.bintray.com/jmonkeyengine/org.jmonkeyengine", "jmonkey simulator"));

      remotes.add(new RemoteRepo("oss-snapshots-repo", "https://oss.sonatype.org/content/groups/public", "sphinx"));
      remotes.add(new RemoteRepo("tudelft", "http://simulation.tudelft.nl/maven", "for j3d core, utils and vector"));
      // remotes.add(new RemoteRepo("jitpack", "https://jitpack.io", "microsoft
      // azure
      // translate"));
      remotes.add(new RemoteRepo("alfresco", "https://artifacts.alfresco.com/nexus/content/repositories/public", "swinggui mxgraph"));

      remotes.add(new RemoteRepo("marytts", "http://mary.dfki.de/repo", "some marytts voices"));

      load();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  public void addStatusPublisher(StatusPublisher service) {
    installStatusPublishers.add(service);
  }

  public void clear() {
    log.info("Repo.clear - clearing libraries");
    FileIO.rm("libraries");
    log.info("Repo.clear - clearing repo");
    FileIO.rm("repo");
    log.info("Repo.clear - {}", REPO_STATE_FILE_NAME);
    FileIO.rm(REPO_STATE_FILE_NAME);
    log.info("Repo.clear - clearing memory");
    installedLibraries.clear();
    log.info("clearing errors");
    clearErrors();
  }

  public void clearErrors() {
    errors.clear();
  }

  public void createBuildFiles() {
    ServiceData sd = ServiceData.getLocalInstance();
    createBuildFiles(null, sd.getServiceTypeNames());
  }

  public void createBuildFiles(String serviceType) {
    createBuildFiles(null, serviceType);
  }

  public void createBuildFiles(String location, String serviceType) {
    String[] types = null;
    if (serviceType == null) {
      ServiceData sd = ServiceData.getLocalInstance();
      types = sd.getServiceTypeNames();
    } else {
      types = new String[] { serviceType };
    }
    createBuildFiles(location, types);
  }

  public abstract void createBuildFiles(String location, String[] serviceType);

  public void createBuildFilesTo(String dir) {
    createBuildFiles(dir, (String) null);
  }

  public void createFilteredFile(Map<String, String> snr, String location, String filename, String ext) throws IOException {
    String ofn = getBuildFileName(location, filename, ext);
    FileOutputStream out = new FileOutputStream(ofn);

    String templateName = String.format("framework/%s.%s.template", filename, ext);
    String filered = filterFile(snr, templateName);
    log.info("writing file {}", ofn);
    out.write(filered.getBytes());
    out.close();
  }

  protected String createWorkDirectory(String location) {
    if (location == null) {
      location = ".";
    }

    log.info("creating work directory {}", location);
    File f = new File(location);
    f.mkdirs();

    return location;
  }

  public String filterFile(Map<String, String> snr, String templateName) {
    String settings = FileIO.resourceToString(templateName);
    ;
    for (String search : snr.keySet()) {
      settings = settings.replace(search, snr.get(search));
    }

    return settings;
  }

  public String getBuildFileName(String location, String filename, String ext) {
    StringBuilder sb = new StringBuilder(location);
    sb.append("/");
    sb.append(filename);
    /*
     * if (ts != null) { sb.append(String.format(".%d", ts)); }
     */
    sb.append(".");
    sb.append(ext);
    return sb.toString();
  }

  public String getFullTypeName(String serviceType) {
    if (serviceType == null) {
      return null;
    }

    if (!serviceType.contains(".")) {
      return String.format("org.myrobotlab.service.%s", serviceType);
    }

    return serviceType;
  }

  public Set<ServiceDependency> getUnfulfilledDependencies(String fullTypeName) {
    return getUnfulfilledDependencies(new String[] { fullTypeName });
  }

  public Set<ServiceDependency> getUnfulfilledDependencies(String[] types) {

    Set<ServiceDependency> ret = new LinkedHashSet<ServiceDependency>();

    for (String type : types) {
      if (!type.contains(".")) {
        type = String.format("org.myrobotlab.service.%s", type);
      }

      // get the dependencies required by the type
      ServiceData sd = ServiceData.getLocalInstance();
      if (!sd.containsServiceType(type)) {
        log.error("{} not found", type);
        return ret;
      }

      ServiceType st = sd.getServiceType(type);

      // look through our repo and resolve
      // if we dont have it - we need it
      List<ServiceDependency> metaDependencies = st.getDependencies();

      if (metaDependencies != null && metaDependencies.size() > 0) {
        for (ServiceDependency library : metaDependencies) {
          if (!installedLibraries.containsKey(library.toString())) {
            ret.add(library);
          } else {
            log.debug("previously installed - {}", library);
          }
        }
      }

      Map<String, ServiceReservation> peers = st.getPeers();
      if (peers != null) {
        for (String key : peers.keySet()) {
          ServiceReservation sr = peers.get(key);
          ret.addAll(getUnfulfilledDependencies(sr.fullTypeName));
        }
      }
    }

    return ret;
  }

  static public void publishStatus(Status status) {
    for (StatusPublisher service : installStatusPublishers) {
      service.broadcastStatus(status);
    }
  }

  static public void info(String format, Object... args) {
    publishStatus(Status.info(format, args));
  }

  synchronized public void install() {
    // if a runtime exits we'll broadcast we are starting to install
    ServiceData sd = ServiceData.getLocalInstance();
    info("starting installation of %s services", sd.getServiceTypeNames().length);
    install(sd.getServiceTypeNames());
    info("finished installing %d services", sd.getServiceTypeNames().length);
  }

  synchronized public void install(String serviceType) {

    String[] types = null;
    if (serviceType == null) {
      ServiceData sd = ServiceData.getLocalInstance();
      types = sd.getServiceTypeNames();
    } else {
      types = new String[] { serviceType };
    }

    install(DEFAULT_INSTALL_DIR, types);
  }

  synchronized public void install(String location, String serviceType) {
    install(location, new String[] { serviceType });
  }

  abstract public void install(String location, String[] serviceTypes);

  synchronized public void install(String[] serviceTypes) {
    install(DEFAULT_INSTALL_DIR, serviceTypes);
  }

  public void installEach() {
    String workDir = String.format(String.format("libraries.ivy.services.%d", System.currentTimeMillis()));
    installEachTo(workDir);
  }

  public void installEachTo(String location) {
    // if a runtime exits we'll broadcast we are starting to install
    ServiceData sd = ServiceData.getLocalInstance();
    String[] serviceNames = sd.getServiceTypeNames();
    info("starting installation of %d services", serviceNames.length);
    install(location, serviceNames);
    info("finished installing %d services", sd.getServiceTypeNames().length);
  }

  public void installTo(String location) {
    // if a runtime exits we'll broadcast we are starting to install
    ServiceData sd = ServiceData.getLocalInstance();
    info("starting installation of %s services", sd.getServiceTypeNames().length);
    install(location, sd.getServiceTypeNames());
    info("finished installing %d services", sd.getServiceTypeNames().length);
  }

  // FIXME - implement String[] serviceTypes parameter
  public boolean isInstalled(String serviceType) {
    return getUnfulfilledDependencies(serviceType).size() == 0;
  }

  /**
   * searches through dependencies directly defined by the service and all Peers
   * - recursively searches for their dependencies if any are not found -
   * returns false
   * 
   * @param fullTypeName
   *          f
   * @return true/false
   */
  public boolean isServiceTypeInstalled(String fullTypeName) {
    ServiceData sd = ServiceData.getLocalInstance();

    if (!sd.containsServiceType(fullTypeName)) {
      log.error("unknown service {}", fullTypeName);
      return false;
    }

    Set<ServiceDependency> libraries = getUnfulfilledDependencies(fullTypeName);
    if (libraries.size() > 0) {
      // log.info("{} is NOT installed", fullTypeName);
      return false;
    }

    // log.info("{} is installed", fullTypeName);
    return true;
  }

  /**
   * loads repo from a file
   */
  public void load() {
    try {

      File f = new File(REPO_STATE_FILE_NAME);
      if (f.exists()) {
        log.info("loading {}", REPO_STATE_FILE_NAME);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        FileInputStream is = new FileInputStream(f);
        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
          baos.write(data, 0, nRead);
        }

        baos.flush();
        baos.close();
        is.close();
        byte[] z = baos.toByteArray();
        if (z != null && z.length > 0) {
          installedLibraries = CodecUtils.fromJson(new String(z), TreeMap.class, String.class, ServiceDependency.class);
        }

      } else {
        log.info("{} not found", REPO_STATE_FILE_NAME);
      }

    } catch (Exception e) {
      log.error("loading threw", e);
    }

    log.info("loaded repo.json");
  }

  /**
   * saves repo to file
   */
  public void save() {
    try {
      FileOutputStream fos = new FileOutputStream(REPO_STATE_FILE_NAME);
      fos.write(CodecUtils.toJson(installedLibraries).getBytes());
      fos.close();
    } catch (Exception e) {
      log.error("save threw", e);
    }
  }

}
package org.myrobotlab.framework;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.zip.ZipFile;

import org.myrobotlab.config.ConfigUtils;
// Do not pull in deps to this class !
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * The purpose of this class is to retrieve all the detailed information
 * regarding the details of the current platform which myrobotlab is running.
 * 
 * It must NOT have references to mrl services, or Runtime, or 3rd party library
 * dependencies except perhaps for logging
 * 
 * FIXME - it's silly to have some values in variables and others in the
 * manifest map - probably should have all in a Tree map but I didn't want to
 * break any javascript which accessed the members directly
 *
 */
public class Platform implements Serializable {
  transient static Logger log = LoggerFactory.getLogger(Platform.class);

  private static final long serialVersionUID = 1L;

  // Nixie
  public static final String VERSION_PREFIX = "1.1.";

  // VM Names
  public final static String VM_DALVIK = "dalvik";
  public final static String VM_HOTSPOT = "hotspot";

  // OS Names
  public final static String OS_LINUX = "linux";
  public final static String OS_MAC = "mac";
  public final static String OS_WINDOWS = "windows";

  public final static String UNKNOWN = "unknown";

  // arch names
  public final static String ARCH_X86 = "x86";
  public final static String ARCH_ARM = "arm";

  // non-changing values
  String os;
  String arch;
  int osBitness;
  int jvmBitness;
  String lang = "java";
  String vmName;
  String vmVersion;
  String mrlVersion;

  String branch;

  String pid;
  String hostname;
  String commit;
  String build;
  String motd;
  Date startTime;

  // all values of the manifest
  Map<String, String> manifest;

  String shortCommit;

  static Platform localInstance;

  /**
   * The one big convoluted function to get all the crazy platform specific
   * data. Potentially, it's done once and only once for a running instance.
   * Most of the data should be immutable, although the "id"
   * 
   * All data should be accessed through public functions on the local instance.
   * If the local instance is desired. If its from a serialized instance, the
   * "getters" will be retrieving appropriate info for that serialized instance.
   *  
   * @return - return the local instance of the current platform
   */
  public static Platform getLocalInstance() {

    if (localInstance == null) {
      log.debug("initializing Platform");

      Platform platform = new Platform();
      platform.startTime = new Date();

      // === OS ===
      platform.os = System.getProperty("os.name").toLowerCase();
      if (platform.os.indexOf("win") >= 0) {
        platform.os = OS_WINDOWS;
      } else if (platform.os.indexOf("mac") >= 0) {
        platform.os = OS_MAC;
      } else if (platform.os.indexOf("linux") >= 0) {
        platform.os = OS_LINUX;
      }

      platform.vmName = System.getProperty("java.vm.name");
      platform.vmVersion = System.getProperty("java.specification.version");

      // === ARCH ===
      String arch = System.getProperty("os.arch").toLowerCase();
      if ("i386".equals(arch) || "i486".equals(arch) || "i586".equals(arch) || "i686".equals(arch)
          || "amd64".equals(arch) || arch.startsWith("x86")) {
        platform.arch = "x86"; // don't care at the moment
      }

      platform.osBitness = ("amd64".equals(arch)) ? 64 : 32;

      if ("arm".equals(arch)) {

        // assume ras pi 1 .
        Integer armv = 6;

        // if the os version has "v7" in it, it's a pi 2
        // TODO: this is still pretty hacky..
        String osVersion = System.getProperty("os.version").toLowerCase();
        if (osVersion.contains("v7")) {
          armv = 7;
        }
        // TODO: revisit how we determine the architecture version
        platform.arch = "armv" + armv + ".hfp";
      }

      // for Ordroid 64 !
      if ("aarch64".equals(arch)) {
        platform.arch = "armv8";
      }

      if (platform.arch == null) {
        platform.arch = arch;
      }

      // === BITNESS ===

      if (platform.isWindows()) {
        // https://blogs.msdn.microsoft.com/david.wang/2006/03/27/howto-detect-process-bitness/
        // this will attempt to guess the bitness of the underlying OS, Java
        // tries very hard to hide this from running programs
        String procArch = System.getenv("PROCESSOR_ARCHITECTURE");
        String procArchWow64 = System.getenv("PROCESSOR_ARCHITEW6432");
        platform.osBitness = (procArch != null && procArch.endsWith("64")
            || procArchWow64 != null && procArchWow64.endsWith("64")) ? 64 : 32;
        switch (arch) {
          case "x86":
          case "i386":
          case "i486":
          case "i586":
          case "i686":
            platform.jvmBitness = 32;
            break;
          case "x86_64":
          case "amd64":
            platform.jvmBitness = 64;
            break;
          default:
            platform.jvmBitness = 0; // ooops, I guess
            break;
        }
      } else {
        // this is actually a really bad way of doing jvm bitness (may return
        // "64","32" or "unknown") - and is sometimes simply not there at all
        // keeping this as a fallback for all Linux and Mac machines,
        // I don't know enough to implement a more robust detection for them
        // (and this was here before me, so it has to be good)
        String model = System.getProperty("sun.arch.data.model");
        platform.jvmBitness = "64".equals(model) ? 64 : 32;
      }

      // === MRL ===
      if (platform.mrlVersion == null) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
        platform.mrlVersion = format.format(new Date());
      }

      // manifest
      Map<String, String> manifest = getManifest();
      platform.manifest = manifest;
      platform.branch = get(manifest, "GitBranch", "unknownBranch");
      platform.commit = get(manifest, "GitCommitIdAbbrev", "unknownCommit");
      // build version or git commit timestamp
      platform.mrlVersion = get(manifest, "Implementation-Version", "unknownVersion");

      // git properties - local build has precedence
      Properties gitProps = gitProperties();
      if (gitProps != null) {
        String gitProp = gitProps.getProperty("git.branch");
        platform.branch = (gitProp != null) ? gitProp : platform.branch;

        gitProp = gitProps.getProperty("git.commit.id");
        platform.commit = (gitProp != null) ? gitProp : platform.commit;
        if (platform.commit != null) {
          platform.shortCommit = platform.commit.substring(0, 7);
        }
      }

      // motd
      platform.motd = "resistance is futile, we have cookies and robots ...";

      // hostname
      try {
        platform.hostname = InetAddress.getLocalHost().getHostName();
        if (platform.hostname != null) {
          platform.hostname = platform.hostname.toLowerCase();
        }
      } catch (Exception e) {
        platform.hostname = "localhost";
      }

      SimpleDateFormat TSFormatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
      platform.pid = TSFormatter.format(platform.startTime);

      try {

        // something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
        // but non standard across jvms & hosts
        // here we will attempt to standardize it - when asked for pid you
        // "only"
        // get pid ... if possible
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        int index = jvmName.indexOf('@');

        if (index > 1) {
          platform.pid = Long.toString(Long.parseLong(jvmName.substring(0, index)));
        } else {
          platform.pid = jvmName;
        }

      } catch (Exception e) {
      }

      localInstance = platform;
    }
    return localInstance;
  }

  static public String get(Map<String, String> manifest, String key, String def) {
    if (manifest != null & manifest.containsKey(key)) {
      return manifest.get(key);
    }
    return def;
  }

  static public Properties gitProperties() {
    try {
      Properties properties = new Properties();
      String rootOfClass = FileIO.getRoot();
      if (FileIO.isJar()) {
        // extract from jar
        log.info("git loading properties from jar {}", rootOfClass);
        properties.load(Platform.class.getResourceAsStream("/git.properties"));
      } else {

        // get from file system
        String path = FileIO.gluePaths(rootOfClass, "git.properties");
        File check = new File(path);
        if (!check.exists()) {
          log.info("git.properties does not exist");
          return null;
        }
        log.info("git loading from file {}", path);
        properties.load(new FileInputStream(path));
      }
      return properties;
    } catch (Exception e) {
      log.error("getProperties threw", e);
    }
    return null;
  }

  public Platform() {
  }

  /**
   * @return The process id of the currently running Java process
   * 
   */
  public String getPid() {
    return pid;
  }

  /**
   * @return The message of the day. "resistance is futile, we have cookies and
   *         robots ..."
   */
  public String getMotd() {
    return motd;
  }

  /**
   * @return The branch this software was built from.
   */
  public String getBranch() {
    return branch;
  }

  public String getBuild() {
    return build;
  }

  /**
   * @return This is the full commit of the source.
   */
  public String getCommit() {
    return commit;
  }

  /**
   * @return CPU Architecture x86, armv6, armv7, armv8
   */

  public String getArch() {
    return arch;
  }

  /**
   * @return Os bitness - should be 64 or 32
   */
  public int getOsBitness() {
    return osBitness;
  }

  /**
   * @return Java virtual machine bitness either 64 or 32 bit
   * 
   */
  public int getJvmBitness() {
    return jvmBitness;
  }

  /**
   * @return Operating system type linux, windows, mac
   * 
   */
  public String getOS() {
    return os;
  }

  /**
   * @return arc bitness and os together x86.64.linux, armv7.32.linux,
   *         x86.32.windows etc..
   * 
   */
  public String getPlatformId() {
    return String.format("%s.%s.%s", getArch(), getJvmBitness(), getOS());
  }

  /**
   * @return version or myrobotlab
   * 
   */
  public String getVersion() {
    return mrlVersion;
  }

  /**
   * @return Name of the Jvm Hotspot or OpenJDK typically
   *
   */
  public String getVMName() {
    return vmName;
  }

  public boolean isArm() {
    return getArch().startsWith(ARCH_ARM);
  }

  public boolean isDalvik() {
    return VM_DALVIK.equals(vmName);
  }

  public boolean isLinux() {
    return OS_LINUX.equals(os);
  }

  public boolean isMac() {
    return OS_MAC.equals(os);
  }

  public boolean isWindows() {
    return OS_WINDOWS.equals(os);
  }

  public boolean isX86() {
    return getArch().equals(ARCH_X86);
  }

  static public Map<String, String> getManifest() {
    Map<String, String> ret = new TreeMap<String, String>();
    ZipFile zf = null;
    try {
      log.debug("getManifest");
      String source = Platform.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
      InputStream in = null;
      log.debug("source {}", source);

      if (source.endsWith("jar")) {
        // runtime
        // DO NOT DO IT THIS WAY ->
        // Platform.class.getResource("/META-INF/MANIFEST.MF").openStream();
        // IT DOES NOT WORK WITH OpenJDK !!!
        zf = new ZipFile(source);
        in = zf.getInputStream(zf.getEntry("META-INF/MANIFEST.MF"));
        // zf.close(); explodes on closing :(
      } else {
        // IDE - version ...
        // in = new FileInputStream("target/classes/META-INF/MANIFEST.MF");//
        // Platform.class.getResource("target/classes/META-INF/MANIFEST.MF").openStream();
        in = new FileInputStream("target/classes/git.properties");// Platform.class.getResource("target/classes/META-INF/MANIFEST.MF").openStream();
      }
      // String manifest = FileIO.toString(in);
      // log.debug("loading manifest {}", manifest);
      Properties p = new Properties();
      p.load(in);

      for (final String name : p.stringPropertyNames()) {
        ret.put(name, p.getProperty(name));
      }

      for (final String name : ret.keySet()) {
        log.debug(name + "=" + p.getProperty(name));
      }

      in.close();
    } catch (Exception e) {
      e.printStackTrace();
      // log.warn("getManifest threw", e);
    } finally {
      if (zf != null) {
        try {
          zf.close();
        } catch (Exception e) {
        }
      }
    }
    return ret;
  }

  @Override
  public String toString() {
    return String.format("%s.%d.%s", arch, jvmBitness, os);
  }

  /**
   * @return The Computer's hostname
   */
  public String getHostname() {
    return hostname;
  }

  /**
   * @return the time when this instance was started
   * 
   */
  public Date getStartTime() {
    return startTime;
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init(Level.DEBUG);
      Platform platform = Platform.getLocalInstance();
      log.debug("platform : {}", platform.toString());
      log.debug("build {}", platform.getBuild());
      log.debug("branch {}", platform.getBranch());
      log.debug("commit {}", platform.getCommit());
      log.debug("toString {}", platform.toString());

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  public String getVmVersion() {
    return vmVersion;
  }
}
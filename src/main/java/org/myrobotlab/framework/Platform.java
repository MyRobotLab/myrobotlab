package org.myrobotlab.framework;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.myrobotlab.io.FileIO;
import org.myrobotlab.lang.NameGenerator;

/**
 * The purpose of this class is to retrieve all the detailed information
 * regarding the details of the current platform which myrobotlab is running.
 * 
 * It must NOT have references to mrl services, or Runtime, or 3rd party library
 * dependencies except perhaps for logging
 *
 */
public class Platform implements Serializable {

  private static final long serialVersionUID = 1L;
  // public static Logger log = LoggerFactory.getLogger(Platform.class);

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
  int bitness;
  String lang = "java";
  String vmName;
  String vmVersion;
  String mrlVersion;
  boolean isVirtual = false;

  /**
   * Static identifier to identify the "instance" of myrobotlab - similar to
   * network ip of a device and used in a similar way
   */
  String id;
  String branch;

  String pid;
  String hostname;
  String commit;
  String build;
  String motd;
  Date startTime;

  static Platform localInstance; // = getLocalInstance();

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
      Platform platform = new Platform();

      platform.startTime = new Date();

      // os
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

      // bitness
      String model = System.getProperty("sun.arch.data.model");
      if ("64".equals(model)) {
        platform.bitness = 64;
      } else {
        platform.bitness = 32;
      }

      // arch
      String arch = System.getProperty("os.arch").toLowerCase();
      if ("i386".equals(arch) || "i686".equals(arch) || "i586".equals(arch) || "amd64".equals(arch) || arch.startsWith("x86")) {
        platform.arch = "x86"; // don't care at the moment
      }

      if ("arm".equals(arch)) {

        // FIXME - procparser is unsafe and borked !!
        // Integer armv = ProcParser.getArmInstructionVersion();
        // Current work around: trigger off the os.version to choose
        // arm6 or arm7

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

      if (platform.mrlVersion == null) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
        platform.mrlVersion = format.format(new Date());
      }

      // manifest
      Map<String, String> manifest = getManifest();

      if (manifest.containsKey("GitBranch")) {
        platform.branch = manifest.get("GitBranch");
      } else {
        platform.branch = "unknownBranch";
      }

      if (manifest.containsKey("Commit")) {
        platform.commit = manifest.get("Commit");
      } else {
        platform.commit = "unknownCommit";
      }

      if (manifest.containsKey("Implementation-Version")) {
        platform.mrlVersion = manifest.get("Implementation-Version");
      } else {
        platform.mrlVersion = "unknownVersion";
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

  public Platform() {
  }

  public String getPid() {
    return pid;
  }

  public String getMotd() {
    return motd;
  }

  public String getBranch() {
    return branch;
  }

  public String getBuild() {
    return build;
  }

  public String getCommit() {
    return commit;
  }

  public String getArch() {
    return arch;
  }

  public int getBitness() {
    return bitness;
  }

  public String getOS() {
    return os;
  }

  public String getPlatformId() {
    return String.format("%s.%s.%s", getArch(), getBitness(), getOS());
  }

  public String getVersion() {
    return mrlVersion;
  }

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
    try {
      String source = Platform.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
      InputStream in = null;
      if (source.endsWith("jar")) {
        // runtime
        in = Platform.class.getResource("/META-INF/MANIFEST.MF").openStream();
      } else {
        // IDE - version ...
        in = Platform.class.getResource("/MANIFEST.MF").openStream();
      }
      Properties p = new Properties();
      p.load(in);
      
      for (final String name : p.stringPropertyNames()) {
        ret.put(name, p.getProperty(name));
      }
      in.close();
    } catch (Exception e) {
      e.printStackTrace();
      // log.warn("getManifest threw", e);
    }
    return ret;
  }

  private static Map<String, String> getAttributes(String part, Attributes attributes) {
    Map<String, String> data = new TreeMap<String, String>();
    Iterator<Object> it = attributes.keySet().iterator();
    while (it.hasNext()) {
      java.util.jar.Attributes.Name key = (java.util.jar.Attributes.Name) it.next();
      Object value = attributes.get(key);
      String partKey = null;
      if (part == null) {
        partKey = key.toString();
      } else {
        partKey = String.format("%s.%s", part, key);
      }

      // log.info( "{}: {}", value,partKey);
      if (value != null) {
        data.put(partKey, value.toString());
      }
    }
    return data;
  }

  @Override
  public String toString() {
    return String.format("%s.%d.%s", arch, bitness, os);
  }

  public String getId() {
    // null ids are not allowed
    if (id == null) {
      id = NameGenerator.getName();
    }
    return id;
  }

  public String getHostname() {
    return hostname;
  }

  public void setId(String newId) {
    id = newId;
  }

  public Date getStartTime() {
    return startTime;
  }

  public static boolean isVirtual() {
    Platform p = getLocalInstance();
    return p.isVirtual;
  }

  public static void setVirtual(boolean b) {
    Platform p = getLocalInstance();
    p.isVirtual = b;
  }

  public static void main(String[] args) {
    try {

      Platform platform = Platform.getLocalInstance();
      // log.info("platform : {}", platform.toString());
      // log.info("build {}", platform.getBuild());
      // log.info("branch {}", platform.getBranch());
      // log.info("commit {}", platform.getCommit());
      // log.info("toString {}", platform.toString());

    } catch (Exception e) {
      e.printStackTrace();
      // log.info("Exception: ", e);
    }
  }

  public boolean getVmVersion() {
    // TODO Auto-generated method stub
    return false;
  }
}
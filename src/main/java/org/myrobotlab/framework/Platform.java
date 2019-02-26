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
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

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
  String vmName;
  String vmVersion;
  String mrlVersion;

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
   * @return
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

      // REMOVED EVIL RECURSION - you can't call a file which has static
      // logging !!
      // logging calls -> platform calls a util class -> calls logging --
      // infinite loop

      /*
       * 
       * StringBuffer sb = new StringBuffer();
       * 
       * try { BufferedReader br = new BufferedReader(new
       * InputStreamReader(Platform.class.getResourceAsStream(
       * Util.getRessourceDir() + "/version.txt"), "UTF-8")); for (int c = br.read(); c != -1; c
       * = br.read()) { sb.append((char) c); } if (sb.length() > 0) {
       * platform.mrlVersion = sb.toString(); } } catch (Exception e) { // no
       * logging silently die }
       */

      if (platform.mrlVersion == null) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
        platform.mrlVersion = format.format(new Date());
      }

      // manifest
      Map<String, String> manifest = getManifest();

      if (manifest.containsKey("Branch")) {
        platform.branch = manifest.get("Branch");
      } else {
        platform.branch = "develop";
      }

      if (manifest.containsKey("Commit")) {
        platform.commit = manifest.get("Commit");
      } else {
        platform.commit = "unknown";
      }

      if (manifest.containsKey("Implementation-Version")) {
        platform.mrlVersion = manifest.get("Implementation-Version");
      } else {
        platform.mrlVersion = "unknown";
      }

      // motd
      platform.motd = "You Know, for Creative Machine Control !";

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

  public String getClassPathSeperator() {
    if (isWindows()) {
      return ";";
    } else {
      return ":";
    }
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

  static public final String getRoot() {
    try {

      String source = Platform.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
      System.out.println("getRoot " + source);
      return source;
    } catch (Exception e) {
      System.out.println("getRoot threw " + e.getMessage());
      return null;
    }
  }

  static public Map<String, String> getManifest() {
    Map<String, String> ret = new TreeMap<String, String>();
    try {
      File f = new File(getRoot());
      Class<?> clazz = Platform.class;
      String className = clazz.getSimpleName() + ".class";
      String classPath = clazz.getResource(className).toString();
      InputStream in = null;

      if (!classPath.startsWith("jar")) {
        System.out.println(String.format("manifest is \"not\" in jar - using file %s/META-INF/MANIFEST.MF", f.getAbsolutePath()));
        // File file = new
        // File(classLoader.getResource("file/test.xml").getFile());
        in = clazz.getResource("/META-INF/MANIFEST.MF").openStream();

      } else {
        String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
        URL url = new URL(manifestPath);
        System.out.println("jar url " + url);
        in = url.openStream();
      }

      Manifest manifest = new Manifest(in);
      ret.putAll(getAttributes(null, manifest.getMainAttributes()));
      final Map<String, Attributes> attrs = manifest.getEntries();
      Iterator<String> it = attrs.keySet().iterator();
      while (it.hasNext()) {
        String key = it.next();
        Attributes attributes = attrs.get(key);
        ret.putAll(getAttributes(key, attributes));
      }

      in.close();
    } catch (Exception e) {
      System.out.println(String.format("getManifest threw %s", e));
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

      System.out.println(partKey + ":  " + value);
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

  public static void main(String[] args) {
    try {

      Platform platform = Platform.getLocalInstance();
      System.out.println("platform : " + platform.toString());
      System.out.println("build " + platform.getBuild());
      System.out.println("branch " + platform.getBranch());
      System.out.println("commit " + platform.getCommit());
      System.out.println("toString " + platform.toString());

    } catch (Exception e) {
      System.out.println(e);
    }
  }

  public String getId() {
    return id;
  }

  public String getHostname() {
    return hostname;
  }

  /**
   * the one place where the "id" can be set
   * 
   * @param argument
   */
  public void setId(String newId) {
    id = newId;
  }

  public Date getStartTime() {
    return startTime;
  }

}
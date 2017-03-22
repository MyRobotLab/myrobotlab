package org.myrobotlab.framework;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

//import org.myrobotlab.logging.Logging;

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

  private String os;
  private String arch;
  private int bitness;
  private String vmName;
  private String mrlVersion;
  private String instanceId;
  private String branch;

  private static Platform localInstance = getLocalInstance();

  // -------------pass through begin -------------------
  public static Platform getLocalInstance() {
    if (localInstance == null) {
      Platform platform = new Platform();

      // os
      platform.os = System.getProperty("os.name").toLowerCase();
      if (platform.os.indexOf("win") >= 0) {
        platform.os = OS_WINDOWS;
      } else if (platform.os.indexOf("mac") >= 0) {
        platform.os = OS_MAC;
      } else if (platform.os.indexOf("linux") >= 0) {
        platform.os = OS_LINUX;
      }

      platform.vmName = System.getProperty("java.vm.name").toLowerCase();

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
      if ("aarch64".equals(arch)){
    	  platform.arch = "armv8";
      }

      if (platform.arch == null) {
        platform.arch = arch;
      }

      // REMOVED EVIL RECURSION - you can't call a file which has static
      // logging !!
      // logging calls -> platform calls a util class -> calls logging --
      // infinite loop
      // platform.mrlVersion = FileIO.getResourceFile("version.txt");
      StringBuffer sb = new StringBuffer();

      try {
        BufferedReader br = new BufferedReader(new InputStreamReader(Platform.class.getResourceAsStream("/resource/version.txt"), "UTF-8"));
        for (int c = br.read(); c != -1; c = br.read()) {
          sb.append((char) c);
        }
        if (sb.length() > 0) {
          platform.mrlVersion = sb.toString();
        }
      } catch (Exception e) {
        // no logging silently die
      }

      if (platform.mrlVersion == null) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
        platform.mrlVersion = format.format(new Date());
      }

      try {
        sb.setLength(0);
        BufferedReader br = new BufferedReader(new InputStreamReader(Platform.class.getResourceAsStream("/resource/branch.txt"), "UTF-8"));
        for (int c = br.read(); c != -1; c = br.read()) {
          sb.append((char) c);
        }
        if (sb.length() > 0) {
          platform.branch = sb.toString();
        }
      } catch (Exception e) {
        // no logging silently die
      }

      if (platform.branch == null) {
        platform.branch = "unknown";
      }

      // TODO - ProcParser

      // System.out.println(sb.toString());

      localInstance = platform;
    }

    return localInstance;
  }

  public Platform() {
  }

  public String getBranch() {
    return branch;
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

  public String getInstanceId() {
    return instanceId;
  }

  public void getInstanceId(String isntanceId) {
    this.instanceId = isntanceId;
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

  @Override
  public String toString() {
    return String.format("%s.%d.%s", arch, bitness, os);
  }

}

package org.myrobotlab.runtime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class ProcParser {

  static public final String CPU = "CPU";

  static public final String MEMORY = "MEMORY";

  static public final String DISK = "DISK";

  static public final String NETWORK = "NETWORK";

  /*
   * The pid of the process - not thread safe
   */
  static private int processPid = -1;

  /*
   * The line which the number of cpu cores is located in /proc/cpuinfo in
   * kernel 2.6.32-34-generic.
   */
  public static final int cpucoresline = 12;

  /*
   * Constant access path string. Those with 'pid' before are in /proc/[pid]/;
   * Those with 'net' are in /proc/net/. Those without are directly in /proc/.
   */
  public static final String pidStatmPath = "/proc/#/statm";

  public static final String pidStatPath = "/proc/#/stat";

  public static final String statPath = "/proc/stat";

  public static final String cpuinfoPath = "/proc/cpuinfo";

  public static final String meminfoPath = "/proc/meminfo";

  public static final String netdevPath = "/proc/net/dev";

  public static final String partitionsPath = "/proc/partitions";
  public static final String diskstatsPath = "/proc/diskstats";
  public static final String EMPTY = "";
  public static final String COLON = ":";
  public static final String SPACE = " ";
  public static final String SHARP = "#";
  public static final String LINE_SEPARATOR = "line.separator";

  public final static Logger log = LoggerFactory.getLogger(ProcParser.class);

  public static Integer getArmInstructionVersion() {
    String[] tempData = null;
    String[] tempFile = null;

    Integer ret = 6;

    // Parse /proc/cpuinfo to obtain how many cores the CPU has.
    String cpuInfo = getContents(cpuinfoPath);
    if (cpuInfo != null) {
      tempFile = cpuInfo.split(System.getProperty(LINE_SEPARATOR));

      for (String line : tempFile) {
        if (line.contains("Processor")) {
          tempData = line.split(COLON);
          break;
        }
      }
      if (tempData == null) {
        log.error("proc data not found - not a Linux system?");
        return null;
      }

      if (tempData.length == 2) {
        String idata = tempData[1];
        int pos0 = idata.indexOf("ARMv");
        if (pos0 > 0) {
          String vdata = idata.substring(pos0 + 4, pos0 + 5);
          try {
            ret = Integer.parseInt(vdata);
          } catch (Exception e) {
            Logging.logError(e);
          }
        }

      }
    }

    return ret;
  }

  /**
   * Fetch the entire contents of a text file, and return it in a String. This
   * style of implementation does not throw Exceptions to the caller.
   * 
   * @param path
   *          is a file which already exists and can be read.
   * @throws IOException
   */
  static private synchronized String getContents(String path) {
    // ...checks on aFile are elided
    StringBuilder contents = new StringBuilder();

    try {
      // use buffering, reading one line at a time
      // FileReader always assumes default encoding is OK!
      BufferedReader input = new BufferedReader(new FileReader(new File(path)));
      try {
        String line = null; // not declared within while loop
        /*
         * readLine is a bit quirky : it returns the content of a line MINUS the
         * newline. it returns null only for the END of the stream. it returns
         * an empty String if two newlines appear in a row.
         */
        while ((line = input.readLine()) != null) {
          contents.append(line);
          contents.append(System.getProperty(LINE_SEPARATOR));
        }
      } finally {
        input.close();

      }
    } catch (IOException e) {
      Logging.logError(e);
    }

    return contents.toString();
  }

  public static ArrayList<String> getCpuUsage() {
    BufferedReader br = null;
    ArrayList<String> data = new ArrayList<String>();
    String[] tempData;
    try {
      int numberOfCores = getNumberofCores();
      // Parse /proc/stat file and fill the member values list
      // We gonna parse de first line (total) and each line corresponding
      // to
      // one core
      // Line example: cpu0 311689 2102 654770 6755602 32431 38 4127 0 0 0
      br = getStream(statPath);
      // read a dummy line just for skip the total cpu line
      br.readLine();
      for (int core = 1; core <= numberOfCores; core++) {
        data.add(String.valueOf(core));
        tempData = br.readLine().split(SPACE);
        // Adds the first 9 fields.
        for (int field = 1; field < 10; field++) {
          data.add(tempData[field]);
        }
      }
      br.close();
    } catch (IOException e) {
      Logging.logError(e);
    }
    return data;
  }

  public static ArrayList<String> getDiskUsage() {
    ArrayList<String> partitionData = getPartitionUsage();
    ArrayList<String> data = new ArrayList<String>();
    String[] tempData = null;
    String[] tempFile = null;

    tempFile = getContents(diskstatsPath).split(System.getProperty(LINE_SEPARATOR));
    ArrayList<String> tempPart = getPartitionNames(partitionData);
    // Parse /proc/diskstats to obtain disk statistics

    for (String line : tempFile) {
      for (String partition : tempPart) {
        if (line.contains(SPACE + partition + SPACE)) {
          // split(SPACE);
          tempData = line.split(SPACE);
          // adds the rest of the disk statistics
          data.addAll(Arrays.asList(tempData));
          data.removeAll(Collections.singleton(EMPTY));
        }
      }
    }

    return data;
  }

  /**
   * @param _processPid the process id
   * @return memory usage information. Files: /proc/[pid]/statm /proc/[pid]/stat
   * 
   */
  public static ArrayList<String> getMemoryUsage(int _processPid) {
    BufferedReader br = null;
    ArrayList<String> data = new ArrayList<String>();
    String[] tempData = null;
    try {
      // Parse /proc/[pid]/statm file and fill the member values list with
      // its contents (all)
      tempData = getContents(pidStatmPath.replace(SHARP, String.valueOf(_processPid))).trim().split(SPACE);
      data.addAll(Arrays.asList(tempData));
      // Parse /proc/[pid]/stat file and fill the member values list just
      // with values 22, 23 and 24 (vsize, resident set size and resident
      // set size limit).
      tempData = getContents(pidStatPath.replace(SHARP, String.valueOf(_processPid))).trim().split(SPACE);
      data.add(tempData[22]);
      data.add(tempData[23]);
      data.add(tempData[24]);
      // Parse /proc/meminfo file for the system memory information.
      br = getStream(meminfoPath);
      for (int i = 0; i < 4; i++) {
        tempData = br.readLine().trim().split(SPACE);
        for (String s : tempData) {
          if (s.length() != 0 && CodecUtils.tryParseInt(s)) {
            data.add(s);
          }
        }
      }
      br.close();
    } catch (IOException e) {
      Logging.logError(e);
    }
    return data;
  }

  public static ArrayList<String> getNetworkUsage() {
    ArrayList<String> data = new ArrayList<String>();
    String[] tempData = null;
    String[] tempFile = null;

    tempFile = getContents(netdevPath).split(System.getProperty(LINE_SEPARATOR));
    // Skip the first two lines (headers)
    for (int i = 2; i < tempFile.length; i++) {
      // Parse /proc/net/dev to obtain network statistics.
      // Line e.g.:
      // lo: 4852 43 0 0 0 0 0 0 4852 43 0 0 0 0 0 0
      tempData = tempFile[i].replace(COLON, SPACE).split(SPACE);
      data.addAll(Arrays.asList(tempData));
      data.removeAll(Collections.singleton(EMPTY));
    }
    return data;
  }

  /**
   * DEPRECATE - why do this? Java can do this?
   * @return int
   * @throws FileNotFoundException e 
   * @throws IOException e
   * @throws NumberFormatException e 
   */
  public static int getNumberofCores() throws FileNotFoundException, IOException, NumberFormatException {
    String[] tempData = null;
    String[] tempFile = null;

    // Parse /proc/cpuinfo to obtain how many cores the CPU has.
    tempFile = getContents(cpuinfoPath).split(System.getProperty(LINE_SEPARATOR));
    for (String line : tempFile) {
      if (line.contains("cpu cores")) {
        tempData = line.split(COLON);
        break;
      }
    }
    return Integer.parseInt(tempData[1].trim());
  }

  /*
   * Create a list with the partitions name to be used to find their statistics
   * in /proc/diskstats file
   */
  public static ArrayList<String> getPartitionNames(ArrayList<String> data) {

    ArrayList<String> partitionsName = new ArrayList<String>();
    for (String string : data) {
      if (!CodecUtils.tryParseInt(string)) {
        partitionsName.add(string);
      }
    }
    return partitionsName;
  }

  public static ArrayList<String> getPartitionUsage() {
    ArrayList<String> data = new ArrayList<String>();
    String[] tempData = null;
    String[] tempFile = null;

    tempFile = getContents(partitionsPath).split(System.getProperty(LINE_SEPARATOR));

    // parse the disk partitions
    for (int i = 2; i < tempFile.length; i++) {
      tempData = tempFile[i].split(SPACE);
      data.addAll(Arrays.asList(tempData));
      data.removeAll(Collections.singleton(EMPTY));
    }
    return data;
  }

  /**
   * Opens a stream from a existing file and return it. This style of
   * implementation does not throw Exceptions to the caller.
   * 
   * @param path
   *          is a file which already exists and can be read.
   * @throws IOException
   */
  private synchronized static BufferedReader getStream(String _path) throws IOException {
    BufferedReader br = null;
    File file = new File(_path);
    FileReader fileReader = null;
    try {
      fileReader = new FileReader(file);
      br = new BufferedReader(fileReader);

    } catch (IOException e) {
      Logging.logError(e);
    }
    return br;
  }

  /*
   * Gathers the usage statistic from the /proc file system for CPU, Memory,
   * Disk and Network
   */
  static public ArrayList<String> getUsage(String uType) {
    if ((uType == null) || (processPid < 0)) {
      throw new IllegalArgumentException();
    }
    ArrayList<String> usageData = null;
    String type = uType.toUpperCase();

    if (type.equals("CPU")) {
      usageData = getCpuUsage();
    } else if (type.equals("MEMORY")) {
      usageData = getMemoryUsage(processPid);
    } else if (type.equals("DISK")) {
      usageData = getDiskUsage();
    } else if (type.equals("NETWORK")) {
      usageData = getNetworkUsage();
    }
    return usageData;
  }

  public static void main(String[] args) {
    LoggingFactory.init("DEBUG");

    Integer v = ProcParser.getArmInstructionVersion();
    log.info("{}", v);
    log.info("{}", v);
  }

  public int setPid(int pid) {
    processPid = pid;
    return pid;
  }
}// end ProcInfoParser

package org.myrobotlab.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Zip {

  final public static String RESOURCE = "RESOURCE";

  final public static String FILE = "FILE";

  static int BUFFER_SIZE = 2048;

  public final static Logger log = LoggerFactory.getLogger(Zip.class);

  public static int countOccurrences(String haystack, char needle) {
    int count = 0;
    for (int i = 0; i < haystack.length(); i++) {
      if (haystack.charAt(i) == needle) {
        count++;
      }
    }
    return count;
  }

  static public void extract(String resourcePath, String targetDirectory, String filter, String resourceType, boolean overwrite) throws IOException {

    if (targetDirectory != null) {

      if (!"/".equals(targetDirectory.substring(targetDirectory.length() - 1))) {
        targetDirectory = targetDirectory + "/";
      }

    }

    log.debug(String.format("extract (%s, %s, %s, %s, %b)", resourcePath, targetDirectory, filter, resourceType, overwrite));

    // String filter = "resource/";
    InputStream source = null;

    if (FILE.equals(resourceType)) {
      File check = new File(resourcePath);
      if (!check.exists() || check.isDirectory()){
    	  log.error("cannot extract self [{}]", resourcePath);
    	  return;
      }
      source = new FileInputStream(resourcePath);
    } else {
      source = ClassLoader.class.getResourceAsStream(resourcePath);
    }

    File target = new File(targetDirectory);

    if (!target.exists() && !target.mkdirs()) {
      throw new RuntimeException("Unable to create directory " + target.getAbsolutePath());
    }

    ZipInputStream in = new ZipInputStream(source);
    try {

      byte[] buffer = new byte[BUFFER_SIZE];

      for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
        log.info(entry.getName());

        if (filter == null || entry.getName().startsWith(filter)) {

          String filename = (filter==null)?entry.getName():entry.getName().substring(filter.length());
          // File file = new File(target, entry.getName());
          File file = new File(target, filename);

          log.debug("Extracted Resource = " + entry.getName());
          if (entry.isDirectory()) {
            file.mkdirs();
          } else {
            file.getParentFile().mkdirs();
            if (!file.exists() || overwrite) {
              OutputStream out = new FileOutputStream(file);
              try {
                int count;
                while ((count = in.read(buffer)) > 0) {
                  out.write(buffer, 0, count);
                }
              } finally {
                out.close();
              }
            }
            in.closeEntry();
          }
        }
      }
    } catch (IOException e) {
      log.error("", e);
      throw e;
    } finally {
      in.close();
    }
  }

  static public void extractFromFile(String filePath, String targetDirectory) throws IOException {
    extractFromFile(filePath, targetDirectory, null);
  }

  // e.g. Zip.extractFromFile("./myrobotlab.jar", "./", "resource");
  static public void extractFromFile(String resourcePath, String targetDirectory, String filter) throws IOException {
    extract(resourcePath, targetDirectory, filter, FILE, true);
  }

  static public void extractFromResource(String resourcePath, String targetDirectory) throws IOException {
    extract(resourcePath, targetDirectory, null, RESOURCE, true);
  }

  static public void extractFromResource(String resourcePath, String targetDirectory, String filter) throws IOException {
    extract(resourcePath, targetDirectory, filter, RESOURCE, true);
  }

  static public void extractFromSelf() throws IOException {
    extractFromSelf(".");
  }

  static public void extractFromSelf(String targetDirectory) throws IOException {
    extractFromFile(".", targetDirectory, null);
  }

  static public void extractFromSelf(String targetDirectory, String filter) throws URISyntaxException, IOException {
    // Logic to get currently executing JAR name
    String filePath = Zip.class.getProtectionDomain().getCodeSource().getLocation().toURI().toASCIIString();
    filePath = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.length());
    filePath = "./" + filePath;
    extractFromFile(filePath, targetDirectory, filter);
  }

  public static ArrayList<String> listDirectoryContents(String zipFile, String dir) throws ZipException, IOException {
    if (dir.charAt(dir.length() - 1) != '/') {
      dir = dir + "/";
    }
    log.info(String.format("listing %s directory %s", zipFile, dir));
    // int BUFFER = 2048;
    File file = new File(zipFile);
    ArrayList<String> children = new ArrayList<String>();

    ZipFile zip = new ZipFile(file);
    // String newPath = zipFile.substring(0, zipFile.length() - 4);
    ZipEntry zipDir = zip.getEntry(dir);
    if (zipDir == null) {
      log.error(String.format("%s not found", dir));
      // don't leak file handles.
      zip.close();
      return children;
    }

    if (!zipDir.isDirectory()) {
      log.error(String.format("%s not a directory", dir));
      // don't leak file handles.
      zip.close();
      return children;
    }

    Enumeration<?> zipFileEntries = zip.entries();

    // Process each entry
    while (zipFileEntries.hasMoreElements()) {
      // grab a zip file entry
      ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
      String currentEntry = entry.getName();

      if (currentEntry.startsWith(dir)) {
        String rest = currentEntry.substring(dir.length());
        if (rest.length() > 0) {
          // not subfiles of sub directories
          if ((!(rest.contains("/") && rest.charAt(rest.length() - 1) != '/')) &&
              // and not sub directories of sub directories
              (countOccurrences(rest, '/') < 2)) {
            // log.info(currentEntry);
            children.add(currentEntry.substring(dir.length()));
          }
        }
      }
      // !entry.isDirectory()
      // make sure we don't leak file handles.
      zip.close();
    }

    return children;

  }

  public static void main(String[] args) throws ZipException, IOException {

    LoggingFactory.init(Level.INFO);

    ArrayList<String> files = listDirectoryContents("myrobotlab.jar", "resource/Python/");
    for (int i = 0; i < files.size(); ++i) {
      log.info(files.get(i));
    }

  }

  static public void unzip(String zipFile, String newPath) throws ZipException, IOException {
    log.info(String.format("unzipping %s to %s", zipFile, newPath));
    int BUFFER = 2048;
    File file = new File(zipFile);

    ZipFile zip = new ZipFile(file);
    // String newPath = zipFile.substring(0, zipFile.length() - 4);

    new File(newPath).mkdir();
    Enumeration<?> zipFileEntries = zip.entries();

    // Process each entry
    while (zipFileEntries.hasMoreElements()) {
      // grab a zip file entry
      ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
      String currentEntry = entry.getName();
      File destFile = new File(newPath, currentEntry);
      // destFile = new File(newPath, destFile.getName());
      File destinationParent = destFile.getParentFile();

      // create the parent directory structure if needed
      destinationParent.mkdirs();

      if (!entry.isDirectory()) {
        BufferedInputStream is = new BufferedInputStream(zip.getInputStream(entry));
        int currentByte;
        // establish buffer for writing file
        byte data[] = new byte[BUFFER];

        // write the current file to disk
        FileOutputStream fos = new FileOutputStream(destFile);
        BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);

        // read and write until last byte is encountered
        while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
          dest.write(data, 0, currentByte);
        }
        dest.flush();
        dest.close();
        is.close();
      } else {
        destFile.mkdirs();
      }
      if (currentEntry.endsWith(".zip")) {
        // found a zip file, try to open
        unzip(destFile.getAbsolutePath(), "./");
      }
    }
    // don't leak file handles.
    zip.close();
  }

  // public static void main(String[] args) throws ZipException, IOException {
  // // TODO Auto-generated method stub
  // // extractFolder("ziptest.zip");
  // Log.init();
  // // extractFromResource("/resource/ziptest.zip", "binx");
  // // extractFromFile("./VivaClient.jar", "binx", "resource/");
  // extractFromSelf("/resource", "binz");
  // log.debug("done");
  //
  // }

}
/**
 *                    
 * @author grog (at) myrobotlab.org
 *   
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * FIXME - can probably start deprecating some of these methods in favor of
 * apache-io or guava or even path/files in java 8
 * 
 * Consider a resources.zip in the jar - then an "extract" just getResourceStream("resources/resource.zip") 
 * simply extracts a single zip. 
 * 
 * */
package org.myrobotlab.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipException;

import org.apache.commons.io.Charsets;
import org.myrobotlab.config.ConfigUtils;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

/**
 * class of useful utility functions we do not use nio - for portability reasons
 * e.g. Android
 * 
 * @author GroG
 *
 */
public class FileIO {

  public static class FileComparisonException extends Exception {
    private static final long serialVersionUID = 1L;

    public FileComparisonException(String msg) {
      super(msg);
    }
  }

  static public final Logger log = LoggerFactory.getLogger(FileIO.class);
  final static public String fs = File.separator;

  /**
   * compares two files - throws if they are not identical, good to use in
   * testing
   * 
   * @param filename1
   *          first file
   * @param filename2
   *          second file
   * @return true if they're equal
   * @throws FileComparisonException
   *           boom
   * @throws IOException
   *           boom
   * 
   */
  static public final boolean compareFiles(String filename1, String filename2) throws FileComparisonException, IOException {
    File file1 = new File(filename1);
    File file2 = new File(filename2);
    if (file1.length() != file2.length()) {
      throw new FileComparisonException(String.format("%s size is %d adn %s is size %d", filename1, file1.length(), filename2, file2.length()));
    }

    byte[] a1 = toByteArray(new File(filename1));
    byte[] a2 = toByteArray(new File(filename2));

    for (int i = 0; i < a1.length; ++i) {
      if (a1[i] != a2[i]) {
        throw new FileComparisonException(String.format("files differ at position %d", i));
      }
    }

    return true;
  }

  /**
   * Copy the contents of dir into the path destination s
   * 
   * @param dir
   *          source directories
   * @param path
   *          dest path
   * @throws IOException
   *           boom
   * 
   */

  final public static void copy(File[] dir, String path) throws IOException {
    for (File f : dir) {
      copy(f, new File(path));
    }
  }

  /**
   * A simple copy method which works like a 'regular' operating system copy
   * 
   * @param src
   *          source file
   * @param dst
   *          dest file
   * @throws IOException
   *           boom
   * 
   */
  static public final void copy(File src, File dst) throws IOException {
    log.info("copying from {} to {}", src, dst);
    if (!src.isDirectory()) {
      byte[] b = toByteArray(src);
      if (dst.exists() && dst.isDirectory()) {
        toFile(new File(gluePaths(dst.getPath(), src.getName())), b);
      } else {
        toFile(dst, b);
      }
    } else {
      if (!dst.exists()) {
        // if dst does not exist copy 'contents' of src into dst
        dst.mkdirs();
        log.info("directory copied from {} to {}", src, dst);
        String files[] = src.list();

        for (String file : files) {
          File srcFile = new File(src, file);
          File destFile = new File(dst, file);
          copy(srcFile, destFile);
        }
      } else {
        // when dst exists - then whole directory src is copied into dst
        copy(src, new File(dst.getPath() + File.separator + src.getName()));
      }

    }
  }

  /**
   * copy file or folder from one place to another with string interface
   * 
   * @param src
   *          source file
   * @param dst
   *          dest file
   * @throws IOException
   *           boom
   * 
   */
  static public final void copy(String src, String dst) throws IOException {
    copy(new File(src), new File(dst));
  }

  static public final boolean extract(String src, String dst) throws IOException {
    return extract(getRoot(), src, dst, true);
  }

  static public final boolean extract(String root, String src, String dst) throws IOException {
    return extract(root, src, dst, true);
  }

  /**
   * extract needs 3 parameters - a root file source, which could be a file
   * directory or jar file, a src location, and a destination. During runtime
   * this method will extract contents from a jar specified, during build time
   * it will copy from a folder location. It could probably be improved, and
   * simplified to take only 2 parameters, but that would mean calling methods
   * would need to be 'smart' enough to use full getURI notation .. e.g.
   * jar:file:/C:/proj/parser/jar/parser.jar!/test.xml
   * 
   * e.g
   * http://stackoverflow.com/questions/402683/how-do-i-get-just-the-jar-url-
   * from-a-jar-url-containing-a-and-a-specific-fi/402771#402771 final URL
   * jarUrl = new URL("jar:file:/C:/proj/parser/jar/parser.jar!/test.xml");
   * final JarURLConnection connection = (JarURLConnection)
   * jarUrl.openConnection(); final URL url = connection.getJarFileURL();
   * 
   * 
   * @param root
   *          - the jar file / or absolute file location .. e.g.
   *          file:/c:/somedir/myrobotlab.jar or file:/c:/somdir/bin
   * @param src
   *          - the folder or file to extract from the root
   * @param dst
   *          - target location
   * @param overwrite
   *          true/false to override
   * @return something
   * @throws IOException
   *           boom
   */
  static public final boolean extract(String root, String src, String dst, boolean overwrite) throws IOException {
    log.info("extract(root={}, src={}, dst={}, overwrite={})", root, src, dst, overwrite);

    boolean contents = false;
    boolean found = false;
    boolean firstMatch = true;

    // === pre-processing / normalizing of paths begin ===
    if (dst == null || dst.equals("") || dst.equals("./")) {
      dst = ".";
    }

    if (src == null || src.equals("") || src.equals("./")) {
      src = ".";
    }

    // normalize slash
    src = src.replace("\\", "\\\\");

    dst = dst.replace("\\", "\\\\");

    // normalize [from | from/ | from/*]
    String fromRoot = null;
    if (src != null && (src.endsWith("/") || src.endsWith("/*"))) {
      fromRoot = src.substring(0, src.lastIndexOf("/"));
      contents = true;
    } else {
      fromRoot = src;
    }

    // extract(/C:/mrl/myrobotlab/dist/myrobotlab.jar, resource, )
    log.info("normalized extract([{}], [{}], [{}])", root, src, dst);

    // === pre-processing / normalizing of paths end ===
    // FIXME - isJar(root)
    if (isJar(root)) {

      // String jarFile = getJarName();

      // if an exact file (not directory) is specified
      // we can immediately extract it
      // however for directories - there is no specific key given
      // and all the contents has to be iterated through :P

      String finalDst = gluePathsForwardSlash(dst, src);
      File check = new File(finalDst);
      if (check.exists() && !overwrite) {
        log.info("{} aleady exists - not extracting", finalDst);
        return false;
      }

      // trying direct key of resource first ....
      URL url = FileIO.class.getResource(src);
      if (url != null) {
        log.info("{} exists on the classpath - extracting to {}", url, dst);
        try {
          byte[] r = toByteArray(FileIO.class.getResourceAsStream(src));
          if (r != null) {
            log.info("resource not null - not a directory - extracting");
            FileIO.toFile(dst, r);
            return true;
          }
        } catch (Exception e) {
          log.info("returned {} assuming directory", e.getClass().getSimpleName());
        }
      }

      log.info("dir extracting from {}", root);

      JarFile jar = new JarFile(root);

      Enumeration<JarEntry> enumEntries = jar.entries();

      // jar access is non-recursive
      while (enumEntries.hasMoreElements()) {
        JarEntry file = enumEntries.nextElement();
        // log.debug(file.getName());

        // spin through resources until a match
        if (fromRoot != null && !file.getName().startsWith(fromRoot)) {
          // log.info(String.format("skipping %s", file.getName()));
          continue;
        }

        found = true;

        // our first match !
        if (!file.isDirectory()) {
          // not a directory
          String name = null;
          if (contents) {
            name = String.format("%s/%s", dst, file.getName().substring((fromRoot.length() + 1)));
          } else {
            if (firstMatch) {
              // file to file
              name = dst;
            } else {
              // dirFile to file
              name = String.format("%s/%s", dst, file.getName());
            }
          }

          log.debug("extracting {} to {}", file.getName(), name);
          // FIXING toFile(filename, data);
          // FIXING toByteArray(is)

          FileIO.toFile(name, toByteArray(jar.getInputStream(file)));

          // file to file copy ... done
          if (firstMatch) {
            break;
          }
        } else {
          // df
          // if (conti)
          String name = null;
          if (contents) {
            name = String.format("%s/%s", dst, file.getName().substring((fromRoot.length() + 1)));
          } else {
            name = String.format("%s/%s", dst, file.getName());
          }
          File d = new File(name);
          d.mkdirs();
        }

        firstMatch = false;
      }

      if (!found) {
        log.error("could not find {}", src);
      }

      jar.close();

      return found;
    }
    log.info("not extracting source is not a jar");
    return false;
  }

  /**
   * extractResources will extract the entire /resource directory out unless it
   * already exist
   * 
   * this process is important to the webgui, as it accesses the AngularJS files
   * from the file system and not within the jar
   * 
   * @return true/false
   */
  static public final boolean extractResources() {
    try {
      extract(getRoot(), "resource", null, false);
    } catch (Exception e) {
      Logging.logError(e);
    }
    return false;
  }

  /**
   * get configuration directory
   * 
   * @return string
   */
  static public final String getCfgDir() {
    try {

      String baseDir = null;
      if (Runtime.getOptions() == null) {
        baseDir = System.getProperty("user.dir");
      } else {
        baseDir = Runtime.DATA_DIR;
      }
      // TODO: is user.dir the same as MRL_HOME / install dir?
      // "always" associated with the data dir
      String dirName = baseDir + File.separator + ".myrobotlab";
      File dir = new File(dirName);

      if (!dir.exists()) {
        boolean ret = dir.mkdirs();
        if (!ret) {
          log.error("could not create {}", dirName);
        }
      }

      if (!dir.isDirectory()) {
        log.error("{} is not a file", dirName);
      }

      return dirName;

    } catch (Exception e) {
      log.error("getCfgDir threw", e);
    }
    return null;
  }

  /**
   * Method to universally get the root location of where mrl is currently
   * running form. It could be from Eclipse's bin directory, a build directory,
   * or inside a jar.
   * 
   * There are 100 rabbit holes, with the different ways you can 'attempt' to
   * get a path to where mrl is running. Some work during Develop-Time, and
   * return null during Runtime. Some encode or decode the path into something
   * which can not be used. Some appear simple superficially. This currently is
   * the best pratical solution I have found.
   * 
   * 2 Step process: 1. get URL of code executing 2. convert to a File
   * 
   * Test Results:
   * 
   * == Develop-Time Windows Eclipse == [/C:/mrlDevelop/myrobotlab/bin/]
   * 
   * == Run-Time Windows Jar == [/C:/mrl.test/current
   * 10/develop/myrobotlab.1.0.${env.TRAVIS_BUILD_NUMBER}.jar]
   * 
   * http://stackoverflow.com/questions/9729197/getting-current-class-name-
   * including-package
   * https://community.oracle.com/blogs/kohsuke/2007/04/25/how-convert-
   * javaneturl-javaiofile
   * 
   * @return string
   */
  static public final String getRoot() {
    try {

      String source = FileIO.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
      log.debug("getRoot {}", source);
      return source;
    } catch (Exception e) {
      log.error("getRoot threw");
      Logging.logError(e);
      return null;
    }
  }

  /**
   * "Clever" function to get the list of service based on class files. It is so
   * "clever" because dev-time may have source files, but test-time utilities
   * might not know where the source is. Runtime has no source.
   * 
   * A better solution might be to maintain a list of services as a text file :(
   * 
   * @return list of services
   * @throws IOException
   *           boom
   * 
   */
  static public final List<String> getServiceList() throws IOException {

    List<URL> urls = listContents(getRoot(), "org/myrobotlab/service", false, new String[] { ".*\\.class" },
        new String[] {} /* { ".*Test\\.class", ".*\\$.*" } */); // allowing all
    // services
    ArrayList<String> classes = new ArrayList<String>();
    log.info("found {} service files in {}", urls.size(), getRoot());
    // String path = packageName.replace('.', '/');
    for (URL url : urls) {
      String urlName = url.getPath();
      if (urlName.contains("$")) {
        // inner classes we are not interested in
        continue;
      }
      String simpleName = urlName.substring(urlName.lastIndexOf("/") + 1, urlName.lastIndexOf("."));
      String classname = String.format("org.myrobotlab.service.%s", simpleName);
      classes.add(classname);
    }
    log.info("found {} service classes", classes.size());
    return classes;
  }

  static public final boolean isJar() {
    return isJar(null);
  }

  static public final boolean isJar(String path) {
    if (path == null) {
      path = getRoot();
    }
    if (path != null) {
      return path.endsWith(".jar");
    }
    return false;
  }

  /**
   *
   * @param src
   *          the source
   * @return list the contents of 'self' at directory 'src'
   * @throws IOException
   *           boom
   * 
   */
  static public final List<URL> listContents(String src) throws IOException {
    return listContents(getRoot(), src, true, null, null);
  }

  static public final List<URL> listContents(String root, String src) throws IOException {
    return listContents(root, src, true, null, null);
  }

  /**
   * list the contents of a file system directory or list the contents of a jar
   * file directory
   * 
   * @param root
   *          the root
   * @param src
   *          source
   * @param recurse
   *          should it recurse
   * @param include
   *          include
   * @param exclude
   *          excludes
   * @return a list of urls
   * @throws IOException
   *           boom
   * 
   */
  static public final List<URL> listContents(String root, String src, boolean recurse, String[] include, String[] exclude) throws IOException {
    List<URL> classes = new ArrayList<URL>();
    log.info("findPackageContents root [{}], src [{}], recurse [{}], include [{}], exclude [{}]", root, src, recurse, Arrays.toString(include), Arrays.toString(exclude));

    if (root == null) {
      root = "./";
    }

    if (!isJar(root)) {
      String rootPath = root + File.separator + src;
      File srcFile = new File(rootPath);
      if (!srcFile.exists()) {
        log.error("{} does not exist", srcFile);
        return classes;
      }
      // MUST BE DIRECTORY !
      if (!srcFile.isDirectory()) {
        log.error("{} is not a directory", srcFile);
        return classes;
      }

      File[] files = srcFile.listFiles();
      for (File file : files) {
        if (file.isDirectory() && recurse) {
          assert !file.getName().contains(".");
          classes.addAll(listContents(rootPath, "/" + file.getName(), recurse, include, exclude));
        } else { // if (file.getName().endsWith(".class")) {
          String filename = file.getName();
          boolean add = false;
          if (include != null) {
            for (int i = 0; i < include.length; ++i) {
              if (filename.matches(include[i])) {
                add = true;
                break;
              }
            }
          }

          if (exclude != null) {
            for (int i = 0; i < exclude.length; ++i) {
              if (filename.matches(exclude[i])) {
                add = false;
                break;
              }
            }
          }

          if (include == null && exclude == null) {
            add = true;
          }

          if (add) {
            // IMPORTANT toURI().toURL() otherwise spaces can be
            // problems
            classes.add(file.toURI().toURL());
          }
        }
      }
    } else {

      ///////////////////////////////////////////////////////////////////////////////////////////////
      // FIXME - location of file will be different from path inside

      // local file
      // url=new URL("jar:file:/C:/dist/current/develop/myrobotlab.jar!/"

      // remote file
      // url=new
      // URL("jar:http://mrl-bucket-01.s3.amazonaws.com/current/develop/myrobotlab.jar!/");
      // URLcon=(JarURLConnection)(url.openConnection());
      // jar=URLcon.getJarFile();

      JarFile jar = new JarFile(root);
      Enumeration<JarEntry> enumEntries = jar.entries();

      /**
       * jar access requires a linear spin through all the entries :( and
       * unfortunately the paths are not ordered as a depth first ordering,
       * instead they are breadth first, so a 'group of files' in a 'directory'
       * can not be optimized in access - you are required to look through 'all'
       * entries because there is no order guarantee you will find them all in
       * one spot
       */
      while (enumEntries.hasMoreElements()) {
        JarEntry jarEntry = enumEntries.nextElement();

        // search for matching entries
        if (!jarEntry.getName().startsWith(src)) {
          // log.info(String.format("skipping %s", file.getName()));
          continue;
        }

        String urlStr = String.format("jar:file:%s!/%s", root, jarEntry.getName());
        boolean isSubDir = (src.split("/").length + 1 < jarEntry.getName().split("/").length);

        if (jarEntry.isDirectory() || (isSubDir && !recurse) || jarEntry.getName().contains("$")) {
          log.debug("filtering out {}", urlStr);
        } else {
          log.debug("adding url {}", urlStr);
          URL url = new URL(urlStr);
          classes.add(url);
        }
      }
      jar.close();
    }
    return classes;
  }

  static public final List<File> getFileList(String directory) throws IOException {
    return getFileList(directory, false, null, null);
  }

  static public final List<File> getFileList(String directory, boolean recurse) throws IOException {
    return getFileList(directory, recurse, null, null);
  }

  // TODO - include empty directories ?
  // TODO - include directories ?
  static public final List<File> getFileList(String directory, boolean recurse, String[] include, String[] exclude) throws IOException {
    List<File> list = new ArrayList<File>();

    // FIXME - security - filter out ../../.. prevent scanning of directories
    // not
    // sub to user.dir ?
    // String directory = gluePaths(System.getProperty("user.dir"), directory);

    File srcFile = new File(directory);
    if (!srcFile.exists()) {
      log.error("{} does not exist", directory);
      return list;
    }

    // MUST BE DIRECTORY !
    if (!srcFile.isDirectory()) {
      log.debug("{} is not a directory", directory);
      return list;
    }

    File[] files = srcFile.listFiles();
    for (File file : files) {
      if (file.isDirectory() && recurse) {
        // TODO - add if include empty directores ....
        List<File> subList = getFileList(file.getAbsolutePath(), recurse, include, exclude);
        list.addAll(subList);
      } else {
        boolean add = false;
        String filename = file.getName();
        if (include != null) {
          for (int i = 0; i < include.length; ++i) {
            if (filename.matches(include[i])) {
              add = true;
              break;
            }
          }
        }

        if (exclude != null) {
          for (int i = 0; i < exclude.length; ++i) {
            if (filename.matches(exclude[i])) {
              add = false;
              break;
            }
          }
        }

        if (include == null && exclude == null) {
          add = true;
        }

        if (add) {
          list.add(file);
        }

      }
    }

    return list;
  }
  /*
   * static public final void addFiles(List<File> allFiles, String directory,
   * boolean recurse, String[] include, String[] exclude) throws IOException {
   * 
   * File srcFile = new File(directory); if (!srcFile.exists()) {
   * log.error("{} does not exist"); }
   * 
   * // MUST BE DIRECTORY ! if (!srcFile.isDirectory()) {
   * log.error("{} is not a directory"); }
   * 
   * File[] files = srcFile.listFiles(); for (File file : files) { if
   * (file.isDirectory() && recurse) { // TODO - add if include empty directores
   * .... List<File> subFiles = listFiles(directory, recurse, include, exclude)
   * if (subFiles.size() > 0) { allFiles.addAll(c) } list.addAll(); } }
   * 
   * }
   * 
   */

  static public final List<File> listResourceContents(String path) throws IOException {
    List<URL> urls = null;
    String root = (path.startsWith("/")) ? "resource" : "resource/";
    urls = listContents(root + path);
    return UrlsToFiles(urls);
  }

  /**
   * inter process file communication - default is to wait and attempt to load a
   * file in the next second - it comes from savePartFile - then the writing of
   * the file from a different process should be an atomic move regardless of
   * file size
   * 
   * @param filename
   *          f
   * @return byte array
   * @throws IOException
   *           e
   */
  static public final byte[] loadPartFile(String filename) throws IOException {
    return loadPartFile(filename, 1000);
  }

  static public final byte[] loadPartFile(String filename, long timeoutMs) throws IOException {
    long startTs = System.currentTimeMillis();
    try {
      while (System.currentTimeMillis() - startTs < timeoutMs) {

        File file = new File(filename);
        if (file.exists()) {
          return toByteArray(file);
        }
        Thread.sleep(30);
      }

    } catch (Exception e) {
      throw new IOException("interrupted while waiting for file to arrive");
    }
    return null;
  }

  // FIXME - UNIT TESTS !!!
  public static void main(String[] args) throws ZipException, IOException {

    // foo://example.com:8042/over/there?name=ferret#nose
    // \_/ \______________/\_________/ \_________/ \__/
    // | | | | |
    // scheme authority path query fragment

    // File f = new File(uri.getPath()); - handle depending on scheme?

    LoggingFactory.init(Level.INFO);

    List<File> fileList = getFileList("InMoov", true);
    log.info("found {} files", fileList.size());

    FileIO.extract("/C:/mrl.test/current/myrobotlab.jar", "/resource/framework/serviceData.json", "C:\\mrl.test\\libraries\\serviceData.json");

    copy("dir1", "dir2");

    // JUNIT BEGIN
    String result = "/" + File.separator + "/a/";
    // assert /a/
    result = "/a/" + File.separator + "\\b\\";
    // assert /a/b/
    log.info(result);
    result = "/a" + File.separator + "\\b\\";
    // assert /a/b/
    result = "\\a\\" + File.separator + "\\b\\";
    // assert /a/b/
    result = "\\a\\" + File.separator + "b\\";
    // assert /a/b/

    try {

      // TODO - matrix of all file listing / url listings
      // TODO - test getRoot with spaces and utf-8
      // file:/C url string paths
      // file:jar:/mrlDevelop/dist
      // TODO - various other url path combos
      // TODO - make a jar - test it

      log.info("=== jar info begin ===");
      log.info("source url [{}]", FileIO.class.getProtectionDomain().getCodeSource().getLocation());
      log.info("source uri [{}]", FileIO.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      log.info("source path [{}]", FileIO.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
      log.info("=== jar info end ===");
      log.info("getRoot [{}]", getRoot());

      // interesting result ! - multiple definitions across many
      // jars are handled with this classloader's 'first jar match'
      URL url = FileIO.class.getResource("/com");
      log.info("{}", url);

      List<String> services = getServiceList();
      log.info("{}", services.size());

      URI uri = new URI("file:/c:/windows");
      File f = new File(uri);
      log.info("{} exists {}", uri, f.exists());

      uri = new URI("file:///c:/windows");
      f = new File(uri);
      log.info("{} exists {}", uri, f.exists());

    } catch (Exception e) {
      Logging.logError(e);
    }

  }

  /**
   * resource directory resource contents read into a byte array
   * 
   * @param src
   *          - location - (root is /resource) - e.g. src =
   *          Python/examples/someFile.py
   * @return byte array
   */
  static public final byte[] resourceToByteArray(String src) {

    log.info("looking for resource {}", src);
    InputStream isr = null;
    String resource = ConfigUtils.getResourceRoot();
    String localFilename = resource + File.separator + src;
    try {
      isr = new FileInputStream(localFilename);
    } catch (Exception e) {
      Logging.logError(e);
      log.error("file not found. {}", localFilename, e);
      return null;
    }

    byte[] data = null;
    try {
      data = toByteArray(isr);
    } finally {
      try {
        if (isr != null) {
          isr.close();
        }
      } catch (Exception e) {

      }
    }
    return data;
  }

  /**
   * resource directory resource contents read into a string
   * 
   * @param src
   *          - location - (root is /resource) - e.g. src =
   *          Python/examples/someFile.py
   * @return string
   */
  static public final String resourceToString(String src) {
    byte[] bytes = resourceToByteArray(src);
    if (bytes == null) {
      return null;
    }
    return new String(bytes);
  }

  /**
   * removes a file or recursively removes directory
   * 
   * @param file
   *          the file to remove
   * @return true/false if it was removed
   * 
   */
  static public final boolean rm(File file) {
    if (file.isDirectory())
      return rmDir(file, null);
    else {
      log.info("removing file {}", file.getAbsolutePath());
      return file.delete();
    }
  }

  /**
   * removes a file or recursively removes directory
   * 
   * @param filename
   *          f
   * @return true/false
   */
  static public final boolean rm(String filename) {
    return rm(new File(filename));
  }

  /**
   * recursively remove files and directories, leaving exlusions
   * 
   * @param directory
   *          - the directory to remove
   * @param exclude
   *          - the exceptions to save
   * @return true/false
   */
  static public final boolean rmDir(File directory, Set<File> exclude) {
    if (directory.exists()) {
      File[] files = directory.listFiles();
      if (null != files) {
        for (int i = 0; i < files.length; i++) {
          if (files[i].isDirectory()) {
            rmDir(files[i], exclude);
          } else {
            if (exclude != null && exclude.contains(files[i])) {
              log.info("skipping exluded file {}", files[i].getName());
            } else {
              log.info("removing file - {}", files[i].getAbsolutePath());
              files[i].delete();
            }
          }
        }
      }
    }

    boolean ret = (exclude != null) ? true : (directory.delete());
    return ret;
  }

  /**
   * for intra-process file writing &amp; locking ..
   * 
   * @param file
   *          f
   * @param data
   *          d
   * @throws IOException
   *           e
   */
  static public final void savePartFile(File file, byte[] data) throws IOException {
    // first delete any part or filename file currently there

    String filename = file.getName();

    if (file.exists()) {
      if (!file.delete()) {
        throw new IOException(String.format("%s exists but could not delete", filename));
      }
    }
    String partFilename = String.format("%s.part", filename);
    File partFile = new File(partFilename);
    if (partFile.exists()) {
      if (!partFile.delete()) {
        throw new IOException(String.format("%s exists but could not delete", partFilename));
      }
    }

    toFile(new File(partFilename), data);

    if (!partFile.renameTo(new File(filename))) {
      throw new IOException(String.format("could not rename %s to %s ..  don't know why.. :(", partFilename, filename));
    }

  }

  /**
   * simple file to byte array
   * 
   * @param file
   *          - file to read
   * @return byte array of contents
   * @throws IOException
   *           e
   */
  static public final byte[] toByteArray(File file) throws IOException {

    FileInputStream fis = null;
    byte[] data = null;

    fis = new FileInputStream(file);
    data = toByteArray(fis);

    fis.close();

    return data;
  }

  /**
   * @param is
   *          IntputStream to byte array
   * @return byte array
   */
  static public final byte[] toByteArray(InputStream is) {

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {

      int nRead;
      byte[] data = new byte[16384];

      while ((nRead = is.read(data, 0, data.length)) != -1) {
        baos.write(data, 0, nRead);
      }

      baos.flush();
      baos.close();
      return baos.toByteArray();
    } catch (Exception e) {
      Logging.logError(e);
    }

    return null;
  }

  /**
   * Copies bytes from src to dst, src must be a file, dst may or may not exist
   * 
   * @param src
   *          the source file
   * @param dst
   *          dest file
   * @throws IOException
   *           boom
   * 
   */
  static public void copyBytes(String src, String dst) throws IOException {
    FileInputStream fis = new FileInputStream(src);
    FileOutputStream fos = new FileOutputStream(dst);

    int nRead;
    byte[] data = new byte[65536];

    while ((nRead = fis.read(data, 0, data.length)) != -1) {
      fos.write(data, 0, nRead);
    }

    fis.close();
    fos.close();

  }

  static public void toFile(File dst, byte[] data) throws IOException {
    File p = dst.getParentFile();
    if (p != null) {
      p.mkdirs();
    }
    FileOutputStream fos = null;
    fos = new FileOutputStream(dst);
    fos.write(data);
    fos.close();
  }

  static public void toFile(String dst, byte[] data) throws IOException {
    toFile(new File(dst), data);
  }

  /**
   * String to file
   * 
   * @param filename
   *          - new file name
   * @param data
   *          - string data to save
   * @throws IOException
   *           e
   */
  static public final void toFile(String filename, String data) throws IOException {
    toFile(new File(filename), data.getBytes());
  }

  static public final String toString(File file) throws IOException {
    byte[] bytes = toByteArray(file);
    if (bytes == null) {
      return null;
    }
    return new String(bytes);
  }

  static public final String toString(String filename) throws IOException {
    return toString(new File(filename));
  }

  static public final List<File> UrlsToFiles(List<URL> urls) {
    List<File> files = new ArrayList<File>();
    for (int i = 0; i < urls.size(); ++i) {
      files.add(new File(urls.get(i).getPath()));
    }
    return files;
  }

  public void zip(String filename, List<File> files) throws IOException {
    zip(filename, files);
  }

  public void zip(String filename, List<File> files, String version) throws IOException {
    if (version == null) {
      version = "1.0";
    }

    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, version);
    JarOutputStream jar = new JarOutputStream(new FileOutputStream(filename), manifest);
    for (int i = 0; i < files.size(); ++i) {
      zipAdd(jar, new File("inputDirectory"));
    }

    jar.close();
  }

  public Map<String, String> getManifest() {
    File f = new File(getRoot());
    TreeMap<String, String> ret = new TreeMap<String, String>();

    if (!f.canRead()) {
      log.error(f.getAbsolutePath() + ": no such file");
      return ret;
    }
    try {
      JarFile jar = new JarFile(f);
      final Manifest manifest = jar.getManifest();
      final Attributes mattr = manifest.getMainAttributes();
      log.info(f.getAbsolutePath());
      log.info("Main attrs: ");
      for (Object a : mattr.keySet()) {
        String key = (String) a;
        String value = mattr.getValue(key);
        ret.put(key, value);
        log.info("\\t " + key + ": " + value);
      }
      log.info("\\nReading other attrs:\\n");

      final Map<String, Attributes> attrs = manifest.getEntries();
      for (String name : attrs.keySet()) {
        final Attributes attr = attrs.get(name);
        log.info(name + ": \\n");
        for (Object a : attr.keySet()) {
          String key = (String) a;
          String value = mattr.getValue(key);
          ret.put(key, value);
          log.info("\\t " + key + ": " + value);
        }
      }
      jar.close();
    } catch (Exception x) {
      log.error("Failed to read manifest for " + f.getAbsolutePath() + ": " + x);
    }

    return ret;
  }

  public void zipAdd(JarOutputStream jar, File src) throws IOException {
    BufferedInputStream in = null;
    try {
      if (src.isDirectory()) {
        String name = src.getPath().replace("\\", "/");
        if (!name.isEmpty()) {
          if (!name.endsWith("/"))
            name += "/";
          JarEntry entry = new JarEntry(name);
          entry.setTime(src.lastModified());
          jar.putNextEntry(entry);
          jar.closeEntry();
        }
        for (File nestedFile : src.listFiles())
          zipAdd(jar, nestedFile);
        return;
      }

      JarEntry entry = new JarEntry(src.getPath().replace("\\", "/"));
      entry.setTime(src.lastModified());
      jar.putNextEntry(entry);
      in = new BufferedInputStream(new FileInputStream(src));

      byte[] buffer = new byte[1024];
      while (true) {
        int count = in.read(buffer);
        if (count == -1)
          break;
        jar.write(buffer, 0, count);
      }
      jar.closeEntry();
    } finally {
      if (in != null)
        in.close();
    }
  }

  public static Properties loadProperties(String propertiesFile) throws IOException {
    Properties props = new Properties();
    InputStream in = new FileInputStream(propertiesFile);
    props.load(in);
    in.close();
    return props;
  }

  public static HashMap<String, String> loadPropertiesAsMap(String propertiesFile) throws IOException {
    Properties props = FileIO.loadProperties(propertiesFile);
    HashMap<String, String> propMap = new HashMap<String, String>();
    for (Object key : props.keySet()) {
      propMap.put(key.toString(), props.getProperty(key.toString()));
    }
    return propMap;
  }

  /**
   * Taken from Commons-io IOUtils
   * 
   * @param input
   *          the input file
   * @return the intput stream with default charset encoding.
   * 
   */
  public static InputStream toInputStream(String input) {
    return toInputStream(input, Charset.defaultCharset());
  }

  /**
   * Taken from Commons-io IOUtils
   * 
   * @param input
   *          the input file
   * @param encoding
   *          the input encoding
   * @return the input stream with encoding specified.
   * 
   */
  public static InputStream toInputStream(String input, Charset encoding) {
    return new ByteArrayInputStream(input.getBytes(Charsets.toCharset(encoding)));
  }

  /**
   * Taken from Commons-io IOUtils
   * 
   * @param input
   *          the input file
   * @param encoding
   *          target encoding to decode as
   * @return an input stream with encoding specified
   * @throws IOException
   *           boom
   * 
   */
  public static InputStream toInputStream(String input, String encoding) throws IOException {
    byte[] bytes = input.getBytes(Charsets.toCharset(encoding));
    return new ByteArrayInputStream(bytes);
  }

  public static String toString(InputStream inputStream) {
    byte[] bytes = toByteArray(inputStream);
    String ret = null;
    if (bytes != null) {
      ret = new String(bytes);
    }
    return ret;
  }

  public static String cleanFileName(String name) {
    return name.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
  }

  /**
   * Same as toString(filename) but does not throw - simply returns null in case
   * of error
   * 
   * @param filename
   *          - name of file
   * @return a string if successful otherwise null
   */
  public static String toSafeString(String filename) {
    try {
      return toString(filename);
    } catch (Exception e) {
      log.error("{} not found", filename);
    }
    return null;
  }

  /**
   * This method correctly glues paths together, with the result being a forward
   * path reference. Most references of file operations work correctly with
   * forward slash even on windows Backslash will fail inside a jar when
   * requesting resource stream references. Network access of course is forward
   * slash ... https://en.wikipedia.org/wiki/Backslash Bill Gates and IBM are
   * evil !
   * 
   * @param path1
   *          the first part of the path
   * @param path2
   *          the second part of the path
   * 
   * @return forward slash path
   */
  static public final String gluePathsForwardSlash(String path1, String path2) {

    path1 = path1.replace("\\", "/").trim();
    path2 = path2.replace("\\", "/").trim();
    // only 1 slash between path1 & path2
    if (path1.endsWith("/")) {
      path1 = path1.substring(0, path1.length() - 1);
    }
    if (path2.startsWith("/")) {
      path2 = path2.substring(1);
    }
    return String.format("%s/%s", path1, path2);
  }

  public static String gluePaths(String path1, String path2) {
    path1 = path1.replace("\\", FileIO.fs).trim();
    path2 = path2.replace("/", FileIO.fs).trim();
    // only 1 slash between path1 & path2
    if (path1.endsWith(FileIO.fs)) {
      path1 = path1.substring(0, path1.length() - 1);
    }
    if (path2.startsWith(FileIO.fs)) {
      path2 = path2.substring(1);
    }
    return String.format("%s%s%s", path1, FileIO.fs, path2);
  }

  public static String getExt(final String filename) {
    if (filename == null) {
      return null;
    }
    int pos = filename.lastIndexOf(".");
    if (pos > -1) {
      return filename.substring(pos + 1);
    }
    return null;
  }

  /**
   * validate a directory exists
   * 
   * @param dir
   * @return
   */
  public static boolean checkDir(String dir) {
    try {
      File check = new File(dir);
      return check.exists() && check.isDirectory();
    } catch (Exception e) {
      log.error("checkDir threw", e);
    }
    return false;
  }

  /**
   * validate a file exists
   * 
   * @param filename
   * @return
   */
  public static boolean checkFile(String filename) {
    try {
      File check = new File(filename);
      return check.exists() && !check.isDirectory();
    } catch (Exception e) {
      log.error("checkDir threw", e);
    }
    return false;
  }

  /**
   * flips all \ to / or / to \ depending on OS
   * 
   * @param dirPath
   *          - non normalized path
   * @return - fixed path
   */
  public static String normalize(String dirPath) {
    if (dirPath == null) {
      return null;
    }
    Platform platform = Platform.getLocalInstance();
    if (platform.isWindows()) {
      return dirPath.replace("/", "\\");
    } else {
      return dirPath.replace("\\", "/");
    }
  }
  
  public static boolean isExecutableAvailable(String command) {
    try {
        // Attempt to execute the command
        Process process = java.lang.Runtime.getRuntime().exec(command);

        // Check the exit value of the process
        // If the process has terminated correctly, the command is available
        if (process.waitFor() == 0) {
            return true;
        }

        // Read any errors from the attempted command
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        return false;
    } catch (IOException e) {
        log.info("IOException: " + e.getMessage());
        return false;
    } catch (InterruptedException e) {
        log.info("InterruptedException: " + e.getMessage());
        return false;
    }
}
  

}

/**
 * @author SwedaKonsult
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 */
package org.myrobotlab.reflection;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Class that allows for finding classes in files or packages.
 * 
 * @author SwedaKonsult
 * 
 */
public class Locator {
  /**
   * Recursive method used to find all classes in a given directory and subdirs.
   * 
   * @param directory
   *          Directory to start searching in
   * @param packageName
   *          The package name for classes found inside the base directory
   * @return The classes
   */
  public static List<Class<?>> findClasses(File directory, String packageName) {
    List<Class<?>> classes = new ArrayList<Class<?>>();
    try {
      if (!directory.exists()) {
        return classes;
      }
    } catch (SecurityException e) {
      // in case we don't have read access to the directory
      return classes;
    }
    File[] files = directory.listFiles();
    Class<?> clazz;
    String fileName;
    for (File file : files) {
      fileName = file.getName();
      // don't include hidden files
      if (fileName.startsWith(".")) {
        continue;
      }
      if (file.isDirectory()) {
        classes.addAll(findClasses(file, String.format("%s.%s", packageName, fileName)));
        continue;
      }
      if (!fileName.endsWith(".class")) {
        continue;
      }
      try {
        clazz = Class.forName(String.format("%s.%s", packageName, fileName.substring(0, fileName.length() - 6)));
      } catch (ClassNotFoundException e) {
        continue;
      } catch (ExceptionInInitializerError e) {
        continue;
      } catch (LinkageError e) {
        continue;
      }
      classes.add(clazz);
    }
    return classes;
  }

  /**
   * Scans all classes accessible from the context class loader which belong to
   * the given package and subpackages.
   * 
   * @param packageName
   *          The package to scan
   * @return Located Classes
   * @throws IOException
   *           if the path based on the packageName is not valid
   */
  public static List<Class<?>> getClasses(String packageName) throws IOException {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    assert classLoader != null;
    String path = packageName.replace('.', '/');
    Enumeration<URL> resources = classLoader.getResources(path);
    List<File> dirs = new ArrayList<File>();
    while (resources.hasMoreElements()) {
      URL resource = resources.nextElement();
      dirs.add(new File(resource.getFile()));
    }
    ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
    for (File directory : dirs) {
      classes.addAll(findClasses(directory, packageName));
    }
    return classes;
  }
}

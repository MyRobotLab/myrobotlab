package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

public class ServiceInterfaceTest {

  public final static Logger log = LoggerFactory.getLogger(ServiceInterfaceTest.class);

  @Test
  public final void testInstallAllServices() throws ClassNotFoundException, ParseException, IOException {
    // TODO: this probably is going to take longer but it's worth while!

    ServiceData sd = ServiceData.getLocalInstance();// CodecUtils.fromJson(FileUtils.readFileToString(new
                                                    // File("../repo/serviceData.json")),
                                                    // ServiceData.class);
    for (ServiceType st : sd.getServiceTypes()) {
      if (!st.isAvailable()) {
        log.info("Installing Service:" + st.getName());
        Runtime.install(st.getName());
      } else {
        log.info("already installed.");
      }
    }

  }

  @Test
  public final void testAllServices() throws ClassNotFoundException {

    ArrayList<String> servicesWithoutWebPages = new ArrayList<String>();
    ArrayList<String> servicesWithoutScripts = new ArrayList<String>();
    ArrayList<String> servicesThatDontStartProperly = new ArrayList<String>();

    ArrayList<String> servicesNotInServiceDataJson = new ArrayList<String>();

    HashSet<String> whiteListServices = new HashSet<String>();
    // CLI seems to mess up the console in the unit test so things
    // don't log well anymore.
    whiteListServices.add("Cli");
    // gui service will probably blow up if you are running in a console.
    whiteListServices.add("GUIService");
    // leap motion blows up because java.libary.path not having the leap deps.
    whiteListServices.add("LeapMotion");
    // jna lib path stuff
    whiteListServices.add("OculusRift");
    // plantoid gets a null pointer on tracking service start
    whiteListServices.add("Plantoid");
    // Shoutbox gets an address in use/failed to bind to port 8888
    whiteListServices.add("Shoutbox");
    // jni class path error
    whiteListServices.add("SLAMBad");
    // WebGUI gets an address in use/failed to bind to port 8888
    whiteListServices.add("WebGui");

    // dependencies missing in repo
    whiteListServices.add("MyoThalmic");
    // NPE exception.
    whiteListServices.add("Tracking");

    // NPE Exception in serial service?
    whiteListServices.add("EddieControlBoard");
    // NPE in serial
    whiteListServices.add("GPS");
    // NPE in regex
    whiteListServices.add("Lidar");
    // starts a webgui and gets bind error
    whiteListServices.add("PickToLight");
    // NPE in serial
    whiteListServices.add("Sabertooth");
    // NPE in serial
    whiteListServices.add("VirtualDevice");
    whiteListServices.add("OpenNI");

    // start up python so we have it available to do some testing with.
    Python python = (Python) Runtime.createAndStart("python", "Python");
    String testScriptDirectory = "./src/resource/Python/examples/";
    List<String> servicesToTest = listAllServices();
    // List<String> servicesToTest = new ArrayList<String>();
    // servicesToTest.add("Cli");

    // Load the service data file

    // TODO: read this from the repo at build time?
    ServiceData sd = ServiceData.getLocalInstance();

    int numServices = servicesToTest.size();
    int numServicePages = 0;
    int numScripts = 0;
    int numScriptsWorky = 0;
    int numStartable = 0;
    log.info("----------------------------------------------");
    for (String service : servicesToTest) {
      System.out.println("SYSTEM TESTING " + service);
      System.out.flush();
      if (whiteListServices.contains(service)) {
        log.info("White listed testing of service {}", service);
        continue;
      }
      log.info("Testing Service: {}", service);

      ServiceType st = sd.getServiceType("org.myrobotlab.service." + service);
      if (st == null) {
        System.out.println("NO SERVICE TYPE FOUND!");
        servicesNotInServiceDataJson.add(service);
      } else {
        System.out.println("Service Type Found!");
      }
      // System.out.flush();
      // try {
      // System.in.read();
      // } catch (IOException e) {
      // // TODO Auto-generated catch block
      // e.printStackTrace();
      // }

      //
      if (serviceHasWebPage(service)) {
        log.info("Service {} has a web page..", service);
        numServicePages++;
      } else {
        log.warn("Service {} does not have a web page..", service);
        servicesWithoutWebPages.add(service);
      }

      if ("Gps".equals(service)) {
        log.info("here");
      }

      if (serviceInterfaceTest(service)) {
        numStartable++;
      } else {
        servicesThatDontStartProperly.add(service);
      }

      // validate that a script exists
      File script = new File(testScriptDirectory + service + ".py");
      if (script.exists()) {
        log.info("Service Has a Script: {}", service);
        numScripts++;
      } else {
        log.warn("Missing Script for Service {}", service);
        servicesWithoutScripts.add(service);
      }

      //
      if (testServiceScript(python, testScriptDirectory, service)) {
        // log.info("Default script for {} executes ok!", service);
        numScriptsWorky++;
      } else {
        // log.warn("Default script for {} blows up!", service);
        // servicesWithoutWorkyScript(service);
      }
      // log.info("SERVICE TESTED WAS :" + service);
      log.info("----------------------------------------------");
      // System.out.println("Next?");
      // try {
      // System.in.read();
      // } catch (IOException e) {
      // // TODO Auto-generated catch block
      // e.printStackTrace();
      // }

    }

    log.info("----------------------------------------------");
    log.info("Service Report");
    log.info("Number of Services:           {}", numServices);
    log.info("Number of Startable Services: {}", numStartable);
    log.info("Number of Services Pages      {}", numServicePages);
    log.info("Number of Scripts:            {}", numScripts);
    log.info("Number of Scripts Worky:      {}", numScriptsWorky);
    log.info("----------------------------------------------");

    for (String s : servicesThatDontStartProperly) {
      log.info("FAILED ON START:" + s);
    }

    for (String s : servicesWithoutWebPages) {
      log.info("NO WEB PAGE    :" + s);
    }

    for (String s : servicesWithoutScripts) {
      log.info("NO SCRIPT      :" + s);
    }

    for (String s : servicesNotInServiceDataJson) {
      log.info("NOT IN SERVICE DATA :" + s);
    }

    log.info("Done...");

  }

  private boolean serviceInterfaceTest(String service) {
    // see if we can start/stop and release the service.

    ServiceInterface foo = Runtime.create(service.toLowerCase(), service);
    if (foo == null) {
      log.warn("Runtime Create returned a null service for {}", service);
      return false;
    }
    System.out.println("Service Test:" + service);
    System.out.flush();
    // Assert.assertNotNull(foo.getCategories());
    Assert.assertNotNull(foo.getDescription());
    Assert.assertNotNull(foo.getName());
    Assert.assertNotNull(foo.getSimpleName());
    Assert.assertNotNull(foo.getType());

    // TODO: add a bunch more tests here!
    foo.startService();
    foo.stopService();
    foo.releaseService();

    foo.startService();
    foo.save();
    foo.load();
    foo.stopService();

    return true;
  }

  private boolean testServiceScript(Python python, String testScriptDirectory, String service) {

    // TODO: this blows stuff up too much.

    String testScriptFile = testScriptDirectory + service + ".py";
    File script = new File(testScriptFile);
    if (!script.exists()) {
      log.warn("No default script for Service {}", script);
      return false;
    } else {
      log.info("Default Script Exists for {}", service);
    }

    return false;

    // dead code
    // if (true) {
    // // Diabled testing of scripts currently.
    // return false;
    // }
    // try {
    // String test = FileUtils.readFileToString(script);
    // System.out.println(test);
    //
    // python.execAndWait(test);
    // } catch (IOException e) {
    // // TODO Auto-generated catch block
    // log.warn("Error Running script {}" , script);
    // e.printStackTrace();
    //
    // return false;
    // }
    // return true;
  }

  private boolean serviceHasWebPage(String service) {
    String url = "http://www.myrobotlab.org/service/" + service;
    InputStream in = null;
    try {
      in = new URL(url).openStream();
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      // e.printStackTrace();
      return false;
    } catch (IOException e) {
      // TODO Auto-generated catch block
      // e.printStackTrace();
      return false;
    }

    try {
      // read the page (we don't care about contents. (yet))
      IOUtils.toString(in);
    } catch (IOException e) {
      // e.printStackTrace();
      return false;
    } finally {
      IOUtils.closeQuietly(in);
    }
    return true;
  }

  public static List<String> listAllServices() throws ClassNotFoundException {
    // TODO: should this be replaced with a call to Runtime ?
    List<Class<?>> classes = ServiceInterfaceTest.getClassesForPackage("org.myrobotlab.service");
    List<String> services = new ArrayList<String>();
    for (Class<?> c : classes) {
      // System.out.println("CLASS:" + c.toString());
      HashSet<String> superClasses = new HashSet<String>();
      Class x = c;
      while (true) {
        if (x.getSuperclass() != null) {
          superClasses.add(x.getSuperclass().toString());
          x = x.getSuperclass();
        } else {
          break;
        }

      }
      if (superClasses.contains("class org.myrobotlab.framework.Service")) {
        // Get just the class name.
        String[] parts = c.toString().split(" ")[1].split("\\.");
        services.add(parts[parts.length - 1]);
        // System.out.println(parts[parts.length-1]);
      }
    }
    return services;
  }

  /**
   * Attempts to list all the classes in the specified package as determined by
   * the context class loader
   * 
   * @param pckgname
   *          the package name to search
   * @return a list of classes that exist within that package
   * @throws ClassNotFoundException
   *           if something went wrong
   * 
   *           Ref:
   *           http://stackoverflow.com/questions/1498122/java-loop-on-all-the-
   *           classes-in-the-classpath
   */
  private static List<Class<?>> getClassesForPackage(String pckgname) throws ClassNotFoundException {
    // This will hold a list of directories matching the pckgname. There may be
    // more than one if a package is split over multiple jars/paths
    ArrayList<File> directories = new ArrayList<File>();
    try {
      ClassLoader cld = Thread.currentThread().getContextClassLoader();
      if (cld == null) {
        throw new ClassNotFoundException("Can't get class loader.");
      }
      String path = pckgname.replace('.', '/');
      // Ask for all resources for the path
      Enumeration<URL> resources = cld.getResources(path);
      while (resources.hasMoreElements()) {
        directories.add(new File(URLDecoder.decode(resources.nextElement().getPath(), "UTF-8")));
      }
    } catch (NullPointerException x) {
      throw new ClassNotFoundException(pckgname + " does not appear to be a valid package (Null pointer exception)");
    } catch (UnsupportedEncodingException encex) {
      throw new ClassNotFoundException(pckgname + " does not appear to be a valid package (Unsupported encoding)");
    } catch (IOException ioex) {
      throw new ClassNotFoundException("IOException was thrown when trying to get all resources for " + pckgname);
    }

    ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
    // For every directory identified capture all the .class files
    for (File directory : directories) {
      if (directory.exists()) {
        // Get the list of the files contained in the package
        String[] files = directory.list();
        for (String file : files) {
          // we are only interested in .class files
          if (file.endsWith(".class")) {
            // removes the .class extension
            try {
              classes.add(Class.forName(pckgname + '.' + file.substring(0, file.length() - 6)));
            } catch (NoClassDefFoundError e) {
              // do nothing. this class hasn't been found by the loader, and we
              // don't care.
            }
          }
        }
      } else {
        throw new ClassNotFoundException(pckgname + " (" + directory.getPath() + ") does not appear to be a valid package");
      }
    }
    return classes;
  }

}

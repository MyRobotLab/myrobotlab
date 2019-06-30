package org.myrobotlab.framework.repo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

/**
 * ServiceData class contains all of the Services meta data. This includes : 1.
 * Dependency information - what libraries are needed to run the class 2.
 * Categories of the service 3. Peers of the service
 * 
 * All this information is Service "type" related - non of it is instance
 * specific. ServiceData has to be created during "build" time since most of the
 * services contain dependencies which might not be fulfilled during runtime.
 * 
 * ServiceData.generate() creates serviceData.json which is packaged by the
 * build in : /resource/framework/serviceData.json
 * 
 * When MyRobotLab runs for the first time, it will extract this file into the
 * .myrobotlab directory.
 * 
 * @author GroG
 *
 */
public class ServiceData implements Serializable {

  private static final long serialVersionUID = 1L;

  transient public final static Logger log = LoggerFactory.getLogger(ServiceData.class);

  /**
   * all services meta data is contained here
   */
  TreeMap<String, ServiceType> serviceTypes = new TreeMap<String, ServiceType>();

  /**
   * the set of all categories
   */
  TreeMap<String, Category> categoryTypes = new TreeMap<String, Category>();

  static private ServiceData localInstance = null;

  static private String serviceDataCacheFileName = FileIO.getCfgDir() + File.separator + "serviceData.json";

  static public ServiceData getLocalInstance() {
    if (localInstance != null) {
      // return the already loaded copy.
      return localInstance;
    } else {

      // step 1 - try local file in the .myrobotlab directory
      // step 2 - extract the file from the jar
      // WE CAN NOT GENERATE THIS FILE DURING RUNTIME !!!

      // step 3 - if 1 & 2 fail - then we can 'assume' were in develop
      // time (we'll isJar check and error if not)
      // - generate it and put it in
      // getRoot()/resource/framework/serviceData.json


      // if we're not in a jar we are in an IDE.

      // First check the .myrobotlab/serviceData.json dir.
      File jsonFile = new File(serviceDataCacheFileName);
      if (jsonFile.exists() ) {
        // load it and return!
        String data = null;
        try {
          data = FileIO.toString(jsonFile);
        } catch (IOException e) {
          log.warn("Error reading serviceData.json from location {}", jsonFile.getAbsolutePath());
        }
        localInstance = CodecUtils.fromJson(data, ServiceData.class);
        log.info("Returning serviceData.json from {}", jsonFile);
        return localInstance;
      } else {
        // it didn't exist, lets either load it from the jar or generate it!
        if (FileIO.isJar()) {
          log.info("Extracting serviceData.json from myrobotlab.jar");
          // extract it from the jar.
          String extractFrom = "/resource/framework/serviceData.json";
          jsonFile.getParentFile().mkdirs();
          try {
            FileIO.extract(extractFrom, jsonFile.getAbsolutePath());
          } catch (IOException e) {
            log.warn("Error extracting serviceData.json from myrobotlab.jar", e);
          }

          // at this point we should be able to load it from the extracted serviceData.json
          String data = null;
          try {
            data = FileIO.toString(jsonFile);
          } catch (IOException e) {
            log.warn("Error reading serviceData.json from location {}", jsonFile.getAbsolutePath());
          }
          localInstance = CodecUtils.fromJson(data, ServiceData.class);
          return localInstance;

        } else {
          // we are running in an IDE and haven't generated/saved the serviceData.json yet.
          try {
            // This must only be run as part of the build or from your IDE.  It will not work when running from a jar.
            localInstance = ServiceData.generate();
            localInstance.save();
            log.info("saved generated serviceData.json to {}", serviceDataCacheFileName);
          } catch (IOException e1) {
            log.error("Unable to generate the serivceData.json file!!");
            // This is a fatal issue. I think we should exit the jvm here.
          }
          return localInstance;
        }
      }
    }
  }

  /**
   * This method has to check the environment first in order to tell if its
   * Develop-Time or Run-Time because the method of generating a service list is
   * different depending on current environment
   * 
   * Develop-Time can simply filter and process the files on the file system
   * given by the code source location
   * 
   * Run-Time must extract itself and scan/filter zip entries which is
   * potentially a lengthy process, and should only have to be done once for the
   * lifetime of the version or mrl
   * 
   * @return the service data description
   * @throws IOException
   *           e
   */
  static public synchronized ServiceData generate() throws IOException {
    log.info("================ generating serviceData.json begin ================");
    ServiceData sd = new ServiceData();

    // get services - all this could be done during Runtime
    // although running through zip entries would be a bit of a pain
    // Especially if you have to spin through 50 megs of data
    List<String> services = FileIO.getServiceList();

    log.info("found {} services", services.size());
    for (int i = 0; i < services.size(); ++i) {

      String fullClassName = services.get(i);
      log.debug("querying {}", fullClassName);
      try {
        Class<?> theClass = Class.forName(fullClassName);
        Method method = theClass.getMethod("getMetaData");
        ServiceType serviceType = (ServiceType) method.invoke(null);

        if (!fullClassName.equals(serviceType.getName())) {
          log.error("Class name {} not equal to the ServiceType's name {}", fullClassName, serviceType.getName());
        }

        sd.add(serviceType);

        for (String cat : serviceType.categories) {
          Category category = null;
          if (serviceType.isAvailable()) {
            if (sd.categoryTypes.containsKey(cat)) {
              category = sd.categoryTypes.get(cat);
            } else {
              category = new Category();
              category.name = cat;
            }
            category.serviceTypes.add(serviceType.getName());
            sd.categoryTypes.put(cat, category);
          }
        }

      } catch (Exception e) {
        log.error("{} does not have a static getMetaData method", fullClassName);
      }
    }
    log.info("================ generating serviceData.json end ================");

    return sd;
  }

  public ServiceData() {
  }

  public void add(ServiceType serviceType) {
    serviceTypes.put(serviceType.getName(), serviceType);
  }

  public boolean containsServiceType(String fullServiceName) {
    return serviceTypes.containsKey(fullServiceName);
  }

  public List<ServiceType> getAvailableServiceTypes() {
    ArrayList<ServiceType> ret = new ArrayList<ServiceType>();
    for (Map.Entry<String, ServiceType> o : serviceTypes.entrySet()) {
      if (o.getValue().isAvailable()) {
        ret.add(o.getValue());
      }
    }
    return ret;
  }

  public Category getCategory(String filter) {
    if (filter == null) {
      return null;
    }
    if (categoryTypes.containsKey(filter)) {
      return categoryTypes.get(filter);
    }
    return null;
  }

  public String[] getCategoryNames() {
    String[] cat = new String[categoryTypes.size()];

    int i = 0;
    for (Map.Entry<String, Category> o : categoryTypes.entrySet()) {
      cat[i] = o.getKey();
      ++i;
    }
    return cat;
  }

  public HashSet<ServiceDependency> getServiceTypeDependencyKeys() {
    HashSet<ServiceDependency> uniqueKeys = new HashSet<ServiceDependency>();
    for (Map.Entry<String, ServiceType> o : serviceTypes.entrySet()) {
      ServiceType st = o.getValue();
      if (st.dependencies != null) {
        for (ServiceDependency library : st.dependencies) {
          uniqueKeys.add(library);
        }
      }
    }

    return uniqueKeys;
  }

  public String[] getServiceTypeNames() {
    return getServiceTypeNames(null);
  }

  public String[] getServiceTypeNames(String categoryFilterName) {

    if (categoryFilterName == null || categoryFilterName.length() == 0 || categoryFilterName.equals("all")) {
      String[] ret = serviceTypes.keySet().toArray(new String[0]);
      Arrays.sort(ret);
      return ret;
    }

    if (!categoryTypes.containsKey(categoryFilterName)) {
      return new String[] {};
    }

    Category cat = categoryTypes.get(categoryFilterName);
    return cat.serviceTypes.toArray(new String[cat.serviceTypes.size()]);

  }

  public ServiceType getServiceType(String fullTypeName) {
    if (!fullTypeName.contains(".")) {
      fullTypeName = String.format("org.myrobotlab.service.%s", fullTypeName);
    }
    return serviceTypes.get(fullTypeName);
  }

  public List<ServiceType> getServiceTypes() {
    return getServiceTypes(true);
  }

  public List<ServiceType> getServiceTypes(boolean showUnavailable) {
    ArrayList<ServiceType> ret = new ArrayList<ServiceType>();
    for (Map.Entry<String, ServiceType> o : serviceTypes.entrySet()) {
      if (!o.getValue().isAvailable() && !showUnavailable) {
        log.info("getServiceTypes ignore : " + o.getValue().getSimpleName());
      } else {
        ret.add(o.getValue());
      }
    }
    return ret;
  }

  public boolean save() {

    log.info("saving {}", serviceDataCacheFileName);
    return save(serviceDataCacheFileName);
  }

  public boolean save(String filename) {
    try {

      FileOutputStream fos = new FileOutputStream(filename);
      String json = CodecUtils.toJson(this);
      fos.write(json.getBytes());
      fos.close();

      return true;
    } catch (Exception e) {
      Logging.logError(e);
    }

    return false;
  }

  // TWO LEVELS !!! 1. Run-time checking & Build-time checking
  // Built-time checking
  // build time has access to the repo - can cross check dependencies to make
  // sure they are in the library
  //
  // Runtime checking
  // for all Peers - do ALL THERE TYPES CURRENTLY EXIST ???
  // FIXME - TODO - FIND

  public List<Category> getCategories() {
    ArrayList<Category> categories = new ArrayList<Category>();
    for (Category category : categoryTypes.values()) {
      categories.add(category);
    }
    return categories;
  }

  static public List<ServiceDependency> getDependencyKeys(String fullTypeName) {
    List<ServiceDependency> keys = new ArrayList<ServiceDependency>();
    ServiceData sd = getLocalInstance();
    if (!sd.serviceTypes.containsKey(fullTypeName)) {
      log.error("{} not defined in service types");
      return keys;
    }

    ServiceType st = localInstance.serviceTypes.get(fullTypeName);
    return st.getDependencies();
  }

  public static void main(String[] args) {
    try {

      // LoggingFactory.init(); - don't change logging for mvn
      String path = "";
      if (args.length > 0) {
        path = args[0];
      } else {
        path = ".";
      }

      String filename = path + File.separator + "serviceData.json";
      log.info("generating {}", filename);
      if (path.length() > 0) {
        new File(path).mkdirs();
      }

      // remove pre-existing filename
      File removeExisting = new File(filename);
      removeExisting.delete();

      // remove .myrobotlab/serviceData.json
      // 20190630 - GroG changed uses FileIO.getCfgDir()
      removeExisting = new File(FileIO.getCfgDir() + File.separatorChar + "serviceData.json");
      removeExisting.delete();

      // THIS IS FOR ANT BUILD - DO NOT CHANGE !!! - BEGIN ----
      ServiceData sd = generate();
      // save a copy to the resources folder so it can be bundled in the jar.
      sd.save(filename);
      // save to the .myrobotlab directory also..
      sd.save();

    } catch (Exception e) {
      Logging.logError(e);
      System.exit(-1);
    }

    // System.exit(0);

  }

}

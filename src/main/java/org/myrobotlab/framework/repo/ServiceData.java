package org.myrobotlab.framework.repo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
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

  static private String serviceDataCacheFileName = String.format("%s%sserviceData.json", FileIO.getCfgDir(), File.separator);

  static public ServiceData getLocalInstance() {
    if (localInstance == null) {

      // step 1 - try local file in the .myrobotlab directory
      // step 2 - extract the file from the jar
      // WE CAN NOT GENERATE THIS FILE DURING RUNTIME !!!

      // step 3 - if 1 & 2 fail - then we can 'assume' were in develop
      // time (we'll isJar check and error if not)
      // - generate it and put it in
      // getRoot()/resource/framework/serviceData.json

      File jsonFile = new File(serviceDataCacheFileName);

      try {
        log.info("try #1 loading local file {}", jsonFile);
        String data = FileIO.toString(jsonFile);
        if (data == null || data.length() == 0) {
          throw new IOException("service data file [{}] contains no data");
        }
        localInstance = CodecUtils.fromJson(data, ServiceData.class);
        return localInstance;
      } catch (FileNotFoundException fe) {
        try {
          log.info("could not find {}", serviceDataCacheFileName);
          jsonFile.getParentFile().mkdirs();
          String extractFrom = "/resource/framework/serviceData.json";
          log.info("try #2 {} not found - extracting from {}", jsonFile.getName(), extractFrom);
          FileIO.extract(extractFrom, jsonFile.getAbsolutePath());
          String data = FileIO.toString(jsonFile);
          localInstance = CodecUtils.fromJson(data, ServiceData.class);
        } catch (Exception e) {
          log.info("could not extract from {}", "/resource/framework/serviceData.json");
          String newJson = FileIO.gluePaths(FileIO.getRoot(), "/resource/framework/serviceData.json");
          log.info("try #3 serviceData.json not found in resource ! - generating and putting it in {}", newJson);
          if (FileIO.isJar()) {
            log.error("we are in a jar!  This is very bad!");
          } else {
            log.info("we are not in a jar ... ok I guess we are doing a \"refresh\" on serviceData.json");
          }
          try {
            ServiceData sd = ServiceData.generate();
            String json = CodecUtils.toJson(sd);

            log.info("saving generated serviceData.json to {}", newJson);
            FileOutputStream fos = new FileOutputStream(newJson);
            fos.write(json.getBytes());
            fos.close();
            log.info("saved -- goodtimes");
            localInstance = sd;
          } catch (Exception e2) {
            log.error("I've tried everything! .. I give up");
            Logging.logError(e2);
          }
        }
        localInstance.save();
      } catch (Exception e) {
        log.error("retrieving service data failed", e);
      }

    }
    return localInstance;
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
   * @return the service data description
   * @throws IOException e
   */
  static public ServiceData generate() throws IOException {
    log.info("================ generating serviceData.json begin ================");
    ServiceData sd = new ServiceData();

    // get services - all this could be done during Runtime
    // although running through zip entries would be a bit of a pain
    // epecially if you have to spin through 12 megs of data
    List<String> services = FileIO.getServiceList();

    log.info("found {} services", services.size());
    for (int i = 0; i < services.size(); ++i) {

      String fullClassName = services.get(i);
      // log.info("querying {}", fullClassName);
      try {
        Class<?> theClass = Class.forName(fullClassName);
        Method method = theClass.getMethod("getMetaData");
        ServiceType serviceType = (ServiceType) method.invoke(null);

        if (!fullClassName.equals(serviceType.getName())) {
          log.error(String.format("Class name %s not equal to the ServiceType's name %s", fullClassName, serviceType.getName()));
        }

        sd.add(serviceType);

        for (String cat : serviceType.categories) {
          Category category = null;
          if (serviceType.isAvailable())
          {
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
        log.error(String.format("%s does not have a static getMetaData method", fullClassName));
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

  public HashSet<String> getServiceTypeDependencyKeys() {
    HashSet<String> uniqueKeys = new HashSet<String>();
    for (Map.Entry<String, ServiceType> o : serviceTypes.entrySet()) {
      ServiceType st = o.getValue();
      if (st.dependencies != null) {
        for (String org : st.dependencies) {
          uniqueKeys.add(org);
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
    return serviceTypes.get(fullTypeName);
  }

  public ArrayList<ServiceType> getServiceTypes() {
    return getServiceTypes(true);
  }
  
  public ArrayList<ServiceType> getServiceTypes(boolean showUnavailable) {
    ArrayList<ServiceType> ret = new ArrayList<ServiceType>();
    for (Map.Entry<String, ServiceType> o : serviceTypes.entrySet()) {
      if (!o.getValue().isAvailable() && !showUnavailable)
      {
        log.info("getServiceTypes ignore : "+o.getValue().getSimpleName());
      }
      else
      {
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

  public ArrayList<Category> getCategories() {
    ArrayList<Category> categories = new ArrayList<Category>();
    for (Category category : categoryTypes.values()) {
      categories.add(category);
    }
    return categories;
  }

  static public Set<String> getDependencyKeys(String fullTypeName) {
    HashSet<String> keys = new HashSet<String>();
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

      LoggingFactory.init();
      // LoggingFactory.getInstance().setLevel("INFO");
      // LoggingFactory.getInstance().addAppender(Appender.FILE);
      String path = "";
      if (args.length > 0) {
        path = args[0];
      }

      String filename = FileIO.gluePaths(path, "serviceData.json");
      log.info("generating {}", filename);
      if (path.length() > 0) {
        new File(path).mkdirs();
      }

      // THIS IS FOR ANT BUILD - DO NOT CHANGE !!! - BEGIN ----
      ServiceData sd = generate();
      FileOutputStream fos = new FileOutputStream(filename);
      fos.write(CodecUtils.toJson(sd).getBytes());
      fos.close();
      // THIS IS FOR ANT BUILD - DO NOT CHANGE !!! - END ----

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}

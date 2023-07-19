package org.myrobotlab.framework.repo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.ServiceReservation;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

/**
 * ServiceData class contains all of the Services meta data. This includes : 1.
 * Dependency information - what libraries are needed to run the class 2.
 * 
 * All this information is Service "type" related - non of it is instance
 * specific. ServiceData has to be created during "build" time since most of the
 * services contain dependencies which might not be fulfilled during runtime.
 * 
 * 
 * When MyRobotLab runs for the first time, it will extract this file into the
 * .myrobotlab directory.
 * 
 * @author GroG
 * 
 * FIXME - this is really just something that manages MetaData ... should make it the same both for clarity and
 * transparency 
 *
 */
public class ServiceData implements Serializable {

  static private ServiceData localInstance = null;
  
  static final public String LIBRARIES = "libraries";

  transient public final static Logger log = LoggerFactory.getLogger(ServiceData.class);

  /**
   * A plan of what services and peers to create. If not defined, the default
   * will be used as its defined in the {MetaData}Meta data class. The default
   * build plan can be modified. This static object will be empty unless there
   * are overrides that modify default meta data.
   * 
   * All entries in planOverrides are all absolute key paths !
   */
  transient static private final Map<String, ServiceReservation> planStore = new TreeMap<>();

  private static final long serialVersionUID = 1L;

  static private String serviceDataCacheFileName = LIBRARIES + File.separator + "serviceData.json";

  /**
   * clears all overrides. All services shall be using the standard hard co
   */
  public static void clearOverrides() {
    planStore.clear();
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

        // filter out the package-info files
        if (fullClassName.contains("package-info")) {
          continue;
        }

        MetaData serviceType = getMetaData(fullClassName);

        if (!fullClassName.equals(serviceType.getType())) {
          log.error("Class name {} not equal to the MetaData's name {}", fullClassName, serviceType.getType());
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
            category.serviceTypes.add(serviceType.getType());
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

  static public List<ServiceDependency> getDependencyKeys(String fullTypeName) {
    List<ServiceDependency> keys = new ArrayList<ServiceDependency>();
    ServiceData sd = getLocalInstance();
    if (!sd.serviceTypes.containsKey(fullTypeName)) {
      log.error("{} not defined in service types");
      return keys;
    }

    MetaData st = localInstance.serviceTypes.get(fullTypeName);
    return st.getDependencies();
  }

  static public String getFullMetaTypeName(String type) {
    if (!type.contains(".") && !type.endsWith("Meta")) {
      type = String.format("org.myrobotlab.service.meta.%sMeta", type);
    } else {
      int pos = type.lastIndexOf(".");
      String serviceTypeName = type.substring(pos + 1);
      type = type.substring(0, pos) + ".meta." + serviceTypeName + "Meta";
    }
    return type;
  }

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
      // libraries/serviceData.json

      // if we're not in a jar we are in an IDE.

      // First check the libraries/serviceData.json dir.
      File jsonFile = new File(serviceDataCacheFileName);
      if (jsonFile.exists()) {
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

        // we are running in an IDE and haven't generated/saved the
        // serviceData.json yet.
        try {
          // This must only be run as part of the build or from your IDE. It
          // will not work when running from a jar.
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

  /**
   * This method returns the default meta data of a class.
   * 
   * @param type
   *          of the service
   * @return the service metadata
   * 
   */
  static public MetaData getMetaData(String type) {
    return getMetaData(null, type);
  }

  public static MetaData getMetaData(String name, String type) {
    return getMetaData(name, type, null);
  }

  /**
   * This method gets the meta data of a service class. If the service is
   * instance specific (ie if the service has a name) it will return that
   * instance's meta data, which can contain overrides.
   * 
   * If a name/instance is not supplied the default meta data is supplied
   * 
   * @param serviceName
   *          the name of the service
   * @param type
   *          the type of the service
   * @param cyclicalCheck
   *          to protect against cycles
   * @return the service metadata
   * 
   */
  public static MetaData getMetaData(String serviceName, String type, Set<String> cyclicalCheck) {
    try {

      if (cyclicalCheck == null) {
        // root node
        cyclicalCheck = new HashSet<>();
      }

      cyclicalCheck.add(type);

      // test for overrides from name - name can override type
      if (serviceName != null && ServiceData.planStore.get(serviceName) != null) {
        ServiceReservation sr = ServiceData.planStore.get(serviceName);
        if (sr != null && sr.type != null) {
          type = sr.type;
        }
      }

      // if (type.equals("org.myrobotlab.service.Cron")) {
      // log.info("here");
      // }

      type = getFullMetaTypeName(type);

      MetaData metaData = MetaData.get(type);


      return metaData;

    } catch (Exception e) {
      log.error("getMetaData threw {}.getMetaData() does not exist", type, e);
    }
    return null;
  }

  static public Map<String, ServiceReservation> getOverrides() {
    return planStore;
  }

  /**
   * the set of all categories
   */
  TreeMap<String, Category> categoryTypes = new TreeMap<String, Category>();

  /**
   * all services meta data is contained here
   */
  TreeMap<String, MetaData> serviceTypes = new TreeMap<String, MetaData>();

  public ServiceData() {
  }

  public void add(MetaData serviceType) {
    serviceTypes.put(serviceType.getType(), serviceType);
  }

  public boolean containsServiceType(String fullServiceName) {
    return serviceTypes.containsKey(fullServiceName);
  }

  public List<MetaData> getAvailableServiceTypes() {
    List<MetaData> ret = new ArrayList<MetaData>();
    for (Map.Entry<String, MetaData> o : serviceTypes.entrySet()) {
      if (o.getValue().isAvailable()) {
        ret.add(o.getValue());
      }
    }
    return ret;
  }

  public List<Category> getCategories() {
    ArrayList<Category> categories = new ArrayList<Category>();
    for (Category category : categoryTypes.values()) {
      categories.add(category);
    }
    return categories;
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
    for (Map.Entry<String, MetaData> o : serviceTypes.entrySet()) {
      MetaData st = o.getValue();
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

  public List<MetaData> getServiceTypes() {
    return getServiceTypes(true);
  }

  public List<MetaData> getServiceTypes(boolean showUnavailable) {
    List<MetaData> ret = new ArrayList<MetaData>();
    for (Map.Entry<String, MetaData> o : serviceTypes.entrySet()) {
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
      File dirs = new File(filename).getParentFile();
      dirs.mkdirs();
      FileOutputStream fos = new FileOutputStream(filename);
      String json = CodecUtils.toJson(this);
      fos.write(json.getBytes());
      fos.close();

      return true;
    } catch (Exception e) {
      log.error("service data saving threw for {}", filename, e);
    }

    return false;
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

      // remove libraries/serviceData.json
      removeExisting = new File("libraries" + File.separatorChar + "serviceData.json");
      removeExisting.delete();

      // THIS IS FOR ANT BUILD - DO NOT CHANGE !!! - BEGIN ----
      ServiceData sd = generate();
      // save a copy to the resources folder so it can be bundled in the jar.
      sd.save(filename);
      // save to the .myrobotlab directory also..
      sd.save();

    } catch (Exception e) {
      log.error("main threw", e);
      System.exit(-1);
    }

    // System.exit(0);

  }


}

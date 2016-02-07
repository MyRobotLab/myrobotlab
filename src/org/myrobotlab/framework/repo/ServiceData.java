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
import org.myrobotlab.io.FileIO;
import org.myrobotlab.io.FindFile;
import org.myrobotlab.logging.Appender;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class ServiceData implements Serializable {

	private static final long serialVersionUID = 1L;

	transient public final static Logger log = LoggerFactory.getLogger(ServiceData.class);

	TreeMap<String, ServiceType> serviceTypes = new TreeMap<String, ServiceType>();

	TreeMap<String, Category> categoryTypes = new TreeMap<String, Category>();

	TreeMap<String, Dependency> dependencyTypes = new TreeMap<String, Dependency>();
	

	
	static public ServiceData generate() throws IOException {
		ServiceData sd = generate("../repo");
		
		FileOutputStream fos = new FileOutputStream("build/classes/resource/framework/serviceData.json");
		fos.write(CodecUtils.toJson(sd).getBytes());
		fos.close();
		
		return sd;
	}
	
	/**
	 * method to create a local cache file from the repo directories of all
	 * libraries
	 * @param repoDir
	 * @return
	 */
	static public ServiceData generate(String repoDir) {
		try {

			ServiceData sd = new ServiceData();

			// get all third party libraries
			// give me all the first level directories of the repo
			// this CAN BE DONE REMOTELY TOO !!! - using v3 githup json api !!!
			List<File> dirs = FindFile.find(repoDir, "^[^.].*[^-_.]$", false, true);
			log.info("found {} files", dirs.size());
			for (int i = 0; i < dirs.size(); ++i) {
				File f = dirs.get(i);
				if (f.isDirectory()) {
					try {
						// log.info("looking in {}", f.getAbsolutePath());
						List<File> subDirsList = FindFile.find(f.getAbsolutePath(), ".*", false, true);
						ArrayList<File> filtered = new ArrayList<File>();
						for (int z = 0; z < subDirsList.size(); ++z) {
							File dir = subDirsList.get(z);
							if (dir.isDirectory()) {
								filtered.add(dir);
							}
						}

						File[] subDirs = filtered.toArray(new File[filtered.size()]);
						Arrays.sort(subDirs);
						// get latest version
						File ver = subDirs[subDirs.length - 1];
						log.info("adding third party library {} {}", f.getName(), ver.getName());
						sd.addThirdPartyLib(f.getName(), ver.getName());
					} catch (Exception e) {
						Logging.logError(e);
					}

				} else {
					log.info("skipping file {}", f.getName());
				}
			}
			
			// get services - all this could be done during Runtime 
			// although running through zip entries would be a bit of a pain
			// epecially if you have to spin through 12 megs of data
			List<String> services = FileIO.getPackageClassNames("org.myrobotlab.service");
			
			log.info("found {} services", services.size());
			for (int i = 0; i < services.size(); ++i) {
				
					String fullClassName = services.get(i);
					log.info("querying {}", fullClassName);
						try {
							Class<?> theClass = Class.forName(fullClassName);
							Method method = theClass.getMethod("getMetaData");
							ServiceType serviceType = (ServiceType)method.invoke(null);
							
							if (!fullClassName.equals(serviceType.getName())){
								log.error(String.format("Class name %s not equal to the ServiceType's name %s", fullClassName, serviceType.getName()));
							}
							
							sd.add(serviceType);
							
							for (String cat : serviceType.categories){
								Category category = null;
								if (sd.categoryTypes.containsKey(cat)){
									category = sd.categoryTypes.get(cat);
								} else {
									category = new Category();
									category.name = category.name;									
								}
								category.serviceTypes.add(serviceType.name);	
								sd.categoryTypes.put(cat, category);
							}
							
						} catch (Exception e) {
							log.error(String.format("%s does not have a static getMetaData method", fullClassName));
						}
			}
			
			if (sd.getInvalidDependencies().length > 0){
				log.error("invalid dependencies [{}]", Arrays.toString(sd.getInvalidDependencies()));
			}

			return sd;
		} catch (Exception e) {
			Logging.logError(e);
		}
		return null;
	}

	
	static public ServiceData getLocal() throws IOException {
		return getLocal(null);
	}

	static public ServiceData getLocal(String filename) throws IOException {

		if (filename == null) {
			filename = String.format("%s%sserviceData.json", FileIO.getCfgDir(), File.separator);
		}
		
		File check = new File (filename);
		if (!check.exists()){
			String serviceData = FileIO.resourceToString("framework/serviceData.json");
			FileIO.stringToFile(filename, serviceData);
		}
		
		String data = FileIO.fileToString(filename);
		return load(data);
	}
	

	/**
	 * long ass process to "not" be doing in a seperate thread ... :P should be
	 * asynchronous - but it would be a challenge to sync with the graphics
	 * 
	 * @param url
	 * @return
	 */
	/* NO LONGER NEEDED
	public static ServiceData getRemote(String url) {
		try {
			log.info("getting {}", url);
			System.out.println(String.format("getting remote file from %s", url));
			String data = new String(FileIO.getURL(new URL(url)));
			return load(data);
		} catch (Exception e) {
			Logging.logError(e);
		}
		return null;
	}
	*/

	public static ServiceData load(String data) {
		try {
			if (data == null) {
				log.warn("can not load serviceData - data is null");
			}
			log.info("loading serviceData");
			ServiceData sd = CodecUtils.fromJson(data, ServiceData.class);
			sd.isValid();

			return sd;
		} catch (Exception e) {
			Logging.logError(e);
		}
		return null;
	}


	public ServiceData() {
	}

	public void add(ServiceType serviceType) {
		serviceTypes.put(serviceType.name, serviceType);
	}

	// FIXME - change to addDependency
	public void addThirdPartyLib(String org, String revision) {
		Dependency dep = new Dependency(org, revision);
		dependencyTypes.put(org, dep);
	}

	boolean containsDependency(String org) {
		return dependencyTypes.containsKey(org);
	}

	public boolean containsServiceType(String fullServiceName) {
		return serviceTypes.containsKey(fullServiceName);
	}

	public ArrayList<ServiceType> getAvailableServiceTypes() {
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

	public Dependency getDependency(String org) {
		if (dependencyTypes.containsKey(org)) {
			return dependencyTypes.get(org);
		}

		return null;
	}

	public String[] getInvalidDependencies() {
		HashSet<String> unique = new HashSet<String>();
		for (Map.Entry<String, ServiceType> o : serviceTypes.entrySet()) {
			ServiceType st = o.getValue();
			if (st.dependencies != null) {
				for (String org : st.dependencies) {
					if (!isValid(org)) {
						unique.add(org);
					}
				}
			}
		}

		String[] ret = new String[unique.size()];
		int x = 0;

		for (String s : unique) {
			ret[x] = s;
			++x;
		}

		Arrays.sort(ret);
		return ret;
	}

	public ServiceType getServiceType(String fullServiceName) {
		if (serviceTypes.containsKey(fullServiceName)) {
			return serviceTypes.get(fullServiceName);
		}

		log.error("could not get {}", fullServiceName);
		return null;
	}

	public String[] getServiceTypeDependencies() {
		HashSet<String> unique = new HashSet<String>();
		for (Map.Entry<String, ServiceType> o : serviceTypes.entrySet()) {
			ServiceType st = o.getValue();
			if (st.dependencies != null) {
				for (String org : st.dependencies) {
					unique.add(org);
				}
			}
		}

		String[] ret = new String[unique.size()];
		int x = 0;

		for (String s : unique) {
			ret[x] = s;
			++x;
		}

		Arrays.sort(ret);
		return ret;
	}

	public String[] getServiceTypeNames(String filter) {

		if (filter == null || filter.length() == 0 || filter.equals("all")) {
			String[] ret = serviceTypes.keySet().toArray(new String[0]);
			Arrays.sort(ret);
			return ret;
		}

		if (!categoryTypes.containsKey(filter)) {
			return new String[] {};
		}

		Category cat = categoryTypes.get(filter);
		return cat.serviceTypes.toArray(new String[cat.serviceTypes.size()]);

	}

	public ArrayList<ServiceType> getServiceTypes() {
		ArrayList<ServiceType> ret = new ArrayList<ServiceType>();
		for (Map.Entry<String, ServiceType> o : serviceTypes.entrySet()) {
			ret.add(o.getValue());
		}
		return ret;
	}

	public String[] getUnusedDependencies() {
		HashSet<String> unique = new HashSet<String>();
		for (Map.Entry<String, Dependency> o : dependencyTypes.entrySet()) {
			String org = o.getValue().getOrg();
			if (isUnused(org)) {
				unique.add(org);
			}
		}

		String[] ret = new String[unique.size()];
		int x = 0;

		for (String s : unique) {
			ret[x] = s;
			++x;
		}

		Arrays.sort(ret);
		return ret;
	}

	public boolean hasUnfulfilledDependencies(String fullServiceName) {

		// no serviceInfo
		if (!serviceTypes.containsKey(fullServiceName)) {
			log.error(String.format("%s not found", fullServiceName));
			return false;
		}

		ServiceType d = serviceTypes.get(fullServiceName);
		if (d.dependencies == null || d.dependencies.size() == 0) {
			log.debug(String.format("no dependencies needed for %s", fullServiceName));
			return false;
		}

		for (String org : d.dependencies) {
			if (!dependencyTypes.containsKey(org)) {
				log.error(String.format("%s has dependency of %s, but it is does not have a defined version", fullServiceName, org));
				return true;
			} else {
				Dependency dep = dependencyTypes.get(org);
				if (!dep.isResolved()) {
					log.debug(String.format("%s had a dependency of %s, but it is currently not resolved", fullServiceName, org));
					return true;
				}
			}
		}

		return false;
	}

	public boolean isUnused(String org) {
		for (Map.Entry<String, ServiceType> o : serviceTypes.entrySet()) {
			ServiceType st = o.getValue();
			if (st.dependencies != null) {
				for (String d : st.dependencies) {
					if (org.equals(d)) {
						return false;
					}
				}
			}
		}

		return true;
	}

	public boolean isValid() {

		// check for validity
		String[] invalid = getInvalidDependencies();
		if (invalid != null && invalid.length > 0) {
			for (int i = 0; i < invalid.length; ++i) {
				log.error(String.format("%s is invalid", invalid[i]));
			}
		}

		String[] unused = getUnusedDependencies();
		if (unused.length > 0) {
			for (int i = 0; i < unused.length; ++i) {
				// log.warn(String.format("repo library %s is unused",
				// unused[i]));
			}
		}

		for (Map.Entry<String, Category> o : categoryTypes.entrySet()) {
			Category category = o.getValue();

			for (int j = 0; j < category.serviceTypes.size(); ++j) {
				String serviceType = category.serviceTypes.get(j);
				if (!serviceTypes.containsKey(serviceType)) {
					log.warn(String.format("category %s contains reference to service type %s which does not exist", category.name, serviceType));
				}
			}
		}

		HashSet<String> categorizedServiceTypes = new HashSet<String>();

		for (Map.Entry<String, Category> o : categoryTypes.entrySet()) {
			Category category = o.getValue();
			if (category.serviceTypes.size() == 0) {
				log.warn(String.format("empty category %s", category.name));
			}
			for (int j = 0; j < category.serviceTypes.size(); ++j) {
				categorizedServiceTypes.add(category.serviceTypes.get(j));
			}
		}

		for (Map.Entry<String, ServiceType> o : serviceTypes.entrySet()) {
			ServiceType st = o.getValue();
			if (!categorizedServiceTypes.contains(st.getName())) {
				log.warn(String.format("uncategorized service %s", st.getName()));
			}
		}

		if (invalid.length > 0) {
			return false;
		}

		return true;
	}

	public boolean isValid(String org) {
		return dependencyTypes.containsKey(org);
	}

	public ServiceData loadLocal() throws IOException {
		return getLocal(String.format("%s%sserviceData.json", FileIO.getCfgDir(), File.separator));
	}

	public boolean save() {
		return save(String.format("%s%sserviceData.json", FileIO.getCfgDir(), File.separator));
	}

	public boolean save(String filename) {
		try {

			isValid();

			// Serializer serializer = new Persister();

			FileOutputStream fos = new FileOutputStream(filename);
			String json = CodecUtils.toJson(this);
			fos.write(json.getBytes());
			fos.close();

			// File f = new File(filename);
			// serializer.write(this, f);

			return true;
		} catch (Exception e) {
			Logging.logError(e);
		}

		return false;
	}
	
	
	public static void main(String[] args) {
		try {

			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel("INFO");
			LoggingFactory.getInstance().addAppender(Appender.FILE);
			
			// for ANT build 
			ServiceData sd = generate();
			/*
			Repo repo = new Repo();
			log.info(String.format("%b", repo.isServiceTypeInstalled("org.myrobotlab.service.InMoov")));
			*/
			
			/*
			
			ServiceData sd = generate("../repo");

			
			FileOutputStream fos = new FileOutputStream("serviceData.compare.json");
			fos.write(CodecUtils.toJson(sd).getBytes());
			fos.close();
			*/

			// String json = FileIO.fileToString("serviceData.compare.json");
			// sd = ServiceData.load(json);

			/*
			 * ServiceData sd = generate("../repo"); json = Encoder.toJson(sd);
			 * FileOutputStream fos = new FileOutputStream(new
			 * File("serviceData.generated.json")); fos.write(json.getBytes());
			 * fos.close();
			 */

			// ServiceData sd =
			// ServiceData.load(FileIO.fileToString(".myrobotlab/serviceData.json"));
			// ServiceData sd = generate("../repo");
			/*
			 * ServiceType st =
			 * sd.getServiceType("org.myrobotlab.service.Arduino");
			 * st.addDependency("cc.arduino");
			 */
			/*
			 * sd.addCategory("actuators", "motion controllers", new
			 * String[]{"org.myrobotlab.service.Motor",
			 * "org.myrobotlab.service.Servo"
			 * ,"org.myrobotlab.service.MouthControl"
			 * ,"org.myrobotlab.service.PID"}); sd.addCategory("audio", "", new
			 * String[]{"org.myrobotlab.service.AudioCapture",
			 * "org.myrobotlab.service.AudioFile"
			 * ,"org.myrobotlab.service.JFugue"}); // ServiceData sd =
			 * ServiceData.getLocal();// .loadLocal();
			 * sd.save("generated.json");
			 */

			/*
			 * Serializer serializer = new Persister();
			 * 
			 * File cfg = new File("serviceData.test.json");
			 * serializer.write(serviceData, cfg);
			 */

			log.info("here");

		} catch (Exception e) {
			Logging.logError(e);
		}
	}


	public ArrayList<Category> getCategories() {
		ArrayList<Category> categories = new ArrayList<Category>();
		for (Category category : categoryTypes.values()){
			categories.add(category);
		}
		return categories;
	}

}

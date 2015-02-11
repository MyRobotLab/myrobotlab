package org.myrobotlab.framework.repo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.fileLib.FindFile;
import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Appender;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

public class ServiceData implements Serializable {

	private static final long serialVersionUID = 1L;
	transient public final static Logger log = LoggerFactory.getLogger(Service.class);

	TreeMap<String, ServiceType> serviceTypes = new TreeMap<String, ServiceType>();
	TreeMap<String, Category> categoryTypes = new TreeMap<String, Category>();
	TreeMap<String, Dependency> dependencyTypes = new TreeMap<String, Dependency>();

	public ServiceData() {
	}

	public void add(ServiceType serviceType) {
		serviceTypes.put(serviceType.name, serviceType);
		if (serviceType.dependencies != null) {
			for (int i = 0; i < serviceType.dependencies.size(); ++i) {
				String org = serviceType.dependencies.get(i);
				if (!containsDependency(org)) {
					log.error(String.format("can %s not find %s in dependencies", org));
				}
			}
		}
	}

	boolean containsDependency(String org) {
		return dependencyTypes.containsKey(org);
	}

	public Dependency getDependency(String org) {
		if (dependencyTypes.containsKey(org)) {
			return dependencyTypes.get(org);
		}

		return null;
	}

	// FIXME - change to addDependency
	public void addThirdPartyLib(String org, String revision) {
		Dependency dep = new Dependency(org, revision);
		dependencyTypes.put(org, dep);
	}

	public void addServiceType(String className) {
		addServiceType(className, null, null);
	}

	public void addServiceType(String className, String[] dependencies) {
		addServiceType(className, null, dependencies);

	}

	public boolean containsServiceType(String fullServiceName) {
		return serviceTypes.containsKey(fullServiceName);
	}

	public ServiceType getServiceType(String fullServiceName) {
		if (serviceTypes.containsKey(fullServiceName)) {
			return serviceTypes.get(fullServiceName);
		}

		log.error("could not get {}", fullServiceName);
		return null;
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

		for (int i = 0; i < d.dependencies.size(); ++i) {
			String org = d.dependencies.get(i);
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

	public boolean isUnused(String org) {
		for (Map.Entry<String, ServiceType> o : serviceTypes.entrySet()) {
			ServiceType st = o.getValue();
			if (st.dependencies != null) {
				for (int j = 0; j < st.dependencies.size(); ++j) {
					String d = st.dependencies.get(j);
					if (org.equals(d)) {
						return false;
					}
				}
			}
		}

		return true;
	}

	public boolean isValid(String org) {
		return dependencyTypes.containsKey(org);
	}

	public String[] getInvalidDependencies() {
		HashSet<String> unique = new HashSet<String>();
		for (Map.Entry<String, ServiceType> o : serviceTypes.entrySet()) {
			ServiceType st = o.getValue();
			if (st.dependencies != null) {
				for (int j = 0; j < st.dependencies.size(); ++j) {
					String org = st.dependencies.get(j);
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

	public String[] getServiceTypeDependencies() {
		HashSet<String> unique = new HashSet<String>();
		for (Map.Entry<String, ServiceType> o : serviceTypes.entrySet()) {
			ServiceType st = o.getValue();
			if (st.dependencies != null) {
				for (int j = 0; j < st.dependencies.size(); ++j) {
					unique.add(st.dependencies.get(j));
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

	public void addServiceType(String className, String description) {
		addServiceType(className, description, null);
	}

	public void addServiceType(String className, String description, String[] dependencies) {
		if (serviceTypes.containsKey(className)) {
			log.error(String.format("duplicate names %s - not adding service type", className));
			return;
		}
		ServiceType st = new ServiceType(className);
		st.description = description;
		serviceTypes.put(st.getName(), st);
		if (dependencies != null) {
			for (int i = 0; i < dependencies.length; ++i) {
				st.addDependency(dependencies[i]);
			}
		}

	}

	static public ServiceData getLocal() throws IOException {
		return getLocal(null);
	}

	static public ServiceData getLocal(String filename) throws IOException {

		if (filename == null) {
			filename = String.format("%s%sserviceData.json", FileIO.getCfgDir(), File.separator);
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
	public static ServiceData getRemote(String url) {
		try {
			log.info("getting {}", url);
			System.out.println(String.format("getting remote file from %s", url));
			String data = new String(FileIO.getURL(new URL(url)));
			return load(data);
		} catch (Exception e) {
			Logging.logException(e);
		}
		return null;
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
				log.warn(String.format("repo library %s is unused", unused[i]));
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

	public static ServiceData load(String data) {
		try {
			if (data == null) {
				log.warn("can not load serviceData - data is null");
			}
			log.info("loading serviceData");
			ServiceData sd = Encoder.gson.fromJson(data, ServiceData.class);
			sd.isValid();

			return sd;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return null;
	}

	public String[] getServiceTypeNames(String filter) {

		if (filter == null || filter.length() == 0 || filter.equals("all")) {
			String[] ret = (String[]) serviceTypes.keySet().toArray(new String[0]);
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

	public void addCategory(String name, String[] serviceTypes) {
		addCategory(name, null, serviceTypes);
	}

	public void addCategory(String name, String description, String[] serviceTypes) {
		Category category = null;
		if (categoryTypes.containsKey(name)) {
			category = categoryTypes.get(name);
		} else {
			category = new Category();
		}

		category.name = name;
		category.description = description;
		for (int i = 0; i < serviceTypes.length; ++i) {
			boolean alreadyHasReference = false;
			for (int j = 0; j < category.serviceTypes.size(); ++j) {
				if (serviceTypes[i].equals(category.serviceTypes.get(j))) {
					alreadyHasReference = true;
					break;
				}
			}

			if (!alreadyHasReference) {
				category.serviceTypes.add(serviceTypes[i]);
			}
		}

		categoryTypes.put(category.name, category);
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
			String json = Encoder.gson.toJson(this);
			fos.write(json.getBytes());
			fos.close();

			// File f = new File(filename);
			// serializer.write(this, f);

			return true;
		} catch (Exception e) {
			Logging.logException(e);
		}

		return false;
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

	static public ServiceData generate(String repoDir) {
		try {

			ServiceData sd = new ServiceData();

			// give me all the first level directories

			List<File> dirs = FindFile.find(repoDir, "^[^.].*[^-_.]$", false, true);
			log.info("found {} files", dirs.size());
			for (int i = 0; i < dirs.size(); ++i) {
				File f = dirs.get(i);
				if (f.isDirectory()) {
					try {
						log.info("looking in {}", f.getAbsolutePath());
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
						Logging.logException(e);
					}

				} else {
					log.info("skipping file {}", f.getName());
				}
			}

			// get services
			File spath = new File("src/org/myrobotlab/service");
			List<File> services = FindFile.find(spath.getAbsolutePath(), ".*", false, false);
			log.info("found {} services", services.size());
			for (int i = 0; i < services.size(); ++i) {
				File sf = services.get(i);
				if (!sf.isDirectory()) {
					String n = sf.getName();
					String sname = n.substring(0, n.lastIndexOf("."));
					String fullClassName = String.format("org.myrobotlab.service.%s", sname);
					log.info("adding {}", sname);

					// TODO - add Peer dependencies
					ServiceType s = new ServiceType(fullClassName);
					

					try {
						if (sname.equals("LeapMotion2") || sname.equals("Test")) {
							continue;
						}
						ServiceInterface si = (ServiceInterface) Service.getNewInstance(fullClassName, sname);
						if (si == null || sname.equals("AWTRobot") || sname.equals("LeapMotion2")) {
							log.error("could not get service interface for {}", sname);
							continue;
						}

						s.description = si.getDescription();
						si.releaseService();
						si.releasePeers();

						// Class<?> theClass = Class.forName(fullClassName);
						// TODO - add list of Peers (compile shapshot for documentation)
						try {
							Class<?> theClass = Class.forName(fullClassName);
							Method method = theClass.getMethod("getPeers", String.class);
							Object peers = method.invoke(si, new String[]{""});
							if (peers != null){
								log.info("here");
							}
						} catch (Exception e) {
							// dont care
						}

						sd.add(s);
					} catch (Exception e) {
						Logging.logException(e);
						continue;
					}

					// TODO - rip bad threads down

				} else {
					log.info("skipping directory {}", sf);
				}
			}

			return sd;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return null;
	}

	public static void main(String[] args) {
		try {

			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel("INFO");
			LoggingFactory.getInstance().addAppender(Appender.FILE);
			ServiceData sd = ServiceData.load(FileIO.fileToString(".myrobotlab/serviceData.json"));
			//ServiceData sd = generate("../repo");
			/*
			ServiceType st = sd.getServiceType("org.myrobotlab.service.Arduino");
			st.addDependency("cc.arduino");
			*/
			sd.addCategory("actuators", "motion controllers", new String[]{"org.myrobotlab.service.Motor", "org.myrobotlab.service.Servo","org.myrobotlab.service.MouthControl","org.myrobotlab.service.PID"});
			sd.addCategory("audio", "", new String[]{"org.myrobotlab.service.AudioCapture", "org.myrobotlab.service.AudioFile","org.myrobotlab.service.JFugue"});
			// ServiceData sd = ServiceData.getLocal();// .loadLocal();
			sd.save("generated.json");

			/*
			 * Serializer serializer = new Persister();
			 * 
			 * File cfg = new File("serviceData.test.json");
			 * serializer.write(serviceData, cfg);
			 */

			log.info("here");

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}

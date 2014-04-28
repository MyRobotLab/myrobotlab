package org.myrobotlab.framework;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ivy.Main;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.util.cli.CommandLineParser;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.fileLib.FindFile;
import org.myrobotlab.fileLib.Zip;
import org.myrobotlab.framework.ServiceData.CategoryList;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.HTTPRequest;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.slf4j.Logger;

/**
 * Singleton implementation for service information.
 * 
 * @author GroG
 * 
 */
public class ServiceInfo implements Serializable {
	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(ServiceInfo.class.toString());

	public HashSet<String> nativeFileExt = new HashSet<String>(Arrays.asList("dll", "so", "dylib", "jnilib"));
	
	private ServiceData serviceData = new ServiceData();
	private ServiceData serviceDataFromRepo = new ServiceData();
	private final List<String> errors;
	private final String ivyFileName;
	private final String settings;

	/**
	 * Singleton constructor
	 */
	public ServiceInfo() {
		errors = new ArrayList<String>();
		// TODO this should probably be a config value
		ivyFileName = "ivychain.xml";
		// TODO should this be a config value tied to versioning?
		settings = "latest.integration";
	}

	/**
	 * 
	 * @param shortName
	 * @param category
	 */
	public void addCategory(String shortName, String category) {
		// TODO - should there be a null check on shortName?
		// TODO - bury all this in ServiceData
		if (serviceData.categories == null) {
			serviceData.categories = new TreeMap<String, CategoryList>();
		}

		String fullname = String.format("org.myrobotlab.service.%1$s", shortName);
		// ArrayList<String>list = null;
		ServiceData.CategoryList list = null;
		if (serviceData.categories.containsKey(shortName)) {
			list = serviceData.categories.get(fullname);
		}
		// make sure we didn't store a null value somewhere else
		if (list == null) {
			list = new ServiceData.CategoryList();
			serviceData.categories.put(fullname, list);
		}
		list.services.add(category);
	}

	/**
	 * can only be called after getting data from the repo will return the first
	 * upgradable Dependency from a service
	 * 
	 * @param fullServiceType
	 * @return
	 */
	public List<Dependency> checkForUpgrade(String fullServiceType) {
		// look through Service's dependencies and see if
		// a newer one is avilable from the repo

		// ServiceDescriptor local =
		// serviceData.serviceInfo.get(fullServiceType);
		ServiceDescriptor fromRepo = serviceDataFromRepo.serviceInfo.get(fullServiceType);

		List<Dependency> deps = new ArrayList<Dependency>();

		if (fromRepo == null) {
			return deps;
		}
		String dependencyName;
		Dependency localDep;
		Dependency repoDep;
		for (int i = 0; i < fromRepo.size(); ++i) {
			dependencyName = fromRepo.get(i);
			localDep = serviceData.thirdPartyLibs.get(dependencyName);
			repoDep = serviceDataFromRepo.thirdPartyLibs.get(dependencyName);

			if (localDep == null || repoDep == null || localDep.version == null) {
				log.info("null");
			}

			// TODO clean up this boolean with some parenthesis in order to make
			// it clearer
			if (!((localDep == null) || // new dependency on repo
			localDep != null && localDep.version != null && repoDep != null && !localDep.version.equals(repoDep.version))) {
				continue;
			}
			deps.add(repoDep);
		}
		return deps;
	}

	/**
	 * Clear the errors.
	 */
	public void clearErrors() {
		errors.clear();
	}

	/**
	 * Get the list of errors.
	 * 
	 * @return
	 */
	public List<String> getErrors() {
		// TODO this is not thread safe - returning a reference to a singleton
		// list
		return errors;
	}

	/**
	 * Get all the keys.
	 * 
	 * @return
	 */
	public Set<String> getKeySet() {
		return serviceData.serviceInfo.keySet();
	}

	/**
	 * this method looks in the .ivy cache directory for resolved dependencies
	 * and builds a master map of third party libs which are on the local system
	 */
	public boolean getLocalResolvedDependencies() {
		boolean ret = false;

		// clear local resolved serviceInfo
		serviceData.thirdPartyLibs.clear();

		File ivyDir = new File(".ivy"); // FIXME - consolidate all ".ivy"
										// references to single assignment - use
										// var/properties
		if (!ivyDir.exists()) {
			log.warn(".ivy dir does not exist");
			return false;
		}
		// load .ivy cache
		try {
			List<File> files = FindFile.find(".ivy", "resolved.*\\.xml$");
			String org;
			String module;
			String versionFile;
			String version;
			Dependency dependency;
			File componentsDir;
			for (File file : files) {
				org = file.getName();
				org = org.substring(org.indexOf("-") + 1);
				org = org.substring(0, org.indexOf("-"));

				module = org.substring(org.lastIndexOf(".") + 1);

				// String contents = FileIO.fileToString(file.getPath());

				versionFile = FileIO.fileToString(String.format(".ivy/%1$s/%2$s/ivydata-%3$s.properties", org, module, settings));

				version = null;
				// hack - more cheesy parsing
				if (versionFile != null && versionFile.length() > 18) {
					version = versionFile.substring(versionFile.indexOf("resolved.revision=") + 18);
					int endPos = version.indexOf("\r");
					if (endPos > -1) {
						version = version.substring(0, endPos);
					} else {
						version = "0";
					}
				}
				log.debug(String.format("adding dependency %1$s %2$s to local thirdPartyLib", org, version));
				dependency = new Dependency(org, module, version, false);

				componentsDir = new File(".ivy/" + org);
				if (componentsDir.exists()) {
					dependency.released = true;
					dependency.resolved = true;
				}
				serviceData.thirdPartyLibs.put(org, dependency);
			}
		} catch (FileNotFoundException e) {
			Logging.logException(e);
			return false;
		}

		return ret;
	}

	/**
	 * default load, loads both the serviceData.xml file and the local .ivy
	 * cache into memory.
	 */
	public boolean getLocalServiceData() {
		boolean ret = loadXML(serviceData, "serviceData.xml");

		if (!ret) {
			log.warn("no local file found - fetching repo version");
			ret = getRepoFile("serviceData.xml");
			// try again :)
			if (ret) {
				ret &= loadXML(serviceData, "serviceData.xml");
			}

		}

		ret &= getLocalResolvedDependencies();
		return ret;
	}

	/**
	 * 
	 * @return
	 */
	public boolean getRepoData() {
		// TODO - populate errors - make event
		// clear errors ? this is the beginning of a high level method

		// first get repo's serviceData.xml (do not cache it !)
		String repoFileName = "serviceData.repo.xml";

		// iterate through it and get all latest dependencies
		boolean ret = getRepoFile(repoFileName);
		ret &= loadXML(serviceDataFromRepo, repoFileName);

		// iterate through services's dependencies
		Iterator<String> it = serviceDataFromRepo.serviceInfo.keySet().iterator();
		String sn;
		ServiceDescriptor sd;
		String dependencyRef;
		Dependency dependency;
		while (it.hasNext()) {
			sn = it.next();
			sd = serviceDataFromRepo.serviceInfo.get(sn);

			// iterate through dependencies - adding to thirdparty libs
			for (int i = 0; i < sd.size(); ++i) {
				dependencyRef = sd.get(i);
				if (!serviceDataFromRepo.thirdPartyLibs.containsKey(dependencyRef)) {
					dependency = getRepoLatestDependencies(dependencyRef);
					serviceDataFromRepo.thirdPartyLibs.put(dependency.organisation, dependency);
				}
			}
		}

		save(serviceDataFromRepo, "serviceData.repo.processed.xml");
		return ret;
	}

	/**
	 * 
	 * @param org
	 * @return
	 */
	public Dependency getRepoLatestDependencies(String org) {

		String module = org.substring(org.lastIndexOf(".") + 1);
		try {
			HTTPRequest http = new HTTPRequest(String.format("http://myrobotlab.googlecode.com/" + "svn/trunk/myrobotlab/thirdParty/repo/%1$s/%2$s/", org, module));
			String s = http.getString();
			if (s == null) {
				return null;
			}
			// ---- begin fragile & ugly parsing -----
			// reverse pos from bottom to find the end of the list of
			// directories
			// to start the pos
			String latestVersion = s.substring(s.lastIndexOf("<li><a href=\"") + 13);
			latestVersion = latestVersion.substring(0, latestVersion.indexOf("/\">"));
			if (!"..".equals(latestVersion)) {
				return new Dependency(org, module, latestVersion, true);
			}
			// ---- end fragile & ugly parsing -----
		} catch (Exception e) {
			Logging.logException(e);
			errors.add(e.getMessage());
		}
		return null;
	}

	/**
	 * 
	 * @param localFileName
	 * @return
	 */
	public boolean getRepoFile(String localFileName) {
		try {
			// TODO this should be a configuration value of some sort
			HTTPRequest http = new HTTPRequest("http://myrobotlab.googlecode.com/svn/trunk/myrobotlab/thirdParty/repo/serviceData.xml");
			String s = http.getString();
			if (s != null && localFileName != null) {
				FileIO.stringToFile(Service.getCFGDir() + File.separator + localFileName, s);
				return true;
			}
		} catch (Exception e) {
			Logging.logException(e);
			errors.add(e.getMessage());
		}
		return false;
	}

	/**
	 * function to return an array of serviceInfo for the Runtime So that Ivy
	 * can download, cache, and manage all the appropriate serviceInfo for a
	 * Service. TODO - make this function abstract and force implementation.
	 * 
	 * @return Array of serviceInfo to be retrieved from the repo
	 */
	public List<String> getRequiredDependencies(String fullname) {
		if (serviceDataFromRepo != null && serviceDataFromRepo.serviceInfo.containsKey(fullname)) {
			ServiceDescriptor d = serviceDataFromRepo.serviceInfo.get(fullname);
			List<String> list = new ArrayList<String>();
			for (int i = 0; i < d.size(); ++i) {
				list.add(d.get(i));
			}
			return list; // repo has precedence
		}

		if (!serviceData.serviceInfo.containsKey(fullname)) {
			// TODO Required should probably throw instead of returning null
			return null;
		}
		List<String> list = new ArrayList<String>();
		ServiceDescriptor d = serviceData.serviceInfo.get(fullname);
		for (int i = 0; i < d.size(); ++i) {
			list.add(d.get(i));
		}
		return list;
	}

	/**
	 * 
	 * @return
	 */
	public String[] getSimpleNames() {
		return getSimpleNames(null);
	}

	/**
	 * 
	 * @param filter
	 * @return
	 */
	public String[] getSimpleNames(String filter) {
		ArrayList<String> sorted = new ArrayList<String>();

		Iterator<String> it = serviceData.serviceInfo.keySet().iterator();
		String sn;
		ServiceData.CategoryList cats;
		while (it.hasNext()) {
			sn = it.next();
			if (filter == null || filter.equals("") || filter.equals("all")) {
				sorted.add(sn.substring(sn.lastIndexOf('.') + 1));
				continue;
			}
			cats = serviceData.categories.get(sn);
			if (cats == null) {
				continue;
			}
			for (int i = 0; i < cats.size(); ++i) {
				if (!filter.equals(cats.get(i))) {
					continue;
				}
				sorted.add(sn.substring(sn.lastIndexOf('.') + 1));
			}
		}
		Collections.sort(sorted);
		return sorted.toArray(new String[sorted.size()]);
	}

	/**
	 * 
	 * @return
	 */
	public String[] getUniqueCategoryNames() {
		ArrayList<String> sorted = new ArrayList<String>();
		HashMap<String, String> normal = new HashMap<String, String>();
		Iterator<String> it = serviceData.categories.keySet().iterator();

		String sn;
		ServiceData.CategoryList al;
		while (it.hasNext()) {
			sn = it.next();
			al = serviceData.categories.get(sn);
			for (int i = 0; i < al.size(); ++i) {
				normal.put(al.get(i), null);
			}
		}

		it = normal.keySet().iterator();
		while (it.hasNext()) {
			sn = it.next();
			sorted.add(sn);
		}

		Collections.sort(sorted);
		return sorted.toArray(new String[sorted.size()]);
	}

	/**
	 * Check if there are any errors recorded.
	 * 
	 * @return
	 */
	public boolean hasErrors() {
		return errors.size() > 0;
	}

	/**
	 * 
	 * @param fullServiceName
	 * @return
	 */
	public boolean hasUnfulfilledDependencies(String fullServiceName) {
		boolean ret = false;
		// log.debug(String.format("inspecting %1$s for unfulfilled dependencies",
		// fullServiceName));

		// no serviceInfo
		if (!serviceData.serviceInfo.containsKey(fullServiceName)) {
			log.error(String.format("need full service name ... got %1$s", fullServiceName));
			return false;
		}

		ServiceDescriptor d = serviceData.serviceInfo.get(fullServiceName);
		if (d == null || d.size() == 0) {
			log.error(String.format("no service descriptor for %1$s", fullServiceName));
			return false;
		}
		Dependency dep;
		for (int i = 0; i < d.size(); ++i) {
			if (!serviceData.thirdPartyLibs.containsKey(d.get(i))) {
				// log.debug(String.format("%1$s can not be found in current thirdPartyLibs",
				// d.get(i)));
				// log.debug("hasUnfulfilledDependencies exit true");
				return true;
			}
			dep = serviceData.thirdPartyLibs.get(d.get(i));
			if (dep.resolved) {
				continue;
			}
			log.debug("hasUnfulfilledDependencies exit true");
			return true;
		}
		// log.debug(String.format("hasUnfulfilledDependencies exit %1$b",
		// ret));
		return ret;
	}

	/**
	 * 
	 * @param o
	 * @param inCfgFileName
	 * @return
	 */
	public boolean loadXML(Object o, String inCfgFileName) {
		String filename = null;
		if (inCfgFileName == null) {
			filename = String.format("%1$s%2$s.xml", Service.getCFGDir(), File.separator);
		} else {
			filename = String.format("%1$s%2$s%3$s", Service.getCFGDir(), File.separator, inCfgFileName);
		}
		File cfg = new File(filename);
		if (!cfg.exists()) {
			log.warn("cfg file " + filename + " does not exist");
			return false;
		}
		if (o == null) {
			o = this;
		}
		Serializer serializer = new Persister();
		try {
			serializer.read(o, cfg);
		} catch (Exception e) {
			Logging.logException(e);
			return false;
		}
		return true;
	}

	/**
	 * gets thirdPartyLibs of a Service using Ivy interfaces with Ivy using its
	 * command parameters
	 * 
	 * @param fullTypeName
	 */
	// TODO - interface to Ivy2 needs to be put into ServiceInfo
	// resolve here "means" retrieve
	public boolean resolve(String fullTypeName) {
		log.debug(String.format("getDependencies %1$s", fullTypeName));

		org.myrobotlab.service.Runtime runtime = org.myrobotlab.service.Runtime.getInstance();

		File ivysettings = new File(ivyFileName);
		if (!ivysettings.exists()) {
			log.warn(String.format("%1$s does not exits - will not try to resolve dependencies", ivyFileName));
			return false;
		}
		List<String> d = getRequiredDependencies(fullTypeName);

		boolean ret = true;
		if (d == null) {
			log.info(String.format("%1$s returned no dependencies", fullTypeName));
		} else {
			log.info(String.format("%1$s found %2$d needed dependencies", fullTypeName, d.size()));
			ret = resolveDependencies(ret, d, runtime);
		}
		runtime.invoke("resolveEnd");

		return ret;
	}

	/**
	 * default save saves the memory serviceData to .myrobotlab/serviceData.xml
	 */
	public boolean save(ServiceData data, String filename) {
		Serializer serializer = new Persister();

		try {
			File cfgdir = new File(Service.getCFGDir());
			if (!cfgdir.exists()) {
				cfgdir.mkdirs();
			}
			File cfg = new File(Service.getCFGDir() + File.separator + filename);
			serializer.write(data, cfg);
		} catch (Exception e) {
			Logging.logException(e);
			return false;
		}
		return true;
	}

	// FIXME - need update(fullTypeName); !!!
	/**
	 * 
	 * @return
	 */
	public boolean update() {

		// load up the serviceData.xml and .ivy cache
		// NOTE - it is the responsibility of some other system
		// to call getRepoServiceData - if a new service & categories
		// definition is wanted
		getLocalServiceData();

		// ask for resolution without retrieving
		Iterator<String> it = getKeySet().iterator();
		while (it.hasNext()) {
			resolve(it.next());
		}
		// TODO - return list object - for event processing on caller
		// TODO - re-check local after processing Ivy - so new Services can be
		// loaded or
		// after reboot -
		return false;
	}

	/**
	 * 
	 * @param dependency
	 * @param module
	 * @return
	 */
	private List<String> getIvyOptions(String dependency, String module) {
		List<String> options = new ArrayList<String>();

		options.add("-cache");
		options.add(".ivy");

		options.add("-retrieve");
		options.add("libraries/[type]/[artifact].[ext]");

		options.add("-settings");
		// cmd.add("ivysettings.xml");
		options.add(ivyFileName);

		// cmd.add("-cachepath");
		// cmd.add("cachefile.txt");

		options.add("-dependency");
		options.add(dependency); // org
		options.add(module); // module
		// cmd.add(dep.version); // version
		options.add(settings);

		options.add("-confs");
		options.add(String.format("runtime,%1$s.%2$d.%3$s", Platform.getArch(), Platform.getBitness(), Platform.getOS()));

		return options;
	}

	/**
	 * 
	 * java -jar libraries\jar\ivy.jar -cache .ivy -settings ivychain.xml
	 * -dependency org.myrobotlab myrobotlab "latest.integration" java -jar
	 * libraries\jar\ivy.jar -cache .ivy -retrieve
	 * libraries/[type]/[artifact].[ext] -settings ivychain.xml -dependency
	 * org.myrobotlab myrobotlab "latest.integration"
	 * 
	 * @param status
	 * @param dependencies
	 * @param runtime
	 * @return
	 */
	private boolean resolveDependencies(boolean status, List<String> dependencies, org.myrobotlab.service.Runtime runtime) {
		if (dependencies == null || dependencies.size() == 0) {
			return status;
		}
		boolean ret = status;
		try {
			String dep;
			String module;
			List<String> cmd;
			StringBuilder sb;
			CommandLineParser parser;
			ResolveReport report;
			ArtifactDownloadReport[] artifacts;
			ArtifactDownloadReport artifact;
			String filename;
			for (int i = 0; i < dependencies.size(); ++i) {
				dep = dependencies.get(i);
				module = dep.substring(dep.lastIndexOf(".") + 1);
				cmd = getIvyOptions(dep, module);

				// show cmd params
				sb = new StringBuilder();
				for (int k = 0; k < cmd.size(); ++k) {
					sb.append(cmd.get(k));
					sb.append(" ");
				}

				// TODO - generate a valid Ivy xml file
				log.info(String.format("Ivy2.run %1$s", sb.toString()));

				parser = Main.getParser();

				runtime.invoke("resolveBegin", module);
				try {
					Ivy2.run(parser, cmd.toArray(new String[cmd.size()]));
				} catch (Exception e) {
					Logging.logException(e);
				}

				report = Ivy2.getReport();
				artifacts = report.getAllArtifactsReports();

				if (report.hasError()) {
					ret = false;
					log.error("Ivy resolve error");
					@SuppressWarnings("unchecked")
					List<String> l = (List<String>) report.getAllProblemMessages();
					runtime.invoke("resolveError", l);
					for (int j = 0; j < l.size(); ++j) {
						log.error(l.get(j));
					}
					continue;
				}

				if (report.getDownloadSize() > 0) {
					log.info("downloaded new artifacts");
					runtime.invoke("newArtifactsDownloaded", module);
				} else {
					log.info("no new artifacts");
				}

				runtime.invoke("resolveSuccess", module);
				for (int j = 0; j < artifacts.length; ++j) {
					artifact = artifacts[j];
					if (artifact.getExt().equals("zip")) {
						filename = String.format("libraries/zip/%s.zip", artifact.getName());
						Zip.unzip(filename, "./");
					}
					
					if (nativeFileExt.contains(artifact.getExt().toLowerCase())) {
						File f = artifact.getLocalFile();
						String abpath = String.format("%s", f.getAbsolutePath());
						String source = String.format("%s",artifact.getName());
						String target = String.format("%s",artifact.getName());
						
						//log.info("cp %")
						//Files.move(source, "libraries/native", StandardCopyOption.REPLACE_EXISTING);
					}
					log.info("artifacts {}",artifacts[j]);
				}
			}
		} catch (Exception e) {
			Logging.logException(e);
			ret = false;
		}
		return ret;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		boolean update = true;
		ServiceInfo info = new ServiceInfo();//.getInstance();

		// set defaults [update all | update none] depending on context

		// get local data
		info.getLocalServiceData();

		info.save(info.serviceData, "serviceData.processed.xml");

		// get remote data
		info.getRepoData();

		// generate update report / dialog

		// get user input (or accept defaults [update all | update none])

		// perform actions

		log.info("latest dependencies {}", info.getRepoLatestDependencies("org.myrobotlab"));
		log.info("{}", info.getRepoLatestDependencies("org.apache.log4j"));
		log.info("{}", info.getRepoLatestDependencies("edu.cmu.sphinx"));
		log.info("{}", info.getRepoLatestDependencies("org.apache.ivy"));

		try {
			java.lang.Runtime.getRuntime().exec("cmd /c start myrobotlab.bat");
			java.lang.Runtime.getRuntime().exec("myrobotlab.sh");
		} catch (IOException e) {
			org.myrobotlab.service.Runtime.logException(e);
		}

		// load the possibly recent serviceData
		info.getLocalServiceData();

		// add the local ivy cache - TODO - rename to thirdPartyLibs
		info.getLocalResolvedDependencies();

		// TODO - verify all keys !
		// info.save();
	}

}

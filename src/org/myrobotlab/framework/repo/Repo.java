package org.myrobotlab.framework.repo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.Filter;
import org.apache.ivy.util.filter.NoFilter;
import org.apache.ivy.util.url.URLHandler;
import org.apache.ivy.util.url.URLHandlerDispatcher;
import org.apache.ivy.util.url.URLHandlerRegistry;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.fileLib.Zip;
import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.HTTPRequest;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

// FIXME - remove all references of Runtime - you must messages to an interface !

public class Repo implements Serializable {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Repo.class);

	public HashSet<String> nativeFileExt = new HashSet<String>(Arrays.asList("dll", "so", "dylib", "jnilib"));
	public final String REPO_BASE_URL = "https://raw.githubusercontent.com/MyRobotLab/repo/master";
	public static final Filter NO_FILTER = NoFilter.INSTANCE;

	// FYI ! - non of these can be transient
	// need this "local" repo from remote instances !!!
	// the one and only local to current running instance
	// private static Repo localInstance = null;
	private ServiceData localServiceData = null;
	private ServiceData remoteServiceData = null;

	private Platform platform;
	transient Ivy ivy = null;

	/**
	 * the Runtime's name which this Repo is operating in behalf of.
	 * There is a possiblity that this will be not a real name or null,
	 * in which case their will be no Runtime running - but Repo requests are
	 * still desired.
	 */
	public final String runtimeName;

	/**
	 * Repo default constructor is needed by serialization, although the
	 * initialization procedures in the constructor will add incorrect data if
	 * the Repo definition was from a different instance - the data will be
	 * overwritten by the serialization process (hopefully) and reflect the
	 * appropriate remote (peer) Repo info
	 */
	public Repo(String runtimeName) {
		this.runtimeName = runtimeName;

		// get my local platform
		platform = Platform.getLocalInstance();

		// load local file
		localServiceData = getServiceDataFile();
	}
	
	// pulled in dependencies .. not sure if that is good
	public void info(String format, Object... args) {
		ServiceInterface si = org.myrobotlab.service.Runtime.getService(runtimeName);
		
		if (si != null){
			si.invoke("updateProgress", Status.info(format, args));
		} else {
			log.info(String.format(format, args));
		}
	}
	
	// pulled in dependencies .. not sure if that is good
	public void error(String format, Object... args) {
		ServiceInterface si = org.myrobotlab.service.Runtime.getService(runtimeName);
		if (si != null){
			si.invoke("updateProgress", Status.error(format, args));
		} else {
			log.error(String.format(format, args));
		}
	}
	
	public void error(Exception e) {
		Logging.logException(e);
		error(e.getMessage());
	}

	public ServiceData getServiceDataFile() {
		return getServiceDataFile(null);
	}

	// FIXME - if remote is retrieved - there needs to be updating from cache ?
	// or not ???
	public ServiceData getServiceDataFile(String filename) {

		// return from memory - this will be returned
		// if this Repo has been transported over the network
		if (localServiceData != null) {
			return localServiceData;
		}

		// return from local file
		try {
			localServiceData = ServiceData.getLocal();
			return localServiceData;
		} catch (FileNotFoundException e) {
			info("local service data file not found");
		}

		// failed getting local - try remote
		// return from remote - last attempt
		remoteServiceData = getServiceDataFromRepo();

		if (remoteServiceData != null) {
			localServiceData = remoteServiceData;
			return localServiceData;
		}

		// all else fails - no local file - no remote - we will get
		// the serviceData packaged with the jar
		String sd = FileIO.resourceToString("framework/serviceData.xml");
		log.info(sd);
		if (sd == null){
			error("resource serviceData not found!");
			return null;
		}
		localServiceData = ServiceData.load(sd);

		return localServiceData;
	}

	public ServiceData getServiceDataFromRepo() {
		try {

			remoteServiceData = ServiceData.getRemote("https://raw.githubusercontent.com/MyRobotLab/repo/master/serviceData.xml");
			if (remoteServiceData == null) {
				error("could not get remote service data");
				return null;
			}
			String repoFileName = String.format("%s%sserviceData.xml", FileIO.getCfgDir(), File.separator);
			info("retrieved remote service data {}", repoFileName);
			remoteServiceData.save(repoFileName);

			return remoteServiceData;
		} catch (Exception e) {
			error(e.getMessage());
			Logging.logException(e);
		}
		return null;
	}

	public String getVersionFromRepo() throws IOException {

		String http_proxy = System.getProperty("http.proxyHost");
		String https_proxy = System.getProperty("https.proxyHost");
		log.info(String.format("http.proxyHost %s https.proxyHost %s", http_proxy, https_proxy));

		String listURL = "https://api.github.com/repos/MyRobotLab/myrobotlab/releases";

		info("trying %s", listURL);
		
		log.info(String.format("getting list of dist %s", listURL));
		HTTPRequest http = new HTTPRequest(listURL);
		String s = http.getString();
		//info(String.format("recieved [%s]", s));

		info("parsing");

		GitHubRelease[] releases = Encoder.gson.fromJson(s, GitHubRelease[].class);
		if (releases == null){
			error("Are you connected to intertoobs?");
			throw new IOException("Are you connected to intertoobs?");
		}
		info(String.format("found %d releases", releases.length));

		String[] r = new String[releases.length];
		for (int i = 0; i < releases.length; ++i) {
			r[i] = releases[i].tag_name;
		}

		Arrays.sort(r);
		
		info("finished parsing and sorting");

		if (r.length > 0) {
			String repoVersion = r[r.length - 1];
			info("returning release string %s", repoVersion);
			return repoVersion;
		} else {
			error("could not get latest version information");
			throw new IOException("could not get latest version information");
		}

	}

	public String getServiceDataURL() {
		return String.format("%s/serviceData.xml", REPO_BASE_URL);
	}

	public boolean getLatestJar() {

		try {
			info("getting latest jar");
			String version = getVersionFromRepo();
			if (version == null) {
				error("could not get latest version from github");
				return false;
			}
			String latestMRLJar = "https://github.com/MyRobotLab/myrobotlab/releases/download/" + version + "/myrobotlab.jar";
			info(String.format("getting latest build from %s", latestMRLJar));
			HTTPRequest zip = new HTTPRequest(latestMRLJar);
			byte[] jarfile = zip.getBinary();

			File updateDir = new File("update");
			updateDir.mkdir();
			File backupDir = new File("backup");
			backupDir.mkdir();

			FileOutputStream out = new FileOutputStream("update/myrobotlab.jar");
			try {
				out.write(jarfile);
				info("getLatestJar - ready for bootstrap");
				return true;
			} catch (Exception e) {
				error(e);
			} finally {
				out.close();
			}

		} catch (IOException e) {
			error(e.getMessage());
			Logging.logException(e);
		}
		
		return false;
	}

	/**
	 * this method attempts to get all update information from the repo it is
	 * not automatically applied - the update info is just retrieved and
	 * compared the resulting Updates data can then be used to apply against
	 * local repo to process updates
	 */
	public Updates checkForUpdates() {
		Updates updates = new Updates(runtimeName);
		try {
			info("=== checking for updates begin ===");

			updates.currentVersion = getVersion();
			updates.repoVersion = getVersionFromRepo();

			info(String.format("current %s latest %s", updates.currentVersion, updates.repoVersion));

			updates.localServiceData = localServiceData;
			remoteServiceData = ServiceData.getRemote(getServiceDataURL());
			updates.remoteServiceData = remoteServiceData;

			// TODO compare localServiceData with remoteServiceData - determine
			// removed | updated | new

			log.info(String.format("Updates.hasJarUpdate %s", updates.hasJarUpdate()));
			updates.isValid = true;
			log.info("=== checking for updates end ===");
		} catch (NoSuchElementException e) {
			Logging.logException(e);
			updates.lastError = "I think it was evil gnomes !";
		} catch (Exception ex){
			Logging.logException(ex);
			updates.lastError = ex.getMessage();
		}
		return updates;

	}

	public String getVersion() {
		return platform.getVersion();
	}

	// TODO - getLocalResolvedDependencies

	public boolean isServiceTypeInstalled(String fullTypeName) {
		if (localServiceData != null) {
			ServiceType st = localServiceData.getServiceType(fullTypeName);
			if (st == null){
				error("unknown service %s", fullTypeName);
				return false;
			}
			if (st.dependencyList != null) {
				for (int i = 0; i < st.dependencyList.size(); ++i) {
					String d = st.dependencyList.get(i);
					if (!localServiceData.containsDependency(d)) {
						error(String.format("service type %s does not have defined dependency %s", st.name, d));
						return false;
					}

					Dependency dep = localServiceData.getDependency(d);
					if (!dep.isResolved()) {
						// log.warn("service type %s has unresolved dependency %s",
						// st.name, d);
						return false;
					}
				}
			}
		}
		return true;
	}

	public Platform getPlatform() {
		return platform;
	}

	public ResolveReport retrieveArtifacts(String org, String revision) throws Exception {
		return resolveArtifacts(org, revision, true);
	}

	/**
	 * resolveArtifact does an Ivy resolve with a URLResolver to MRL's repo at
	 * github. The equivalent command line is -settings ivychain.xml -dependency
	 * "gnu.io.rxtx" "rxtx" "2.1-7r2" -confs "runtime,x86.64.windows"
	 * 
	 * @param org
	 * @param revision
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 * @throws Exception
	 */
	// FIXME - should have a better return than just Files
	synchronized public ResolveReport resolveArtifacts(String org, String revision, boolean retrieve) throws ParseException, IOException {
		info("%s %s.%s", (retrieve)?"retrieve":"resolve", org, revision);
		// creates clear ivy settings
		// IvySettings ivySettings = new IvySettings();
		String module;
		int p = org.lastIndexOf(".");
		if (p != -1) {
			module = org.substring(p + 1, org.length());
		} else {
			module = org;
		}

		// creates an Ivy instance with settings
		// Ivy ivy = Ivy.newInstance(ivySettings);
		if (ivy == null) {
			ivy = Ivy.newInstance();
			ivy.getLoggerEngine().pushLogger(new DefaultMessageLogger(Message.MSG_DEBUG));

			// PROXY NEEDED ?
			// CredentialsStore.INSTANCE.addCredentials(realm, host, username,
			// passwd);

			URLHandlerDispatcher dispatcher = new URLHandlerDispatcher();
			URLHandler httpHandler = URLHandlerRegistry.getHttp();
			dispatcher.setDownloader("http", httpHandler);
			dispatcher.setDownloader("https", httpHandler);
			URLHandlerRegistry.setDefault(dispatcher);

			File ivychain = new File("ivychain.xml");
			if (!ivychain.exists()) {
				try {
					String xml = FileIO.resourceToString("framework/ivychain.xml");
					FileOutputStream fos = new FileOutputStream(ivychain);
					fos.write(xml.getBytes());
					fos.close();
				} catch (Exception e) {
					Logging.logException(e);
				}
			}
			ivy.configure(ivychain);
			ivy.pushContext();

		}

		IvySettings settings = ivy.getSettings();
		settings.setDefaultCache(new File(System.getProperty("user.home"), ".repo"));
		settings.addAllVariables(System.getProperties());

		File cache = new File(settings.substitute(settings.getDefaultCache().getAbsolutePath()));

		if (!cache.exists()) {
			cache.mkdirs();
		} else if (!cache.isDirectory()) {
			error(cache + " is not a directory");
		}

		Platform platform = Platform.getLocalInstance();
		String platformConf = String.format("runtime,%s.%s.%s", platform.getArch(), platform.getBitness(), platform.getOS());
		log.info(String.format("requesting %s", platformConf));

		String[] confs = new String[] { platformConf };
		String[] dep = new String[] { org, module, revision };

		File ivyfile = File.createTempFile("ivy", ".xml");
		ivyfile.deleteOnExit();

		DefaultModuleDescriptor md = DefaultModuleDescriptor.newDefaultInstance(ModuleRevisionId.newInstance(dep[0], dep[1] + "-caller", "working"));
		DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md, ModuleRevisionId.newInstance(dep[0], dep[1], dep[2]), false, false, true);
		for (int i = 0; i < confs.length; i++) {
			dd.addDependencyConfiguration("default", confs[i]);
		}
		md.addDependency(dd);
		XmlModuleDescriptorWriter.write(md, ivyfile);
		confs = new String[] { "default" };

		ResolveOptions resolveOptions = new ResolveOptions().setConfs(confs).setValidate(true).setResolveMode(null).setArtifactFilter(NO_FILTER);

		ResolveReport report = ivy.resolve(ivyfile.toURI().toURL(), resolveOptions);
		List<?> errors = report.getAllProblemMessages();

		if (errors.size() > 0) {
			for (int i = 0; i < errors.size(); ++i){
				error(errors.get(i).toString());
			}
		} else {
			info("%s %s.%s for %s", (retrieve)?"retrieved":"resolved", org, revision, platform.getPlatformId());
		}
		// TODO - no error
		if (retrieve && errors.size() == 0) {

			// TODO check on extension here - additional processing

			String retrievePattern = "libraries/[type]/[artifact].[ext]";// settings.substitute(line.getOptionValue("retrieve"));

			String ivyPattern = null;
			int ret = ivy.retrieve(md.getModuleRevisionId(), retrievePattern, new RetrieveOptions().setConfs(confs).setSync(false)// check
					.setUseOrigin(false).setDestIvyPattern(ivyPattern).setArtifactFilter(NO_FILTER).setMakeSymlinks(false).setMakeSymlinksInMass(false));

			log.info("retrieve returned {}", ret);

			Dependency dependency = localServiceData.getDependency(org);
			if (dependency == null) {
				error("successfully resolved dependency - but it is not defined in local serviceData.xml");
				return report;
			}

			if (!dependency.getRevision().equals(revision)) {
				error(String.format("forcing revision of %s with revision %s for dependency %s", revision, dependency.getRevision(), org));
				dependency.setRevision(revision);
			}

			dependency.setResolved(true);
			localServiceData.save();

			// TODO - retrieve should mean unzip from local cache -> to root of
			// execution
			ArtifactDownloadReport[] artifacts = report.getAllArtifactsReports();
			for (int i = 0; i < artifacts.length; ++i) {
				ArtifactDownloadReport ar = artifacts[i];
				Artifact artifact = ar.getArtifact();
				File file = ar.getLocalFile();
				log.info("{}", file.getAbsoluteFile());
				// FIXME - native move up one directory !!! - from denormalized back to normalized Yay!
				// maybe look for PlatformId in path ? 
				if (ret == 1 && "zip".equalsIgnoreCase(artifact.getType())) {
					String filename = String.format("libraries/zip/%s.zip", artifact.getName());
					info("unzipping %s", filename);
					Zip.unzip(filename, "./");
					info("unzipped %s", filename);
				}
			}

		}

		return report;
	}

	public ArrayList<Dependency> getDependencies(String fullServiceName) {
		ServiceData sd = getServiceDataFile();
		ArrayList<Dependency> deps = new ArrayList<Dependency>();
		if (sd.containsServiceType(fullServiceName)) {
			ArrayList<String> orgs = sd.getServiceType(fullServiceName).dependencyList;
			if (orgs != null) {
				// Dependency[] deps = new Dependency[orgs.size()];
				for (int i = 0; i < orgs.size(); ++i) {
					deps.add(sd.getDependency(orgs.get(i)));
				}
				return deps;
			}
		} else {
			error(String.format("getDepenencies %s not found", fullServiceName));
		}
		return null;
	}

	public ArrayList<ResolveReport> retrieveServiceType(String fullTypeName) throws ParseException, IOException {
		
	ArrayList<ResolveReport> reports = new ArrayList<ResolveReport>();
		ArrayList<Dependency> deps = getDependencies(fullTypeName);
		if (deps != null){
		for (int i = 0; i < deps.size(); ++i) {
			Dependency dep = deps.get(i);
			if (!dep.isResolved()) {
				ResolveReport report = resolveArtifacts(dep.getOrg(), dep.getRevision(), true);
				reports.add(report);

				if (report.hasError()) {
					// TODO - invoke through "myRuntime"
					// INTERFACE - through message sink / message source
					// interface
					List<?> problems = report.getAllProblemMessages();
					for (int j = 0; j < problems.size(); ++j) {
						Object problem = problems.get(j);
						// error(problem.toString()); - already prints out when retrieved
					}
				}

			}
		}
		
		} else {
			// FIXME - fill reports with HAPPY ENTRY :D
			log.info("%s is free of dependencies ", fullTypeName);
		}

		return reports;
	}

	public void save() {
		localServiceData.save();
	}

	public ArrayList<ServiceType> getServiceTypes() {
		return localServiceData.getServiceTypes();
	}

	public ArrayList<ResolveReport> retrieveAll() {
		ArrayList<ResolveReport> reports = new ArrayList<ResolveReport>();
		String[] orgs = localServiceData.getServiceTypeDependencies();
		for (int i = 0; i < orgs.length; ++i) {
			String org = orgs[i];
			Dependency dep = localServiceData.getDependency(org);
			try {
				ResolveReport report = retrieveArtifacts(dep.getOrg(), dep.getRevision());
				reports.add(report);
			} catch (Exception e) {
				Logging.logException(e);
			}
		}

		return reports;
	}

	public void cleanCache() {

	}

	public static void test() throws Exception {

		/**
		 * TODO - test with all directories missing test as "one jar"
		 * 
		 * Use Cases : jar / no jar serviceData.xml - none, local, remote (no
		 * communication) / proxy / no proxy updateJar - no connection /
		 * connection / preserve main args - jvm parameters update repo - no
		 * connection / dependency affects others / single Service type / single
		 * Dependency update repo - new Service Type purge respawner - use
		 * always
		 * 
		 */

		// FIXME - sync serviceData with ivy cache & library

		// get local instance
		Repo repo = new Repo("test");

		repo.retrieveServiceType("org.myrobotlab.service.Arduino");

		Updates updates = repo.checkForUpdates();
		log.info(String.format("updates %s", updates));
		if (updates.hasJarUpdate()) {
			repo.getLatestJar();
		}

		// resolve All
		repo.retrieveAll();

		// repo.clear(org, revision) // whipes out cache for 1 dep
		// repo.clear() // whipes out cache

		// FIXME - no serviceData.xml = get from remote - will lose local cache
		// info

		// get service type names
		List<ServiceType> serviceTypes = repo.getServiceTypes();
		for (int i = 0; i < serviceTypes.size(); ++i) {
			ServiceType st = serviceTypes.get(i);
			if (repo.isServiceTypeInstalled(st.getName())) {
				log.info(String.format("%s installed", st));
			} else {
				log.info(String.format("%s not installed", st));
			}
		}

		// iterate through them see

		// resolve dependency for 1

		// resolve all dependencies

		// update jar

		// resolving
		repo.retrieveServiceType("org.myrobotlab.service.Arduino");

		// repo.getAllDepenencies();

		// remote tests

	}

	public static void main(String[] args) {
		try {
			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);

			Repo.test();

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}

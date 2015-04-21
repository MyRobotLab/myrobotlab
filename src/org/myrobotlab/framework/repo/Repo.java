package org.myrobotlab.framework.repo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

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
import org.myrobotlab.service.interfaces.RepoUpdateListener;
import org.slf4j.Logger;

// FIXME - remove all references of Runtime - you must messages to an interface !
// Update Listener

// FIXME
// clearRepo - whipes out all (calls other methods) <- not static
// clearRepoCache - wipes out .repo <- static since it IS static - only 1 on machine
// clearLibraries <- not static - this is per instance/installation
// clearServiceData <- not static - this is per instance/installation 

public class Repo implements Serializable {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Repo.class);

	public HashSet<String> nativeFileExt = new HashSet<String>(Arrays.asList("dll", "so", "dylib", "jnilib"));

	public final String REPO_BASE_URL = "https://raw.githubusercontent.com/MyRobotLab/repo/master";
	public static final Filter NO_FILTER = NoFilter.INSTANCE;

	ArrayList<String> errors = new ArrayList<String>();

	// FYI ! - non of these can be transient
	// need this "local" repo from remote instances !!!
	// the one and only local to current running instance
	// private static Repo localInstance = null;
	private ServiceData localServiceData = null;
	/**
	 * the Runtime's name which this Repo is operating in behalf of. There is a
	 * possiblity that this will be not a real name or null, in which case their
	 * will be no Runtime running - but Repo requests are still desired.
	 */

	private ServiceData remoteServiceData = null;

	private Platform platform;
	transient Ivy ivy = null; // we'll never use remote ivy only local

	/**
	 * call back notification of progress
	 */
	RepoUpdateListener listener = null;

	private boolean getRemoteRepo = false;

	static public String getLatestVersion(String[] versions) {

		if (versions == null || versions.length == 0) {
			return null;
		}

		int[][] ver = new int[versions.length][3];

		int major = 0;
		int minor = 0;
		int build = 0;

		int latestMajor = 0;
		int latestMinor = 0;
		int latestBuild = 0;

		int latestIndex = 0;

		for (int i = 0; i < versions.length; ++i) {
			try {
				major = 0;
				minor = 0;
				build = 0;

				String[] parts = versions[i].split("\\.");
				major = Integer.parseInt(parts[0]);
				minor = Integer.parseInt(parts[1]);
				build = Integer.parseInt(parts[2]);
			} catch (Exception e) {
				log.error(e.getMessage());
			}

			if (major > latestMajor) {
				latestMajor = major;
				latestMinor = minor;
				latestBuild = build;
				latestIndex = i;
			} else if (major == latestMajor) {
				// go deeper (minor)
				if (minor > latestMinor) {
					latestMajor = major;
					latestMinor = minor;
					latestBuild = build;
					latestIndex = i;
				} else if (minor == latestMinor) {
					// go deeper (build)
					if (build > latestBuild) {
						latestMajor = major;
						latestMinor = minor;
						latestBuild = build;
						latestIndex = i;
					}
				}
			}
		}

		return versions[latestIndex];
	}

	public static void main(String[] args) {
		try {
			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);

			Repo.test();

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public static void test() throws Exception {

		/**
		 * TODO - test with all directories missing test as "one jar"
		 * 
		 * Use Cases : jar / no jar serviceData.json - none, local, remote (no
		 * communication) / proxy / no proxy updateJar - no connection /
		 * connection / preserve main args - jvm parameters update repo - no
		 * connection / dependency affects others / single Service type / single
		 * Dependency update repo - new Service Type purge respawner - use
		 * always
		 * 
		 */

		// FIXME - sync serviceData with ivy cache & library

		// get local instance
		Repo repo = new Repo();

		String[] versions = { "1.0.100", "1.0.101", "1.0.102", "1.0.104", "1.0.105", "1.0.106", "1.0.107", "1.0.92", "1.0.93", "1.0.94", "1.0.95", "1.0.96", "1.0.97", "1.0.98",
				"1.0.99" };

		String latest = Repo.getLatestVersion(versions);
		log.info(latest);

		// assert "1.0.107" == latest ->

		if (!repo.isServiceTypeInstalled("org.myrobotlab.service.InMoov")) {
			log.info("not installed");
		} else {
			log.info("is installed");
		}

		repo.install("org.myrobotlab.service.Arduino");

		Updates updates = repo.checkForUpdates();
		log.info(String.format("updates %s", updates));
		if (updates.hasJarUpdate()) {
			repo.getLatestJar();
		}

		// resolve All
		repo.retrieveAll();

		// repo.clear(org, revision) // whipes out cache for 1 dep
		// repo.clear() // whipes out cache

		// FIXME - no serviceData.json = get from remote - will lose local cache
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
		repo.install("org.myrobotlab.service.Arduino");

		// repo.getAllDepenencies();

		// remote tests

	}

	/**
	 * Repo default constructor is needed by serialization, although the
	 * initialization procedures in the constructor will add incorrect data if
	 * the Repo definition was from a different instance - the data will be
	 * overwritten by the serialization process (hopefully) and reflect the
	 * appropriate remote (peer) Repo info
	 */
	public Repo() {

		// get my local platform
		platform = Platform.getLocalInstance();

		// load local file
		localServiceData = getServiceDataFile();
	}

	public void addRepoUpdateListener(RepoUpdateListener listener) {
		this.listener = listener;
	}

	/**
	 * this method attempts to get all update information from the repo it is
	 * not automatically applied - the update info is just retrieved and
	 * compared the resulting Updates data can then be used to apply against
	 * local repo to process updates
	 */
	public Updates checkForUpdates() {
		Updates updates = new Updates();
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
			Logging.logError(e);
			updates.lastError = "I think it was evil gnomes !";
		} catch (Exception ex) {
			Logging.logError(ex);
			updates.lastError = ex.getMessage();
		}
		return updates;

	}

	public boolean clearLibraries() {
		return clearLibraries(null);
	}

	public boolean clearLibraries(Set<File> exclude) {
		File libraries = new File("libraries");
		if (libraries.exists()) {
			if (!FileIO.rmDir(libraries, exclude)) {
				log.error(String.format("could not remove %s", libraries.getAbsolutePath()));
				return false;
			}
		} else {
			log.info("libraries does not exist - its clean !");
		}
		return true;
	}

	/**
	 * clears all files from local cache, serviceData.json, and libraries
	 */
	public boolean clearRepo() {
		boolean ret = true;
		ret &= clearRepoCache(null);
		ret &= clearLibraries(null);
		ret &= clearServiceData();
		return ret;
	}

	/**
	 * clears local cache, serviceData.json, and libraries selectively cleans -
	 * excludes will be preserved
	 */
	public boolean clearRepoCache(Set<File> exclude) {

		boolean ret = true;
		String cacheDir = String.format("%s%s.repo", System.getProperty("user.home"), File.separator);
		log.info(String.format("cleanCache [%s]", cacheDir));

		File cache = new File(cacheDir);
		if (cache.exists()) {
			log.info(String.format("%s exists we need to remove it", cacheDir));
			if (!FileIO.rmDir(new File(cacheDir), exclude)) {
				log.error(String.format("could not remove cache [%s]", cacheDir));
				return false;
			}

		} else {
			log.info(String.format("cache %s does not exist - it's clean !", cacheDir));
		}

		ret &= clearLibraries(exclude);
		return ret;
	}

	/**
	 * clears local service data json file and localServiceData memory so that
	 * subsequent calls to the repo force dependency resolution from the local
	 * (.repo) ..
	 * 
	 * used after clearLibraries - to clear local mrl instance files but still
	 * use local (.repo) cache
	 * 
	 * @return
	 */
	public boolean clearServiceData() {
		String serviceDataFileName = String.format("%s%sserviceData.json", FileIO.getCfgDir(), File.separator);
		File serviceData = new File(serviceDataFileName);

		if (serviceData.exists()) {
			// we must remove it
			log.info(String.format("%s exists we need to remove it", serviceDataFileName));
			if (!serviceData.delete()) {
				log.error(String.format("could not delete %s", serviceDataFileName));
				return false;
			}
		}

		localServiceData = null;

		return true;
	}

	public void error(Exception e) {
		Logging.logError(e);
		error(e.getMessage());
	}

	// pulled in dependencies .. not sure if that is good
	public void error(String format, Object... args) {
		if (listener != null) {
			listener.updateProgress(Status.error(format, args));
		}
	}

	public HashSet<Dependency> getDependencies(String fullServiceName) {
		ServiceData sd = getServiceDataFile();
		HashSet<Dependency> deps = new HashSet<Dependency>();
		// these are immediate dependencies - not Peer
		if (sd.containsServiceType(fullServiceName)) {
			ServiceType st = sd.getServiceType(fullServiceName);
			ArrayList<String> orgs = st.dependencies;
			if (orgs != null) {
				// Dependency[] deps = new Dependency[orgs.size()];
				for (int i = 0; i < orgs.size(); ++i) {
					Dependency d = sd.getDependency(orgs.get(i));
					if (d != null) {
						deps.add(d);
					} else {
						error("NO DEPENDENCY DEFINED FOR %s - %s", fullServiceName, orgs.get(i));
					}
				}
			}

			// get all the dependencies of my peers
			TreeMap<String, String> peers = st.peers;
			if (peers != null) {
				for (String peerType : peers.values()) {
					deps.addAll(getDependencies(peerType));
				}
			}
		} else {
			error(String.format("getDepenencies %s not found", fullServiceName));
		}
		return deps;
	}

	public String getErrors() {
		StringBuffer sb = new StringBuffer();
		for (String error : errors) {
			sb.append(error);
		}

		return sb.toString();
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
			Logging.logError(e);
		}

		return false;
	}

	public Platform getPlatform() {
		return platform;
	}

	public ServiceData getServiceData() {
		return localServiceData;
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
		} catch (IOException e) {
			info("local service data file not found - fetching remote");
			getRemoteRepo = true;
		}

		// FIXME FIXME FIXME - DO NOT AUTO GRAB THE LATEST !!!
		// failed getting local - try remote
		// return from remote - last attempt
		if (getRemoteRepo) {
			remoteServiceData = getServiceDataFromRepo();

			if (remoteServiceData != null) {
				localServiceData = remoteServiceData;
				return localServiceData;
			}
		}

		// all else fails - no local file - no remote - we will get
		// the serviceData packaged with the jar
		String sd = FileIO.resourceToString("framework/serviceData.json");
		log.info(sd);
		if (sd == null) {
			error("resource serviceData not found!");
			return null;
		}
		localServiceData = ServiceData.load(sd);

		return localServiceData;
	}

	public ServiceData getServiceDataFromRepo() {
		try {

			remoteServiceData = ServiceData.getRemote("https://raw.githubusercontent.com/MyRobotLab/repo/master/serviceData.json");
			if (remoteServiceData == null) {
				error("could not get remote service data");
				return null;
			}
			String repoFileName = String.format("%s%sserviceData.json", FileIO.getCfgDir(), File.separator);
			info("retrieved remote service data {}", repoFileName);
			remoteServiceData.save(repoFileName);

			return remoteServiceData;
		} catch (Exception e) {
			error(e.getMessage());
			Logging.logError(e);
		}
		return null;
	}

	public String getServiceDataURL() {
		return String.format("%s/serviceData.json", REPO_BASE_URL);
	}

	public ArrayList<ServiceType> getServiceTypes() {
		return localServiceData.getServiceTypes();
	}

	public String getVersion() {
		return platform.getVersion();
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
		// info(String.format("recieved [%s]", s));

		info("parsing");

		GitHubRelease[] releases = Encoder.fromJson(s, GitHubRelease[].class);
		if (releases == null) {
			error("Are you connected to intertoobs?");
			throw new IOException("Are you connected to intertoobs?");
		}
		info(String.format("found %d releases", releases.length));

		String[] r = new String[releases.length];
		for (int i = 0; i < releases.length; ++i) {
			r[i] = releases[i].tag_name;
		}

		/*
		 * Arrays.sort(r);
		 * 
		 * info("finished parsing and sorting");
		 * 
		 * if (r.length > 0) { String repoVersion = r[r.length - 1];
		 * info("returning release string %s", repoVersion); return repoVersion;
		 * } else { error("could not get latest version information"); throw new
		 * IOException("could not get latest version information"); }
		 */
		String latest = getLatestVersion(r);
		return latest;
	}

	public boolean hasErrors() {
		return (errors.size() > 0) ? true : false;
	}

	// pulled in dependencies .. not sure if that is good
	public void info(String format, Object... args) {
		if (listener != null) {
			listener.updateProgress(Status.info(format, args));
		}
	}

	public ArrayList<ResolveReport> install(String fullTypeName) throws ParseException, IOException {
		return install(fullTypeName, false);
	}

	public ArrayList<ResolveReport> install(String fullTypeName, boolean force) throws ParseException, IOException {

		if (!fullTypeName.contains(".")) {
			fullTypeName = String.format("org.myrobotlab.service.%s", fullTypeName);
		}

		ArrayList<ResolveReport> reports = new ArrayList<ResolveReport>();
		HashSet<Dependency> deps = getDependencies(fullTypeName);
		if (deps != null) {
			for (Dependency dep : deps) {
				if (!dep.isResolved() || force) {
					ResolveReport report = resolveArtifacts(dep.getOrg(), dep.getRevision(), true);
					reports.add(report);

					if (report.hasError()) {
						// TODO - invoke through "myRuntime"
						// INTERFACE - through message sink / message source
						// interface
						List<?> problems = report.getAllProblemMessages();
						for (int j = 0; j < problems.size(); ++j) {
							Object problem = problems.get(j);

							// error(problem.toString()); - already prints out
							// when retrieved
						}
					}

				}
			}

		} else {
			// FIXME - fill reports with HAPPY ENTRY :D
			log.info("{} is free of dependencies ", fullTypeName);
		}

		return reports;
	}

	/**
	 * searches through dependencies directly defined by the service and all
	 * Peers - recursively searches for their dependencies if any are not found
	 * - returns false
	 * 
	 * @param fullTypeName
	 * @return
	 */
	public boolean isServiceTypeInstalled(String fullTypeName) {
		boolean ret = true;

		if (localServiceData != null) {
			ServiceType st = localServiceData.getServiceType(fullTypeName);
			if (st == null) {
				error("unknown service %s", fullTypeName);
				return false;
			}

			// get all dependencies of the service
			HashSet<Dependency> deps = getDependencies(fullTypeName);
			// see if all dependnecies have been installed
			for (Dependency dependency : deps) {
				ret &= dependency.isInstalled();
			}

			return ret;
		}
		return true;
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
		info("%s %s.%s", (retrieve) ? "retrieve" : "resolve", org, revision);
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
					Logging.logError(e);
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
		List<?> err = report.getAllProblemMessages();

		if (err.size() > 0) {
			for (int i = 0; i < err.size(); ++i) {
				String errStr = err.get(i).toString();
				error(errStr);
				errors.add(errStr);
			}
		} else {
			info("%s %s.%s for %s", (retrieve) ? "retrieved" : "installed", org, revision, platform.getPlatformId());
		}
		// TODO - no error
		if (retrieve && err.size() == 0) {

			// TODO check on extension here - additional processing

			String retrievePattern = "libraries/[type]/[artifact].[ext]";// settings.substitute(line.getOptionValue("retrieve"));

			String ivyPattern = null;
			int ret = ivy.retrieve(md.getModuleRevisionId(), retrievePattern, new RetrieveOptions().setConfs(confs).setSync(false)// check
					.setUseOrigin(false).setDestIvyPattern(ivyPattern).setArtifactFilter(NO_FILTER).setMakeSymlinks(false).setMakeSymlinksInMass(false));

			log.info("retrieve returned {}", ret);

			Dependency dependency = localServiceData.getDependency(org);
			if (dependency == null) {
				error("successfully resolved dependency - but it is not defined in local serviceData.json");
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
				// FIXME - native move up one directory !!! - from denormalized
				// back to normalized Yay!
				// maybe look for PlatformId in path ?
				// ret > 0 && <-- retrieved -
				if ("zip".equalsIgnoreCase(artifact.getType())) {
					String filename = String.format("libraries/zip/%s.zip", artifact.getName());
					info("unzipping %s", filename);
					Zip.unzip(filename, "./");
					info("unzipped %s", filename);
				}
			}
		}

		return report;
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
				Logging.logError(e);
			}
		}

		return reports;
	}

	public ResolveReport retrieveArtifacts(String org, String revision) throws Exception {
		return resolveArtifacts(org, revision, true);
	}

	public void save() {
		localServiceData.save();
	}

}

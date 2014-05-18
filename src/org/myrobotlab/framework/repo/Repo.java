package org.myrobotlab.framework.repo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.HTTPRequest;
import org.slf4j.Logger;

// FIXME - remove all references of Runtime - you must messages to an interface !

public class Repo {
	
	public final String REPO_BASE_URL = "https://raw.githubusercontent.com/MyRobotLab/repo/master";

	public final static Logger log = LoggerFactory.getLogger(Repo.class);
	
	// FYI ! - non of these can be transient
	//  need this "local" repo from remote instances !!!
	// the one and only local to current running instance
	private static Repo localInstance = null;
	private ServiceData localServiceData = null;
	private ServiceData remoteServiceData = null;

	private Platform platform;

	
	static public Repo getLocalInstance(){
		
		if (localInstance == null){
			localInstance = new Repo();
			localInstance.platform = Platform.getLocalInstance();
			
			// load local file
			localInstance.localServiceData = localInstance.getServiceDataFile();
			
			// get info from ivy
			
		}
		
		return localInstance;
	}
	
	public ServiceData getServiceDataFile() {
		return getServiceDataFile(null);
	}

	public ServiceData getServiceDataFile(String filename) {
		try {
			
			// return from memory - this will be returned
			// if this Repo has been transported over the network
			if (localServiceData != null){
				return localServiceData;
			}
			
			// return from local file
			ServiceData sd = ServiceData.getLocalServiceData(filename);
			return sd;
		} catch (Exception e) {
			Logging.logException(e);
		}
		
		// failed getting local - try remote
		// return from remote
		return getRemoteServiceData();
	}
	/* update a specific ServiceType 
	 
	 	// FIXME - update(Updates updates)
	public void update(String fullTypeName) {
		remoteServiceData = ServiceData.getServiceData(String.format("%s/serviceData.xml", REPO_BASE_URL));
		serviceDataManager.resolve(fullTypeName);
	}
	
	 */

	public ServiceData getRemoteServiceData() {
		
		remoteServiceData = ServiceData.getServiceData("https://raw.githubusercontent.com/MyRobotLab/repo/master/serviceData.xml");
		if (remoteServiceData == null) {
			log.error("could not get remote service data");
			return null;
		}
		String repoFileName = String.format("%s%s", FileIO.getCfgDir(), File.separator);
		log.info("retrieved remote service data %s", repoFileName);
		remoteServiceData.save(repoFileName);

		return remoteServiceData;
	}

	public String getRepoVersion() {
		try {

			String listURL = "https://api.github.com/repos/MyRobotLab/myrobotlab/releases";

			log.info(String.format("getting list of dist %s", listURL));
			HTTPRequest http;
			http = new HTTPRequest(listURL);
			String s = http.getString();
			log.info(String.format("recieved [%s]", s));
			log.info("parsing");

			GitHubRelease[] releases = Encoder.gson.fromJson(s, GitHubRelease[].class);
			log.info("found %d releases", releases.length);

			String[] r = new String[releases.length];
			for (int i = 0; i < releases.length; ++i) {
				r[i] = releases[i].tag_name;
			}

			Arrays.sort(r);

			if (r.length > 0) {
				return r[r.length - 1];
			}

		} catch (Exception e) {
			Logging.logException(e);
		}

		log.error("could not get latest version information");

		return null;
	}
	
	public String getServiceDataURL() {
		return String.format("%s/serviceData.xml", REPO_BASE_URL);
	}

	public void getLatestVersionJar() {

		try {
			log.info("getBleedingEdgeMyRobotLabJar");
			String version = getRepoVersion();
			if (version == null) {
				log.error("could not get latest version from github");
				return;
			}
			String latestMRLJar = "https://github.com/MyRobotLab/myrobotlab/releases/download/" + version + "/myrobotlab.jar";
			log.info(String.format("getting latest build from %s", latestMRLJar));
			HTTPRequest zip = new HTTPRequest(latestMRLJar);
			byte[] jarfile = zip.getBinary();

			File updateDir = new File("update");
			updateDir.mkdir();
			File backupDir = new File("backup");
			backupDir.mkdir();

			FileOutputStream out = new FileOutputStream("update/myrobotlab.jar");
			try {
				out.write(jarfile);
				log.info("getBleedingEdgeMyRobotLabJar - done - since there is an update you will probably want to run scripts/update.(sh)(bat) to replace the jar");
			} catch (Exception e) {
				Logging.logException(e);
			} finally {
				out.close();
			}

		} catch (IOException e) {
			Logging.logException(e);
		}
	}
	
	/**
	 * this method attempts to get all update information from the repo it is
	 * not automatically applied - the update info is just retrieved and
	 * compared the resulting Updates data can then be used to apply against
	 * local repo to process updates
	 */
	public Updates checkForUpdates() {
		log.info("=== checking for updates begin ===");
		
		Updates updates = new Updates();
		
		updates.currentVersion = getCurrentVersion();
		updates.repoVersion = getRepoVersion();
		
		log.info(String.format("bleedingEdgeVersion - %s", updates.repoVersion));

		remoteServiceData = ServiceData.getServiceData(getServiceDataURL());

		if (remoteServiceData == null) {
			log.error("could not get repo data %s/serviceData.xml", REPO_BASE_URL);
			//return null;// ???
		}

		// addListener ready for updates
		// runtime.invoke("proposedUpdates", remoteServiceData);
		log.info("=== checking for updates end ===");
		return updates;

	}
	
	/* from auto update
	 
	 
			String newVersion = Repo.getBleedingEdgeVersionString();
			String currentVersion = FileIO.resourceToString("version.txt");
			log.info(String.format("comparing new version %s with current version %s", newVersion, currentVersion));
			if (newVersion == null) {
				runtime.info("newVersion == null - nothing available");
			} else if (currentVersion.compareTo(newVersion) >= 0) {
				log.info("no updates");
				runtime.info("no updates available");
			} else {
				runtime.info(String.format("updating with %s", newVersion));
				// Custom button text
				// FIXM re-implement but only start if you have a task
				// runtime.timer.schedule(new AutoUpdate(),
				// autoUpdateCheckIntervalSeconds * 1000);
				Runtime.getBleedingEdgeMyRobotLabJar();
				Runtime.restart("moveUpdate");

			}

	 */


	public String getCurrentVersion() {
		return  platform.getMRLVersion();
	}

	/**
	 * FIXME - deprecate - require calling code to implement loop - support only
	 * the single update(fullTypeName) - that way calling code can handle
	 * detailed info such as reporting to gui/console which components are being
	 * updated and which have errors in the update process. Will need a list of
	 * all or filtered ArrayList<fullTypeName>
	 * 
	 * update - force system to check for all dependencies of all possible
	 * Services - Ivy will attempt to check & fufill dependencies by downloading
	 * jars from the repo
	 */
	/*
	public void updateAll() {

		boolean getNewRepoData = true;

		// TODO - have it return list of data objects "errors" so
		// events can be generated
		serviceDataManager.clearErrors();

		// FIXME - not needed - set defaults [update all = true]
		if (getNewRepoData) {
			serviceDataManager.getRepoFile("serviceData.xml");
		}
		if (!serviceDataManager.hasErrors()) {
			serviceDataManager.update();
			Runtime.getBleedingEdgeMyRobotLabJar();
			Runtime.restart("moveUpdate");
		}

		List<String> errors = serviceDataManager.getErrors();
		for (int i = 0; i < errors.size(); ++i) {
			error(errors.get(i));
		}
	}

	static public void updateAndRestart() {
		try {
			Runtime.getBleedingEdgeMyRobotLabJar();
			restart(new Move("update/myrobotlab.jar", "libraries/jar/myrobotlab.jar"));
		} catch (Exception e) {
			Logging.logException(e);
		}
	}
	*/
	
	public boolean isServiceTypeInstalled(String fullTypeName) {
		return false;
	}

	public Platform getPlatform() {
		return platform;
	}
	
	public static void main(String[] args){
		try {
			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);

			Repo repo = new Repo();
			Updates updates = repo.checkForUpdates();
			log.info(String.format("updates %s", updates));
			if (updates.hasUpdate()){
				repo.getLatestVersionJar();
			}
			
		} catch(Exception e){
			Logging.logException(e);
		}
	}

}

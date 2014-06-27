package org.myrobotlab.framework.repo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.slf4j.Logger;

@Root
public class ServiceData implements Serializable {

	private static final long serialVersionUID = 1L;
	transient public final static Logger log = LoggerFactory.getLogger(Service.class);

	transient static private Serializer serializer = new Persister();

	@ElementMap(key = "name", entry = "serviceType", inline = false, required = false)
	private transient TreeMap<String, ServiceType> serviceTypesNameIndex = new TreeMap<String, ServiceType>();

	@ElementMap(key = "name", entry = "categories", inline = true, required = false)
	private transient TreeMap<String, Category> categoriesNameIndex = new TreeMap<String, Category>();

	@ElementMap(key = "name", name = "thirdPartyLibs", entry = "lib", inline = false, required = false)
	private transient TreeMap<String, Dependency> dependenciesOrgIndex = new TreeMap<String, Dependency>();

	public ServiceData() {
	}
	

	public void add(ServiceType serviceType) {
		serviceTypesNameIndex.put(serviceType.name, serviceType);
		for (int i = 0; i < serviceType.dependencyList.size(); ++i) {
			String org = serviceType.dependencyList.get(i);
			if (!containsDependency(org)) {
				log.error(String.format("can %s not find %s in dependencies", org));
			}
		}
	}

	boolean containsDependency(String org) {
		return dependenciesOrgIndex.containsKey(org);
	}

	public Dependency getDependency(String org) {
		if (dependenciesOrgIndex.containsKey(org)) {
			return dependenciesOrgIndex.get(org);
		}

		return null;
	}

	// FIXME - change to addDependency
	public void addThirdPartyLib(String org, String revision) {
		Dependency dep = new Dependency(org, revision);
		dependenciesOrgIndex.put(org, dep);
	}

	public void addServiceType(String className) {
		addServiceType(className, null, null);
	}

	public void addServiceType(String className, String[] dependencies) {
		addServiceType(className, null, dependencies);

	}

	public boolean containsServiceType(String fullServiceName) {
		return serviceTypesNameIndex.containsKey(fullServiceName);
	}

	public ServiceType getServiceType(String fullServiceName) {
		if (serviceTypesNameIndex.containsKey(fullServiceName)) {
			return serviceTypesNameIndex.get(fullServiceName);
		}

		return null;
	}

	public boolean hasUnfulfilledDependencies(String fullServiceName) {

		// no serviceInfo
		if (!serviceTypesNameIndex.containsKey(fullServiceName)) {
			log.error(String.format("%s not found", fullServiceName));
			return false;
		}

		ServiceType d = serviceTypesNameIndex.get(fullServiceName);
		if (d.dependencyList == null || d.dependencyList.size() == 0) {
			log.debug(String.format("no dependencies needed for %s", fullServiceName));
			return false;
		}

		for (int i = 0; i < d.dependencyList.size(); ++i) {
			String org = d.dependencyList.get(i);
			if (!dependenciesOrgIndex.containsKey(org)) {
				log.error(String.format("%s has dependency of %s, but it is does not have a defined version", fullServiceName, org));
				return true;
			} else {
				Dependency dep = dependenciesOrgIndex.get(org);
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
		for (Map.Entry<String, Dependency> o : dependenciesOrgIndex.entrySet()) {
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
		for (Map.Entry<String, ServiceType> o : serviceTypesNameIndex.entrySet()) {
			ServiceType st = o.getValue();
			if (st.dependencyList != null) {
				for (int j = 0; j < st.dependencyList.size(); ++j) {
					String d = st.dependencyList.get(j);
					if (org.equals(d)) {
						return false;
					}
				}
			}
		}

		return true;
	}

	public boolean isValid(String org) {
		return dependenciesOrgIndex.containsKey(org);
	}

	public String[] getInvalidDependencies() {
		HashSet<String> unique = new HashSet<String>();
		for (Map.Entry<String, ServiceType> o : serviceTypesNameIndex.entrySet()) {
			ServiceType st = o.getValue();
			if (st.dependencyList != null) {
				for (int j = 0; j < st.dependencyList.size(); ++j) {
					String org = st.dependencyList.get(j);
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
		for (Map.Entry<String, ServiceType> o : serviceTypesNameIndex.entrySet()) {
			ServiceType st = o.getValue();
			if (st.dependencyList != null) {
				for (int j = 0; j < st.dependencyList.size(); ++j) {
					unique.add(st.dependencyList.get(j));
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
		if (serviceTypesNameIndex.containsKey(className)) {
			log.error(String.format("duplicate names %s - not adding service type", className));
			return;
		}
		ServiceType st = new ServiceType(className);
		st.description = description;
		serviceTypesNameIndex.put(st.getName(), st);
		if (dependencies != null) {
			for (int i = 0; i < dependencies.length; ++i) {
				st.addDependency(dependencies[i]);
			}
		}

	}

	static public ServiceData getLocal() throws FileNotFoundException {
		return getLocal(null);
	}
	static public ServiceData getLocal(String filename) throws FileNotFoundException {

		if (filename == null) {
			filename = String.format("%s%sserviceData.xml", FileIO.getCfgDir(), File.separator);
		}
		String data = FileIO.fileToString(filename);
		return load(data);
	}

	public static ServiceData getRemote(String url) {
		try {
			log.info("getting {}", url);
			String data = new String(FileIO.getURL(new URL(url)));
			return load(data);
		} catch(Exception e){
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

		for (Map.Entry<String, Category> o : categoriesNameIndex.entrySet()) {
			Category category = o.getValue();

			for (int j = 0; j < category.serviceTypes.size(); ++j) {
				String serviceType = category.serviceTypes.get(j);
				if (!serviceTypesNameIndex.containsKey(serviceType)) {
					log.warn(String.format("category %s contains reference to service type %s which does not exist", category.name, serviceType));
				}
			}
		}

		HashSet<String> categorizedServiceTypes = new HashSet<String>();

		for (Map.Entry<String, Category> o : categoriesNameIndex.entrySet()) {
			Category category = o.getValue();
			if (category.serviceTypes.size() == 0) {
				log.warn(String.format("empty category %s", category.name));
			}
			for (int j = 0; j < category.serviceTypes.size(); ++j) {
				categorizedServiceTypes.add(category.serviceTypes.get(j));
			}
		}

		for (Map.Entry<String, ServiceType> o : serviceTypesNameIndex.entrySet()) {
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
			if (data == null){
				log.warn("can not load serviceData - data is null");
			}
			log.info("loading serviceData");
			ServiceDataLoader sdl = serializer.read(ServiceDataLoader.class, data);
			ServiceData sd = new ServiceData();

			for (int i = 0; i < sdl.serviceTypes.size(); ++i) {
				ServiceType st = sdl.serviceTypes.get(i);
				sd.serviceTypesNameIndex.put(st.getName(), st);
			}

			for (int i = 0; i < sdl.categories.size(); ++i) {
				Category st = sdl.categories.get(i);
				sd.categoriesNameIndex.put(st.name, st);
			}

			for (int i = 0; i < sdl.dependencies.size(); ++i) {
				Dependency st = sdl.dependencies.get(i);
				//log.info(st.toString());
				sd.dependenciesOrgIndex.put(st.getOrg(), st);
			}

			sd.isValid();

			return sd;
		} catch (FileNotFoundException e) {
			log.info(e.getMessage());
		} catch (Exception e) {
			Logging.logException(e);
		}
		return null;
	}

	public String[] getServiceTypeNames(String filter) {
		
		if (filter == null || filter.length() == 0 || filter.equals("all")){
			return serviceTypesNameIndex.keySet().toArray(new String[0]);
		}
		
		if (!categoriesNameIndex.containsKey(filter)){
			return new String[]{};
		}
		
		Category cat = categoriesNameIndex.get(filter);
		return cat.serviceTypes.toArray(new String[cat.serviceTypes.size()]);

	}

	public ArrayList<ServiceType> getServiceTypes() {
		ArrayList<ServiceType> ret = new ArrayList<ServiceType>();
		for (Map.Entry<String, ServiceType> o : serviceTypesNameIndex.entrySet()) {
			ret.add(o.getValue());
		}
		return ret;
	}

	public void addCategory(String name, String[] serviceTypes) {
		addCategory(name, null, serviceTypes);
	}

	public void addCategory(String name, String description, String[] serviceTypes) {
		Category category = null;
		if (categoriesNameIndex.containsKey(name)) {
			category = categoriesNameIndex.get(name);
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

		categoriesNameIndex.put(category.name, category);
	}

	public ServiceData loadLocal() throws FileNotFoundException {
		return getLocal(String.format("%s%sserviceData.xml", FileIO.getCfgDir(), File.separator));
	}

	public boolean save() {
		return save(String.format("%s%sserviceData.xml", FileIO.getCfgDir(), File.separator));
	}

	public boolean save(String filename) {
		try {

			isValid();

			Serializer serializer = new Persister();

			ServiceDataLoader sdl = new ServiceDataLoader();

			for (Map.Entry<String, ServiceType> o : serviceTypesNameIndex.entrySet()) {
				ServiceType st = o.getValue();
				sdl.serviceTypes.add(st);
			}

			for (Map.Entry<String, Category> o : categoriesNameIndex.entrySet()) {
				Category st = o.getValue();
				sdl.categories.add(st);
			}

			for (Map.Entry<String, Dependency> o : dependenciesOrgIndex.entrySet()) {
				Dependency st = o.getValue();
				sdl.dependencies.add(st);
			}

			FileOutputStream fos = new FileOutputStream(filename);
			serializer.write(sdl, fos);
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

		String[] cat = new String[categoriesNameIndex.size()];

		int i = 0;
		for (Map.Entry<String, Category> o : categoriesNameIndex.entrySet()) {
			cat[i] = o.getKey();
			++i;
		}
		return cat;
	}

	public static void main(String[] args) {
		try {

			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel("INFO");

			// ServiceData sd =
			// ServiceData.getLocalServiceData("serviceData.test.xml");
			// ServiceData remote =
			// ServiceData.getServiceData("serviceData.xml");

			// working level - 0 non implementation 1 implemented dev not tested
			// 2 implemented basic unit testing 3 implemented
			/*
			ServiceData serviceData = new ServiceData();

			serviceData.addServiceType("org.myrobotlab.service.ACEduinoMotorShield", "ACEduino Motor Shield for Arduino", new String[] { "gnu.io.rxtx", "cc.arduino",
					"ph.com.alexan.aceduino" });
			serviceData.addServiceType("org.myrobotlab.service.Adafruit16CServoDriver", "AdaFruit 16-channel 12-bit PWM/servo driver - I2C interface", new String[] {
					"gnu.io.rxtx", "cc.arduino", "com.adafruit.servodriver" });
			serviceData.addServiceType("org.myrobotlab.service.AdafruitMotorShield", "Adafruit Motor Shield for Arduino", new String[] { "gnu.io.rxtx", "cc.arduino",
					"com.adafruit.motorshield" });
			serviceData.addServiceType("org.myrobotlab.service.Arduino", "The Arduino service is used to communicate and control the very popular Arduino micro-controller",
					new String[] { "gnu.io.rxtx", "cc.arduino" });
			serviceData.addServiceType("org.myrobotlab.service.AudioCapture", "captures audio for playback");
			serviceData.addServiceType("org.myrobotlab.service.AudioFile", "plays audio files", new String[] { "javazoom.jl.player" });
			serviceData.addServiceType("org.myrobotlab.service.BeagleBoardBlack", "service beagle board black to interface with GPIO");
			serviceData.addServiceType("org.myrobotlab.service.AWTRobot", "allows screen capture, and control of mouse and keyboard");
			serviceData.addServiceType("org.myrobotlab.service.ChessGame", "chess game engine", new String[] { "org.op.chess" });
			serviceData.addServiceType("org.myrobotlab.service.CleverBot", "interface to CleverBot JaberWacky and PandoraBots", new String[] { "com.googlecode.chatterbot" });
			serviceData.addServiceType("org.myrobotlab.service.Clock", "general clock and event producer");
			serviceData.addServiceType("org.myrobotlab.service.Cortex", "visual processing and memory", new String[] { "com.googlecode.javacv", "net.sourceforge.opencv" });
			serviceData.addServiceType("org.myrobotlab.service.Cron", "general clock and event producer - cron syntax", new String[] { "it.sauronsoftware.cron4j" });
			// deprecate?
			serviceData.addServiceType("org.myrobotlab.service.GestureRecognition", "uses OpenNI NITE for tracking gestures", new String[] { "com.googlecode.simpleopenni" });
			serviceData.addServiceType("org.myrobotlab.service.GUIService", "a swing graphical user interface for MyRobotLab");
			serviceData.addServiceType("org.myrobotlab.service.GoogleSTT", "uses OpenNI NITE for tracking gestures", new String[] { "javaFlacEncoder.FLAC_FileEncoder",
					"org.tritonus.share.sampled.floatsamplebuffer" });
			serviceData.addServiceType("org.myrobotlab.service.Houston", "control service for Houston robot project", new String[] { "org.apache.commons.httpclient",
					"javax.speech.recognition", "com.googlecode.javacv", "net.sourceforge.opencv", "edu.cmu.sphinx", "gnu.io.rxtx", "cc.arduino", "com.googlecode.simpleopenni",
					"com.sun.java3d" });
			serviceData.addServiceType("org.myrobotlab.service.HTTPClient", "service to retrieve internet content", new String[] { "org.apache.commons.httpclient" });
			serviceData.addServiceType("org.myrobotlab.service.InMoov", "service to control the InMoov robot", new String[] { "com.sun.speech.freetts",
					"org.apache.commons.httpclient", "javax.speech.recognition", "com.googlecode.javacv", "net.sourceforge.opencv", "edu.cmu.sphinx", "gnu.io.rxtx", "cc.arduino",
					"com.googlecode.simpleopenni", "javazoom.jl.player", "org.jivesoftware.smack" });
			serviceData.addServiceType("org.myrobotlab.service.InMoovArm", "service to control the InMoov robot", new String[] { "com.sun.speech.freetts",
					"org.apache.commons.httpclient", "javax.speech.recognition", "com.googlecode.javacv", "net.sourceforge.opencv", "edu.cmu.sphinx", "gnu.io.rxtx", "cc.arduino",
					"com.googlecode.simpleopenni", "javazoom.jl.player", "org.jivesoftware.smack" });
			serviceData.addServiceType("org.myrobotlab.service.InMoovHand", "service to control the InMoov robot", new String[] { "com.sun.speech.freetts",
					"org.apache.commons.httpclient", "javax.speech.recognition", "com.googlecode.javacv", "net.sourceforge.opencv", "edu.cmu.sphinx", "gnu.io.rxtx", "cc.arduino",
					"com.googlecode.simpleopenni", "javazoom.jl.player", "org.jivesoftware.smack" });
			serviceData.addServiceType("org.myrobotlab.service.InMoovHead", "service to control the InMoov robot", new String[] { "com.sun.speech.freetts",
					"org.apache.commons.httpclient", "javax.speech.recognition", "com.googlecode.javacv", "net.sourceforge.opencv", "edu.cmu.sphinx", "gnu.io.rxtx", "cc.arduino",
					"com.googlecode.simpleopenni", "javazoom.jl.player", "org.jivesoftware.smack" });
			serviceData.addServiceType("org.myrobotlab.service.InverseKinematics", "Service to calculate InverseKinematics of a 2 joints robot arm, on a rotating base");
			serviceData.addServiceType("org.myrobotlab.service.IPCamera", "service to control and retrieve a video feed from a ip camera. Foscam cameras currently supported",
					new String[] { "com.googlecode.javacv" });
			serviceData.addServiceType("org.myrobotlab.service.Java", "Java IDE", new String[] { "org.drjava.java", "com.strobel.decompiler" });
			serviceData.addServiceType("org.myrobotlab.service.JFugue", "music and tone generation service", new String[] { "org.jfugue.music" });
			serviceData.addServiceType("org.myrobotlab.service.Joystick", "general joystick and gamepad service", new String[] { "net.java.games.jinput" });
			// FIXME - no current api in the repo
			// serviceData.addServiceType("org.myrobotlab.service.LeapMotion","This service will use the hand tracking capabilities of the Leap Motion device.",new
			// String[] {"net.java.games.jinput"});
			serviceData.addServiceType("org.myrobotlab.service.Python", "scripting engine and IDE. All services are controllable through Python.",
					new String[] { "org.python.core" });

			serviceData.addServiceType("org.myrobotlab.service.Keyboard", "a service which allows capture and transmission of keyboard events (keystrokes)");
			serviceData.addServiceType("org.myrobotlab.service.Log", "a service capable of logging framework messages");
			serviceData.addServiceType("org.myrobotlab.service.Motor", "A general purpose Motor service");
			serviceData.addServiceType("org.myrobotlab.service.MouthControl", "allows control of a mouth based on text said");
			serviceData.addServiceType("org.myrobotlab.service.OpenCV", "The OpenCV Service is a library of vision functions", new String[] { "com.googlecode.javacv",
					"net.sourceforge.opencv" });
			serviceData.addServiceType("org.myrobotlab.service.OpenNI", "service to provide OpenNI methods such as depth cloud and skeleton tracking",
					new String[] { "com.googlecode.simpleopenni" });
			serviceData.addServiceType("org.myrobotlab.service.PickToLight", "pick to light controller", new String[] { "org.apache.commons.httpclient", "com.pi4j.pi4j" });

			serviceData.addServiceType("org.myrobotlab.service.Plantoid", "Plantoid robotics", new String[] { "com.sun.speech.freetts", "org.apache.commons.httpclient",
					"javax.speech.recognition", "com.googlecode.javacv", "net.sourceforge.opencv", "edu.cmu.sphinx", "gnu.io.rxtx", "cc.arduino", "com.googlecode.simpleopenni",
					"javazoom.jl.player" });
			serviceData.addServiceType("org.myrobotlab.service.PointCloud", "Uses OpenNI to generated a 3D pointcloud, will be useful in SLAM", new String[] {
					"com.googlecode.simpleopenni", "com.sun.java3d" });
			serviceData.addServiceType("org.myrobotlab.service.ParallelPort", "general parallel port control service", new String[] { "gnu.io.rxtx" });
			serviceData.addServiceType("org.myrobotlab.service.PID", "service used to control other systems with PID");
			serviceData.addServiceType("org.myrobotlab.service.Propeller", "service for the Propeller micro-controller", new String[] { "gnu.io.rxtx" });

			serviceData.addServiceType("org.myrobotlab.service.RemoteAdapter", "allows connectivity to multiple instances of MyRobotLab. Allows peer to peer service sharing");
			serviceData.addServiceType("org.myrobotlab.service.RasPi", "a service to allow access to the Raspberry Pi's GPIO and I2C offered in Pi4J",
					new String[] { "com.pi4j.pi4j" });
			serviceData.addServiceType("org.myrobotlab.service.Roomba", "allows MyRobotLab to connect and control a Roomba", new String[] { "gnu.io.rxtx" });

			// serviceData.addServiceType("org.myrobotlab.service.RemoteAdapter",
			// "allows peer to peer connectivity to multiple instances of MyRobotLab");

			serviceData.addServiceType("org.myrobotlab.service.Security", "provides security for MRL");
			serviceData.addServiceType("org.myrobotlab.service.SensorMonitor", "allows the graphical monitoring of sensor data, similar to the Arduino Oscope");
			serviceData.addServiceType("org.myrobotlab.service.Serial", "Serial service: send and recieve serial data into and out of MRL", new String[] { "gnu.io.rxtx" });
			serviceData.addServiceType("org.myrobotlab.service.Servo", "allows speed and position control of a Servo");
			serviceData.addServiceType("org.myrobotlab.service.SLAMBad", "3D Simulator based on Simbad", new String[] { "javax.vecmath", "com.sun.java3d" });
			serviceData.addServiceType("org.myrobotlab.service.Speech", "FreeTTS and Google text to speech service", new String[] { "com.sun.speech.freetts",
					"org.apache.commons.httpclient", "javazoom.jl.player" });
			serviceData.addServiceType("org.myrobotlab.service.Sphinx", "Sphinx speech recognition", new String[] { "javax.speech.recognition", "edu.cmu.sphinx" });

			serviceData.addServiceType("org.myrobotlab.service.TesseractOCR", "optical character recognition (OCR) for robots !", new String[] { "net.sourceforge.tess4j",
					"com.sun.jna" });
			// FIXME - probably has httpclient as dependency
			serviceData.addServiceType("org.myrobotlab.service.ThingSpeak", "interface to ThingSpeak for data logging and charting");
			serviceData.addServiceType("org.myrobotlab.service.TopCodes", "Tangible Object Placement Codes", new String[] { "edu.northwestern.topcodes" });
			// TODO - make Peer info from Peer lists --> Peer's contain
			// dependencies
			serviceData.addServiceType("org.myrobotlab.service.Tracking", "vision tracking service", new String[] { "edu.northwestern.topcodes" });
			serviceData.addServiceType("org.myrobotlab.service.Twitter", "Twitter service - it allows to publish and read tweets", new String[] { "org.twitter4j.twitter" });
			serviceData.addServiceType("org.myrobotlab.service.VideoStreamer", "stream video from a video source");
			serviceData.addServiceType("org.myrobotlab.service.WebGUI", "The new GUI for MRL", new String[] { "com.google.gson", "org.java_websocket.websocket" });
			serviceData.addServiceType("org.myrobotlab.service.Wii", "service for the Wii controller", new String[] { "wiiuse.wiimote" });
			serviceData.addServiceType("org.myrobotlab.service.WolframAlpha", "service for the WolframAlpha - ask questions get answers", new String[] {
					"org.apache.commons.httpclient", "com.wolfram.alpha" });
			serviceData.addServiceType("org.myrobotlab.service.XMPP", "client xmpp service - will allow a duplex communication without the need of port-forwarding",
					new String[] { "org.jivesoftware.smack" });

			serviceData.addCategory("audio", new String[] { "org.myrobotlab.service.AudioCapture", "org.myrobotlab.service.AudioFile", "org.myrobotlab.service.JFugue" });
			serviceData.addCategory("microcontroller", new String[] { "org.myrobotlab.service.Arduino", "org.myrobotlab.service.Propeller" });
			serviceData.addCategory("programming", new String[] { "org.myrobotlab.service.Python", "org.myrobotlab.service.Java", "org.myrobotlab.service.GUIService" });
			serviceData.addCategory("robots", new String[] { "org.myrobotlab.service.Houston", "org.myrobotlab.service.InMoov", "org.myrobotlab.service.InMoovArm",
					"org.myrobotlab.service.InMoovHand", "org.myrobotlab.service.InMoov", "org.myrobotlab.service.Plantoid", "org.myrobotlab.service.Roomba" });
			serviceData.addCategory("network", new String[] { "org.myrobotlab.service.RemoteAdapter", "org.myrobotlab.service.InMoov" });
			serviceData.addCategory("actuators", new String[] { "org.myrobotlab.service.Motor", "org.myrobotlab.service.Servo" });
			serviceData.addCategory("vision", new String[] { "org.myrobotlab.service.OpenCV", "org.myrobotlab.service.OpenNI", "org.myrobotlab.service.IPCamera",
					"org.myrobotlab.service.Tracking", "org.myrobotlab.service.TopCodes" });
			serviceData.addCategory("simulators", new String[] { "org.myrobotlab.service.SLAMBad" });
			serviceData.addCategory("display", new String[] { "org.myrobotlab.service.GUIService", "org.myrobotlab.service.WebGUI", "org.myrobotlab.service.VideoStreamer",
					"org.myrobotlab.service.ThingSpeak" });
			serviceData.addCategory("speech synthesis", new String[] { "org.myrobotlab.service.Speech" });
			serviceData.addCategory("speech recognition", new String[] { "org.myrobotlab.service.Sphinx" });
			serviceData.addCategory("intelligence", new String[] { "org.myrobotlab.service.CleverBot" });

			// TODO -
			// http://stackoverflow.com/questions/13685042/how-to-set-a-unix-dynamic-library-path-ld-library-path-in-java

			serviceData.addThirdPartyLib("cc.arduino", "1.0");
			serviceData.addThirdPartyLib("com.adafruit.motorshield", "1.0");
			serviceData.addThirdPartyLib("com.adafruit.servodriver", "1.0");
			serviceData.addThirdPartyLib("be.hogent.tarsos", "1.7");
			serviceData.addThirdPartyLib("com.google.gson", "2.2.4");
			serviceData.addThirdPartyLib("com.googlecode.chatterbot", "1.2.1");
			serviceData.addThirdPartyLib("com.googlecode.javacv", "0.6");
			serviceData.addThirdPartyLib("com.pi4j.pi4j", "0.0.5");
			serviceData.addThirdPartyLib("com.strobel.decompiler", "0.3.2");
			serviceData.addThirdPartyLib("com.sun.java3d", "1.5.1");
			serviceData.addThirdPartyLib("com.sun.jna", "3.2.2");
			serviceData.addThirdPartyLib("com.sun.speech.freetts", "1.2");
			serviceData.addThirdPartyLib("com.wolfram.alpha", "1.1");
			serviceData.addThirdPartyLib("edu.cmu.sphinx", "4-1.0beta6");
			serviceData.addThirdPartyLib("edu.northwestern.topcodes", "1.0");
			serviceData.addThirdPartyLib("gnu.io.rxtx", "2.1-7r2");
			serviceData.addThirdPartyLib("it.sauronsoftware.cron4j", "2.2.5");
			serviceData.addThirdPartyLib("javaFlacEncoder.FLAC_FileEncoder", "0.1");
			serviceData.addThirdPartyLib("javax.speech.recognition", "1.0");
			serviceData.addThirdPartyLib("javax.vecmath", "1.5.1");
			serviceData.addThirdPartyLib("javazoom.jl.player", "1.0.1");
			serviceData.addThirdPartyLib("net.java.games.jinput", "20120914");
			serviceData.addThirdPartyLib("net.sourceforge.opencv", "2.4.6");
			serviceData.addThirdPartyLib("net.sourceforge.tess4j", "1.1");
			serviceData.addThirdPartyLib("org.apache.commons.httpclient", "4.2.5");
			serviceData.addThirdPartyLib("org.drjava.java", "20120818");
			serviceData.addThirdPartyLib("org.java_websocket.websocket", "1.2");
			serviceData.addThirdPartyLib("org.jfugue.music", "4.0.3");
			serviceData.addThirdPartyLib("org.jivesoftware.smack", "3.3.0");
			serviceData.addThirdPartyLib("org.op.chess", "1.0.0");
			serviceData.addThirdPartyLib("com.googlecode.simpleopenni", "1.96");
			serviceData.addThirdPartyLib("org.python.core", "2.5.2");
			serviceData.addThirdPartyLib("org.tritonus.share.sampled.floatsamplebuffer", "0.3.6");
			serviceData.addThirdPartyLib("org.twitter4j.twitter", "3.0.5");
			serviceData.addThirdPartyLib("ph.com.alexan.aceduino", "1.0");
			serviceData.addThirdPartyLib("wiiuse.wiimote", "0.12b");

			serviceData.save();
			*/
			
			ServiceData sd = ServiceData.getLocal();//.loadLocal();
			sd.save();

			/*
			 * Serializer serializer = new Persister();
			 * 
			 * File cfg = new File("serviceData.test.xml");
			 * serializer.write(serviceData, cfg);
			 */

			log.info("here");

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}

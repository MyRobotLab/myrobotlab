package org.myrobotlab.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FilenameUtils;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.inmoov.Utils;
import org.myrobotlab.inmoov.Vision;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis.Voice;
import org.myrobotlab.service.data.JoystickData;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.service.interfaces.JoystickListener;
import org.myrobotlab.service.interfaces.LocaleProvider;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.Simulator;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

public class InMoov2 extends Service implements TextListener, TextPublisher, JoystickListener, LocaleProvider {

	public final static Logger log = LoggerFactory.getLogger(InMoov2.class);

	public static LinkedHashMap<String, String> lpVars = new LinkedHashMap<String, String>();

	// FIXME - why
	static boolean RobotCanMoveRandom = true;
	private static final long serialVersionUID = 1L;

	static String speechRecognizer = "WebkitSpeechRecognition";

	/**
	 * This static method returns all the details of the class without it having to
	 * be constructed. It has description, categories, dependencies, and peer
	 * definitions.
	 * 
	 * @return ServiceType - returns all the data
	 * 
	 */
	static public ServiceType getMetaData() {

		ServiceType meta = new ServiceType(InMoov2.class);
		meta.addDescription("InMoov2 Service");
		meta.addCategory("robot");

		meta.sharePeer("mouthControl.mouth", "mouth", "MarySpeech", "shared Speech");

                meta.addPeer("eye", "OpenCV", "eye");
                meta.addPeer("servomixer", "ServoMixer", "for making gestures");
		meta.addPeer("ultraSonicRight", "UltrasonicSensor", "measure distance");
		meta.addPeer("ultraSonicLeft", "UltrasonicSensor", "measure distance");
		meta.addPeer("pir", "Pir", "infrared sensor");

		// the two legacy controllers .. :(
		meta.addPeer("left", "Arduino", "legacy controller");
		meta.addPeer("right", "Arduino", "legacy controller");
		meta.addPeer("controller3", "Arduino", "legacy controller");
		meta.addPeer("controller4", "Arduino", "legacy controller");

		meta.addPeer("htmlFilter", "HtmlFilter", "filter speaking html");

		meta.addPeer("brain", "ProgramAB", "brain");
		meta.addPeer("simulator", "JMonkeyEngine", "simulator");

		meta.addPeer("head", "InMoov2Head", "head");
		meta.addPeer("torso", "InMoov2Torso", "torso");
		// meta.addPeer("eyelids", "InMoovEyelids", "eyelids");
		meta.addPeer("leftArm", "InMoov2Arm", "left arm");
		meta.addPeer("leftHand", "InMoov2Hand", "left hand");
		meta.addPeer("rightArm", "InMoov2Arm", "right arm");
		meta.addPeer("rightHand", "InMoov2Hand", "right hand");
		meta.addPeer("mouthControl", "MouthControl", "MouthControl");
		// meta.addPeer("imageDisplay", "ImageDisplay", "image display service");
		meta.addPeer("mouth", "MarySpeech", "InMoov speech service");
		meta.addPeer("ear", speechRecognizer, "InMoov webkit speech recognition service");

		meta.addPeer("headTracking", "Tracking", "Head tracking system");

		meta.sharePeer("headTracking.opencv", "eye", "OpenCV", "shared head OpenCV");
		// meta.sharePeer("headTracking.controller", "left", "Arduino", "shared head
		// Arduino"); NO !!!!
		meta.sharePeer("headTracking.x", "head.rothead", "Servo", "shared servo");
		meta.sharePeer("headTracking.y", "head.neck", "Servo", "shared servo");

		// Global - undecorated by self name
		meta.addRootPeer("python", "Python", "shared Python service");

		// latest - not ready until repo is ready
		meta.addDependency("fr.inmoov", "inmoov2", null, "zip");

		return meta;
	}

	/**
	 * This method will load a python file into the python interpreter.
	 */
	public static boolean loadFile(String file) {
		File f = new File(file);
		Python p = (Python) Runtime.getService("python");
		log.info("Loading  Python file {}", f.getAbsolutePath());
		if (p == null) {
			log.error("Python instance not found");
			return false;
		}
		String script = null;
		try {
			script = FileIO.toString(f.getAbsolutePath());
		} catch (IOException e) {
			log.error("IO Error loading file : ", e);
			return false;
		}
		// evaluate the scripts in a blocking way.
		boolean result = p.exec(script, true);
		if (!result) {
			log.error("Error while loading file {}", f.getAbsolutePath());
			return false;
		} else {
			log.debug("Successfully loaded {}", f.getAbsolutePath());
		}
		return true;
	}

	public static void main(String[] args) {
		try {

			LoggingFactory.init(Level.INFO);
			Platform.setVirtual(true);
			// Runtime.main(new String[] { "--install", "InMoov2" });
			// Runtime.main(new String[] { "--interactive", "--id", "inmoov",
			// "--install-dependency","fr.inmoov", "inmoov2", "latest", "zip"});
			Runtime.main(new String[] { 
					"--resource-override", 
					"InMoov2=/lhome/grperry/github/mrl.develop/myrobotlab/src/main/resources/resource/InMoov2/resource/InMoov2",
					"WebGui=/lhome/grperry/github/mrl.develop/myrobotlab/src/main/resources/resource/InMoov2/resource/WebGui",
					"ProgramAB=/lhome/grperry/github/mrl.develop/myrobotlab/src/main/resources/resource/InMoov2/resource/ProgramAB",
					"--interactive", "--id", "inmoov" });

			String[] langs = java.util.Locale.getISOLanguages();

			java.util.Locale[] locales = java.util.Locale.getAvailableLocales();
			log.info("{}", locales.length);
			for (java.util.Locale l : locales) {
				log.info("------------------------");
				log.info(CodecUtils.toJson(l));
				log.info(l.getDisplayLanguage());
				log.info(l.getLanguage());
				log.info(l.getCountry());
				log.info(l.getDisplayCountry());

				log.info(CodecUtils.toJson(new Locale(l)));

				if (l.getLanguage().equals("en")) {
					log.info("here");
				}
			}

			InMoov2 i01 = (InMoov2) Runtime.start("i01", "InMoov2");

			WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
			webgui.autoStartBrowser(false);
			webgui.startService();

			boolean done = true;
			if (done) {
				return;
			}

			i01.startBrain();

			i01.startAll("COM3", "COM4");
			Runtime.start("python", "Python");
			// Runtime.start("log", "Log");

			/*
			 * OpenCV cv = (OpenCV) Runtime.start("cv", "OpenCV"); cv.setCameraIndex(2);
			 */
			// i01.startSimulator();
			/*
			 * Arduino mega = (Arduino) Runtime.start("mega", "Arduino");
			 * mega.connect("/dev/ttyACM0");
			 */

		} catch (Exception e) {
			log.error("main threw", e);
		}
	}

	boolean autoStartBrowser = false;

	transient ProgramAB brain;

	Set<String> configs = null;

	String currentConfigurationName = "default";

	transient SpeechRecognizer ear;

	transient OpenCV eye;

	transient Tracking eyesTracking;
	// waiting controable threaded gestures we warn user
	boolean gestureAlreadyStarted = false;
	// FIXME - what the hell is this for ?
	Set<String> gestures = new TreeSet<String>();
	transient InMoov2Head head;

	transient Tracking headTracking;

	transient HtmlFilter htmlFilter;
	
	transient UltrasonicSensor ultraSonicRight;

	transient UltrasonicSensor ultraSonicLeft;
	
	transient Pir pir;

	// transient ImageDisplay imageDisplay;

	/**
	 * simple booleans to determine peer state of existence FIXME - should be an
	 * auto-peer variable
	 */

	boolean isBrainActivated = false;

	boolean isEarActivated = false;

	boolean isEyeActivated = false;

	boolean isEyeLidsActivated = false;

	boolean isHeadActivated = false;

	boolean isLeftArmActivated = false;

	boolean isLeftHandActivated = false;

	boolean isMouthActivated = false;

	boolean isRightArmActivated = false;

	boolean isRightHandActivated = false;

	boolean isSimulatorActivated = false;

	boolean isTorsoActivated = false;

	boolean isNeopixelActivated = false;

	boolean isPirActivated = false;

	boolean isUltraSonicRightActivated = false;

	boolean isUltraSonicLeftActivated = false;
	
	boolean isServoMixerActivated = false;

	// TODO - refactor into a Simulator interface when more simulators are borgd
	transient JMonkeyEngine jme;

	String lastGestureExecuted;

	Long lastPirActivityTime;

	transient InMoov2Arm leftArm;

	// transient LanguagePack languagePack = new LanguagePack();

	// transient InMoovEyelids eyelids; eyelids are in the head

	transient InMoov2Hand leftHand;

	Locale locale;

	/**
	 * supported locales
	 */
	Map<String, Locale> locales = null;

	int maxInactivityTimeSeconds = 120;

	transient SpeechSynthesis mouth;

	// FIXME ugh - new MouthControl service that uses AudioFile output
	transient public MouthControl mouthControl;

	boolean mute = false;

	transient NeoPixel neopixel;
	
	transient ServoMixer servomixer;

	transient Python python;

	transient InMoov2Arm rightArm;

	transient InMoov2Hand rightHand;

	/**
	 * used to remember/serialize configuration the user's desired speech type
	 */
	String speechService = "MarySpeech";

	transient InMoov2Torso torso;

	@Deprecated
	public Vision vision;

	// FIXME - remove all direct references
	// transient private HashMap<String, InMoov2Arm> arms = new HashMap<>();

	protected List<Voice> voices = null;

	protected String voiceSelected;

	transient WebGui webgui;

	public InMoov2(String n, String id) {
		super(n, id);

		// by default all servos will auto-disable
		Servo.setAutoDisableDefault(true);

		locales = Locale.getLocaleMap("en-US", "fr-FR", "es-ES", "de-DE", "nl-NL", "ru-RU", "hi-IN", "it-IT", "fi-FI",
				"pt-PT");
		locale = Runtime.getInstance().getLocale();

		python = (Python) startPeer("python");
		load(locale.getTag());

		// get events of new services and shutdown
		Runtime r = Runtime.getInstance();
		subscribe(r.getName(), "shutdown");

		listConfigFiles();

		// FIXME - Framework should auto-magically auto-start peers AFTER
		// construction - unless explicitly told not to
		// peers to start on construction
		// imageDisplay = (ImageDisplay) startPeer("imageDisplay");
	}

	@Override /* local strong type - is to be avoided - use name string */
	public void addTextListener(TextListener service) {
		// CORRECT WAY ! - no direct reference - just use the name in a subscription
		addListener("publishText", service.getName());
	}

	@Override
	public void attachTextListener(TextListener service) {
		addListener("publishText", service.getName());
	}

	public void attachTextPublisher(String name) {
		subscribe(name, "publishText");
	}

	@Override
	public void attachTextPublisher(TextPublisher service) {
		subscribe(service.getName(), "publishText");
	}

	public void beginCheckingOnInactivity() {
		beginCheckingOnInactivity(maxInactivityTimeSeconds);
	}

	public void beginCheckingOnInactivity(int maxInactivityTimeSeconds) {
		this.maxInactivityTimeSeconds = maxInactivityTimeSeconds;
		// speakBlocking("power down after %s seconds inactivity is on",
		// this.maxInactivityTimeSeconds);
		log.info("power down after %s seconds inactivity is on", this.maxInactivityTimeSeconds);
		addTask("checkInactivity", 5 * 1000, 0, "checkInactivity");
	}

	public long checkInactivity() {
		// speakBlocking("checking");
		long lastActivityTime = getLastActivityTime();
		long now = System.currentTimeMillis();
		long inactivitySeconds = (now - lastActivityTime) / 1000;
		if (inactivitySeconds > maxInactivityTimeSeconds) {
			// speakBlocking("%d seconds have passed without activity",
			// inactivitySeconds);
			powerDown();
		} else {
			// speakBlocking("%d seconds have passed without activity",
			// inactivitySeconds);
			info("checking checkInactivity - %d seconds have passed without activity", inactivitySeconds);
		}
		return lastActivityTime;
	}

	public void closeAllImages() {
		// imageDisplay.closeAll();
		log.error("implement webgui.closeAllImages");
	}

	public void cycleGestures() {
		// if not loaded load -
		// FIXME - this needs alot of "help" :P
		// WHY IS THIS DONE ?
		if (gestures.size() == 0) {
			loadGestures();
		}

		for (String gesture : gestures) {
			try {
				String methodName = gesture.substring(0, gesture.length() - 3);
				speakBlocking(methodName);
				log.info("executing gesture {}", methodName);
				python.eval(methodName + "()");

				// wait for finish - or timeout ?

			} catch (Exception e) {
				error(e);
			}
		}
	}

	public void disable() {
		if (head != null) {
			head.disable();
		}
		if (rightHand != null) {
			rightHand.disable();
		}
		if (leftHand != null) {
			leftHand.disable();
		}
		if (rightArm != null) {
			rightArm.disable();
		}
		if (leftArm != null) {
			leftArm.disable();
		}
		if (torso != null) {
			torso.disable();
		}
	}

	public void displayFullScreen(String src) {
		try {
			// imageDisplay.displayFullScreen(src);
			log.error("implement webgui.displayFullScreen");
		} catch (Exception e) {
			error("could not display picture %s", src);
		}
	}

	public void enable() {
		if (head != null) {
			head.enable();
		}
		if (rightHand != null) {
			rightHand.enable();
		}
		if (leftHand != null) {
			leftHand.enable();
		}
		if (rightArm != null) {
			rightArm.enable();
		}
		if (leftArm != null) {
			leftArm.enable();
		}
		if (torso != null) {
			torso.enable();
		}
	}

	/**
	 * This method will try to launch a python command with error handling
	 */
	public String execGesture(String gesture) {

		lastGestureExecuted = gesture;
		if (python == null) {
			log.warn("execGesture : No jython engine...");
			return null;
		}
		subscribe(python.getName(), "publishStatus", this.getName(), "onGestureStatus");
		startedGesture(lastGestureExecuted);
		return python.evalAndWait(gesture);
	}

	public void finishedGesture() {
		finishedGesture("unknown");
	}

	public void finishedGesture(String nameOfGesture) {
		if (gestureAlreadyStarted) {
			waitTargetPos();
			RobotCanMoveRandom = true;
			gestureAlreadyStarted = false;
			log.info("gesture : {} finished...", nameOfGesture);
		}
	}

	public void fullSpeed() {
		if (head != null) {
			head.fullSpeed();
		}
		if (rightHand != null) {
			rightHand.fullSpeed();
		}
		if (leftHand != null) {
			leftHand.fullSpeed();
		}
		if (rightArm != null) {
			rightArm.fullSpeed();
		}
		if (leftArm != null) {
			leftArm.fullSpeed();
		}
		if (torso != null) {
			torso.fullSpeed();
		}
	}

	public String get(String param) {
		if (lpVars.containsKey(param.toUpperCase())) {
			return lpVars.get(param.toUpperCase());
		}
		return "not yet translated";

	}

	public InMoov2Arm getArm(String side) {
		if ("left".equals(side)) {
			return leftArm;
		} else if ("right".equals(side)) {
			return rightArm;
		} else {
			log.error("can not get arm {}", side);
		}
		return null;
	}

	public InMoov2Hand getHand(String side) {
		if ("left".equals(side)) {
			return leftHand;
		} else if ("right".equals(side)) {
			return rightHand;
		} else {
			log.error("can not get arm {}", side);
		}
		return null;
	}

	public InMoov2Head getHead() {
		return head;
	}

	/**
	 * get current language
	 */
	public String getLanguage() {
		return locale.getLanguage();
	}

	/**
	 * finds most recent activity
	 * 
	 * @return the timestamp of the last activity time.
	 */
	public long getLastActivityTime() {

		long lastActivityTime = 0;

		if (leftHand != null) {
			lastActivityTime = Math.max(lastActivityTime, leftHand.getLastActivityTime());
		}

		if (leftArm != null) {
			lastActivityTime = Math.max(lastActivityTime, leftArm.getLastActivityTime());
		}

		if (rightHand != null) {
			lastActivityTime = Math.max(lastActivityTime, rightHand.getLastActivityTime());
		}

		if (rightArm != null) {
			lastActivityTime = Math.max(lastActivityTime, rightArm.getLastActivityTime());
		}

		if (head != null) {
			lastActivityTime = Math.max(lastActivityTime, head.getLastActivityTime());
		}

		if (torso != null) {
			lastActivityTime = Math.max(lastActivityTime, torso.getLastActivityTime());
		}

		if (lastPirActivityTime != null) {
			lastActivityTime = Math.max(lastActivityTime, lastPirActivityTime);
		}

		if (lastActivityTime == 0) {
			error("invalid activity time - anything connected?");
			lastActivityTime = System.currentTimeMillis();
		}

		return lastActivityTime;
	}

	public InMoov2Arm getLeftArm() {
		return leftArm;
	}

	public InMoov2Hand getLeftHand() {
		return leftHand;
	}

	@Override
	public Locale getLocale() {
		return locale;
	}

	@Override
	public Map<String, Locale> getLocales() {
		return locales;
	}

	public InMoov2Arm getRightArm() {
		return rightArm;
	}

	public InMoov2Hand getRightHand() {
		return rightHand;
	}

	public Simulator getSimulator() {
		return jme;
	}

	public InMoov2Torso getTorso() {
		return torso;
	}

	public void halfSpeed() {
		if (head != null) {
			head.setSpeed(25.0, 25.0, 25.0, 25.0, -1.0, 25.0);
		}

		if (rightHand != null) {
			rightHand.setSpeed(30.0, 30.0, 30.0, 30.0, 30.0, 30.0);
		}
		if (leftHand != null) {
			leftHand.setSpeed(30.0, 30.0, 30.0, 30.0, 30.0, 30.0);
		}
		if (rightArm != null) {
			rightArm.setSpeed(25.0, 25.0, 25.0, 25.0);
		}
		if (leftArm != null) {
			leftArm.setSpeed(25.0, 25.0, 25.0, 25.0);
		}
		if (torso != null) {
			torso.setSpeed(20.0, 20.0, 20.0);
		}

	}

	public boolean isCameraOn() {
		if (eye != null) {
			if (eye.isCapturing()) {
				return true;
			}
		}
		return false;
	}

	public boolean isEyeLidsActivated() {
		return isEyeLidsActivated;
	}

	public boolean isHeadActivated() {
		return isHeadActivated;
	}

	public boolean isLeftArmActivated() {
		return isLeftArmActivated;
	}

	public boolean isLeftHandActivated() {
		return isLeftHandActivated;
	}

	public boolean isMute() {
		return mute;
	}

	public boolean isNeopixelActivated() {
		return isNeopixelActivated;
	}

	public boolean isRightArmActivated() {
		return isRightArmActivated;
	}

	public boolean isRightHandActivated() {
		return isRightHandActivated;
	}

	public boolean isTorsoActivated() {
		return isTorsoActivated;
	}

	public boolean isPirActivated() {
		return isPirActivated;
	}

	public boolean isUltraSonicRightActivated() {
		return isUltraSonicRightActivated;
	}

	public boolean isUltraSonicLeftActivated() {
		return isUltraSonicLeftActivated;
	}
	
	public boolean isServoMixerActivated() {
        return isServoMixerActivated;
        }

	public Set<String> listConfigFiles() {

		configs = new HashSet<>();

		// data list
		String configDir = getResourceDir() + fs + "config";
		File f = new File(configDir);
		if (!f.exists()) {
			f.mkdirs();
		}
		String[] files = f.list();
		for (String config : files) {
			configs.add(config);
		}

		// data list
		configDir = getDataDir() + fs + "config";
		f = new File(configDir);
		if (!f.exists()) {
			f.mkdirs();
		}
		files = f.list();
		for (String config : files) {
			configs.add(config);
		}

		return configs;
	}

	/*
	 * iterate over each txt files in the directory
	 */
	public void load(String locale) {
		String extension = "lang";
		File dir = Utils.makeDirectory(getResourceDir() + File.separator + "system" + File.separator + "languagePack"
				+ File.separator + locale);
		if (dir.exists()) {
			lpVars.clear();
			for (File f : dir.listFiles()) {
				if (f.isDirectory()) {
					continue;
				}
				if (FilenameUtils.getExtension(f.getAbsolutePath()).equalsIgnoreCase(extension)) {
					log.info("Inmoov languagePack load : {}", f.getName());
					try {
						BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
						for (String line = br.readLine(); line != null; line = br.readLine()) {
							String[] parts = line.split("::");
							if (parts.length > 1) {
								lpVars.put(parts[0].toUpperCase(), parts[1]);
							}
						}
					} catch (IOException e) {
						log.error("LanguagePack : {}", e);
					}
				} else {
					log.warn("{} is not a {} file", f.getAbsolutePath(), extension);
				}
			}
		}
	}

	// FIXME - what is this for ???
	public void loadGestures() {
		loadGestures(getResourceDir() + fs + "gestures");
	}

	/**
	 * This blocking method will look at all of the .py files in a directory. One by
	 * one it will load the files into the python interpreter. A gesture python file
	 * should contain 1 method definition that is the same as the filename.
	 * 
	 * @param directory - the directory that contains the gesture python files.
	 */
	public boolean loadGestures(String directory) {
		speakBlocking(get("STARTINGGESTURES"));

		// iterate over each of the python files in the directory
		// and load them into the python interpreter.
		String extension = "py";
		Integer totalLoaded = 0;
		Integer totalError = 0;

		File dir = new File(directory);
		dir.mkdirs();

		if (dir.exists()) {
			for (File f : dir.listFiles()) {
				if (FilenameUtils.getExtension(f.getAbsolutePath()).equalsIgnoreCase(extension)) {
					if (loadFile(f.getAbsolutePath()) == true) {
						totalLoaded += 1;
						String methodName = f.getName().substring(0, f.getName().length() - 3) + "()";
						gestures.add(methodName);
					} else {
						error("could not load %s", f.getName());
						totalError += 1;
					}
				} else {
					log.info("{} is not a {} file", f.getAbsolutePath(), extension);
				}
			}
		}
		info("%s Gestures loaded, %s Gestures with error", totalLoaded, totalError);
		broadcastState();
		if (totalError > 0) {
			speakAlert(get("GESTURE_ERROR"));
			return false;
		}
		return true;
	}

	public void moveArm(String which, double bicep, double rotate, double shoulder, double omoplate) {
		InMoov2Arm arm = getArm(which);
		if (arm == null) {
			info("%s arm not started", which);
			return;
		}
		arm.moveTo(bicep, rotate, shoulder, omoplate);
	}

	public void moveEyelids(double eyelidleftPos, double eyelidrightPos) {
		if (head != null) {
			head.moveEyelidsTo(eyelidleftPos, eyelidrightPos);
		} else {
			log.warn("moveEyelids - I have a null head");
		}
	}

	public void moveEyes(double eyeX, double eyeY) {
		if (head != null) {
			head.moveTo(null, null, eyeX, eyeY, null, null);
		} else {
			log.warn("moveEyes - I have a null head");
		}
	}

	public void moveHand(String which, double thumb, double index, double majeure, double ringFinger, double pinky) {
		moveHand(which, thumb, index, majeure, ringFinger, pinky, null);
	}

	public void moveHand(String which, Double thumb, Double index, Double majeure, Double ringFinger, Double pinky,
			Double wrist) {
		InMoov2Hand hand = getHand(which);
		if (hand == null) {
			log.warn("{} hand does not exist");
			return;
		}
		hand.moveTo(thumb, index, majeure, ringFinger, pinky, wrist);
	}

	public void moveHead(double neck, double rothead) {
		moveHead(neck, rothead, null);
	}

	public void moveHead(double neck, double rothead, double eyeX, double eyeY, double jaw) {
		moveHead(neck, rothead, eyeX, eyeY, jaw, null);
	}

	public void moveHead(Double neck, Double rothead, Double rollNeck) {
		moveHead(neck, rothead, null, null, null, rollNeck);
	}

	public void moveHead(Double neck, Double rothead, Double eyeX, Double eyeY, Double jaw, Double rollNeck) {
		if (head != null) {
			head.moveTo(neck, rothead, eyeX, eyeY, jaw, rollNeck);
		} else {
			log.error("I have a null head");
		}
	}

	public void moveHeadBlocking(double neck, double rothead) {
		moveHeadBlocking(neck, rothead, null);
	}

	public void moveHeadBlocking(double neck, double rothead, Double rollNeck) {
		moveHeadBlocking(neck, rothead, null, null, null, rollNeck);
	}

	public void moveHeadBlocking(double neck, double rothead, Double eyeX, Double eyeY, Double jaw) {
		moveHeadBlocking(neck, rothead, eyeX, eyeY, jaw, null);
	}

	public void moveHeadBlocking(Double neck, Double rothead, Double eyeX, Double eyeY, Double jaw, Double rollNeck) {
		if (head != null) {
			head.moveToBlocking(neck, rothead, eyeX, eyeY, jaw, rollNeck);
		} else {
			log.error("I have a null head");
		}
	}

	public void moveTorso(double topStom, double midStom, double lowStom) {
		if (torso != null) {
			torso.moveTo(topStom, midStom, lowStom);
		} else {
			log.error("moveTorso - I have a null torso");
		}
	}

	public void moveTorsoBlocking(double topStom, double midStom, double lowStom) {
		if (torso != null) {
			torso.moveToBlocking(topStom, midStom, lowStom);
		} else {
			log.error("moveTorsoBlocking - I have a null torso");
		}
	}

	public void onGestureStatus(Status status) {
		if (!status.equals(Status.success()) && !status.equals(Status.warn("Python process killed !"))) {
			error("I cannot execute %s, please check logs", lastGestureExecuted);
		}
		finishedGesture(lastGestureExecuted);
		unsubscribe(python.getName(), "publishStatus", this.getName(), "onGestureStatus");
	}

	@Override
	public void onJoystickInput(JoystickData input) throws Exception {
		// TODO Auto-generated method stub

	}

	public OpenCVData onOpenCVData(OpenCVData data) {
		return data;
	}

	@Override
	public void onText(String text) {
		// FIXME - we should be able to "re"-publish text but text is coming from
		// different sources
		// some might be coming from the ear - some from the mouth ... - there has
		// to be a distinction
		log.info("onText - {}", text);
		invoke("publishText", text);
	}

	// TODO FIX/CHECK this, migrate from python land
	public void powerDown() {

		rest();
		purgeTasks();
		disable();

		if (ear != null) {
			ear.lockOutAllGrammarExcept("power up");
		}

		python.execMethod("power_down");
	}

	// TODO FIX/CHECK this, migrate from python land
	public void powerUp() {
		enable();
		rest();

		if (ear != null) {
			ear.clearLock();
		}

		beginCheckingOnInactivity();

		python.execMethod("power_up");
	}

	/**
	 * all published text from InMoov2 - including ProgramAB
	 */
	@Override
	public String publishText(String text) {
		return text;
	}

	public void releaseService() {
		try {
			disable();
			releasePeers();
			super.releaseService();
		} catch (Exception e) {
			error(e);
		}
	}

	// FIXME NO DIRECT REFERENCES - publishRest --> (onRest) --> rest
	public void rest() {
		log.info("InMoov2.rest()");
		if (head != null) {
			head.rest();
		}
		if (rightHand != null) {
			rightHand.rest();
		}
		if (leftHand != null) {
			leftHand.rest();
		}
		if (rightArm != null) {
			rightArm.rest();
		}
		if (leftArm != null) {
			leftArm.rest();
		}
		if (torso != null) {
			torso.rest();
		}
	}

	@Deprecated
	public void setArmVelocity(String which, Double bicep, Double rotate, Double shoulder, Double omoplate) {
		InMoov2Arm arm = getArm(which);
		if (arm == null) {
			warn("%s hand not started", which);
			return;
		}
		arm.setSpeed(bicep, rotate, shoulder, omoplate);
	}

	public void setAutoDisable(Boolean param) {
		if (head != null) {
			head.setAutoDisable(param);
		}
		if (rightArm != null) {
			rightArm.setAutoDisable(param);
		}
		if (leftArm != null) {
			leftArm.setAutoDisable(param);
		}
		if (leftHand != null) {
			leftHand.setAutoDisable(param);
		}
		if (rightHand != null) {
			leftHand.setAutoDisable(param);
		}
		if (torso != null) {
			torso.setAutoDisable(param);
		}
		/*
		 * if (eyelids != null) { eyelids.setAutoDisable(param); }
		 */
	}

	public void setHandSpeed(String which, Double thumb, Double index, Double majeure, Double ringFinger,
			Double pinky) {
		setHandSpeed(which, thumb, index, majeure, ringFinger, pinky, null);
	}

	public void setHandSpeed(String which, Double thumb, Double index, Double majeure, Double ringFinger, Double pinky,
			Double wrist) {
		InMoov2Hand hand = getHand(which);
		if (hand == null) {
			warn("%s hand not started", which);
			return;
		}
		hand.setSpeed(thumb, index, majeure, ringFinger, pinky, wrist);
	}

	@Deprecated
	public void setHandVelocity(String which, Double thumb, Double index, Double majeure, Double ringFinger,
			Double pinky) {
		setHandSpeed(which, thumb, index, majeure, ringFinger, pinky, null);
	}

	@Deprecated
	public void setHandVelocity(String which, Double thumb, Double index, Double majeure, Double ringFinger,
			Double pinky, Double wrist) {
		setHandSpeed(which, thumb, index, majeure, ringFinger, pinky, wrist);
	}

	public void setHeadSpeed(Double rothead, Double neck) {
		setHeadSpeed(rothead, neck, null, null, null);
	}

	public void setHeadSpeed(Double rothead, Double neck, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed) {
		setHeadSpeed(rothead, neck, eyeXSpeed, eyeYSpeed, jawSpeed, null);
	}

	public void setHeadSpeed(Double rothead, Double neck, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed,
			Double rollNeckSpeed) {
		if (head == null) {
			warn("setHeadSpeed - head not started");
			return;
		}
		head.setSpeed(rothead, neck, eyeXSpeed, eyeYSpeed, jawSpeed, rollNeckSpeed);
	}

	@Deprecated
	public void setHeadVelocity(Double rothead, Double neck) {
		setHeadSpeed(rothead, neck, null, null, null, null);
	}

	@Deprecated
	public void setHeadVelocity(Double rothead, Double neck, Double rollNeck) {
		setHeadSpeed(rothead, neck, null, null, null, rollNeck);
	}

	@Deprecated
	public void setHeadVelocity(Double rothead, Double neck, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed) {
		setHeadSpeed(rothead, neck, eyeXSpeed, eyeYSpeed, jawSpeed, null);
	}

	@Deprecated
	public void setHeadVelocity(Double rothead, Double neck, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed,
			Double rollNeckSpeed) {
		setHeadSpeed(rothead, neck, eyeXSpeed, eyeYSpeed, jawSpeed, rollNeckSpeed);
	}

	/**
	 * TODO : use system locale set language for InMoov service used by chatbot +
	 * 
	 * @param code
	 * @return
	 */
	@Deprecated /* use setLocale */
	public String setLanguage(String code) {
		setLocale(code);
		return code;
	}

	@Override
	public void setLocale(String code) {

		if (code == null) {
			log.warn("setLocale null");
			return;
		}

		// filter of the set of supported locales
		if (!locales.containsKey(code)) {
			error("InMooov does not support %s only %s", code, locales.keySet());
			return;
		}

		locale = new Locale(code);

		speakBlocking("setting language to %s", locale.getDisplayLanguage());

		// attempt to set all other language providers to the same language as me
		List<String> providers = Runtime.getServiceNamesFromInterface(LocaleProvider.class);
		for (String provider : providers) {
			if (!provider.equals(getName())) {
				log.info("{} setting locale to %s", provider, code);
				send(provider, "setLocale", code);
				send(provider, "broadcastState");
			}
		}

		load(locale.getTag());

	}

	public void setMute(boolean mute) {
		info("Set mute to %s", mute);
		this.mute = mute;
		sendToPeer("mouth", "setMute", mute);
		broadcastState();
	}

	public void setNeopixelAnimation(String animation, Integer red, Integer green, Integer blue, Integer speed) {
		if (neopixel != null /* && neopixelArduino != null */) {
			neopixel.setAnimation(animation, red, green, blue, speed);
		} else {
			warn("No Neopixel attached");
		}
	}

	public String setSpeechType(String speechType) {
		speechService = speechType;
		setPeer("mouth", speechType);
		return speechType;
	}

	public void setTorsoSpeed(Double topStom, Double midStom, Double lowStom) {
		if (torso != null) {
			torso.setSpeed(topStom, midStom, lowStom);
		} else {
			log.warn("setTorsoSpeed - I have no torso");
		}
	}

	@Deprecated
	public void setTorsoVelocity(Double topStom, Double midStom, Double lowStom) {
		if (torso != null) {
			torso.setVelocity(topStom, midStom, lowStom);
		} else {
			log.warn("setTorsoVelocity - I have no torso");
		}
	}

	/**
	 * overridden setVirtual for InMoov sets "all" services to virtual
	 */
	public boolean setVirtual(boolean virtual) {
		super.setVirtual(virtual);
		Platform.setVirtual(virtual);
		return virtual;
	}

	public void setVoice(String name) {
		if (mouth != null) {
			mouth.setVoice(name);
			voiceSelected = name;
			speakBlocking("setting voice to %s", name);
		}
	}

	public void speak(String toSpeak) {
		sendToPeer("mouth", "speak", toSpeak);
	}

	public void speakAlert(String toSpeak) {
		speakBlocking(get("ALERT"));
		speakBlocking(toSpeak);
	}

	public void speakBlocking(String speak) {
		speakBlocking(speak, null);
	}

	// FIXME - publish text regardless if mouth exists ...
	public void speakBlocking(String format, Object... args) {

		if (format == null) {
			return;
		}

		String toSpeak = format;
		if (args != null) {
			toSpeak = String.format(format, args);
		}

		// FIXME - publish onText when listening
		invoke("publishText", toSpeak);

		if (!mute) {
			// sendToPeer("mouth", "speakBlocking", toSpeak);
			invokePeer("mouth", "speakBlocking", toSpeak);
		}
	}

	public void startAll() throws Exception {
		startAll(null, null);
	}

	public void startAll(String leftPort, String rightPort) throws Exception {
		startMouth();
		startBrain();

		startHeadTracking();
		// startEyesTracking();
		// startOpenCV();
		startEar();

		startServos(leftPort, rightPort);
		// startMouthControl(head.jaw, mouth);

		speakBlocking("startup sequence completed");
	}

	public ProgramAB startBrain() {

		try {

			brain = (ProgramAB) startPeer("brain");
			isBrainActivated = true;

			speakBlocking(get("CHATBOTACTIVATED"));

			// GOOD EXAMPLE ! - no type, uses name - does a set of subscriptions !
			// attachTextPublisher(brain.getName());

			/*
			 * not necessary - ear needs to be attached to mouth not brain if (ear != null)
			 * { ear.attachTextListener(brain); }
			 */

			brain.attachTextPublisher(ear);

			// this.attach(brain); FIXME - attach as a TextPublisher - then re-publish
			// FIXME - deal with language
			// speakBlocking(get("CHATBOTACTIVATED"));
			brain.repetitionCount(10);
			brain.setPath(getResourceDir() + fs + "chatbot");
			brain.startSession("default", locale.getTag());
			// reset some parameters to default...
			brain.setPredicate("topic", "default");
			brain.setPredicate("questionfirstinit", "");
			brain.setPredicate("tmpname", "");
			brain.setPredicate("null", "");
			// load last user session
			if (!brain.getPredicate("name").isEmpty()) {
				if (brain.getPredicate("lastUsername").isEmpty()
						|| brain.getPredicate("lastUsername").equals("unknown")) {
					brain.setPredicate("lastUsername", brain.getPredicate("name"));
				}
			}
			brain.setPredicate("parameterHowDoYouDo", "");
			try {
				brain.savePredicates();
			} catch (IOException e) {
				log.error("saving predicates threw", e);
			}
			// start session based on last recognized person
			if (!brain.getPredicate("default", "lastUsername").isEmpty()
					&& !brain.getPredicate("default", "lastUsername").equals("unknown")) {
				brain.startSession(brain.getPredicate("lastUsername"));
			}

			htmlFilter = (HtmlFilter) startPeer("htmlFilter");// Runtime.start("htmlFilter",
																// "HtmlFilter");
			brain.attachTextListener(htmlFilter);
			htmlFilter.attachTextListener((TextListener) getPeer("mouth"));
			brain.attachTextListener(this);
		} catch (Exception e) {
			speak("could not load brain");
			error(e.getMessage());
			speak(e.getMessage());
		}
		broadcastState();
		return brain;
	}

	public SpeechRecognizer startEar() {

		ear = (SpeechRecognizer) startPeer("ear");
		isEarActivated = true;

		ear.attachSpeechSynthesis((SpeechSynthesis) getPeer("mouth"));
		ear.attachTextListener(brain);

		speakBlocking(get("STARTINGEAR"));
		broadcastState();
		return ear;
	}

	public void startedGesture() {
		startedGesture("unknown");
	}

	public void startedGesture(String nameOfGesture) {
		if (gestureAlreadyStarted) {
			warn("Warning 1 gesture already running, this can break spacetime and lot of things");
		} else {
			log.info("Starting gesture : {}", nameOfGesture);
			gestureAlreadyStarted = true;
			RobotCanMoveRandom = false;
		}
	}

	// FIXME - universal (good) way of handling all exceptions - ie - reporting
	// back to the user the problem in a short concise way but have
	// expandable detail in appropriate places
	public OpenCV startEye() throws Exception {
		speakBlocking(get("STARTINGOPENCV"));
		eye = (OpenCV) startPeer("eye", "OpenCV");
		subscribeTo(eye.getName(), "publishOpenCVData");
		isEyeActivated = true;
		return eye;
	}

	public Tracking startEyesTracking() throws Exception {
		if (head == null) {
			startHead();
		}
		return startHeadTracking(head.eyeX, head.eyeY);
	}

	public Tracking startEyesTracking(ServoControl eyeX, ServoControl eyeY) throws Exception {
		if (eye == null) {
			startEye();
		}
		speakBlocking(get("TRACKINGSTARTED"));
		eyesTracking = (Tracking) this.startPeer("eyesTracking");
		eyesTracking.connect(eye, head.eyeX, head.eyeY);
		return eyesTracking;
	}

	public InMoov2Head startHead() throws Exception {
		return startHead(null, null, null, null, null, null, null, null);
	}

	public InMoov2Head startHead(String port) throws Exception {
		return startHead(port, null, null, null, null, null, null, null);
	}

	// legacy inmoov head exposed pins
	public InMoov2Head startHead(String port, String type, Integer headYPin, Integer headXPin, Integer eyeXPin,
			Integer eyeYPin, Integer jawPin, Integer rollNeckPin) {

		// log.warn(InMoov.buildDNA(myKey, serviceClass))
		// speakBlocking(get("STARTINGHEAD") + " " + port);
		// ??? SHOULD THERE BE REFERENCES AT ALL ??? ... probably not

		speakBlocking(get("STARTINGHEAD"));

		head = (InMoov2Head) startPeer("head");
		isHeadActivated = true;

		if (headYPin != null) {
			head.setPins(headYPin, headXPin, eyeXPin, eyeYPin, jawPin, rollNeckPin);
		}

		// lame assumption - port is specified - it must be an Arduino :(
		if (port != null) {
			try {
				speakBlocking(get(port));
				Arduino arduino = (Arduino) startPeer("left", "Arduino");
				arduino.connect(port);

				arduino.attach(head.neck);
				arduino.attach(head.rothead);
				arduino.attach(head.eyeX);
				arduino.attach(head.eyeY);
				arduino.attach(head.jaw);
				arduino.attach(head.rollNeck);

			} catch (Exception e) {
				error(e);
			}
		}

		speakBlocking(get("STARTINGMOUTHCONTROL"));
		mouthControl = (MouthControl) startPeer("mouthControl");
		mouthControl.attach(head.jaw);
		mouthControl.attach((Attachable) getPeer("mouth"));
		mouthControl.setmouth(10, 50);// <-- FIXME - not the right place for
										// config !!!

		return head;
	}

	public void startHeadTracking() throws Exception {
		if (eye == null) {
			startEye();
		}

		if (head == null) {
			startHead();
		}

		if (headTracking == null) {
			speakBlocking(get("TRACKINGSTARTED"));
			headTracking = (Tracking) this.startPeer("headTracking");
			headTracking.connect(this.eye, head.rothead, head.neck);
		}
	}

	public Tracking startHeadTracking(ServoControl rothead, ServoControl neck) throws Exception {
		if (eye == null) {
			startEye();
		}

		if (headTracking == null) {
			speakBlocking(get("TRACKINGSTARTED"));
			headTracking = (Tracking) this.startPeer("headTracking");
			headTracking.connect(this.eye, rothead, neck);
		}
		return headTracking;
	}

	public InMoov2Arm startLeftArm() {
		return startLeftArm(null);
	}

	public InMoov2Arm startLeftArm(String port) {
		// log.warn(InMoov.buildDNA(myKey, serviceClass))
		// speakBlocking(get("STARTINGHEAD") + " " + port);
		// ??? SHOULD THERE BE REFERENCES AT ALL ??? ... probably not

		speakBlocking(get("STARTINGLEFTARM"));
		leftArm = (InMoov2Arm) startPeer("leftArm");
		isLeftArmActivated = true;

		if (port != null) {
			try {
				speakBlocking(port);
				Arduino arduino = (Arduino) startPeer("left", "Arduino");
				arduino.connect(port);

				arduino.attach(leftArm.bicep);
				arduino.attach(leftArm.omoplate);
				arduino.attach(leftArm.rotate);
				arduino.attach(leftArm.shoulder);
			} catch (Exception e) {
				error(e);
			}
		}
		return leftArm;
	}

	public InMoov2Hand startLeftHand() {
		return startLeftHand(null);
	}

	public InMoov2Hand startLeftHand(String port) {

		speakBlocking(get("STARTINGLEFTHAND"));
		leftHand = (InMoov2Hand) startPeer("leftHand");
		isLeftHandActivated = true;

		if (port != null) {
			try {
				speakBlocking(port);
				Arduino arduino = (Arduino) startPeer("left", "Arduino");
				arduino.connect(port);

				arduino.attach(leftHand.thumb);
				arduino.attach(leftHand.index);
				arduino.attach(leftHand.majeure);
				arduino.attach(leftHand.ringFinger);
				arduino.attach(leftHand.pinky);
				arduino.attach(leftHand.wrist);

			} catch (Exception e) {
				error(e);
			}
		}

		return leftHand;
	}

	// TODO - general objective "might" be to reduce peers down to something
	// that does not need a reference - where type can be switched before creation
	// and the onnly thing needed is pubs/subs that are not handled in abstracts
	public SpeechSynthesis startMouth() {

		mouth = (SpeechSynthesis) startPeer("mouth");
		voices = mouth.getVoices();
		Voice voice = mouth.getVoice();
		if (voice != null) {
			voiceSelected = voice.getName();
		}

		isMouthActivated = true;

		if (mute) {
			mouth.setMute(true);
		}

		mouth.attachSpeechRecognizer(ear);
		// mouth.attach(htmlFilter); // same as brain not needed

		// this.attach((Attachable) mouth);
		// if (ear != null) ....

		broadcastState();

		speakBlocking(get("STARTINGMOUTH"));
		if (Platform.isVirtual()) {
			speakBlocking("in virtual hardware mode");
		}
		speakBlocking(get("WHATISTHISLANGUAGE"));

		return mouth;
	}

	@Deprecated /* use start eye */
	public void startOpenCV() throws Exception {
		startEye();
	}

	public InMoov2Arm startRightArm() {
		return startRightArm(null);
	}

	public InMoov2Arm startRightArm(String port) {

		speakBlocking(get("STARTINGRIGHTARM"));

		rightArm = (InMoov2Arm) startPeer("rightArm");
		isRightArmActivated = true;

		if (port != null) {
			try {
				speakBlocking(port);
				Arduino arduino = (Arduino) startPeer("right", "Arduino");
				arduino.connect(port);

				arduino.attach(rightArm.bicep);
				arduino.attach(rightArm.omoplate);
				arduino.attach(rightArm.rotate);
				arduino.attach(rightArm.shoulder);
			} catch (Exception e) {
				error(e);
			}
		}

		return rightArm;
	}

	public InMoov2Hand startRightHand() {
		return startRightHand(null);
	}

	public InMoov2Hand startRightHand(String port) {

		speakBlocking(get("STARTINGRIGHTHAND"));
		rightHand = (InMoov2Hand) startPeer("rightHand");
		isRightHandActivated = true;

		if (port != null) {
			try {
				speakBlocking(port);
				Arduino arduino = (Arduino) startPeer("right", "Arduino");
				arduino.connect(port);

				arduino.attach(rightHand.thumb);
				arduino.attach(rightHand.index);
				arduino.attach(rightHand.majeure);
				arduino.attach(rightHand.ringFinger);
				arduino.attach(rightHand.pinky);
				arduino.attach(rightHand.wrist);

			} catch (Exception e) {
				error(e);
			}
		}
		return rightHand;
	}
	
	public Double getUltraSonicRightDistance() {
	if (ultraSonicRight != null) {
	  return ultraSonicRight.range();
	} else {
	  warn("No UltraSonicRight attached");
	  return 0.0;
		}
	}

	public Double getUltraSonicLeftDistance() {
	if (ultraSonicLeft != null) {
	  return ultraSonicLeft.range();
	} else {
	  warn("No UltraSonicLeft attached");
	  return 0.0;
		}
	}
	
	//public void publishPin(Pin pin) {
		//log.info("{} - {}", pin.pin, pin.value);
		//if (pin.value == 1) {
			//lastPIRActivityTime = System.currentTimeMillis();
		//}
		/// if its PIR & PIR is active & was sleeping - then wake up !
		//if (pin == pin.pin && startSleep != null && pin.value == 1) {
			//powerUp();
		//}
	//}

	public void startServos(String leftPort, String rightPort) throws Exception {
		startHead(leftPort);
		startLeftArm(leftPort);
		startLeftHand(leftPort);
		startRightArm(rightPort);
		startRightHand(rightPort);
		startTorso(leftPort);
	}

	// FIXME .. externalize in a json file included in InMoov2
	public Simulator startSimulator() throws Exception {

		speakBlocking(get("STARTINGVIRTUAL"));

		if (jme != null) {
			log.info("start called twice - starting simulator is reentrant");
			return jme;
		}

		jme = (JMonkeyEngine) startPeer("simulator");

		isSimulatorActivated = true;

		// adding InMoov2 asset path to the jonkey simulator
		String assetPath = getResourceDir() + fs + JMonkeyEngine.class.getSimpleName();

		File check = new File(assetPath);
		log.info("loading assets from {}", assetPath);
		if (!check.exists()) {
			log.warn("%s does not exist");
		}

		// disable the frustrating servo events ...
		// Servo.eventsEnabledDefault(false);
		// jme.loadModels(assetPath); not needed - as InMoov2 unzips the model into
		// /resource/JMonkeyEngine/assets

		// ========== gael's calibrations begin ======================
		jme.setRotation(getName() + ".head.jaw", "x");
		jme.setRotation(getName() + ".head.neck", "x");
		jme.setRotation(getName() + ".head.rothead", "y");
		jme.setRotation(getName() + ".head.rollNeck", "z");
		jme.setRotation(getName() + ".head.eyeY", "x");
		jme.setRotation(getName() + ".head.eyeX", "y");
		jme.setRotation(getName() + ".torso.topStom", "z");
		jme.setRotation(getName() + ".torso.midStom", "y");
		jme.setRotation(getName() + ".torso.lowStom", "x");
		jme.setRotation(getName() + ".rightArm.bicep", "x");
		jme.setRotation(getName() + ".leftArm.bicep", "x");
		jme.setRotation(getName() + ".rightArm.shoulder", "x");
		jme.setRotation(getName() + ".leftArm.shoulder", "x");
		jme.setRotation(getName() + ".rightArm.rotate", "y");
		jme.setRotation(getName() + ".leftArm.rotate", "y");
		jme.setRotation(getName() + ".rightArm.omoplate", "z");
		jme.setRotation(getName() + ".leftArm.omoplate", "z");
		jme.setRotation(getName() + ".rightHand.wrist", "y");
		jme.setRotation(getName() + ".leftHand.wrist", "y");

		jme.setMapper(getName() + ".head.jaw", 0, 180, -5, 80);
		jme.setMapper(getName() + ".head.neck", 0, 180, 20, -20);
		jme.setMapper(getName() + ".head.rollNeck", 0, 180, 30, -30);
		jme.setMapper(getName() + ".head.eyeY", 0, 180, 40, 140);
		jme.setMapper(getName() + ".head.eyeX", 0, 180, -10, 70); // HERE there need
																	// to be
		// two eyeX (left and
		// right?)
		jme.setMapper(getName() + ".rightArm.bicep", 0, 180, 0, -150);
		jme.setMapper(getName() + ".leftArm.bicep", 0, 180, 0, -150);

		jme.setMapper(getName() + ".rightArm.shoulder", 0, 180, 30, -150);
		jme.setMapper(getName() + ".leftArm.shoulder", 0, 180, 30, -150);
		jme.setMapper(getName() + ".rightArm.rotate", 0, 180, 80, -80);
		jme.setMapper(getName() + ".leftArm.rotate", 0, 180, -80, 80);
		jme.setMapper(getName() + ".rightArm.omoplate", 0, 180, 10, -180);
		jme.setMapper(getName() + ".leftArm.omoplate", 0, 180, -10, 180);

		jme.setMapper(getName() + ".rightHand.wrist", 0, 180, -20, 60);
		jme.setMapper(getName() + ".leftHand.wrist", 0, 180, 20, -60);

		jme.setMapper(getName() + ".torso.topStom", 0, 180, -30, 30);
		jme.setMapper(getName() + ".torso.midStom", 0, 180, 50, 130);
		jme.setMapper(getName() + ".torso.lowStom", 0, 180, -30, 30);

		// ========== gael's calibrations end ======================

		// ========== 3 joint finger mapping and attaching begin ===

		// ========== Requires VinMoov5.j3o ========================

		jme.attach(getName() + ".leftHand.thumb", getName() + ".leftHand.thumb1", getName() + ".leftHand.thumb2",
				getName() + ".leftHand.thumb3");
		jme.setRotation(getName() + ".leftHand.thumb1", "y");
		jme.setRotation(getName() + ".leftHand.thumb2", "x");
		jme.setRotation(getName() + ".leftHand.thumb3", "x");

		jme.attach(getName() + ".leftHand.index", getName() + ".leftHand.index", getName() + ".leftHand.index2",
				getName() + ".leftHand.index3");
		jme.setRotation(getName() + ".leftHand.index", "x");
		jme.setRotation(getName() + ".leftHand.index2", "x");
		jme.setRotation(getName() + ".leftHand.index3", "x");

		jme.attach(getName() + ".leftHand.majeure", getName() + ".leftHand.majeure", getName() + ".leftHand.majeure2",
				getName() + ".leftHand.majeure3");
		jme.setRotation(getName() + ".leftHand.majeure", "x");
		jme.setRotation(getName() + ".leftHand.majeure2", "x");
		jme.setRotation(getName() + ".leftHand.majeure3", "x");

		jme.attach(getName() + ".leftHand.ringFinger", getName() + ".leftHand.ringFinger",
				getName() + ".leftHand.ringFinger2", getName() + ".leftHand.ringFinger3");
		jme.setRotation(getName() + ".leftHand.ringFinger", "x");
		jme.setRotation(getName() + ".leftHand.ringFinger2", "x");
		jme.setRotation(getName() + ".leftHand.ringFinger3", "x");

		jme.attach(getName() + ".leftHand.pinky", getName() + ".leftHand.pinky", getName() + ".leftHand.pinky2",
				getName() + ".leftHand.pinky3");
		jme.setRotation(getName() + ".leftHand.pinky", "x");
		jme.setRotation(getName() + ".leftHand.pinky2", "x");
		jme.setRotation(getName() + ".leftHand.pinky3", "x");

		// left hand mapping complexities of the fingers
		jme.setMapper(getName() + ".leftHand.index", 0, 180, -110, -179);
		jme.setMapper(getName() + ".leftHand.index2", 0, 180, -110, -179);
		jme.setMapper(getName() + ".leftHand.index3", 0, 180, -110, -179);

		jme.setMapper(getName() + ".leftHand.majeure", 0, 180, -110, -179);
		jme.setMapper(getName() + ".leftHand.majeure2", 0, 180, -110, -179);
		jme.setMapper(getName() + ".leftHand.majeure3", 0, 180, -110, -179);

		jme.setMapper(getName() + ".leftHand.ringFinger", 0, 180, -110, -179);
		jme.setMapper(getName() + ".leftHand.ringFinger2", 0, 180, -110, -179);
		jme.setMapper(getName() + ".leftHand.ringFinger3", 0, 180, -110, -179);

		jme.setMapper(getName() + ".leftHand.pinky", 0, 180, -110, -179);
		jme.setMapper(getName() + ".leftHand.pinky2", 0, 180, -110, -179);
		jme.setMapper(getName() + ".leftHand.pinky3", 0, 180, -110, -179);

		jme.setMapper(getName() + ".leftHand.thumb1", 0, 180, -30, -100);
		jme.setMapper(getName() + ".leftHand.thumb2", 0, 180, 80, 20);
		jme.setMapper(getName() + ".leftHand.thumb3", 0, 180, 80, 20);

		// right hand

		jme.attach(getName() + ".rightHand.thumb", getName() + ".rightHand.thumb1", getName() + ".rightHand.thumb2",
				getName() + ".rightHand.thumb3");
		jme.setRotation(getName() + ".rightHand.thumb1", "y");
		jme.setRotation(getName() + ".rightHand.thumb2", "x");
		jme.setRotation(getName() + ".rightHand.thumb3", "x");

		jme.attach(getName() + ".rightHand.index", getName() + ".rightHand.index", getName() + ".rightHand.index2",
				getName() + ".rightHand.index3");
		jme.setRotation(getName() + ".rightHand.index", "x");
		jme.setRotation(getName() + ".rightHand.index2", "x");
		jme.setRotation(getName() + ".rightHand.index3", "x");

		jme.attach(getName() + ".rightHand.majeure", getName() + ".rightHand.majeure",
				getName() + ".rightHand.majeure2", getName() + ".rightHand.majeure3");
		jme.setRotation(getName() + ".rightHand.majeure", "x");
		jme.setRotation(getName() + ".rightHand.majeure2", "x");
		jme.setRotation(getName() + ".rightHand.majeure3", "x");

		jme.attach(getName() + ".rightHand.ringFinger", getName() + ".rightHand.ringFinger",
				getName() + ".rightHand.ringFinger2", getName() + ".rightHand.ringFinger3");
		jme.setRotation(getName() + ".rightHand.ringFinger", "x");
		jme.setRotation(getName() + ".rightHand.ringFinger2", "x");
		jme.setRotation(getName() + ".rightHand.ringFinger3", "x");

		jme.attach(getName() + ".rightHand.pinky", getName() + ".rightHand.pinky", getName() + ".rightHand.pinky2",
				getName() + ".rightHand.pinky3");
		jme.setRotation(getName() + ".rightHand.pinky", "x");
		jme.setRotation(getName() + ".rightHand.pinky2", "x");
		jme.setRotation(getName() + ".rightHand.pinky3", "x");

		jme.setMapper(getName() + ".rightHand.index", 0, 180, 65, -10);
		jme.setMapper(getName() + ".rightHand.index2", 0, 180, 70, -10);
		jme.setMapper(getName() + ".rightHand.index3", 0, 180, 70, -10);

		jme.setMapper(getName() + ".rightHand.majeure", 0, 180, 65, -10);
		jme.setMapper(getName() + ".rightHand.majeure2", 0, 180, 70, -10);
		jme.setMapper(getName() + ".rightHand.majeure3", 0, 180, 70, -10);

		jme.setMapper(getName() + ".rightHand.ringFinger", 0, 180, 65, -10);
		jme.setMapper(getName() + ".rightHand.ringFinger2", 0, 180, 70, -10);
		jme.setMapper(getName() + ".rightHand.ringFinger3", 0, 180, 70, -10);

		jme.setMapper(getName() + ".rightHand.pinky", 0, 180, 65, -10);
		jme.setMapper(getName() + ".rightHand.pinky2", 0, 180, 70, -10);
		jme.setMapper(getName() + ".rightHand.pinky3", 0, 180, 60, -10);

		jme.setMapper(getName() + ".rightHand.thumb1", 0, 180, 30, 110);
		jme.setMapper(getName() + ".rightHand.thumb2", 0, 180, -100, -150);
		jme.setMapper(getName() + ".rightHand.thumb3", 0, 180, -100, -160);

		// additional experimental mappings
		/*
		 * simulator.attach(getName() + ".leftHand.pinky", getName() +
		 * ".leftHand.index2"); simulator.attach(getName() + ".leftHand.thumb",
		 * getName() + ".leftHand.index3"); simulator.setRotation(getName() +
		 * ".leftHand.index2", "x"); simulator.setRotation(getName() +
		 * ".leftHand.index3", "x"); simulator.setMapper(getName() + ".leftHand.index",
		 * 0, 180, -90, -270); simulator.setMapper(getName() + ".leftHand.index2", 0,
		 * 180, -90, -270); simulator.setMapper(getName() + ".leftHand.index3", 0, 180,
		 * -90, -270);
		 */
		return jme;
	}

	public InMoov2Torso startTorso() {
		return startTorso(null);
	}

	public InMoov2Torso startTorso(String port) {
		if (torso == null) {
			speakBlocking(get("STARTINGTORSO"));
			isTorsoActivated = true;

			torso = (InMoov2Torso) startPeer("torso");

			if (port != null) {
				try {
					speakBlocking(port);
					Arduino left = (Arduino) startPeer("left");
					left.connect(port);
					left.attach(torso.lowStom);
					left.attach(torso.midStom);
					left.attach(torso.topStom);
				} catch (Exception e) {
					error(e);
				}
			}
		}
		return torso;
	}

	/**
	 * called with only port - will default with defaulted pins
	 * @param port
	 * @return
	 */
	public UltrasonicSensor startUltraSonicRight(String port) {
	  return startUltraSonicRight(port, 64, 63);
	}

	/**
	 * called explicitly with pin values
	 * @param port
	 * @param trigPin
	 * @param echoPin
	 * @return
	 */
	public UltrasonicSensor startUltraSonicRight(String port, int trigPin, int echoPin) {
	
		if (ultraSonicRight == null) {
			speakBlocking(get("STARTINGULTRASONIC"));
			isUltraSonicRightActivated = true;

			ultraSonicRight = (UltrasonicSensor) startPeer("ultraSonicRight");

			if (port != null) {
				try {
					speakBlocking(port);
					Arduino right = (Arduino) startPeer("right");
					right.connect(port);
					right.attach(ultraSonicRight, trigPin, echoPin);
				} catch (Exception e) {
					error(e);
				}
			}
		}
		return ultraSonicRight;
	}

	
	 public UltrasonicSensor startUltraSonicLeft(String port) {
	   return startUltraSonicLeft(port, 64, 63);
	 }

  public UltrasonicSensor startUltraSonicLeft(String port, int trigPin, int echoPin) {
		
		if (ultraSonicLeft == null) {
			speakBlocking(get("STARTINGULTRASONIC"));
			isUltraSonicLeftActivated = true;

			ultraSonicLeft = (UltrasonicSensor) startPeer("ultraSonicLeft");

			if (port != null) {
				try {
					speakBlocking(port);
					Arduino left = (Arduino) startPeer("left");
					left.connect(port);
					left.attach(ultraSonicLeft, trigPin, echoPin);
				} catch (Exception e) {
					error(e);
				}
			}
		}
		return ultraSonicLeft;
	}
	
	public Pir startPir(String port) {
		return startPir(port, 23);
	}

	public Pir startPir(String port, int pin) {
		
		if (pir == null) {
			speakBlocking(get("STARTINGPIR"));
			isPirActivated = true;

			pir = (Pir) startPeer("pir");

			if (port != null) {
				try {
					speakBlocking(port);
					Arduino right = (Arduino) startPeer("right");
					right.connect(port);
					right.attach(pir, pin);
				} catch (Exception e) {
					error(e);
				}
			}
		}
		return pir;
	}
	
	public ServoMixer startServoMixer() {

               servomixer = (ServoMixer) startPeer("servomixer");
               isServoMixerActivated = true;

               speakBlocking(get("STARTINGSERVOMIXER"));
               broadcastState();
               return servomixer;
        }

	public void stop() {
		if (head != null) {
			head.stop();
		}
		if (rightHand != null) {
			rightHand.stop();
		}
		if (leftHand != null) {
			leftHand.stop();
		}
		if (rightArm != null) {
			rightArm.stop();
		}
		if (leftArm != null) {
			leftArm.stop();
		}
		if (torso != null) {
			torso.stop();
		}
	}

	public void stopBrain() {
		speakBlocking(get("STOPCHATBOT"));
		releasePeer("brain");
		isBrainActivated = false;
	}
	
	public void stopHead() {
		speakBlocking(get("STOPHEAD"));
		releasePeer("head");
		isHeadActivated = false;
	}

	public void stopEar() {
		speakBlocking(get("STOPEAR"));
		releasePeer("ear");
		isEarActivated = false;
		broadcastState();
	}

	public void stopEye() {
		speakBlocking(get("STOPOPENCV"));
		isEyeActivated = false;
		releasePeer("eye");
	}

	public void stopGesture() {
		Python p = (Python) Runtime.getService("python");
		p.stop();
	}

	public void stopLeftArm() {
		speakBlocking(get("STOPLEFTARM"));
		releasePeer("leftArm");
		isLeftArmActivated = false;
	}

	public void stopLeftHand() {
		speakBlocking(get("STOPLEFTHAND"));
		releasePeer("leftHand");
		isLeftHandActivated = false;
	}

	public void stopMouth() {
		speakBlocking(get("STOPMOUTH"));
		releasePeer("mouth");
		// TODO - potentially you could set the field to null in releasePeer
		mouth = null;
		isMouthActivated = false;
	}

	public void stopRightArm() {
		speakBlocking(get("STOPRIGHTARM"));
		releasePeer("rightArm");
		isRightArmActivated = false;
	}

	public void stopRightHand() {
		speakBlocking(get("STOPRIGHTHAND"));
		releasePeer("rightHand");
		isRightHandActivated = false;
	}

	public void stopTorso() {
		speakBlocking(get("STOPTORSO"));
		releasePeer("torso");
		isTorsoActivated = false;
	}

	public void stopSimulator() {
		speakBlocking(get("STOPVIRTUAL"));
		releasePeer("simulator");
		jme = null;
		isSimulatorActivated = false;
	}

	public void stopUltraSonicRight() {
		speakBlocking(get("STOPULTRASONIC"));
		releasePeer("ultraSonicRight");
		isUltraSonicRightActivated = false;
	}

	public void stopUltraSonicLeft() {
		speakBlocking(get("STOPULTRASONIC"));
		releasePeer("ultraSonicLeft");
		isUltraSonicLeftActivated = false;
	}
	
	public void stopPir() {
		speakBlocking(get("STOPPIR"));
		releasePeer("pir");
		isPirActivated = false;
	}
	
        public void stopServoMixer() {
                speakBlocking(get("STOPSERVOMIXER"));
                releasePeer("servomixer");
                isServoMixerActivated = false;
        }

	public void waitTargetPos() {
		if (head != null) {
			head.waitTargetPos();
		}
		if (leftArm != null) {
			leftArm.waitTargetPos();
		}
		if (rightArm != null) {
			rightArm.waitTargetPos();
		}
		if (leftHand != null) {
			leftHand.waitTargetPos();
		}
		if (rightHand != null) {
			rightHand.waitTargetPos();
		}
		if (torso != null) {
			torso.waitTargetPos();
		}
	}

}

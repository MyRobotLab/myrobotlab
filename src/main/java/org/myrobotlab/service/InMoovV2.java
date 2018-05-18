package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.repo.Category;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;

/**
 * InMoov V2 - The InMoov Service ( WIP ).
 * 
 * The InMoov service allows control of the InMoov robot. This robot was created
 * by Gael Langevin. It's an open source 3D printable robot. All of the parts
 * and instructions to build are on http://www.inmoov.fr/). InMoov is a
 * composite of servos, Arduinos, microphone, camera, kinect and computer. The
 * InMoov service is composed of many other services, and allows easy
 * initialization and control of these sub systems.
 *
 */
public class InMoovV2 extends Service {

	private static final long serialVersionUID = 1L;

	transient private static Runtime myRuntime = null; //  
	transient private static ServiceData serviceData = null; // = myRuntime.getServiceData();

	// interfaces declaration needed by InMooov
	transient public SpeechSynthesis mouth;
	transient public SpeechRecognizer ear;

	/**
	 * this is for gui combo content
	 */
	public static HashMap<Integer, String[]> languages = new HashMap<Integer, String[]>();
	public static List<String> speechEngines = new ArrayList<String>();
	public static List<String> earEngines = new ArrayList<String>();

	// ---------------------------------------------------------------
	// Store parameters inside json related service
	// ---------------------------------------------------------------

	Integer language;
	boolean mute;
	String speechEngine;
	String earEngine;

	// ---------------------------------------------------------------
	// end of config
	// ---------------------------------------------------------------
	
	
	public InMoovV2(String n) {
		super(n);
		if (myRuntime == null) {
			myRuntime = (Runtime) Runtime.getInstance();;
		}
		
		if (serviceData == null) {
			serviceData = myRuntime.getServiceData();
		}
	}


	public static void main(String[] args) {
		try {
			LoggingFactory.init(Level.INFO);

			String leftPort = "COM3";
			String rightPort = "COM4";

			VirtualArduino vleft = (VirtualArduino) Runtime.start("vleft", "VirtualArduino");
			VirtualArduino vright = (VirtualArduino) Runtime.start("vright", "VirtualArduino");
			Python python = (Python) Runtime.start("python", "Python");
			vleft.connect("COM3");
			vright.connect("COM4");
			Runtime.start("gui", "SwingGui");

			InMoovV2 inMoov = (InMoovV2) Runtime.start("inMoov", "InMoovV2");
			inMoov.startMouth();

		} catch (Exception e) {
			log.error("main threw", e);
		}
	}

	@Override
	public void startService() {
		super.startService();
		// feed possible languages : https://github.com/MyRobotLab/inmoov/issues/151
		languages.put(0, new String[] { "en", "English - United States" });
		languages.put(1, new String[] { "fr", "French - France" });
		languages.put(2, new String[] { "es", "Spanish - Spain" });
		languages.put(3, new String[] { "nl", "Dutch - The Netherlands" });
		languages.put(4, new String[] { "de", "German - Germany" });
		languages.put(5, new String[] { "ru", "Russian - Russia" });
		languages.put(6, new String[] { "hi", "Hindi - India" });
		languages.put(7, new String[] { "it", "Italian - Italy" });
		InMoovV2.speechEngines = getServicesFromCategory("speech");
		InMoovV2.earEngines = getServicesFromCategory("speech recognition");
		this.language = getLanguage();
		this.speechEngine = getSpeechEngine();
		this.earEngine = getEarEngine();
	}

	static public ServiceType getMetaData() {

		ServiceType meta = new ServiceType(InMoovV2.class);
		meta.setAvailable(false);
		return meta;
	}

	// list services from meta category (pasted from RuntimeGui.java)
	public List<String> getServicesFromCategory(final String filter) {
		List<String> servicesFromCategory = new ArrayList<String>();
		Category category = serviceData.getCategory(filter);
		HashSet<String> filtered = null;
		filtered = new HashSet<String>();
		ArrayList<String> f = category.serviceTypes;
		for (int i = 0; i < f.size(); ++i) {
			filtered.add(f.get(i));
		}

		// populate with serviceData
		List<ServiceType> possibleService = serviceData.getServiceTypes();
		for (int i = 0; i < possibleService.size(); ++i) {
			ServiceType serviceType = possibleService.get(i);
			if (filtered.contains(serviceType.getName())) {
				if (serviceType.isAvailable()) {
					// log.debug("serviceType : " + serviceType.getName());
					servicesFromCategory.add(serviceType.getSimpleName());
				}
			}

		}
		return servicesFromCategory;
	}

	// ---------------------------------------------------------------
	// start core interfaces
	// ---------------------------------------------------------------
	/**
	 * Start InMoov speech engine also called "mouth"
	 * 
	 * @return started SpeechSynthesis service
	 */
	public SpeechSynthesis startMouth() throws Exception {
		mouth = (SpeechSynthesis) Runtime.start(this.getIntanceName() + ".mouth", speechEngine);
		broadcastState();
		// speakBlocking("starting mouth");
		return mouth;
	}

	/**
	 * Start InMoov ear engine
	 * 
	 * @return started SpeechRecognizer service
	 */
	public SpeechRecognizer startEar() throws Exception {
		ear = (SpeechRecognizer) Runtime.create(this.getIntanceName() + ".ear", earEngine);
		ear.setLanguage(languages.get(language)[0]);
		ear = (SpeechRecognizer) Runtime.start(this.getIntanceName() + ".ear", earEngine);
		broadcastState();
		// speakBlocking("starting mouth");
		return ear;
	}
	// ---------------------------------------------------------------
	// end core interfaces
	// ---------------------------------------------------------------

	// ---------------------------------------------------------------
	// setters & getters
	// ---------------------------------------------------------------

	public String getSpeechEngine() {
		if (this.speechEngine == null) {
			setSpeechEngine("MarySpeech");
		}
		return speechEngine;
	}

	public void setSpeechEngine(String speechEngine) {
		this.speechEngine = speechEngine;
		info("Set InMoov speech engine : %s", speechEngine);
		broadcastState();
	}

	public String getEarEngine() {
		if (this.earEngine == null) {
			setEarEngine("WebkitSpeechRecognition");
		}
		return earEngine;
	}

	public void setEarEngine(String earEngine) {
		this.earEngine = earEngine;
		info("Set InMoov ear engine : %s", earEngine);
		broadcastState();
	}

	/**
	 * @return the mute startup state ( InMoov vocal startup actions )
	 */
	public Boolean getMute() {
		return mute;
	}

	/**
	 * @param mute
	 *            the startup mute state to set ( InMoov vocal startup actions )
	 */
	public void setMute(Boolean mute) {
		this.mute = mute;
		info("Set InMoov to mute at startup : %s", mute);
		broadcastState();
	}

	/**
	 * set language for InMoov service used by chatbot + ear + mouth TODO : choose a
	 * language code format instead of index
	 * 
	 * @param i
	 *            - format : en / fr etc ...
	 */
	public void setLanguage(int i) {
		this.language = i;
		info("Set InMoov language to %s", languages.get(i)[0]);
		broadcastState();
	}

	/**
	 * get current language used by InMoov service
	 */
	public Integer getLanguage() {
		if (this.language == null) {
			setLanguage(0);
		}
		return this.language;
	}

	// ---------------------------------------------------------------
	// end setter & getter
	// ---------------------------------------------------------------
}

package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.repo.GitHub;
import org.myrobotlab.framework.repo.Repo;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.framework.repo.ServiceType;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.io.FindFile;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

/**
 * Minimal dependency service for rigorous testing
 * 
 * @author GroG
 * 
 *         TODO - grab and report all missing Service Pages & all missing Python
 *         scripts !
 *
 */
public class Test extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Test.class);

	// state information
	Date now = new Date();
	transient Set<Thread> threads = null;
	transient Set<File> files = new HashSet<File>();

	// python
	ArrayList<String> neededPythonScripts = new ArrayList<String>();
	ArrayList<String> pythonScriptsWithNoServiceType = new ArrayList<String>();
	ArrayList<String> failedPythonScripts = new ArrayList<String>();
	ArrayList<String> passedPythonScripts = new ArrayList<String>();
	HashSet<String> skippedPythonScript = new HashSet<String>();
	

	// thread blocking
	Object lock = new Object();

	List<Status> status = new ArrayList<Status>();

	TreeMap<String, String> pythonScripts = null;

	LinkedBlockingQueue<Object> data = new LinkedBlockingQueue<Object>();

	public static void logThreadNames() {

		Set<Thread> threads = Runtime.getThreads();
		String[] tn = new String[threads.size()];
		int x = 0;
		for (Thread thread : threads) {
			tn[x] = thread.getName();
			++x;
		}

		Arrays.sort(tn);
		log.warn(CodecUtils.toJson(tn));
		/*
		 * for (int i = 0; i < t.length; ++i){ log.warn(t[i]); }
		 */
	}

	/*
	 * careful with using other services - as they incur dependencies
	 * 
	 * public Status pythonTest() { Python python = (Python)
	 * Runtime.start("python", "Python"); Serial uart99 = (Serial)
	 * Runtime.start("uart99", "Serial"); // take inventory of currently running
	 * services HashSet<String> keepMeRunning = new HashSet<String>();
	 * 
	 * VirtualSerialPort.createNullModemCable("UART99", "COM12");
	 * 
	 * List<ServiceInterface> list = Runtime.getServices(); for (int j = 0; j <
	 * list.size(); ++j) { ServiceInterface si = list.get(j);
	 * keepMeRunning.add(si.getName()); }
	 * 
	 * String[] serviceTypeNames = Runtime.getInstance().getServiceTypeNames();
	 * Status status = Status.info("subTest");
	 * 
	 * status.add(Status.info("will test %d services",
	 * serviceTypeNames.length));
	 * 
	 * for (int i = 0; i < serviceTypeNames.length; ++i) { String fullName =
	 * serviceTypeNames[i]; String shortName =
	 * fullName.substring(fullName.lastIndexOf(".") + 1);
	 * 
	 * String py =
	 * FileIO.resourceToString(String.format("Python/examples/%s.py",
	 * shortName));
	 * 
	 * if (py == null || py.length() == 0) { status.addError(
	 * "%s.py does not exist", shortName); } else { uart99.connect("UART99");
	 * uart99.recordRX(String.format("%s.rx", shortName)); // FIXME // FILENAME
	 * // OVERLOAD python.exec(py); uart99.stopRecording(); // check rx file
	 * against saved data }
	 * 
	 * // get python errors !
	 * 
	 * // clean services Runtime.releaseAllServicesExcept(keepMeRunning); }
	 * 
	 * return null;
	 * 
	 * }
	 */

	/*
	 * 
	 * public void testPythonScripts() { try {
	 * 
	 * Python python = (Python) Runtime.start("python", "Python"); // String
	 * script; ArrayList<File> list =
	 * FileIO.listInternalContents("/resource/Python/examples");
	 * 
	 * Runtime.createAndStart("gui", "GUIService"); python = (Python)
	 * startPeer("python"); // InMoov i01 = (InMoov)
	 * Runtime.createAndStart("i01", "InMoov");
	 * 
	 * HashSet<String> keepMeRunning = new HashSet<String>(Arrays.asList("i01",
	 * "gui", "runtime", "python", getName()));
	 * 
	 * for (int i = 0; i < list.size(); ++i) { String r = list.get(i).getName();
	 * if (r.startsWith("InMoov2")) { warn("testing script %s", r); String
	 * script = FileIO.resourceToString(String.format("Python/examples/%s", r));
	 * python.exec(script); log.info("here"); // i01.detach();
	 * Runtime.releaseAllServicesExcept(keepMeRunning); } }
	 * 
	 * } catch (Exception e) { Logging.logException(e); } }
	 * 
	 * public void testInMoovPythonScripts() { try {
	 * 
	 * Python python = (Python) Runtime.start("python", "Python"); // String
	 * script; ArrayList<File> list =
	 * FileIO.listInternalContents("/resource/Python/examples");
	 * 
	 * Runtime.createAndStart("gui", "GUIService"); python = (Python)
	 * startPeer("python"); // InMoov i01 = (InMoov)
	 * Runtime.createAndStart("i01", "InMoov");
	 * 
	 * HashSet<String> keepMeRunning = new HashSet<String>(Arrays.asList("i01",
	 * "gui", "runtime", "python", getName()));
	 * 
	 * for (int i = 0; i < list.size(); ++i) { String r = list.get(i).getName();
	 * if (r.startsWith("InMoov2")) { warn("testing script %s", r); String
	 * script = FileIO.resourceToString(String.format("Python/examples/%s", r));
	 * python.exec(script); log.info("here"); // i01.detach();
	 * Runtime.releaseAllServicesExcept(keepMeRunning); } }
	 * 
	 * } catch (Exception e) { Logging.logException(e); } }
	 */
	// very good - dynamicly subscribing to other service's
	// published errors
	// step 1 subscribe to runtimes registered event
	// step 2 in any registered -
	// step 3 - fix up - so that state is handled (not just "error")

	public Test(String n) {
		super(n);
	}

	/*
	 * public Status junit(){
	 * 
	 * 
	 * Status status = Status.info("starting %s %s junit", getName(),
	 * getType()); File buildFile = new File("build.xml"); Project p = new
	 * Project(); p.setUserProperty("ant.file", buildFile.getAbsolutePath());
	 * p.init(); ProjectHelper helper = ProjectHelper.getProjectHelper();
	 * p.addReference("ant.projectHelper", helper); helper.parse(p, buildFile);
	 * p.executeTarget(p.getDefaultTarget());
	 * 
	 * 
	 * }
	 */

	public Status arduinoTest() {
		Status status = Status.info("testing arduino");

		Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");

		return status;
	}

	public void exit(List<Status> status) {
		try {
			// check against current state for
			// NOT NEEDED Regular save file - since Agent is process.waitFor
			// FIXME - append states to file
			FileIO.savePartFile(new File("test.json"), CodecUtils.toJson(status).getBytes());
			// Runtime.releaseAll();
			// TODO - should be all clean - if not someone left threads open -
			// report them
			// big hammer
		} catch (Exception e) {
			Logging.logError(e);
		}
		System.exit(0);
	}

	/**
	 * used to get state of the current service and runtime - so that the
	 * environment and final system can be cleaned to an original "base" state
	 */
	public void getState() {
		try {
			threads = Thread.getAllStackTraces().keySet();
			List<File> f = FindFile.find("libraries", ".*");
			for (int i = 0; i < f.size(); ++i) {
				files.add(f.get(i));
			}
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	/**
	 * call-back from service under testing to route errors to this service...
	 */
	public void onError(String errorMsg) {
		if (status != null) {
			status.add(Status.error(errorMsg));
		}
	}

	public void registered(ServiceInterface sw) {

		subscribe(sw.getName(), "publishError");
	}

	// TODO - do all forms of serialization - binary json xml
	public Status serializeTest(ServiceInterface s) {
		log.info("serializeTest {}", s.getName(), s.getSimpleName());
		String name = s.getName();

		// multiple formats binary json xml
		Status status = Status.info("serializeTest for %s", name);

		try {

			// TODO put in encoder
			ByteArrayOutputStream fos = null;
			ObjectOutputStream out = null;
			fos = new ByteArrayOutputStream();
			out = new ObjectOutputStream(fos);
			out.writeObject(s);
			fos.flush();
			out.close();

			// json encoding
			CodecUtils.toJson(s);

			// TODO JAXB xml - since it comes with java 7

		} catch (Exception ex) {
			return Status.error(ex);
		}

		return null;
	}

	public Object subscribe(Object inData) {
		log.info("subscribe has received data");
		log.info(String.format("Test.subscribed received %s", inData));
		data.add(inData);
		return inData;
	}

	public Status test() {
		// we are started so .. we'll use the big hammer at the end
		Status status = Status.info("========TESTING=============");
		log.info("===========INFO TESTING========");
		log.info(String.format("TEST PID = %s", Runtime.getPid()));
		// big hammer
		System.exit(0);
		return status;
	}

	/**
	 * The outer level of all tests on a per Service basis Environment is
	 * expected to be prepared correctly by an Agent. This method will test the
	 * heck out of a single service and save the results in a partFile
	 * 
	 * @param serviceType
	 * @return
	 */
	public List<Status> test(String serviceType) {

		List<Status> ret = new ArrayList<Status>();

		ret.add(Status.info("==== testing %s ====", serviceType));

		try {

			// install of depencencies and environment is done by
			// the Agent smith (thompson)

			ServiceInterface s = null;

			// create test
			try {
				s = Runtime.create(serviceType, serviceType);
			} catch (Exception e) {
				ret.add(Status.error(e));
				exit(status);
			}

			// start test
			if (s == null) {
				status.add(Status.info("could not create %s", serviceType));
				exit(status);
			}

			// add error route - for call backs
			subscribe(s.getName(), "publishError", getName(), "onError");

			try {
				s.startService();
				// FIXME - s.waitForStart();
				// Thread.sleep(500);
			} catch (Exception e) {
				status.add(Status.error(e));
				exit(status);
			}

			status.add(serializeTest(s));

			// FIXME - JUNIT TESTS !!!!
			// status.add(s.test()); - can not do this
			// logThreadNames();

			// assume installed - Agent's job

			// serialize test

			// python test

			// release
			try {
				status.add(Status.info("releasePeers"));
				if (s.hasPeers()) {
					s.releasePeers();
				}
			} catch (Exception e) {
				status.add(Status.error(e));
			}

			try {
				status.add(Status.info("releaseService"));
				s.releaseService();
			} catch (Exception e) {
				status.add(Status.error(e));
			}

			log.info("exiting environment");

		} catch (Exception e) {
			status.add(Status.error(e));
		}

		exit(status);
		return status;
	}

	/**
	 * this can not be used to test environment
	 * 
	 * @return
	 */
	public List<Status> testAll() {

		List<Status> ret = new ArrayList<Status>();
		String[] serviceTypeNames = Runtime.getInstance().getServiceTypeNames();
		Status status = Status.info("subTest");

		// status.add(Status.info("will test %d services",
		// serviceTypeNames.length));

		for (int i = 0; i < serviceTypeNames.length; ++i) {
			String fullName = serviceTypeNames[i];
			ret.addAll((test(fullName)));
		}

		return ret;
	}

	public void testServiceScripts() {
		// get download zip

		// uncompress locally

		// test - instrumentation for
	}

	public Status verifyServicePageScript(String serviceType) {
		Status status = new Status("starting");
		return status;
	}

	// save / load test !

	public List<Status> verifyServicePageScripts() {
		List<Status> ret = new ArrayList<Status>();
		Repo repo = Runtime.getInstance().getRepo();
		ServiceData serviceData = ServiceData.getLocalInstance();
		ArrayList<ServiceType> serviceTypes = serviceData.getServiceTypes();

		Status status = Status.info("serviceTest will test %d services", serviceTypes.size());
		long startTime = System.currentTimeMillis();
		ret.add(info("startTime", "%d", startTime));

		for (int i = 0; i < serviceTypes.size(); ++i) {
			ServiceType serviceType = serviceTypes.get(i);
			Status retStatus = verifyServicePageScript(serviceType.getName());
			if (retStatus.isError()) {
				ret.add(retStatus);
			}
		}

		return ret;
	}

	/**
	 * This static method returns all the details of the class without it having
	 * to be constructed. It has description, categories, dependencies, and peer
	 * definitions.
	 * 
	 * @return ServiceType - returns all the data
	 * 
	 *         FIXME - todo - make junit html report TODO - simple install start
	 *         release - check for rogue threads
	 * 
	 */
	static public ServiceType getMetaData() {

		ServiceType meta = new ServiceType(Test.class.getCanonicalName());
		meta.addDescription("Testing service");
		meta.addCategory("testing", "framework");
		return meta;
	}

	public void startAndReleaseTest(String serviceType) {

	}

	public void onFinishedPythonScript(String result) {
		log.info("DONE !");
		log.info("onFinishedPythonScript - {}", result);
		synchronized (lock){
			lock.notifyAll();
		}
	}

	public Map<String, String> getPyRobotLabServiceScripts() throws Exception {

		if (pythonScripts == null) {
			neededPythonScripts.clear();
			pythonScriptsWithNoServiceType.clear();

			HashSet<String> serviceTypes = new HashSet<String>();

			pythonScripts = new TreeMap<String, String>();
			ServiceData sd = ServiceData.getLocalInstance();
			List<ServiceType> sts = sd.getServiceTypes();
			for (int i = 0; i < sts.size(); ++i) {
				ServiceType st = sts.get(i);
				serviceTypes.add(st.getSimpleName());
				String script = GitHub.getPyRobotLabScript(st.getSimpleName());
				if (script != null) {
					pythonScripts.put(st.getSimpleName(), script);

				} else {
					log.info("{}<br>", st.getSimpleName());
					neededPythonScripts.add(String.format("%s<br>", st.getSimpleName()));
					StringBuffer t = new StringBuffer("# start the service\n");
					String lowercase = st.getSimpleName().toLowerCase();
					t.append(String.format("%s = Runtime.start(\"%s\",\"%s\")", lowercase, lowercase, st.getSimpleName()));
					FileIO.toFile(new File(String.format("%s.py", st.getSimpleName())), t.toString().getBytes());
				}
			}
			Set<String> gitHubServiceScripts = GitHub.getServiceScriptNames();
			for (String key : gitHubServiceScripts) {
				String serviceName = key.substring(0, key.lastIndexOf("."));
				if (!serviceTypes.contains(serviceName)) {
					pythonScriptsWithNoServiceType.add(key);
				}
			}

			log.info("needed scripts - service type found but no script {}", neededPythonScripts);
			log.info("remove scripts - script found by no service type {}", pythonScriptsWithNoServiceType);

		}
		return pythonScripts;
	}

	public List<String> getServicesWithOutScripts() {
		ArrayList<String> ret = new ArrayList<String>();

		return ret;
	}

	/**
	 * Gets all the pyrobotlab/service/scripts and does some basic testing. This
	 * method also finds all script not associated with active services (to be
	 * removed). And all services which do not have scripts (to be added) It
	 * runs in the same process as Python and the expectation is the Agent (with
	 * the help of the Test service) has created an environment where the
	 * service to be tested has all its depedencies
	 * 
	 * @throws Exception
	 * 
	 * FIXME - need to change to testPythonScript(serviceName) .. because
	 * only 1 will be run in a 'clean' environment ...
	 * 
	 * FIXME - structured logging back to self to generate report
	 */
	public void testPythonScripts() throws Exception {
		// get scripts
		getPyRobotLabServiceScripts();
		failedPythonScripts.clear();

		// check if python is installed
		// start python
		Python python = (Python) Runtime.start("python", "Python");
		
		
		skippedPythonScript.add("AcapelaSpeech");  // currently tooooo long.....
		skippedPythonScript.add("Adafruit16CServoDriver"); 
		skippedPythonScript.add("AdafruitMotorShield"); 
		
		
		for (String serviceName : pythonScripts.keySet()) {
			String script = pythonScripts.get(serviceName);
			log.info("TESTING SCRIPT {} - quiet on the set please...", serviceName);
			
			if (skippedPythonScript.contains(serviceName)){
				log.info("SKIPPING {} ....", serviceName);
				continue;
			}

			// challenge #1
			// I would prefer to be in the same
			// process as python when things are exectuted
			// therefore "something else" or Test in a different
			// process needs to execute Test with 'installed' components

			// challenge #2 execute blocking ? or is there a callback 'from the
			// script' ?
			// append exit ?
			// append test done callback !!!

			// add callback at the end of script
			StringBuffer callback = new StringBuffer();
			callback.append("\n");
			callback.append("sleep(1) # 1 second to cool down");
			callback.append("\n\n");
			// callback.append("import Test from org.myrobotlab.service");
			callback.append(String.format("%s.onFinishedPythonScript('done!')\n", getName()));

			// by default - python will create a new thread
			// to execute the script
			python.exec(script + callback);//, true, true);

			// script has at maximum 1 minute to return
			synchronized (lock) {
				long ts = System.currentTimeMillis();
				lock.wait(60000);
				if (System.currentTimeMillis() - ts >= 60000) {
					log.error("script {} FAILED !", serviceName);
					failedPythonScripts.add(serviceName);
				} else {
					log.info("script {} PASSED !", serviceName);
					passedPythonScripts.add(serviceName);
				}
			}

			// max execution time ??? - then error

			log.info("inspect python errors (syntax) + java errors");
		}
		
		log.info("TESTING COMPLETED");

	}

	// TODO - subscribe to registered --> generates subscription to
	// publishState() - filter on Errors
	// FIXME - FILE COMMUNICATION !!!!
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		try {

			Test test = (Test)Runtime.start("test", "Test");
			// Runtime.start("cli", "Cli");
			Agent agent = (Agent)Runtime.start("agent", "Agent");
			String[] cmdline = new String[]{"-fromAgent", "-service", "guiservice", "GUIService"};
			agent.spawn(cmdline);
			
			// requirements:
			// run all junit tests
			// clear repo
			// install all dependencies
			// run python
			// load test
			// queue speed test
			// "use Agent's spawn"???
			// Repo repo = Repo.getLocalInstance();

			// repo.clearRepo();
			// dirty clean :)
			// repo.clearLibraries();
			// repo.clearServiceData();
			// repo.install(serviceType);
			/*
			
			Test test = (Test) Runtime.start("test", "Test");
			test.getPyRobotLabServiceScripts();
			test.getState();
			test.testPythonScripts();
			log.info("here");
			
			*/
			
			log.info("here");

		} catch (Exception e) {
			Logging.logError(e);
		}

	}

}

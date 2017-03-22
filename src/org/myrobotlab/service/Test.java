package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.client.ClientProtocolException;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.repo.GitHub;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.io.FindFile;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.myrobotlab.service.interfaces.StatusListener;
import org.slf4j.Logger;

/**
 * Minimal dependency service for rigorous testing
 * 
 * @author GroG
 * 
 *         TODO - grab and report all missing Service Pages & all missing Python
 *         scripts !
 * 
 *         TODO - install create start stop release test TODO - serialization
 *         json + native test TODO - run Python & JavaScript tests - last method
 *         appended is a callback
 *
 */
public class Test extends Service implements StatusListener {

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
	// transient Object lock = new Object();
	transient StatusLock lock = new StatusLock();

	List<Status> status = new ArrayList<Status>();

	transient TreeMap<String, String> pythonScripts = null;

	transient LinkedBlockingQueue<Object> data = new LinkedBlockingQueue<Object>();

	TestMatrix matrix = new TestMatrix();

	public static class Progress implements Serializable {
		private static final long serialVersionUID = 1L;
		String currentActivity;
		int percentDone;
		public int testsDone;
		public int errors;
		public int errorPercentage;
		public int successes;
		public int successPercentage;
		public int totalTests;

		public void process(Status status) {
			testsDone++;
			percentDone = testsDone * 100 / totalTests;

			if (status.isError()) {
				errors++;
				errorPercentage = errors * 100 / testsDone;
			} else {
				successes++;
				successPercentage = successes * 100 / testsDone;
			}
		}
	}

	public static class TestMatrix implements Serializable {
		private static final long serialVersionUID = 1L;
		Progress currentProgress = new Progress();
		Date lastTestDt;
		long lastTestDurationMs;
		long totalDuration;
		public boolean isRunning = false;

		/**
		 * this will be important when we start posting test matrices from
		 * different platforms
		 */
		Platform platform = Platform.getLocalInstance();

		HashSet<String> testsToRun = new HashSet<String>();
		HashSet<String> servicesToTest = new HashSet<String>();
		HashSet<String> services = new HashSet<String>();
		Map<String, TestResults> results = new TreeMap<String, TestResults>();

	}

	public static class TestResults implements Serializable {
		private static final long serialVersionUID = 1L;
		String fullTypeName;
		String simpleName;
		ServiceType type;
		TreeMap<String, TestResult> results = new TreeMap<String, TestResult>();
	}

	public static class TestResult implements Serializable {
		private static final long serialVersionUID = 1L;

		String test;
		long startTime;
		long endTime;
		Status status;

		public String link;

		public TestResult(String testName) {
			this.test = testName;
			this.link = testName;
			this.startTime = System.currentTimeMillis();
		}
	}

	public static class StatusLock {
		public Status status;
	}

	public static void logThreadNames() {

		Set<Thread> threads = Runtime.getThreads();
		String[] tn = new String[threads.size()];
		int x = 0;
		for (Thread thread : threads) {
			tn[x] = thread.getName();
			++x;
		}

		Arrays.sort(tn);
		// log.warn(CodecUtils.toJson(tn));
		/*
		 * for (int i = 0; i < t.length; ++i){ log.warn(t[i]); }
		 */
	}

	public Progress publishProgress(Progress progress) {
		return progress;
	}

	public void startService() {
		super.startService();
		createTestPlan();
	}

	TestMatrix createTestPlan() {

		ServiceData serviceData = ServiceData.getLocalInstance();
		ArrayList<ServiceType> types = serviceData.getServiceTypes();
		for (int i = 0; i < types.size(); ++i) {
			ServiceType type = types.get(i);
			if (!type.isAvailable()){
				continue;
			}
			TestResults results = new TestResults();
			String n = type.getName();
			results.fullTypeName = type.getName();

			results.simpleName = n.substring(n.lastIndexOf(".") + 1);
			results.type = type;
			matrix.results.put(results.simpleName, results);
			matrix.servicesToTest.add(results.simpleName);
			matrix.services.add(results.simpleName);
		}

		broadcastState();
		return matrix;
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
	 * Runtime.createAndStart("gui", "SwingGui"); python = (Python)
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
	 * Runtime.createAndStart("gui", "SwingGui"); python = (Python)
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

		Runtime.start("arduino", "Arduino");

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

	ArrayList<String> serializationTestFailures = new ArrayList<String>();

	// TODO - do all forms of serialization - binary json xml
	public Status serializeTest(ServiceInterface s) {
		log.info("serializeTest {}", s.getName(), s.getSimpleName());
		String name = s.getName();
		if (name == null) {
			log.warn("Name was null on serialize test?!?");
		}
		// multiple formats binary json xml
		// Status status = Status.info("serializeTest for %s", name);

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
			// serializationTestFailures
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

	// FIXME - initially i was trying to control this through an Agent
	// as it would then be possible to have a 'clean' repo without the
	// dependencies of the Test(er) mixing with the Tested target
	// out-of-process testing can be a challenge - even more so through std:io
	// but with UDP control this should be significantly easier

	// in the interim we have lots of other testing we can do - junit, scripts,
	// invoking scripts, service pages .. yatta yatta

	// test is the main interface to test everything
	// failures need to be collected & options (like junit) to halt on error
	// or continue and report

	/**
	 * need to do a type conversion here... in JS land there is no HashSet
	 * <String> only List & HashMap types
	 * 
	 * @param servicesToTest
	 */
	public void test(List<String> servicesToTest, List<String> testsToRun) {
		// clear results ???
		matrix.servicesToTest.clear();
		matrix.testsToRun.clear();
		for (int i = 0; i < servicesToTest.size(); ++i) {
			matrix.servicesToTest.add(servicesToTest.get(i));
		}
		for (int i = 0; i < testsToRun.size(); ++i) {
			matrix.testsToRun.add(testsToRun.get(i));
		}

		// test();
		Tester tester = new Tester(this);
		// TODO: remove or make this do something.
		log.info("Tester: {}", tester);
	}

	public static class Tester extends Thread {
		transient Test test;

		public Tester(Test test) {
			super("tester");
			this.test = test;
			start();
		}

		public void run() {
			test.test();
		}
	}

	synchronized public void test() {
		matrix.isRunning = true;
		// we are started so .. we'll use the big hammer at the end
		/*
		 * Status status = Status.info("========TESTING=============");
		 * log.info( "===========INFO TESTING========"); log.info(String.format(
		 * "TEST Pid = %s", Runtime.getPid())); // big hammer System.exit(0);
		 * return status;
		 */

		// do the cross product of
		// tests to run over services selected
		// update the matrix accordingly broadcast after every service x test

		// single method test(getMethod(String name))

		// small but powerfull test engine
		Progress progress = new Progress();
		matrix.currentProgress = progress;

		invoke("publishProgress", progress);

		int total = matrix.testsToRun.size() * matrix.servicesToTest.size();

		progress.totalTests = total;
		progress.percentDone = 0;
		progress.currentActivity = String.format("starting %d tests", total);

		for (String testName : matrix.testsToRun) {
			for (String name : matrix.servicesToTest) {
				TestResults results = matrix.results.get(name);

				String activity = String.format("test %s %s", testName, results.simpleName);
				log.info(activity);
				progress.currentActivity = activity;
				invoke("publishProgress", progress);

				// Create a new test result to hold the results for this test
				TestResult result = new TestResult(testName);
				// load it into the results
				results.results.put(testName, result);

				// do the TEST !!
				invoke(testName, testName, results);

				result.endTime = System.currentTimeMillis();

				progress.process(results.results.get(testName).status);

				broadcastState(); // admittedly a bit heavy handed

				activity = String.format("tested %s(%s)", testName, results.simpleName);
				invoke("publishProgress", progress);

			}

		}

		log.info("here");
	}

	public String[] getAllServiceNames() {
		ServiceData sd = ServiceData.getLocalInstance();
		return sd.getServiceTypeNames();
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
		meta.addCategory("testing");
		meta.addPeer("http", "HttpClient", "to interface with Service pages");
		meta.addPeer("python", "Python", "python to excercise python scripts");
		return meta;
	}

	public void startAndReleaseTest(String serviceType) {

	}

	public void onFinishedPythonScript(String result) {
		log.info("DONE !");
		log.info("onFinishedPythonScript - {}", result);
		synchronized (lock) {
			lock.notifyAll();
		}
	}

	// TODO - load python
	public TestResults PythonScriptExists(String testName, TestResults test) {

		TestResult result = test.results.get(testName);
		try {
			String name = test.simpleName;
			String script = GitHub.getPyRobotLabScript(name);

			String branch = Platform.getLocalInstance().getBranch();
			// https://github.com/MyRobotLab/pyrobotlab/edit/develop/service/AcapelaSpeech.py
			String url = String.format("https://github.com/MyRobotLab/pyrobotlab/edit/%s/service/%s.py", branch, name);
			result.link = String.format("<a href=\"%s\">%s</a>", url, result.test);

			if (script == null) {
				result.status = Status.error("script not found");

				ServiceData sd = ServiceData.getLocalInstance();
				ServiceType st = sd.getServiceType(test.fullTypeName);
				StringBuffer t = new StringBuffer();
				t.append("#########################################\n");
				t.append(String.format("# %s.py\n", name));
				t.append(String.format("# description: %s\n", st.getDescription()));
				t.append(String.format("# categories: %s\n", Arrays.toString(st.categories.toArray(new String[st.categories.size()]))));
				t.append(String.format("# possibly more info @: http://myrobotlab.org/service/%s\n", name));
				t.append("#########################################\n");
				t.append("# start the service\n");
				String lowercase = name.toLowerCase();
				t.append(String.format("%s = Runtime.start(\"%s\",\"%s\")", lowercase, lowercase, name));
				FileIO.toFile(new File(String.format("%s.py", name)), t.toString().getBytes());
			} else {
				result.status = Status.success();
			}

		} catch (Exception e) {
			result.status = Status.error(e);
		}
		return test;
	}

	// TODO - BasicCreateStartStopRelease - over network control
	public TestResults JunitService(String testName, TestResults test) {
		TestResult result = test.results.get(testName);
		try {

			/*
			 * Suite suite = new Suite(klass, new RunnerBuilder() { ... //
			 * Implement methods }); JUnitCore c = new JUnitCore();
			 * c.run(Request.runner(suite));
			 */

			Class<?> junitTest = Class.forName(String.format("org.myrobotlab.service.%sTest", test.simpleName));
			JUnitCore junit = new JUnitCore();
			Result junitResult = junit.run(junitTest);
			log.info("JUnit Result : {}", junitResult);

			// result.link = String.format("<a href=\"%s\">%s</a>", url,
			// testName);
			/*
			 * if (servicePage == null || servicePage.contains("Page not found"
			 * )) { result.status = Status.error("script not found"); } else {
			 * result.status = Status.success(); }
			 */

			result.status = Status.success();

		} catch (Exception e) {
			result.status = Status.error(e);
		}

		return test;
	}

	public TestResults ServicePageExists(String testName, TestResults test) {
		TestResult result = test.results.get(testName);
		try {

			HttpClient http = (HttpClient) startPeer("http");

			String n = test.simpleName;
			String url = String.format("http://myrobotlab.org/service/%s", n);
			String servicePage = http.get(url);

			result.link = String.format("<a href=\"%s\">%s</a>", url, testName);
			if (servicePage == null || servicePage.contains("Page not found")) {
				result.status = Status.error("script not found");
			} else {
				result.status = Status.success();
			}

		} catch (Exception e) {
			result.status = Status.error(e);
		}

		return test;
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

	public List<String> getServicesWithOutServicePages() throws ClientProtocolException, IOException {
		ArrayList<String> ret = new ArrayList<String>();
		ServiceData sd = ServiceData.getLocalInstance();
		ArrayList<ServiceType> serviceTypes = sd.getServiceTypes();
		HttpClient http = (HttpClient) startPeer("http");
		for (int i = 0; i < serviceTypes.size(); ++i) {
			ServiceType serviceType = serviceTypes.get(i);

			// Status retStatus =
			// verifyServicePageScript(serviceType.getName());
			String n = serviceType.getSimpleName();
			String url = String.format("http://myrobotlab.org/service/%s", n);
			String servicePage = http.get(url);
			if (servicePage.contains("Page not found")) {
				log.warn("no service page for {}", n);
				ret.add(n);
			}

		}

		return ret;
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
	 * @throws InterruptedException
	 * 
	 * @throws Exception
	 * 
	 *             FIXME - need to change to testPythonScript(serviceName) ..
	 *             because only 1 will be run in a 'clean' environment ...
	 * 
	 *             FIXME - structured logging back to self to generate report
	 */

	public TestResults PythonScriptTest(String testName, TestResults test) throws Exception {

		// a test will resolve in 3 possible states
		// 1. complete & success - with onFinish callback called
		// 2. with an error
		// 3. with a time out

		// all three need to be handled

		Python python = (Python) startPeer("python", "Python");
		subscribe(python.getName(), "publishStatus");

		TestResult result = test.results.get(testName);
		try {

			String script = GitHub.getPyRobotLabScript(test.simpleName);

			if (script == null) {
				result.status = Status.error("script does not exist");
				return test;
			}

			String serviceName = test.simpleName;
			log.info("TESTING SCRIPT {} - quiet on the set please...", serviceName);

			/*
			 * if (skippedPythonScript.contains(serviceName)) { log.info(
			 * "SKIPPING {} ....", serviceName); continue; }
			 */

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
			python.exec(script + callback);// , true, true);

			// script has at maximum 1 minute to return
			synchronized (lock) {
				long ts = System.currentTimeMillis();
				lock.wait(60000);
				if (System.currentTimeMillis() - ts >= 60000) {
					result.status = Status.error("script %s FAILED - took longer than 1 minute!", serviceName);
				} else {
					if (lock.status.isError()) {
						// the callback had error - set the result of the test
						result.status = lock.status;
						// reset the lock
						lock.status = Status.success();
					} else {
						log.info("script {} PASSED !", serviceName);
						result.status = Status.success();
					}
				}
			}

			// max execution time ??? - then error

			log.info("inspect python errors (syntax) + java errors");

		} catch (Exception e) {
			result.status = Status.error(e);
		}

		log.info("TESTING COMPLETED");

		return test;

	}

	// TODO - subscribe to registered --> generates subscription to
	// publishState() - filter on Errors
	// FIXME - FILE COMMUNICATION !!!!
	public static void main(String[] args) {
		LoggingFactory.init(Level.INFO);
		try {

			Test test = (Test) Runtime.start("test", "Test");

			Runtime.start("webgui", "WebGui");

			boolean done = true;
			if (done) {
				return;
			}

			String[] all = test.getAllServiceNames();
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < all.length; ++i) {
				sb.append(String.format("%s\n", all[i]));
			}

			log.info("\n{}\n", sb.toString());

			List<String> ret = test.getServicesWithOutServicePages();
			for (String s : ret) {
				log.info(s);
			}

			// Runtime.start("cli", "Cli");
			Agent agent = (Agent) Runtime.start("agent", "Agent");
			String[] cmdline = new String[] { "-fromAgent", "-service", "guiservice", "SwingGui" };
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
			 * 
			 * Test test = (Test) Runtime.start("test", "Test");
			 * test.getPyRobotLabServiceScripts(); test.getState();
			 * test.testPythonScripts(); log.info("here");
			 * 
			 */

			log.info("here");

		} catch (Exception e) {
			Logging.logError(e);
		}

	}

	@Override
	public void onStatus(Status status) {
		synchronized (lock) {
			lock.status = status;
			lock.notifyAll();
		}
	}

}

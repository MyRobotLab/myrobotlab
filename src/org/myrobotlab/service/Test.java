package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.fileLib.FindFile;
import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.repo.Repo;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.framework.repo.ServiceType;
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

	Date now = new Date();
	transient Set<Thread> threads = null;
	transient Set<File> files = new HashSet<File>();

	Status status = null;

	BlockingQueue<Object> data = new LinkedBlockingQueue<Object>();

	public static void logThreadNames() {
		
		Set<Thread> threads = Runtime.getThreads();
		String[] tn = new String[threads.size()];
		int x = 0;
		for(Thread thread: threads){
			tn[x] = thread.getName();
			++x;
		}

		Arrays.sort(tn);
		log.warn(Encoder.toJson(tn));
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
	 * if (py == null || py.length() == 0) {
	 * status.addError("%s.py does not exist", shortName); } else {
	 * uart99.connect("UART99"); uart99.recordRX(String.format("%s.rx",
	 * shortName)); // FIXME // FILENAME // OVERLOAD python.exec(py);
	 * uart99.stopRecording(); // check rx file against saved data }
	 * 
	 * // get python errors !
	 * 
	 * // clean services Runtime.releaseAllServicesExcept(keepMeRunning); }
	 * 
	 * return null;
	 * 
	 * }
	 */

	// TODO - subscribe to registered --> generates subscription to
	// publishState() - filter on Errors
	// FIXME - FILE COMMUNICATION !!!!
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		try {

			String serviceType = "InMoovHand";
			Repo repo = new Repo();
			// repo.clearRepo();
			// dirty clean :)
			// repo.clearLibraries();
			// repo.clearServiceData();
			repo.install(serviceType);
			Test test = (Test) Runtime.start("test", "Test");
			test.getState();
			test.test(serviceType);
		} catch (Exception e) {
			Logging.logError(e);
		}

	}

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

	public void exit(Status status) {
		try {
			// check against current state for
			// NOT NEEDED Regular save file - since Agent is process.waitFor
			// FIXME - append states to file
			FileIO.savePartFile("test.json", Encoder.toJson(status).getBytes());
			// Runtime.releaseAll();
			// TODO - should be all clean - if not someone left threads open -
			// report them
			// big hammer
		} catch (Exception e) {
			Logging.logError(e);
		}
		System.exit(0);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "testing", "framework" };
	}

	@Override
	public String getDescription() {
		return "used as a general template";
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
	public void handleError(String errorMsg) {
		if (status != null) {
			status.addError(errorMsg);
		}
	}

	public void registered(ServiceInterface sw) {

		subscribe(sw.getName(), "publishError", "handleError");
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
			Encoder.toJson(s);

			// TODO JAXB xml - since it comes with java 7

		} catch (Exception ex) {
			status.addError(ex);
			return status;
		}

		// NO ERROR !!
		return null;
	}

	public Object subscribe(Object inData) {
		log.info("subscribe has received data");
		log.info(String.format("Test.subscribed received %s", inData));
		data.add(inData);
		return inData;
	}

	@Override
	public Status test() {
		// we are started so .. we'll use the big hammer at the end
		Status status = Status.info("========TESTING=============");
		log.info("===========INFO TESTING========");
		log.info(String.format("TEST PID = %s", Runtime.getPID()));
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
	public void test(String serviceType) {

		// getState();
		// logThreadNames();

		log.warn(String.format("==== testing %s ====", serviceType));
		status = Status.info("==== testing %s ====", serviceType);

		try {

			// install of depencencies and environment is done by
			// the Agent smith (thompson)

			ServiceInterface s = null;

			// create test
			try {
				s = Runtime.create(serviceType, serviceType);
			} catch (Exception e) {
				status.addError("create %s", e);
				exit(status);
			}

			// start test
			if (s == null) {
				status.addError("could not create %s", serviceType);
				exit(status);
			}

			// add error route - for call backs
			subscribe("publishError", s.getName(), "handleError", String.class);

			try {
				s.startService();
				// FIXME - s.waitForStart();
				// Thread.sleep(500);
			} catch (Exception e) {
				status.addError("startService %s", e);
				exit(status);
			}

			status.add(serializeTest(s));

			status.add(s.test());
			// logThreadNames();

			// assume installed - Agent's job

			// serialize test

			// python test

			// release
			try {
				if (s.hasPeers()) {
					s.releasePeers();
				}
			} catch (Exception e) {
				status.addError("releasePeers %s", e);
			}

			try {
				s.releaseService();
			} catch (Exception e) {
				status.addError("releaseService %s", e);
			}

			log.info("exiting environment");

		} catch (Exception e) {
			status.addError(e);
		}

		exit(status);
	}

	/**
	 * this can not be used to test environment
	 * 
	 * @return
	 */
	public Status testAll() {

		String[] serviceTypeNames = Runtime.getInstance().getServiceTypeNames();
		Status status = Status.info("subTest");

		status.add(Status.info("will test %d services", serviceTypeNames.length));

		for (int i = 0; i < serviceTypeNames.length; ++i) {
			String fullName = serviceTypeNames[i];
			test(fullName);
			// status.add(test(fullName)); cant accumulate with exit(status)
		}

		return status;
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

	public Status verifyServicePageScripts() {
		Repo repo = Runtime.getInstance().getRepo();
		ServiceData serviceData = repo.getServiceData();
		ArrayList<ServiceType> serviceTypes = serviceData.getServiceTypes();

		Status status = Status.info("serviceTest will test %d services", serviceTypes.size());
		long startTime = System.currentTimeMillis();
		status.addNamedInfo("startTime", "%d", startTime);

		for (int i = 0; i < serviceTypes.size(); ++i) {
			ServiceType serviceType = serviceTypes.get(i);
			Status retStatus = verifyServicePageScript(serviceType.getName());
			if (retStatus.hasError()) {
				status.add(retStatus);
			}
		}

		return status;
	}

}

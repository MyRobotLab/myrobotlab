package org.myrobotlab.service;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.myrobotlab.cmdline.CMDLine;
import org.myrobotlab.fileLib.FindFile;
import org.myrobotlab.framework.Bootstrap;
import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.repo.Repo;
import org.myrobotlab.framework.repo.UpdateReport;
import org.myrobotlab.logging.Appender;
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
 */
public class Test extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Test.class);

	Date now = new Date();

	// transient public XMPP xmpp;

	// transient public WebGUI webgui;
	// transient public Python python;

	// transient Index<Object> cache = new Index<Object>();

	// TODO - take snapshot of threads - compare at
	// any other times - find the diff of threads - generated errors
	// approprately

	// TODO - subscribe to registered --> generates subscription to
	// publishState() - filter on Errors

	// FIXME NEED TO AD SOME REPO MANAGEMENT ROUTINES TO RUNTIME - LIKE REMOVE
	// REPO

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		// peers.suggestRootAs("python", "python", "Python",
		// "shared python instance");

		// peers.suggestAs("python", "python", "Python",
		// "shared python instance");

		// peers.put("xmpp", "XMPP", "XMPP service");
		// peers.put("webgui", "WebGUI", "WebGUI service");
		// peers.put("python", "Python", "Python service");

		return peers;
	}

	public Test() {
		this("test");
	}

	public Test(String n) {
		super(n);
		addRoutes();
	}

	@Override
	public void startService() {
		super.startService();
	}

	public static Status install(String fullType) {
		Status status = Status.info("install %s", fullType);
		try {
			Repo repo = new Repo("test");

			if (!repo.isServiceTypeInstalled(fullType)) {
				repo.retrieveServiceType(fullType);
				if (repo.hasErrors()) {
					status.addError(repo.getErrors());
				}

			} else {
				log.info("installed {}", fullType);
			}
		} catch (Exception e) {
			status.addError(e);
		}
		return status;
	}

	public Status serviceTest() {

		String[] serviceTypeNames = Runtime.getInstance().getServiceTypeNames();

		Status status = Status.info("serviceTest will test %d services", serviceTypeNames.length);

		Set<Thread> originalThreads = Thread.getAllStackTraces().keySet();

		for (int i = 0; i < serviceTypeNames.length; ++i) {

			ServiceInterface s = null;
			String fullType = serviceTypeNames[i];

			if ("org.myrobotlab.service.Test".equals(fullType) || "org.myrobotlab.service.Runtime".equals(fullType)) {
				log.info("skipping Test & Runtime");
				continue;
			}

			try {

				// install it
				status.add(install(fullType));

				// create it
				log.info("creating {}", fullType);
				s = Runtime.create(fullType, fullType);

				if (s == null) {
					status.addError("could not create %s service", fullType);
					continue;
				}

				// start it
				log.info("starting {}", fullType);
				s.startService();

				log.info("starting {}", fullType);
				Status result = s.test();
				if (result != null && result.hasError()) {
					status.add(result);
				}

				s.releaseService();

				if (s.hasPeers()) {
					s.releasePeers();
				}

			} catch (Exception e) {
				status.addError("ERROR - %s", fullType);
				status.addError(e);
				continue;
			}
		}
		return status;

	}

	// FIXME - 2 sets of services - 1 by serviceData.xml & 1 by all files in
	// org.myrobotlab.service
	// FIXME - do all types of serialization
	// TODO - encode decode test JSON & XML
	// final ArrayList<Status>
	public Status serializeTest() {

		String[] serviceTypeNames = Runtime.getInstance().getServiceTypeNames();
		Status status = Status.info("serializeTest");

		status.add(Status.info("will test %d services", serviceTypeNames.length));

		Set<Thread> originalThreads = Thread.getAllStackTraces().keySet();

		for (int i = 0; i < serviceTypeNames.length; ++i) {

			ServiceInterface s = null;
			String fullType = serviceTypeNames[i];

			if ("org.myrobotlab.service.Test".equals(fullType) || "org.myrobotlab.service.Runtime".equals(fullType)) {
				log.info("skipping Test & Runtime");
				continue;
			}

			// fullType = "org.myrobotlab.service.JFugue";

			try {

				// install it
				status.add(install(fullType));

				// create it
				log.info("creating {}", fullType);
				s = Runtime.create(fullType, fullType);

				if (s == null) {
					status.addError("could not create %s service", fullType);
					continue;
				}

				// start it
				log.info("starting {}", fullType);
				s.startService();

			} catch (Exception e) {
				status.addError("ERROR - %s", fullType);
				status.addError(e);
				continue;
			}

			try {

				log.info("serializing {}", fullType);

				// TODO put in encoder
				ByteArrayOutputStream fos = null;
				ObjectOutputStream out = null;
				fos = new ByteArrayOutputStream();
				out = new ObjectOutputStream(fos);
				out.writeObject(s);
				fos.flush();
				out.close();

				log.info("releasing {}", fullType);

				if (s.hasPeers()) {
					s.releasePeers();
				}

				s.releaseService();
				sleep(300);

				Set<Thread> currentThreads = Thread.getAllStackTraces().keySet();

				if (currentThreads.size() > originalThreads.size()) {
					for (Thread t : currentThreads) {
						if (!originalThreads.contains(t)) {
							status.addError("%s has added thread %s but not cleanly removed it", fullType, t.getName());

							// resetting original thread count
							originalThreads = currentThreads;
						}
					}

				}

				log.info("released {}", fullType);

			} catch (Exception ex) {
				status.addError(ex);
			}
		} // end of loop

		return status;
	}

	/*
	 * public IndexNode<Object> get(String robotName) { return
	 * cache.getNode(robotName); }
	 */

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public Status subTest() {

		HashSet<String> keepMeRunning = new HashSet<String>();
		List<ServiceInterface> list = Runtime.getServices();
		for (int j = 0; j < list.size(); ++j) {
			ServiceInterface si = list.get(j);
			keepMeRunning.add(si.getName());
		}

		String[] serviceTypeNames = Runtime.getInstance().getServiceTypeNames();
		Status status = Status.info("subTest");

		status.add(Status.info("will test %d services", serviceTypeNames.length));

		for (int i = 0; i < serviceTypeNames.length; ++i) {
			String fullName = serviceTypeNames[i];
			String shortName = fullName.substring(fullName.lastIndexOf(".") + 1);
			try {
				ServiceInterface si = Runtime.start(shortName, shortName);
				status.add(si.test());
			} catch (Exception e) {
				status.addError(e);
			}

			// clean services
			Runtime.releaseAllServicesExcept(keepMeRunning);
		}

		return status;

	}

	public Status serialTest() {

		Status status = Status.info("serialTest");
		log.info("starting serial Test");

		return status;
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

	public void testServiceScripts() {
		// get download zip

		// uncompress locally

		// test - instrumentation for
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
	public void addRoutes() {
		// register with runtime for any new services
		// their errors are routed to mouth
		subscribe(this.getName(), "publishError", "handleError");

		Runtime r = Runtime.getInstance();
		r.addListener(getName(), "registered");
	}

	public void registered(ServiceInterface sw) {

		subscribe(sw.getName(), "publishError", "handleError");
	}

	public void handleError(String msg) {
		// AHHHH! with just error (vs log.error) - goes in infinite loop
		log.error(String.format("cool - all errors are caught here since we register for them - this error is - %s", msg));
	}

	public void handleError(Status status) {
		/*
		 * // FIXME - remove - only add xmp if HandleError requires an error
		 * alert XMPP xmpp = (XMPP) startPeer("xmpp"); // python = (Python)
		 * startPeer("python");
		 * 
		 * xmpp.startService(); // webgui.startService();
		 * 
		 * xmpp.login("test@myrobotlab.org", "hatchMe!");
		 * 
		 * xmpp.addAuditor("Greg Perry"); // python.startService();
		 * xmpp.sendMessage(Encoder.gson.toJson(status), "Greg Perry"); //
		 * xmpp.releaseService(); // TODO email
		 */
	}

	/**
	 * install all service
	 */
	public void installAll() {

		Runtime runtime = Runtime.getInstance();
		UpdateReport report = runtime.updateAll();
		log.info(report.toString());
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

	transient Set<Thread> threads = null;
	transient Set<File> files = new HashSet<File>();

	/**
	 * used to get state of the current service and runtime - so that the
	 * environment and final system can be cleaned to an original "base" state
	 */
	public void getState() {
		try {
			threads = Thread.getAllStackTraces().keySet();
			List<File> f  = FindFile.find("libraries", ".*");
			for (int i = 0; i < f.size(); ++i){
				files.add(f.get(i));
			}
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	// beginning of interactive stdin stdout stderr shell
	public void Echo() throws IOException {

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String s;
		while ((s = in.readLine()) != null && s.length() != 0)
			System.out.println(s);
		// An empty line or Ctrl-Z terminates the program

	} // /:~

	boolean cleanDepenencies = true;

	public void clean() {
		// Runtime.cleanCache();
		Runtime.cleanCache(files);
	}

	/**
	 * use an argument list to set test parameters
	 * 
	 * @param args
	 */
	public Status test(CMDLine testArgs) {

		Status status = Status.info("starting %s %s test", getName(), getType());
		// get set of starter threads to keep safe .. prepare to kill all others
		// ..

		try {

			if (testArgs.containsKey("-service")) {
				ArrayList<String> servicesToTest = testArgs.getArgumentList("-service");

				for (int i = 0; i < servicesToTest.size(); ++i) {

					String service = servicesToTest.get(i);
					// TO JUNIT OR NOT TO JUNIT THAT IS THE QUESTION
					// YES JUNIT !
					// start -
					// FIXME ivy pull of transitive test dependencies
					// clean then spawn a Test

					log.info("cleaning cache");
					/*
					 * if (!Runtime.cleanCache()) { throw new
					 * IOException("could not clean cache"); }
					 */

					// pull testing dependencies
					// spawn and get a reference to a tester
					Process tester = Bootstrap.spawn(new String[] { "-service", "test", "Test" });

					/*
					 * JUnitCore junit = new JUnitCore(); junit. Result result =
					 * junit.run(testClasses);
					 */

					// spawn test process
					log.info("spawn test process");

					// check results
					log.info("check results");

					// destroy environment
					log.info("destroy environment");

					status.add(subTest());
					// status.add(serializeTest());
					// status.add(serviceTest());
				}
			}

			if (status.hasError()) {
				handleError(status);
			}

		} catch (Exception e) {
			status.addError(e);
		}

		return status;
	}
	
	public Status test(){
		// we are started so .. we'll use the big hammer at the end
		Status status = Status.info("========TESTING=============");
		log.info("INFO TESTING");
		log.info(String.format("PID = %s", Runtime.getPID()));
		System.exit(0);
		return status;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		LoggingFactory.getInstance().addAppender(Appender.FILE);

		// Runtime.cleanCache();

		Test test = (Test) Runtime.start("test", "Test");
		test.getState();
		test.clean();
		
		test.test(new CMDLine(new String[] { "-test", "-service", "Serial" }));
		// test.servoArduinoOpenCVGUIService();

		/*
		 * test.installAll(); // test.startTest();
		 * 
		 * test.testPythonScripts();
		 * 
		 * // Runtime.createAndStart("gui", "GUIService");
		 */

	}

}

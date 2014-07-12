package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.ivy.core.report.ResolveReport;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Index;
import org.myrobotlab.framework.IndexNode;
import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.repo.Repo;
import org.myrobotlab.framework.repo.UpdateReport;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

public class Incubator extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Incubator.class);

	transient public XMPP xmpp;
	transient public WebGUI webgui;
	transient public Python python;

	transient Index<Object> cache = new Index<Object>();

	// TODO - take snapshot of threads - compare at
	// any other times - find the diff of threads - generated errors
	// approprately

	// TODO - subscribe to registered --> generates subscription to
	// publishState() - filter on Errors

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		peers.suggestRootAs("python", "python", "Python", "shared python instance");

		// peers.suggestAs("python", "python", "Python",
		// "shared python instance");

		peers.put("xmpp", "XMPP", "XMPP service");
		peers.put("webgui", "WebGUI", "WebGUI service");
		peers.put("python", "Python", "Python service");

		return peers;
	}

	public Incubator(String n) {
		super(n);
		addRoutes();
	}

	@Override
	public void startService() {
		super.startService();
	}

	public void startPeers() {
		xmpp = (XMPP) createPeer("xmpp");
		python = (Python) createPeer("python");
		webgui = (WebGUI) createPeer("webgui");
		webgui.port = 4321;

		xmpp.startService();
		webgui.startService();

		xmpp.login("incubator@myrobotlab.org", "hatchMe!");

		xmpp.addAuditor("Greg Perry");
		python.startService();

	}

	/*
	static public class Error {
		public String name;
		public String type;
		public String description;

		public Error(String name, String type, Exception e) {
			this.name = name;
			this.type = type;
			this.description = Logging.stackToString(e);
		}

		public Error(String simpleType, String type) {
			this.name = simpleType;
			this.type = type;
		}
	}
	*/

	// FIXME - do all types of serialization
	// TODO - encode decode test JSON & XML
	public final ArrayList<Status> serializeTest() {

		String[] serviceTypeNames = Runtime.getInstance().getServiceTypeNames();
		ArrayList<Status> badServices = new ArrayList<Status>();

		Runtime runtime = Runtime.getInstance();

		for (int i = 0; i < serviceTypeNames.length; ++i) {
			ServiceInterface s = null;
			ByteArrayOutputStream fos = null;
			ObjectOutputStream out = null;
			String fullType = serviceTypeNames[i];

			try {
				/*
				if (fullType.equals(Incubator.class.getSimpleName())) {
					log.warn("skipping Incubator class");
					continue;
				}
				log.warn("starting " + fullType);
				*/

				Repo repo = new Repo("test");
				
				//String fullType = String.format("org.myrobotlab.service.%s", fullTypex);
				if (!repo.isServiceTypeInstalled(fullType)) {
					 ArrayList<ResolveReport> reports = repo.retrieveServiceType(fullType);
					 
					 for (int j = 0; j < reports.size(); ++j){
						 ResolveReport report = reports.get(j);
						 List<?> errors = report.getAllProblemMessages();
						 if (errors.size() > 0){
							 log.error("ERROR");
							 badServices.add(Status.error("retrieving %s returned errors %s", fullType, Arrays.toString(errors.toArray())));
						 }
					 }
					/*
					badServices.add(new Error(simpleType, "notInstalled"));
					continue;
					*/
				}

				if (fullType.equals("tracking")){
					log.info("here");
				}
				s = Runtime.create(fullType, fullType);

				//if (!Runtime.isHeadless() || (Runtime.isHeadless() && !s.hasDisplay())) {
					s.startService();
				//} else {
				//	log.warn(String.format("won't start %s - do not have a display", fullType));
				//}

			} catch (Exception e) {
				badServices.add(Status.error("%s - %s", fullType, e.getMessage()));
			}

			try {
				log.warn("serializing " + fullType);

				// TODO put in encoder
				fos = new ByteArrayOutputStream();
				out = new ObjectOutputStream(fos);
				out.writeObject(s);
				fos.flush();
				out.close();

				log.info("releasing " + fullType);

				if (s.hasPeers()) {
					s.releasePeers();
				}

				s.releaseService();

				log.warn("released %s", fullType);
			} catch (Exception e) {
				badServices.add(Status.error("serializing %s threw %s", fullType, e.getMessage()));
			}

		}

		return badServices;
	}

	public void startTest() {
		ArrayList<Status> errors = serializeTest();
		log.info(String.format("found %d errors in serialization", errors.size()));
	}

	public IndexNode<Object> get(String robotName) {
		return cache.getNode(robotName);
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public void testPythonScripts() {
		try {
			// String script;
			ArrayList<File> list = FileIO.listInternalContents("/resource/Python/examples");

			Runtime.createAndStart("gui", "GUIService");
			python = (Python) startPeer("python");
			InMoov i01 = (InMoov) Runtime.createAndStart("i01", "InMoov");

			HashSet<String> keepMeRunning = new HashSet<String>(Arrays.asList("i01", "gui", "runtime", "python", getName()));

			for (int i = 0; i < list.size(); ++i) {
				String r = list.get(i).getName();
				if (r.startsWith("InMoov2")) {
					warn("testing script %s", r);
					String script = FileIO.resourceToString(String.format("Python/examples/%s", r));
					python.exec(script);
					log.info("here");
					i01.detach();
					Runtime.releaseAllServicesExcept(keepMeRunning);
				}
			}

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

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
		log.error(msg);
		// TODO email
	}

	/**
	 * install all service
	 */
	public void installAll() {

		Runtime runtime = Runtime.getInstance();
		UpdateReport report = runtime.updateAll();
		log.info(report.toString());
	}

	public void servoArduinoOpenCVGUIService() {
		try {
			Servo servo = (Servo) Runtime.start("servo", "Servo");
			OpenCV opencv = (OpenCV) Runtime.start("opencv", "OpenCV");
			GUIService gui = (GUIService) Runtime.start("gui", "GUIService");

			opencv.addFilter("PyramidDown");
			opencv.capture();

			sleep(5000);

			servo.test();
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	// remove all - install single 1 - check for errors on start

	// install all 3rd party libraries ???

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		Incubator incubator = new Incubator("incubator");
		incubator.serializeTest();
		//incubator.servoArduinoOpenCVGUIService();

		/*
		incubator.installAll();
		// incubator.startTest();

		incubator.testPythonScripts();

		// Runtime.createAndStart("gui", "GUIService");
		 * 
		 */

	}

}

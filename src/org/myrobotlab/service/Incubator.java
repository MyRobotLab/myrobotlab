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
import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Index;
import org.myrobotlab.framework.IndexNode;
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

		xmpp = (XMPP) startPeer("xmpp");
		python = (Python) startPeer("python");
		webgui = (WebGUI) createPeer("webgui");
		webgui.port = 4321;
		webgui.startService();

		xmpp.startService();
		webgui.startService();

		xmpp.login("incubator@myrobotlab.org", "hatchMe!");

		xmpp.addAuditor("Greg Perry");
		python.startService();
	}

	// FIXME - do all types of serialization
	// TODO - encode decode test JSON & XML
	// final ArrayList<Status>
	public ArrayList<Status> serializeTest() {

		String[] serviceTypeNames = Runtime.getInstance().getServiceTypeNames();
		ArrayList<Status> badServices = new ArrayList<Status>();

		for (int i = 0; i < serviceTypeNames.length; ++i) {
			
			ServiceInterface s = null;
			
			ByteArrayOutputStream fos = null;
			ObjectOutputStream out = null;
			String fullType = serviceTypeNames[i];
			

			if ("org.myrobotlab.service.PickToLight".equals(fullType) || "org.myrobotlab.service.Incubator".equals(fullType) || "org.myrobotlab.service.Runtime".equals(fullType)  || "org.myrobotlab.service.Plantoid".equals(fullType)) {
				continue;
			}


			try {

				Repo repo = new Repo("test");

				if (!repo.isServiceTypeInstalled(fullType)) {
					ArrayList<ResolveReport> reports = repo.retrieveServiceType(fullType);

					for (int j = 0; j < reports.size(); ++j) {
						ResolveReport report = reports.get(j);
						List<?> errors = report.getAllProblemMessages();
						if (errors.size() > 0) {
							log.error("ERROR");
							badServices.add(Status.error("retrieving %s returned errors %s", fullType, Arrays.toString(errors.toArray())));
						}
					}
					/*
					 * badServices.add(new Error(simpleType, "notInstalled"));
					 * continue;
					 */
				}
				
				log.info("creating {}", fullType);
				s = Runtime.create(fullType, fullType);

				// if (!Runtime.isHeadless() || (Runtime.isHeadless() &&
				// !s.hasDisplay())) {
				log.info("starting {}", fullType);
				s.startService();
				// } else {
				// log.warn(String.format("won't start %s - do not have a display",
				// fullType));
				// }

			} catch (Exception e) {
				badServices.add(Status.error("%s - %s", fullType, e.getMessage()));
				continue;
			}

			try {

				log.info("serializing {}", fullType);

				// TODO put in encoder
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

				log.warn("released {}", fullType);

			} catch (Exception e) {
				badServices.add(Status.error("serializing %s threw %s", fullType, e.getMessage()));
			}
		}

 		return badServices;
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

	public void test() {
		ArrayList<Status> stati = serializeTest();
		xmpp.sendMessage(Encoder.gson.toJson(stati), "Greg Perry");
	}

	// remove all - install single 1 - check for errors on start

	// install all 3rd party libraries ???

	public void handleError(Exception e) {
		Logging.logException(e);
		xmpp.sendMessage(String.format("%s -> %s", e.getMessage(), Logging.stackToString(e)), "Greg Perry");
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		LoggingFactory.getInstance().addAppender(Appender.FILE);
		

		Incubator incubator = (Incubator) Runtime.start("incubator", "Incubator");
		incubator.test();
		// incubator.servoArduinoOpenCVGUIService();

		/*
		 * incubator.installAll(); // incubator.startTest();
		 * 
		 * incubator.testPythonScripts();
		 * 
		 * // Runtime.createAndStart("gui", "GUIService");
		 */

	}

}

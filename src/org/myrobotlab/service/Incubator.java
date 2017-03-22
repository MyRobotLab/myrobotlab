package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.repo.Repo;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Appender;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

/**
 * Incubator - This is a sort of testing / example service.
 *
 */
public class Incubator extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Incubator.class);

  Date now = new Date();

  // transient public XMPP xmpp;

  // transient public WebGui webgui;
  // transient public Python python;

  // transient Index<Object> cache = new Index<Object>();

  // TODO - take snapshot of threads - compare at
  // any other times - find the diff of threads - generated errors
  // approprately

  // TODO - subscribe to onRegistered --> generates subscription to
  // publishState() - filter on Errors

  // FIXME NEED TO AD SOME REPO MANAGEMENT ROUTINES TO RUNTIME - LIKE REMOVE
  // REPO

  public static Status install(String fullType) {
    try {

      // install everything...
      Repo repo = Repo.getLocalInstance();

      if (!repo.isServiceTypeInstalled(fullType)) {
        repo.install(fullType);
        if (repo.hasErrors()) {
          log.error("repo had errors");
        }
      }
    } catch (Exception e) {
      return Status.error(e);
    }
    return null;
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    LoggingFactory.getInstance().addAppender(Appender.FILE);

    Runtime.start("incubator", "Incubator");

    // incubator.servoArduinoOpenCVSwing();

    /*
     * incubator.installAll(); // incubator.startTest();
     * 
     * incubator.testPythonScripts();
     * 
     * // Runtime.createAndStart("gui", "SwingGui");
     */

  }

  public Incubator() {
    this("incubator");
  }

  public Incubator(String n) {
    super(n);
    addRoutes();
  }

  // very good - dynamicly subscribing to other service's
  // published errors
  // step 1 subscribe to runtimes onRegistered event
  // step 2 in any onRegistered -
  // step 3 - fix up - so that state is handled (not just "error")
  public void addRoutes() {

    Runtime r = Runtime.getInstance();
    subscribe(r.getName(), "registered");

    // handle my own error the same way too
    subscribe(getName(), "publishError");
  }

  public void onError(Status status) {
    try {
      // FIXME - remove - only add xmp if onError requires an error
      // alert
      Xmpp xmpp = (Xmpp) startPeer("xmpp");
      // python = (Python) startPeer("python");
      /*
       * webgui = (WebGui) createPeer("webgui"); webgui.port = 4321;
       * webgui.startService();
       */
      xmpp.startService();
      // webgui.startService();

      xmpp.connect("incubator@myrobotlab.org", "hatchMe!");

      // FIXME - xmpp.addAuditor("Greg Perry");
      // python.startService();
      xmpp.sendMessage(CodecUtils.toJson(status), "Greg Perry");
      // xmpp.releaseService();
      // TODO email
    } catch (Exception e) {
      Logging.logError(e);
    }

  }

  public void onError(String msg) {
    // AHHHH! with just error (vs log.error) - goes in infinite loop
    log.error(String.format("cool - all errors are caught here since we register for them - this error is - %s", msg));
  }

  /**
   * install all service
   * 
   * @throws IOException
   * @throws ParseException
   */
  public void installAll() throws ParseException, IOException {
    // Runtime.getInstance();
    Repo repo = Repo.getLocalInstance();
    repo.install();
  }

  public List<Status> pythonTest() throws IOException {
    ArrayList<Status> ret = new ArrayList<Status>();

    Python python = (Python) Runtime.start("python", "Python");
    Serial uart99 = (Serial) Runtime.start("uart99", "Serial");
    // take inventory of currently running services
    HashSet<String> keepMeRunning = new HashSet<String>();

    List<ServiceInterface> list = Runtime.getServices();
    for (int j = 0; j < list.size(); ++j) {
      ServiceInterface si = list.get(j);
      keepMeRunning.add(si.getName());
    }

    String[] serviceTypeNames = Runtime.getInstance().getServiceTypeNames();
    ret.add(new Status("subTest"));

    ret.add(Status.info("will test %d services", serviceTypeNames.length));
    ret.add(new Status("subTest"));

    for (int i = 0; i < serviceTypeNames.length; ++i) {
      String fullName = serviceTypeNames[i];
      String shortName = fullName.substring(fullName.lastIndexOf(".") + 1);

      String py = FileIO.resourceToString(String.format("Python/examples/%s.py", shortName));

      if (py == null || py.length() == 0) {
        ret.add(Status.error("%s.py does not exist", shortName));
      } else {
        // uart99.connect("UART99");
        uart99.record(); // FIXME
        // FILENAME
        // OVERLOAD
        python.exec(py);
        uart99.stopRecording();
        // check rx file against saved data
      }

      // get python errors !

      // clean services
      Runtime.releaseAllServicesExcept(keepMeRunning);
    }

    return null;

  }

  public void onRegistered(ServiceInterface sw) {

    subscribe(sw.getName(), "publishError");
  }

  // FIXME - 2 sets of services - 1 by serviceData.xml & 1 by all files in
  // org.myrobotlab.service
  // FIXME - do all types of serialization
  // TODO - encode decode test JSON & XML
  // final ArrayList<Status>
  public ArrayList<Status> serializeTest() {

    ArrayList<Status> ret = new ArrayList<Status>();

    String[] serviceTypeNames = Runtime.getInstance().getServiceTypeNames();
    // Status status = Status.info("serializeTest");

    ret.add(Status.info("will test %d services", serviceTypeNames.length));

    Set<Thread> originalThreads = Thread.getAllStackTraces().keySet();

    for (int i = 0; i < serviceTypeNames.length; ++i) {

      ServiceInterface s = null;
      String fullType = serviceTypeNames[i];

      if ("org.myrobotlab.service.Incubator".equals(fullType) || "org.myrobotlab.service.Runtime".equals(fullType)) {
        log.info("skipping Incubator & Runtime");
        continue;
      }

      // fullType = "org.myrobotlab.service.JFugue";

      try {

        // install it
        ret.add(install(fullType));

        // create it
        log.info("creating {}", fullType);
        s = Runtime.create(fullType, fullType);

        if (s == null) {
          ret.add(Status.error("could not create %s service", fullType));
          continue;
        }

        // start it
        log.info("starting {}", fullType);
        s.startService();

      } catch (Exception e) {
        ret.add(Status.error("ERROR - %s", fullType));
        ret.add(new Status(e));
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
              ret.add(Status.error("%s has added thread %s but not cleanly removed it", fullType, t.getName()));

              // resetting original thread count
              originalThreads = currentThreads;
            }
          }

        }

        log.info("released {}", fullType);

      } catch (Exception ex) {
        ret.add(new Status(ex));
      }
    } // end of loop

    return ret;
  }

  public List<Status> serviceTest() {
    List<Status> ret = new ArrayList<Status>();

    String[] serviceTypeNames = Runtime.getInstance().getServiceTypeNames();

    // Status status = Status.info("serviceTest will test %d services",
    // serviceTypeNames.length);

    // Set<Thread> originalThreads = Thread.getAllStackTraces().keySet();

    for (int i = 0; i < serviceTypeNames.length; ++i) {

      ServiceInterface s = null;
      String fullType = serviceTypeNames[i];

      if ("org.myrobotlab.service.Incubator".equals(fullType) || "org.myrobotlab.service.Runtime".equals(fullType)) {
        log.info("skipping Incubator & Runtime");
        continue;
      }

      try {

        // install it
        ret.add(install(fullType));

        // create it
        log.info("creating {}", fullType);
        s = Runtime.create(fullType, fullType);

        if (s == null) {
          ret.add(Status.error("could not create %s service", fullType));
          continue;
        }

        // start it
        log.info("starting {}", fullType);
        s.startService();

        log.info("starting {}", fullType);
        // FIXME - will need to do JUnit !!!!
        /*
         * Status result = s.test(); if (result != null && result.hasError()) {
         * ret.add(result); }
         */

        s.releaseService();

        if (s.hasPeers()) {
          s.releasePeers();
        }

      } catch (Exception e) {
        ret.add(error(e));
        continue;
      }
    }

    return ret;
  }

  @Override
  public void startService() {
    super.startService();
  }

  /*
   * public Status subTest() {
   * 
   * HashSet<String> keepMeRunning = new HashSet<String>();
   * List<ServiceInterface> list = Runtime.getServices(); for (int j = 0; j <
   * list.size(); ++j) { ServiceInterface si = list.get(j);
   * keepMeRunning.add(si.getName()); }
   * 
   * String[] serviceTypeNames = Runtime.getInstance().getServiceTypeNames();
   * Status status = Status.info("subTest");
   * 
   * ret.add(Status.info("will test %d services", serviceTypeNames.length));
   * 
   * for (int i = 0; i < serviceTypeNames.length; ++i) { String fullName =
   * serviceTypeNames[i]; String shortName =
   * fullName.substring(fullName.lastIndexOf(".") + 1); try { ServiceInterface
   * si = Runtime.start(shortName, shortName); ret.add(si.test()); } catch
   * (Exception e) { ret.addError(e); }
   * 
   * // clean services Runtime.releaseAllServicesExcept(keepMeRunning); }
   * 
   * return status;
   * 
   * }
   */

  /*
   * @Override public List<Status> test() { Status status = Status.info(
   * "starting %s %s test", getName(), getType());
   * 
   * // ret.add(subTest()); // ret.add(serializeTest()); return
   * ret.add(serviceTest());
   * 
   * if (status.hasError()) { onError(status); }
   * 
   * return status; }
   */

  public void testInMoovPythonScripts() {
    try {

      Python python = (Python) Runtime.start("python", "Python");
      // String script;
      List<File> list = FileIO.listResourceContents("Python/examples");

      Runtime.createAndStart("gui", "SwingGui");
      python = (Python) startPeer("python");
      // InMoov i01 = (InMoov) Runtime.createAndStart("i01", "InMoov");

      HashSet<String> keepMeRunning = new HashSet<String>(Arrays.asList("i01", "gui", "runtime", "python", getName()));

      for (int i = 0; i < list.size(); ++i) {
        String r = list.get(i).getName();
        if (r.startsWith("InMoov2")) {
          warn("testing script %s", r);
          String script = FileIO.resourceToString(String.format("Python/examples/%s", r));
          python.exec(script);
          log.info("here");
          // i01.detach();
          Runtime.releaseAllServicesExcept(keepMeRunning);
        }
      }

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public void testPythonScripts() {
    try {

      Python python = (Python) Runtime.start("python", "Python");
      // String script;
      List<File> list = FileIO.listResourceContents("Python/examples");

      Runtime.createAndStart("gui", "SwingGui");
      python = (Python) startPeer("python");
      // InMoov i01 = (InMoov) Runtime.createAndStart("i01", "InMoov");

      HashSet<String> keepMeRunning = new HashSet<String>(Arrays.asList("i01", "gui", "runtime", "python", getName()));

      for (int i = 0; i < list.size(); ++i) {
        String r = list.get(i).getName();
        if (r.startsWith("InMoov2")) {
          warn("testing script %s", r);
          String script = FileIO.resourceToString(String.format("Python/examples/%s", r));
          python.exec(script);
          log.info("here");
          // i01.detach();
          Runtime.releaseAllServicesExcept(keepMeRunning);
        }
      }

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public void testServiceScripts() {
    // get download zip

    // uncompress locally

    // test - instrumentation for
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {
    ServiceType meta = new ServiceType(Incubator.class.getCanonicalName());
    meta.addDescription("This connector will connect to an IMAP based email server and crawl the emails");
    meta.addCategory("testing");
    meta.addPeer("python", "Python", "shared python instance");
    meta.addPeer("xmpp", "Xmpp", "Xmpp service");
    return meta;
  }

}

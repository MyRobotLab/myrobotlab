package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.client.ClientProtocolException;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.framework.repo.GitHub;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.io.FindFile;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.StatusListener;
import org.slf4j.Logger;

/**
 * Minimal dependency service for rigorous testing
 * 
 * @author GroG
 * 
 *         TODO - grab and report all missing Service Pages &amp; all missing
 *         Python scripts !
 * 
 *         TODO - install create start stop release test TODO - serialization
 *         json + native test TODO - run Python &amp; JavaScript tests - last
 *         method appended is a callback
 *
 */
public class Test extends Service implements StatusListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Test.class);

  static final transient ServiceData serviceData = ServiceData.getLocalInstance();

  // state information
  Date now = new Date();
  transient Set<Thread> threads = null;
  transient Set<File> files = new HashSet<File>();

  /**
   * tests which have returned error
   */
  List<TestData> errors = new ArrayList<TestData>();

  /**
   * the flattened queue of tests (in the form of test results) on a wonderful
   * blocking queue which "potentially" could allow multi-threaded testing -
   * although Tests themselves are certainly not guaranteed to be "thread safe"
   */
  transient BlockingQueue<TestData> testQueue = new LinkedBlockingQueue<TestData>();

  /**
   * the tester - always looking for tests on the testQueu
   */
  transient Tester tester;

  /**
   * ts - tested results - serializable history keyed on timestamp
   */
  Map<Long, Queue<TestData>> history = new TreeMap<Long, Queue<TestData>>();

  // stats & accumulators python
  List<String> servicesNeedingPythonScripts = new ArrayList<String>();
  List<String> pythonScriptsWithNoServiceType = new ArrayList<String>();
  List<String> failedPythonScripts = new ArrayList<String>();
  List<String> passedPythonScripts = new ArrayList<String>();
  Set<String> skippedPythonScript = new TreeSet<String>();
  Set<String> nonAvailableServices = new TreeSet<String>();

  // thread blocking
  transient StatusLock lock = new StatusLock();

  List<Status> status = new ArrayList<Status>();

  transient TreeMap<String, String> pythonScripts = null;

  // FIXME remove ...
  transient LinkedBlockingQueue<Object> data = new LinkedBlockingQueue<Object>();

  TestMatrix matrix = new TestMatrix();

  public static class Progress implements Serializable {
    private static final long serialVersionUID = 1L;
    String currentActivity;
    int percentDone;
    public int testsDone;
    public int errorCount;
    public int errorPercentage;
    public int successes;
    public int successPercentage;
    public int totalTests;

    public void process(Status status) {
      testsDone++;
      percentDone = testsDone * 100 / totalTests;

      if (status.isError()) {
        errorCount++;
        errorPercentage = errorCount * 100 / testsDone;
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
     * this will be important when we start posting test matrices from different
     * platforms
     */
    Platform platform = Platform.getLocalInstance();

    /**
     * next test of tests to run
     */
    HashSet<String> testsToRun = new HashSet<String>();
    /**
     * next set of services to test
     */
    HashSet<String> servicesToTest = new HashSet<String>();

  }
  /*
   * public static class ServiceTestSuite implements Serializable {
   * 
   * private static final long serialVersionUID = 1L;
   * 
   * // FIXME remove - only use ServiceType (normalize please) String
   * fullTypeName; String simpleName; ServiceType type;
   * 
   * TreeMap<String, TestData> results = new TreeMap<String, TestData>();
   * 
   * public ServiceTestSuite(String name) { if (!name.contains(".")) {
   * fullTypeName = String.format("org.myrobotlab.service.%s", name); } else {
   * fullTypeName = name; }
   * 
   * type = serviceData.getServiceType(fullTypeName); simpleName =
   * type.getSimpleName(); }
   * 
   * public ServiceTestSuite() { } }
   */

  public static class TestData implements Serializable {
    private static final long serialVersionUID = 1L;

    String testName;
    String serviceName;
    long startTime;
    long endTime;
    Status status = Status.success();

    public String link;

    public String branch;

    public TestData(String serviceName, String testName) {
      this.serviceName = serviceName;
      this.testName = testName;
      this.link = testName;
      this.startTime = System.currentTimeMillis();
    }

    public String toString() {
      return String.format("testName: %s service: %s/%s status: %s", testName, branch, serviceName, status);
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
  }

  public Progress publishProgress(Progress progress) {
    return progress;
  }

  public void startService() {
    super.startService();
    tester = new Tester(this);
    loadDefaultTests();
  }

  public void stopService() {
    super.stopService();
    if (tester != null) {
      tester.interrupt();
    }
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
   * status.add(Status.info("will test %d services", serviceTypeNames.length));
   * 
   * for (int i = 0; i < serviceTypeNames.length; ++i) { String fullName =
   * serviceTypeNames[i]; String shortName =
   * fullName.substring(fullName.lastIndexOf(".") + 1);
   * 
   * String py = FileIO.resourceToString(String.format("Python/examples/%s.py",
   * shortName));
   * 
   * if (py == null || py.length() == 0) { status.addError(
   * "%s.py does not exist", shortName); } else { uart99.connect("UART99");
   * uart99.recordRX(String.format("%s.rx", shortName)); // FIXME // FILENAME //
   * OVERLOAD python.exec(py); uart99.stopRecording(); // check rx file against
   * saved data }
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
   * startPeer("python"); // InMoov i01 = (InMoov) Runtime.createAndStart("i01",
   * "InMoov");
   * 
   * HashSet<String> keepMeRunning = new HashSet<String>(Arrays.asList("i01",
   * "gui", "runtime", "python", getName()));
   * 
   * for (int i = 0; i < list.size(); ++i) { String r = list.get(i).getName();
   * if (r.startsWith("InMoov2")) { warn("testing script %s", r); String script
   * = FileIO.resourceToString(String.format("Python/examples/%s", r));
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
   * startPeer("python"); // InMoov i01 = (InMoov) Runtime.createAndStart("i01",
   * "InMoov");
   * 
   * HashSet<String> keepMeRunning = new HashSet<String>(Arrays.asList("i01",
   * "gui", "runtime", "python", getName()));
   * 
   * for (int i = 0; i < list.size(); ++i) { String r = list.get(i).getName();
   * if (r.startsWith("InMoov2")) { warn("testing script %s", r); String script
   * = FileIO.resourceToString(String.format("Python/examples/%s", r));
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
   * Status status = Status.info("starting %s %s junit", getName(), getType());
   * File buildFile = new File("build.xml"); Project p = new Project();
   * p.setUserProperty("ant.file", buildFile.getAbsolutePath()); p.init();
   * ProjectHelper helper = ProjectHelper.getProjectHelper();
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

  /*
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

  // FIXME - all tests must start with test .. so testSerialization
  ArrayList<String> serializationTestFailures = new ArrayList<String>();

  String pythonServiceScriptDir;

  public TestData testSerialization(TestData test) {
    log.info("serializeTest {}", test.serviceName);
    String name = test.serviceName;
    if (name == null) {
      log.warn("Name was null on serialize test?!?");
    }
    // multiple formats binary json xml
    // Status status = Status.info("serializeTest for %s", name);

    try {
      if (name.equals("org.myrobotlab.service.Plantoid")){
        log.info("her name is my name too");
      }
      ServiceInterface s = Runtime.start(name, name);
      
      if (s == null){
        test.status = Status.error("could not create %s", name);
        return test;
      }

      // native serialization
      ByteArrayOutputStream fos = null;
      ObjectOutputStream out = null;
      fos = new ByteArrayOutputStream();
      out = new ObjectOutputStream(fos);
      out.writeObject(s);
      fos.flush();
      out.close();

      // json serialization
      CodecUtils.toJson(s);

      s.releaseService();

    } catch (Exception e) {
      // serializationTestFailures
      test.status = Status.error(e);
    }

    return test;
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

  /*
   * need to do a type conversion here... in JS land there is no HashSet
   * &lt;String&gt; only List &amp; HashMap types
   * 
   */
  public void loadTests(List<String> servicesToTest, List<String> testsToRun) {
    // clear results ???
    matrix.servicesToTest.clear();
    matrix.testsToRun.clear();
    testQueue.clear();
    for (int i = 0; i < servicesToTest.size(); ++i) {
      matrix.servicesToTest.add(servicesToTest.get(i));
    }
    for (int i = 0; i < testsToRun.size(); ++i) {
      matrix.testsToRun.add(testsToRun.get(i));
    }
    matrix.currentProgress.totalTests = matrix.servicesToTest.size() * matrix.testsToRun.size();
  }

  /**
   * moves the prepared tests to the test queue so the "tester" can run them all
   */
  public void test() {
    for (String serviceName : matrix.servicesToTest) {
      for (String testName : matrix.testsToRun) {
        try {
          testQueue.put(new TestData(serviceName, testName));
        } catch (InterruptedException e) {
        }
      }
    }

  }

  public static class Tester extends Thread {
    transient Test test;

    public Tester(Test test) {
      super("tester");
      this.test = test;
      start();
    }

    public void run() {
      test.runTests();
    }
  }

  /**
   * FIXME - add TestSuite test the matrix - the test matrix consists of all
   * services to be tested by all selected services FIXME - a TestSuite is a
   * complete run of the matrix with all results saved to a test suite - which
   * includes the all the tests and services at that time tested and their
   * results
   */
  synchronized public void runTests() {
    matrix.isRunning = true;

    // broadcasting starting progress of testing
    Progress progress = new Progress();
    matrix.currentProgress = progress;
    invoke("publishProgress", progress);

    int total = matrix.testsToRun.size() * matrix.servicesToTest.size();

    progress.totalTests = total;
    progress.percentDone = 0;
    progress.currentActivity = String.format("starting %d tests", total);
    boolean done = false;

    while (!done) {
      TestData test = null;
      try {
        test = testQueue.take();
      } catch (InterruptedException e) {
      }
      String testName = test.testName;
      String serviceName = test.serviceName;

      String activity = String.format("test %s on %s", testName, serviceName);
      log.info(activity);
      progress.currentActivity = activity;
      invoke("publishProgress", progress);

      // do the TEST !!
      invoke(test.testName, test);

      test.endTime = System.currentTimeMillis();

      // broadcast incremental progress
      progress.process(test.status);

      if (test.status.isError()) {
        errors.add(test);
      }

      broadcastState(); // admittedly a bit heavy handed

      activity = String.format("tested %s(%s)", testName, test.serviceName);
      invoke("publishProgress", progress);

      if (testQueue.size() == 0) {
        log.info("test queue size is 0 - finished testing");
        done = true;
        log.info("error count {} errors {}", errors.size(), errors);
      }
    }

  }

  /**
   * creates the default set of tests which include all "available" services
   * tested by all tests, call test() would then move all tests over to the test
   * queue
   */
  public void loadDefaultTests() {

    ArrayList<ServiceType> types = serviceData.getServiceTypes();
    for (int i = 0; i < types.size(); ++i) {
      ServiceType type = types.get(i);

      // FIXME - switch to include nonAvailable ??? global field in service
      if (!type.isAvailable()) {
        nonAvailableServices.add(type.getSimpleName());
        continue;
      }
      log.info("adding {}", type.getSimpleName());
      matrix.servicesToTest.add(type.getSimpleName());
    }
    log.info("non available services {} {}", nonAvailableServices.size(), nonAvailableServices);
    log.info("testing {} available services out of {}", matrix.servicesToTest.size(), types.size());

    String[] methods = getDeclaredMethodNames();
    for (int i = 0; i < methods.length; ++i) {
      if (methods[i].startsWith("test") && methods[i].length() > 4) {
        matrix.testsToRun.add(methods[i]);
      }
    }

    matrix.currentProgress.totalTests = matrix.servicesToTest.size() * matrix.testsToRun.size();
  }

  public List<String> getAllServiceNames() {
    String[] x = serviceData.getServiceTypeNames();
    List<String> serviceNames = new ArrayList<String>();
    for (int i = 0; i < x.length; ++i) {
      serviceNames.add(x[i]);
    }
    return serviceNames;
  }

  /**
   * The outer level of all tests on a per Service basis Environment is expected
   * to be prepared correctly by an Agent. This method will test the heck out of
   * a single service and save the results in a partFile
   * 
   * FIXME - test dependencies (out of process) - use an agent ?
   * 
   */
  public List<Status> garbageStartAndRelease(String serviceType) {

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
    // meta.addPeer("python", "Python", "python to excercise python scripts");
    return meta;
  }

  public void startAndReleaseTest(String serviceType) {

  }

  public void onFinishedPythonScript(String result) {
    log.info("DONE !");
    log.info("onFinishedPythonScript - {}", result);
    // we finished !
    lock.status = Status.success();
    synchronized (lock) {
      lock.notifyAll();
    }
  }

  // TODO - load python
  public TestData testPythonScriptExists(TestData test) {

    try {
      String name = test.serviceName;
      String script = GitHub.getPyRobotLabScript(test.branch, name);

      // String branch = Platform.getLocalInstance().getBranch();
      // https://github.com/MyRobotLab/pyrobotlab/edit/develop/service/AcapelaSpeech.py
      String url = String.format("https://github.com/MyRobotLab/pyrobotlab/edit/%s/service/%s.py", test.branch, name);
      test.link = String.format("<a href=\"%s\">%s</a>", url, test.testName);

      if (script == null) {
        test.status = Status.error("script not found");
        /*
         * ServiceData sd = ServiceData.getLocalInstance(); ServiceType st =
         * sd.getServiceType(test.fullTypeName); StringBuffer t = new
         * StringBuffer();
         * t.append("#########################################\n");
         * t.append(String.format("# %s.py\n", name));
         * t.append(String.format("# description: %s\n", st.getDescription()));
         * t.append(String.format("# categories: %s\n",
         * Arrays.toString(st.categories.toArray(new
         * String[st.categories.size()])))); t.append(String.
         * format("# possibly more info @: http://myrobotlab.org/service/%s\n",
         * name)); t.append("#########################################\n");
         * t.append("# start the service\n"); String lowercase =
         * name.toLowerCase();
         * t.append(String.format("%s = Runtime.start(\"%s\",\"%s\")",
         * lowercase, lowercase, name)); FileIO.toFile(new
         * File(String.format("%s.py", name)), t.toString().getBytes());
         */
      } else {
        test.status = Status.success();
      }

    } catch (Exception e) {
      test.status = Status.error(e);
    }
    return test;
  }

  // TODO - BasicCreateStartStopRelease - over network control
  public TestData JunitService(TestData test) {

    try {

      /*
       * Suite suite = new Suite(klass, new RunnerBuilder() { ... // Implement
       * methods }); JUnitCore c = new JUnitCore();
       * c.run(Request.runner(suite));
       */

      Class<?> junitTest = Class.forName(String.format("org.myrobotlab.service.%sTest", test.serviceName));
      JUnitCore junit = new JUnitCore();
      Result junitResult = junit.run(junitTest);
      log.info("JUnit Result : {}", junitResult);

      // result.link = String.format("<a href=\"%s\">%s</a>", url,
      // testName);
      /*
       * if (servicePage == null || servicePage.contains("Page not found" )) {
       * result.status = Status.error("script not found"); } else {
       * result.status = Status.success(); }
       */

      test.status = Status.success();

    } catch (Exception e) {
      test.status = Status.error(e);
    }

    return test;
  }

  public TestData testServicePageExists(TestData test) {
    try {

      HttpClient http = (HttpClient) startPeer("http");

      String n = test.serviceName;
      String url = String.format("http://myrobotlab.org/service/%s", n);
      String servicePage = http.get(url);

      test.link = String.format("<a href=\"%s\">%s</a>", url, test.testName);
      if (servicePage == null || servicePage.contains("Page not found")) {
        test.status = Status.error("script not found");
      } else {
        test.status = Status.success();
      }

    } catch (Exception e) {
      test.status = Status.error(e);
    }

    return test;
  }

  public Map<String, String> getPyRobotLabServiceScripts(String branch) throws Exception {

    if (pythonScripts == null) {

      pythonScriptsWithNoServiceType.clear();

      HashSet<String> serviceTypes = new HashSet<String>();

      pythonScripts = new TreeMap<String, String>();
      List<ServiceType> sts = serviceData.getServiceTypes();
      for (int i = 0; i < sts.size(); ++i) {
        ServiceType st = sts.get(i);
        serviceTypes.add(st.getSimpleName());
        String script = GitHub.getPyRobotLabScript(branch, st.getSimpleName());
        if (script != null) {
          pythonScripts.put(st.getSimpleName(), script);

        } else {
          log.info("{}<br>", st.getSimpleName());
          servicesNeedingPythonScripts.add(st.getSimpleName());
        }
      }
      Set<String> gitHubServiceScripts = GitHub.getServiceScriptNames();
      for (String key : gitHubServiceScripts) {
        String serviceName = key.substring(0, key.lastIndexOf("."));
        if (!serviceTypes.contains(serviceName)) {
          pythonScriptsWithNoServiceType.add(key);
        }
      }
      log.info("remove scripts - script found but no service type {}", pythonScriptsWithNoServiceType);

    }
    return pythonScripts;
  }

  public List<String> getServicesWithOutServicePages() throws ClientProtocolException, IOException {
    ArrayList<String> ret = new ArrayList<String>();
    ArrayList<ServiceType> serviceTypes = serviceData.getServiceTypes();
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

  public List<String> getServicesWithOutScripts(String branch) {
    try {

      getPyRobotLabServiceScripts(branch);
      return servicesNeedingPythonScripts;
    } catch (Exception e) {
      log.error("getServicesWithOutScripts threw", e);
    }
    return null;
  }

  public void generatePythonScripts(String branch) {
    try {
      List<String> services = getServicesWithOutScripts(branch);
      File dir = new File(String.format("generatedPython/%s", branch));
      dir.mkdirs();

      for (String service : services) {
        FileOutputStream fos = new FileOutputStream(String.format("generatedPython/%s/%s.py", branch, service));
        StringBuilder t = new StringBuilder("");
        /*
         * t.append("###########################################");
         * t.append("# " + service + ".py service python script\n");
         * t.append("# more information @\n");
         * t.append("# http://myrobotlab.org/service/" + service +
         * " service page\n");
         * t.append("###########################################\n");
         * t.append("\n"); t.append("# start the service\n");
         */

        ServiceType st = serviceData.getServiceType(String.format("org.myrobotlab.service.%s", service));

        t.append("#########################################\n");
        t.append(String.format("# %s.py\n", service));
        t.append(String.format("# description: %s\n", st.getDescription()));
        t.append(String.format("# categories: %s\n", Arrays.toString(st.categories.toArray(new String[st.categories.size()])).replace("[", "").replaceAll("]", "")));
        t.append(String.format("# more info @: http://myrobotlab.org/service/%s\n", service));
        t.append("#########################################\n\n");
        t.append("# start the service\n");
        String lowercase = service.toLowerCase();
        t.append(String.format("%s = Runtime.start('%s','%s')\n", lowercase, lowercase, service));
        // FileIO.toFile(new File(String.format("%s.py", service)),
        // t.toString().getBytes());

        // t.append(String.format("%s = Runtime.start('%s','%s')\n", lower,
        // lower, service));
        fos.write(t.toString().getBytes());
        fos.close();
      }
    } catch (Exception e) {
      log.error("generatePythonScripts threw", e);
    }
  }

  /*
   * Gets all the pyrobotlab/service/scripts and does some basic testing. This
   * method also finds all script not associated with active services (to be
   * removed). And all services which do not have scripts (to be added) It runs
   * in the same process as Python and the expectation is the Agent (with the
   * help of the Test service) has created an environment where the service to
   * be tested has all its depedencies
   * 
   * 
   * @throws Exception
   * 
   * FIXME - need to change to testPythonScript(serviceName) .. because only 1
   * will be run in a 'clean' environment ...
   * 
   * FIXME - structured logging back to self to generate report
   */

  // FIXME !!!! - refactor - the outer part of this script is handling service
  // creation and destruction through inventory
  public TestData testPythonScriptTest(TestData test) throws Exception {

    // String testName = new Object(){}.getClass().getEnclosingMethod().getName(
    // identify the original services before the test
    String[] sn = Runtime.getServiceNames();
    HashSet<String> preServices = new HashSet<String>();
    for (String s : sn) {
      preServices.add(s);
    }

    // a test will resolve in 3 possible states
    // 1. complete & success - with onFinish callback called
    // 2. with an error
    // 3. with a time out

    // all three need to be handled

    // FIXME - change location (option to test locally)
    String script = null;
    if (pythonServiceScriptDir != null) {
      String scriptFilename = pythonServiceScriptDir + test.serviceName + ".py";
      script = FileIO.toString(new File(scriptFilename));
    } else {
      script = GitHub.getPyRobotLabScript(test.branch, test.serviceName);
    }
    // String script = FileIO.toString(new
    // File(String.format("../pyrobotlab/service/%s.py", st.getSimpleName())));

    if (script == null) {
      test.status = Status.error("script does not exist");
      return test;
    }

    test.status = Status.success();

    pythonTestScript(script, test.testName, test);

    // find the current services now registered
    sn = Runtime.getServiceNames();
    HashSet<String> currentServices = new HashSet<String>();
    for (String s : sn) {
      currentServices.add(s);
    }

    for (String cs : currentServices) {
      if (!preServices.contains(cs)) {
        log.info(String.format("service %s created for testing %s - test finished attempting to release", cs, test.serviceName));
        ServiceInterface si = Runtime.getService(cs);
        si.releaseService();
      }
    }

    log.info("TESTING COMPLETED");

    return test;

  }

  public TestData pythonTestScript(String script, String testName, TestData test) {

    String serviceName = test.serviceName;
    log.info("TESTING SCRIPT {} - quiet on the set please...", serviceName);

    Python python = (Python) Runtime.start("python", "Python");
    subscribe(python.getName(), "publishStatus");

    try {

      StringBuilder prePendScript = new StringBuilder();
      prePendScript.append("virtual = True\n");

      /*
       * if (skippedPythonScript.contains(serviceName)) { log.info(
       * "SKIPPING {} ....", serviceName); continue; }
       */

      // challenge #1
      // I would prefer to be in the same
      // process as python when things are executed
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
      python.exec(prePendScript.toString() + script + callback);// , true,
                                                                // true);

      // script has at maximum 1 minute to return
      synchronized (lock) {
        long ts = System.currentTimeMillis();
        lock.wait(60000);
        if (System.currentTimeMillis() - ts >= 60000) {
          test.status = Status.error("script %s FAILED - took longer than 1 minute!", serviceName);
        } else {
          if (lock.status.isError()) {
            // the callback had error - set the result of the test
            test.status = lock.status;
            // reset the lock
            lock.status = Status.success();
          } else {
            log.info("script {} PASSED !", serviceName);
            test.status = Status.success("ba");
          }
        }
      }

      // max execution time ??? - then error

      log.info("inspect python errors (syntax) + java errors");

    } catch (Exception e) {
      log.error("python script failed", e);
      test.status = Status.error(e);
    }
    return test;
  }

  @Override
  public void onStatus(Status status) {
    synchronized (lock) {
      // Make sure Python syntax errors or exceptions fail test quickly
      lock.status = status;
      log.info("onStatus {}", status);
      lock.notifyAll();
    }
  }

  // TODO - subscribe to registered --> generates subscription to
  // publishState() - filter on Errors
  // FIXME - FILE COMMUNICATION !!!!
  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    try {
      Test test = (Test) Runtime.start("test", "Test");
      test.pythonServiceScriptDir = "../pyrobotlab/service/";
      // Runtime.start("webgui","WebGui");
      // Runtime.start("gui","SwingGui");
      // Runtime.start("python", "Python");

      List<String> servicesToTest = new ArrayList<String>();
      List<String> testsToRun = new ArrayList<String>();

      // FIXME - must add branch !!
      // servicesToTest.add("ImageDisplay");
      servicesToTest.add("Plantoid");
      // testsToRun.add("PythonScriptTest");
      testsToRun.add("testSerialization");

      // setting up new tests to run in matrix
      test.loadTests(test.getAllServiceNames(), testsToRun);
      // test.loadTests(servicesToTest, testsToRun);
      test.test();

      boolean done = true;
      if (done) {
        return;
      }

      ArrayList<ServiceType> sts = serviceData.getServiceTypes();

      for (ServiceType st : sts) {
        // FIXME - script folder location as seperate input
        log.info("testing {}", st.getSimpleName());
        String script = FileIO.toString(new File(String.format("../pyrobotlab/service/%s.py", st.getSimpleName())));
        TestData testdata = new TestData(st.getSimpleName(), "PythonTestScript");
        test.pythonTestScript(script, "PythonTestScript", testdata);

        log.info("results {}", testdata);
      }

      // Runtime.start("webgui", "WebGui");

      // List<Status> results = test.test("UltrasonicSensor");
      // log.info(arg0);
      // TestResults results = new TestResults();
      // test.PythonScriptTest("develop",

      // Runtime.start("webgui", "WebGui");

      // test.generatePythonScripts("develop");
      // test.test();
      // test.test("InMoov");

      List<String> all = test.getAllServiceNames();
      StringBuffer sb = new StringBuffer();
      for (String name : all) {
        sb.append(String.format("%s\n", name));
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

}

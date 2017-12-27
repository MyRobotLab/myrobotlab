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
 * TODO - Agent "spawned" testing for dependency and process isolation
 * 
 * TODO - Test for checking if all dependencies download "install all" test
 * 
 * TODO - grab and report all missing Service Pages &amp; all missing
 * Python scripts !
 * 
 * TODO - install create start stop release test TODO - serialization
 *         json + native test TODO - run Python &amp; JavaScript tests - last
 *         method appended is a callback
 *
 */
public class Test extends Service implements StatusListener {
  
  /**
   * filter services by availabilities 
   */
  public boolean showUnavailableServices=false;
  
  /**
   * services which are necessary to conduct the test and will NOT
   * be released after cleaning up a test
   */
  Set<String> globalServices = null;

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
    
    

    // FIXME - most of these can be removed as the "TestData" failure is saved for each failed test
    List<String> servicesWithErrors = new ArrayList<String>();

    // stats & accumulators python
    List<String> servicesNeedingPythonScripts = new ArrayList<String>();

    List<String> pythonScriptsWithNoServiceType = new ArrayList<String>();

    List<String> passedPythonScripts = new ArrayList<String>();    

    // service indexed list of tests which were processed
    Map<String, Set<TestData>> results = new TreeMap<String, Set<TestData>>();
    
    // TODO - before test init

    public void process(TestData test) {
      Status status = test.status;
      testsDone++;
      percentDone = testsDone * 100 / totalTests;

      if (status.isError()) {
        errorCount++;
      } else {
        successes++;       
      }
      errorPercentage = errorCount * 100 / testsDone;
      successPercentage = successes * 100 / testsDone;

      Set<TestData> testSet = null;
      if (results.containsKey(test.serviceName)) {
        testSet = results.get(test.serviceName);
      } else {
        testSet = new HashSet<TestData>();
        results.put(test.serviceName, testSet);
      }

      testSet.add(test);

    }

  }

  public static class StatusLock {
    public Status status;
  }

  public static class TestData implements Serializable {
    private static final long serialVersionUID = 1L;

    String testName;
    String serviceName;
    Long startTime;
    Long endTime;
    Status status = Status.success();
    Boolean isRunning;

    public String link;

    public String branch;

    public TestData(String serviceName, String testName) {
      this.serviceName = serviceName;
      this.testName = testName;
      this.link = testName;
    }
    
    public void start(){
      this.startTime = System.currentTimeMillis();
      this.isRunning = true;
    }
    
    public void stop(){
      this.startTime = System.currentTimeMillis();
      this.isRunning = false;
    }


    public String toString() {
      return String.format("=== TEST ===> testName: %s service: %s/%s status: %s", testName, branch, serviceName, status);
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
      log.info("Tester is done - leaving");
    }
  }

  // matrices - set of all matrixes tested
  // FIXME - should be ALL (as in list of) all the test info
  public static class TestMatrix implements Serializable {

    private static final long serialVersionUID = 1L;
    Progress currentProgress = new Progress();
    TestData currentTest = null;
    TestData lastTest = null;

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

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Test.class);

  static final transient ServiceData serviceData = ServiceData.getLocalInstance();

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
    meta.setAvailable(false);
    // meta.addPeer("python", "Python", "python to excercise python scripts");
    return meta;
  }
  
  /**
   * pre-test
   */
  synchronized public void globalInit(){
    if (globalServices != null){
      return;
    }
    
    // get list of services to NOT release after tests      
    globalServices = new TreeSet<String>(Arrays.asList(Runtime.getServiceNames()));
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

  // TODO - subscribe to registered --> generates subscription to
  // publishState() - filter on Errors
  // FIXME - FILE COMMUNICATION !!!!
  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    try {

      Test test = (Test) Runtime.start("test", "Test");
      test.pythonServiceScriptDir = "../pyrobotlab/service/";
      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser = false;
      webgui.startService();
      //Runtime.start("gui", "SwingGui");
      Runtime.start("python", "Python");
      webgui.startBrowser("http://localhost:8888/#/service/test");
      // Runtime.start("python", "Python");

      // setting up new tests to run in matrix
      // test.loadTests(test.getAllServiceNames(), testsToRun);
      // test.loadTests(servicesToTest, testsToRun);

      // test.loadTests("WebGui", "testSerialization");
      // test.test();

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  // state information
  transient Set<Thread> threads = null;
  
  /**
   * tests which have returned error
   */
  List<TestData> errors;
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

  // thread blocking
  transient StatusLock lock = new StatusLock();


  transient TreeMap<String, String> pythonScripts = null;

  // FIXME remove ...
  transient LinkedBlockingQueue<Object> data = new LinkedBlockingQueue<Object>();

  TestMatrix matrix;
  
  
  /**
   * list of possible test methods - all begin with "test"{Method}(TestData
   * test)
   */
  List<String> tests = new ArrayList<String>();

  // FIXME - all tests must start with test .. so testSerialization
  ArrayList<String> serializationTestFailures = new ArrayList<String>();

  List<ServiceType> services = null;

  String pythonServiceScriptDir;

  public Test(String n) {
    super(n);

    // protecting previously serialized data
    if (matrix == null){
      matrix = new TestMatrix();
    }
    
    if (errors == null){
      errors = new ArrayList<TestData>();
    }
    
    // get all possible tests
    String[] methods = getDeclaredMethodNames();
    for (int i = 0; i < methods.length; ++i) {
      if (methods[i].startsWith("test") && methods[i].length() > 4) {
        tests.add(methods[i]);
      }
    }

    // get all possible services
    services = getServices();
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

  public List<String> getAllServiceNames() {
    String[] x = serviceData.getServiceTypeNames();
    List<String> serviceNames = new ArrayList<String>();
    for (int i = 0; i < x.length; ++i) {
    }
    return serviceNames;
  }

  public Map<String, String> getPyRobotLabServiceScripts(String branch) throws Exception {

    if (pythonScripts == null) {

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
          matrix.currentProgress.servicesNeedingPythonScripts.add(st.getSimpleName());
        }
      }
      Set<String> gitHubServiceScripts = GitHub.getServiceScriptNames();
      for (String key : gitHubServiceScripts) {
        String serviceName = key.substring(0, key.lastIndexOf("."));
        if (!serviceTypes.contains(serviceName)) {
          matrix.currentProgress.pythonScriptsWithNoServiceType.add(key);
        }
      }
      log.info("remove scripts - script found but no service type {}", matrix.currentProgress.pythonScriptsWithNoServiceType);

    }
    return pythonScripts;
  }

  public List<ServiceType> getServices() {
    return serviceData.getServiceTypes(showUnavailableServices);
  }

  public List<String> getServicesWithOutScripts(String branch) {
    try {

      getPyRobotLabServiceScripts(branch);
      return matrix.currentProgress.servicesNeedingPythonScripts;
    } catch (Exception e) {
      log.error("getServicesWithOutScripts threw", e);
    }
    return null;
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
   * used to get state of the current service and runtime - so that the
   * environment and final system can be cleaned to an original "base" state
   */
  public void getState() {
    try {
      threads = Thread.getAllStackTraces().keySet();
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public void loadAndRunTests(List<String> servicesToTest, List<String> testsToRun) {
    loadTests(servicesToTest, testsToRun);
    test();
  }

  /**
   * creates the default set of tests which include all services
   * tested by all tests, call test() would then move all tests over to the test
   * queue
   */
  public void loadDefaultTests() {

    ArrayList<ServiceType> types = serviceData.getServiceTypes();
    for (int i = 0; i < types.size(); ++i) {
      ServiceType type = types.get(i);
      log.info("adding {}", type.getSimpleName());
      matrix.servicesToTest.add(type.getSimpleName());
    }

    for (String test : tests) {
      matrix.testsToRun.add(test);
    }
    matrix.currentProgress.totalTests = matrix.servicesToTest.size() * matrix.testsToRun.size();
  }

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

  public void loadTests(String serviceName, String testName) {
    List<String> servicesToTest = new ArrayList<String>();
    servicesToTest.add(serviceName);
    List<String> testsToRun = new ArrayList<String>();
    testsToRun.add(testName);
    loadTests(servicesToTest, testsToRun);
  }

  public void loadTests(String[] serviceNames, String[] testNames) {
    List<String> servicesToTest = new ArrayList<String>();
    for (int i = 0; i < serviceNames.length; ++i) {
      servicesToTest.add(serviceNames[i]);
    }

    List<String> testsToRun = new ArrayList<String>();
    for (int i = 0; i < testNames.length; ++i) {
      testsToRun.add(testNames[i]);
    }
    loadTests(servicesToTest, testsToRun);
  }

 /**
  * call-back from service under testing to route errors to this service...
  * @param errorMsg error message
  */
  public void onError(String errorMsg) {
    if (matrix.currentTest != null){
      matrix.currentTest.status = Status.error(errorMsg);
    }
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

  @Override
  public void onStatus(Status status) {
    synchronized (lock) {
      // Make sure Python syntax errors or exceptions fail test quickly
      lock.status = status;
      log.info("onStatus {}", status);
      lock.notifyAll();
    }
  }

  public Progress publishProgress(Progress progress) {
    return progress;
  }

  public TestData pythonTestScriptXXX(String script, String testName, TestData test) {

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

  public void registered(ServiceInterface sw) {

    subscribe(sw.getName(), "publishError");
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

    progress.totalTests = matrix.testsToRun.size() * matrix.servicesToTest.size();

    progress.percentDone = 0;
    progress.currentActivity = String.format("starting %d tests", progress.totalTests);

    while (matrix.isRunning) {
      TestData test = null;
      try {
        test = testQueue.take();
      } catch (InterruptedException e) {
      }
      
      matrix.currentTest = test;
      String testName = test.testName;
      String serviceName = test.serviceName;
      
      if (test.branch == null){
        test.branch = Platform.getLocalInstance().getBranch();
        log.info(String.format("Testing on branch ", test.branch));
      }

      String activity = String.format("test %s on %s", testName, serviceName);
      log.info(activity);
      progress.currentActivity = activity;
      
      invoke("publishProgress", progress);

      // save just before processing the test
      // in case things don't work out - we have
      save();
      
      // do the TEST !!
      test.start();
      invoke(test.testName, test);
      test.stop();

      test.endTime = System.currentTimeMillis();
      
      matrix.lastTest = matrix.currentTest;
      matrix.currentTest = null;
      
      // broadcast incremental progress
      progress.process(test);

      if (test.status.isError()) {
        errors.add(test);
      }
      
      // broadcastState(); // admittedly a bit heavy handed

      activity = String.format("tested %s(%s)", testName, test.serviceName);
      invoke("publishProgress", progress);

      if (testQueue.size() == 0) {
        log.info("test queue size is 0 - finished testing");
        log.info("error count {} errors {}", errors.size(), errors);
      }
    }
  }

  public void startAndReleaseTest(String serviceType) {

  }

  public void startService() {
    super.startService();
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

  public void stopTest() {
    matrix.isRunning = true;
  }

  public Object subscribe(Object inData) {
    log.info("subscribe has received data");
    log.info(String.format("Test.subscribed received %s", inData));
    data.add(inData);
    return inData;
  }

  /**
   * moves the prepared tests to the test queue so the "tester" can run them all
   */
  public void test() {
   //  globalInit(); WTF ??
    for (String serviceName : matrix.servicesToTest) {
      for (String testName : matrix.testsToRun) {
        try {
          // create an put all the tests on the test queue
          testQueue.put(new TestData(serviceName, testName));
        } catch (InterruptedException e) {
        }
      }
    }

    // create the tester
    tester = new Tester(this);
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

  // TODO - BasicCreateStartStopRelease - over network control
  public TestData testJUnit(TestData test) {

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

  // FIXME !!!! - refactor - the outer part of this script is handling service
  // creation and destruction through inventory
  public TestData testPythonScript(TestData test) throws Exception {

	  log.info("testPythonScript {}", test.toString());

    // TEST SETUP BEGIN .....
    Python python = (Python) Runtime.start("python", "Python");

    // identify the original services before the test
    String[] sn = Runtime.getServiceNames();
    HashSet<String> preServices = new HashSet<String>();
    for (String s : sn) {
      preServices.add(s);
    }
    
    Set<Thread> preThreads = Thread.getAllStackTraces().keySet();
    
    log.info("pre test {} services {} threads {}", sn.length, Arrays.toString(sn), preThreads.size());

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

    // TEST SETUP END ....
    
    ///////////// BEGIN //////////////////////////

    String serviceName = test.serviceName;
    log.info("TESTING SCRIPT {} - quiet on the set please...", serviceName);

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

      // script has at maximum 1 minute to return
      synchronized (lock) {
        long ts = System.currentTimeMillis();

        // by default - python will create a new thread
        // to execute the script
        python.exec(prePendScript.toString() + script + callback);// , true,
                                                                  // true);
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
            String activity = String.format("script {} PASSED !", serviceName);
            log.info(activity);
            test.status = Status.success(activity);
          }
        }
      }
    } catch (Exception e) {
      log.error("python script failed", e);
      test.status = Status.error(e);
    }

    ///////////// END //////////////////////////

    // find the current services now registered
    sn = Runtime.getServiceNames();
    HashSet<String> currentServices = new HashSet<String>();
    for (String s : sn) {
      currentServices.add(s);
    }

    // tear down and release all newly created services
    for (String cs : currentServices) {
      if (!preServices.contains(cs)) {
        log.info(String.format("service %s created for testing %s - test finished attempting to release", cs, test.serviceName));
        ServiceInterface si = Runtime.getService(cs);
        if (si != null){
        	si.releaseService();
        }
      }
    }
    
    // get services and threads after releasing all services involved in testing
    sn = Runtime.getServiceNames();
    Set<Thread> postThreads = Thread.getAllStackTraces().keySet();
    
    log.info("post test {} services {} threads {}", sn.length, Arrays.toString(sn), postThreads.size());
    
    /*
    for (Thread t: postThreads){
      if (preThreads.contains(t)){
        if (t.getName().startsWith("New I/O")){
          continue;
        }
        log.error("dirty release - thread [{}] {} {} not released, interrupting it !", t.getName(), t.getId(), t.isAlive());
        t.interrupt();
        // t.stop();
      }
    }
    */

    log.info("TESTING COMPLETED");

    return test;

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

  public TestData testSerialization(TestData test) {
    // log.info("serializeTest {}", test.serviceName);
	  log.info("testSerialization {}", test.toString());
    String name = test.serviceName;
    if (name == null) {
      log.warn("Name was null on serialize test?!?");
    }
    // multiple formats binary json xml
    // Status status = Status.info("serializeTest for %s", name);

    try {

      ServiceInterface s = Runtime.start(name, name);

      if (s == null) {
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
}

package org.myrobotlab.test;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class AbstractTest {

  private static long coolDownTimeMs = 100;

  /** cached network test value for tests */
  static Boolean hasInternet = null;

  static boolean install = true;

  protected static boolean installed = true;

  public final static Logger log = LoggerFactory.getLogger(AbstractTest.class);

  static private boolean logWarnTestHeader = false;

  private static boolean releaseRemainingServices = true;

  private static boolean releaseRemainingThreads = false;
  
  protected transient Queue<Object> queue = new LinkedBlockingQueue<>();

  static transient Set<Thread> threadSetStart = null;
  
  protected Set<Attachable> attached = new HashSet<>();

  protected boolean printMethods = true;

  @Rule
  public final TestName testName = new TestName();
  static public String simpleName;
  private static boolean lineFeedFooter = true;

  public String getSimpleName() {
    return simpleName;
  }

  public String getName() {
    return testName.getMethodName();
  }

  static public boolean hasInternet() {
    if (hasInternet == null) {
      hasInternet = Runtime.hasInternet();
    }
    return hasInternet;
  }

  static public boolean isHeadless() {
    return Runtime.isHeadless();
  }

  public static void main(String[] args) {
    try {
      AbstractTest test = new AbstractTest();
      // LoggingFactory.init("INFO");

      ChaosMonkey.giveToMonkey(test, "testFunction");
      ChaosMonkey.giveToMonkey(test, "testFunction");
      ChaosMonkey.giveToMonkey(test, "testFunction");
      ChaosMonkey.startMonkeys();
      ChaosMonkey.monkeyReport();
      log.info("here");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  // super globals - probably better not to use the mixin - but just initialize
  // statics in the
  // constructor of the AbstractTest
  @BeforeClass
  public static void setUpAbstractTest() throws Exception {

    String junitLogLevel = System.getProperty("junit.logLevel");
    if (junitLogLevel != null) {
      Runtime.setLogLevel(junitLogLevel);
    } else {
      Runtime.setLogLevel("warn"); // error instead ?
    }

    log.info("setUpAbstractTest");
    if (threadSetStart == null) {
      threadSetStart = Thread.getAllStackTraces().keySet();
    }
  }
  
  static public List<String> getThreadNames(){
    List <String> ret = new ArrayList<>();
    Set<Thread> tds = Thread.getAllStackTraces().keySet();
    for (Thread t : tds ) {
      ret.add(t.getName());
    }
    return ret;
  }

  static public void sleep(int sleepMs) {
    try {
      Thread.sleep(sleepMs);
    } catch (InterruptedException e) {
      // don't care
    }
  }

  public static void sleep(long sleepTimeMs) {
    try {
      Thread.sleep(coolDownTimeMs);
    } catch (Exception e) {

    }
  }

  @AfterClass
  public static void tearDownAbstractTest() throws Exception {
    log.info("tearDownAbstractTest");

    if (releaseRemainingServices) {
      releaseServices();
    }

    if (logWarnTestHeader) {
      log.warn("=========== finished test {} ===========", simpleName);
    }

    if (lineFeedFooter) {
      System.out.println();
    }
  }

  protected void installAll() throws ParseException, IOException {
    if (!installed) {
      log.warn("installing all services");
      Runtime.install();
      installed = true;
    }
  }

  public static void releaseServices() {

    // services to be cleaned up/released
    String[] services = Runtime.getServiceNames();
    Set<String> releaseServices = new TreeSet<>();
    for (String service : services) {
      // don't kill runtime - although in the future i hope this is possible
      if (!"runtime".equals(service)) {
        releaseServices.add(service);
        log.info("service {} left in registry - releasing", service);
        Runtime.releaseService(service);
      }
    }

    if (releaseServices.size() > 0) {
      log.info("attempted to release the following {} services [{}]", releaseServices.size(), String.join(",", releaseServices));
      log.info("cooling down for {}ms for dependencies with asynchronous shutdown", coolDownTimeMs);
      sleep(coolDownTimeMs);
    }

    // check threads - kill stragglers
    // Set<Thread> stragglers = new HashSet<Thread>();
    Set<Thread> threadSetEnd = Thread.getAllStackTraces().keySet();
    Set<String> threadsRemaining = new TreeSet<>();
    for (Thread thread : threadSetEnd) {
      if (!threadSetStart.contains(thread) && !"runtime_outbox_0".equals(thread.getName()) && !"runtime".equals(thread.getName())) {
        if (releaseRemainingThreads) {
          log.info("interrupting thread {}", thread.getName());
          thread.interrupt();
          /*
           * if (useDeprecatedThreadStop) { thread.stop(); }
           */
        } else {
          // log.warn("thread {} marked as straggler - should be killed",
          // thread.getName());
          threadsRemaining.add(thread.getName());
        }
      }
    }
    if (threadsRemaining.size() > 0) {
      log.info("{} straggling threads remain [{}]", threadsRemaining.size(), String.join(",", threadsRemaining));
    }
    log.info("finished the killing ...");
  }

  public AbstractTest() {
    
    // default : make testing environment "virtual"
    Platform.setVirtual(true);
    
    simpleName = this.getClass().getSimpleName();
    if (logWarnTestHeader) {
      log.warn("=========== starting test {} ===========", this.getClass().getSimpleName());
    }
    if (install) {
      try {
        installAll();
      } catch (Exception e) {
        log.error("installing services failed");
        fail("installing service failed");
      }
    }
    
    // log.warn("=====java.library.path===== [{}]", System.getProperty("java.library.path"));
    // log.warn("=====jna.library.path===== [{}]", System.getProperty("jna.library.path"));
    
  }
  
  public void setVirtual() {
    Platform.setVirtual(true);
  }
  
  public boolean isVirtual() {
    return Platform.isVirtual();
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  public void testFunction() {
    log.info("tested testFunction");
  }

}

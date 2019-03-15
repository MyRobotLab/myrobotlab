package org.myrobotlab.test;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class AbstractTest {

  private static long coolDownTimeMs = 2000;
  /**
   * cached internet test value for tests
   */
  static Boolean hasInternet = null;

  public final static Logger log = LoggerFactory.getLogger(AbstractTest.class);

  static private boolean logTestHeader = true;

  private static boolean releaseRemainingServices = true;

  private static boolean releaseRemainingThreads = false;

  static transient Set<Thread> threadSetStart = null;
  
  protected boolean printMethods = true;

  // private static boolean useDeprecatedThreadStop = false;
  
  @Rule
  public final TestName testName = new TestName();
  static public String simpleName;
  
  public String getSimpleName() {
    return simpleName;
  }
  
  protected String getName() {
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

  static public boolean isVirtual() {
    boolean isVirtual = true;
    String isVirtualProp = System.getProperty("junit.isVirtual");

    if (isVirtualProp != null) {
      isVirtual = Boolean.parseBoolean(isVirtualProp);
    }
    return isVirtual;
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

    log.warn("=========== finished test {} ===========", simpleName);
  }
  
  protected void installAll() throws ParseException, IOException {
   Runtime.install();
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
      log.warn("attempted to release the following {} services [{}]", releaseServices.size(), String.join(",", releaseServices));
      log.warn("cooling down for {}ms for dependencies with asynchronous shutdown", coolDownTimeMs);
      sleep(coolDownTimeMs);
    }

    // check threads - kill stragglers
    // Set<Thread> stragglers = new HashSet<Thread>();
    Set<Thread> threadSetEnd = Thread.getAllStackTraces().keySet();
    Set<String> threadsRemaining = new TreeSet<>();
    for (Thread thread : threadSetEnd) {
      if (!threadSetStart.contains(thread) && !"runtime_outbox_0".equals(thread.getName()) && !"runtime".equals(thread.getName())) {
        if (releaseRemainingThreads) {
          log.warn("interrupting thread {}", thread.getName());
          thread.interrupt();
          /*
          if (useDeprecatedThreadStop) {
            thread.stop();
          }
          */
        } else {
          // log.warn("thread {} marked as straggler - should be killed", thread.getName());
          threadsRemaining.add(thread.getName());
        }
      }
    }
    if (threadsRemaining.size() > 0) {
      log.warn("{} straggling threads remain [{}]", threadsRemaining.size(), String.join(",", threadsRemaining));
    }
    log.info("finished the killing ...");
  }

  public AbstractTest() {
    simpleName = this.getClass().getSimpleName();
    if (logTestHeader) {
      log.warn("=========== starting test {} ===========", this.getClass().getSimpleName());
    }
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

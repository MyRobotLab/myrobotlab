package org.myrobotlab.test;

import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class AbstractTest {

  private static long coolDownTimeMs = 5000;
  /**
   * cached internet test value for tests
   */
  static Boolean hasInternet = null;

  public final static Logger log = LoggerFactory.getLogger(AbstractTest.class);

  static private boolean logTestHeader = true;

  private static boolean releaseRemainingServices = true;

  private static boolean releaseRemainingThreads = true;

  static transient Set<Thread> threadSetStart = null;
  private static boolean useDeprecatedThreadStop = false;

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
    log.warn("tearDownAbstractTest");

    if (releaseRemainingServices) {
      // services to be cleaned up/released
      StringBuilder sb = new StringBuilder();
      String[] services = Runtime.getServiceNames();
      for (String service : services) {
        if (!"runtime".equals(service)) {
          sb.append(service);
          sb.append(" ");
          log.warn("service {} left in registry - releasing", service);
          Runtime.releaseService(service);
        }
      }

      if (sb.length() > 0) {
        log.warn("had to release the following services {}", sb.toString());
        log.warn("cooling down for {}ms for dependencies with asynchronous shutdown", coolDownTimeMs);
        sleep(coolDownTimeMs);
      }
    }

    // check threads - kill stragglers
    // Set<Thread> stragglers = new HashSet<Thread>();
    Set<Thread> threadSetEnd = Thread.getAllStackTraces().keySet();
    for (Thread thread : threadSetEnd) {
      if (!threadSetStart.contains(thread)) {
        if (releaseRemainingThreads) {
          log.warn("killing thread {}", thread.getName());
          thread.interrupt();
          if (useDeprecatedThreadStop ) {
            thread.stop();
          }
        } else {
          log.warn("thread {} marked as straggler - should be killed", thread.getName());
        }
      }
    }

    log.warn("=========== finished test ===========");
  }

  public AbstractTest() {
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

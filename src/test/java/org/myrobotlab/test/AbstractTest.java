package org.myrobotlab.test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.config.RuntimeConfig;
import org.slf4j.Logger;

public class AbstractTest {

  /** cached network test value for tests */
  static Boolean hasInternet = null;

  protected static boolean installed = false;

  public final static Logger log = LoggerFactory.getLogger(AbstractTest.class);

  static private boolean logWarnTestHeader = false;

  private static boolean releaseRemainingThreads = false;

  protected transient Queue<Object> queue = new LinkedBlockingQueue<>();

  static transient Set<Thread> threadSetStart = null;

  protected Set<Attachable> attached = new HashSet<>();

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

  @BeforeClass
  public static void setUpAbstractTest() throws Exception {
    
    // setup runtime resource = src/main/resources/resource
    File runtimeYml = new File("data/conf/default/runtime.yml");
//    if (!runtimeYml.exists()) {
      runtimeYml.getParentFile().mkdirs();
      RuntimeConfig rc = new RuntimeConfig();
      rc.resource = "src/main/resources/resource";
      String yml = CodecUtils.toYaml(rc);
      
      FileOutputStream fos = null;
      fos = new FileOutputStream(runtimeYml);
      fos.write(yml.getBytes());
      fos.close();
      
//    }

    Platform.setVirtual(true);

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
    installAll();
  }

  static public List<String> getThreadNames() {
    List<String> ret = new ArrayList<>();
    Set<Thread> tds = Thread.getAllStackTraces().keySet();
    for (Thread t : tds) {
      ret.add(t.getName());
    }
    return ret;
  }

  public static void sleep(long sleepTimeMs) {
    try {
      Thread.sleep(sleepTimeMs);
    } catch (Exception e) {
    }
  }

  @AfterClass
  public static void tearDownAbstractTest() throws Exception {
    log.info("tearDownAbstractTest");

    releaseServices();

    if (logWarnTestHeader) {
      log.warn("=========== finished test {} ===========", simpleName);
    }

    if (lineFeedFooter) {
      System.out.println();
    }
  }

  static protected void installAll() {
    if (!installed) {
      log.warn("=====================installing all services=====================");
      // install all service while blocking until done
      Runtime.install(null, true);
      installed = true;
    }
  }

  /**
   * release all services except runtime ?
   */
  public static void releaseServices() {

    log.info("end of test - id {} remaining services {}", Platform.getLocalInstance().getId(),
        Arrays.toString(Runtime.getServiceNames()));

    // release all including runtime - be careful of default runtime.yml
    Runtime.releaseAll(true, true);
    // wait for draining threads
    sleep(100);
    // resets runtime with fresh new instance
    Runtime.getInstance();

    // check threads - kill stragglers
    // Set<Thread> stragglers = new HashSet<Thread>();
    Set<Thread> threadSetEnd = Thread.getAllStackTraces().keySet();
    Set<String> threadsRemaining = new TreeSet<>();
    for (Thread thread : threadSetEnd) {
      if (!threadSetStart.contains(thread) && !"runtime_outbox_0".equals(thread.getName())
          && !"runtime".equals(thread.getName())) {
        if (releaseRemainingThreads) {
          log.warn("interrupting thread {}", thread.getName());
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

    // log.warn("end of test - id {} remaining services after release {}",
    // Platform.getLocalInstance().getId(),
    // Arrays.toString(Runtime.getServiceNames()));
  }

  public AbstractTest() {
    simpleName = this.getClass().getSimpleName();
    if (logWarnTestHeader) {
      log.info("=========== starting test {} ===========", this.getClass().getSimpleName());
    }
  }

  public void setVirtual() {
    Platform.setVirtual(true);
  }

  public boolean isVirtual() {
    return Platform.isVirtual();
  }

}

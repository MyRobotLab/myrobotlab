package org.myrobotlab.test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.config.RuntimeConfig;
import org.slf4j.Logger;

public class AbstractTest {

  /**
   * cached network test value for tests
   */
  protected static Boolean hasInternet = null;

  /**
   * Install dependencies once per process, same process will not check. A new
   * process will use the libraries/serviceData.json to determine if deps are
   * satisfied
   */
  protected static boolean installed = false;

  protected final static Logger log = LoggerFactory.getLogger(AbstractTest.class);

  protected static transient Set<Thread> threadSetStart = null;

  @Rule
  public TestWatcher watchman = new TestWatcher() {
    @Override
    protected void starting(Description description) {
      System.out.println("Starting: " + description.getClassName() + "." + description.getMethodName());
    }

    @Override
    protected void succeeded(Description description) {
      // System.out.println("Succeeded: " + description.getMethodName());
    }

    @Override
    protected void failed(Throwable e, Description description) {
      System.out.println("Failed: " + description.getMethodName());
    }

    @Override
    protected void skipped(org.junit.AssumptionViolatedException e, Description description) {
      System.out.println("Skipped: " + description.getMethodName());
    }

    @Override
    protected void finished(Description description) {
      System.out.println("Finished: " + description.getMethodName());
    }
  };

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
    File runtimeYml = new File("data/config/default/runtime.yml");
    // if (!runtimeYml.exists()) {
    runtimeYml.getParentFile().mkdirs();
    RuntimeConfig rc = new RuntimeConfig();
    rc.resource = "src/main/resources/resource";
    String yml = CodecUtils.toYaml(rc);

    FileOutputStream fos = null;
    fos = new FileOutputStream(runtimeYml);
    fos.write(yml.getBytes());
    fos.close();

    // }

    Runtime.getInstance().setVirtual(true);

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

    log.info("end of test - id {} remaining services {}", Runtime.getInstance().getId(),
        Arrays.toString(Runtime.getServiceNames()));

    // release all including runtime - be careful of default runtime.yml
    Runtime.releaseAll(true, true);
    // wait for draining threads
    sleep(100);

    // check threads - kill stragglers
    // Set<Thread> stragglers = new HashSet<Thread>();
    Set<Thread> threadSetEnd = Thread.getAllStackTraces().keySet();
    Set<String> threadsRemaining = new TreeSet<>();
    for (Thread thread : threadSetEnd) {
      if (!threadSetStart.contains(thread) && !"runtime_outbox_0".equals(thread.getName())
          && !"runtime".equals(thread.getName())) {
        threadsRemaining.add(thread.getName());
      }
    }
    if (threadsRemaining.size() > 0) {
      log.warn("{} straggling threads remain [{}]", threadsRemaining.size(), String.join(",", threadsRemaining));
    }

    // resets runtime with fresh new instance
    Runtime.getInstance();

  }

  public void setVirtual() {
    Runtime.getInstance().setVirtual(true);
  }

  public boolean isVirtual() {
    return Runtime.getInstance().isVirtual();
  }

}

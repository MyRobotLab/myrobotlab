package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.repo.GitHub;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.test.AbstractTest;

public class ServiceIT extends AbstractTest {

  private static String branch;
  private static ServiceData sd;
  private static List<ServiceType> serviceTypes;
  private Map<String, String> pythonScripts;
  private List<String> servicesNeedingPythonScripts = new ArrayList<String>();
  private List<String> pythonScriptsWithNoServiceType = new ArrayList<String>();

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {

    branch = (System.getProperty("branch") != null) ? System.getProperty("branch") : "develop";
    // branch = Runtime.getBranch(); ???
    sd = ServiceData.getLocalInstance();
    serviceTypes = sd.getAvailableServiceTypes();
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  public Map<String, String> getPyRobotLabServiceScripts(String branch) throws Exception {

    if (pythonScripts == null) {

      HashSet<String> serviceTypes = new HashSet<String>();

      pythonScripts = new TreeMap<String, String>();
      List<ServiceType> sts = ServiceData.getLocalInstance().getServiceTypes();
      log.info("found {} services", sts.size());
      for (int i = 0; i < sts.size(); ++i) {
        ServiceType st = sts.get(i);
        serviceTypes.add(st.getSimpleName());
        System.out.println(System.getProperty("user.dir") + "/src/main/resources/resource/" + st.getSimpleName() + "/" + st.getSimpleName() + ".py");
        String script = FileIO.toString("src/main/resources/resource/" + st.getSimpleName() + "/" + st.getSimpleName() + ".py"); // GitHub.getPyRobotLabScript(branch, st.getSimpleName());
        if (script != null) {
          pythonScripts.put(st.getSimpleName(), script);
        } else {
          log.warn("{} needs a service script", st.getSimpleName());
          servicesNeedingPythonScripts.add(st.getSimpleName());
        }
      }
      Set<String> gitHubServiceScripts = GitHub.getServiceScriptNames();
      for (String key : gitHubServiceScripts) {
        String serviceName = key.substring(0, key.lastIndexOf("."));
        if (!serviceTypes.contains(serviceName)) {
          log.info("script exists {} but service does not", key);
          pythonScriptsWithNoServiceType.add(key);
        }
      }
      log.info("remove scripts - script found but no service type {}", pythonScriptsWithNoServiceType);
    }
    return pythonScripts;
  }

  @Test
  public void test() throws Exception {
    String[] b = new String[] { "LeapMotion", "OpenNi", "Runtime", "SlamBad", "_TemplateService", "Cli", "WebGui", "JMonkeyEngine", "ImageDisplay", "GoogleAssistant",
        "PickToLight", "PythonProxy", "Sprinkler", "_TemplateProxy", "SwingGui" };
    Set<String> blacklist = new HashSet<String>(Arrays.asList(b));

    pythonScripts = getPyRobotLabServiceScripts(branch);
    log.info("testing {} scripts", pythonScripts.size());

    for (String name : pythonScripts.keySet()) {
      log.info("testing script {}", name);

      // clear the stage
      releaseServices();
      
      // install service ??? in own jvm
      
      // add pre scripts to subscribe for errors & finish script with countdown timer

      // fire the python

      // collect the results

      // publish the errors (asserts if ready)

    }

  }

}

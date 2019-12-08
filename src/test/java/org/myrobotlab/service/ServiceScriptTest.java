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
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.test.AbstractTest;
@Ignore
public class ServiceScriptTest extends AbstractTest {
 
  private Map<String, String> pythonScripts;
  private List<String> servicesNeedingPythonScripts = new ArrayList<String>();
  private List<String> pythonScriptsWithNoServiceType = new ArrayList<String>();

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {

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

  public Map<String, String> getPyRobotLabServiceScripts() throws Exception {

    if (pythonScripts == null) {

      HashSet<String> serviceTypes = new HashSet<String>();

      pythonScripts = new TreeMap<String, String>();
      List<ServiceType> sts = ServiceData.getLocalInstance().getServiceTypes();
      log.info("found {} services", sts.size());
      for (int i = 0; i < sts.size(); ++i) {
        ServiceType st = sts.get(i);
        serviceTypes.add(st.getSimpleName());
        System.out.println(System.getProperty("user.dir") + "/src/main/resources/resource/" + st.getSimpleName() + "/" + st.getSimpleName() + ".py");
        String script = FileIO.toSafeString("src/main/resources/resource/" + st.getSimpleName() + "/" + st.getSimpleName() + ".py"); // GitHub.getPyRobotLabScript(branch,
                                                                                                                                     // st.getSimpleName());
        if (script != null) {
          pythonScripts.put(st.getSimpleName(), script);
        } else {
          log.warn("{} needs a service script", st.getSimpleName());
          servicesNeedingPythonScripts.add(st.getSimpleName());
        }
      }

      log.info("remove scripts - script found but no service type {}", pythonScriptsWithNoServiceType);
    }
    return pythonScripts;
  }

  @Test
  public void testServiceScripts() throws Exception {
    String[] b = new String[] { "Agent", "LeapMotion", "OpenNi", "Runtime", "SlamBad", "_TemplateService", "WebGui", "JMonkeyEngine", "ImageDisplay", "GoogleAssistant",
        "PickToLight", "PythonProxy", "Sprinkler", "_TemplateProxy", "SwingGui" };
    Set<String> blacklist = new HashSet<String>(Arrays.asList(b));

    pythonScripts = getPyRobotLabServiceScripts();
    log.info("testing {} scripts", pythonScripts.size());

    for (String name : pythonScripts.keySet()) {
      
      // name = "AudioFile";
      
      log.info("testing script {}", name);

      // clear the stage
      releaseServices();

      Python python = (Python) Runtime.start("python", "Python");
      // FIXME - do seperate install in different jvm on subfolder - this will
      // test dependency definition
      // dead code

      try {
        log.error("testing ######## {} ########", name);

        python.execAndWait(pythonScripts.get(name));
        
        // FIXME !!! - look for Python ERRORS !!! - THEY SHOULD BE PUBLISHED !!!

      } catch (Exception e) {
        log.error("testing {} threw", name, e);
      }

      // add pre scripts to subscribe for errors & finish script with countdown
      // timer

      // fire the python

      // collect the results

      // publish the errors (asserts if ready)

    }
  }

}

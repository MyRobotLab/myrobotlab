package org.myrobotlab.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class ServiceInterfaceTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(ServiceInterfaceTest.class);

  private boolean testWebPages = false;

  // FIXME - add to report at end of "all" testing ...
  private boolean serviceHasWebPage(String service) {
    String url = "http://myrobotlab.org/service/" + service;
    InputStream in = null;
    try {
      in = new URL(url).openStream();
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      // e.printStackTrace();
      return false;
    } catch (IOException e) {
      // TODO Auto-generated catch block
      // e.printStackTrace();
      return false;
    }

    try {
      // read the page (we don't care about contents. (yet))
      IOUtils.toString(in);
    } catch (IOException e) {
      // e.printStackTrace();
      return false;
    } finally {
      IOUtils.closeQuietly(in);
    }
    return true;
  }

  private boolean serviceInterfaceTest(String service) throws IOException {
    // see if we can start/stop and release the service.
    
    // set a configuration path
    Runtime.setConfig("serviceInterfaceTest");

    ServiceInterface foo = Runtime.create(service.toLowerCase(), service);
    if (foo == null) {
      log.warn("Runtime Create returned a null service for {}", service);
      return false;
    }
    System.out.println("Service Test:" + service);

    if (service.equals("As5048AEncoder")){
      log.info("here");
    }

    System.out.flush();
    // Assert.assertNotNull(foo.getCategories());
    Assert.assertNotNull(foo.getDescription());
    Assert.assertNotNull(foo.getName());
    Assert.assertNotNull(foo.getSimpleName());
    Assert.assertNotNull(foo.getTypeKey());

    // TODO: add a bunch more tests here!
    foo.startService();
    foo.stopService();

    foo.startService();
    foo.save();
    // foo.load(); SHOULD NOT BE USED !
    // foo.apply(); <-  THIS SHOULD BE IMPLEMENTED
    foo.stopService();

    foo.releaseService();

    return true;
  }

  @Test
  public final void testAllServices() throws ClassNotFoundException, IOException {

    ArrayList<String> servicesWithoutWebPages = new ArrayList<String>();
    ArrayList<String> servicesWithoutScripts = new ArrayList<String>();
    ArrayList<String> servicesThatDontStartProperly = new ArrayList<String>();

    ArrayList<String> servicesNotInServiceDataJson = new ArrayList<String>();

    HashSet<String> blacklist = new HashSet<String>();
    blacklist.add("OpenNi");    
    blacklist.add("As5048AEncoder");    
    blacklist.add("IntegratedMovement");    
    blacklist.add("VirtualDevice");
    blacklist.add("Joystick");
    blacklist.add("GoogleAssistant");
    blacklist.add("LeapMotion");
    blacklist.add("Python"); // python's interpreter cannot be restarted cleanly
    blacklist.add("Runtime");
    blacklist.add("OpenCV");
    blacklist.add("InMoov2");
    blacklist.add("WorkE");
    blacklist.add("JMonkeyEngine");
    blacklist.add("_TemplateService");
    blacklist.add("EddieControlBoard");// band because peer is Keyboard
    blacklist.add("Lloyd");
    blacklist.add("Solr");
    blacklist.add("Proxy"); // interesting idea - but no worky
    blacklist.add("Sphinx");

    // This one just takes so darn long.
    blacklist.add("Deeplearning4j");
    blacklist.add("OculusDiy");

    // start up python so we have it available to do some testing with.
    Python python = (Python) Runtime.start("python", "Python");
    Service.sleep(1000);
    ServiceData sd = ServiceData.getLocalInstance();
    List<MetaData> sts = sd.getServiceTypes(); // there is also
                                               // sd.getAvailableServiceTypes();

    int numServices = sts.size();
    int numServicePages = 0;
    int numScripts = 0;
    int numScriptsWorky = 0;
    int numStartable = 0;
    log.info("----------------------------------------------");
    // FIXME - subscribe to all errors of all new services !!! - a prefix script
    // !
    // FIXME - must have different thread (prefix script) which runs a timer -
    // script REQUIRED to complete in 4 minutes ... or BOOM it fails

    // sts.clear();
    // sts.add(sd.getServiceType("org.myrobotlab.service.InMoov"));

    for (MetaData serviceType : sts) {
      // test single service
      // serviceType =
      // sd.getServiceType("org.myrobotlab.service.VirtualDevice");
      String service = serviceType.getSimpleName();
      // System.out.println("SYSTEM TESTING " + service);
      // System.out.flush();

      // service = "org.myrobotlab.service.Hd44780";

      if (blacklist.contains(
          service)/* || !serviceType.getSimpleName().equals("Emoji") */) {
        log.info("White listed testing of service {}", service);
        continue;
      }
      // log.info("Testing Service: {}", service);
      
      System.out.println("testing " + service);

      MetaData st = ServiceData.getMetaData("org.myrobotlab.service." + service);
      if (st == null) {
        System.out.println("NO SERVICE TYPE FOUND!"); // perhaps this should
                                                      // throw
        servicesNotInServiceDataJson.add(service);
      }

      if (testWebPages) {
        if (serviceHasWebPage(service)) {
          log.info("Service {} has a web page..", service);
          numServicePages++;
        } else {
          log.warn("Service {} does not have a web page..", service);
          servicesWithoutWebPages.add(service);
        }
      }

      if (serviceInterfaceTest(service)) {
        numStartable++;
      } else {
        servicesThatDontStartProperly.add(service);
      }

      String serviceScript = Service.getServiceScript(service);
      if (serviceScript != null) {
        log.info("Service Has a Script: {}", service);
        numScripts++;
      } else {
        log.warn("Missing Script for Service {}", service);
        servicesWithoutScripts.add(service);
      }

      //
      if (testServiceScript(python, service)) {
        // log.info("Default script for {} executes ok!", service);
        numScriptsWorky++;
      } else {
        // log.warn("Default script for {} blows up!", service);
        // servicesWithoutWorkyScript(service);
      }
      // log.info("SERVICE TESTED WAS :" + service);
      log.info("----------------------------------------------");

      // CLEAN-UP !!!
      releaseServices();
      // System.out.println("Next?");
      // try {
      // System.in.read();
      // } catch (IOException e) {
      // // TODO Auto-generated catch block
      // e.printStackTrace();
      // }

    }

    System.out.println("----------------------------------------------");
    System.out.println("Service Report");
    System.out.println(String.format("Number of Services:           %d", numServices));
    System.out.println(String.format("Number of Startable Services: %d", numStartable));
    System.out.println(String.format("Number of Services Pages      %d", numServicePages));
    System.out.println(String.format("Number of Scripts:            %d", numScripts));
    System.out.println(String.format("Number of Scripts Worky:      %d", numScriptsWorky));
    System.out.println("----------------------------------------------");

    for (String s : servicesThatDontStartProperly) {
      log.warn("FAILED ON START:" + s);
    }

    for (String s : servicesWithoutWebPages) {
      log.warn("NO WEB PAGE    :" + s);
    }

    for (String s : servicesWithoutScripts) {
      log.warn("NO SCRIPT      :" + s);
    }

    for (String s : servicesNotInServiceDataJson) {
      log.info("NOT IN SERVICE DATA :" + s);
    }

    log.info("Done...");

  }

  @Test
  public final void testInstallAllServices() throws ClassNotFoundException, ParseException, IOException {
    // TODO: this probably is going to take longer but it's worth while!
    ServiceData sd = ServiceData.getLocalInstance();// CodecUtils.fromJson(FileUtils.readFileToString(new
                                                    // File("../libraries/serviceData.json")),
                                                    // ServiceData.class);
    for (MetaData st : sd.getServiceTypes()) {
      if (!st.isAvailable()) {
        log.info("Installing Service:" + st.getType());
        Runtime.install(st.getType(), true);
      } else {
        log.info("already installed.");
      }
    }
  }

  private boolean testServiceScript(Python python, String serviceType) {

    String testScriptFile = Service.getServiceScript(serviceType);
    if (testScriptFile == null) {
      log.warn("No default script for Service {}", Service.getResource(serviceType, serviceType + ".py"));
      return false;
    } else {
      log.info("Default Script Exists for {}", serviceType);
    }

    return false;
  }

}

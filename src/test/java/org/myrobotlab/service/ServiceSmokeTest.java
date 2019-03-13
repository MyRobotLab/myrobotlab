package org.myrobotlab.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

import org.junit.Test;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

/**
 * This test will iterate all possible services (except for the blacklisted
 * ones) it will create an instance of that service and pass the service to the
 * json serializer to ensure it doesn't blow up.
 * 
 * @author kwatters
 *
 */

public class ServiceSmokeTest extends AbstractTest {

  transient public final static Logger log = LoggerFactory.getLogger(ServiceSmokeTest.class);

  @Test
  public void testAllServiceSerialization() throws IOException, ParseException {

    installAll();

    // known problematic services?! TODO: fix them and remove from the following
    // list.
    ArrayList<String> blacklist = new ArrayList<String>();
    // kills test if library not found! eek.
    blacklist.add("org.myrobotlab.service.LeapMotion");
    // same..
    blacklist.add("org.myrobotlab.service.OpenNi");
    // not sure what the heck happens here.
    blacklist.add("org.myrobotlab.service.Runtime");
    // more bad stuff happens with this service.
    blacklist.add("org.myrobotlab.service.SlamBad");
    // why the heck does this one fail?!
    // blacklist.add("org.myrobotlab.service._TemplateService");

    // TODO: lets skip a few painful services to test additionally.
    blacklist.add("org.myrobotlab.service.Cli");
    blacklist.add("org.myrobotlab.service.WebGui");
    blacklist.add("org.myrobotlab.service.JMonkeyEngine");
    blacklist.add("org.myrobotlab.service.ImageDisplay");

    // this seems to start webgui also as a peer.. eek!
    blacklist.add("org.myrobotlab.service.GoogleAssistant");
    blacklist.add("org.myrobotlab.service.PickToLight");
    blacklist.add("org.myrobotlab.service.PythonProxy");
    blacklist.add("org.myrobotlab.service.Sprinkler");
    blacklist.add("org.myrobotlab.service._TemplateProxy");

    // just don't want a swing gui opening up in the unit test.
    blacklist.add("org.myrobotlab.service.SwingGui");
    // blacklist.add("org.myrobotlab.service.DiyServo");
    
    //  anything which has Keyboard as a service - explodes on Linux
    blacklist.add("org.myrobotlab.service.EddieControlBoard");
    blacklist.add("org.myrobotlab.service.Keyboard");
    
    
    
    // the service data!
    ServiceData serviceData = ServiceData.getLocalInstance();

    // we need to load a service for each service type we have.
    String[] x = serviceData.getServiceTypeNames();

    for (String serviceType : x) {
      log.info("Service Type: {}", serviceType);
    }
    log.info("Press any key to continue");
    // System.in.read();
    for (String serviceType : x) {

      long start = System.currentTimeMillis();

      log.info("Testing service type {}", serviceType);
      if (blacklist.contains(serviceType)) {
        log.info("Skipping known problematic service {}", serviceType);
        continue;
      }
      log.warn("Testing service type {}", serviceType);
      String serviceName = serviceType.toLowerCase();
      ServiceInterface s = Runtime.start(serviceName, serviceType);
      assertNotNull(s);
      // log.error("serviceType {}", s.getName());
      testSerialization(s);
      // TODO: validate the service is released!
      s.releaseService();

      long delta = System.currentTimeMillis() - start;
      log.info("Done testing serialization of {} in {} ms", serviceType, delta);
      // System.in.read();

    }

    Runtime.releaseAll();

    log.info("Done with tests..");

    assertTrue(true);
  }

  public void testSerialization(ServiceInterface s) {

    // TODO: perhaps some extra service type specific initialization?!
    String res = CodecUtils.toJson(s);
    assertNotNull(res);
    log.info("Serialization successful for {}", s.getType());

    // ServiceInterface s = CodecUtils.fromJson(res, clazz)
    // assertNotNull(res);
  }

}
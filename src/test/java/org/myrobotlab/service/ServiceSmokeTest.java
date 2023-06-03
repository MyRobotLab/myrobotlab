package org.myrobotlab.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

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
  public void testAllServiceSerialization() {
    try {
      installAll();

      // known problematic services?! TODO: fix them and remove from the
      // following
      // list.
      ArrayList<String> blacklist = new ArrayList<String>();
      blacklist.add("org.myrobotlab.service.OpenNi");
      blacklist.add("org.myrobotlab.service.Blender");
      blacklist.add("org.myrobotlab.service.Joystick");
      blacklist.add("org.myrobotlab.service.WorkE");
      blacklist.add("org.myrobotlab.service.PythonProxy");
      blacklist.add("org.myrobotlab.service.Sweety");
      blacklist.add("org.myrobotlab.service.Sphinx");
      blacklist.add("org.myrobotlab.service.LeapMotion");
      blacklist.add("org.myrobotlab.service.Runtime");
      blacklist.add("org.myrobotlab.service.Proxy"); // interesting idea - but no worky
      blacklist.add("org.myrobotlab.service.JMonkeyEngine");
      blacklist.add("org.myrobotlab.service.Lloyd");
      blacklist.add("org.myrobotlab.service._TemplateService");

      // FIXME - really ? lame
      blacklist.add("org.myrobotlab.service.Joystick");

      // the service data!
      ServiceData serviceData = ServiceData.getLocalInstance();

      // we need to load a service for each service type we have.
      // String[] serviceTypes = serviceData.getServiceTypeNames();

      String[] serviceTypes = new String[] { "org.myrobotlab.service.Joystick" };

      for (String serviceType : serviceTypes) {
        log.info("Service Type: {}", serviceType);
      }
      log.info("Press any key to continue");
      // System.in.read();
      for (String serviceType : serviceTypes) {

        // serviceType = "org.myrobotlab.service.Sabertooth";

        long start = System.currentTimeMillis();

        if (blacklist.contains(serviceType)) {
          log.warn("Skipping known problematic service {}", serviceType);
          continue;
        }
        log.warn("Testing {}", serviceType);
        String serviceName = serviceType.toLowerCase();
        ServiceInterface s = Runtime.create(serviceName, serviceType);
        if (s == null) {
          log.error("service type {} could not be created", serviceType);
        }
        assertNotNull(String.format("could not create %s", serviceName), s);
        s.setVirtual(true);
        s = Runtime.start(serviceName, serviceType);
        assertNotNull(String.format("could not start %s", serviceName), s);
        // log.error("serviceType {}", s.getName());
        testSerialization(s);
        // TODO: validate the service is released!
        s.releaseService();

        long delta = System.currentTimeMillis() - start;
        log.info("Done testing serialization of {} in {} ms", serviceType, delta);

      }

      Runtime.releaseAll();

      log.info("Done with tests..");

    } catch (Exception e) {
      log.error("ServiceSmokeTest threw", e);
      fail("ServiceSmokeTest threw");
    }
  }

  public void testSerialization(ServiceInterface s) {

    // TODO: perhaps some extra service type specific initialization?!
    String res = CodecUtils.toJson(s);
    assertNotNull(res);
    log.info("Serialization successful for {}", s.getTypeKey());

    // ServiceInterface s = CodecUtils.fromJson(res, clazz)
    // assertNotNull(res);
  }

}
package org.myrobotlab.framework;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.codec.ClassUtil;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Motor;
import org.myrobotlab.service.interfaces.AnalogListener;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class ClassUtilTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(ClassUtilTest.class);

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @Test
  public void classUtilTest() throws Exception {

    Set<String> interfaces = ClassUtil.getInterfaces(Motor.class);
    log.warn("Motor class has {} interfaces", interfaces.size());
    log.warn(interfaces.toString());
    
    assertTrue(ClassUtil.getInterfaces(Motor.class).contains(AnalogListener.class.getCanonicalName()));
    assertTrue(ClassUtil.getInterfaces(Motor.class).contains(ServiceInterface.class.getCanonicalName()));

  }

}
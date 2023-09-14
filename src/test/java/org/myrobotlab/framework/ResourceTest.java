package org.myrobotlab.framework;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class ResourceTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(ResourceTest.class);

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @Test
  public void test() throws InterruptedException, IOException {
    // set Runtime's resource cmdline

    // === access as a service ===
    Servo servo = (Servo) Runtime.start("servo", "Servo");

    byte[] resource = Service.getServiceIcon(Servo.class);
    byte[] strParam = Service.getServiceIcon("Servo");
    byte[] nonStatic = servo.getServiceIcon();

    assertTrue(resource.length > 0 && (nonStatic.length == resource.length && strParam.length == resource.length));

    String rs = servo.getServiceScript();
    String s = Service.getServiceScript(Servo.class);

    String script = servo.getResourceAsString("Servo.py");
    String resourceString = Service.getResourceAsString(Servo.class, "Servo.py");
    String strParams = Service.getResourceAsString("Servo", "Servo.py");

    String b = new String(servo.getResource("Servo.py"));
    String bs = new String(Service.getResource(Servo.class, "Servo.py"));
    String bstr = new String(Service.getResource("Servo", "Servo.py"));

    String serviceScript = Service.getServiceScript("Servo");

    assertTrue(rs != null && (rs.contentEquals(s) && rs.contentEquals(script) && rs.contentEquals(resourceString) && rs.contentEquals(strParams) && rs.contentEquals(b)
        && rs.contentEquals(bs) && rs.contentEquals(serviceScript) && rs.contentEquals(bstr)));

  }

}
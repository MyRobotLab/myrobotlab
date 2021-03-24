package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.myrobotlab.framework.Service;
import org.python.core.PyInteger;

public class PythonTest extends AbstractServiceTest {

  @Override
  public Service createService() {
    return (Service) Runtime.start("python", "Python");
  }

  @Override
  public void testService() throws Exception {
    Python python = (Python)Runtime.start("python", "Python");
    
    // checking basic setting of a value
    python.set("test", "we love testing!");
    
    // basic value checking - getting values from jython into java    
    // python.exec("test = 'yay!'");
    String test00 = (String)python.get("test");
    String expected00 = "we love testing!";
    assertEquals(expected00, test00);
    
    python.exec("test = 12");
    Integer test01 = (Integer)python.get("test");
    Integer expected01 = 12;
    assertEquals(expected01, test01);

    python.exec("test = 12.4321");
    Double test02 = (Double)python.get("test");
    Double expected02 = 12.4321;
    assertEquals(expected02, test02);
    
    python.exec("test = [0, 1, 2, 3, 4, 5, 6]");
    Object[] test03 = (Object[])python.get("test");
    PyInteger expected03 = new PyInteger(3);
    assertEquals(expected03, test03[3]);

    python.exec("test = {'foo':True, 'bar':False}");
    Map test04 = (Map)python.get("test");
    assertEquals(true, test04.get("foo"));
    
    python.exec("sleep(2)");
    python.waitFor("python","finishedExecutingScript", 3000);
    
    // verifying callbacks from subscriptions can call python methods
    python.exec("count = 0\ndef onPulse(clock_date):\n\tprint('successs !', clock_date)\n\tglobal count\n\tcount = count + 1");
    Clock clock = (Clock)Runtime.start("clock01", "Clock");
    python.subscribe("clock01", "pulse");
    clock.startClock();
    sleep(2000);
    Integer count = (Integer)python.get("count");
    assertTrue(count > 0);
    
  }

}

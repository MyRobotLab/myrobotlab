package org.myrobotlab.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.myrobotlab.framework.Service;


public class RasPiTest extends AbstractServiceTest {

  @Override /* FIXME - this assumes a single service is in the test - which rarely happens - seems not useful and silly */
  public Service createService() throws Exception {
    return (Service)Runtime.start("raspi", "RasPi");
  }

  @Override
  public void testService() throws Exception {
    RasPi raspi = (RasPi)Runtime.start("raspi", "RasPi");
    // raspi.enablePin(5); will not work on a x86
    Pcf8574 pcf = (Pcf8574)Runtime.start("pcf", "Pcf8574");
    raspi.attach(pcf);
    assertTrue(raspi.isAttached("pcf"));
    assertTrue(raspi.isAttached(pcf));
    assertTrue(pcf.isAttached("raspi"));
    assertTrue(pcf.isAttached(raspi));
    raspi.detach(pcf);
    assertFalse(raspi.isAttached("pcf"));
    assertFalse(raspi.isAttached(pcf));
    assertFalse(pcf.isAttached("raspi"));
    assertFalse(pcf.isAttached(raspi));
  }


}

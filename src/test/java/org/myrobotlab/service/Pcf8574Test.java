package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.myrobotlab.framework.Service;


public class Pcf8574Test extends AbstractServiceTest {

  @Override
  public Service createService() throws Exception {
    Pcf8574 pcf = (Pcf8574)Runtime.start("pcf","Pcf8574");
    return pcf;
  }

  @Override
  public void testService() throws Exception {
    RasPi raspi = (RasPi)Runtime.start("raspi","RasPi");
    //arduino = Runtime.start("arduino","Arduino")
    //arduino.setBoardMega()
    //arduino.connect("COM3")
    
    Pcf8574 pcf = (Pcf8574)Runtime.start("pcf","Pcf8574");


    int KeyColumn = 0;
    int LastKeyPress = 0;
    
    // Before we can use this, 
    // we need to configure the I2C Bus 
    //KeyPad.setBus("1")
    // and address then connect it.
    //KeyPad.setAddress("0x20")
    //KeyPad.attachI2CController(raspi)
    // KeyPad.attach(raspi, "1", "0x20");
    // KeyPad.attach(raspi, "1", "0x20");
    pcf.attach(raspi);
    pcf.setBus("1");
    pcf.setAddress("0x20");
    
    assertEquals("1", pcf.getBus());
    assertEquals("0x20", pcf.getAddress());
    assertTrue(pcf.isAttached(raspi));
    
    pcf.detach(raspi);

    assertFalse(pcf.isAttached(raspi));
    
  }


}

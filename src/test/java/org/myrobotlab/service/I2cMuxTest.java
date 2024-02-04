package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.I2CController;

@RunWith(JUnit4.class)
public class I2cMuxTest {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  private I2CController mockController;
  
  @Mock
  private I2CControl mockControl;

  private I2cMux i2cMux;

  @Before
  public void setUp() {
    i2cMux = (I2cMux) Runtime.start("i2cMux", "I2cMux");
    when(mockController.getName()).thenReturn("controller");
    when(mockControl.getName()).thenReturn("controlName");
    when(mockControl.getBus()).thenReturn("bus");
    when(mockControl.getAddress()).thenReturn("address");    
  }

  @Test
  public void testAttachController() throws Exception {
    i2cMux.attach(mockController);
    assertTrue(i2cMux.isAttached(mockController));
  }

  @Test
  public void testDetachController() {
    i2cMux.detach(mockController);
    assertFalse(i2cMux.isAttached(mockController));
  }

  @Test
  public void testSetDeviceBus() {
    String deviceBus = "1";
    i2cMux.setDeviceBus(deviceBus);
    assertEquals(deviceBus, i2cMux.getDeviceBus());
  }

  @Test
  public void testSetDeviceAddress() {
    String deviceAddress = "0x70";
    i2cMux.setDeviceAddress(deviceAddress);
    assertEquals(deviceAddress, i2cMux.getDeviceAddress());
  }
  
  @Test
  public void testAttachI2CControlNotPreviouslyAttached() {
      i2cMux.attachI2CControl(mockControl);

      // Verify the control is attached and the correct methods were called
      // verify(mockControl).attachI2CController(i2cMux);
      assertTrue(i2cMux.geti2cDevices().containsKey("controlName"));
      assertEquals(i2cMux.geti2cDevices().get("controlName").serviceName, "controlName");
      assertEquals(i2cMux.geti2cDevices().get("controlName").busAddress, "bus");
      assertEquals(i2cMux.geti2cDevices().get("controlName").deviceAddress, "address");
  }

  @Test
  public void testAttachI2CControlAlreadyAttached() {
      i2cMux.attachI2CControl(mockControl); // first attachment
      i2cMux.attachI2CControl(mockControl); // second attempt

      // Verify attachI2CController on mockControl was only called once
      verify(mockControl, times(1)).attachI2CController(i2cMux);

      // Verify that the device map only has one entry for "controlName"
      assertTrue(i2cMux.geti2cDevices().containsKey("controlName"));
      assertEquals(i2cMux.geti2cDevices().size(), 1);
  }

  @Test
  public void testGetI2cDevices() {
      // Initially, there should be no devices
      i2cMux.getConfig().i2cDevices.clear();
      assertTrue(i2cMux.geti2cDevices().isEmpty());

      // Attach a control
      i2cMux.attachI2CControl(mockControl);

      // Now, there should be one device
      assertEquals(i2cMux.geti2cDevices().size(), 1);
      assertTrue(i2cMux.geti2cDevices().containsKey("controlName"));
  }

  // Other tests can be added here
}

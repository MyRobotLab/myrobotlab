/**
 * 
 */
package org.myrobotlab.service;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.service.interfaces.NeoPixelController;
import org.myrobotlab.test.TestUtils;

/**
 * @author chris
 *
 */
public class NeoPixelTest {
  private static final String V_PORT_1 = "test_port_1";
  public Arduino ard;
  private NeoPixel neopixel;


  @Before
  public void setUp() throws Exception {
    // setup the test environment , and create an arduino with a virtual backend for it.
    TestUtils.initEnvirionment();
    VirtualArduino va1 = (VirtualArduino)Runtime.createAndStart("va1", "VirtualArduino");
    va1.connect(V_PORT_1);
    ard = (Arduino) Runtime.createAndStart("ard", "Arduino");
    ard.connect(V_PORT_1);
    neopixel = (NeoPixel) Runtime.createAndStart("neopixel", "NeoPixel");
    neopixel.attach(ard, 28, 16);
    
  }

  /**
   * Test method for {@link org.myrobotlab.service.NeoPixel#setPixel(int, int, int, int)}.
   */
  @Test
  public void testSetPixelIntIntIntInt() {
    neopixel.setPixel(2, 255, 0, 0);
    assertTrue(neopixel.pixelMatrix.get(2).isEqual(new NeoPixel.PixelColor(2, 255, 0, 0)));
  }

  /**
   * Test method for {@link org.myrobotlab.service.NeoPixel#sendPixel(int, int, int, int)}.
   */
  @Test
  public void testSendPixelIntIntIntInt() {
    neopixel.sendPixel(2, 0, 255, 0);
    assertTrue(neopixel.pixelMatrix.get(2).isEqual(new NeoPixel.PixelColor(2, 0, 255, 0)));
  }

  /**
   * Test method for {@link org.myrobotlab.service.NeoPixel#writeMatrix()}.
   */
  @Test
  public void testWriteMatrix() {
    neopixel.writeMatrix();
  }

  /**
   * Test method for {@link org.myrobotlab.service.NeoPixel#turnOff()}.
   */
  @Test
  public void testTurnOff() {
    neopixel.turnOff();
    assertTrue(neopixel.pixelMatrix.get(2).isEqual(new NeoPixel.PixelColor(2, 0, 0, 0)));
    neopixel.turnOn();
  }


  /**
   * Test method for {@link org.myrobotlab.service.NeoPixel#attach(org.myrobotlab.service.interfaces.NeoPixelController, int, int)}.
   */
  @Test
  public void testAttachNeoPixelControllerIntInt() {
    assertTrue(neopixel.isAttached);
  }

  /**
   * Test method for {@link org.myrobotlab.service.NeoPixel#detach(org.myrobotlab.service.interfaces.NeoPixelController)}.
   */
  @Test
  public void testDetachNeoPixelController() {
    neopixel.detach((NeoPixelController)ard);
    assertFalse(neopixel.isAttached);
    neopixel.attach(ard, 28, 16);

  }

  /**
   * Test method for {@link org.myrobotlab.service.NeoPixel#setAnimation(java.lang.String, int, int, int, int)}.
   */
  @Test
  public void testSetAnimationStringIntIntIntInt() {
    neopixel.setAnimation("Theater Chase Rainbow", 0, 0, 255, 1);
  }

}

/**
 * 
 */
package org.myrobotlab.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.service.interfaces.NeoPixelController;
import org.myrobotlab.test.AbstractTest;

/**
 * @author chris
 *
 */
public class NeoPixelTest extends AbstractTest {
  private static final String V_PORT_1 = "test_port_1";
  public Arduino arduino;
  private NeoPixel neopixel;

  @Before
  public void setUp() throws Exception {
    arduino = (Arduino) Runtime.start("ard", "Arduino");
    arduino.connect(V_PORT_1);
    neopixel = (NeoPixel) Runtime.start("neopixel", "NeoPixel");
    neopixel.attach(arduino, 28, 16);

  }

  /**
   * Test method for
   * {@link org.myrobotlab.service.NeoPixel#attach(org.myrobotlab.service.interfaces.NeoPixelController, int, int)}.
   */
  @Test
  public void testAttachNeoPixelControllerIntInt() {
    assertTrue(neopixel.isAttached);
  }

  /**
   * Test method for
   * {@link org.myrobotlab.service.NeoPixel#detach(org.myrobotlab.service.interfaces.NeoPixelController)}.
   */
  @Test
  public void testDetachNeoPixelController() {
    neopixel.detach((NeoPixelController) arduino);
    assertFalse(neopixel.isAttached);
    neopixel.attach(arduino, 28, 16);

  }

  /**
   * Test method for
   * {@link org.myrobotlab.service.NeoPixel#sendPixel(int, int, int, int)}.
   */
  @Test
  public void testSendPixelIntIntIntInt() {
    neopixel.sendPixel(2, 0, 255, 0);
    assertTrue(neopixel.pixelMatrix.get(2).isEqual(new NeoPixel.PixelColor(2, 0, 255, 0)));
  }

  /**
   * Test method for
   * {@link org.myrobotlab.service.NeoPixel#setAnimation(java.lang.String, int, int, int, int)}.
   */
  @Test
  public void testSetAnimationStringIntIntIntInt() {
    neopixel.setAnimation("Theater Chase Rainbow", 0, 0, 255, 1);
  }

  /**
   * Test method for
   * {@link org.myrobotlab.service.NeoPixel#setPixel(int, int, int, int)}.
   */
  @Test
  public void testSetPixelIntIntIntInt() {
    neopixel.setPixel(2, 255, 0, 0);
    assertTrue(neopixel.pixelMatrix.get(2).isEqual(new NeoPixel.PixelColor(2, 255, 0, 0)));
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
   * Test method for {@link org.myrobotlab.service.NeoPixel#writeMatrix()}.
   */
  @Test
  public void testWriteMatrix() {
    neopixel.writeMatrix();
  }

}
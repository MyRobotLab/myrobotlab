/**
 * 
 */
package org.myrobotlab.service;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.test.AbstractTest;

/**
 * @author chris
 *
 */
public class NeoPixelTest extends AbstractTest {
  private static final String VIRTUAL_PORT = "COM13";
  private static Arduino arduino;
  private static NeoPixel neopixel;

  @BeforeClass
  static public void setUp() throws Exception {
    Runtime.setAllVirtual(true);
    arduino = (Arduino) Runtime.start("neoArduino", "Arduino");
    arduino.connect(VIRTUAL_PORT);
    neopixel = (NeoPixel) Runtime.start("neopixel", "NeoPixel");
    neopixel.setPin(16);
    neopixel.setPixelCount(32);
    neopixel.attach(arduino);

  }
  
  // FIXME - do teardown 

  /**
   * Test method for
   * {@link org.myrobotlab.service.NeoPixel#attach(org.myrobotlab.service.interfaces.NeoPixelController, int, int)}.
   */
  @Test
  public void testAttachNeoPixelControllerIntInt() {
    assertTrue(neopixel.isAttached(arduino));
  }

  /**
   * Test method for
   * {@link org.myrobotlab.service.NeoPixel#detach(org.myrobotlab.service.interfaces.NeoPixelController)}.
   * 
   * @throws Exception
   */
  @Test
  public void testDetachNeoPixelController() throws Exception {
//    neopixel.detach(arduino);
    neopixel.setPin(16);
    neopixel.setPixelCount(32);
    // assertFalse(neopixel.isAttached);
    neopixel.attach(arduino);

  }

  /**
   * Test method for
   * {@link org.myrobotlab.service.NeoPixel#sendPixel(int, int, int, int)}.
   */
  @Test
  public void testSendPixelIntIntIntInt() {
    neopixel.setPixel(3, 24, 53, 87);
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
    // assertTrue(neopixel.pixelMatrix.get(2).isEqual(new NeoPixel.PixelColor(2,
    // 255, 0, 0, 0)));
  }

  /**
   * Test method for {@link org.myrobotlab.service.NeoPixel#turnOff()}.
   */
  @Test
  public void testTurnOff() {
    neopixel.clear();
    neopixel.setPixel(10, 10, 10, 10);
    // neopixel.turnOn();
  }

  /**
   * Test method for {@link org.myrobotlab.service.NeoPixel#writeMatrix()}.
   */
  @Test
  public void testWriteMatrix() {
    neopixel.writeMatrix();
  }

}
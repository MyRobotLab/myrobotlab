package org.myrobotlab.service;

import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.event.KeyEvent;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.KeyboardSimConfig;
import org.slf4j.Logger;

/**
 * @author MaVo (MyRobotLab) / LunDev (GitHub)
 */

public class KeyboardSim extends Service<KeyboardSimConfig>
{
  static final long serialVersionUID = 1L;
  static final Logger log = LoggerFactory.getLogger(KeyboardSim.class);

  transient private Robot robot;

  public KeyboardSim(String n, String id) {
    super(n, id);
    try {
      if (!GraphicsEnvironment.isHeadless()) {
        robot = new Robot();
      } else {
        log.warn("headless environment - but KeyboardSim requires a display");
      }
    } catch (Exception e) {
      log.error("could not create java.awt.Robot", e);
    }
  }

  /**
   * simulate pressing and releasing a keyboard key after the desired duration
   * 
   * @param keycode
   *                 keycode of key (use KeyEvent.VK_XXX, where XXX is your key)
   * @param duration
   *                 key press duration in milliseconds
   */
  public void pressAndRelease(int keycode, int duration) {
    press(keycode);
    sleep(duration);
    release(keycode);
  }

  /**
   * simulate pressing a keyboard key (does NOT release the key!)
   * 
   * @param keycode
   *                keycode of key (use KeyEvent.VK_XXX, where XXX is your key)
   */
  public void press(int keycode) {
    robot.keyPress(keycode);
  }

  /**
   * simulate releasing a keyboard key (does NOT press the key!)
   * 
   * @param keycode
   *                keycode of key (use KeyEvent.VK_XXX, where XXX is your key)
   */
  public void release(int keycode) {
    robot.keyRelease(keycode);
  }

  /**
   * simulate pressing and releasing several keyboard keys after the desired
   * duration The keys are released in reverse order they were pressed.
   * 
   * @param keycodes
   *                 array of keycodes of keys (use KeyEvent.VK_XXX, where XXX is
   *                 your
   *                 key)
   * @param duration
   *                 key press duration in milliseconds
   */
  public void pressAndRelease(int[] keycodes, int duration) {
    press(keycodes);
    sleep(duration);
    releaseReversed(keycodes);
  }

  /**
   * simulate pressing several keyboard keys (does NOT release the keys!)
   * 
   * @param keycodes
   *                 array of keycodes of keys (use KeyEvent.VK_XXX, where XXX is
   *                 your
   *                 key)
   */
  public void press(int[] keycodes) {
    for (int keycode : keycodes) {
      press(keycode);
    }
  }

  /**
   * simulate releasing several keyboard keys (does NOT press the keys!)
   * 
   * @param keycodes
   *                 array of keycodes of keys (use KeyEvent.VK_XXX, where XXX is
   *                 your
   *                 key)
   */
  public void release(int[] keycodes) {
    for (int keycode : keycodes) {
      release(keycode);
    }
  }

  /**
   * simulate releasing several keyboard keys (does NOT press the keys!)
   * Releases keys in reversed order they were specified.
   * 
   * @param keycodes
   *                 array of keycodes of keys (use KeyEvent.VK_XXX, where XXX is
   *                 your
   *                 key)
   */
  public void releaseReversed(int[] keycodes) {
    for (int i = keycodes.length - 1; i >= 0; i--) {
      robot.keyRelease(keycodes[i]);
    }
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    Runtime.start("swing", "SwingGui");
    KeyboardSim keysim = (KeyboardSim) Runtime.start("keysim", "KeyboardSim");

    sleep(1000);

    keysim.pressAndRelease(new int[] { KeyEvent.VK_SHIFT, KeyEvent.VK_A }, 1000);
  }

}

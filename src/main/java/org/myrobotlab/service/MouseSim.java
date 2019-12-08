package org.myrobotlab.service;

import java.awt.AWTException;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.event.InputEvent;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * @author MaVo (MyRobotLab) / LunDev (GitHub)
 */

public class MouseSim extends Service {
  static final long serialVersionUID = 1L;
  static final Logger log = LoggerFactory.getLogger(MouseSim.class);

  transient private Robot robot;

  public MouseSim(String n, String id) {
    super(n, id);
    try {
      if (!GraphicsEnvironment.isHeadless()) {
        robot = new Robot();
      } else {
        log.warn("headless environment - but MouseSim requires a display");
      }
    } catch (Exception e) {
      log.error("could not create java.awt.Robot", e);
    }
  }

  /**
   * simulate pressing and releasing a mouse button after the desired duration
   * 
   * @param keycode
   *          buttoncode of button (use InputEvent.VK_XXX, where XXX is your
   *          button)
   * @param duration
   *          key press duration in milliseconds
   */
  public void pressAndRelease(int keycode, int duration) {
    press(keycode);
    sleep(duration);
    release(keycode);
  }

  /**
   * simulate pressing a mouse button (does NOT release the button!)
   * 
   * @param keycode
   *          buttoncode of button (use InputEvent.VK_XXX, where XXX is your
   *          button)
   */
  public void press(int keycode) {
    robot.mousePress(keycode);
  }

  /**
   * simulate releasing a mouse button (does NOT press the button!)
   * 
   * @param keycode
   *          buttoncode of button (use InputEvent.VK_XXX, where XXX is your
   *          button)
   */
  public void release(int keycode) {
    robot.mouseRelease(keycode);
  }

  /**
   * simulate pressing and releasing several mouse buttns after the desired
   * duration The buttons are released in reverse order they were pressed.
   * 
   * @param keycodes
   *          array of buttoncodes of buttons (use InputEvent.VK_XXX, where XXX
   *          is your button)
   * @param duration
   *          key press duration in milliseconds
   */
  public void pressAndRelease(int[] keycodes, int duration) {
    press(keycodes);
    sleep(duration);
    releaseReversed(keycodes);
  }

  /**
   * simulate pressing several mouse buttons (does NOT release the buttons!)
   * 
   * @param keycodes
   *          array of buttoncodes of buttons (use InputEvent.VK_XXX, where XXX
   *          is your button)
   */
  public void press(int[] keycodes) {
    for (int keycode : keycodes) {
      press(keycode);
    }
  }

  /**
   * simulate releasing several mouse buttons (does NOT press the buttons!)
   * 
   * @param keycodes
   *          array of buttoncodes of buttons (use InputEvent.VK_XXX, where XXX
   *          is your button)
   */
  public void release(int[] keycodes) {
    for (int keycode : keycodes) {
      release(keycode);
    }
  }

  /**
   * simulate releasing several mouse buttons (does NOT press the buttons!)
   * Releases buttons in reversed order they were specified.
   * 
   * @param keycodes
   *          array of buttoncodes of buttons (use InputEvent.VK_XXX, where XXX
   *          is your button)
   */
  public void releaseReversed(int[] keycodes) {
    for (int i = keycodes.length - 1; i >= 0; i--) {
      robot.keyRelease(keycodes[i]);
    }
  }

  /**
   * simulate moving the mouse cursor to the specified screen coordinates
   * 
   * @param x
   *          X-Position
   * @param y
   *          Y-Position
   */
  public void move(int x, int y) {
    robot.mouseMove(x, y);
  }

  /**
   * simulate rotating (/scrolling) the mouse wheel
   * 
   * @param wheelAmt
   *          rotation amount, negative amounts rotate "backwards"
   */
  public void wheel(int wheelAmt) {
    robot.mouseWheel(wheelAmt);
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    // Runtime.start("swing", "SwingGui");
    MouseSim mousesim = (MouseSim) Runtime.start("keysim", "KeyboardSim");

    sleep(1000);

    mousesim.pressAndRelease(InputEvent.BUTTON1_DOWN_MASK, 1000);
    mousesim.move(10, 10);
    mousesim.wheel(10);
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {
    ServiceType meta = new ServiceType(MouseSim.class.getCanonicalName());
    meta.addDescription("simulate mouse interactions");
    meta.addCategory("control");
    return meta;
  }

}

package org.myrobotlab.oculus.lwjgl;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;

/**
 * OpenGL display manager class.
 * 
 * @author kwatters
 *
 */
public class DisplayManager {

  private static final int FPS_CAP = 120;

  public static void createDisplay() {
    createDisplay(1280, 720);
  }

  public static void createDisplay(int width, int height) {
    ContextAttribs attribs = new ContextAttribs(4, 1).withForwardCompatible(true).withProfileCore(true);
    try {
      Display.setDisplayMode(new DisplayMode(width, height));
      Display.create(new PixelFormat(), attribs);
      Display.setTitle("MRL LWJGL GUI");
    } catch (LWJGLException e) {
      e.printStackTrace();
    }
    GL11.glViewport(0, 0, width, height);
  };

  public static void updateDisplay() {
    // sync the display
    Display.sync(FPS_CAP);
    Display.update();
  };

  public static void closeDisplay() {
    Display.destroy();
  };
}

package org.saintandreas.gl.app;

import static org.lwjgl.opengl.GL11.*;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.PixelFormat;

public abstract class LwjglApp implements Runnable {
  private GLContext glContext = new GLContext();
  protected int width, height;
  protected float aspect = 1.0f;
  protected ContextAttribs contextAttributes = new ContextAttribs();
  protected PixelFormat pixelFormat = new PixelFormat();

  protected abstract void setupDisplay();

  protected abstract void drawFrame();

  protected void setupContext() { }

  protected void setupDisplay(Rectangle r) {
    setupDisplay(r.x, r.y, r.width, r.height);
  }

  protected void setupDisplay(int left, int top, int width, int height) {
    try {
      Display.setDisplayMode(new DisplayMode(width, height));
    } catch (LWJGLException e) {
      throw new RuntimeException(e);
    }
    Display.setLocation(left, top);
    Display.setVSyncEnabled(true);
    onResize(width, height);
  }

  protected void initGl() { }

  @Override
  public void run() {
    try {
      setupContext();
      setupDisplay();
      Display.create(pixelFormat, contextAttributes);
      // This supresses a strange error where when using 
      // the Oculus Rift in direct mode on Windows, 
      // there is an OpenGL GL_INVALID_FRAMEBUFFER_OPERATION 
      // error present immediately after the context has been created.  
      @SuppressWarnings("unused")
      int err = glGetError();
      GLContext.useContext(glContext, false);

      Mouse.create();
      Keyboard.create();
    } catch (LWJGLException e) {
      throw new RuntimeException(e);
    }
    initGl();
    while (!Display.isCloseRequested()) {
      if (Display.wasResized()) {
        onResize(Display.getWidth(), Display.getHeight());
      }
      update();
      drawFrame();
      finishFrame();
    }
    onDestroy();
    Display.destroy();
  }

  protected void finishFrame() {
    Display.update();
  }

  protected void onKeyboardEvent() {
    int key = Keyboard.getEventKey();
    switch (key) {
    case Keyboard.KEY_ESCAPE:
      System.exit(0);
      break;
    }
  }

  protected void onMouseEvent() {

  }

  protected void update() {
    while (Keyboard.next()) {
      onKeyboardEvent();
    }

    while (Mouse.next()) {
      onMouseEvent();
    }
  }

  protected void onResize(int width, int height) {
    this.width = width;
    this.height = height;
    this.aspect = (float) width / (float) height;
  }

  protected void onDestroy() {
  }

  public static Rectangle findNonPrimaryRect() {
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice[] gs = ge.getScreenDevices();
    for (int j = 0; j < gs.length; j++) {
      GraphicsDevice gd = gs[j];
      if (gd == ge.getDefaultScreenDevice()) {
        continue;
      }
      return gd.getDefaultConfiguration().getBounds();
    }
    return null;
  }

}

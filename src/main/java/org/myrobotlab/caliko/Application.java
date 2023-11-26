package org.myrobotlab.caliko;

import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;

import org.myrobotlab.service.Caliko;

import au.edu.federation.caliko.demo.CalikoDemo;
import au.edu.federation.utils.Vec3f;

/**
 * An example application to demonstrate the Caliko library in both 2D and 3D
 * modes.
 *
 * Use up/down cursors to change between 2D/3D mode and left/right cursors to
 * change demos. In 2D mode clicking using the left mouse button (LMB) changes
 * the target location, and you can click and drag. In 3D mode, use W/S/A/D to
 * move the camera and the mouse with LMB held down to look.
 *
 * See the README.txt for further documentation and controls.
 *
 * @author Al Lansley
 * @version 1.0 - 31/01/2016
 */
public class Application {
  // Define cardinal axes
  final Vec3f X_AXIS = new Vec3f(1.0f, 0.0f, 0.0f);
  final Vec3f Y_AXIS = new Vec3f(0.0f, 1.0f, 0.0f);
  final Vec3f Z_AXIS = new Vec3f(0.0f, 0.0f, 1.0f);

  // State tracking variables
  boolean use3dDemo = true;
  int demoNumber = 7;
  boolean fixedBaseMode = true;
  boolean rotateBasesMode = false;
  boolean drawLines = true;
  boolean drawAxes = false;
  boolean drawModels = true;
  boolean drawConstraints = true;
  boolean leftMouseButtonDown = false;
  boolean paused = true;

  // Create our window and OpenGL context
  int windowWidth = 800;
  int windowHeight = 600;
  public OpenGLWindow window = null;

  // Declare a CalikoDemo object which can run our 3D and 2D demonstration
  // scenarios
  transient private CalikoDemo demo;
  transient private Caliko service;
  public boolean running = true;

  public Application(Caliko service) {
    this.service = service;
    window = new OpenGLWindow(this, service, windowWidth, windowHeight);
    demo = new CalikoDemo3D(this);
    mainLoop();
    window.cleanup();
  }

  public Caliko getService() {
    return service;
  }

  private void mainLoop() {
    // Run the rendering loop until the user closes the window or presses Escape
    while (!glfwWindowShouldClose(window.mWindowId) && running) {
      // Clear the screen and depth buffer then draw the demo
      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
      demo.draw();

      // Swap the front and back colour buffers and poll for events
      window.swapBuffers();
      glfwPollEvents();
    }
  }

} // End of Application class

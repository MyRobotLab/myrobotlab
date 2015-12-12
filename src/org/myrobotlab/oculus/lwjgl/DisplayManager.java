package org.myrobotlab.oculus.lwjgl;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;
public class DisplayManager {
	
	// TODO: this is probably wrong resolution?
	private static final int WIDTH=1280;
	private static final int HEIGHT=720;
	private static final int FPS_CAP = 120;
	
	public static void createDisplay() {
		ContextAttribs attribs = new ContextAttribs(4,1).
				withForwardCompatible(true).
				withProfileCore(true);
		try {
			Display.setDisplayMode(new DisplayMode(WIDTH,HEIGHT));
			Display.create(new PixelFormat(), attribs);
			Display.setTitle("MRL LWLGL GUI");
		} catch (LWJGLException e) {
			e.printStackTrace();
		}
		GL11.glViewport(0, 0, WIDTH, HEIGHT);
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

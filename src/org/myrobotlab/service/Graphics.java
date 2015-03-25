/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import java.awt.Color;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Random;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

public class Graphics extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Graphics.class.getCanonicalName());

	public int width = 640;
	public int height = 480;

	public String guiServiceName = null;

	HashMap<String, Color> plotColorMap = new HashMap<String, Color>();

	HashMap<String, Integer> plotXValueMap = new HashMap<String, Integer>();

	public Random rand = new Random();

	int plotTextStart = 10;

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);
		try {
			GUIService gui = new GUIService("gui");
			Graphics graph = new Graphics("graph");
			// OpenCV opencv = new OpenCV("opencv");
			// manual intervention - clear screen

			gui.startService();
			graph.startService();

			graph.attach(gui.getName());
			graph.createGraph(640, 480);
			graph.setColor(new Color(0x666666));
			graph.drawGrid(10, 10);

		} catch (Exception e) {
			Logging.logError(e);
		}

		// graph.draw

	}

	public Graphics(String n) {
		super(n);
	}

	// in order for a graphics service to work it needs to be associated with a
	// GUIService
	// this is how to associate it
	public boolean attach(String guiServiceName) {
		ServiceInterface sw = Runtime.getService(guiServiceName);
		if (sw.getClass() != GUIService.class) {
			log.warn(String.format("attaching type of %s instead of GUIService instance %s", sw.getSimpleName(), guiServiceName));
		}
		this.guiServiceName = guiServiceName;
		return true;
	}

	public void clearRect(Integer x, Integer y, Integer width, Integer height) {
		send(guiServiceName, "clearRect", x, y, width, height);
	}

	public void createGraph() {
		createGraph(width, height);
	}

	/*
	 * publishing points begin -------------------------------------------- This
	 * would be fore static routes and invoking - but it is abondoned for
	 * non-static routes and message "send"ing with more robuts parameter
	 * handling
	 */
	// sent to GUIService - TODO - this message node CAN NOT be private
	// (unfortunately) because the invoking is only allowed to touch
	// public methods - This is a little confusing, because the "user" of this
	// function might think it does something immediately
	// in actuality it does something only when it is INVOKED
	public Dimension createGraph(Dimension d) {
		return d;
	}

	public void createGraph(Integer x, Integer y) {
		send(guiServiceName, "createGraph", new Dimension(x, y));
	}

	public void detach() {
		this.guiServiceName = null;
	}

	public void drawGrid(Integer x, Integer y) {
		send(guiServiceName, "drawGrid", x, y);
	}

	// wrappers end --------------------------

	// wrappers begin --------------------------
	public void drawLine(Integer x1, Integer y1, Integer x2, Integer y2) {
		send(guiServiceName, "drawLine", x1, y1, x2, y2);
	}

	public void drawRect(Integer x, Integer y, Integer width, Integer height) {
		send(guiServiceName, "drawRect", x, y, width, height);
	}

	/*
	 * publishing points end --------------------------------------------
	 */

	public void drawString(String str, Integer x, Integer y) {
		send(guiServiceName, "drawString", str, x, y);
	}

	public void fillOval(Integer x, Integer y, Integer width, Integer height) {
		send(guiServiceName, "fillOval", x, y, width, height);
	}

	public void fillRect(Integer x, Integer y, Integer width, Integer height) {
		send(guiServiceName, "fillRect", x, y, width, height);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "display" };
	}

	/*
	 * public void plot(Message msg) { if (msg.data == null)
	 * 
	 * if (!plotColorMap.containsKey(msg.sender)) { Color color = new
	 * Color(rand.nextInt(16777215)); plotColorMap.put(msg.sender, color);
	 * setColor(plotColorMap.get(msg.sender)); drawString(msg.sender, 10,
	 * plotTextStart); plotTextStart += 10; }
	 * 
	 * if (!plotXValueMap.containsKey(msg.sender)) { Integer x = new Integer(0);
	 * plotXValueMap.put(msg.sender, x); }
	 * 
	 * Integer value = cfg.getInt("height"); if (msg.data != null &&
	 * msg.data.length > 0) { value = (Integer) msg.data[0]; }
	 * 
	 * int x = plotXValueMap.get(msg.sender); int y = cfg.getInt("height") -
	 * value % cfg.getInt("height"); setColor(plotColorMap.get(msg.sender));
	 * drawLine(x, y, x, y); if (x % cfg.getInt("width") == 0 || y %
	 * cfg.getInt("height") == 0) { int textPos = 10; Iterator<String> it =
	 * plotColorMap.keySet().iterator(); while (it.hasNext()) { String name =
	 * it.next(); setColor(plotColorMap.get(name)); drawString(name, 10,
	 * textPos); textPos += 10; } } plotXValueMap.put(msg.getName(), ++x %
	 * cfg.getInt("width")); }
	 */

	@Override
	public String getDescription() {
		return "a graphics service encapsulating Java swing graphic methods";
	}

	public void refreshDisplay() {
		send(guiServiceName, "refreshDisplay");
	}

	public void setColor(Color c) {
		send(guiServiceName, "setColor", c);
	}

}

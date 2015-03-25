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

package org.myrobotlab.control;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;

import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.service.GUIService;

public class VideoWidget extends ServiceGUI {

	HashMap<String, VideoDisplayPanel> displays = new HashMap<String, VideoDisplayPanel>();
	boolean allowFork = false;
	Dimension normalizedSize = null;
	int videoDisplayXPos = 0;
	int videoDisplayYPos = 0;

	public VideoWidget(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	public VideoWidget(final String boundFilterName, final GUIService myService, final JTabbedPane tabs, boolean allowFork) {
		this(boundFilterName, myService, tabs);
		this.allowFork = allowFork;
	}

	public VideoDisplayPanel addVideoDisplayPanel(String source) {
		return addVideoDisplayPanel(source, null);
	}

	public VideoDisplayPanel addVideoDisplayPanel(String source, ImageIcon icon) {
		// FIXME FIXME FIXME - should be FlowLayout No?

		if (videoDisplayXPos % 2 == 0) {
			videoDisplayXPos = 0;
			++videoDisplayYPos;
		}

		gc.gridx = videoDisplayXPos;
		gc.gridy = videoDisplayYPos;

		VideoDisplayPanel vp = new VideoDisplayPanel(source, this, myService, boundServiceName);

		// add it to the map of displays
		displays.put(source, vp);

		// add it to the display
		display.add(vp.myDisplay, gc);

		++videoDisplayXPos;
		display.invalidate();
		myService.pack();

		return vp;
	}

	@Override
	public void attachGUI() {
		subscribe("publishDisplay", "displayFrame", SerializableImage.class);
	}

	public void attachGUI(String srcMethod, String dstMethod, Class<?> c) {
		subscribe(srcMethod, dstMethod, c);
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishDisplay", "displayFrame", SerializableImage.class);
	}

	// multiplex images if desired
	public void displayFrame(SerializableImage img) {

		// FIXME not quite right

		String source = img.getSource();
		if (displays.containsKey(source)) {
			displays.get(source).displayFrame(img);
		} else if (allowFork) {
			VideoDisplayPanel vdp = addVideoDisplayPanel(img.getSource());
			vdp.displayFrame(img);
		} else {
			displays.get("output").displayFrame(img); // FIXME - kludgy !!!
		}
		/*
		 * else if (displays.size() == 0) { VideoDisplayPanel vdp =
		 * addVideoDisplayPanel(img.getSource()); vdp.displayFrame(img); } else
		 * { displays.get("output").displayFrame(img); // catchall }
		 */

	}

	@Override
	// FIXME - do in constructor for krikey sakes !
	public void init() {
		init(null);
	}

	public void init(ImageIcon icon) {
		TitledBorder title;
		title = BorderFactory.createTitledBorder(boundServiceName + " " + " video widget");
		display.setBorder(title);

		addVideoDisplayPanel("output");
	}

	public void removeAllVideoDisplayPanels() {
		Iterator<String> itr = displays.keySet().iterator();
		while (itr.hasNext()) {
			String n = itr.next();
			log.error("removing " + n);
			// removeVideoDisplayPanel(n);
			VideoDisplayPanel vdp = displays.get(n);
			display.remove(vdp.myDisplay);
		}
		displays.clear();
		videoDisplayXPos = 0;
		videoDisplayYPos = 0;
	}

	/*
	 * public void displayFrame(OpenCVData data) { IplImage img =
	 * data.getImage(); displayFrame(img); }
	 */

	public void removeVideoDisplayPanel(String source) {
		if (!displays.containsKey(source)) {
			log.error("cannot remove VideoDisplayPanel " + source);
			return;
		}

		VideoDisplayPanel vdp = displays.remove(source);
		display.remove(vdp.myDisplay);
		display.invalidate();
		myService.pack();
	}

	public void setNormalizedSize(int x, int y) {
		normalizedSize = new Dimension(x, y);
	}
}

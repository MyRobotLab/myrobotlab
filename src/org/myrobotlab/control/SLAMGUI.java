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

import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JTabbedPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.interfaces.VideoGUISource;
import org.slf4j.Logger;

import wiiusej.wiiusejevents.physicalevents.IREvent;

public class SLAMGUI extends ServiceGUI implements ListSelectionListener, VideoGUISource {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(SLAMGUI.class.toString());

	VideoWidget video = null;
	Graphics g = null;
	BufferedImage img = null;

	int width = 1024;
	int height = 768;

	public Random rand = new Random();

	int x;

	int y;

	public SLAMGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	@Override
	public void attachGUI() {
		video.attachGUI();
		subscribe("publishIR", "publishIR", IREvent.class);
		video.displayFrame(new SerializableImage(img, boundServiceName));
	}

	protected ImageIcon createImageIcon(String path, String description) {
		java.net.URL imgURL = getClass().getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	@Override
	public void detachGUI() {
		video.detachGUI();
		unsubscribe("publishIR", "publishIR", IREvent.class);
	}

	public void displayFrame(SerializableImage img) {
		video.displayFrame(img);
	}

	@Override
	public VideoWidget getLocalDisplay() {
		return video;
	}

	@Override
	public void init() {

		img = new BufferedImage(width / 2, height / 2, BufferedImage.TYPE_INT_RGB);
		g = img.getGraphics();
		video.displayFrame(new SerializableImage(img, boundServiceName));

		video = new VideoWidget(boundServiceName, myService, tabs);
		video.init();
		gc.gridx = 0;
		gc.gridy = 0;
		gc.gridheight = 4;
		gc.gridwidth = 2;
		display.add(video.display, gc);

		setCurrentFilterMouseListener();

	}

	// TODO - encapsulate this
	// MouseListener mouseListener = new MouseAdapter() {
	public void setCurrentFilterMouseListener() {
		MouseListener mouseListener = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent mouseEvent) {
				JList theList = (JList) mouseEvent.getSource();
				if (mouseEvent.getClickCount() == 2) {
					int index = theList.locationToIndex(mouseEvent.getPoint());
					if (index >= 0) {
						Object o = theList.getModel().getElementAt(index);
						System.out.println("Double-clicked on: " + o.toString());
					}
				}
			}
		};

	}

	@Override
	public void valueChanged(ListSelectionEvent arg0) {
	}

}

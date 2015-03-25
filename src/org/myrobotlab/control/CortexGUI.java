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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.myrobotlab.control.widget.MemoryWidget;
import org.myrobotlab.control.widget.NodeGUI;
import org.myrobotlab.framework.Status;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.memory.Node;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.service.Cortex;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Tracking;
import org.myrobotlab.service.data.Rectangle;
import org.myrobotlab.service.interfaces.MemoryDisplay;

public class CortexGUI extends ServiceGUI implements MemoryDisplay {

	static final long serialVersionUID = 1L;

	JList textDisplay = new JList();
	DefaultListModel textModel = new DefaultListModel();
	VideoWidget video0 = null;
	JTextField status = new JTextField("", 20);
	JPanel center = new JPanel(new BorderLayout());
	JPanel thumbnails = new JPanel(new GridLayout(5, 30));

	MemoryWidget tree = new MemoryWidget(this);

	public CortexGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", Tracking.class);
		subscribe("publishStatus", "displayStatus", String.class);
		subscribe("publishFrame", "displayFrame", SerializableImage.class);
		subscribe("publishNode", "publishNode", String.class, Node.class);
		subscribe("putNode", "putNode", Node.NodeContext.class);
		video0.attachGUI(); // default attachment
		myService.send(boundServiceName, "crawlAndPublish");
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void clear() {
		textModel.removeAllElements();
		video0.removeAllVideoDisplayPanels();
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", Tracking.class);
		unsubscribe("publishStatus", "displayStatus", String.class);
		unsubscribe("publishFrame", "displayFrame", SerializableImage.class);
		unsubscribe("publishNode", "publishNode", Node.class);
		unsubscribe("putNode", "putNode", Node.NodeContext.class);
		video0.detachGUI(); // default attachment

	}

	@Override
	public void display(Node node) {
		textModel.addElement(String.format("id=%d", node.hashCode()));
		for (Map.Entry<String, ?> nodeData : node.getNodes().entrySet()) {
			String key = nodeData.getKey();
			Object object = nodeData.getValue();
			log.info("{}{}", key, object);

			// display based on type for all non-recursive memory
			Class<?> clazz = object.getClass();
			if (clazz != Node.class) {
				if (clazz == OpenCVData.class) {
					OpenCVData data = (OpenCVData) object;
					SerializableImage lastImage = null;
					video0.removeAllVideoDisplayPanels();
					// for (Map.Entry<String,?> img :
					// data.getImages().entrySet())
					/*
					 * SerializableImage img = data.getImage(); { lastImage =
					 * img; video0.displayFrame(lastImage); }
					 */
					Graphics2D g2d = lastImage.getImage().createGraphics();
					g2d.setColor(Color.RED);
					ArrayList<Rectangle> bb = data.getBoundingBoxArray();
					if (bb != null) {
						for (int i = 0; i < bb.size(); ++i) {
							Rectangle rect = bb.get(i);
							g2d.drawRect((int) rect.x, (int) rect.y, (int) rect.width, (int) rect.height);
						}
					}
				} else if (clazz == ArrayList.class) {
					// test for contained type
					// display.remove(center);
					// display.add(thumbnails, BorderLayout.CENTER);
					thumbnails.removeAll();

					ArrayList list = (ArrayList) object;
					for (int i = 0; i < list.size(); ++i) {
						SerializableImage bimg = (SerializableImage) list.get(i);
						ImageIcon icon = new ImageIcon();
						icon.setImage(bimg.getImage());
						JLabel l = new JLabel(icon, SwingConstants.LEFT);
						icon.setImageObserver(l);
						// l.setIcon(icon);
						thumbnails.add(l);
					}

				} else {
					textModel.addElement(String.format("%s=%s", key, object.toString()));
				}
			}
		}

	}

	public void displayFrame(SerializableImage img) {
		video0.displayFrame(img);
	}

	// TODO - need a refresh - which will publish a single node

	@Override
	public void displayStatus(final Status newStatus) {
		status.setText(newStatus.detail);
	}

	public VideoWidget getLocalDisplay() {
		// TODO Auto-generated method stub
		return video0; // else return video1
	}

	public void getState(final Cortex cortex) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

			}
		});
	}

	@Override
	public void init() {
		video0 = new VideoWidget(boundServiceName, myService, tabs, true);
		video0.init();
		// video0.setNormalizedSize(160, 120);

		textDisplay.setModel(textModel);
		// textDisplay.setEnabled(false);
		textModel.addElement("id 0");
		textModel.addElement("Timestamp 0");
		textModel.addElement("locationX 0");
		textModel.addElement("locationY 0");
		center.add(textDisplay, BorderLayout.NORTH);
		// center.add(video0.getDisplay(), BorderLayout.CENTER);
		center.add(thumbnails, BorderLayout.CENTER);

		status.setEditable(false);

		JPanel west = new JPanel();
		west.add(tree.getDisplay());

		display.setLayout(new BorderLayout());
		display.add(center, BorderLayout.CENTER);
		display.add(west, BorderLayout.WEST);
		display.add(status, BorderLayout.SOUTH);
	}

	// TODO - most likely the body of this needs to go
	// into MemoryWidget
	public void publishNode(Node.NodeContext nodeContext) {
		tree.put(nodeContext.parentPath, nodeContext.node);
	}

	// FIXME !!!! SHOULD BE IN NodeGUI !!!!
	// Add a Node to the GUIService - since a GUIService Tree
	// is constructed to model the memory Tree
	// this is a merge between what the user is interested in
	// and what is in memory
	// memory will grow an update the parts which a user
	// expand - perhaps configuration will allow auto-expand
	// versus user controlled expand of nodes on tree
	public void putNode(Node.NodeContext context) {
		NodeGUI guiNode = tree.put(context.parentPath, context.node);
		/*
		 * new nodes replace null - so testing for null is not valid if (guiNode
		 * == null) { log.error("could not put node {} to path {}",
		 * context.node.getName(), context.parentPath); }
		 */
	}

}

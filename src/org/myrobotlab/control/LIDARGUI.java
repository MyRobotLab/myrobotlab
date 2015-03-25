/**
 *
 * @author greg (at) myrobotlab.org
 *
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 2 of the License, or (at your option) any later
 * version (subject to the "Classpath" exception as provided in the LICENSE.txt
 * file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for details.
 *
 * Enjoy !
 *
 *
 */
package org.myrobotlab.control;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;
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

public class LIDARGUI extends ServiceGUI implements ListSelectionListener, VideoGUISource {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(LIDARGUI.class.toString());
	VideoWidget screen = null;
	Graphics cam = null;
	Graphics graph = null;
	BufferedImage camImage = null;
	BufferedImage graphImage = null;
	int width = 1536;
	int height = 768;
	int xyScale = 2;
	ArrayList<ArrayList<Integer>> history = new ArrayList<ArrayList<Integer>>(); // A
																					// list
																					// of
																					// lists
																					// for
																					// holding
																					// old
																					// data
	public Random rand = new Random();
	int vheight = height / xyScale;
	int vwidth = width / xyScale;
	ArrayList<Integer> hist = new ArrayList<Integer>();
	boolean staticInfo = false;

	DecimalFormat df = new DecimalFormat("#.##");

	int cnt = 0;

	public LIDARGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	@Override
	public void attachGUI() {

		subscribe("publishLidarData", "displaySweepData", int[].class);

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
		unsubscribe("publishLidarData", "displaySweepData", int[].class);
	}

	public void displayFrame(SerializableImage camImage) {
		screen.displayFrame(camImage);
	}

	// TODO - check for copy/ref of parameter moved locally
	// TODO remove IREven
	// find angle
	public void displaySweepData(int[] points) {

		int[] x = new int[points.length]; // Just initializing them to the
											// correct size, values will be
											// overwritten below
		int[] y = new int[points.length];

		int zScale = 8; // 11
		int xOffset = vwidth / 2; // set origin of polar in middle

		if (points != null) {
			// blank screen
			graph.setColor(Color.black);
			graph.fillRect(0, 0, vwidth, vheight);

			// draw static parts

			// Draw Crosshairs
			graph.setColor(Color.gray);
			int midh = (vheight) / 2;
			int midw = (vwidth) / 2;
			graph.drawLine(midw - 20, vheight - 2, midw + 20, vheight - 2);
			graph.drawLine(midw, vheight, midw, vheight - 30);

			for (int i = 0; i < vheight; i += 60) {
				graph.drawLine(0, vheight - i, vwidth, vheight - i);
			}

			drawStaticInfo(); // draw arcs on the screen

			// get the first and last point
			int first = points[0];

			graph.setColor(Color.green);

			int length = points.length;

			float startingAngle = 0;
			if (length == 101 || length == 201 || length == 401) {
				startingAngle = 40;
			}
			int p;

			graph.setColor(Color.green);
			for (int i = 0; i < length; ++i) {
				// polar coordinates !
				// x = (int) ((points[i]) * Math.cos(((i * 100 / length) +
				// startingAngle) * (3.14159 / 180)) + xOffset);
				// y = vheight - (int) ((points[i]) * Math.sin(((i * 100 /
				// length) + startingAngle) * (3.14159 / 180))); //vheight
				// inverts the Y-coordinates
				x[i] = (int) ((points[i]) * Math.cos(((i * 100 / length) + startingAngle) * (3.14159 / 180)));
				y[i] = (int) ((points[i]) * Math.sin(((i * 100 / length) + startingAngle) * (3.14159 / 180))); // vheight
																												// inverts
																												// the
																												// Y-coordinates
				System.out.println(x[i] + "\t" + y[i]);
			}

			// map current value to window size. Values will be plotted so
			// largest value = 95% of the window size
			int[] xMinMax = getMinMax(x); // Get Max and Min values from the
											// array
			int[] yMinMax = getMinMax(y); // Get Max and Min values from the
											// array
			System.out.println("xMin= " + xMinMax[0] + " xmax= " + xMinMax[1] + " ymin= " + yMinMax[0] + " ymax= " + yMinMax[1]);
			for (int i = 0; i < length; ++i) {

				// scales the screen so that the values fit within 95% of the X
				// and Y values
				// x[i] = map(x[i], 100, xMax - 100, 0, (int) (vwidth *
				// 0.95))+xOffset/2 ;
				// y[i] = map(y[i], 100, yMax - 100, 0, (int) (vheight * 0.95));

				/*
				 * Scales y to 8 meters high and X to 8 meters in each direction
				 * the Max distance of a LMS200 and X to
				 */
				x[i] = map(x[i], xMinMax[0], xMinMax[1], 0, vwidth - 5);
				y[i] = vheight - map(y[i], 0, yMinMax[1], 0, vheight - 5); // the
																			// "vheight -"
																			// inverts
																			// the
																			// data
																			// for
																			// display
																			// in
																			// the
																			// window
				System.out.println(x[i] + "\t" + y[i]);

				// graph.drawLine(x[i], y[i], x[i], y[i]);//draws a dot on the
				// point
				if (i > 0) {
					graph.drawLine(x[i - 1], y[i - 1], x[i], y[i]);// draws an
																	// actual
																	// line from
																	// point to
																	// point
				}
			}

			// draw history TODO - draw history first
			/*
			 * history = (dir.compareTo(leftstr)==0)?left:right;
			 */
			int v = 180;
			for (int i = 0; i < history.size(); ++i) {
				ArrayList<Integer> sweep = history.get(i);
				graph.setColor(new Color(v, v, v));
				for (int j = sweep.size(); j > 0; j--) {
					p = sweep.get(j);
					// // x = vwidth - ((int) (p *
					// Math.cos((i*100/length)+startingAngle) * zScale));
					// // y = vheight - ((int) (p *
					// Math.sin((i*100/length)+startingAngle) * zScale));
					// x = vwidth ((int) (p *
					// Math.cos((i*100/length)+startingAngle) *
					// zScale)+xOffset);
					// y = vheight - ((int) (p *
					// Math.sin((i*100/length)+startingAngle) * zScale));
					// graph.drawLine(x, y, x, y);
				}
				v -= 30;

			}

			int maxHistory = 3;

			// history.add(WiiDAR.copy(points));
			// p = history.get(0).get(0);
			if (history.size() > maxHistory) {
				history.remove(0);
			}

			screen.displayFrame(new SerializableImage(graphImage, boundServiceName));

		} else {
			log.error("points null");
		}

	}

	public void drawStaticInfo() {
		graph.setColor(Color.gray);

		int inches = 0;

		// 10 inches per 140 inches

		int r = 160;

		for (r = 160; r < vheight * 2; r += 160) {
			inches += 10;
			graph.drawArc(vwidth / 2 - r / 2, vheight - r / 2, r, r, 0, 180);
			// graph.drawLine(0, vheight - i, vwidth, vheight - i);
			// graph.drawArc(vwidth - 40, vheight, vheight - i - 5, vheight - i
			// - 5, 0, 360);
			// graph.drawArc(vwidth - 40, vheight, 30, 30, 0, 360);
			// graph.drawArc(vwidth/2 - 100, vheight - 100, 200, 200, 0, 180);

			// Print text on the screen
			// graph.drawString("" + inches, vwidth / 2 + r / 2, vheight - 10);
		}

		staticInfo = true;
	}

	@Override
	public VideoWidget getLocalDisplay() {
		return screen;
	}

	public int getMax(int[] points) {
		int max; // Values are stored as {min, max}

		max = points[0]; // set initial values

		for (int i = 1; i < points.length; i++) { // store the highest value
			if (points[i] > max) {
				max = points[i];
			}

		}// end for all values
			// log.info("Min num = "+minMax[0]+" Max num = "+minMax[1]);
		return max;
	}

	public int[] getMinMax(int[] points) {
		int[] minMax = { 0, 0 }; // Values are stored as {min, max}

		minMax[0] = points[0]; // set initial values
		minMax[1] = points[0];

		for (int i = 1; i < points.length; i++) { // store the smallest value
			if (points[i] < minMax[0]) {
				minMax[0] = points[i];
			}
			if (points[i] > minMax[1]) { // Store the highest value
				minMax[1] = points[i];
			}
		}// end for all values
			// log.info("Min num = "+minMax[0]+" Max num = "+minMax[1]);
		return minMax;
	}

	@Override
	public void init() {
		screen = new VideoWidget(boundServiceName, myService, tabs);
		screen.init();

		camImage = new BufferedImage(width / xyScale, height / xyScale, BufferedImage.TYPE_INT_RGB);
		graphImage = new BufferedImage(width / xyScale, height / xyScale, BufferedImage.TYPE_INT_RGB);

		cam = camImage.getGraphics();
		graph = graphImage.getGraphics();

		graph.setColor(Color.green);

		screen.displayFrame(new SerializableImage(graphImage, boundServiceName));

		gc.gridx = 0;
		gc.gridy = 0;
		gc.gridheight = 4;
		gc.gridwidth = 2;
		display.add(screen.display, gc);

		gc.gridx = 2;

		gc.gridx = 0;
		gc.gridheight = 1;
		gc.gridwidth = 1;
		gc.gridy = 5;

		setCurrentFilterMouseListener();

	}

	public int map(int x, int in_min, int in_max, int out_min, int out_max) {
		// log.info("mapping "+ x+ " to a min of "+out_min +
		// " and a max of "+out_max);
		return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
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

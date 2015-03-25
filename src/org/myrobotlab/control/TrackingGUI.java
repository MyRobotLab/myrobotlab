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
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.myrobotlab.framework.Status;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Tracking;

public class TrackingGUI extends ServiceGUI {

	static final long serialVersionUID = 1L;
	JLabel cnt = new JLabel("0");
	JLabel latency = new JLabel("0");
	JTextField status = new JTextField("", 20);
	JLabel xp = new JLabel("0");
	JLabel xi = new JLabel("0");
	JLabel xd = new JLabel("0");
	JLabel yp = new JLabel("0");
	JLabel yi = new JLabel("0");
	JLabel yd = new JLabel("0");

	VideoWidget video0 = null;

	public TrackingGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", Tracking.class);
		subscribe("publishStatus", "setStatus", String.class);
		subscribe("publishFrame", "displayFrame", SerializableImage.class);
		video0.attachGUI(); // default attachment
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", Tracking.class);
		unsubscribe("publishStatus", "setStatus", String.class);
		unsubscribe("publishFrame", "displayFrame", SerializableImage.class);
		video0.detachGUI(); // default attachment

	}

	public void displayFrame(SerializableImage img) {
		video0.displayFrame(img);
	}

	public VideoWidget getLocalDisplay() {
		// TODO Auto-generated method stub
		return video0; // else return video1
	}

	public void getState(final Tracking tracker) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				cnt.setText(String.format("%d ", tracker.cnt));
				latency.setText(String.format("%d ms", tracker.latency));
			}
		});
	}

	@Override
	public void init() {
		video0 = new VideoWidget(boundServiceName, myService, tabs, false);
		video0.init();
		// video0.allowFork = true;

		status.setEditable(false);

		JPanel p = new JPanel(new GridLayout(0, 2));
		p.add(new JLabel("cnt "));
		p.add(cnt);
		p.add(new JLabel("latency "));
		p.add(latency);
		p.add(new JLabel("xp "));
		p.add(xp);
		p.add(new JLabel("xi "));
		p.add(xi);
		p.add(new JLabel("xd "));
		p.add(xd);

		p.add(new JLabel("yp "));
		p.add(yp);
		p.add(new JLabel("yi "));
		p.add(yi);
		p.add(new JLabel("yd "));
		p.add(yd);

		display.setLayout(new BorderLayout());
		display.add(video0.getDisplay(), BorderLayout.CENTER);
		display.add(p, BorderLayout.EAST);
		display.add(status, BorderLayout.SOUTH);
	}

	public void setStatus(final Status newStatus) {
		// SwingUtilities.invokeLater(new Runnable() {
		// public void run() {
		status.setText(String.format("%s %s", newStatus.level, newStatus.detail)); // JTextArea
																					// is
																					// thread
																					// safe
		// }
		// });
	}

	public void setStatus(final String newStatus) {
		// SwingUtilities.invokeLater(new Runnable() {
		// public void run() {
		status.setText(newStatus); // JTextArea is thread safe
		// }
		// });
	}

}

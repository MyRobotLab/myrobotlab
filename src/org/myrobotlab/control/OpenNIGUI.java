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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.openni.OpenNIData;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.OpenNI;

public class OpenNIGUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;

	VideoWidget video;

	JButton captureButton = new JButton("capture");
	JButton recordButton = new JButton("record");
	JButton playbackButton = new JButton("playback");
	JButton GestureRecognitionButton = new JButton("point cloud");
	JButton depthCloudButton = new JButton("depth");
	JButton imageCloudButton = new JButton("image");

	JPanel eastPanel = new JPanel();

	SerializableImage source = new SerializableImage(null, "kinect");

	String displayType = "display"; // display (composite of skeleton or
									// anything OpenNI has written to "frame") |
									// depth | rgb

	public OpenNIGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		video = new VideoWidget(boundServiceName, myService, tabs);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (o == captureButton) {
			if (captureButton.getText().equals("capture")) {
				myService.send(boundServiceName, "capture");
				captureButton.setText("stop capture");
			} else {
				myService.send(boundServiceName, "stopCapture");
				captureButton.setText("capture");
			}
		} else if (o == recordButton) {
			if (recordButton.getText().equals("record")) {
				myService.send(boundServiceName, "record");
				recordButton.setText("stop recording");
			} else {
				myService.send(boundServiceName, "stopRecording");
				recordButton.setText("record");
			}
		} else if (o == playbackButton) {
			if (playbackButton.getText().equals("playback")) {
				myService.send(boundServiceName, "playback");
				playbackButton.setText("stop playback");
			} else {
				myService.send(boundServiceName, "stopPlayback");
				playbackButton.setText("playback");
			}
		}
	}

	@Override
	public void attachGUI() {
		// subscribe & ask for the initial state of the service
		subscribe("publishState", "getState", OpenNI.class);
		subscribe("publishOpenNIData", "publishOpenNIData");
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", OpenNI.class);
		unsubscribe("publishOpenNIData", "publishOpenNIData");
	}

	public void getState(OpenNI openni) {
		// TODO - update state
	}

	@Override
	public void init() {

		video.init();

		display.setLayout(new BorderLayout());
		display.add(video.getDisplay(), BorderLayout.CENTER);

		eastPanel.setLayout(new GridLayout(6, 1));
		eastPanel.add(captureButton);
		eastPanel.add(recordButton);
		eastPanel.add(playbackButton);
		eastPanel.add(depthCloudButton);
		eastPanel.add(imageCloudButton);

		display.add(eastPanel, BorderLayout.EAST);

		captureButton.addActionListener(this);
		recordButton.addActionListener(this);
		playbackButton.addActionListener(this);

	}

	public void publishFrame(SerializableImage si) {
		video.displayFrame(si);
	}

	public void publishOpenNIData(OpenNIData data) {
		// TODO - display type based on config
		if ("display".equals(displayType)) {
			source.setImage(data.depth);
		} else if ("depth".equals(displayType)) {
			source.setImage(data.depth);
		} else if ("rgb".equals(displayType)) {
			source.setImage(data.rbgPImage.getImage());
		}
		video.displayFrame(source);
	}

}

/**
 *                    
 * @author GroG (at) myrobotlab.org
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

package org.myrobotlab.swing.vision;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.bytedeco.javacv.FrameGrabber;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.vision.FilterWrapper;
import org.myrobotlab.vision.OpenCVFilterInput;

public class OpenCVFilterInputGui extends OpenCVFilterGui implements ActionListener {

	// input
	JPanel captureCfg = new JPanel();
	JRadioButton fileRadio = new JRadioButton();
	JRadioButton cameraRadio = new JRadioButton();
	JTextField inputFile = new JTextField("http://localhost:8888/cgi/videostream");
	JButton inputFileButton = new JButton("open file");

	JComboBox<String> kinectImageOrDepth = new JComboBox<String>(new String[] { "image", "depth", "interleave" });
	JComboBox<String> grabberTypeSelect = null;

	JComboBox<Integer> cameraIndex = new JComboBox<Integer>(new Integer[] { 0, 1, 2, 3, 4, 5 });

	JButton capture = new JButton("capture");

	JComboBox<String> IPCameraType = new JComboBox<String>(new String[] { "foscam FI8918W" });
	DefaultComboBoxModel<String> pipelineHookModel = new DefaultComboBoxModel<String>();
	JComboBox<String> pipelineHook = new JComboBox<String>(pipelineHookModel);
	JLabel cameraIndexLable = new JLabel("camera");
	JLabel modeLabel = new JLabel("mode");
	JLabel inputFileLable = new JLabel("file");
	
	// makes it camera "OR" file
	ButtonGroup groupRadio = new ButtonGroup();

	public OpenCVFilterInputGui(String boundFilterName, String boundServiceName, SwingGui myService) {
		super(boundFilterName, boundServiceName, myService);

		// we don't want the usual input sources - so clear the display
		main.removeAll();

		ArrayList<String> frameGrabberList = new ArrayList<String>();
		// Add all of the OpenCV defined FrameGrabbers
		for (int i = 0; i < FrameGrabber.list.size(); ++i) {
			String ss = FrameGrabber.list.get(i);
			String fg = ss.substring(ss.lastIndexOf(".") + 1);
			frameGrabberList.add(fg);
		}

		grabberTypeSelect = new JComboBox(frameGrabberList.toArray());

		// Add the MRL Frame Grabbers
		frameGrabberList.add("IPCamera");
		frameGrabberList.add("Pipeline"); // service which implements
		// ImageStreamSource
		frameGrabberList.add("ImageFile");
		frameGrabberList.add("SlideShowFile");
		frameGrabberList.add("Sarxos");

		// build input begin ------------------

		grabberTypeSelect.addActionListener(this);

		// capture panel
		JPanel cpanel = new JPanel();
		cpanel.setBorder(BorderFactory.createEtchedBorder());
		cpanel.add(capture);
		cpanel.add(grabberTypeSelect);

		/*
	
		captureCfg.setLayout(new BorderLayout());
		captureCfg.setBorder(BorderFactory.createEtchedBorder());
		// mode for kinect ???
		captureCfg.add(modeLabel);
		captureCfg.add(kinectImageOrDepth);
		// pipeline 
		captureCfg.add(IPCameraType);
		captureCfg.add(pipelineHook);
		*/


		// camera
		JPanel cameraPanel = new JPanel();
		cameraPanel.add(cameraRadio);
		cameraPanel.add(cameraIndexLable);
		cameraPanel.add(cameraIndex);
		
		// file
		JPanel filePanel = new JPanel();
		filePanel.add(fileRadio);
		filePanel.add(inputFileLable);
		filePanel.add(inputFile);
		
	
		// input.add(cpanel);
		// input.add(captureCfg);
		
		// camera OR file
		groupRadio.add(cameraRadio);
		groupRadio.add(fileRadio);
		
		// cpanel.setAlignmentX(Component.LEFT_ALIGNMENT);		
		// cpanel.setAlignmentX(Component.LEFT_ALIGNMENT);		
		// filePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		// vbox.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		display.setLayout(new BoxLayout(display, BoxLayout.Y_AXIS));
		display.add(cpanel);
		display.add(cameraPanel);
		display.add(filePanel);
		
		main.add(display, BorderLayout.EAST);
		// build input end ------------------
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();

		if (o == grabberTypeSelect) {
			handleGrabberType(grabberTypeSelect);
		}

		OpenCVFilterInput filter = (OpenCVFilterInput) boundFilter.filter;

		setFilterState(filter);
	}

	private void handleGrabberType(JComboBox<String> grabberTypeSelect2) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				String type = (String) grabberTypeSelect.getSelectedItem();
				if ("OpenKinect".equals(type)) {
					cameraRadio.setSelected(true);
					cameraIndexLable.setVisible(true);
					cameraIndex.setVisible(true);
					modeLabel.setVisible(true);
					kinectImageOrDepth.setVisible(true);
					inputFileLable.setVisible(false);
					inputFile.setVisible(false);
					fileRadio.setVisible(false);

					IPCameraType.setVisible(false);
					pipelineHook.setVisible(false);
				}

				if ("OpenCV".equals(type) || "VideoInput".equals(type) || "FFmpeg".equals(type)) {
					// cameraRadio.setSelected(true);
					kinectImageOrDepth.setSelectedItem("image");
					// myOpenCV.format
					// =
					// "image";
					cameraIndexLable.setVisible(true);
					cameraIndex.setVisible(true);
					modeLabel.setVisible(false);
					kinectImageOrDepth.setVisible(false);
					inputFileLable.setVisible(true);
					inputFile.setVisible(true);

					fileRadio.setVisible(true);
					cameraRadio.setVisible(true);

					IPCameraType.setVisible(false);
					pipelineHook.setVisible(false);
				}

				if ("IPCamera".equals(type)) {
					// cameraRadio.setSelected(true);
					// kinectImageOrDepth.setSelectedItem("image");
					// myOpenCV.format
					// =
					// "image";
					cameraIndexLable.setVisible(false);
					cameraIndex.setVisible(false);
					modeLabel.setVisible(false);
					kinectImageOrDepth.setVisible(false);
					inputFileLable.setVisible(true);
					inputFile.setVisible(true);
					fileRadio.setSelected(true);

					fileRadio.setVisible(false);
					cameraRadio.setVisible(false);

					IPCameraType.setVisible(true);
					pipelineHook.setVisible(false);
				}

				if ("Pipeline".equals(type)) {
					// cameraRadio.setSelected(true);
					// kinectImageOrDepth.setSelectedItem("image");
					// myOpenCV.format
					// =
					// "image";
					cameraIndexLable.setVisible(false);
					cameraIndex.setVisible(false);
					modeLabel.setVisible(false);
					kinectImageOrDepth.setVisible(false);
					inputFileLable.setVisible(false);
					inputFile.setVisible(false);
					fileRadio.setSelected(true);

					fileRadio.setVisible(false);
					cameraRadio.setVisible(false);

					IPCameraType.setVisible(false);
					// this
					// has
					// static
					// /
					// global
					// internals
					// VideoSources vs = new VideoSources();
					// Set<String> p = vs.getKeySet();
					/*
					 * pipelineHookModel.removeAllElements(); for (String i : p)
					 * { pipelineHookModel.insertElementAt(i, 0); }
					 * pipelineHook.setVisible(true);
					 */
				}
			}
		});
	}

	// @Override
	public void attachGui() {
		log.debug("attachGui");

	}

	// @Override
	public void detachGui() {
		log.debug("detachGui");

	}

	@Override
	public void getFilterState(final FilterWrapper filterWrapper) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				OpenCVFilterInput filter = (OpenCVFilterInput) filterWrapper.filter;
			}
		});
	}

}

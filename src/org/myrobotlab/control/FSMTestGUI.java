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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;

import org.myrobotlab.image.KinectImageNode;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.memory.NodeDeprecate;
import org.myrobotlab.service.FSMTest;
import org.myrobotlab.service.FSMTest.MatchResult;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.interfaces.VideoGUISource;
import org.slf4j.Logger;

public class FSMTestGUI extends ServiceGUI implements VideoGUISource {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(FSMTestGUI.class.toString());

	VideoWidget bestFitVideo = null;
	VideoWidget newImageVideo = null;
	// VideoWidget video1 = null;
	// VideoWidget video2 = null;
	BufferedImage graph = null;
	Graphics g = null;
	JLabel matchIndex = new JLabel("0");
	JLabel matchWord = new JLabel();

	public FSMTestGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	public class StateActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			JButton button = (JButton) e.getSource();
			myService.send(boundServiceName, "heard", button.getText());
		}

	}

	public void init() {

		StateActionListener state = new StateActionListener();

		JButton b = new JButton("look");
		b.addActionListener(state);
		display.add(b, gc);

		++gc.gridx;
		b = new JButton("ball");
		b.addActionListener(state);
		display.add(b, gc);

		++gc.gridx;
		b = new JButton("hand");
		b.addActionListener(state);
		display.add(b, gc);

		++gc.gridx;
		b = new JButton("box");
		b.addActionListener(state);
		display.add(b, gc);

		++gc.gridx;
		b = new JButton("cup");
		b.addActionListener(state);
		display.add(b, gc);

		++gc.gridx;
		b = new JButton("save");
		b.addActionListener(state);
		display.add(b, gc);

		gc.gridx = 0;

		++gc.gridy;
		display.add(matchWord, gc);

		++gc.gridy;
		display.add(new JLabel("match index : "), gc);

		++gc.gridx;
		display.add(matchIndex, gc);

		gc.gridx = 0;
		++gc.gridy;

		bestFitVideo = new VideoWidget(boundServiceName, myService, tabs);
		bestFitVideo.init();
		display.add(bestFitVideo.display, gc);

		gc.gridwidth = 10;
		++gc.gridx;
		newImageVideo = new VideoWidget(boundServiceName, myService, tabs);
		newImageVideo.init();
		display.add(newImageVideo.display, gc);

		/*
		 * ++gc.gridy; video1 = new VideoWidget(boundServiceName, myService);
		 * video1.init(); display.add(video1.display, gc);
		 * 
		 * 
		 * ++gc.gridx; video2 = new VideoWidget(boundServiceName, myService);
		 * video2.init(); display.add(video2.display, gc);
		 */

	}

	public void displayMatch(MatchResult result) {
		matchWord.setText(result.word);
		matchIndex.setText("" + result.matchIndex);
		
		bestFitVideo.displayFrame(result.bestFit.cropped);
		
		newImageVideo.displayFrame(result.newImage.cropped);
	}

	// TODO - com....Sensor interface
	public void displayVideo0(HashMap<String, NodeDeprecate> memory) {

		Iterator<String> itr = memory.keySet().iterator();
		NodeDeprecate unknown = memory.get(FSMTest.UNKNOWN);
		log.error("cvBoundingBox {}",unknown.imageData.get(0).cvBoundingBox);
		log.error("boundingBox {}",unknown.imageData.get(0).boundingBox);

		while (itr.hasNext()) {
			String n = itr.next();
			NodeDeprecate node = memory.get(n);
			for (int i = 0; i < node.imageData.size(); ++i) {
				KinectImageNode kin = node.imageData.get(i);
				// kin.extraDataLabel
				VideoDisplayPanel vdp = bestFitVideo.addVideoDisplayPanel(n);
				vdp.extraDataLabel.setText(node.word + " match:" + kin.lastGoodFitIndex);
				// TODO - write bounding box - mask & crop image - do this at
				// node level?
				// in filter
				SerializableImage si = kin.cameraFrame;
				si.setSource(node.word);
				Graphics g = si.getImage().getGraphics();
				g.setColor(Color.WHITE);
				Rectangle r = kin.boundingBox;
				g.drawRect(r.x, r.y, r.width, r.height);
				g.dispose();
				bestFitVideo.displayFrame(si);
			}
		}
	}

	/*
	 * public void displayVideo1(Node node) { displayVideo(video1, node); }
	 * 
	 * public void displayVideo2(Node node) { displayVideo(video2, node); }
	 */

	public void clearVideo0() {
		bestFitVideo.removeAllVideoDisplayPanels();
	}

	/*
	 * 
	 * public void displayVideo(VideoWidget v, Node node) { for (int i = 0; i <
	 * node.imageData.size(); ++i) {
	 * v.displayFrame(OpenCV.publishFrame("unknown " +
	 * i,node.imageData.get(i).cvCameraFrame.getBufferedImage())); } }
	 * 
	 * public void displayMatchResult (IplImage img) {
	 * video2.displayFrame(OpenCV.publishFrame("matched result",
	 * img.getBufferedImage())); }
	 */
	@Override
	public void attachGUI() {
		// bestFitVideo.attachGUI(); // is this necessary - it ju
		// newImageVideo.attachGUI();
		// video1.attachGUI();
		// video2.attachGUI();
		// subscribe(outMethod, inMethod, parameterType)
		// subscribe("publishVideo0", "displayVideo0", HashMap.class);
		subscribe("publishMatch", "displayMatch", MatchResult.class);
		// subscribe("clearVideo0"); // FIXME - bad notation .. come on !
		// subscribe("publishVideo1", "displayVideo1", Node.class);
		// subscribe("publishVideo2", "displayVideo2", Node.class);
		// subscribe("publishMatchResult", "displayMatchResult",
		// IplImage.class);
		myService.send(boundServiceName, "attach", (Object) myService.getName());
	}

	@Override
	public void detachGUI() {
		// bestFitVideo.detachGUI();
		// unsubscribe("publishVideo0", "displayVideo0", HashMap.class);
		// unsubscribe("clearVideo0"); // FIXME - bad notation .. come on !
		// video1.detachGUI();
		// video2.detachGUI();
		myService.send(boundServiceName, "detach");
	}

	@Override
	public VideoWidget getLocalDisplay() {
		// TODO Auto-generated method stub
		return bestFitVideo;
	}

}

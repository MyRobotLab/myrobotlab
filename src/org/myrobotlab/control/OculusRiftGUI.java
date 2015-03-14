package org.myrobotlab.control;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.OculusRift;
import org.myrobotlab.service.OculusRift.RiftFrame;
import org.myrobotlab.service.interfaces.VideoGUISource;
import org.slf4j.Logger;

public class OculusRiftGUI extends ServiceGUI implements VideoGUISource, ActionListener {
	
	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(OculusRiftGUI.class.toString());
	
	// Left and right eye video widgets
	VideoWidget leftEye = null;
	VideoWidget rightEye = null;
	
	public OculusRiftGUI(String boundServiceName, GUIService myService, JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public VideoWidget getLocalDisplay() {
		// TODO : Who calls this ever?
		return null;
	}

	@Override
	public void init() {
		// Create the 2 video widgets
		leftEye = new VideoWidget(String.format("%s."+OculusRift.LEFT_OPEN_CV, boundServiceName), myService, tabs, false);
		leftEye.init();
		
		rightEye = new VideoWidget(String.format("%s."+OculusRift.RIGHT_OPEN_CV, boundServiceName), myService, tabs, false);
		rightEye.init();
		
		JPanel leftVideoPanel = new JPanel();
		leftVideoPanel.add(leftEye.display);
		
		JPanel rightVideoPanel = new JPanel();
		rightVideoPanel.add(rightEye.display);
		
		// the two video widgets add to display.
		display.add(leftVideoPanel);
		display.add(rightVideoPanel);
		
	}
	
	public void onRiftFrame(RiftFrame frame){
		if (frame.left != null && frame.right != null) {
			leftEye.displayFrame(frame.left);
			rightEye.displayFrame(frame.right);
		}
	}

	@Override
	public void attachGUI() {
		// gui msg routes created
		subscribe("publishState", "getState", OculusRift.class);
		myService.send(boundServiceName, "publishState");
		subscribe("publishRiftFrame","onRiftFrame", RiftFrame.class);
	}

	@Override
	public void detachGUI() {
		// gui msg routes removed
		unsubscribe("publishState", "getState", OculusRift.class);
		unsubscribe("publishRiftFrame","onRiftFrame", RiftFrame.class);
	}
	
}

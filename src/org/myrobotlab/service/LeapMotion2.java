package org.myrobotlab.service;

import java.io.IOException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.leap.LeapMotionListener;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Frame;
import com.leapmotion.leap.Gesture;
import com.leapmotion.leap.Hand;


public class LeapMotion2 extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(LeapMotion2.class);
	
	LeapMotionListener listener = null;
	Controller controller = new Controller();
	
	public LeapMotion2(String n) {
		super(n);
		listener = new LeapMotionListener(this);
	}
	
	@Override
	public String getDescription() {
		return "used as a general template";
	}
	
	public float getRightStrength(){
		Frame frame = controller.frame();
		Hand hand = frame.hands().rightmost();
		float strength = hand.grabStrength();
		return strength;
	}
	
	public float getLeftStrength(){
		Frame frame = controller.frame();
		Hand hand = frame.hands().leftmost();
		float strength = hand.grabStrength();
		return strength;
	}

	public Frame publishFrame(Frame frame) {
		return frame;
	}
	
	public void addFrameListener(Service service){
		addListener("publishFrame", service.getName(), "onFrame", Frame.class);
	}
	
	public Controller publishInit(Controller controller) {
		return controller;
	}

	public Controller publishConnect(Controller controller) {
		return controller;
	}

	public Controller publishDisconnect(Controller controller) {
		return controller;
	}

	public Controller publishExit(Controller controller) {
		return controller;
	}
	
	public void startTracking(){
		controller.addListener(listener);
	}
	
	public void stopTracking(){
		controller.removeListener(listener);
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		LeapMotion2 leap = new LeapMotion2("leap");
		leap.startService();
			
		Runtime.start("gui", "GUIService");
		leap.startTracking();

        // Have the sample listener receive events from the controller

        // Keep this process running until Enter is pressed
        log.info("Press Enter to quit...");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Remove the sample listener when done
	}

}

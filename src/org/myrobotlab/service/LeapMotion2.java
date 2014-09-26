package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.Math;

import com.leapmotion.leap.*;
import com.leapmotion.leap.Gesture.State;

import org.myrobotlab.leap.*;
import org.myrobotlab.service.WiiDAR.Point;


public class LeapMotion2 extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(LeapMotion2.class);
	
	public LeapMotion2(String n) {
		super(n);
	}
	
	@Override
	public String getDescription() {
		return "used as a general template";
	}
	
	public float getStrength(){
		Controller controller = new Controller();
		Listener listener = new Listener();
		controller.addListener(listener);
		Frame frame = controller.frame();
		Hand hand = frame.hands().rightmost();
		float strength = hand.grabStrength();
		return strength;
	}
	
	public void strength(){
		
		System.out.println("event !");
		Controller controller = new Controller();
		Listener listener = new Listener();
		controller.addListener(listener);
		Frame frame = controller.frame();
		Hand hand = frame.hands().rightmost();
		float strength = hand.grabStrength();
		invoke("publishStrenght", strength);
		System.out.println("published!");
	
    }
	
	
    public void publishStrength(){
    	System.out.println("event !");
    }
	

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		LeapMotion2 leap = new LeapMotion2("leap");
			
		Runtime.start("gui", "GUIService");
		
		Sample2 listener = new Sample2();
        Controller controller = new Controller();

        // Have the sample listener receive events from the controller
        controller.addListener(listener);
        

        // Keep this process running until Enter is pressed
        System.out.println("Press Enter to quit...");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Remove the sample listener when done
        controller.removeListener(listener);


		
		
	}
}

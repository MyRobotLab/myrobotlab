package org.myrobotlab.leap;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.LeapMotion2;
import org.slf4j.Logger;

import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Frame;
import com.leapmotion.leap.Gesture;
import com.leapmotion.leap.Hand;
import com.leapmotion.leap.Listener;

public class LeapMotionListener extends Listener {
	
	public final static Logger log = LoggerFactory.getLogger(LeapMotionListener.class);
	LeapMotion2 myService = null; 
	
	public LeapMotionListener(LeapMotion2 myService){ 
		 this.myService = myService; 
    }

	
	public void onInit(Controller controller) {
        log.info("Initialized");
    }

    public void onConnect(Controller controller) {
        log.info("Connected");
        controller.enableGesture(Gesture.Type.TYPE_SWIPE);
        controller.enableGesture(Gesture.Type.TYPE_CIRCLE);
        controller.enableGesture(Gesture.Type.TYPE_SCREEN_TAP);
        controller.enableGesture(Gesture.Type.TYPE_KEY_TAP);
    }

    public void onDisconnect(Controller controller) {
        //Note: not dispatched when running in a debugger.
        log.info("Disconnected");
    }

    public void onExit(Controller controller) {
        log.info("Exited");
    }
	
	public void onFrame(Controller controller){
	Frame frame = controller.frame();
	Hand hand = frame.hands().rightmost();
	log.info("Strenght is: " + hand.grabStrength());
	float strength = hand.grabStrength();
	myService.publishStrength();
    
	
	if (!frame.hands().isEmpty()) {
        log.info("");
    }
}}

package org.myrobotlab.leap;

import java.io.IOException;
import java.lang.Math;

import org.myrobotlab.framework.Service;
import org.myrobotlab.service.LeapMotion2;

import com.leapmotion.leap.*;
import com.leapmotion.leap.Gesture.State;

public class LeapMotionListener extends Listener {
	
	LeapMotion2 myService = null; 
	
	public LeapMotionListener(LeapMotion2 myService){ 
		 this.myService = myService; 
    }

	
	public void onInit(Controller controller) {
        System.out.println("Initialized");
    }

    public void onConnect(Controller controller) {
        System.out.println("Connected");
        controller.enableGesture(Gesture.Type.TYPE_SWIPE);
        controller.enableGesture(Gesture.Type.TYPE_CIRCLE);
        controller.enableGesture(Gesture.Type.TYPE_SCREEN_TAP);
        controller.enableGesture(Gesture.Type.TYPE_KEY_TAP);
    }

    public void onDisconnect(Controller controller) {
        //Note: not dispatched when running in a debugger.
        System.out.println("Disconnected");
    }

    public void onExit(Controller controller) {
        System.out.println("Exited");
    }
	
	public void onFrame(Controller controller){
	Frame frame = controller.frame();
	Hand hand = frame.hands().rightmost();
	System.out.println("Strenght is: " + hand.grabStrength());
	float strength = hand.grabStrength();
	myService.publishStrength();
    
	
	if (!frame.hands().isEmpty()) {
        System.out.println();
    }
}}

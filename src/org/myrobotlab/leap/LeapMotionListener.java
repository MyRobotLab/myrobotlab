package org.myrobotlab.leap;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.LeapMotion2;
import org.slf4j.Logger;

import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Gesture;
import com.leapmotion.leap.Listener;

public class LeapMotionListener extends Listener {

	public final static Logger log = LoggerFactory.getLogger(LeapMotionListener.class);
	LeapMotion2 myService = null;

	public LeapMotionListener(LeapMotion2 myService) {
		this.myService = myService;
	}

	public void onInit(Controller controller) {
		log.info("publishInit");
		myService.invoke("onInit", controller);
	}

	public void onConnect(Controller controller) {
		log.info("Connected");
		controller.enableGesture(Gesture.Type.TYPE_SWIPE);
		controller.enableGesture(Gesture.Type.TYPE_CIRCLE);
		controller.enableGesture(Gesture.Type.TYPE_SCREEN_TAP);
		controller.enableGesture(Gesture.Type.TYPE_KEY_TAP);
		log.info("Connected");
		myService.invoke("publishConnect", controller);
	}

	public void onDisconnect(Controller controller) {
		log.info("onDisconnect");
		myService.invoke("publishDisconnect", controller);
	}

	public void onExit(Controller controller) {
		log.info("onExit");
		myService.invoke("publishExit", controller);
	}

	public void onFrame(Controller controller) {
		myService.invoke("publishFrame", controller.frame());
	}
}

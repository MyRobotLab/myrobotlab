package org.myrobotlab.leap;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.LeapMotion2;
import org.myrobotlab.service.LeapMotion2.LeapData;
import org.slf4j.Logger;

import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Finger;
import com.leapmotion.leap.Gesture;
import com.leapmotion.leap.Hand;
import com.leapmotion.leap.Listener;
import com.leapmotion.leap.Vector;

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
		LeapData data = new LeapData();
		Hand lh = controller.frame().hands().leftmost();
		
		Finger f = lh.fingers().get(0);
		Vector palmNormal = lh.palmNormal();
		Vector fDir = f.direction();
		// TODO: validate that this is what we actually want.
		// otherwise we can directly compute the angleTo in java.
		float angleInRadians = palmNormal.angleTo(fDir);
		// convert to degrees so it's easy to pass to servos
		int angle = (int)Math.toDegrees(angleInRadians);
		
		data.frame = controller.frame();
		data.leftHand.thumb = angle;
		
		myService.lastLeapData = data;
		
		myService.invoke("publishLeapData", data);
	}
}

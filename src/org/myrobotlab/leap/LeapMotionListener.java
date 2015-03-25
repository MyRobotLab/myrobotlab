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

	private double computeAngleDegrees(Finger f, Vector palmNormal) {
		Vector fDir = f.direction();
		// TODO: validate that this is what we actually want.
		// otherwise we can directly compute the angleTo in java.
		double angleInRadians = palmNormal.angleTo(fDir);
		// convert to degrees so it's easy to pass to servos
		double angle = Math.toDegrees(angleInRadians);
		return angle;
	}

	private LeapMotion2.Hand mapLeapHandData(Hand lh) {
		LeapMotion2.Hand mrlHand = new LeapMotion2.Hand();
		// process the normal
		Vector palmNormal = lh.palmNormal();
		mrlHand.palmNormalX = palmNormal.getX();
		mrlHand.palmNormalY = palmNormal.getY();
		mrlHand.palmNormalZ = palmNormal.getZ();

		// handle the fingers.
		for (Finger.Type t : Finger.Type.values()) {
			Finger f = lh.fingers().get(t.ordinal());
			double angle = computeAngleDegrees(f, palmNormal);
			if (t.equals(Finger.Type.TYPE_INDEX))
				mrlHand.index = angle;
			else if (t.equals(Finger.Type.TYPE_MIDDLE))
				mrlHand.middle = angle;
			else if (t.equals(Finger.Type.TYPE_RING))
				mrlHand.ring = angle;
			else if (t.equals(Finger.Type.TYPE_PINKY))
				mrlHand.pinky = angle;
			else if (t.equals(Finger.Type.TYPE_THUMB))
				mrlHand.thumb = angle;
			else
				log.warn("Unknown finger! eek..");
		}
		return mrlHand;
	}

	@Override
	public void onConnect(Controller controller) {
		log.info("Connected");
		controller.enableGesture(Gesture.Type.TYPE_SWIPE);
		controller.enableGesture(Gesture.Type.TYPE_CIRCLE);
		controller.enableGesture(Gesture.Type.TYPE_SCREEN_TAP);
		controller.enableGesture(Gesture.Type.TYPE_KEY_TAP);
		log.info("Connected");
		myService.invoke("publishConnect", controller);
	}

	@Override
	public void onDisconnect(Controller controller) {
		log.info("onDisconnect");
		myService.invoke("publishDisconnect", controller);
	}

	@Override
	public void onExit(Controller controller) {
		log.info("onExit");
		myService.invoke("publishExit", controller);
	}

	@Override
	public void onFrame(Controller controller) {
		LeapData data = new LeapData();
		// The old publishFrame method for those who want it.
		data.frame = controller.frame();
		myService.invoke("publishFrame", data.frame);
		// grab left/right hands
		Hand lh = controller.frame().hands().leftmost();
		Hand rh = controller.frame().hands().rightmost();
		// map the data to the MRL Hand pojo
		LeapMotion2.Hand mrlLHand = mapLeapHandData(lh);
		LeapMotion2.Hand mrlRHand = mapLeapHandData(rh);
		// set them to the LeapData obj
		data.leftHand = mrlLHand;
		data.rightHand = mrlRHand;
		// Grab the current frame
		// Track the last valid data frame.
		// TODO: test and make sure this is worky?
		if (data.frame.isValid()) {
			myService.lastLeapData = data;
			// only publish valid frames ?
			myService.invoke("publishLeapData", data);
		}

	}

	@Override
	public void onInit(Controller controller) {
		log.info("publishInit");
		myService.invoke("onInit", controller);
	}
}

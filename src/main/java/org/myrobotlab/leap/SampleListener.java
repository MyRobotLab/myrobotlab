package org.myrobotlab.leap;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import com.leapmotion.leap.Arm;
import com.leapmotion.leap.Bone;
import com.leapmotion.leap.CircleGesture;
import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Finger;
import com.leapmotion.leap.Frame;
import com.leapmotion.leap.Gesture;
import com.leapmotion.leap.Gesture.State;
import com.leapmotion.leap.GestureList;
import com.leapmotion.leap.Hand;
import com.leapmotion.leap.KeyTapGesture;
import com.leapmotion.leap.Listener;
import com.leapmotion.leap.ScreenTapGesture;
import com.leapmotion.leap.SwipeGesture;
import com.leapmotion.leap.Tool;
import com.leapmotion.leap.Vector;

public class SampleListener extends Listener {

  public final static Logger log = LoggerFactory.getLogger(SampleListener.class);
  
  @Override
  public void onConnect(Controller controller) {
    log.info("Connected");
    controller.enableGesture(Gesture.Type.TYPE_SWIPE);
    controller.enableGesture(Gesture.Type.TYPE_CIRCLE);
    controller.enableGesture(Gesture.Type.TYPE_SCREEN_TAP);
    controller.enableGesture(Gesture.Type.TYPE_KEY_TAP);
  }

  @Override
  public void onDisconnect(Controller controller) {
    // Note: not dispatched when running in a debugger.
    log.info("Disconnected");
  }

  @Override
  public void onExit(Controller controller) {
    log.info("Exited");
  }

  @Override
  public void onFrame(Controller controller) {
    // Get the most recent frame and report some basic information
    Frame frame = controller.frame();
    log.info("Frame id: " + frame.id() + ", timestamp: " + frame.timestamp() + ", hands: " + frame.hands().count() + ", fingers: " + frame.fingers().count() + ", tools: "
        + frame.tools().count() + ", gestures " + frame.gestures().count());

    // Get hands
    for (Hand hand : frame.hands()) {
      String handType = hand.isLeft() ? "Left hand" : "Right hand";
      log.info("  " + handType + ", id: " + hand.id() + ", palm position: " + hand.palmPosition());

      // Get the hand's normal vector and direction
      Vector normal = hand.palmNormal();
      Vector direction = hand.direction();

      // Calculate the hand's pitch, roll, and yaw angles
      log.info("  pitch: " + Math.toDegrees(direction.pitch()) + " degrees, " + "roll: " + Math.toDegrees(normal.roll()) + " degrees, " + "yaw: "
          + Math.toDegrees(direction.yaw()) + " degrees");

      // Get arm bone
      Arm arm = hand.arm();
      log.info("  Arm direction: " + arm.direction() + ", wrist position: " + arm.wristPosition() + ", elbow position: " + arm.elbowPosition());

      // Get fingers
      for (Finger finger : hand.fingers()) {
        log.info("    " + finger.type() + ", id: " + finger.id() + ", length: " + finger.length() + "mm, width: " + finger.width() + "mm");

        // Get Bones
        for (Bone.Type boneType : Bone.Type.values()) {
          Bone bone = finger.bone(boneType);
          log.info("      " + bone.type() + " bone, start: " + bone.prevJoint() + ", end: " + bone.nextJoint() + ", direction: " + bone.direction());
        }
      }
    }

    // Get tools
    for (Tool tool : frame.tools()) {
      log.info("  Tool id: " + tool.id() + ", position: " + tool.tipPosition() + ", direction: " + tool.direction());
    }

    GestureList gestures = frame.gestures();
    for (int i = 0; i < gestures.count(); i++) {
      Gesture gesture = gestures.get(i);

      switch (gesture.type()) {
        case TYPE_CIRCLE:
          CircleGesture circle = new CircleGesture(gesture);

          // Calculate clock direction using the angle between circle
          // normal and pointable
          String clockwiseness;
          if (circle.pointable().direction().angleTo(circle.normal()) <= Math.PI / 2) {
            // Clockwise if angle is less than 90 degrees
            clockwiseness = "clockwise";
          } else {
            clockwiseness = "counterclockwise";
          }

          // Calculate angle swept since last frame
          double sweptAngle = 0;
          if (circle.state() != State.STATE_START) {
            CircleGesture previousUpdate = new CircleGesture(controller.frame(1).gesture(circle.id()));
            sweptAngle = (circle.progress() - previousUpdate.progress()) * 2 * Math.PI;
          }

          log.info("  Circle id: " + circle.id() + ", " + circle.state() + ", progress: " + circle.progress() + ", radius: " + circle.radius() + ", angle: "
              + Math.toDegrees(sweptAngle) + ", " + clockwiseness);
          break;
        case TYPE_SWIPE:
          SwipeGesture swipe = new SwipeGesture(gesture);
          log.info("  Swipe id: " + swipe.id() + ", " + swipe.state() + ", position: " + swipe.position() + ", direction: " + swipe.direction() + ", speed: " + swipe.speed());
          break;
        case TYPE_SCREEN_TAP:
          ScreenTapGesture screenTap = new ScreenTapGesture(gesture);
          log.info("  Screen Tap id: " + screenTap.id() + ", " + screenTap.state() + ", position: " + screenTap.position() + ", direction: " + screenTap.direction());
          break;
        case TYPE_KEY_TAP:
          KeyTapGesture keyTap = new KeyTapGesture(gesture);
          log.info("  Key Tap id: " + keyTap.id() + ", " + keyTap.state() + ", position: " + keyTap.position() + ", direction: " + keyTap.direction());
          break;
        default:
          log.info("Unknown gesture type.");
          break;
      }
    }

    if (!frame.hands().isEmpty() || !gestures.isEmpty()) {
      log.info("Nothing?");
    }
  }

  @Override
  public void onInit(Controller controller) {
    log.info("Initialized");
  }

  public void strenght(Controller controller) {
    Frame frame = controller.frame();
    Hand hand = frame.hands().rightmost();
    log.info("Strenght is: " + hand.grabStrength());

  }
}

package org.myrobotlab.leap;

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

  @Override
  public void onConnect(Controller controller) {
    System.out.println("Connected");
    controller.enableGesture(Gesture.Type.TYPE_SWIPE);
    controller.enableGesture(Gesture.Type.TYPE_CIRCLE);
    controller.enableGesture(Gesture.Type.TYPE_SCREEN_TAP);
    controller.enableGesture(Gesture.Type.TYPE_KEY_TAP);
  }

  @Override
  public void onDisconnect(Controller controller) {
    // Note: not dispatched when running in a debugger.
    System.out.println("Disconnected");
  }

  @Override
  public void onExit(Controller controller) {
    System.out.println("Exited");
  }

  @Override
  public void onFrame(Controller controller) {
    // Get the most recent frame and report some basic information
    Frame frame = controller.frame();
    System.out.println("Frame id: " + frame.id() + ", timestamp: " + frame.timestamp() + ", hands: " + frame.hands().count() + ", fingers: " + frame.fingers().count() + ", tools: "
        + frame.tools().count() + ", gestures " + frame.gestures().count());

    // Get hands
    for (Hand hand : frame.hands()) {
      String handType = hand.isLeft() ? "Left hand" : "Right hand";
      System.out.println("  " + handType + ", id: " + hand.id() + ", palm position: " + hand.palmPosition());

      // Get the hand's normal vector and direction
      Vector normal = hand.palmNormal();
      Vector direction = hand.direction();

      // Calculate the hand's pitch, roll, and yaw angles
      System.out.println("  pitch: " + Math.toDegrees(direction.pitch()) + " degrees, " + "roll: " + Math.toDegrees(normal.roll()) + " degrees, " + "yaw: "
          + Math.toDegrees(direction.yaw()) + " degrees");

      // Get arm bone
      Arm arm = hand.arm();
      System.out.println("  Arm direction: " + arm.direction() + ", wrist position: " + arm.wristPosition() + ", elbow position: " + arm.elbowPosition());

      // Get fingers
      for (Finger finger : hand.fingers()) {
        System.out.println("    " + finger.type() + ", id: " + finger.id() + ", length: " + finger.length() + "mm, width: " + finger.width() + "mm");

        // Get Bones
        for (Bone.Type boneType : Bone.Type.values()) {
          Bone bone = finger.bone(boneType);
          System.out.println("      " + bone.type() + " bone, start: " + bone.prevJoint() + ", end: " + bone.nextJoint() + ", direction: " + bone.direction());
        }
      }
    }

    // Get tools
    for (Tool tool : frame.tools()) {
      System.out.println("  Tool id: " + tool.id() + ", position: " + tool.tipPosition() + ", direction: " + tool.direction());
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

          System.out.println("  Circle id: " + circle.id() + ", " + circle.state() + ", progress: " + circle.progress() + ", radius: " + circle.radius() + ", angle: "
              + Math.toDegrees(sweptAngle) + ", " + clockwiseness);
          break;
        case TYPE_SWIPE:
          SwipeGesture swipe = new SwipeGesture(gesture);
          System.out
              .println("  Swipe id: " + swipe.id() + ", " + swipe.state() + ", position: " + swipe.position() + ", direction: " + swipe.direction() + ", speed: " + swipe.speed());
          break;
        case TYPE_SCREEN_TAP:
          ScreenTapGesture screenTap = new ScreenTapGesture(gesture);
          System.out.println("  Screen Tap id: " + screenTap.id() + ", " + screenTap.state() + ", position: " + screenTap.position() + ", direction: " + screenTap.direction());
          break;
        case TYPE_KEY_TAP:
          KeyTapGesture keyTap = new KeyTapGesture(gesture);
          System.out.println("  Key Tap id: " + keyTap.id() + ", " + keyTap.state() + ", position: " + keyTap.position() + ", direction: " + keyTap.direction());
          break;
        default:
          System.out.println("Unknown gesture type.");
          break;
      }
    }

    if (!frame.hands().isEmpty() || !gestures.isEmpty()) {
      System.out.println();
    }
  }

  @Override
  public void onInit(Controller controller) {
    System.out.println("Initialized");
  }

  public void strenght(Controller controller) {
    Frame frame = controller.frame();
    Hand hand = frame.hands().rightmost();
    System.out.println("Strenght is: " + hand.grabStrength());

  }
}

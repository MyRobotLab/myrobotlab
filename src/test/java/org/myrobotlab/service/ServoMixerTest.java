package org.myrobotlab.service;

import org.junit.Test;
import org.myrobotlab.kinematics.Action;
import org.myrobotlab.kinematics.Gesture;
import org.myrobotlab.test.AbstractTest;

public class ServoMixerTest extends AbstractTest {

  @Test
  public void testService() throws Exception {
    
    Servo s1 = (Servo)Runtime.start("s1", "Servo");
    Servo s2 = (Servo)Runtime.start("s2", "Servo");
    
    ServoMixer mixer = (ServoMixer)Runtime.start("mixer", "ServoMixer");

    Gesture gesture = new Gesture();
    
    s1.moveTo(5);
    s2.moveTo(5);
    mixer.saveGesture("gesture-1");
    mixer.addNewGestureFile("test-gesture-1");
    
    Action action1 = new Action();
    mixer.addAction(action1, 0);
   
  }

}
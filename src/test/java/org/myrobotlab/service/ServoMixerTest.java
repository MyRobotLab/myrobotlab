package org.myrobotlab.service;

import org.junit.Test;
import org.myrobotlab.kinematics.Gesture;
import org.myrobotlab.kinematics.GesturePart;
import org.myrobotlab.test.AbstractTest;

public class ServoMixerTest extends AbstractTest {

  @Test
  public void testService() throws Exception {
    
    Servo s1 = (Servo)Runtime.start("s1", "Servo");
    Servo s2 = (Servo)Runtime.start("s2", "Servo");
    
    ServoMixer mixer = (ServoMixer)Runtime.start("mixer", "ServoMixer");
    String gestureFileName = "mixerTest-1";
    // mixer.addNewGestureFile(gestureFileName);
    Gesture gesture = new Gesture();
    
    s1.moveTo(5);
    s2.moveTo(5);
    mixer.savePose("pose-1");
    
    GesturePart part1 = new GesturePart();
    
    gesture.getParts().add(null);
   
  }

}
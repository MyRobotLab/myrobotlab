
import org.myrobotlab.service.*;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.PinArrayListener;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

/**
 * A quick little scratch .. TODO: delete this before checking in and add a proper unit test for this.
 * 
 * This starts an arduino / an inmoov hand
 * and attaches the analog pin data from A0 -A4 to flow through the hand.
 * 
 * @author kwatters
 *
 */

@Ignore
public class InMoovHandSensorTest {

  @Test
  public void testHandSensor() throws Exception {
   
    
    Runtime.start("gui", "SwingGui");
    Runtime.start("python", "Python");
    Arduino ard = (Arduino)Runtime.start("ard", "Arduino");
    InMoovHand leftHand = (InMoovHand)Runtime.start("i01.leftHand", "InMoovHand");
    
    
    String port = "COM3";
    // ard.setBoard("mega");
    ard.connect(port);
    
    Thread.sleep(1000);
    // ok.. now what?
    // the arduino is  a pin array publisher... so we want to attach the inmoov hand which is a pin array listener.
    // TODO: in python land, this syntax might be a bit goofy..
    
    
    
   // leftHand.attach(ard);
    
    // at this point.. the sensor data should be flowing to the lefthand ...
    
    
    ard.enablePin("A0");
    ard.enablePin("A1");
//    ard.enablePin("A2");
//    ard.enablePin("A3");
//    ard.enablePin("A4");
//    ard.enablePin("A5");
    
    Thread.sleep(1000);
    
   ard.attach((PinArrayListener)leftHand);
    
    
    leftHand.sensorsEnabled = true;
    
    
    System.out.println("Press the any key to exit.");
    System.in.read();
  }
}



import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;

import java.util.ArrayList;
import java.util.Random;

import org.junit.Test;
import org.junit.Ignore;

// TODO: delete this class.. it's just here to help demonstrate some unstable arduino -> mrlcomm behavior.
// author: kwatters
// notes.  this test when run will connect to an arduino, add some servos... move those servos ,,, and finally disconnect.
// that will run in a loop..   when observing a servo that has power to it, connected to the arduino,
// I only see it move about 50% of the time through the connect/disconnect loop.
// it seems that the wait for board lock isn't working as expected... 
@Ignore
public class ServoPalsyTest {
  
  @Test
  public void testServos() throws Exception {
    
    Random rand = new Random(); 
    
    // Ok. we want to start up 2 arduino services.. and add a bunch of servos.
    Arduino left = (Arduino)Runtime.start("left", "Arduino");
//    Arduino right = (Arduino)Runtime.start("right", "Arduino");
    ArrayList<Servo> servos = addServos(left);

    for (int i = 0; i < 10; i++) {
      left.connect("COM3");
      moveServos(rand, servos);
      left.disconnect();
    }
    
//    right.disconnect();
    System.out.println("Done.");
  }

  private void moveServos(Random rand, ArrayList<Servo> servos) throws InterruptedException {
    for (int i = 0 ; i < 10; i++) {
      for (Servo s : servos) {
        s.moveTo((double)rand.nextInt(180));
      }
      Thread.sleep(100);
    }
  }

  private ArrayList<Servo> addServos(Arduino left) throws Exception {
    ArrayList<Servo> servos = new ArrayList<Servo>();
    for (int i = 2; i<=11; i++) {
      Servo s = (Servo)Runtime.start("s"+i, "Servo");
      s.setPin(i);
     // s.setSpeed(50.0);
      left.attach(s);
      servos.add(s);
    }
    return servos;
  }
}

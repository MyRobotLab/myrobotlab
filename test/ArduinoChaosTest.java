import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.framework.MRLException;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.Motor;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;

@Ignore
public class ArduinoChaosTest {

  @Test
  public void testArduino() throws IOException, MRLException {

    LoggingFactory.getInstance().configure();
    LoggingFactory.getInstance().setLevel(Level.INFO);

    //
    Arduino arduino = (Arduino) Runtime.createAndStart("arduino", "Arduino");
    //Serial serial = (Serial) arduino.getSerial();

    arduino.connect("COM30");

    // digitial PWM pins for hbrdige/motor control.
    int leftPwm = 6;
    int rightPwm = 7;
    // analog feedback pin A0 on the Uno
    int potPin = 14;
    boolean testMotor = true;
    boolean testServo = true;
    boolean startAnalogPolling = true;
    if (startAnalogPolling) {
      arduino.analogReadPollingStart(potPin);
    }
    arduino.setLoadTimingEnabled(false);
    Motor motor = (Motor)Runtime.createAndStart("motor", "Motor");
    motor.setType2Pwm(leftPwm, rightPwm);
    // motor.attach(arduino);
    arduino.motorAttach(motor);


    Servo servo = (Servo)Runtime.createAndStart("servo", "Servo");
    servo.setPin(13);
    arduino.servoAttach(servo);
    servo.attach();

    int angle = 0;
    int max = 5000;

    while (true) {
      // try to overrun?
      // rand between -1 and 1.
      if (testMotor) {
        double rand = (Math.random() - 0.5)*2;
        motor.move(rand);
      }

      if (testServo) {
        angle++;
        servo.moveTo(angle %180);
      }
    }

  }
}

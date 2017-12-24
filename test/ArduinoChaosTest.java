import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.arduino.ArduinoUtils;
import org.myrobotlab.framework.MRLException;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.Motor;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;

import com.pi4j.jni.Serial;

@Ignore
public class ArduinoChaosTest {

  @Test
  public void testArduino() throws IOException, MRLException, InterruptedException {

    boolean upload = false;
    
    LoggingFactory.init(Level.INFO);

    String port = "COM30";
    String board = Arduino.BOARD_TYPE_UNO;
    if (upload) {
      boolean success = ArduinoUtils.uploadSketch(port, board);
      assertTrue(success);
    }
    
    //
    Arduino arduino = (Arduino) Runtime.createAndStart("arduino", "Arduino");
    //Serial serial = (Serial) arduino.getSerial();

    arduino.setBoardUno();
    // arduino.boardType= board;
    arduino.connect("COM30");
   

    // let the board connect
    Thread.sleep(2000);
    
    // digital PWM pins for hbrdige/motor control.
    int leftPwm = 6;
    int rightPwm = 7;
    // analog feedback pin A0 on the Uno
    int potPin = 14;
    boolean testMotor = true;
    boolean testServo = true;
    boolean startAnalogPolling = true;
    
    // Start testing the functions one by one to make sure we're worky!
    
    // debug enable/disable first.
    arduino.setDebug(true);
    Thread.sleep(1000);
    arduino.setDebug(false);
    Thread.sleep(1000);
    // Now iterate all of the possible commands
    
    arduino.digitalWrite(0, 0);
    Thread.sleep(1000);
    arduino.analogWrite(0, 110);
    Thread.sleep(1000);
    
    
    // TODO: is this a digital or analog pin?
    arduino.pinMode(2, "INPUT");
    Thread.sleep(1000);
    arduino.pinMode(2, "OUTPUT");
    Thread.sleep(1000);
    Servo servo = (Servo)Runtime.createAndStart("servo", "Servo");
    // servo.setPin(13);
    // arduino.servoAttach(servo);
    Thread.sleep(1000);
    arduino.servoSweepStart(servo);
    Thread.sleep(1000);
    arduino.servoSweepStop(servo);


    Thread.sleep(1000);
    // TODO : this blows up
    // arduino.servoWrite(servo);
    servo.moveTo(90);
    Thread.sleep(1000);
    int pos = 90;
    // arduino.publishServoEvent(pos);
    Thread.sleep(1000);
    int uS = 1400;
    servo.writeMicroseconds(uS);
    // TODO: why does this blow up
    // arduino.servoWriteMicroseconds(servo);
    Thread.sleep(1000);
    servo.setSpeed(0);
    Thread.sleep(1000);
    arduino.servoDetachPin(servo);
    Thread.sleep(1000);
    arduino.enableBoardInfo(true);
    Thread.sleep(1000);
    arduino.enableBoardInfo(false);
    
    Thread.sleep(1000);
 
    // int analogReadPin = 2;
    // arduino.analogReadPollingStart(analogReadPin);
    //  Thread.sleep(1000);
    //Thread.sleep(1000);
    //arduino.analogReadPollingStop(analogReadPin);

    Thread.sleep(1000);
    int digitalPin = 3;
    arduino.enablePin(digitalPin);
    Thread.sleep(1000);
    arduino.disablePin(digitalPin);
    Thread.sleep(1000);

    int pulsePin = 1;
    // // arduino.pulse(pulsePin);
    Thread.sleep(1000);
    // TODO: what is this?
    // // arduino.pulseStop();
    Thread.sleep(1000);
    arduino.setTrigger(3, 122);
    Thread.sleep(1000);
    // TODO: which pin are we debouncing?
    arduino.setDebounce(5, 10);
    // TODO: what is this for?
    Thread.sleep(1000);
    // arduino.setDigitalTriggerOnly(true);
    Thread.sleep(1000);
    // arduino.setDigitalTriggerOnly(false);
    
    Thread.sleep(1000);
    arduino.setSerialRate(Serial.BAUD_RATE_57600);
    Thread.sleep(1000);
    arduino.setSerialRate(Serial.BAUD_RATE_115200);
    
    Thread.sleep(1000);
    arduino.getBoardInfo();
    
    Thread.sleep(1000);
    // TODO: what does "12" mean?  12 hertz?!
    // arduino.setSampleRate(12);
    
    
    Thread.sleep(1000);
    arduino.softReset();
    
    
    Thread.sleep(1000);
    // ?!
    // arduino.sensorPollingStart("A0", 123);
    Thread.sleep(1000);
    // arduino.sensorPollingStop("A0");
    
    // TODO: add the
    // AF_BEGIN
    // AF_SET_PWM_FREQ
    // AF_SET_PWM
    // AF_SET_SERVO
    // NOP
    // DEBUG_ENABLE
    // DEBUG_DISABLE
    
    // unknown command should return an error!

    
    
    
    
    
    
    
    
    
    
    
    
    
    //    arduino.digitalWrite(0, 1);
//    if (startAnalogPolling) {
//      arduino.analogReadPollingStart(potPin);
//    }
    
    // 5 second pause ?
    
    // Thread.sleep(5000);
    
    System.out.println("Press the any key to continue.");
    System.in.read();
    
    arduino.enableBoardInfo(true);
    
    
    Motor motor = (Motor)Runtime.createAndStart("motor", "Motor");
    // motor.setType2Pwm(leftPwm, rightPwm);
    // motor.attach(arduino);
    // arduino.attachDevice(motor); // null  config is this right ?


//    servo.attach();

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

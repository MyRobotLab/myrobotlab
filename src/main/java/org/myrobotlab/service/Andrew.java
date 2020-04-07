package org.myrobotlab.service;

import java.io.IOException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.openni.OpenNiData;
import org.myrobotlab.openni.Skeleton;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.slf4j.Logger;

// TODO set pir sensor

/**
 * 
 * Andrew - The Andrew robot service. Maintained by \@beetlejuice
 *
 */
public class Andrew extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Andrew.class);

  transient public Arduino arduino;
  transient public Adafruit16CServoDriver adaFruit16cRight;
  transient public Adafruit16CServoDriver adaFruit16cLeft;
  transient public WebkitSpeechRecognition ear;
  transient public WebGui webgui;
  transient public SpeechSynthesis mouth;
  transient public static Tracking tracker;
  transient public ProgramAB chatBot;
  transient public static OpenNi openni;
  transient public Pid pid;
  transient public Pir pir;
  transient public HtmlFilter htmlFilter;
  transient public OpenCV openCV;

  // Right arm Servomotors
  transient public Servo rightShoulderServo;
  transient public Servo rightArmServo;
  transient public Servo rightBicepsServo;
  transient public Servo rightElbowServo;
  transient public Servo rightWristServo;

  // Left arm Servomotors
  transient public Servo leftShoulderServo;
  transient public Servo leftArmServo;
  transient public Servo leftBicepsServo;
  transient public Servo leftElbowServo;
  transient public Servo leftWristServo;

  // Right hand Servomotors
  transient public Servo rightThumbServo;
  transient public Servo rightIndexServo;
  transient public Servo rightMiddleServo;
  transient public Servo rightRingServo;
  transient public Servo rightPinkyServo;

  // Left hand Servomotors
  transient public Servo leftThumbServo;
  transient public Servo leftIndexServo;
  transient public Servo leftMiddleServo;
  transient public Servo leftRingServo;
  transient public Servo leftPinkyServo;

  // Head Servomotors
  transient public Servo neckTiltServo;
  transient public Servo neckPanServo;

  // Ultrasonic sensors

  transient public UltrasonicSensor USfront;
  transient public UltrasonicSensor USfrontRight;
  transient public UltrasonicSensor USfrontLeft;
  transient public UltrasonicSensor USback;
  transient public UltrasonicSensor USbackRight;
  transient public UltrasonicSensor USbackLeft;

  boolean copyGesture = false;
  boolean firstSkeleton = true;
  boolean saveSkeletonFrame = false;

  // Adafruit16CServoDriver setup
  String i2cBus = "0";
  String i2cAdressRight = "0x40";
  String i2cAdressLeft = "0x41";

  // arduino pins variables
  int rightMotorDirPin = 2;
  int rightMotorPwmPin = 3;
  int leftMotorDirPin = 4;
  int leftMotorPwmPin = 5;

  int backUltrasonicTrig = 22;
  int backUltrasonicEcho = 23;
  int back_leftUltrasonicTrig = 24;
  int back_leftUltrasonicEcho = 25;
  int back_rightUltrasonicTrig = 26;
  int back_rightUltrasonicEcho = 27;
  int front_leftUltrasonicTrig = 28;
  int front_leftUltrasonicEcho = 29;
  int frontUltrasonicTrig = 30;
  int frontUltrasonicEcho = 31;
  int front_rightUltrasonicTrig = 32;
  int front_rightUltrasonicEcho = 33;

  int SHIFT = 14;
  int LATCH = 15;
  int DATA = 16;
  int pirSensorPin = 17;

  int pin = 0;
  int min = 1;
  int max = 2;
  int rest = 3;

  class ServoConfig {
    public int pin;
    public double min;
    public double max;
    public double rest;

    public ServoConfig(int pin, double min, double max, double rest) {
      this.pin = pin;
      this.min = min;
      this.max = max;
      this.rest = rest;
    }

    public ServoConfig(double[] data) {
      if (data[0] != -1) {
        pin = (int) data[0];
      }
      if (data[1] != -1) {
        min = data[1];
      }
      if (data[2] != -1) {
        max = data[2];
      }
      if (data[3] != -1) {
        rest = data[3];
      }
    }
  }

  // for arms and hands, the values are pin,min,max,rest

  // Right arm
  ServoConfig rightShoulder = new ServoConfig(34, 0, 180, 0);
  ServoConfig rightArm = new ServoConfig(1, 45, 155, 140);
  ServoConfig rightBiceps = new ServoConfig(2, 12, 90, 12);
  ServoConfig rightElbow = new ServoConfig(3, 8, 90, 8);
  ServoConfig rightWrist = new ServoConfig(4, 0, 140, 140);

  // Left arm
  ServoConfig leftShoulder = new ServoConfig(35, 0, 150, 148);
  ServoConfig leftArm = new ServoConfig(1, 0, 85, 0);
  ServoConfig leftBiceps = new ServoConfig(2, 60, 140, 140);
  ServoConfig leftElbow = new ServoConfig(3, 0, 75, 0);
  ServoConfig leftWrist = new ServoConfig(4, 0, 168, 0);

  // Right hand
  ServoConfig rightThumb = new ServoConfig(5, 170, 75, 170);
  ServoConfig rightIndex = new ServoConfig(6, 70, 180, 180);
  ServoConfig rightMiddle = new ServoConfig(7, 1, 2, 3);
  ServoConfig rightRing = new ServoConfig(8, 15, 130, 15);
  ServoConfig rightPinky = new ServoConfig(9, 25, 180, 25);

  // Left hand
  ServoConfig leftThumb = new ServoConfig(5, 40, 105, 40);
  ServoConfig leftIndex = new ServoConfig(6, 0, 180, 0);
  ServoConfig leftMiddle = new ServoConfig(7, 0, 180, 0);
  ServoConfig leftRing = new ServoConfig(8, 10, 180, 180);
  ServoConfig leftPinky = new ServoConfig(9, 65, 180, 180);

  // Head
  ServoConfig neckTilt = new ServoConfig(6, 15, 50, 30);
  ServoConfig neckPan = new ServoConfig(7, 20, 130, 75);

  /**
   * Replace the values of an array , if a value == -1 the old value is keep
   * Exemple if rightArm[]={35,1,2,3} and user ask to change by {-1,1,2,3}, this
   * method will return {35,1,2,3} This method must receive an array of ten
   * arrays. If one of these arrays is less or more than four numbers length ,
   * it doesn't will be changed.
   */
  int[][] changeArrayValues(int[][] valuesArray) {
    // valuesArray contain first the news values and after, the old values
    for (int i = 0; i < (valuesArray.length / 2); i++) {
      if (valuesArray[i].length == 4) {
        for (int j = 0; j < 3; j++) {
          if (valuesArray[i][j] == -1) {
            valuesArray[i][j] = valuesArray[i + 5][j];
          }
        }
      } else {
        valuesArray[i] = valuesArray[i + (valuesArray.length / 2)];
      }
    }
    return valuesArray;
  }

  /**
   * Set pin, min, max, and rest for each servos. -1 in an array mean "no
   * change" Exemple
   * setRightArm({39,1,2,3},{40,1,2,3},{41,1,2,3},{-1,1,2,3},{-1,1,2,3}) Python
   * exemple :
   * Andrew.setRightArm([1,0,180,90],[2,0,180,0],[3,180,90,90],[7,7,4,4],[8,5,8,1])
   */
  public void setRightArm(double[] shoulder, double[] arm, double[] biceps, double[] elbow, double[] wrist) {
    rightShoulder = new ServoConfig(shoulder);
    rightArm = new ServoConfig(arm);
    rightBiceps = new ServoConfig(biceps);
    rightElbow = new ServoConfig(elbow);
    rightWrist = new ServoConfig(wrist);
  }

  /**
   * Same as setRightArm
   */
  public void setLefttArm(double[] shoulder, double[] arm, double[] biceps, double[] elbow, double[] wrist) {
    leftShoulder = new ServoConfig(shoulder);
    leftArm = new ServoConfig(arm);
    leftBiceps = new ServoConfig(biceps);
    leftElbow = new ServoConfig(elbow);
    leftWrist = new ServoConfig(wrist);
  }

  /**
   * Same as setRightArm
   */
  public void setLeftHand(double[] thumb, double[] index, double[] middle, double[] ring, double[] pinky) {
    leftThumb = new ServoConfig(thumb);
    leftIndex = new ServoConfig(index);
    leftMiddle = new ServoConfig(middle);
    leftRing = new ServoConfig(ring);
    leftPinky = new ServoConfig(pinky);
  }

  /**
   * Same as setRightArm
   */
  public void setRightHand(double[] thumb, double[] index, double[] middle, double[] ring, double[] pinky) {
    rightThumb = new ServoConfig(thumb);
    rightIndex = new ServoConfig(index);
    rightMiddle = new ServoConfig(middle);
    rightRing = new ServoConfig(ring);
    rightPinky = new ServoConfig(pinky);
  }

  /**
   * Set pin, min, max, and rest for head tilt and pan . -1 in an array mean "no
   * change" Exemple setHead({39,1,2,3},{40,1,2,3}) Python exemple :
   * Andrew.setHead([1,0,180,90],[2,0,180,0])
   */
  public void setHead(double[] tilt, double[] pan) {
    neckTilt = new ServoConfig(tilt);
    neckPan =  new ServoConfig(pan);
  }

  // set Adafruit16CServoDriver setup
  public void setadafruitServoDriver(String i2cBusValue, String i2cAdressRightValue, String i2cAdressLeftValue) {
    i2cBus = i2cBusValue;
    i2cAdressRight = i2cAdressRightValue;
    i2cAdressLeft = i2cAdressLeftValue;

  }

  // variables for speak / mouth sync
  public int delaytime = 3;
  public int delaytimestop = 5;
  public int delaytimeletter = 1;

  String lang;

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      Runtime.start("andrew", "Andrew");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public Andrew(String n, String id) {
    super(n, id);
  }

  /**
   * Attach the servos to arduino and adafruitServoDriver pins
   * 
   * @throws Exception
   *           e
   */
  public void attach() throws Exception {
    adaFruit16cLeft.attach("arduino", i2cBus, i2cAdressLeft);
    adaFruit16cRight.attach("arduino", i2cBus, i2cAdressRight);
    rightElbowServo.attach(adaFruit16cRight, rightElbow.pin);
    rightShoulderServo.attach(adaFruit16cRight, rightShoulder.pin);
    rightArmServo.attach(adaFruit16cRight, rightArm.pin);
    rightBicepsServo.attach(adaFruit16cRight, rightBiceps.pin);
    rightElbowServo.attach(adaFruit16cRight, rightElbow.pin);
    rightWristServo.attach(adaFruit16cRight, rightWrist.pin);
    leftShoulderServo.attach(adaFruit16cLeft, leftShoulder.pin);
    leftArmServo.attach(adaFruit16cLeft, leftArm.pin);
    leftBicepsServo.attach(adaFruit16cLeft, leftBiceps.pin);
    leftElbowServo.attach(adaFruit16cLeft, leftElbow.pin);
    leftWristServo.attach(adaFruit16cLeft, leftWrist.pin);
    rightThumbServo.attach(adaFruit16cRight, rightThumb.pin);
    rightIndexServo.attach(adaFruit16cRight, rightIndex.pin);
    rightMiddleServo.attach(adaFruit16cRight, rightMiddle.pin);
    rightRingServo.attach(adaFruit16cRight, rightRing.pin);
    rightPinkyServo.attach(adaFruit16cRight, rightPinky.pin);
    leftThumbServo.attach(adaFruit16cLeft, leftThumb.pin);
    leftIndexServo.attach(adaFruit16cLeft, leftIndex.pin);
    leftMiddleServo.attach(adaFruit16cLeft, leftMiddle.pin);
    leftRingServo.attach(adaFruit16cLeft, leftRing.pin);
    leftPinkyServo.attach(adaFruit16cLeft, leftPinky.pin);
    neckTiltServo.attach(arduino, neckTilt.pin);
    neckPanServo.attach(arduino, neckPan.pin);

    // Inverted servos
    neckTiltServo.setInverted(true);
  }

  /**
   * Connect the arduino to a COM port . Exemple : connect("COM8")
   * 
   * @param port
   *          port
   * @throws IOException
   *           e
   */
  public void connect(String port) throws IOException {
    arduino.connect(port);
    sleep(2000);
    arduino.pinMode(SHIFT, Arduino.OUTPUT);
    arduino.pinMode(LATCH, Arduino.OUTPUT);
    arduino.pinMode(DATA, Arduino.OUTPUT);
    arduino.pinMode(pirSensorPin, Arduino.INPUT);
  }

  /**
   * detach the servos from arduino pins
   */
  public void detach() {

    if (rightElbowServo != null)
      rightElbowServo.detach();
    if (rightShoulderServo != null)
      rightShoulderServo.detach();
    if (rightArmServo != null)
      rightArmServo.detach();
    if (rightBicepsServo != null)
      rightBicepsServo.detach();
    if (rightElbowServo != null)
      rightElbowServo.detach();
    if (rightWristServo != null)
      rightWristServo.detach();
    if (leftShoulderServo != null)
      leftShoulderServo.detach();
    if (leftShoulderServo != null)
      leftShoulderServo.detach();
    if (leftBicepsServo != null)
      leftBicepsServo.detach();
    if (leftElbowServo != null)
      leftElbowServo.detach();
    if (leftWristServo != null)
      leftWristServo.detach();
    if (rightThumbServo != null)
      rightThumbServo.detach();
    if (rightIndexServo != null)
      rightIndexServo.detach();
    if (rightMiddleServo != null)
      rightMiddleServo.detach();
    if (rightRingServo != null)
      rightRingServo.detach();
    if (rightPinkyServo != null)
      rightPinkyServo.detach();
    if (leftThumbServo != null)
      leftThumbServo.detach();
    if (leftIndexServo != null)
      leftIndexServo.detach();
    if (leftMiddleServo != null)
      leftMiddleServo.detach();
    if (leftRingServo != null)
      leftRingServo.detach();
    if (leftPinkyServo != null)
      leftPinkyServo.detach();
    if (neckTiltServo != null)
      neckTiltServo.detach();
    if (neckPanServo != null)
      neckPanServo.detach();

  }

  /**
   * Move the head . Use : head(neckTiltAngle, neckPanAngle -1 mean "no change"
   * 
   * @param neckTiltAngle
   *          tilt
   * @param neckPanAngle
   *          pan
   */
  public void setHeadPosition(double neckTiltAngle, double neckPanAngle) {

    if (neckTiltAngle == -1) {
      neckTiltAngle = neckTiltServo.getPos();
    }
    if (neckPanAngle == -1) {
      neckPanAngle = neckPanServo.getPos();
    }

    neckTiltServo.moveTo(neckTiltAngle);
    neckPanServo.moveTo(neckPanAngle);
  }

  /**
   * Move the right arm . Use : setRightArm(shoulder angle, arm angle, biceps
   * angle, Elbow angle, wrist angle) -1 mean "no change"
   * 
   * @param shoulderAngle
   *          s
   * @param armAngle
   *          a
   * @param bicepsAngle
   *          b
   * @param ElbowAngle
   *          f
   * @param wristAngle
   *          w
   */
  public void setRightArmPosition(double shoulderAngle, double armAngle, double bicepsAngle, double ElbowAngle, double wristAngle) {

    // TODO protect against self collision
    if (shoulderAngle == -1) {
      shoulderAngle = rightShoulderServo.getPos();
    }
    if (armAngle == -1) {
      armAngle = rightArmServo.getPos();
    }
    if (bicepsAngle == -1) {
      armAngle = rightBicepsServo.getPos();
    }
    if (ElbowAngle == -1) {
      ElbowAngle = rightElbowServo.getPos();
    }
    if (wristAngle == -1) {
      wristAngle = rightWristServo.getPos();
    }

    rightShoulderServo.moveTo(shoulderAngle);
    rightArmServo.moveTo(armAngle);
    rightBicepsServo.moveTo(bicepsAngle);
    rightElbowServo.moveTo(ElbowAngle);
    rightWristServo.moveTo(wristAngle);
  }

  /*
   * Move the left arm . Use : setLeftArm(shoulder angle, arm angle, biceps
   * angle, Elbow angle, Elbow angle,wrist angle) -1 mean "no change"
   * 
   * @param shoulderAngle s
   * 
   * @param armAngle a
   * 
   * @param bicepsAngle b
   * 
   * @param ElbowAngle f
   * 
   * @param wristAngle w
   */
  public void setLeftArmPosition(double shoulderAngle, double armAngle, double bicepsAngle, double ElbowAngle, double wristAngle) {
    // TODO protect against self collision with -> servoName.getPos()
    if (shoulderAngle == -1) {
      shoulderAngle = leftShoulderServo.getPos();
    }
    if (armAngle == -1) {
      armAngle = leftArmServo.getPos();
    }
    if (bicepsAngle == -1) {
      armAngle = leftBicepsServo.getPos();
    }
    if (ElbowAngle == -1) {
      ElbowAngle = leftElbowServo.getPos();
    }
    if (wristAngle == -1) {
      wristAngle = leftWristServo.getPos();
    }

    leftShoulderServo.moveTo(shoulderAngle);
    leftArmServo.moveTo(armAngle);
    leftBicepsServo.moveTo(bicepsAngle);
    leftElbowServo.moveTo(ElbowAngle);
    leftWristServo.moveTo(wristAngle);
  }

  /*
   * Move the left hand . Use : setLeftHand(thumb angle, index angle, middle
   * angle, ring angle, pinky angle) -1 mean "no change"
   */
  public void setLeftHandPosition(double thumbAngle, double indexAngle, double middleAngle, double ringAngle, double pinkyAngle) {
    if (thumbAngle == -1) {
      thumbAngle = leftThumbServo.getPos();
    }
    if (indexAngle == -1) {
      indexAngle = leftIndexServo.getPos();
    }
    if (middleAngle == -1) {
      middleAngle = leftMiddleServo.getPos();
    }
    if (ringAngle == -1) {
      ringAngle = leftRingServo.getPos();
    }
    if (pinkyAngle == -1) {
      pinkyAngle = leftPinkyServo.getPos();
    }

    leftThumbServo.moveTo(thumbAngle);
    leftIndexServo.moveTo(indexAngle);
    leftMiddleServo.moveTo(middleAngle);
    leftRingServo.moveTo(ringAngle);
    leftPinkyServo.moveTo(pinkyAngle);
  }

  /*
   * Move the right hand . Use : setrightHand(thumb angle, index angle, middle
   * angle, ring angle, pinky angle) -1 mean "no change"
   */
  public void setRightHandPosition(double thumbAngle, double indexAngle, double middleAngle, double ringAngle, double pinkyAngle) {
    if (thumbAngle == -1) {
      thumbAngle = rightThumbServo.getPos();
    }
    if (indexAngle == -1) {
      indexAngle = rightIndexServo.getPos();
    }
    if (middleAngle == -1) {
      middleAngle = rightMiddleServo.getPos();
    }
    if (ringAngle == -1) {
      ringAngle = rightRingServo.getPos();
    }
    if (pinkyAngle == -1) {
      pinkyAngle = rightPinkyServo.getPos();
    }

    rightThumbServo.moveTo(thumbAngle);
    rightIndexServo.moveTo(indexAngle);
    rightMiddleServo.moveTo(middleAngle);
    rightRingServo.moveTo(ringAngle);
    rightPinkyServo.moveTo(pinkyAngle);
  }

  /*
   * Set the mouth attitude . choose : smile, notHappy, speechLess, empty.
   */
  public void mouthState(String value) {
    if (value == "smile") {
      myShiftOut("11011100");
    } else if (value == "notHappy") {
      myShiftOut("00111110");
    } else if (value == "speechLess") {
      myShiftOut("10111100");
    } else if (value == "empty") {
      myShiftOut("00000000");
    }

  }

  /*
   * drive the motors . Speed &gt; 0 go forward . Speed &lt; 0 go backward .
   * Direction &gt; 0 go right . Direction &lt; 0 go left
   */
  public void moveMotors(int speed, int direction) {
    int speedMin = 50; // min PWM needed for the motors
    boolean isMoving = false;
    int rightCurrentSpeed = 0;
    int leftCurrentSpeed = 0;

    if (speed < 0) { // Go backward
      arduino.analogWrite(rightMotorDirPin, 0);
      arduino.analogWrite(leftMotorDirPin, 0);
      speed = speed * -1;
    } else {// Go forward
      arduino.analogWrite(rightMotorDirPin, 255);
      arduino.analogWrite(leftMotorDirPin, 255);
    }

    if (direction > speedMin && speed > speedMin) {// move and turn to the
      // right
      if (isMoving) {
        arduino.analogWrite(rightMotorPwmPin, direction);
        arduino.analogWrite(leftMotorPwmPin, speed);
      } else {
        rightCurrentSpeed = speedMin;
        leftCurrentSpeed = speedMin;
        while (rightCurrentSpeed < speed && leftCurrentSpeed < direction) {
          if (rightCurrentSpeed < direction) {
            rightCurrentSpeed++;
          }
          if (leftCurrentSpeed < speed) {
            leftCurrentSpeed++;
          }
          arduino.analogWrite(rightMotorPwmPin, rightCurrentSpeed);
          arduino.analogWrite(leftMotorPwmPin, leftCurrentSpeed);
          sleep(20);
        }
        isMoving = true;
      }
    } else if (direction < (speedMin * -1) && speed > speedMin) {// move and
      // turn
      // to
      // the
      // left
      direction *= -1;
      if (isMoving) {
        arduino.analogWrite(leftMotorPwmPin, direction);
        arduino.analogWrite(rightMotorPwmPin, speed);
      } else {
        rightCurrentSpeed = speedMin;
        leftCurrentSpeed = speedMin;
        while (rightCurrentSpeed < speed && leftCurrentSpeed < direction) {
          if (rightCurrentSpeed < speed) {
            rightCurrentSpeed++;
          }
          if (leftCurrentSpeed < direction) {
            leftCurrentSpeed++;
          }
          arduino.analogWrite(rightMotorPwmPin, rightCurrentSpeed);
          arduino.analogWrite(leftMotorPwmPin, leftCurrentSpeed);
          sleep(20);
        }
        isMoving = true;
      }
    } else if (speed > speedMin) { // Go strait
      if (isMoving) {
        arduino.analogWrite(leftMotorPwmPin, speed);
        arduino.analogWrite(rightMotorPwmPin, speed);
      } else {
        int CurrentSpeed = speedMin;
        while (CurrentSpeed < speed) {
          CurrentSpeed++;
          arduino.analogWrite(rightMotorPwmPin, CurrentSpeed);
          arduino.analogWrite(leftMotorPwmPin, CurrentSpeed);
          sleep(20);
        }
        isMoving = true;
      }
    } else if (speed < speedMin && direction < speedMin * -1) {// turn left
      arduino.analogWrite(rightMotorDirPin, 255);
      arduino.analogWrite(leftMotorDirPin, 0);
      arduino.analogWrite(leftMotorPwmPin, speedMin);
      arduino.analogWrite(rightMotorPwmPin, speedMin);

    }

    else if (speed < speedMin && direction > speedMin) {// turn right
      arduino.analogWrite(rightMotorDirPin, 0);
      arduino.analogWrite(leftMotorDirPin, 255);
      arduino.analogWrite(leftMotorPwmPin, speedMin);
      arduino.analogWrite(rightMotorPwmPin, speedMin);
    } else {// stop
      arduino.analogWrite(leftMotorPwmPin, 0);
      arduino.analogWrite(rightMotorPwmPin, 0);
      isMoving = false;
    }

  }

  /**
   * Used to manage a shift register
   */
  private void myShiftOut(String value) {
    arduino.digitalWrite(LATCH, 0); // Stop the copy
    for (int i = 0; i < 8; i++) { // Store the data
      if (value.charAt(i) == '1') {
        arduino.digitalWrite(DATA, 1);
      } else {
        arduino.digitalWrite(DATA, 0);
      }
      arduino.digitalWrite(SHIFT, 1);
      arduino.digitalWrite(SHIFT, 0);
    }
    arduino.digitalWrite(LATCH, 1); // copy

  }

  /**
   * Move the servos to show asked posture
   * 
   * @param pos
   *          pos
   */
  public void posture(String pos) {
    if (pos == "rest") {
      setLeftArmPosition(leftShoulder.rest, leftArm.rest, leftBiceps.rest, leftElbow.rest, leftWrist.rest);
      setRightArmPosition(rightShoulder.rest, rightArm.rest, rightBiceps.rest, rightElbow.rest, rightWrist.rest);
      setLeftHandPosition(leftThumb.rest, leftIndex.rest, leftMiddle.rest, leftRing.rest, leftPinky.rest);
      setRightHandPosition(rightThumb.rest, rightIndex.rest, rightMiddle.rest, rightRing.rest, rightPinky.rest);
      setHeadPosition(neckTilt.rest, neckPan.rest);
    }
    /*
     * Template else if (pos == ""){ setLeftArmPosition(, , , 85, 150);
     * setRightArmPosition(, , , 116, 10); setHeadPosition(75, 127, 75); }
     */
    // TODO correct angles for posture
    else if (pos == "yes") {
      setLeftArmPosition(0, 95, 136, 85, 150);
      setRightArmPosition(155, 55, 5, 116, 10);
      setLeftHandPosition(-1, -1, -1, -1, -1);
      setRightHandPosition(-1, -1, -1, -1, -1);
      setHeadPosition(75, 85);
    } else if (pos == "concenter") {
      setLeftArmPosition(37, 116, 85, 85, 150);
      setRightArmPosition(109, 43, 54, 116, 10);
      setLeftHandPosition(-1, -1, -1, -1, -1);
      setRightHandPosition(-1, -1, -1, -1, -1);
      setHeadPosition(75, 85);
    } else if (pos == "showLeft") {
      setLeftArmPosition(68, 63, 160, 85, 150);
      setRightArmPosition(2, 76, 40, 116, 10);
      setLeftHandPosition(-1, -1, -1, -1, -1);
      setRightHandPosition(-1, -1, -1, -1, -1);
      setHeadPosition(75, 85);
    } else if (pos == "showRight") {
      setLeftArmPosition(145, 79, 93, 85, 150);
      setRightArmPosition(80, 110, 5, 116, 10);
      setLeftHandPosition(-1, -1, -1, -1, -1);
      setRightHandPosition(-1, -1, -1, -1, -1);
      setHeadPosition(75, 85);
    } else if (pos == "handsUp") {
      setLeftArmPosition(0, 79, 93, 85, 150);
      setRightArmPosition(155, 76, 40, 116, 10);
      setLeftHandPosition(-1, -1, -1, -1, -1);
      setRightHandPosition(-1, -1, -1, -1, -1);
      setHeadPosition(75, 85);
    } else if (pos == "carryBags") {
      setLeftArmPosition(145, 79, 93, 85, 150);
      setRightArmPosition(2, 76, 40, 116, 10);
      setLeftHandPosition(-1, -1, -1, -1, -1);
      setRightHandPosition(-1, -1, -1, -1, -1);
      setHeadPosition(75, 85);
    }

  }

  @Override
  public Andrew publishState() {
    super.publishState();
    if (arduino != null)
      arduino.publishState();
    if (rightShoulderServo != null)
      rightShoulderServo.publishState();
    if (rightArmServo != null)
      rightArmServo.publishState();
    if (rightBicepsServo != null)
      rightBicepsServo.publishState();
    if (rightElbowServo != null)
      rightElbowServo.publishState();
    if (rightWristServo != null)
      rightWristServo.publishState();
    if (leftShoulderServo != null)
      leftShoulderServo.publishState();
    if (leftArmServo != null)
      leftArmServo.publishState();
    if (leftElbowServo != null)
      leftElbowServo.publishState();
    if (leftBicepsServo != null)
      leftBicepsServo.publishState();
    if (leftWristServo != null)
      leftWristServo.publishState();
    if (rightThumbServo != null)
      rightThumbServo.publishState();
    if (rightIndexServo != null)
      rightIndexServo.publishState();
    if (rightMiddleServo != null)
      rightMiddleServo.publishState();
    if (rightRingServo != null)
      rightRingServo.publishState();
    if (rightPinkyServo != null)
      rightPinkyServo.publishState();
    if (leftThumbServo != null)
      leftThumbServo.publishState();
    if (leftIndexServo != null)
      leftIndexServo.publishState();
    if (leftMiddleServo != null)
      leftMiddleServo.publishState();
    if (leftRingServo != null)
      leftRingServo.publishState();
    if (leftPinkyServo != null)
      leftPinkyServo.publishState();
    if (neckTiltServo != null)
      neckTiltServo.publishState();
    if (neckPanServo != null)
      neckPanServo.publishState();

    return this;
  }

  /**
   * Say text and move mouth leds
   * 
   * @param text
   *          text being said
   */
  public synchronized void saying(String text) { // Adapt mouth leds to words
    log.info("Saying :" + text);
    try {
      mouth.speak(text);
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public synchronized void onStartSpeaking(String text) {
    sleep(15);
    boolean ison = false;
    String testword;
    String[] a = text.split(" ");
    for (int w = 0; w < a.length; w++) {
      testword = a[w];
      char[] c = testword.toCharArray();

      for (int x = 0; x < c.length; x++) {
        char s = c[x];

        if ((s == 'a' || s == 'e' || s == 'i' || s == 'o' || s == 'u' || s == 'y') && !ison) {

          myShiftOut("00011100");
          ison = true;
          sleep(delaytime);
          myShiftOut("00000100");
        } else if (s == '.') {
          ison = false;
          myShiftOut("00000000");
          sleep(delaytimestop);
        } else {
          ison = false;
          sleep(delaytimeletter); //
        }

      }

    }
  }

  public synchronized void onEndSpeaking(String utterance) {
    myShiftOut("00000000");
  }

  public void setdelays(Integer d1, Integer d2, Integer d3) {

    delaytime = d1;
    delaytimestop = d2;
    delaytimeletter = d3;
  }

  public void setLanguage(String lang) {
    this.lang = lang;
  }

  public void setVoice(String voice) {
    mouth.setVoice(voice);
  }

  @Override
  public void startService() {
    super.startService();

    arduino = (Arduino) Runtime.start("arduino", "Arduino");
    adaFruit16cLeft = (Adafruit16CServoDriver) Runtime.start("I2cServoControlLeft", "Adafruit16CServoDriver");
    adaFruit16cRight = (Adafruit16CServoDriver) Runtime.start("I2cServoControlRight", "Adafruit16CServoDriver");
    chatBot = (ProgramAB) Runtime.start("chatBot", "ProgramAB");
    htmlFilter = (HtmlFilter) Runtime.start("htmlFilter", "HtmlFilter");
    mouth = (SpeechSynthesis) Runtime.start("mouth", "MarySpeech");
    ear = (WebkitSpeechRecognition) Runtime.start("ear", "WebkitSpeechRecognition");
    webgui = (WebGui) Runtime.start("webgui", "WebGui");
    pir = (Pir) Runtime.start("pir", "Pir");

    // configure services
    pir.attach(arduino, pirSensorPin);

    // FIXME - there is likely an "attach" that does this...
    subscribe(mouth.getName(), "publishStartSpeaking");
    subscribe(mouth.getName(), "publishEndSpeaking");

  }

  public void startServos() {

    rightShoulderServo = (Servo) Runtime.start("rightShoulderServo", "Servo");
    rightArmServo = (Servo) Runtime.start("rightArmServo", "Servo");
    rightBicepsServo = (Servo) Runtime.start("rightBicepsServo", "Servo");
    rightElbowServo = (Servo) Runtime.start("rightElbowServo", "Servo");
    rightWristServo = (Servo) Runtime.start("rightWristServo", "Servo");
    leftShoulderServo = (Servo) Runtime.start("leftShoulderServo", "Servo");
    leftArmServo = (Servo) Runtime.start("leftArmServo", "Servo");
    leftBicepsServo = (Servo) Runtime.start("leftBicepsServo", "Servo");
    leftElbowServo = (Servo) Runtime.start("leftElbowServo", "Servo");
    leftWristServo = (Servo) Runtime.start("leftWristServo", "Servo");
    rightThumbServo = (Servo) Runtime.start("rightThumbServo", "Servo");
    rightIndexServo = (Servo) Runtime.start("rightIndexServo", "Servo");
    rightMiddleServo = (Servo) Runtime.start("rightMiddleServo", "Servo");
    rightRingServo = (Servo) Runtime.start("rightRingServo", "Servo");
    rightPinkyServo = (Servo) Runtime.start("rightPinkyServo", "Servo");
    leftThumbServo = (Servo) Runtime.start("leftThumbServo", "Servo");
    leftIndexServo = (Servo) Runtime.start("leftIndexServo", "Servo");
    leftMiddleServo = (Servo) Runtime.start("leftMiddleServo", "Servo");
    leftRingServo = (Servo) Runtime.start("leftRingServo", "Servo");
    leftPinkyServo = (Servo) Runtime.start("leftPinkyServo", "Servo");
    neckTiltServo = (Servo) Runtime.start("neckTiltServo", "Servo");
    neckPanServo = (Servo) Runtime.start("neckPanServo", "Servo");

    // Set min and max angle for each servos
    rightShoulderServo.setMinMax(rightShoulder.min, rightShoulder.max);
    rightArmServo.setMinMax(rightArm.min, rightArm.max);
    rightBicepsServo.setMinMax(rightBiceps.min, rightBiceps.max);
    rightElbowServo.setMinMax(rightElbow.min, rightElbow.max);
    rightWristServo.setMinMax(rightWrist.min, rightWrist.max);
    leftShoulderServo.setMinMax(leftShoulder.min, leftShoulder.max);
    leftArmServo.setMinMax(leftArm.min, leftArm.max);
    leftBicepsServo.setMinMax(leftBiceps.min, leftBiceps.max);
    leftElbowServo.setMinMax(leftElbow.min, leftElbow.max);
    leftWristServo.setMinMax(leftWrist.min, leftWrist.max);
    rightThumbServo.setMinMax(rightThumb.min, rightThumb.max);
    rightIndexServo.setMinMax(rightIndex.min, rightIndex.max);
    rightMiddleServo.setMinMax(rightMiddle.min, rightMiddle.max);
    rightRingServo.setMinMax(rightRing.min, rightRing.max);
    rightPinkyServo.setMinMax(rightPinky.min, rightPinky.max);
    leftThumbServo.setMinMax(leftThumb.min, leftThumb.max);
    leftIndexServo.setMinMax(leftIndex.min, leftIndex.max);
    leftMiddleServo.setMinMax(leftMiddle.min, leftMiddle.max);
    leftRingServo.setMinMax(leftRing.min, leftRing.max);
    leftPinkyServo.setMinMax(leftPinky.min, leftPinky.max);
    neckTiltServo.setMinMax(neckTilt.min, neckTilt.max);
    neckPanServo.setMinMax(neckPan.min, neckPan.max);

    // Set rest for each servos
    rightShoulderServo.setRest(rightShoulder.rest);
    rightArmServo.setRest(rightArm.rest);
    rightBicepsServo.setRest(rightBiceps.rest);
    rightElbowServo.setRest(rightElbow.rest);
    rightWristServo.setRest(rightWrist.rest);
    leftShoulderServo.setRest(leftShoulder.rest);
    leftArmServo.setRest(leftArm.rest);
    leftBicepsServo.setRest(leftBiceps.rest);
    leftElbowServo.setRest(leftElbow.rest);
    leftWristServo.setRest(leftWrist.rest);
    rightThumbServo.setRest(rightThumb.rest);
    rightIndexServo.setRest(rightIndex.rest);
    rightMiddleServo.setRest(rightMiddle.rest);
    rightRingServo.setRest(rightRing.rest);
    rightPinkyServo.setRest(rightPinky.rest);
    leftThumbServo.setRest(leftThumb.rest);
    leftIndexServo.setRest(leftIndex.rest);
    leftMiddleServo.setRest(leftMiddle.rest);
    leftRingServo.setRest(leftRing.rest);
    leftPinkyServo.setRest(leftPinky.rest);
    neckTiltServo.setRest(neckTilt.rest);
    neckPanServo.setRest(neckPan.rest);

    setSpeed(75);
  }

  void setSpeed(double value) {
    rightShoulderServo.setSpeed(value);
    rightArmServo.setSpeed(value);
    rightBicepsServo.setSpeed(value);
    rightElbowServo.setSpeed(value);
    rightWristServo.setSpeed(value);
    leftShoulderServo.setSpeed(value);
    leftArmServo.setSpeed(value);
    leftBicepsServo.setSpeed(value);
    leftElbowServo.setSpeed(value);
    leftWristServo.setSpeed(value);
    rightThumbServo.setSpeed(value);
    rightIndexServo.setSpeed(value);
    rightMiddleServo.setSpeed(value);
    rightRingServo.setSpeed(value);
    rightPinkyServo.setSpeed(value);
    leftThumbServo.setSpeed(value);
    leftIndexServo.setSpeed(value);
    leftMiddleServo.setSpeed(value);
    leftRingServo.setSpeed(value);
    leftPinkyServo.setSpeed(value);
    neckTiltServo.setSpeed(value);
    neckPanServo.setSpeed(value);

  }

  /**
   * Start the tracking services
   */
  public void startTrack() throws Exception {
    tracker = (Tracking) Runtime.start("tracker", "Tracking");
    openCV = (OpenCV) Runtime.start("openCv", "OpenCV");
    sleep(1000);
    tracker.connect(openCV, neckPanServo, neckTiltServo);
    // tracker.pid.invert("y");
    // tracker.clearPreFilters();

  }

  /**
   * Start the ultrasonic sensors services
   * 
   * @param port
   *          port
   * @throws Exception
   *           e
   */

  public void startUltraSonic(String port) throws Exception {
    USfront = (UltrasonicSensor) Runtime.start("USfront", "UltrasonicSensor");
    USfrontRight = (UltrasonicSensor) Runtime.start("USfrontRight", "UltrasonicSensor");
    USfrontLeft = (UltrasonicSensor) Runtime.start("USfrontLeft", "UltrasonicSensor");
    USback = (UltrasonicSensor) Runtime.start("USback", "UltrasonicSensor");
    USbackRight = (UltrasonicSensor) Runtime.start("USbackRight", "UltrasonicSensor");
    USbackLeft = (UltrasonicSensor) Runtime.start("USbackLeft", "UltrasonicSensor");

    USfront.attach(arduino, frontUltrasonicTrig, frontUltrasonicEcho);
    USfrontRight.attach(arduino, front_rightUltrasonicTrig, front_rightUltrasonicEcho);
    USfrontLeft.attach(arduino, front_leftUltrasonicTrig, front_leftUltrasonicEcho);
    USback.attach(arduino, backUltrasonicTrig, backUltrasonicEcho);
    USbackRight.attach(arduino, back_rightUltrasonicTrig, back_rightUltrasonicEcho);
    USbackLeft.attach(arduino, back_leftUltrasonicTrig, back_leftUltrasonicEcho);
  }

  /**
   * Stop the tracking services
   * 
   * @throws Exception
   *           e
   */
  public void stopTrack() throws Exception {
    tracker.opencv.stopCapture();
    tracker.releaseService();
  }

  public OpenNi startOpenNI() throws Exception {
    // TODO modify this function to fit new Andrew
    /*
     * Start the Kinect service
     */
    if (openni == null) {
      System.out.println("starting kinect");
      openni = (OpenNi) Runtime.start("openni", "OpenNi");
      pid = (Pid) Runtime.start("pid", "Pid");

      pid.setMode("kinect", Pid.MODE_AUTOMATIC);
      pid.setOutputRange("kinect", -1, 1);
      pid.setPID("kinect", 10.0, 0.0, 1.0);
      pid.setControllerDirection("kinect", 0);

      // re-mapping of skeleton !
      // openni.skeleton.leftElbow.mapXY(0, 180, 180, 0);
      openni.skeleton.rightElbow.mapXY(0, 180, 180, 0);

      // openni.skeleton.leftShoulder.mapYZ(0, 180, 180, 0);
      openni.skeleton.rightShoulder.mapYZ(0, 180, 180, 0);

      openni.skeleton.leftShoulder.mapXY(0, 180, 180, 0);
      // openni.skeleton.rightShoulder.mapXY(0, 180, 180, 0);

      openni.addListener("publishOpenNIData", this.getName(), "onOpenNIData");
      // openni.addOpenNIData(this);
    }
    return openni;
  }

  public boolean copyGesture(boolean b) throws Exception {
    log.info("copyGesture {}", b);
    if (b) {
      if (openni == null) {
        openni = startOpenNI();
      }
      System.out.println("copying gestures");
      openni.startUserTracking();
    } else {
      System.out.println("stop copying gestures");
      if (openni != null) {
        openni.stopCapture();
        firstSkeleton = true;
      }
    }

    copyGesture = b;
    return b;
  }

  public String captureGesture() {
    return captureGesture(null);
  }

  public String captureGesture(String gestureName) {
    StringBuffer script = new StringBuffer();

    String indentSpace = "";

    if (gestureName != null) {
      indentSpace = "  ";
      script.append(String.format("def %s():\n", gestureName));
    }

    script.append(indentSpace);
    script.append(String.format("Andrew.setRightArmPosition(%f,%f,%f,%f,%f)\n", rightShoulderServo.getPos(), rightArmServo.getPos(), rightBicepsServo.getPos(),
        rightElbowServo.getPos(), rightWristServo.getPos()));
    script.append(indentSpace);
    script.append(String.format("Andrew.setLeftArmPosition(%f,%f,%f,%f,%f)\n", leftShoulderServo.getPos(), leftArmServo.getPos(), leftBicepsServo.getPos(), leftElbowServo.getPos(),
        leftWristServo.getPos()));
    script.append(indentSpace);
    script.append(String.format("Andrew.setHeadPosition(%f,%f)\n", neckTiltServo.getPos(), neckPanServo.getPos()));

    send("python", "appendScript", script.toString());

    return script.toString();
  }

  public void onOpenNIData(OpenNiData data) {

    Skeleton skeleton = data.skeleton;

    if (firstSkeleton) {
      System.out.println("i see you");
      firstSkeleton = false;
    }
    // TODO adapt for new design

    double LElbow = Math.round(skeleton.leftElbow.getAngleXY()) - (180 - leftElbow.max);
    double Larm = Math.round(skeleton.leftShoulder.getAngleXY()) - (180 - leftArm.max);
    double Lshoulder = Math.round(skeleton.leftShoulder.getAngleYZ()) + leftShoulder.min;
    double RElbow = Math.round(skeleton.rightElbow.getAngleXY()) + rightElbow.min;
    double Rarm = Math.round(skeleton.rightShoulder.getAngleXY()) + rightArm.min;
    double Rshoulder = Math.round(skeleton.rightShoulder.getAngleYZ()) - (180 - rightShoulder.max);

    // Move the left side
    setLeftArmPosition(Lshoulder, Larm, LElbow, -1, -1);

    // Move the right side
    setRightArmPosition(Rshoulder, Rarm, RElbow, -1, -1);
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, and dependencies
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(Andrew.class.getCanonicalName());
    meta.addDescription("service for the Andrew robot");
    meta.addCategory("robot");

    return meta;
  }

}

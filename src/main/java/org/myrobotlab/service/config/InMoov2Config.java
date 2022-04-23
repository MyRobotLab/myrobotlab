package org.myrobotlab.service.config;

public class InMoov2Config extends ServiceConfig {

  // ;----------------------------- BASIC CONFIGURATION
  // ----------------------------------------
  // [MAIN]
  // FIXME - should be runtime virtual not controlled or needed by scripts
  // public String ScriptType="Virtual";
  // RightSide: Also called FINGERSTARTER : connect one arduino ( called right )
  // to use FingerStarter + inmoov right side
  // LeftSide: connect one arduino ( called left) to use head / inmoov left side
  // setup your com ports inside service_6_Arduino.config
  // NoArduino: vocal Only
  // Full: Both side arduinos connected
  // Virtual: virtual arduino and inmoov !

  // FIXME - does nothing ????
  // public boolean debug=false;

  /*
   * public String Language="en-US"; // ;
   * en-US,fr-FR,es-ES,de-DE,nl-NL,ru-RU,hi-IN,it-IT,fi-FI,pt-PT,tr-TR
   * 
   * 
   * 
   * 
   * // ;----------------------------------- END
   * --------------------------------------------------
   * 
   * // ;----------------------------- ADVANCED & OPTIONAL CONFIGURATION
   * -------------------------------------
   * 
   * // [VOCAL] public boolean mute=false; // ;if true; : robot don't talk about
   * starting actions
   * 
   * // [GENERAL] public boolean LoadingPicture=false; public boolean
   * StartupSound=true; // IuseLinux=false; // ;some things dont work on mac and
   * linux like marytts voice automatic download public boolean LaunchSwingGui =
   * false; // ;usefull to speedup the system sometime // betaversion = false;
   * // ;develop branch updates // ;----------------------------------- END
   * --------------------------------------------------
   * 
   * 
   * ////////////////////////////////// InMoovLife.config.default
   * ////////////////////////////////// // ;----------------------------- INMOOV
   * LIFE CONFIGURATION ----------------------------------------
   * 
   * // ;wip health check // [HEALTHCHECK] public boolean Activated=true; public
   * int TimerValue=60000; public boolean BatteryInSystem=false;
   * 
   * // ;ramdom move the head // [MOVEHEADRANDOM] public boolean
   * RobotCanMoveHeadWhileSpeaking=true;
   * 
   * 
   * // [SLEEPMODE] public boolean ActivatedSleep=true; public boolean
   * UsePirToWakeUp=true; public boolean UsePirToActivateTracking=false;
   * 
   * // ;Sleep 5 minutes after last presence detected public int
   * SleepTimeout=300000;
   * 
   * // ;Turn off tracking 20 seconds after last presence detected public int
   * TrackingTimeout=10000;
   * 
   * //////////////////// service_1_AudioFile.config.default
   * //////////////////////////// // ;----------------------------- AUDIO
   * CONFIGURATION ---------------------------------------- // [AUDIO]
   * 
   * public String MyMusicPath="C:\\User\\XXX\\Music\\"; // ; Define the path
   * for your music directory, this will be used when playing music by the robot
   * // ;------------------------------------- END
   * ------------------------------------------------
   * 
   * 
   * //////////////////// service_4_Ear.config.default
   * //////////////////////////// // ;----------------------------- EAR
   * CONFIGURATION ---------------------------------------- // [MAIN] public
   * String EarEngine="WebkitSpeechRecognition"; //
   * ;WebkitSpeechRecognition,AndroidSpeechRecognition,Sphinx public int
   * setContinuous=0; // ;setContinuous=0/1 0 is immediate processing public int
   * setAutoListen=1; // ;auto rearm microphone public int
   * ForceMicroOnIfSleeping=1; // MagicCommandToWakeUp=wake up ; user
   * AbstractSpeechRecognition "wakeWord" // USE wakeWord // ;sleep tweaks //
   * ;----------------------------------- END
   * --------------------------------------------------
   * 
   * 
   * //////////////////// service_5_Mouth.config.default
   * ////////////////////////////
   * 
   * // [TTS]
   * 
   * public String Speechengine="MarySpeech";
   * 
   * // ; you can use : // ;MarySpeech : open source TTS :
   * http://myrobotlab.org/service/MarySpeech // ;LocalSpeech : Local operating
   * system TTS : http://myrobotlab.org/service/LocalSpeech // ;Polly : [NEED
   * API KEY AccessKey (apiKey1) and SecretKey (apiKey2)]
   * http://myrobotlab.org/service/Polly // ;VoiceRss : [NEED API KEY (apiKey1)]
   * Free cloud service : http://myrobotlab.org/service/VoiceRss // ;IndianTts :
   * [NEED API KEY (apiKey1) and userid (apiKey2)] Hindi support :
   * http://myrobotlab.org/service/IndianTts
   * 
   * public String VoiceName="Mark";
   * 
   * // ; MarySpeech voices - http://myrobotlab.org/service/MarySpeech ( Mark,
   * etc... ) print mouth.getVoices() // ; LocalSpeech : use local windows
   * voices/macOs ( Ziza, etc... ) print mouth.getVoices() // ; amazon polly :
   * http://docs.aws.amazon.com/polly/latest/dg/voicelist.html
   * 
   * // [API_KEY] // apiKey1= // apiKey2=
   * 
   * //////////////////// service_6_Arduino.config.default
   * ////////////////////////////
   * 
   * // ;----------------------------- ARDUINOS CONFIG
   * ---------------------------------------- // [MAIN]
   * 
   * // ;my rightport if used ( /dev/ttyUSB0 for linux/macos ) public String
   * MyRightPort="COM4"; public String BoardTypeMyRightPort="atmega2560"; public
   * String ArefRightArduino="DEFAULT";
   * 
   * // ;my leftport if used public String MyLeftPort="COM3"; public String
   * BoardTypeMyLeftPort="atmega2560"; public String ArefLeftArduino="DEFAULT";
   * 
   * public boolean ForceArduinoIsConnected=false; // ; BoardType Info // ;
   * atmega2560 | atmega168 | atmega328 | atmega328p | atmega1280 | atmega32u4
   * 
   * // ;----------------------------------- END
   * --------------------------------------------------
   * 
   * 
   * //////////////////// service_8_NervoBoardRelay.config.default
   * ////////////////////////////
   * 
   * // ;----------------------------- NERVO BOARD RELAY
   * ---------------------------------------- // [MAIN] public boolean
   * isNervoboardRelayActivated=false;
   * 
   * // ;witch arduino control relay : public String
   * NervoboardRelayControlerArduino="left"; public int PinLeftNervoPower1=51;
   * public int PinRightNervoPower1=53;
   * 
   * // ;----------------------------------- END
   * --------------------------------------------------
   * 
   * //////////////////// service_9_neoPixel.config.default
   * ////////////////////////////
   * 
   * // ;----------------------------- NEOPIXEL RX/TX CONFIG
   * ---------------------------------------- // [MAIN] public boolean
   * enableNeoPixel=false; public String NeopixelMaster="left"; //
   * ;NeopixelMaster= right / left public String NeopixelMasterPort="Serial2";
   * // ;NeopixelMasterPort= COMx for usb / Serialx for rx-tx ( Serial is case
   * sensitive ) // [NEOPIXEL] public int pin=2; public int numberOfPixel=16;
   * 
   * // ;Background neopixel reactions // [BASIC_REACTIONS] public boolean
   * boot_green=true; public boolean downloadSomething_blue=true; public boolean
   * error_red=true; public boolean flash_when_speak = true; //
   * ;----------------------------------- END
   * --------------------------------------------------
   * 
   * //////////////////// service_A_Chatbot.config.default
   * //////////////////////////// // ;----------------------------- CHATBOT
   * CONFIG ---------------------------------------- // [MAIN] public boolean
   * isChatBotActivated=true; // ;----------------------------------- END
   * --------------------------------------------------
   * 
   * //////////////////// service_C_Pir.config.default
   * //////////////////////////// // ;----------------------------- PIR
   * CONFIGURATION ---------------------------------------- // [MAIN] public
   * boolean enablePir=false;
   * 
   * // ;witch arduino control pir : public String pirControlerArduino="right";
   * public int pirPin=23; public boolean PlayCurstomSoundIfDetection=true;
   * 
   * // ;----------------------------------- END
   * --------------------------------------------------
   * 
   * //////////////////// service_D_OpenCv.config.default
   * //////////////////////////// // ;----------------------------- OPENCV
   * CONFIGURATION ---------------------------------------- // [MAIN] public
   * boolean isOpenCvActivated=true; // ;Activate OpenCv service... public
   * boolean faceRecognizerActivated=true; // ;Activate faceRecognizer filter if
   * tracking activated ...
   * 
   * public int CameraIndex=0; public String DisplayRender="OpenCV"; //
   * ;Available DisplayRender : // ;Sarxos // ;VideoInput // ;OpenCV
   * 
   * public boolean streamerEnabled=false;
   * 
   * // ;----------------------------------- END
   * -------------------------------------------------- ////////////////////
   * service_D_OpenCv.config.default //////////////////////////// //
   * ;----------------------------- KINECT CONFIGURATION
   * ---------------------------------------- // [MAIN] public boolean
   * isKinectActivated=false; public int openNiShouldersOffset=-50; public
   * boolean openNiLeftShoulderInverted=true; public boolean
   * openNiRightShoulderInverted=true;
   * 
   * // ;----------------------------------- END
   * --------------------------------------------------
   * 
   * 
   * //////////////////// service_D_OpenCv.config.default
   * //////////////////////////// // ;----------------------------- VIRTUAL
   * INMOOV CONFIGURATION ---------------------------------------- // [MAIN]
   * public boolean enableSimulator=true; // ;use real arduino + virtual inmoov
   * public boolean VinmoovMonitorActivated=false; // ;some kind of control
   * center, unfinished public boolean ForceVinmoovDisable=false; // ;never
   * launch JME3/Virtual Inmoov // ;----------------------------------- END
   * --------------------------------------------------
   * 
   * //////////////////// service_G_Translator.config.default
   * //////////////////////////// // ;----------------------------- TRANSLATOR
   * CONFIGURATION ---------------------------------------- // [MAIN] public
   * String outputSpeechService="default"; // ;default=you can use your daily
   * speech service for translated things // ;if your speech service is
   * languages limited, you can use an other for translated things only : // ;(
   * MarySpeech, Polly, NaturalReaderSpeech )
   * 
   * public boolean UseMaleVoice=false; // apikey= // ;Get your Microsoft KEY :
   * https://github.com/MyRobotLab/inmoov/wiki/TRANSLATOR-SERVICE //
   * ;----------------------------------- END
   * --------------------------------------------------
   * 
   * //////////////////// service_H_OpenWeatherMap.config.default
   * //////////////////////////// // ;-----------------------------
   * OPENWEATHERMAP CONFIGURATION ---------------------------------------- //
   * [MAIN] public String setUnits="metric"; // ;or imperial // apikey= public
   * String town="paris,FR"; // ;Get your KEY : https://home.openweathermap.org
   * // ;----------------------------------- END
   * --------------------------------------------------
   * 
   * //////////////////// service_I_UltraSonicSensor.config.default
   * //////////////////////////// // ;----------------------------- ultra Sonic
   * Sensor CONFIGURATION ---------------------------------------- // [MAIN]
   * public boolean ultraSonicRightActivated=false; public String
   * ultraSonicRightArduino="right"; public int trigRightPin=64; public int
   * echoRightPin=63;
   * 
   * public boolean ultraSonicLeftActivated=false; public String
   * ultraSonicLeftArduino="left"; public int trigLeftPin=64; public int
   * echoLeftPin=63; //
   * ;--------------------------------END---------------------------------------
   * ----
   * 
   * 
   * //////////////////////////// service_J_SensorFinger.config
   * //////////////////////////////////
   * 
   * // ;----------------------------- finger Sensor CONFIGURATION
   * ----------------------------------------; // [MAIN]
   * 
   * public boolean rightHandSensorActivated=false; public String
   * rightHandSensorArduino="right";
   * 
   * public int right_thumb_Psi_min=544 ;// Put here the max value when the
   * sensor is NOT pressed public int right_thumb_Psi_low=545 ;// In average
   * cases take the min value and add +1 if using a AH3503 Hall Sensor public
   * int right_thumb_Psi_mid=547 ;// In average cases take the min value and add
   * +4 public int right_thumb_Psi_max=550 ;// In average cases take the min
   * value and add +7
   * 
   * public int right_index_Psi_min=538; public int right_index_Psi_low=539;
   * public int right_index_Psi_mid=542; public int right_index_Psi_max=545;
   * 
   * public int right_majeure_Psi_min=544; public int right_majeure_Psi_low=545;
   * public int right_majeure_Psi_mid=547; public int right_majeure_Psi_max=550;
   * 
   * public int right_ringFinger_Psi_min=544; public int
   * right_ringFinger_Psi_low=545; public int right_ringFinger_Psi_mid=547;
   * public int right_ringFinger_Psi_max=550;
   * 
   * public int right_pinky_Psi_min=544; public int right_pinky_Psi_low=545;
   * public int right_pinky_Psi_mid=547; public int right_pinky_Psi_max=550;
   * 
   * public int right_extra_Psi_min=544; public int right_extra_Psi_low=545;
   * public int right_extra_Psi_mid=547; public int right_extra_Psi_max=550;
   * 
   * 
   * // ;analog pin range are 14-18 on uno, 54-70 on mega; public int
   * right_thumbPin=54 ;//--------A0; public int right_indexPin=55
   * ;//--------A1; public int right_majeurePin=56 ;//------A2; public int
   * right_ringFingerPin=57 ;//---A3; public int right_pinkyPin=58
   * ;//--------A4; public int right_extraPin=62 ;//--------A5;
   * 
   * 
   * public boolean leftHandSensorActivated=false; public String
   * leftHandSensorArduino="left";
   * 
   * public int left_thumb_Psi_min=544; public int left_thumb_Psi_low=545;
   * public int left_thumb_Psi_mid=547; public int left_thumb_Psi_max=550;
   * 
   * public int left_index_Psi_min=544; public int left_index_Psi_low=545;
   * public int left_index_Psi_mid=547; public int left_index_Psi_max=550;
   * 
   * public int left_majeure_Psi_min=544; public int left_majeure_Psi_low=545;
   * public int left_majeure_Psi_mid=547; public int left_majeure_Psi_max=550;
   * 
   * public int left_ringFinger_Psi_min=544; public int
   * left_ringFinger_Psi_low=545; public int left_ringFinger_Psi_mid=547; public
   * int left_ringFinger_Psi_max=550;
   * 
   * public int left_pinky_Psi_min=544; public int left_pinky_Psi_low=545;
   * public int left_pinky_Psi_mid=547; public int left_pinky_Psi_max=550;
   * 
   * public int left_extra_Psi_min=544; public int left_extra_Psi_low=545;
   * public int left_extra_Psi_mid=547; public int left_extra_Psi_max=550;
   * 
   * // ;analog pin range are 14-18 on uno, 54-70 on mega; public int
   * left_thumbPin=54 ;//--------A0; public int left_indexPin=55 ;//--------A1;
   * public int left_majeurePin=56 ;//------A2; public int left_ringFingerPin=57
   * ;//---A3; public int left_pinkyPin=58 ;//--------A4; public int
   * left_extraPin=62 ;//--------A5;
   * 
   * /////////////////////////////////// skeleton_eyeLids.config
   * ////////////////////////////
   * 
   * // ;----------------------------- EYELID CONFIGURATION
   * ---------------------------------------- // [MAIN] public boolean
   * enableEyelids=false; public String EyeLidsConnectedToArduino="right"; //
   * ;chose left or right existing and connected arduino
   * 
   * public boolean EyeLidsLeftActivated=false; public boolean
   * EyeLidsRightActivated=false; // ;EyeLidsLeftActivated : 1 servo control 2
   * eyelids // ;EyeLidsRightActivated+EyeLidsLeftActivated : 1 servo control 1
   * eyelid
   * 
   * 
   * // [SERVO_MINIMUM_MAP_OUTPUT] // ;your servo minimal limits public int
   * eyelidLeft=60; public int eyelidRight=60;
   * 
   * // [SERVO_MAXIMUM_MAP_OUTPUT] // ;your servo maximal limits public int
   * eyelidLeftMap=120; public int eyelidRightMap=120;
   * 
   * // [SERVO_REST_POSITION] public int eyelidLeftRest=0; public int
   * eyelidRightRest=0;
   * 
   * 
   * // ;----------------------------------- ADVANCED CONFIGURATION
   * --------------------------------------------------
   * 
   * // [SERVO_INVERTED] public boolean eyelidLeftInverted=false; public boolean
   * eyelidRightInverted=false;
   * 
   * // [MAX_SPEED] public int eyelidLeftSpeed=100; public int
   * eyelidRightSpeed=100;
   * 
   * //[MINIMUM_MAP_INPUT] public int eyelidLeftInput=0; public int
   * eyelidRightInput=0;
   * 
   * // [MAXIMUM_MAP_INPUT] // public int eyelidLeftMap=180; //public int
   * eyelidRightMap=180;
   * 
   * // [SERVO_PIN] public int eyelidLeftPin=22; public int eyelidRightPin=24;
   * 
   * // [SERVO_AUTO_DISABLE] public boolean eyelidLeftDisable=true; public
   * boolean eyelidRightDisable=true;
   * 
   * // ;----------------------------------- END
   * --------------------------------------------------
   * 
   * 
   * 
   * 
   * public boolean isController3Activated = false; public boolean
   * isController4Activated = false; public boolean isLeftPortActivated =
   * false;; public boolean isRightPortActivated = false;
   * 
   * 
   * public boolean RobotCanMoveBodyRandom; public boolean
   * RobotCanMoveEyesRandom; public boolean RobotCanMoveHeadRandom; public
   * boolean RobotCanMoveRandom; public boolean RobotIsSleeping; public boolean
   * RobotIsStarted;
   */
  ////////////////// VET'D CONFIG BEGINS ///////////////////////////

  /**
   * Wake word functionality is activated when it is set (ie not null) This
   * means recognizing events will be processed "after" it hears the wake word.
   * It will continue to publish events until a idle timeout period is reached.
   * It can continue to listen after this, but it will not publish. It fact, it
   * 'must' keep listening since in this idle state it needs to search for the
   * wake word
   */
  // public String wakeWord; - needs to be in the speech syntheis config

  // enable peers TODO - this should be done auto-magically by peers
  // FIXME - this monotonous variable propagation and checking/setting starting stuff
  // should be done automagically by the framework - freeing my time to do more fun stuff
  public boolean enableAudioPlayer = true;
  public boolean enableChatBot = false;
  public boolean enableEar = false;
  public boolean enableEyeTracking = false;
  public boolean enableFsm = false;
  public boolean enableHeadTracking = false;
  public boolean enableHtmlFilter = false;
  public boolean enableHead = false;
  public boolean enableImageDisplay = false;
  public boolean enableLeftArm = false;
  public boolean enableLeftHand = false;
  public boolean enableMouth = false;
  public boolean enableMouthControl = false;
  public boolean enableNeoPixel = false;
  public boolean enableOpenCV = false;
  public boolean enableOpenWeatherMap = false;
  public boolean enablePid = false;
  public boolean enablePir = false;
  public boolean enableRandom = false;
  public boolean enableRightHand = false;
  public boolean enableRightArm = false;
  public boolean enableServoMixer = false;
  public boolean enableSimulator = false;
  public boolean enableTorso = false;
  public boolean enableUltrasonicLeft = false;
  public boolean enableUltrasonicRight = false;

  public boolean pirWakeUp = false;
  public boolean pirEnableTracking = false;

  public boolean loadGestures = true;

  public boolean virtual = false;

  public String locale = "en-US";

  /**
   * startup and shutdown will pause inmoov - set the speed to this value then
   * attempt to move to rest
   */
  public double shutdownStartupSpeed = 50;

  // peers - this will hold the peer names
  public String audioPlayer;
  public String chatBot;
  public String ear;
  public String eyeTracking;
  public String fsm;
  public String headTracking;
  public String htmlFilter;
  public String head;
  public String imageDisplay;
  public String leftArm;
  public String leftHand;
  public String mouth;
  public String mouthControl;
  public String neoPixel;
  public String opencv;
  public String openWeatherMap;
  public String pid;
  public String pir;
  public String random;
  public String rightHand;
  public String rightArm;
  public String servoMixer;
  public String simulator;
  public String torso;
  public String ultrasonicLeft;
  public String ultrasonicRight;

}

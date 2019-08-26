##############################################################
# MyRobotLab configuration file
# This file is generated from a running instance of MyRobotLab.
# It is meant to get MyRobotLab as close to that instance's state a possible.
# This file can be generated at any time using Runtime.save(filename)
# More information @ http://myrobotlab.org and https://github.com/myrobotlab
# version 1.1.86
# generated Sun Jul 14 21:56:28 PDT 2019

##############################################################
## imports ####
import org.myrobotlab.framework.Platform as Platform

Platform.setVirtual(True)
##############################################################
## creating and starting 22 services ####
# Although Runtime.start(name,type) both creates and starts services it might be desirable on creation to
# substitute peers, types or references of other sub services before the service is "started"
# e.g. i01 = Runtime.create('i01', 'InMoov') # this will "create" the service and config could be manipulated before starting 
# e.g. i01_left = Runtime.create('i01.left', 'Ssc32UsbServoController')
_dev_ttyUSB0_UART = Runtime.start('_dev_ttyUSB0.UART', 'Serial')
gui = Runtime.start('gui', 'SwingGui')
security = Runtime.start('security', 'Security')
worke = Runtime.start('worke', 'WorkE')
worke_brain = Runtime.start('worke.brain', 'ProgramAB')
worke_cli = Runtime.start('worke.cli', 'Cli')
worke_controller = Runtime.start('worke.controller', 'Sabertooth')
worke_controller_serial = Runtime.start('worke.controller.serial', 'Serial')
worke_cv = Runtime.start('worke.cv', 'OpenCV')
worke_ear = Runtime.start('worke.ear', 'WebkitSpeechRecognition')
worke_emoji = Runtime.start('worke.emoji', 'Emoji')
worke_emoji_display = Runtime.start('worke.emoji.display', 'ImageDisplay')
worke_emoji_fsm = Runtime.start('worke.emoji.fsm', 'FiniteStateMachine')
worke_emoji_http = Runtime.start('worke.emoji.http', 'HttpClient')
worke_git = Runtime.start('worke.git', 'Git')
worke_joystick = Runtime.start('worke.joystick', 'Joystick')
worke_motorLeft = Runtime.start('worke.motorLeft', 'MotorPort')
worke_motorRight = Runtime.start('worke.motorRight', 'MotorPort')
worke_mouth = Runtime.start('worke.mouth', 'Polly')
worke_mouth_audioFile = Runtime.start('worke.mouth.audioFile', 'AudioFile')
worke_webgui = Runtime.start('worke.webgui', 'WebGui')

##############################################################
## configuring services ####

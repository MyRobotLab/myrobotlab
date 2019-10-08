# FIXME - "default" checkPoint() - e.g. watchdog.checkPoint() should work
# FIXME - re-running the script makes multiple timers & multiple corrective actions

# start services
Runtime.start("joy", "Joystick")
Runtime.start("gui","SwingGui")
Runtime.start("watchdog", "WatchDogTimer")
Runtime.start("python", "Python")
Runtime.start("m1", "Motor")

# adding and activating a checkpoint
watchdog.addTimer("joystickCheck")
watchdog.addAction("m1", "stop")

# python subscribes to joystick data
python.subscribe("joy","publishJoystickInput")

# new joystick data suppresses activation action
def onJoystickInput(data):
  watchdog.checkPoint("watchdog", "joystickCheck")

# stop the watchdog
# watchdog.stop()

# start the watchdog
# watchdog.start()

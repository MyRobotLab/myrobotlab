# Perception - pir, camera, ultrasonic, kinect, network, mqtt, 
# Behaviors
# Actuators, lights, servos, work-e
# is kristina here -> (zone minder feed & iphone )


# FIXME - def subscribe(blah, "") along with console
# FIXME - easy to use state saving/getting
# FIXME - centralized peer definition - and name management
# FIXME - centralize subscription maintenance

robot = 'i01'

toggle_heartbeat = True
global_state = {
    "toggle_heartbeat":True
}

# need to start inmoov service here
# because we have subscribes to do - (subscribes handled internally?)
runtime.start(robot, 'InMoov2')

# i01/InMoov #################################
# publishing point from the inmoov2 service
def onHeartbeat(src):
    global global_state

    # print('toggle_heartbeat', src)
    if runtime.isStarted('neomouth'):
        if global_state.get('toggle_heartbeat'):
            neomouth.fill(10,0,0)
            global_state['toggle_heartbeat'] = False
        else:
            neomouth.fill(0,0,0)
            global_state['toggle_heartbeat'] = True

python.subscribe(robot, 'publishHeartbeat')


# pir #####################################
def onSense(sensed):
    print('onSense', sensed)
    if runtime.isStarted('neomouth'):
        neomouth.fill(0,10,0)

python.subscribe(robot + '.pir', 'publishSense')

# opencv ##############################
def onClassification(classification):
    print('onClassification', classification)
    if runtime.isStarted('neomouth'):
        neomouth.fill(0,0,10)

python.subscribe(robot + '.opencv', 'publishClassification')

# chatBot #############################
def onText(text):
    print('publishText', text)
    if text == 'Power down.':
        # publish('speak', 'powering down now')
        print('powering down now')
    elif text == 'System check.':
        print('system check')
        # publish('speak', 'performing a system check')
        runtime.getServices().size()

python.subscribe(robot + '.chatBot', 'publishText')

# runtime #############################
def publishIpFound(ip):
    print('publishIpFound', ip)

def publishIpNotFound(ip):
    print('publishIpNotFound', ip)

python.subscribe(robot + '.chatBot', 'publishText')


print('loaded dewey.py')


# these subscriptions need fixing !!
# i01_chatBot.attach(i01_ear) 
# yep, it worked
# test 



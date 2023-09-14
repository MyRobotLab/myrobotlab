angular.module('mrlapp.service.ServoMixerGui', []).controller('ServoMixerGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('ServoMixerGuiCtrl')
    var _self = this
    var msg = this.msg
    var globalPoseIndex = 0

    $scope.minView = true
    $scope.delay = 3 // initial
    $scope.showGestureSave = false

    $scope.searchServo = {
        displayName: null
    }
    
    $scope.servos = []
    $scope.sliders = []
    // list of current pose files
    $scope.poseFiles = []
    $scope.gestureFiles = []

    $scope.state = {
        // gestureIndex is a string representation from $index :( dumb
        'gestureIndex': "0",
        'selectedPose': null,
        'selectedGestureFile': null,
        'selectedGesture': null,
        'playingPose': null,
        'currentGesture':{
            'parts':[]
        }
    }

    $scope.options = [];
  angular.forEach("a:alpha,b:beta,d:delta,g:gamma,e:eta,E:epsilon,o:omega,z:zeta".split(','), function(val) {
    var parts = val.split(":");
    $scope.options.push({
      name: parts[0],
      value: parts[1]
    });
  });

    // unique id for new poses added to gesture
    let id = 0

    // FIXME - this should be done in a base class or in framework
    $scope.mrl = mrl;

    // sublist object of servo panels - changes based onRegistered and onReleased events
    $scope.subPanels = {}

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {        
        // do the update
        $scope.service = service
    }

    $scope.toggle = function(servo) {
        $scope.sliders[servo].tracking = !$scope.sliders[servo].tracking
    }

    _self.onSliderChange = function(servoName) {
        if (!$scope.sliders[servoName].tracking) {
            msg.sendTo(servoName, 'moveTo', $scope.sliders[servoName].value)
        }
    }

    $scope.setSearchServo = function(text) {
        $scope.searchServo.displayName = text
    }

    this.updateState($scope.service)

    this.onMsg = function(inMsg) {
        var data = inMsg.data[0];
        switch (inMsg.method) {
        case 'onStatus':
            console.log(inMsg)
            break
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onSearch':
            $scope.searchServo.displayName = data
            $scope.searchServos(data)
            // sets pose name from selected
            $scope.state.selectedPose = data + "_" + globalPoseIndex++
            $scope.$apply()
            break                
        case 'onPlayingGesturePart':
            // FIXME rename
            if (data.type != 'Delay'){
                $scope.state.playingPose = data    
            } else {
                $scope.state.playingPose.value = data.value/1000
            }
            $scope.$apply()
            break
        case 'onPlayingGesturePartIndex':
            // FIXME rename
            $scope.state.gestureIndex = data + ""
            $scope.state.playingPoseIndex = data
            $scope.$apply()
            break
        case 'onStopPose':
            // $scope.state.playingPose = ' '
            $scope.$apply()
            break
        case 'onServoEvent':
            $scope.sliders[data.name].value = data.pos;
            $scope.$apply()
            break
        case 'onPoseFiles':
            $scope.poseFiles = data
            $scope.$apply()
            if (data && data.length > 0) {
                $scope.state.selectedPose = data[data.length - 1]
            }
            break
        case 'onGesture':
            $scope.state.currentGesture = data
            $scope.$apply()
            break
        case 'onGestureFiles':
            $scope.gestureFiles = data
            if (!$scope.state.selectedGestureFile && $scope.gestureFiles && $scope.gestureFiles.length > 0){
                $scope.state.selectedGestureFile = $scope.gestureFiles[0]
                msg.send('getGesture', $scope.state.selectedGestureFile)
            }
            $scope.$apply()
            break
        case 'onListAllServos':
            // servos sliders are either in "tracking" or "control" state
            // "tracking" they are moving from callback position info published by servos
            // "control" they are sending control messages to the servos
            $scope.servos = data
            for (var servo of $scope.servos) {
                // dynamically build sliders
                $scope.sliders[servo.name] = {
                    value: 0,
                    tracking: false,
                    options: {
                        id: servo.name,
                        floor: 0,
                        ceil: 180,
                        onStart: function(id) {
                            console.info('ServoMixer.onStart')
                        },
                        onChange: function(id) {
                            _self.onSliderChange(id)
                        },
                        /*
                        onChange: function() {
                            if (!this.tracking) {
                                // if not tracking then control
                                msg.sendTo(servo, 'moveToX', sliders[servo].value)
                            }
                        },*/
                        onEnd: function(id) {}
                    }
                }
                // dynamically add callback subscriptions
                // these are "intermediate" subscriptions in that they
                // don't send a subscribe down to service .. yet 
                // that must already be in place (and is in the case of Servo.publishServoEvent)
                // FIXME .. servo.getName() == servo.getFullName() :( - needs to be done in framework
                msg.subscribeTo(_self, servo.name + '@' + servo.id, 'publishServoEvent')

            }
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }


    $scope.addPoseToGesture = function() {
        // get pos entry
        let pose = {
            'name': $scope.state.selectedPose,
            'type': 'Pose',
            'blocking': false
        }

        $scope.state.currentGesture.parts.splice(parseInt($scope.state.gestureIndex) + 1, 0, pose)
    }

    $scope.removePoseFromGesture = function() {
        $scope.state.currentGesture.parts.splice($scope.state.gestureIndex, 1)
    }

    move = function(arr, fromIndex, toIndex) {
        var element = arr[fromIndex];
        arr.splice(fromIndex, 1);
        arr.splice(toIndex, 0, element);
        // stupid ass conversion back to string for list 'select'
        $scope.state.gestureIndex = toIndex + ''
    }

    $scope.moveUpPoseInGesture = function() {
        move($scope.state.currentGesture.parts, $scope.state.gestureIndex, parseInt($scope.state.gestureIndex) - 1)
    }

    $scope.moveDownPoseInGesture = function() {
        move($scope.state.currentGesture.parts, $scope.state.gestureIndex, parseInt($scope.state.gestureIndex) + 1)
    }

    $scope.searchServos = function(searchText) {
        var result = {}
        angular.forEach($scope.subPanels, function(value, key) {
            if (!searchText || mrl.getShortName(key).indexOf(searchText) != -1) {
                result[key] = value;
            }
        }) 
        return result
    }

    $scope.savePose = function(pose) {
        msg.send('savePose', pose);
    }

    // this method initializes subPanels when a new service becomes available
    this.onRegistered = function(panel) {
        if (panel.simpleName == 'Servo') {
            $scope.subPanels[panel.name] = panel
        }
    }

    // this method removes subPanels references from released service
    this.onReleased = function(panelName) {
        delete $scope.subPanels[panelName]
        console.info('here')
    }

    $scope.step = function(){
        let index = parseInt($scope.state.gestureIndex)
        let part = $scope.state.currentGesture.parts[index]
        if (part.type === 'Pose'){
            msg.send('moveToPose', part.name)    
        }
        index++
        $scope.state.gestureIndex =  index + ""
        
    }

    // initialize all services which have panel references in Intro    
    let servicePanelList = mrl.getPanelList()
    for (let index = 0; index < servicePanelList.length; ++index) {
        this.onRegistered(servicePanelList[index])
    }

    $scope.saveGesture = function(gestureName) {
        // gestureName = $scope.state.selectedGestureFile
        $scope.state.currentGesture.name = gestureName
        if ($scope.gestureFiles.includes(gestureName)){
            // saving current file
            msg.send('saveGesture', gestureName, $scope.state.currentGesture)
        } else {
            // saving new file
            blankGesture = {
                parts:[],
                repeat: false
            }
            msg.send('saveGesture', gestureName, blankGesture)
        }
        
    }

    $scope.addDelay = function(seconds){

        let value = parseFloat(seconds)
        
        if (Number.isNaN(value)){
            console.error(seconds, "is not a valid number for delay")
            return
        }
        
        let delay = {
            'name': 'delay',
            'type': 'Delay',
            'value': value * 1000,
            'blocking': true
        }
        
        $scope.state.currentGesture.parts.splice(parseInt($scope.state.gestureIndex) + 1, 0, delay)
    }

    $scope.playGesture = function(gesture) {
        if (gesture){
            msg.send('playGesture', gesture)    
        } else {
            console.warn('gesture empty')
        }
    }

    $scope.removeGesture = function(gesture) {
        if (gesture){
            msg.send('removeGesture', gesture)    
        } else {
            console.warn('removeGesture empty')
        }
    }

    $scope.displayValue = function(pose) {
        // !pose.value || Number.isNaN(pose.value)?'':pose.value/1000
        if (pose.type == 'Delay'){
            return pose.value/1000
        } else {
            return pose.value
        }
    }

    $scope.speak = function() {
        
        let delay = {
            'name': 'speech',
            'type': 'Speech',
            'value': $scope.text,
            'blocking': true
        }
        
        $scope.state.currentGesture.parts.splice(parseInt($scope.state.gestureIndex) + 1, 0, delay)
    }


    msg.subscribe('getPoseFiles')
    msg.subscribe('getGesture')
    msg.subscribe('getGestureFiles')
    msg.subscribe('listAllServos')
    msg.subscribe('search')
    
    msg.send('listAllServos')
    msg.send('getPoseFiles');
    msg.send('getGestureFiles');

    msg.subscribe("publishPlayingGesturePart")
    msg.subscribe("publishPlayingGesturePartIndex")
    msg.subscribe("publishStopPose")

    mrl.subscribeToRegistered(this.onRegistered)
    mrl.subscribeToReleased(this.onReleased)

    msg.subscribe(this)
}
])

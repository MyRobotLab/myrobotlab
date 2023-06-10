angular.module('mrlapp.service.ServoMixerGui', []).controller('ServoMixerGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('ServoMixerGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.minView = true
    $scope.delay = 3 // initial

    $scope.searchServo = {
        displayName: null
    }
    
    $scope.servos = []
    $scope.sliders = []
    // list of current pose files
    $scope.poseFiles = []
    $scope.sequenceFiles = []

    $scope.state = {
        // sequenceIndex is a string representation from $index :( dumb
        'sequenceIndex': "0",
        'selectedPose': null,
        'selectedSequenceFile': null,
        'selectedSequence': null,
        'currentRunningPose': null,
        'currentSequence':{
            'parts':[]
        }
    }

    // unique id for new poses added to sequence
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
            $scope.$apply()
            break                
        case 'onPlayingPose':
            $scope.state.currentRunningPose = data
            $scope.$apply()
            break
        case 'onStopPose':
            $scope.state.currentRunningPose = ' '
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
        case 'onSequence':
            $scope.state.currentSequence = data
            $scope.$apply()
            break
        case 'onSequenceFiles':
            $scope.sequenceFiles = data
            if (!$scope.state.selectedSequenceFile && $scope.sequenceFiles && $scope.sequenceFiles.length > 0){
                $scope.state.selectedSequenceFile = $scope.sequenceFiles[0]
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


    $scope.addPoseToSequence = function() {
        // get pos entry
        let pose = {
            'name': $scope.state.selectedPose,
            'type': 'Pose',
            'blocking': false
        }

        $scope.state.currentSequence.parts.splice(parseInt($scope.state.sequenceIndex) + 1, 0, pose)
    }

    $scope.removePoseFromSequence = function() {
        $scope.state.currentSequence.parts.splice($scope.state.sequenceIndex, 1)
    }

    move = function(arr, fromIndex, toIndex) {
        var element = arr[fromIndex];
        arr.splice(fromIndex, 1);
        arr.splice(toIndex, 0, element);
        // stupid ass conversion back to string for list 'select'
        $scope.state.sequenceIndex = toIndex + ''
    }

    $scope.moveUpPoseInSequence = function() {
        move($scope.state.currentSequence.parts, $scope.state.sequenceIndex, parseInt($scope.state.sequenceIndex) - 1)
    }

    $scope.moveDownPoseInSequence = function() {
        move($scope.state.currentSequence.parts, $scope.state.sequenceIndex, parseInt($scope.state.sequenceIndex) + 1)
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
        let index = parseInt($scope.state.sequenceIndex)
        let part = $scope.state.currentSequence.parts[index]
        if (part.type === 'Pose'){
            part.send('moveToPose', part)    
        }        
    }

    // initialize all services which have panel references in Intro    
    let servicePanelList = mrl.getPanelList()
    for (let index = 0; index < servicePanelList.length; ++index) {
        this.onRegistered(servicePanelList[index])
    }

    $scope.saveSequence = function(name) {
        $scope.state.currentSequence.name = name
        msg.send('saveSequence', name, $scope.state.currentSequence)
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
        
        $scope.state.currentSequence.parts.splice(parseInt($scope.state.sequenceIndex) + 1, 0, delay)
    }

    $scope.playSequence = function(sequence) {
        if (sequence){
            msg.send('playSequence', sequence)    
        } else {
            console.warn('sequence empty')
        }
    }

    $scope.removeSequence = function(sequence) {
        if (sequence){
            msg.send('removeSequence', sequence)    
        } else {
            console.warn('removeSequence empty')
        }
    }

    msg.subscribe('getPoseFiles')
    msg.subscribe('getSequence')
    msg.subscribe('getSequenceFiles')
    msg.subscribe('listAllServos')
    msg.subscribe('search')
    
    msg.send('listAllServos')
    msg.send('getPoseFiles');
    msg.send('getSequenceFiles');

    msg.subscribe("publishPlayingPose")
    msg.subscribe("publishStopPose")

    mrl.subscribeToRegistered(this.onRegistered)
    mrl.subscribeToReleased(this.onReleased)

    msg.subscribe(this)
}
])

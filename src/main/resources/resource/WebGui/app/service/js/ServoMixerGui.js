angular.module('mrlapp.service.ServoMixerGui', []).controller('ServoMixerGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('ServoMixerGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.minView = true
    
    $scope.servos = []
    $scope.sliders = []
    // list of current pose files
    $scope.poseFiles = []
    $scope.sequenceFiles = []

    $scope.state = {
        'selectedPose': null,
        'selectedSequenceFile': null,
        'selectedSequence': null,
        'currentRunningPose': null,
        'currentSequence':{
            'poses':[]
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

    getIndexOfSelectedPoseInSequence = function() {
        if (!$scope.state.selectedSequence) {
            return 0
        }

        // change the selected sequence back into an object
        let ssId = JSON.parse($scope.state.selectedSequence).id

        let index = 0
        for (var p of $scope.state.currentSequence.poses) {
            posId = $scope.state.currentSequence.poses[index].id
            if (ssId == posId) {
                return index
            }
            index++
        }
        return index
    }

    $scope.addPoseToSequence = function() {
        // get pos entry
        let pose = {
            'id': id,
            'name': $scope.state.selectedPose,
            'waitTimeMs': 3000
        }

        // maintain unique id
        ++id

        let currentIndex = getIndexOfSelectedPoseInSequence()
        currentIndex++
        $scope.state.currentSequence.poses.splice(currentIndex, 0, pose)
    }

    $scope.removePoseFromSequence = function() {
        let currentIndex = getIndexOfSelectedPoseInSequence()
        $scope.state.currentSequence.poses.splice(currentIndex, 1)
    }

    move = function(arr, fromIndex, toIndex) {
        var element = arr[fromIndex];
        arr.splice(fromIndex, 1);
        arr.splice(toIndex, 0, element);
    }

    $scope.moveUpPoseInSequence = function() {
        let currentIndex = getIndexOfSelectedPoseInSequence()
        move($scope.state.currentSequence.poses, currentIndex, currentIndex - 1)
    }

    $scope.moveDownPoseInSequence = function() {
        let currentIndex = getIndexOfSelectedPoseInSequence()
        move($scope.state.currentSequence.poses, currentIndex, currentIndex + 1)
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

    $scope.setSequence = function() {
        let seq = JSON.parse($scope.state.selectedSequence)
        $scope.delay = seq.waitTimeMs/1000
        console.info($scope.state.selectedSequence)
    }

    $scope.moveSequenceContent = function(seqstr){
        let seq = JSON.parse(seqstr)
        msg.send('moveToPose', seq.name)
    }

    // initialize all services which have panel references in Intro    
    let servicePanelList = mrl.getPanelList()
    for (let index = 0; index < servicePanelList.length; ++index) {
        this.onRegistered(servicePanelList[index])
    }

    $scope.saveSequence = function(name) {
        $scope.state.currentSequence.name = name
        /*
        $scope.state.currentSequence.poses = []
        for (var p of $scope.state.currentSequence.poses) {
            $scope.state.currentSequence.poses.push(p.name)
        }*/

        // because angular adds crap to identify select options :(
        // let json = JSON.stringify($scope.state.currentSequence)
        let json = angular.toJson($scope.state.currentSequence)
        msg.send('saveSequence', name, json)
    }

    $scope.addSequenceDelay = function(delay){
        let index = getIndexOfSelectedPoseInSequence()
        if (delay == ""){
            $scope.state.currentSequence.poses[index].waitTimeMs = null
        } else {
            $scope.state.currentSequence.poses[index].waitTimeMs = delay * 1000
        }
    }

    msg.subscribe('getPoseFiles')
    msg.subscribe('getSequence')
    msg.subscribe('getSequenceFiles')
    msg.subscribe('listAllServos')
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

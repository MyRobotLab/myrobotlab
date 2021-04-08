angular.module('mrlapp.service.ServoMixerGui', []).controller('ServoMixerGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('ServoMixerGuiCtrl')
    var _self = this
    var msg = this.msg
    $scope.selectedPose = ""
    $scope.currentPose = {}
    $scope.servos = []
    $scope.sliders = []
    $scope.poseFiles = []
    $scope.sequenceFiles = []
    $scope.loadedPose = null
    // FIXME - this should be done in a base class or in framework
    $scope.mrl = mrl;

    // sublist object of servo panels - changes based onRegistered and onReleased events
    $scope.subPanels = {}

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        if (!service.currentPose) {
            // user has no definition
            service.currentPose = {}
        } else {
            // replace with service definition
            $scope.currentPose = service.currentPose
        }
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
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onServoEvent':
            $scope.sliders[data.name].value = data.pos;
            $scope.$apply()
            break
        case 'onPoseFiles':
            $scope.poseFiles = data
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

    $scope.searchServos = function(searchText) {
        var result = {}
        angular.forEach($scope.subPanels, function(value, key) {
            if (!searchText || mrl.getShortName(key).indexOf(searchText) != -1){
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

    // initialize all services which have panel references in Intro    
    let servicePanelList = mrl.getPanelList()
    for (let index = 0; index < servicePanelList.length; ++index) {
        this.onRegistered(servicePanelList[index])
    }

    msg.subscribe('getPoseFiles')
    msg.subscribe('listAllServos')
    msg.send('listAllServos')
    msg.send('getPoseFiles');

    mrl.subscribeToRegistered(this.onRegistered)
    mrl.subscribeToReleased(this.onReleased)

    msg.subscribe(this)
}
])

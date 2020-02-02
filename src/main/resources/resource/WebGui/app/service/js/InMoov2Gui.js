angular.module('mrlapp.service.InMoov2Gui', []).controller('InMoov2GuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    $log.info('InMoov2GuiCtrl')
    var _self = this
    var msg = this.msg
    $scope.servos = []
    $scope.sliders = []

    // text published from InMoov2 service
    $scope.onText = null
    $scope.languageSelected = null
    $scope.speakText = null

    $scope.speechTypeSelected = 'org.myrobotlab.service.MarySpeech'
    $scope.speechTypes = null
    $scope.mouth = null

    $scope.selectedGesture = null

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        $scope.languageSelected = service.locale.tag

        $scope.mouth = mrl.getService(service.name + '.mouth')
        $scope.$apply()
    }

    $scope.toggle = function(servo) {
        $scope.sliders[servo].tracking = !$scope.sliders[servo].tracking
    }

    _self.onSliderChange = function(servo) {
        if (!$scope.sliders[servo].tracking) {
            msg.sendTo(servo, 'moveTo', $scope.sliders[servo].value)
        }
    }

    $scope.active = ["btn", "btn-default", "active"]

    $scope.executeGesture = function(gesture) {
        msg.send('execGesture', gesture);
    }

    $scope.setActive = function(val) {
        var index = array.indexOf(5);
        if (index > -1) {
            array.splice(index, 1);
        }
    }

    $scope.getStyle = function(bool) {
        // return ['btn', 'btn-default', 'active']
        return 'active';
        // return mrl.getStyle(bool)
    }

    $scope.getPeer = function(peerName) {
        let s = mrl.getService($scope.service.name + '.' + peerName + '@' + this.service.id)
        return s
    }

    $scope.startMouth = function() {
        msg.send('setSpeechType', $scope.speechTypeSelected)
        msg.send('startMouth')
    }

    $scope.speak = function() {
        if ($scope.mouth == null) {
            $scope.startMouth()
        }
        msg.send('speakBlocking', $scope.speakText)
    }

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0];

        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onServiceTypeNamesFromInterface':
            $scope.speechTypes = data.serviceTypes;
            $scope.$apply()
            break
        case 'onText':
            $scope.onText = data;
            $scope.$apply()
            break
        case 'onServoData':

            $scope.sliders[data.name].value = data.pos;
            $scope.$apply()
            break
        case 'onServoNames':
            // servos sliders are either in "tracking" or "control" state
            // "tracking" they are moving from callback position info published by servos
            // "control" they are sending control messages to the servos
            $scope.servos = inMsg.data[0]
            for (var servo of $scope.servos) {
                // dynamically build sliders
                $scope.sliders[servo] = {
                    value: 0,
                    tracking: true,
                    options: {
                        id: servo,
                        floor: 0,
                        ceil: 180,
                        onStart: function(id) {},
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
                // that must already be in place (and is in the case of Servo.publishServoData)
                msg.subscribeTo(_self, servo, 'publishServoData')

            }
            $scope.$apply()
            break
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    // msg.subscribe('getServoNames')
    // msg.send('getServoNames')
    // mrl.subscribeToServiceMethod(_self.onMsg, mrl.getRuntime().name, 'getServiceTypeNamesFromInterface');

    /*

    msg.subscribeTo(_self, mrl.getRuntime().name, 'getServiceTypeNamesFromInterface')
    msg.subscribe('getServiceTypeNamesFromInterface')
    */

    // FIXME FIXME FIXME - single simple subscribeTo(name, method) !!!
    mrl.subscribe(mrl.getRuntime().name, 'getServiceTypeNamesFromInterface');
    mrl.subscribeToServiceMethod(_self.onMsg, mrl.getRuntime().name, 'getServiceTypeNamesFromInterface');

    msg.subscribe('publishText')
    msg.sendTo(mrl.getRuntime().name, 'getServiceTypeNamesFromInterface', 'SpeechSynthesis')
    msg.subscribe(this)
}
])

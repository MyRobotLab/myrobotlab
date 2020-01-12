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

    $scope.selectedGesture = null

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        $scope.languageSelected = service.language
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


    $scope.executeGesture = function(gesture){
        msg.send('execGesture', gesture);
    }

    $scope.setActive = function(val) {
        var index = array.indexOf(5);
        if (index > -1) {
            array.splice(index, 1);
        }
    }

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0];

        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
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

    msg.subscribe('publishText')
    msg.subscribe(this)
}
])

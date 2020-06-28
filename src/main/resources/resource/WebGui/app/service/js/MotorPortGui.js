angular.module('mrlapp.service.MotorPortGui', []).controller('MotorPortGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('MotorPortGuiCtrl')
    var _self = this
    var msg = this.msg
    $scope.position = 0

    $scope.speedSlider = {
        value: 0,
        options: {
            floor: -100,
            ceil: 100,
            minLimit: -100,
            maxLimit: 100,
            // hideLimitLabels: false,
            onStart: function() {},
            onChange: function() {
                msg.send('move', $scope.speedSlider.value/100)
            },
            onEnd: function() {}
        }
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        if (service.config != null) {
            var type = service.config.type
            $scope.newType = type
            if (type == 'MotorPortConfigDualPwm') {
                $scope.newPin0 = service.config.leftPin
                $scope.newPin1 = service.config.rightPin
            }
        }
    }

    this.onMsg = function(inMsg) {
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(inMsg.data[0])
            $scope.$apply()
            break
        case 'onUpdatePosition':
            $scope.position = inMsg.data[0]
            $scope.$apply()
            break
        case 'onServiceNamesFromInterface':
            $scope.controllers = inMsg.data[0]
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    $scope.attach = function() {
        console.info('attach')
        // TODO - check validity
        msg.send('attach', $scope.newController, $scope.newType, $scope.newPin0, $scope.newPin1, $scope.newEncoderType, $scope.newEncoderPin)
    }

    $scope.detach = function() {
        console.info('detach')
        msg.send('detach')
    }

    $scope.move = function() {
        console.info('move')
        msg.send('move', $scope.moveToPos)
    }
   
    msg.subscribe("updatePosition")
    msg.subscribe(this)
}
])

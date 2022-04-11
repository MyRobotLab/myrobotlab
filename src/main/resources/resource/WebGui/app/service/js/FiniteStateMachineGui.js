angular.module('mrlapp.service.FiniteStateMachineGui', []).controller('FiniteStateMachineGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('FiniteStateMachineGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.servoUpIsActive = function() {
        msg.send('runtime.start("servoUp", "Servo")')
    }
    $scope.servoMiddleIsActive = function() {
        msg.send('runtime.start("servoMiddle", "Servo")')
    }
    $scope.servoDownIsActive = function() {
        msg.send('runtime.start("servoDown", "Servo")')
    }

    // init

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
    }

    this.onMsg = function(inMsg) {
        var data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        default:
            $log.info("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method)
            break
        }

    }

    msg.subscribe(this)
}
])
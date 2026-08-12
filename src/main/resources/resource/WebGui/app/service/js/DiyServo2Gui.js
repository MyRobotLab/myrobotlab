angular.module('mrlapp.service.DiyServo2Gui', []).controller('DiyServo2GuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('DiyServo2GuiCtrl')
    var _self = this
    var msg = this.msg

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
    }

    $scope.moveTo = function(pos) {
        msg.send('moveTo', pos)
    }

    this.onMsg = function(inMsg) {
        var data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        // TODO: figure out which callbacks we will subscribe to here.
        case 'onServoEvent':
            $scope.data = data
            $scope.$apply()
            break
        case 'onServoData':
            $scope.data = data
            $scope.$apply()
            break
        case 'onStatus':
			console.info("On status")
        default:
            console.info("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method)
            break
        }

    };

    msg.subscribe('publishServoEvent')
    msg.subscribe(this)}
])

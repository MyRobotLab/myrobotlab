angular.module('mrlapp.service.LeapMotionGui', []).controller('LeapMotionGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    var _self = this
    var msg = this.msg

    // From template.
    this.updateState = function(service) {
        $scope.service = service
    }

    // leap data is pretty much everything.
    $scope.leapData = ''

    $scope.toFixed= function(num){
        if (!isNaN(num)){
            return num.toFixed(2)
        } else {
            return ''
        }
    }

    this.onMsg = function(inMsg) {
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(inMsg.data[0])
            $scope.$apply()
            break
        case 'onLeapData':
            $scope.leapData = inMsg.data[0]
            $scope.$apply()
            break
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    msg.subscribe('publishLeapData')
    //msg.subscribe('publishPoints')
    msg.subscribe(this)
}
])

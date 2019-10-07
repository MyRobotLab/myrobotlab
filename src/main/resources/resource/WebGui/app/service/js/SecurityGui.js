angular.module('mrlapp.service.SecurityGui', []).controller('SecurityGuiCtrl', ['$scope', '$log', 'mrl', '$timeout', function($scope, $log, mrl, $timeout) {
    $log.info('SecurityGuiCtrl')
    var _self = this
    var msg = this.msg

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
    }

    //you HAVE TO define this method
    //-> you will receive all messages routed to your service here
    this.onMsg = function(inMsg) {
        switch (inMsg.method) {
        case 'onState':
            $timeout(function() {
                _self.updateState(inMsg.data[0])
            })
            break
        case 'onPulse':
            $timeout(function() {
                $scope.pulseData = inMsg.data[0]
            })
            break
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }
    

    _self.updateState($scope.service)

    //to send a message to the service, use:
    //$scope.msg.<serviceFunction>()
    // $scope.msg.startClock()

    //subscribe to functions and to the service
    // msg.subscribe('x')
    msg.subscribe(this)
}
])

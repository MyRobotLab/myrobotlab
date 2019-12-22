angular.module('mrlapp.service.JMonkeyEngineGui', []).controller('JMonkeyEngineGuiCtrl', ['$scope', '$log', 'mrl', '$timeout', function($scope, $log, mrl, $timeout) {
    $log.info('JMonkeyEngineGuiCtrl')
    var _self = this
    var msg = this.msg

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
    }

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

    msg.subscribe('pulse')
    msg.subscribe(this)

}
])

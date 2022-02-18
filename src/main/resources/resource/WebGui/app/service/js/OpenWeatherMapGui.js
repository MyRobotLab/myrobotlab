angular.module('mrlapp.service.OpenWeatherMapGui', []).controller('OpenWeatherMapGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('OpenWeatherMapGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.setKey = function() {
        msg.send('setKey', $scope.currentApikey)
    }
    $scope.setUnits = function() {
        msg.send('setUnits', $scope.currentUnits)
    }
    $scope.setLocalUnits = function() {
        msg.send('setUnits', $scope.currentLocalUnits)
    }
    $scope.setTown = function() {
        msg.send('setTown', $scope.currentTown)
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
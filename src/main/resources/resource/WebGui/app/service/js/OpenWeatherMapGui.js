angular.module('mrlapp.service.OpenWeatherMapGui', []).controller('OpenWeatherMapGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('OpenWeatherMapGuiCtrl')
    var _self = this
    var msg = this.msg
    $scope.forecast = null

    $scope.units = ['metric', 'imperial']

    $scope.setKey = function() {
        msg.send('setKey', $scope.key)
    }
    $scope.setUnits = function(unit) {
        msg.send('setUnits', unit)
        msg.send('broadcastState')
    }
    $scope.setLocation = function() {
        msg.send('setLocation', $scope.service.location)
        msg.send('broadcastState')
    }

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
        case 'onKey':
            $scope.key = data;
            $scope.$apply()
            break
        case 'onFetchForecast':
            $scope.forecast = data;
            $scope.$apply()
            break
        default:
            console.log("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method)
            break
        }

    }

    msg.subscribe('fetchForecast')
    msg.subscribe('getKey')
    msg.send('getKey')
    msg.subscribe(this)
}
])

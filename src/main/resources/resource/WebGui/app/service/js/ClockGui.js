angular.module('mrlapp.service.ClockGui', []).controller('ClockGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('ClockGuiCtrl')
    var _self = this
    var msg = this.msg

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
    }

    // init scope variables
    $scope.onTime = null
    $scope.onEpoch = null

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onTime':
            const date = new Date(data);
            $scope.onTime = date.toLocaleString(); 
            $scope.$apply()
            break
        case 'onEpoch':
            $scope.onEpoch = data
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    msg.subscribe('publishTime')
    msg.subscribe('publishEpoch')
    msg.subscribe(this)
}
])

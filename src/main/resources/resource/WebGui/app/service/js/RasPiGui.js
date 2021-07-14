angular.module('mrlapp.service.RasPiGui', []).controller('RasPiGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('RasPiGuiCtrl')
    var _self = this
    var msg = this.msg

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
    }

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    $scope.scan = function(bus){
        msg.send('scan', bus)
    }

    msg.subscribe(this)
}
])

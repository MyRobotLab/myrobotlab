angular.module('mrlapp.service.DruppNeckGui', []).controller('DruppNeckGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('DruppNeckGuiCtrl')
    var _self = this
    var msg = this.msg

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

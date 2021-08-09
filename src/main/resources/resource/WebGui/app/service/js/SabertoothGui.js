angular.module('mrlapp.service.SabertoothGui', []).controller('SabertoothGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('SabertoothGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.controllers = []

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
        case 'onStatus':
        default:
            console.info("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method)
            break
        }

    }

    // msg.subscribe('publishSabertoothData')
    msg.subscribe(this)
}
])

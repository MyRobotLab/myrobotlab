angular.module('mrlapp.service.SecurityGui', []).controller('SecurityGuiCtrl', ['$scope', '$log', 'mrl', '$timeout', function($scope, $log, mrl, $timeout) {
    $log.info('SecurityGuiCtrl')
    var _self = this
    var msg = this.msg
    
    // FIXME - need better way to have multiple panels, currently this is just for testing
    $scope.mrl = mrl
    $scope.keyNames = [];
    $scope.requiredKeyNames = [];

    this.updateState = function(service) {
        $scope.service = service
    }

    //you HAVE TO define this method
    //-> you will receive all messages routed to your service here
    this.onMsg = function(inMsg) {
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(inMsg.data[0])
            break
        case 'onKeyNames':
            $scope.keyNames = inMsg.data[0]
            $scope.$apply()
            break
        case 'onStatus':
            console.info(inMsg.data[0])
            break
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    msg.subscribe('getKeyNames')
    msg.send('getKeyNames')
    msg.subscribe(this)
}
])

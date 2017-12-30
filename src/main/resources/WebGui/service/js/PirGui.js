angular.module('mrlapp.service.PirGui', []).controller('PirGuiCtrl', ['$scope', '$log', 'mrl', '$timeout', function($scope, $log, mrl, $timeout) {
    $log.info('PirGuiCtrl');
    var _self = this;
    var msg = this.msg;
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service;
    }
    ;
    
    _self.updateState($scope.service);
    // init scope variables
    $scope.pulseData = '';
    this.onMsg = function(inMsg) {
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(inMsg.data[0]);
            break;
        case 'onSense':
            $scope.pulseData = inMsg.data[0];
            break;
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
            break;
        }
    }
    ;

    msg.subscribe('publishSense');
    msg.subscribe(this);
}
]);

angular.module('mrlapp.service.PidGui', [])
.controller('PidGuiCtrl', ['$log', '$scope', 'mrl', function($log, $scope, mrl) {
    $log.info('PidGuiCtrl');
    var _self = this;
    var msg = this.msg;
    
    // init
    //$scope.controller = '';
    // $scope.controllerName = '';
    // $scope.data = [];  
       $scope.testKey = 'x';  
    
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service;
        $scope.piddata = service.data;
    }
    ;
    
    _self.updateState($scope.service);
    
    this.onMsg = function(inMsg) {
        var data = inMsg.data[0];
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data);
            $scope.$apply();
            break;
        default:
            $log.info("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method);
            break;
        }
        ;
    
    }
    ;

    msg.subscribe(this);
}
]);

angular.module('mrlapp.service.AgentGui', [])
.controller('AgentGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    $log.info('AgentGuiCtrl');
    var _self = this;
    var msg = this.msg;
    
    $scope.tsDiff = function(ts) {
        var now = new Date();
        var difference = now.getTime() - ts;
        
        var daysDifference = Math.floor(difference / 1000 / 60 / 60 / 24);
        difference -= daysDifference * 1000 * 60 * 60 * 24
        
        var hoursDifference = Math.floor(difference / 1000 / 60 / 60);
        difference -= hoursDifference * 1000 * 60 * 60
        
        var minutesDifference = Math.floor(difference / 1000 / 60);
        difference -= minutesDifference * 1000 * 60
        
        var secondsDifference = Math.floor(difference / 1000);
        
        var ret = (daysDifference + ' days ' + hoursDifference + ' hours ' + minutesDifference + ' minutes ' + secondsDifference + ' seconds ');
        return ret;
    }
    
    
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service;
    }
    ;
    _self.updateState($scope.service);
    
    // init scope variables
    $scope.pulseData = '';
    
    this.onMsg = function(inMsg) {
        var data = inMsg.data[0];
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data);
            $scope.$apply();
            break;
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
            break;
        }
    }
    ;
    
    //mrl.subscribe($scope.service.name, 'pulse');
    msg.subscribe('pulse');
    msg.subscribe(this);
}
]);

angular.module('mrlapp.service.RemoteAdapterGui', [])
.controller('RemoteAdapterGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    $log.info('RemoteAdapterGuiCtrl');
    // get latest copy of a services
    $scope.service = mrl.getService($scope.service.name);
    
    $scope.connectText = "connect";
    $scope.scanText = "scan";
    
    this.onMsg = function(msg) {
        
        switch (msg.method) {
        case 'onPulse':
            $scope.pulseData = msg.data[0];
            $scope.$apply();
            break;
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + msg.method);
            break;
        }
    }
    ;
    
    $scope.connect = function(uri) {
        mrl.sendTo($scope.service.name, "connect", uri);
    }
    ;
    
    $scope.scan = function() {
        mrl.sendTo($scope.service.name, "scan");
    }
    ;
    
    mrl.subscribe($scope.service.name, 'pulse');
    mrl.subscribe($scope.service.name, 'remoteadapterStarted');
    mrl.subscribe($scope.service.name, 'remoteadapterStopped');
    
//    $scope.panel.initDone();
}
]);

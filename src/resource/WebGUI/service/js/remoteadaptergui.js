angular.module('mrlapp.service.remoteadaptergui', [])
.controller('RemoteAdapterGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
        $log.info('RemoteAdapterGuiCtrl');
        // get latest copy of a services
        $scope.service = mrl.getService($scope.service.name);
     
        // load data bindings for this type
        $scope.pulseData = '';

        $scope.connectText = "connect";

        $scope.panel.onMsg = function(msg) {
           
            switch (msg.method) {
                case 'onPulse':
                    $scope.pulseData = msg.data[0];
                    $scope.$apply();
                    break;     
                default:
                    $log.error("ERROR - unhandled method " + $scope.name + " " + msg.method);
                    break;
            }
        };
        
        $scope.toggle = function(label, interval) {
            if (label == "Start") {
                mrl.sendTo($scope.service.name, "setInterval", interval);
                mrl.sendTo($scope.service.name, "startRemoteAdapter");
            } else {
                mrl.sendTo($scope.service.name, "stopRemoteAdapter");
            }
        };
        
        mrl.subscribe($scope.service.name, 'pulse');
        mrl.subscribe($scope.service.name, 'remoteadapterStarted');
        mrl.subscribe($scope.service.name, 'remoteadapterStopped');
        
        $scope.panel.initDone();
    }]);

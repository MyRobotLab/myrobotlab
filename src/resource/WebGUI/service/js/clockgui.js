angular.module('mrlapp.service.clockgui', [])
.controller('ClockGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
        console.log('ClockGuiCtrl');
        $scope.service = mrl.getService($scope.service.name);
        
        $scope.interval = $scope.service.interval;
        if ($scope.service.isClockRunning == true){
                $scope.label = "Stop";
        } else {
                $scope.label = "Start";
        }

        $scope.gui.setPanelCount(1);
        // load data bindings for this type
        $scope.pulseData = '';

        $scope.service.onMsg = function(msg) {
           
            switch (msg.method) {
                case 'onPulse':
                    $scope.pulseData = msg.data[0];
                    $scope.$apply();
                    break;
                case 'onClockStarted':
                    $scope.label = "Stop";
                    $scope.$apply();
                    break;
                case 'onClockStopped':
                    $scope.label = "Start";
                    $scope.$apply();
                    break;
                default:
                    console.log("ERROR - unhandled method " + msg.method);
                    break;
            }
        };
        
        $scope.toggle = function() {
            if ($scope.label == "Start") {
                mrl.sendTo($scope.service.name, "startClock");
            } else {
                mrl.sendTo($scope.service.name, "stopClock");
            }
        };
        
        $scope.changeInterval = function() {
            mrl.sendTo($scope.service.name, "setInterval", $scope.interval);
        }
        
        mrl.subscribe($scope.service.name, 'pulse');
        mrl.subscribe($scope.service.name, 'clockStarted');
        mrl.subscribe($scope.service.name, 'clockStopped');
        
        $scope.gui.initDone();
    }]);

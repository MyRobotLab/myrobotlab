angular.module('mrlapp.service.clockgui', [])
.controller('ClockGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
        console.log('ClockGuiCtrl');
        $scope.service = mrl.getService($scope.service.name);
        
        $scope.interval = $scope.service.interval;
        if ($scope.service.isClockRunning == true){
                $scope.label = "Stop";
                $scope.intervalDisabled = true;
        } else {
                $scope.label = "Start";
                $scope.intervalDisabled = false;
        }

        // load data bindings for this type
        $scope.pulseData = '';


        $scope.gui.onMsg = function(msg) {
           
            switch (msg.method) {
                case 'onPulse':
                    $scope.pulseData = msg.data[0];
                    $scope.$apply();
                    break;
                case 'onClockStarted':
                    $scope.label = "Stop";
                    $scope.intervalDisabled = true;
                    $scope.$apply();
                    break;
                case 'onClockStopped':
                    $scope.label = "Start";
                    $scope.intervalDisabled = false;
                    $scope.$apply();
                    break;
                default:
                    console.log("ERROR - unhandled method " + $scope.name + " " + msg.method);
                    break;
            }
        };
        
        $scope.toggle = function() {
            if ($scope.label == "Start") {
                mrl.sendTo($scope.service.name, "setInterval", $scope.interval);
                mrl.sendTo($scope.service.name, "startClock");
            } else {
                mrl.sendTo($scope.service.name, "stopClock");
            }
        };
        
        mrl.subscribe($scope.service.name, 'pulse');
        mrl.subscribe($scope.service.name, 'clockStarted');
        mrl.subscribe($scope.service.name, 'clockStopped');
        
        $scope.gui.initDone();
    }]);

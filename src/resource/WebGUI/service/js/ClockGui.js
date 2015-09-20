angular.module('mrlapp.service.ClockGui', [])
.controller('ClockGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    $log.info('ClockGuiCtrl');
    var _self = this;
    $scope.pulseData = '';
    
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        mrl.updateState(service);
        // $scope.service = service; // probably should not do this - propegates stale data
        $scope.interval = service.interval;
        if (service.isClockRunning == true) {
            $scope.label = "Stop";
            $scope.intervalDisabled = true;
        } else {
            $scope.label = "Start";
            $scope.intervalDisabled = false;
        }
    
    }
    ;

    // get latest copy of a services - it will be stale
    _self.updateState(mrl.getService($scope.service.name));
    
    $scope.panel.onMsg = function(msg) {
        
        switch (msg.method) {
        case 'onState':
            _self.updateState(msg.data[0]);           
            $scope.$apply();
            break;
        case 'onPulse':
            $scope.pulseData = msg.data[0];
            $scope.$apply();
            break;
        case 'onClockStarted':
        /*
            $scope.label = "Stop";
            $scope.intervalDisabled = true;
            $scope.$apply();
            */
            break;
        case 'onClockStopped':
        /*
            $scope.label = "Start";
            $scope.intervalDisabled = false;
            $scope.$apply();
            */
            break;
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + msg.method);
            break;
        }
    }
    ;
    
    $scope.broadcastState = function() {
        mrl.sendTo($scope.service.name, "broadcastState");
    }
    
    $scope.toggle = function(label, interval) {
        if (label == "Start") {
            mrl.sendTo($scope.service.name, "setInterval", interval);
            mrl.sendTo($scope.service.name, "startClock");
        } else {
            mrl.sendTo($scope.service.name, "stopClock");
        }
    }
    ;
    
    mrl.subscribe($scope.service.name, 'pulse');
    mrl.subscribe($scope.service.name, 'clockStarted');
    mrl.subscribe($scope.service.name, 'clockStopped');
    mrl.subscribe($scope.service.name, 'publishState');
    
    
    
    // mrl.sendTo($scope.service.name, "broadcastState");
    $scope.panel.initDone();
}
]);

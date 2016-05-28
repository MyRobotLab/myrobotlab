angular.module('mrlapp.service.MyoThalmicGui', [])
.controller('MyoThalmicGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    $log.info('MyoThalmicGuiCtrl');
    var _self = this;
    var msg = this.msg;
    
    // init vars from current service state
    $scope.batteryLevel = $scope.service.batteryLevel;
    $scope.locked = $scope.service.locked;

    // load data bindings for this type
    if ($scope.service.isConnected) {
        $scope.connectText = "disconnect";
    } else {
        $scope.connectText = "connect";
    }
    
    this.onMsg = function(inMsg) {
        
        switch (inMsg.method) {
        case 'onPose':
            $scope.pose = inMsg.data[0].type;
            $scope.$apply();
            break;
        case 'onMyoData':
            $scope.myoData = inMsg.data[0];
            $scope.pose = $scope.myoData.currentPose;
            // FIXME - it should be SENT THIS WAY - 
            // FIXME - DO FILTERING AS CLOSE TO THE SOURCE AS POSSIBLE !!!!
            // FIXME - ONLY SEND DATA IF THE CHANGE IS RELEVANT !!!
            $scope.$apply();
            break;
        case 'onArmSync':
            $scope.armSync = inMsg.data[0];
            $scope.$apply();
            break;
        case 'onLocked':
            $scope.locked = inMsg.data[0];
            $scope.$apply();
            break;
        case 'onBatteryLevel':
            $scope.batteryLevel = inMsg.data[0];
            $scope.$apply();
            break;
        case 'onState':
            $scope.service = inMsg.data[0];
            if ($scope.service.isConnected) {
                $scope.connectText = "disconnect";
            } else {
                $scope.connectText = "connect";
            }
            $scope.$apply();
            break;
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
            break;
        }
    }
    ;
    
    $scope.connect = function() {
        if (!$scope.service.isConnected) {
            mrl.sendTo($scope.service.name, "connect");
        } else {
            mrl.sendTo($scope.service.name, "disconnect");
        }
    }
    ;
    
    msg.subscribe('publishState');
    msg.subscribe('publishMyoData');
    msg.subscribe('publishPose');
    msg.subscribe('publishArmSync');
    msg.subscribe('publishBatteryLevel');
    msg.subscribe('publishLocked');
    msg.subscribe(this);
    
}
]);

angular.module('mrlapp.service.myothalmicgui', [])
.controller('MyoThalmicGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    $log.info('MyoThalmicGuiCtrl');
    // get latest copy of a services
    $scope.service = mrl.getService($scope.service.name);
    
    // load data bindings for this type
    if ($scope.service.isConnected) {
        $scope.connectText = "disconnect";
    } else {
        $scope.connectText = "connect";
    }
    
    $scope.panel.onMsg = function(msg) {
        
        switch (msg.method) {
        case 'onPose':
            $scope.pose = msg.data[0].type;
            $scope.$apply();
            break;
        case 'onMyoData':
            $scope.myoData = msg.data[0];
            // FIXME - it should be SENT THIS WAY - 
            // FIXME - DO FILTERING AS CLOSE TO THE SOURCE AS POSSIBLE !!!!
            // FIXME - ONLY SEND DATA IF THE CHANGE IS RELEVANT !!!
            $scope.$apply();
            break;
        case 'onState':
            $scope.service = msg.data[0];
            if ($scope.service.isConnected) {
                $scope.connectText = "disconnect";
            } else {
                $scope.connectText = "connect";
            }
            $scope.$apply();
            break;
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + msg.method);
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
    
    
    mrl.subscribe($scope.service.name, 'publishState');
    mrl.subscribe($scope.service.name, 'publishMyoData');
    mrl.subscribe($scope.service.name, 'publishPose');
    
    $scope.panel.initDone();
}
]);

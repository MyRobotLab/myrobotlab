angular.module('mrlapp.service.inversekinematics3dgui', [])
.controller('InverseKinematics3DGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
        $log.info('InverseKinematics3D');
        // get latest copy of a services
        $scope.service = mrl.getService($scope.service.name);
        $scope.interval = $scope.service.interval;
       // if ($scope.service.isClockRunning == true){
       //         $scope.label = "Stop";
       //         $scope.intervalDisabled = true;
       // } else {
       //         $scope.label = "Start";
       //         $scope.intervalDisabled = false;
       // }

        // load data bindings for this type
      //  $scope.pulseData = '';

        $scope.positions = 'No positions Yet.';
        $scope.angles = 'No angles Yet';
        
        $scope.panel.onMsg = function(msg) {
            $log.info("On Message IK3D!");
            $log.info(msg);
            switch (msg.method) {
                case 'onJointPositions':
                	$log.info("On Joint Positions..");
                    $scope.positions = msg.data[0];
                    $scope.$apply();
                    break;
                case 'onJointAngles':
                	$log.info("On Joint Angles..");
                	$scope.angles = msg.data[0];
                	$scope.$apply();
                	break;
                default:
                    $log.error("ERROR - unhandled method " + $scope.name + " " + msg.method);
                    break;
            };
        };
        
        $scope.moveTo = function(x,y,z) {
    	  // Invoke the moveTo..
          mrl.sendTo($scope.service.name, "moveTo", x, y, z);
        };
        mrl.subscribe($scope.service.name, 'publishJointPositions');
        mrl.subscribe($scope.service.name, 'publishJointAngles');
        $scope.panel.initDone();
    }]);

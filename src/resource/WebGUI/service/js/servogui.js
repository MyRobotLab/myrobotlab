angular.module('mrlapp.service.servogui', [])
.controller('ServoGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
        console.log('ServoGuiCtrl');
        
        // Get a fresh copy!
        $scope.service = mrl.getService($scope.service.name);
        $scope.controller = '';
        $scope.pin = '';
        $scope.min = 0;
        $scope.max = 180;
        $scope.angle = 0;
        $scope.isAttached = $scope.service.isAttached;
        // get latest copy of a services
        
        // get and initalize current state of servo
        $scope.attachButtonLabel = "Attach";
        $scope.status = "No Status";
        $scope.$apply();
        
        $scope.panel.onMsg = function(msg) {
        	console.log("SERVO MSG: " + msg);
            switch (msg.method) {
                case 'onState':
                    $scope.status = msg.data[0];
                    $scope.isAttached = $scope.status.isAttached;
                    $scope.angle = $scope.status.angle;
                    $scope.min = $scope.status.outputYMin;
                    $scope.max = $scope.status.outputYMax;
                    if ($scope.isAttached === true) {
                    	$scope.attachButtonLabel = "Detach";
                    } else {
                    	$scope.attachButtonLabel = "Attach";
                    };
                    $scope.$apply();
                    break;
                case 'onServoEvent':
                    $scope.status = msg.data[0];
                    $scope.$apply();
                    break;
                case 'onStatus':
                    $scope.status = msg.data[0];
                    $scope.$apply();
                    break;
                case 'addListener':
                	console.log("Add listener called");
                	$scope.status = msg.data[0];
                    $scope.$apply();
                    break;
                default:
                	$scope.status = msg.data[0];
                    $scope.$apply();
                    console.log("ERROR - unhandled method " + $scope.name + " Method " + msg.method);
                    break;
            };
        };
        
        $scope.attachDetach = function(controller,pin) {
        	if ($scope.status.isAttached === true) {
        		console.log("Detach Servo");
            	mrl.sendTo($scope.service.name, "detach");
            	$scope.attachButtonLabel = "Detach";
            	$scope.$apply();
        	} else {
        		console.log("Attach Servo");
            	mrl.sendTo($scope.service.name, "attach", controller, pin);
            	$scope.attachButtonLabel = "Attach";
            	$scope.$apply();
        	};
        };
        
        $scope.moveTo = function(angle) {
        	console.log("Move TO " + angle);
        	mrl.sendTo($scope.service.name, "moveTo", angle);
        };
        
        $scope.updateLimits = function(min,max) {
        	console.log("Update Limits");
        	mrl.sendTo($scope.service.name, "setMinMax", min, max);
        };
        
        // mrl.subscribe($scope.service.name, 'publishServoEvent');  ??
        $scope.panel.initDone();
    }]);

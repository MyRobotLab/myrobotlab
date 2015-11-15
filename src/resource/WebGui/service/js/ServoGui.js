angular.module('mrlapp.service.ServoGui', [])
.controller('ServoGuiCtrl', ['$log','$scope', 'mrl', function($log, $scope, mrl) {
        $log.info('ServoGuiCtrl');

        var _self = this;
        var msg = this.msg;

        // GOOD TEMPLATE TO FOLLOW
        this.updateState = function (service) {
            $scope.service = service;
        };
        _self.updateState($scope.service);
        
        // Get a fresh copy!
        // $scope.service = mrl.getService($scope.service.name);
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
        // $scope.$apply();
        $log.info('ServoGuiCtrl part 3 ');
        this.onMsg = function(msg) {
        	$log.info("SERVO MSG: " + msg);
        	
            switch (msg.method) {
                case 'onState':
                    _self.updateState(inMsg.data[0]);
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
                	$log.info("Add listener called");
                	$scope.status = msg.data[0];
                    $scope.$apply();
                    break;
                default:
                	$scope.status = msg.data[0];
                    $scope.$apply();
                    $log.info("ERROR - unhandled method " + $scope.name + " Method " + msg.method);
                    break;
            };
            
        };
        
        $scope.attachDetach = function(controller,pin) {
        	if ($scope.status.isAttached === true) {
        		$log.info("Detach Servo");
            	mrl.sendTo($scope.service.name, "detach");
            	$scope.attachButtonLabel = "Detach";
            	$scope.$apply();
        	} else {
        		$log.info("Attach Servo");
            	mrl.sendTo($scope.service.name, "attach", controller, pin);
            	$scope.attachButtonLabel = "Attach";
            	$scope.$apply();
        	};
        };
        
        $scope.moveTo = function(angle) {
        	$log.info("Move TO " + angle);
        	mrl.sendTo($scope.service.name, "moveTo", angle);
        };
        
        $scope.updateLimits = function(min,max) {
        	$log.info("Update Limits");
        	mrl.sendTo($scope.service.name, "setMinMax", min, max);
        };
        
        // mrl.subscribe($scope.service.name, 'publishServoEvent');  ??
        msg.subscribe("servoEvent");
        msg.subscribe(this);
       
        
    }]);

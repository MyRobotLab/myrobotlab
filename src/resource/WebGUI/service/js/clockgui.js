angular.module('mrlapp.service.clockgui', [])
        .controller('ClockGuiCtrl', ['$scope', 'mrl', function ($scope, mrl) {
                console.log('ClockGuiCtrl');
                $scope.label = "Start";
                $scope.interval = 1000;
                $scope.gui.setPanelCount(1);
                // load data bindings for this type
                $scope.service.pulseData = '';
                $scope.service.onMsg = function (msg) {
                    console.log("Clock Msg ! - ");
                    if (msg.method == "onPulse") {
                        var pulseData = msg.data[0];
                        $scope.service.pulseData = pulseData;
                        console.log('pulseData', $scope.service.pulseData);
                        $scope.$apply();
                    };
                    if (msg.method == "onClockStarted") {
                    	// track the clock state
                    	$scope.service.isClockRunning = true;
                    	$scope.$apply();
                    };

                    if (msg.method == "onClockStopped") {
                    	// track the clock state
                    	$scope.service.isClockRunning = false;
                    	$scope.$apply();
                    };

                };
                $scope.toggle = function() {
                	// just to make sure we have the right scope?
                	$scope.service = mrl.getService($scope.service.name);
                	if ($scope.service.isClockRunning == false) {
                		// start the clock and update the label
                		mrl.sendTo($scope.service.name, "startClock");
                		$scope.label = "Stop";
                	} else {
                		// stop the clock and update the label
                		mrl.sendTo($scope.service.name, "stopClock");
                		$scope.label = "Start";
                	};
                };
                
                $scope.changeInterval = function() {
                	mrl.sendTo($scope.service.name, "setInterval", $scope.interval);
                }
                
                mrl.subscribe($scope.service.name, 'pulse');
                mrl.subscribe($scope.service.name, 'clockStarted');
                mrl.subscribe($scope.service.name, 'clockStopped');
                $scope.gui.initDone();
            }]);
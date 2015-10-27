angular.module('mrlapp.service.ClockGui', [])
        .controller('ClockGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                $log.info('ClockGuiCtrl');
                var _self = this;
                var msg = this.msg;

                // init scope variables
                $scope.pulseData = '';

                // GOOD TEMPLATE TO FOLLOW
                this.updateState = function (service) {
                    // FIXME let the framework 
//        mrl.updateState(service);
                    $scope.service = service;
//                    $scope.interval = service.interval;
//                    if (service.isClockRunning == true) {
//                        $scope.label = "Stop";
//                        $scope.intervalDisabled = true;
//                    } else {
//                        $scope.label = "Start";
//                        $scope.intervalDisabled = false;
//                    }
                };

                _self.updateState($scope.service);

                this.onMsg = function (msg) {
                    switch (msg.method) {
                        case 'onState':
                            _self.updateState(msg.data[0]);
                            $scope.$apply();
                            break;
                        case 'onPulse':
                            $scope.pulseData = msg.data[0];
                            $scope.$apply();
                            break;
                        default:
                            $log.error("ERROR - unhandled method " + $scope.name + " " + msg.method);
                            break;
                    }
                };

                mrl.subscribe($scope.service.name, 'pulse');
                msg.subscribe(this);
            }
        ]);

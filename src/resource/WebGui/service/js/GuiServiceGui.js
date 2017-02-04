angular.module('mrlapp.service.GUIServiceGui', [])
        .controller('GUIServiceGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                $log.info('GUIServiceGuiCtrl');
                var _self = this;
                var msg = this.msg;

                this.updateState = function (service) {
                    $scope.service = service;
                };
                _self.updateState($scope.service);

                this.onMsg = function (inMsg) {
                    switch (inMsg.method) {
//                        case 'onPulse':
//                            $scope.pulseData = inMsg.data[0];
//                            $scope.$apply();
//                            break;
//                        case 'onClockStarted':
//                            $scope.label = "Stop";
//                            $scope.$apply();
//                            break;
//                        case 'onClockStopped':
//                            $scope.label = "Start";
//                            $scope.$apply();
//                            break;
                        default:
                            $log.error("ERROR - unhandled method " + inMsg.method);
                            break;
                    }
                };

                //you can subscribe to methods
//                mrl.subscribe($scope.service.name, 'pulse');
//                mrl.subscribe($scope.service.name, 'clockStarted');
//                mrl.subscribe($scope.service.name, 'clockStopped');

                msg.subscribe(this);
            }]);

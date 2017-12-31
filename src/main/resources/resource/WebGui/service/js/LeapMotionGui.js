angular.module('mrlapp.service.LeapMotionGui', [])
        .controller('LeapMotionGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                $log.info('LeapMotionGuiCtrl');
                var _self = this;
                var msg = this.msg;

                // From template.
                this.updateState = function (service) {
                    $scope.service = service;
                };
                _self.updateState($scope.service);

                // leap data is pretty much everything.
                $scope.leapData = '';

                this.onMsg = function (inMsg) {
                    switch (inMsg.method) {
                        case 'onState':
                            _self.updateState(inMsg.data[0]);
                            $scope.$apply();
                            break;
                        case 'onLeapData':
                            $scope.leapData = inMsg.data[0];
                            $scope.$apply();
                            break;
                        default:
                            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
                            break;
                    }
                };

                msg.subscribe('publishLeapData');
                //msg.subscribe('publishPoints');
                msg.subscribe(this);
            }
        ]);

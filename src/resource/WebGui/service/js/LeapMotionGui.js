angular.module('mrlapp.service.LeapMotionGui', [])
        .controller('LeapMotionGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                $log.info('LeapMotionGuiCtrl');
                var _self = this;
                var msg = this.msg;

                // GOOD TEMPLATE TO FOLLOW
                this.updateState = function (service) {
                    $scope.service = service;
                };
                _self.updateState($scope.service);

                // init scope variables
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
                //msg.subscribt('publishPoints');
                msg.subscribe(this);
            }
        ]);

angular.module('mrlapp.service.OculusDIYGui', [])
        .controller('OculusDIYGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                $log.info('AndroidGuiCtrl');
                var _self = this;
                var msg = this.msg;

                // init scope variables
                $scope.oculus = { roll:0, pitch:0, yaw:0};
                
                // GOOD TEMPLATE TO FOLLOW
                this.updateState = function (service) {
                    $scope.service = service;
                };

                _self.updateState($scope.service);

                this.onMsg = function (inMsg) {
                    switch (inMsg.method) {
                        case 'onState':
                            _self.updateState(inMsg.data[0]);
                            $scope.$apply();
                            break;
                        case 'onOculusData':
                            $scope.oculus = inMsg.data[0];
                            $scope.$apply();
                            break;
                        default:
                            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
                            break;
                    }
                };

                //mrl.subscribe($scope.service.name, 'pulse');
                msg.subscribe('publishOculusData');
                msg.subscribe(this);
            }
        ]);

angular.module('mrlapp.service.AndroidGui', [])
        .controller('AndroidGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                $log.info('AndroidGuiCtrl');
                var _self = this;
                var msg = this.msg;

                // init scope variables
                $scope.motion = { x:0, y:0, z:0};
                
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
                        case 'onMotion':
                            $scope.motion = inMsg.data[0];
                            $scope.$apply();
                            break;
                        default:
                            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
                            break;
                    }
                };

                //mrl.subscribe($scope.service.name, 'pulse');
                msg.subscribe('publishMotion');
                msg.subscribe(this);
            }
        ]);

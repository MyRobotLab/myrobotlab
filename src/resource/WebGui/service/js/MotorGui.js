angular.module('mrlapp.service.MotorGui', [])
        .controller('MotorGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                $log.info('MotorGuiCtrl');
                var _self = this;
                var msg = this.msg;

                // init scope variables
                $scope.controller = '';
                $scope.controllers = [];

                // GOOD TEMPLATE TO FOLLOW
                this.updateState = function (service) {
                    $scope.service = service;
                };

                _self.updateState($scope.service);

                this.onMsg = function (msg) {
                    switch (msg.method) {
                        case 'onState':
                            _self.updateState(msg.data[0]);
                            $scope.$apply();
                            break;
                        case 'onServiceNamesFromInterface':
                            $scope.controllers = msg.data[0];
                            $scope.$apply();
                            break;
                        default:
                            $log.error("ERROR - unhandled method " + $scope.name + " " + msg.method);
                            break;
                    }
                };

                var runtimeName = mrl.getRuntime().name;
                // subscribe from Runtime --> WebGui (gateway)
                mrl.subscribe(runtimeName, 'getServiceNamesFromInterface');
                // subscribe callback from nameMethodCallbackMap --> onMsg !!!! FIXME - since this is to a "different" service and
                // not self - it can be overwritten by another service subscribing to the same service.method  :(
                mrl.subscribeToServiceMethod(this.onMsg, runtimeName, 'getServiceNamesFromInterface');
                msg.subscribe(this);
                mrl.sendTo(runtimeName, 'getServiceNamesFromInterface', 'org.myrobotlab.service.interfaces.MotorController');
            }
        ]);

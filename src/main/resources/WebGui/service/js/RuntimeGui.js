angular.module('mrlapp.service.RuntimeGui', [])
        .controller('RuntimeGuiCtrl', ['$scope', '$log', 'mrl', '$timeout', function ($scope, $log, mrl, $timeout) {
                $log.info('RuntimeGuiCtrl');
                var _self = this;
                var msg = this.msg;

                this.updateState = function (service) {
                    $scope.service = service;
                };

                _self.updateState(mrl.getService($scope.service.name));

                $scope.platform = $scope.service.platform;

                this.onMsg = function (inMsg) {
                    switch (inMsg.method) {
                        case 'onState':
                            $timeout(function () {
                                _self.updateState(inMsg.data[0]);
                            });
                            break;
                         case 'onReleased':{
                                $log.info("runtime - onRelease" +  inMsg.data[0] );
                           break;
                         }
                        default:
                            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
                            break;
                    }
                };

//                $scope.serviceData = $scope.service.serviceData.serviceTypes;           

                $scope.possibleServices = mrl.getPossibleServices();

                msg.subscribe(this);
            }]);

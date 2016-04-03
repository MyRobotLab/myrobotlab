angular.module('mrlapp.service.RuntimeGui', [])
        .controller('RuntimeGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                $log.info('RuntimeGuiCtrl');
                var _self = this;
                var msg = this.msg;

                this.updateState = function (service) {
                    $scope.service = service;
                };

                _self.updateState(mrl.getService($scope.service.name));

                var platform = $scope.service.platform;
                // make the platform string
                $scope.platform = platform.arch + "." + platform.bitness + "." + platform.os;
                $scope.version = platform.mrlVersion;

                this.onMsg = function (inMsg) {

                    switch (inMsg.method) {
                        case 'onState':
                            _self.updateState(inMsg.data[0]);
                            $scope.$apply();
                            break;
                        default:
                            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
                            break;
                    }
                };

//                $scope.serviceData = $scope.service.serviceData.serviceTypes;           

                $scope.possibleServices = mrl.getPossibleServices();

                msg.subscribe(this);
            }]);

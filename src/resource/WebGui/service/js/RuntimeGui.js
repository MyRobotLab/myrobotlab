angular.module('mrlapp.service.RuntimeGui', [])
        .controller('RuntimeGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                $log.info('RuntimeGuiCtrl');
                var _self = this;
                var msg = this.msg;

                $scope.service = mrl.getService($scope.service.name);
                var platform = $scope.service.platform;
                // make the platform string
                $scope.platform = platform.arch + "." + platform.bitness + "." + platform.os;
                $scope.version = platform.mrlVersion;

                this.onMsg = function (msg) {

                    switch (msg.method) {
                        case 'onPulse':
                            $scope.pulseData = msg.data[0];
                            $scope.$apply();
                            break;
                        case 'onClockStarted':
                            $scope.label = "Stop";
                            $scope.intervalDisabled = true;
                            $scope.$apply();
                            break;
                        case 'onClockStopped':
                            $scope.label = "Start";
                            $scope.intervalDisabled = false;
                            $scope.$apply();
                            break;
                        default:
                            $log.error("ERROR - unhandled method " + $scope.name + " " + msg.method);
                            break;
                    }
                };

                $scope.serviceData = $scope.service.serviceData.serviceTypes;
                //mrl.subscribe($scope.service.name, 'pulse');            

                $scope.newType = undefined;
                $scope.possibleServices = mrl.getPossibleServices();

                $scope.start = function (newName, newTypeModel) {
                    mrl.sendTo($scope.service.name, "start", newName, newTypeModel.name);
                }

                $scope.install = function () {
                    mrl.sendTo($scope.service.name, "install");
                }

            }]);

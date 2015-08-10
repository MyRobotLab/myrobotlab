angular.module('mrlapp.service.runtimegui', [])

        .controller('RuntimeGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                var _self = this;
                $log.info('RuntimeGuiCtrl');

                this.init = function () {
                    $scope.data.service = this.getService();
                    var platform = $scope.data.service.platform;
                    // make the platform string
                    $scope.data.platform = platform.arch + "." + platform.bitness + "." + platform.os;
                    $scope.data.version = platform.mrlVersion;

                    this.onMsg = function (msg) {
                        switch (msg.method) {
                            case 'onPulse':
                                $scope.data.pulseData = msg.data[0];
                                $scope.$apply();
                                break;
                            case 'onClockStarted':
                                $scope.data.label = "Stop";
                                $scope.data.intervalDisabled = true;
                                $scope.$apply();
                                break;
                            case 'onClockStopped':
                                $scope.data.label = "Start";
                                $scope.data.intervalDisabled = false;
                                $scope.$apply();
                                break;
                            default:
                                $log.error("ERROR - unhandled method " + $scope.data.service.name + " " + msg.method);
                                break;
                        }
                    };

                    $scope.data.localServiceData = $scope.data.service.repo.localServiceData.serviceTypes;
                    //this.subscribe('pulse');            

                    $scope.data.newType = undefined;
                    $scope.data.possibleServices = mrl.getPossibleServices();

                    $scope.data.start = function (newName, newTypeModel) {
                        _self.send("start", newName, newTypeModel.name);
                    };
                };

                $scope.cb.notifycontrollerisready(this);
            }]);

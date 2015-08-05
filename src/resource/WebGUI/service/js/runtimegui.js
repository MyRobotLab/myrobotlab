angular.module('mrlapp.service.runtimegui', [])

        .controller('RuntimeGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                var _self = this;
                $log.info('RuntimeGuiCtrl');

                this.init = function () {

                    var getSimpleName = function (fullname) {
                        return (fullname.substring(fullname.lastIndexOf(".") + 1));
                    };

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
                    $scope.data.possibleServices = [];

                    // pump model data from repo
                    for (var property in $scope.data.service.repo.localServiceData.serviceTypes) {
                        if ($scope.data.service.repo.localServiceData.serviceTypes.hasOwnProperty(property)) {
                            var serviceType = $scope.data.service.repo.localServiceData.serviceTypes[property];
                            var model = {};
                            model.name = getSimpleName(property);
                            model.img = model.name + '.png';
                            model.alt = serviceType.description;
                            $scope.data.possibleServices.push(model);
                        }
                    }

                    $scope.data.possibleServices = $scope.data.possibleServices; //?

                    $scope.data.start = function (newName, newTypeModel) {
                        _self.send("start", newName, newTypeModel.name);
                    };
                };

                $scope.cb.notifycontrollerisready(this);
            }]);

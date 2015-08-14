angular.module('mrlapp.service.webguigui', [])
        .controller('WebGUIGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                var _self = this;
                $log.info('WebGUIGuiCtrl');

                this.init = function () {
                    // get fresh copy
                    $scope.data.service = this.getService();

                    this.onMsg = function (msg) {
                        switch (msg.method) {
                            case 'onPulse':
                                $scope.data.pulseData = msg.data[0];
                                $scope.$apply();
                                break;
                            case 'onClockStarted':
                                $scope.data.label = "Stop";
                                $scope.$apply();
                                break;
                            case 'onClockStopped':
                                $scope.data.label = "Start";
                                $scope.$apply();
                                break;
                            default:
                                $log.error("ERROR - unhandled method " + msg.method);
                                break;
                        }
                    };
                };

                $scope.cb.notifycontrollerisready(this);
            }]);

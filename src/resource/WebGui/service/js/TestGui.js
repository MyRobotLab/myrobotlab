angular.module('mrlapp.service.TestGui', [])
        .controller('TestGuiCtrl', ['$scope', '$log', 'mrl', '$timeout', function ($scope, $log, mrl, $timeout) {
                $log.info('TestGuiCtrl');
                var _self = this;
                var msg = this.msg;

                //$scope.serviceTypes = {};
                $scope.matrix = {};

                // GOOD TEMPLATE TO FOLLOW
                this.updateState = function (service) {
                    $scope.service = service;
                    $scope.matrix  = service.matrix;
                };

                _self.updateState($scope.service);

                // init scope variables
                $scope.pulseData = '';

                this.onMsg = function (inMsg) {
                    switch (inMsg.method) {
                        case 'onState':
                            $timeout(function () {
                                _self.updateState(inMsg.data[0]);
                            });
                            break;
                        case 'onPulse':
                            $timeout(function () {
                                $scope.pulseData = inMsg.data[0];
                            });
                            break;
                        default:
                            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
                            break;
                    }
                };

                $scope.toNumber = function (val) {
                    if (angular.isUndefined(val)) {
                        var val = '0';
                    }
                    val = val.toString();
                    var clean = val.replace(/[^0-9\.]/g, '').replace('.', '').replace(' ', '');
                    if (clean == '') {
                        clean = '0';
                    }
                    return clean;
                };

                msg.subscribe('pulse');
                msg.subscribe(this);
            }
        ])
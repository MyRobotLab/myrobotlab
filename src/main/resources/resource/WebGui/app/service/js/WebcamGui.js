angular.module('mrlapp.service.WebcamGui', [])
        .controller('WebcamGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                $log.info('WebcamGuiCtrl');
                var _self = this;
                var msg = this.msg;

                // GOOD TEMPLATE TO FOLLOW
                this.updateState = function (service) {
                    $scope.service = service;
                    $scope.port = service.port;
                };
               

                // init scope variables
                $scope.pulseData = '';

                this.onMsg = function (inMsg) {
                    switch (inMsg.method) {
                        case 'onState':
                            _self.updateState(inMsg.data[0]);
                            $scope.$apply();
                            break;service
                        default:
                            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
                            break;
                    }
                };

                //mrl.subscribe($scope.service.name, 'pulse');
                msg.subscribe('publishShowAll');
                // msg.subscribe('publishHideAll'); FIXME ? not symmetric
                msg.subscribe('publishHide');
                msg.subscribe('publishShow');
                msg.subscribe('publishSet');
                msg.subscribe(this);
            }
        ]);

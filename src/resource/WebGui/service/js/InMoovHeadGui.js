angular.module('mrlapp.service.InMoovHeadGui', [])
        .controller('InMoovHeadGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                $log.info('InMoovHeadGuiCtrl');
                var _self = this;
                var msg = this.msg;
                // init scope variables

                //$scope.assimpModelUrl = "service/InMoovHead/cube.json";
                //$scope.assimpModelUrl = "service/InMoovHead/head1.json";
                //$scope.assimpModelUrl = "service/InMoovHead/interior.3ds.json";
                $scope.assimpModelUrl = "service/InMoovHead/cube2.json";


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
                        case 'onPulse':
                            $scope.pulseData = inMsg.data[0];
                            $scope.$apply();
                            break;
                        default:
                            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
                            break;
                    }
                };

                //mrl.subscribe($scope.service.name, 'pulse');
                // msg.subscribe('pulse');
                msg.subscribe(this);
            }
        ]);

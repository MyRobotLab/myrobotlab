angular.module('mrlapp.service.WebGuiGui', [])

        .controller('WebGuiGuiCtrl', ['$scope', '$log', 'mrl', 'serviceSvc', function ($scope, $log, mrl, serviceSvc) {
                $log.info('WebGuiGuiCtrl');
                var _self = this;
                var msg = this.msg;

                // GOOD TEMPLATE TO FOLLOW
                this.updateState = function (service) {
                    $scope.service = service;
                    $scope.port = service.port;
                };
                
                _self.updateState($scope.service);

                // init scope variables
                $scope.pulseData = '';

                this.onMsg = function (inMsg) {
                    switch (inMsg.method) {
                        case 'onState':
                            _self.updateState(inMsg.data[0]);
                            $scope.$apply();
                            break;service
                        case 'onShowAll':
                            serviceSvc.showAll(inMsg.data[0]);
                            break;                     
                        case 'onShow':
                            serviceSvc.show(inMsg.data[0]);
                            break;
                        case 'onHide':
                            serviceSvc.hide(inMsg.data[0]);
                            break;
                        case 'onSet':
                            serviceSvc.set(inMsg.data[0]);
                            break;
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

angular.module('mrlapp.service.WebcamGui', [])
        .controller('WebcamGuiCtrl', ['$scope', '$log', 'mrl', 'panelSvc', function ($scope, $log, mrl, panelSvc) {
                $log.info('WebcamGuiCtrl');
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
                            panelSvc.showAll(inMsg.data[0]);
                            break;                     
                        case 'onShow':
                            panelSvc.show(inMsg.data[0]);
                            break;
                        case 'onHide':
                            panelSvc.hide(inMsg.data[0]);
                            break;
                        case 'onSet':
                            panelSvc.set(inMsg.data[0]);
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

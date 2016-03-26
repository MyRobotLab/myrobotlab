angular.module('mrlapp.service')
        .controller('serviceCtrl', ['$scope', '$log', '$uibModal', 'mrl', 'serviceSvc', 'noWorkySvc',
            function ($scope, $log, $uibModal, mrl, serviceSvc, noWorkySvc) {
                $log.info('serviceCtrl', $scope.panel.name);

//                var isUndefinedOrNull = function (val) {
//                    return angular.isUndefined(val) || val === null;
//                };
                
                $scope.release = function () {
                    //TODO - important - send message to release service
                };
                
                $scope.noworky = function () {
                    noWorkySvc.openNoWorkyModal($scope.panel.name);
                };
                
                $scope.updateServiceData = function () {
                    //get an updated / fresh servicedata & convert it to json
                    var servicedata = mrl.getService($scope.panel.name);
                    $scope.servicedatajson = JSON.stringify(servicedata, null, 2);
                    console.log($scope.servicedatajson);
                };
                $scope.updateServiceData();

                //service-menu-size-change-buttons
                $scope.changesize = function (size) {
                    $log.info("change size", $scope.panel.name, size);
                    if (size == 'min') {
                        $scope.panel.panelsize.oldsize = $scope.panel.panelsize.aktsize;
                        $scope.panel.panelsize.aktsize = size;
                        $scope.panel.notifySizeChanged();
                        serviceSvc.movePanelToList($scope.panel.name, $scope.panel.panelname, 'min');
                    } else if (size == 'unmin') {
                        $scope.panel.panelsize.aktsize = $scope.panel.panelsize.oldsize;
                        $scope.panel.notifySizeChanged();
                        serviceSvc.movePanelToList($scope.panel.name, $scope.panel.panelname, 'main');
                    } else {
                        $scope.panel.panelsize.oldsize = $scope.panel.panelsize.aktsize;
                        $scope.panel.panelsize.aktsize = size;
                        $scope.panel.notifySizeChanged();
                        if ($scope.panel.panelsize.sizes[$scope.panel.panelsize.aktsize].fullscreen) {
                            //launch the service as a modal ('full')
                            var modalInstance = $uibModal.open({
                                animation: true,
                                templateUrl: 'service/servicefulltemplate.html',
                                controller: 'serviceFullCtrl',
                                size: 'lg',
                                scope: $scope
                            });
                            //modal closed -> recover to old size
                            modalInstance.result.then(function () {
                                $scope.panel.panelsize.aktsize = $scope.panel.panelsize.oldsize;
                                $scope.panel.panelsize.oldsize = null;
                                $scope.panel.notifySizeChanged();
                            }, function (e) {
                                $scope.panel.panelsize.aktsize = $scope.panel.panelsize.oldsize;
                                $scope.panel.panelsize.oldsize = null;
                                $scope.panel.notifySizeChanged();
                            });
                        }
                    }
                };
            }]);

angular.module('mrlapp.service')
        .controller('serviceCtrl', ['$scope', '$log', '$modal', 'mrl', 'serviceSvc',
            function ($scope, $log, $modal, mrl, serviceSvc) {
                $log.info('serviceCtrl', $scope.panel.name);

                var isUndefinedOrNull = function (val) {
                    return angular.isUndefined(val) || val === null;
                };

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
                            var modalInstance = $modal.open({
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

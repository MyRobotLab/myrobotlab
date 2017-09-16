angular.module('mrlapp.service')
        .controller('serviceCtrl', ['$scope', '$log', '$uibModal', 'mrl', 'panelSvc', 'noWorkySvc',
            function ($scope, $log, $uibModal, mrl, panelSvc, noWorkySvc) {
                $log.info('serviceCtrl', $scope.panel.name);

//                var isUndefinedOrNull = function (val) {
//                    return angular.isUndefined(val) || val === null;
//                };

                $scope.release = function () {
                    mrl.sendTo(mrl.getRuntime().name, 'release', $scope.panel.name);
                };

                $scope.noworky = function () {
                    noWorkySvc.openNoWorkyModal($scope.panel.name);
                };

                $scope.updateServiceData = function () {
                    //get an updated / fresh servicedata & convert it to json
                    var servicedata = mrl.getService($scope.panel.name);
                    $scope.servicedatajson = JSON.stringify(servicedata, null, 2);
                };
                $scope.updateServiceData();

                //service-menu-size-change-buttons
                $scope.changesize = function (size) {
                    $log.info("change size", $scope.panel.name, size);
                    $scope.panel.size = size;
                    if (size == 'free') {
                        $scope.panel.notifySizeChanged(800);
                    } else if (size == 'full') {
                        $scope.panel.notifySizeChanged(200);
                        var modalInstance = $uibModal.open({
                            animation: true,
                            templateUrl: 'service/servicefulltemplate.html',
                            controller: 'serviceFullCtrl',
                            size: 'lg',
                            scope: $scope
                        });
                    } else if (size == 'tiny') {
                        $scope.panel.notifySizeChanged(200);
                    }
                };
            }]);

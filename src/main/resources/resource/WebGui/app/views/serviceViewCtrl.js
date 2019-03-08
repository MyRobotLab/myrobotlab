angular.module('mrlapp.views')
        .controller('serviceViewCtrl', ['$scope', '$log', '$stateParams', '$filter', '$timeout', 'mrl', 'panelSvc',
            function ($scope, $log, $stateParams, $filter, $timeout, mrl, panelSvc) {
                $log.info('serviceViewCtrl');
                
                $scope.servicename = $stateParams.servicename;
                
                var isUndefinedOrNull = function (val) {
                    return angular.isUndefined(val) || val === null;
                };

                //service-panel(s) & update-routine
                var panelsUpdated = function (panels) {
                    $scope.panels = panels;
                    $timeout(function () {
                        var temp;
                        angular.forEach($scope.panels, function (value, key) {
                            if (value.name == $scope.servicename) {
                                temp = value;
                            }
                        });
                        if (!isUndefinedOrNull(temp)) {
                            $scope.panel = temp;
                            $scope.panelfound = true;
                        } else {
                            $scope.panelfound = false;
                        }
                        $log.info('panel-serviceView', $scope.panel);
                    });
                };
                panelsUpdated(panelSvc.getPanelsList());
                panelSvc.subscribeToUpdates(panelsUpdated);
            }]);

angular.module('mrlapp.views')
        .controller('serviceViewCtrl', ['$scope', '$log', '$stateParams', '$filter', '$timeout', 'mrl', 'serviceSvc',
            function ($scope, $log, $stateParams, $filter, $timeout, mrl, serviceSvc) {
                $log.info('serviceViewCtrl');
                
                $scope.servicename = $stateParams.servicename;
                
                var isUndefinedOrNull = function (val) {
                    return angular.isUndefined(val) || val === null;
                };

                //service-panel(s) & update-routine
                var panelsUpdated = function (panels) {
                    $scope.allpanels = panels;
                    $timeout(function () {
                        var temp;
                        angular.forEach($scope.allpanels, function (value, key) {
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
                panelsUpdated(serviceSvc.getPanelsList());
                serviceSvc.subscribeToUpdates(panelsUpdated);
            }]);

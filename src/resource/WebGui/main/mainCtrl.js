angular.module('mrlapp.main.mainCtrl', ['mrlapp.mrl'])
        .controller('mainCtrl', ['$scope', '$log', '$filter', '$timeout', 'mrl', 'serviceSvc', 'mrlLog',
            function ($scope, $log, $filter, $timeout, mrl, serviceSvc, mrlLog) {
                $log.info('mainCtrl');

                //service-panels & update-routine
                var panelsUpdated = function (panels) {
                    $scope.allpanels = panels;
                    $timeout(function () {
                        $scope.panels = $filter('panellist')($scope.allpanels, 'main');
                        $log.info('panels-main', $scope.panels);
                    });
                };
                panelsUpdated(serviceSvc.getPanelsList());
                serviceSvc.subscribeToUpdates(panelsUpdated);

                //access the array containing all log-messages logged using $log
                $scope.log = mrlLog.getLogMessages();
            }]);

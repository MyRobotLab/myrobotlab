angular.module('mrlapp.main.mainCtrl', ['mrlapp.mrl'])
        .controller('mainCtrl', ['$scope', '$log', '$filter', '$timeout', 'mrl', 'ServiceSvc', 'mrlLog',
            function ($scope, $log, $filter, $timeout, mrl, ServiceSvc, mrlLog) {
                $log.info('mainCtrl');

                //service-panels & update-routine
                var panelsUpdated = function () {
                    $timeout(function () {
                        $scope.allpanels = ServiceSvc.getPanelsList();
                        $scope.panels = $filter('panellist')($scope.allpanels, 'main');
                        $log.info('panels-main', $scope.panels);
                    });
                };
                panelsUpdated();
                ServiceSvc.subscribeToUpdates(panelsUpdated);

                //access the array containing all log-messages logged using $log
                $scope.log = mrlLog.getLogMessages();
            }]);

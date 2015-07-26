angular.module('mrlapp.main.mainCtrl', ['mrlapp.mrl'])
        .controller('mainCtrl', ['$scope', '$log', '$filter', 'mrl', 'ServiceSvc', 'mrlLog',
            function ($scope, $log, $filter, mrl, ServiceSvc, mrlLog) {

                $log.info('is connected: ' + mrl.isConnected());

                //service-panels & update-routine
                $scope.allpanels = ServiceSvc.getPanelsList();
                $scope.panels = $filter('panellist')($scope.allpanels, 'main');
                var panelsUpdated = function () {
                    $scope.panels = $filter('panellist')($scope.allpanels, 'main');
                    $scope.$apply();
                };
                ServiceSvc.subscribeToUpdates(panelsUpdated);

                //access the array containing all log-messages logged using $log
                $scope.log = mrlLog.getLogMessages();
            }]);

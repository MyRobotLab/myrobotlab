angular.module('mrlapp.main.mainCtrl', ['mrlapp.mrl'])
        .controller('mainCtrl', ['$scope', '$log', '$filter', '$timeout', 'mrl', 'serviceSvc',
            function ($scope, $log, $filter, $timeout, mrl, serviceSvc) {
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
            }]);

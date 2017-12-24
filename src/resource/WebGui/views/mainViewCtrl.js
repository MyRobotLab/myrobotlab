angular.module('mrlapp.views')
        .controller('mainViewCtrl', ['$scope', '$log', '$filter', '$timeout', 'mrl', 'panelSvc', '$state',
            function ($scope, $log, $filter, $timeout, mrl, panelSvc, $state) {
                $log.info('mainViewCtrl');

                //service-panels & update-routine
                var panelsUpdated = function (panels) {
                    $scope.panels = panels;
                    $timeout(function () {
                        $scope.panels = $filter('panellist')($scope.panels, 'main');
                        $log.info('panels-main', $scope.panels);
                    });
                };
                panelsUpdated(panelSvc.getPanelsList());
                panelSvc.subscribeToUpdates(panelsUpdated);
            }]);

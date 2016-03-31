angular.module('mrlapp.views')
        .controller('mainViewCtrl', ['$scope', '$log', '$filter', '$timeout', 'mrl', 'serviceSvc', '$state',
            function ($scope, $log, $filter, $timeout, mrl, serviceSvc, $state) {
                $log.info('mainViewCtrl');

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

angular.module('mrlapp.mrl').controller('mainViewCtrl', ['$scope', '$log', '$filter', '$timeout', 'mrl', '$state', function($scope, $log, $filter, $timeout, mrl, $state) {
    $log.info('mainViewCtrl');

    //service-panels & update-routine
    var panelsUpdated = function(panels) {
        $scope.panels = panels;
        $timeout(function() {
            $scope.panels = $filter('panellist')($scope.panels, 'main');
            $log.info('panels-main', $scope.panels);
        });
    };
    //panelsUpdated(mrl.getPanelList());
    
    panelsUpdated(mrl.getPanelList());
    mrl.subscribeToUpdates(panelsUpdated);
}
]);

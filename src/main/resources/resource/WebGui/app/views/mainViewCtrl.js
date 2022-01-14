angular.module('mrlapp.mrl').controller('mainViewCtrl', ['$scope', '$filter', '$timeout', 'mrl', '$state', function($scope, $filter, $timeout, mrl, $state) {
    console.info('mainViewCtrl');

    //service-panels & update-routine
    var panelsUpdated = function(panels) {
        $scope.panels = panels;
        $timeout(function() {
            $scope.panels = $filter('panellist')($scope.panels, 'main');
            console.info('panels-main', $scope.panels);
        });
    };
        
    panelsUpdated(mrl.getPanelList());
    mrl.subscribeToUpdates(panelsUpdated);
}
]);

angular.module('mrlapp.mrl').controller('tabsViewCtrl', ['$scope', '$log', '$filter', '$timeout', 'mrl', '$state', function($scope, $log, $filter, $timeout, mrl, $state) {
    $log.info('tabsViewCtrl');

    var isUndefinedOrNull = function(val) {
        return angular.isUndefined(val) || val === null;
    }

    $scope.view_tab = 'default';

    $scope.noworky = function() {
        noWorkySvc.openNoWorkyModal($scope.panel.name);
    }

    $scope.updateServiceData = function() {
        //get an updated / fresh servicedata & convert it to json
        var servicedata = mrl.getService($scope.view_tab);
        $scope.servicedatajson = JSON.stringify(servicedata, null, 2);
    }

    $scope.getName = function(panel) {
        return panel.name;
    }

    //service-panels & update-routine
    var panelsUpdated = function(panels) {
        console.debug('tabsViewCtrl.panelsUpdated ' + panels.length)
        $scope.panels = panels;
        $timeout(function() {
            console.debug('tabsViewCtrl.$timeout ' + $scope.panels.length)
            $scope.panels = $filter('panellist')($scope.panels, 'main');

            $log.info('tab-panels-post filter', $scope.panels.length, $scope.panels);
            if ($scope.view_tab == 'default' && !isUndefinedOrNull($scope.panels) && !isUndefinedOrNull($scope.panels[0])) {
                $scope.view_tab = $scope.panels[0].name;
            }
        });
    };
    panelsUpdated(mrl.getPanelsList());
    mrl.subscribeToUpdates(panelsUpdated);

    $scope.changeTab = function(tab) {
        $scope.view_tab = tab;
        $timeout(function() {
            $scope.$broadcast('rzSliderForceRender')
        })

    }
    ;
}
]);

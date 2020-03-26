angular.module('mrlapp.mrl').controller('serviceViewCtrl', ['$scope', '$stateParams', '$filter', '$timeout', 'mrl', function($scope, $stateParams, $filter, $timeout, mrl) {
    console.info('serviceViewCtrl')

    $scope.servicename = $stateParams.servicename

    var isUndefinedOrNull = function(val) {
        return angular.isUndefined(val) || val === null
    }

    //service-panel(s) & update-routine
    var panelsUpdated = function(panels) {
        console.debug('serviceViewCtrl.panelsUpdated ')
        $scope.panels = panels
        var temp
        angular.forEach($scope.panels, function(value, key) {
            if (value.name == $scope.servicename) {
                temp = value
            }
        })
        if (!isUndefinedOrNull(temp)) {
            $scope.panel = temp
            $scope.panelfound = true
        } else {
            $scope.panelfound = false
        }
        console.info('panel-serviceView', $scope.panel)
    }
    panelsUpdated(mrl.getPanelList())
    mrl.subscribeToUpdates(panelsUpdated)
}
])

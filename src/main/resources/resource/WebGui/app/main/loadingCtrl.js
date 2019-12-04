angular.module('mrlapp.main.mainCtrl').controller('loadingCtrl', ['$scope', '$log', 'mrl', '$state', '$previousState', function($scope, $log, mrl, $state, $previousState) {
    $log.info('loadingCtrl.js')

    $scope.mrlReady = false
    $scope.serviceSvcReady = false

    var go = function() {
        var previousstate = $previousState.get()
        if (previousstate == null) {
            $log.warn('can"t go back - there is no back -> defaulting to /main')
            $log.info('transitioning to state main')
            $state.go('main')
        } else {
            $log.info('transitioning to state', previousstate.state.name, 'with params', previousstate.params)
            $state.go(previousstate.state.name, previousstate.params)
        }
    }
}
])

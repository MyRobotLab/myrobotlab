angular.module('mrlapp.nav').controller('noWorkyCtrl', ['$scope', '$uibModalInstance', 'reason', 'mrl', function($scope, $uibModalInstance, reason, mrl) {
    let _self = this
    $scope.close = $uibModalInstance.close
    $scope.status = 'waitingforinformation'

    $scope.noWorky = function(userId) {
        $scope.status = 'sendingnoworky'
        mrl.noWorky(userId)
    }

    _self.onNoWorky = function(status) {
        if (status.level == 'error') {
            $scope.status = 'error'
            $scope.statuskey = status.key
        } else {
            $scope.status = 'success'
        }
        $scope.$apply()
    }

    mrl.subscribeTo('runtime', 'noWorky', _self.onNoWorky)
}
])

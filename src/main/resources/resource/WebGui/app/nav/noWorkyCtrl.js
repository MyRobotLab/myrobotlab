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

    // mrl.subscribeTo('runtime', 'noWorky')
    // defaulted to runtime@{remoteId} callback to runtime@{jsId}
    mrl.subscribeTo('runtime', 'noWorky', _self.onNoWorky)

    // mrl.subscribeToServiceMethod(onNoWorky, 'runtime@' + mrl.getRemoteId(), 'publishNoWorky')
    // jsRuntimeMethodCallbackMap[fullname + '.onDescribe'] = _self.onDescribe
    // mrl.sendTo('runtime@' + mrl.getRemoteId(), "addListener", "noWorky", 'runtime@' + mrl.getId())
    // mrl.subscribeTo(this, 'runtime@' + mrl.getRemoteId(), 'noWorky', 'runtime@' + mrl.getId())
}
])

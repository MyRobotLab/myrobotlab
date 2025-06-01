angular.module('mrlapp.nav').controller('shutdownCtrl', ['$scope', '$uibModalInstance', 'type', 'mrl', function($scope, $uibModalInstance, type, mrl) {
    $scope.type = type
    
    $scope.close = function() {
        $uibModalInstance.close()
    }
    
    $scope.shutdown = function() {
        switch (type) {
        case 'shutdown':
            console.info('attempt to SHUTDOWN')
            mrl.sendTo(mrl.getRuntime().name, 'shutdown')
            $uibModalInstance.close()
            break
        case 'restart':
            console.info('attempt to RESTART')
            mrl.sendTo(mrl.getRuntime().name, 'restart')
            $uibModalInstance.close()
            break
        default:
            break
        }
    }
}
])

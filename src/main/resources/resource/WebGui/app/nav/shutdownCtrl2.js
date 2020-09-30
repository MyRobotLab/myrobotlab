angular.module('mrlapp.nav').controller('shutdownCtrl2', ['$scope', '$uibModalInstance', 'type', 'mrl', function($scope, $uibModalInstance, type, mrl) {
    $scope.type = type
    $scope.shutdown = function() {
        switch (type) {
        case 'shutdown':
            console.info('attempt to SHUTDOWN')
            mrl.sendTo($scope.service.name + '@' + $scope.service.id, 'shutdown')
            $uibModalInstance.close()
            break
        case 'restart':
            console.info('attempt to RESTART')
            mrl.sendTo($scope.service.name + '@' + $scope.service.id, 'restart')
            $uibModalInstance.close()
            break
        default:
            break
        }
    }
}
])

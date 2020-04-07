angular.module('mrlapp.nav')
        .controller('aboutCtrl', ['$scope', '$uibModalInstance', 'mrl', function ($scope, $uibModalInstance, mrl) {
                $scope.close = $uibModalInstance.close;
        
                // get platform information for display
                $scope.platform = mrl.getPlatform();
                $scope.remotePlatform = mrl.getRemotePlatform();
                $scope.id = mrl.getId();

                $scope.registry = mrl.getRegistry();
            }]);
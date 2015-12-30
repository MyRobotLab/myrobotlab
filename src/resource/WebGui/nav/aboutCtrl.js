angular.module('mrlapp.nav')
        .controller('aboutCtrl', ['$scope', '$uibModalInstance', function ($scope, $uibModalInstance) {
                $scope.close = $uibModalInstance.close;
            }]);
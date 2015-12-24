angular.module('mrlapp.nav')
        .controller('aboutCtrl', ['$scope', '$modalInstance', function ($scope, $modalInstance) {
                $scope.close = $modalInstance.close;
            }]);
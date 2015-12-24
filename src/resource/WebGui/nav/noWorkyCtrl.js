angular.module('mrlapp.nav')
        .controller('noWorkyCtrl', ['$scope', '$modalInstance', 'reason', function ($scope, $modalInstance, reason) {
                $scope.close = $modalInstance.close;
            }]);
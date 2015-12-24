angular.module('mrlapp.nav')
        .controller('noWorkyCtrl', ['$scope', '$modalInstance', 'reason', 'mrl', function ($scope, $modalInstance, reason, mrl) {
                $scope.close = $modalInstance.close;
                
                $scope.noWorky = mrl.noWorky;
            }]);
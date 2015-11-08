angular.module('mrlapp.service')
        //TODO - maybe move this controller to serviceCtrl.js
        .controller('serviceFullCtrl', ['$scope', '$modalInstance', function ($scope, $modalInstance) {
                //controller for the modal (service-full)

                $scope.close = $modalInstance.close;
            }]);

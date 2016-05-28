angular.module('mrlapp.service')
        //TODO - maybe move this controller to serviceCtrl.js
        .controller('serviceFullCtrl', ['$scope', '$uibModalInstance', function ($scope, $uibModalInstance) {
                //controller for the modal (service-full)

                $scope.close = $uibModalInstance.close;
            }]);

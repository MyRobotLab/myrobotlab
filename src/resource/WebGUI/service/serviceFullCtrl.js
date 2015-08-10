angular.module('mrlapp.service')

        .controller('ServiceFullCtrl', ['$scope', '$modalInstance', function ($scope, $modalInstance) {
                //Controller for the modal (service-full)

                $scope.close = function () {
                    $modalInstance.close();
                };
            }]);

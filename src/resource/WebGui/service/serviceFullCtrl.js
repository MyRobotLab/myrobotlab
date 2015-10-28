angular.module('mrlapp.service')

        .controller('ServiceFullCtrl', function ($scope, $modalInstance, service, panel, service) {
            //Controller for the modal (service-full)

            $scope.service = service;
            $scope.panel = gui;
            $scope.service = service;

            $scope.modal = true;

            $scope.close = function () {
                $modalInstance.close();
            };
        });

angular.module('mrlapp.service')

        .controller('ServiceFullCtrl', function ($scope, $modalInstance, service, gui, service) {
            //Controller for the modal (service-full)

            $scope.service = service;
            $scope.gui = gui;
            $scope.service = service;

            $scope.modal = true;

            $scope.close = function () {
                $modalInstance.close();
            };
        });

angular.module('mrlapp.service')

        .controller('ServiceFullCtrl', function ($scope, $modalInstance, spawndata, gui, service) {
            //Controller for the modal (service-full)

            $scope.spawndata = spawndata;
            $scope.gui = gui;
            $scope.service = service;

            $scope.modal = true;

            $scope.close = function () {
                $modalInstance.close();
            };
        });

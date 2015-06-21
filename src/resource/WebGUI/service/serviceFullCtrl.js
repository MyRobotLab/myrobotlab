angular.module('mrlapp.service')

        .controller('ServiceFullCtrl', function ($scope, $modalInstance, service, fw, data, guidata, methods) {
            //Controller for the modal (service-full)

            $scope.service = service;
            $scope.fw = fw;
            $scope.data = data;
            $scope.guidata = guidata;
            $scope.methods = methods;

            $scope.modal = true;

            $scope.close = function () {
                $modalInstance.close();
            };
        });

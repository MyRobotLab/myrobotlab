angular.module('mrlapp.service.clockgui', ['ngDragDrop'])

        .controller('ClockGuiCtrl', ['$scope', 'ServiceControllerService', function ($scope, ServiceControllerService) {

                $scope.inst.methods.pulse = function () {
                    console.log("pulse - YEAH!!!");
                };

                ServiceControllerService.addListener($scope.name, "pulse", "pulse");

                //let "pulse" be called
                ServiceControllerService.test($scope.name);
            }]);
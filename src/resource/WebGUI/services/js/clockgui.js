angular.module('mrlapp.service.clockgui', [])

        .controller('ClockGuiCtrl', ['$scope', 'ServiceControllerService', function ($scope, ServiceControllerService) {

                $scope.inst.methods.pulse = function () {
                    console.log("pulse - YEAH!!!");
                };

                //refactor & ...
                ServiceControllerService.addListener($scope.name, "pulse", "pulse");

                //let "pulse" be called
                ServiceControllerService.test($scope.name);
            }]);
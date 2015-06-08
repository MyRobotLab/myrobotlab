angular.module('mrlapp.service.clockgui', [])

        .controller('ClockGuiCtrl', ['$scope', function ($scope) {

                if ($scope.inst.data.test != 'yaya') {
                    $scope.inst.data.test = 'yaya';
                    //only call subscribe once (loop!, onLocalServices is send back, reinitializing, re-subscribing, ...)
                    //-> fix Java (I think)
                    $scope.inst.fw.subscribe("pulse", "pulse");
                }

//                $scope.inst.methods.pulse = function () {
//                    console.log("pulse - YEAH!!!");
//                };
//
//                //let "pulse" be called
//                ServiceControllerService.test($scope.name, "pulse");
            }]);
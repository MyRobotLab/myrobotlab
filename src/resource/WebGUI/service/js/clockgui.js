angular.module('mrlapp.service.clockgui', [])

        .controller('ClockGuiCtrl', ['$scope', 'mrl', function ($scope, mrl) {
                console.log('ClockGuiCtrl');

                // load data bindings for this type
                $scope.data.pulseData = '';

                $scope.methods.onMsg = function (msg) {
                    console.log("Clock Msg ! - ");
                    if (msg.method == "onPulse") {
                        var pulseData = msg.data[0];
                        //$scope.serviceDirectory[msg.sender].pulseData = pulseData;
                        $scope.data.pulseData = pulseData;
                        console.log('pulseData', $scope.data.pulseData);
                        $scope.$apply();
                    }
                };

                mrl.subscribe($scope.data.name, 'pulse');

//                if ($scope.inst.data.test != 'yaya') {
//                    $scope.inst.data.test = 'yaya';
//                    //only call subscribe once (loop!, onLocalServices is send back, reinitializing, re-subscribing, ...)
//                    //-> fix Java (I think)
//                    $scope.inst.fw.subscribe("pulse", "pulse");
//                }

//                $scope.inst.methods.pulse = function () {
//                    console.log("pulse - YEAH!!!");
//                };
//
//                //let "pulse" be called
//                ServiceControllerService.test($scope.name, "pulse");
                $scope.fw.initDone();
            }]);
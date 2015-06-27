angular.module('mrlapp.service.clockgui', [])

        .controller('ClockGuiCtrl', ['$scope', 'mrl', function ($scope, mrl) {
                console.log('ClockGuiCtrl');
        
                $scope.gui.setPanelCount(2);

                // load data bindings for this type
                $scope.service.pulseData = '';

                $scope.service.onMsg = function (msg) {
                    console.log("Clock Msg ! - ");
                    if (msg.method == "onPulse") {
                        var pulseData = msg.data[0];
                        //$scope.serviceDirectory[msg.sender].pulseData = pulseData;
                        $scope.service.pulseData = pulseData;
                        console.log('pulseData', $scope.service.pulseData);
                        $scope.$apply();
                    }
                };

                mrl.subscribe($scope.service.name, 'pulse');

                $scope.gui.initDone();
            }]);